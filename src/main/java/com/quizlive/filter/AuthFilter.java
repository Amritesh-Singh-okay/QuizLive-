package com.quizlive.filter;

import com.quizlive.model.AppUser;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/",
            "/index.jsp",
            "/login",
            "/login.jsp",
            "/register",
            "/register.jsp",
            "/logout",
            "/health",
            "/unauthorized.jsp"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (isPublicResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user != null) {
            chain.doFilter(request, response);
            return;
        }

        if (isJsonRequest(req, path)) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        } else {
            res.sendRedirect(req.getContextPath() + "/login.jsp");
        }
    }

    private boolean isPublicResource(String path) {
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/ws/");
    }

    private boolean isJsonRequest(HttpServletRequest req, String path) {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json")) ||
                "XMLHttpRequest".equalsIgnoreCase(requestedWith) ||
                path.startsWith("/api/");
    }
}
