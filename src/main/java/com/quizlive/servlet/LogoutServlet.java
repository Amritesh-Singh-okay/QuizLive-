package com.quizlive.servlet;

import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout", "/api/logout", "/api/auth/logout"})
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processLogout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processLogout(req, resp);
    }

    private void processLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (isJsonRequest(req)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "Logged out successfully");
            JsonUtil.sendSuccess(resp, data);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?loggedOut=true");
        }
    }

    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String requestedWith = req.getHeader("X-Requested-With");
        String contentType = req.getContentType();
        return (accept != null && accept.contains("application/json")) ||
                "XMLHttpRequest".equalsIgnoreCase(requestedWith) ||
                (contentType != null && contentType.contains("application/json"));
    }
}
