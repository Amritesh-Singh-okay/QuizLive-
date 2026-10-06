package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
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

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register", "/api/auth/register", "/api/register"})
public class RegisterServlet extends HttpServlet {

    private final AuthService authService;

    public RegisterServlet() {
        this(new AuthService());
    }

    public RegisterServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user != null) {
            resp.sendRedirect(LoginServlet.resolveDashboardUrl(req, user.getRole()));
        } else {
            resp.sendRedirect(req.getContextPath() + "/register.jsp");
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

        String name = params.get("name");
        String email = params.get("email");
        String password = params.get("password");
        String roleStr = params.get("role");

        Role role = Role.PARTICIPANT;
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            role = Role.fromString(roleStr);
            if (role == null) {
                if (isHtmlFormPost(req)) {
                    resp.sendRedirect(req.getContextPath() + "/register.jsp?error=Invalid+role");
                } else {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid role specified");
                }
                return;
            }
        }

        if (role == Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Registration with ADMIN role is not permitted");
            return;
        }

        try {
            AppUser newUser = authService.register(name, email, password, role);
            HttpSession session = req.getSession(true);
            session.setAttribute("user", newUser);

            String redirectUrl = LoginServlet.resolveDashboardUrl(req, newUser.getRole());
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(redirectUrl);
            } else {
                Map<String, Object> userData = LoginServlet.createUserData(req, newUser);
                JsonUtil.sendCreated(resp, userData);
            }
        } catch (IllegalArgumentException e) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/register.jsp?error=" + e.getMessage());
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        } catch (SQLException e) {
            if (isHtmlFormPost(req)) {
                resp.sendRedirect(req.getContextPath() + "/register.jsp?error=Server+error");
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error during registration");
            }
        }
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

    private boolean isHtmlFormPost(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String contentType = req.getContentType();
        return contentType != null && contentType.contains("application/x-www-form-urlencoded")
                && accept != null && accept.contains("text/html")
                && !accept.contains("application/json");
    }
}
