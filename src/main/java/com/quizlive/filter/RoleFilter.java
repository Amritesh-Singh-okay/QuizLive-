package com.quizlive.filter;

import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
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

@WebFilter("/*")
public class RoleFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());

        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/admin/") || path.startsWith("/api/admin/")) {
            if (user.getRole() != Role.ADMIN) {
                denyAccess(req, res, user);
                return;
            }
        } else if (path.startsWith("/creator/") || path.startsWith("/api/creator/")) {
            if (user.getRole() != Role.CREATOR && user.getRole() != Role.ADMIN) {
                denyAccess(req, res, user);
                return;
            }
        } else if (path.startsWith("/participant/") || path.startsWith("/api/participant/")) {
            if (user.getRole() != Role.PARTICIPANT && user.getRole() != Role.ADMIN) {
                denyAccess(req, res, user);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse res, AppUser user) throws IOException {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        boolean isJson = (accept != null && accept.contains("application/json")) ||
                "XMLHttpRequest".equalsIgnoreCase(requestedWith);

        if (isJson) {
            JsonUtil.sendError(res, HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient permissions");
        } else {
            res.sendRedirect(req.getContextPath() + user.getDashboardUrl());
        }
    }
}
