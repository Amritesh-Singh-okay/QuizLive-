package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.exception.UnauthorizedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.service.AuthService;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private final AuthService authService;

    public LoginServlet() {
        this(new AuthService());
    }

    public LoginServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (isJsonRequest(req)) {
            if (user != null) {
                Map<String, Object> userData = createUserData(req, user);
                JsonUtil.sendSuccess(resp, userData);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            }
            return;
        }

        if (user != null) {
            resp.sendRedirect(resolveDashboardUrl(req, user.getRole()));
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, String> params;
        try {
            params = parseParameters(req);
        } catch (JsonSyntaxException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request body");
            return;
        }

        String email = params.get("email");
        String password = params.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?error=Missing+credentials");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email and password are required");
            }
            return;
        }

        try {
            AppUser user = authService.login(email.trim(), password);
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);

            String redirectUrl = resolveDashboardUrl(req, user.getRole());
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(redirectUrl);
            } else {
                Map<String, Object> userData = createUserData(req, user);
                JsonUtil.sendSuccess(resp, userData);
            }
        } catch (UnauthorizedException e) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?error=Invalid+credentials");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?error=" + e.getMessage());
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } catch (SQLException e) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/login.jsp?error=Server+error");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during authentication");
            }
        }
    }

    public static String resolveDashboardUrl(HttpServletRequest req, Role role) {
        String context = req.getContextPath();
        if (role == null) {
            return context + "/login.jsp";
        }
        return switch (role) {
            case ADMIN -> context + "/admin/dashboard.jsp";
            case CREATOR -> context + "/creator/dashboard.jsp";
            case PARTICIPANT -> context + "/participant/dashboard.jsp";
        };
    }

    public static Map<String, Object> createUserData(HttpServletRequest req, AppUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        data.put("redirectUrl", resolveDashboardUrl(req, user.getRole()));
        return data;
    }

    private Map<String, String> parseParameters(HttpServletRequest req) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String json = sb.toString().trim();
            if (!json.isEmpty()) {
                Map<?, ?> jsonMap = JsonUtil.fromJson(json, Map.class);
                if (jsonMap != null) {
                    for (Map.Entry<?, ?> entry : jsonMap.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            params.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                    }
                }
            }
        }

        req.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0 && !params.containsKey(key)) {
                params.put(key, values[0]);
            }
        });

        return params;
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        String contentType = req.getContentType();
        return (accept != null && accept.contains("application/json")) ||
                "XMLHttpRequest".equalsIgnoreCase(requestedWith) ||
                (contentType != null && contentType.contains("application/json"));
    }

    private boolean isHtmlFormPost(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String contentType = req.getContentType();
        return contentType != null && contentType.contains("application/x-www-form-urlencoded")
                && accept != null && accept.contains("text/html")
                && !accept.contains("application/json");
    }
}
