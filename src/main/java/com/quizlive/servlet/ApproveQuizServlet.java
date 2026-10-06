package com.quizlive.servlet;

import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
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

@WebServlet(name = "ApproveQuizServlet", urlPatterns = {"/quizzes/approve", "/quizzes/reject", "/admin/quizzes/approve", "/admin/quizzes/reject", "/api/quizzes/approve", "/api/quizzes/reject", "/api/admin/quizzes/approve", "/api/admin/quizzes/reject"})
public class ApproveQuizServlet extends HttpServlet {

    private final QuizService quizService;

    public ApproveQuizServlet() {
        this(new QuizService());
    }

    public ApproveQuizServlet(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        if (!user.canApproveQuiz() && user.getRole() != Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can approve or reject quizzes");
            return;
        }

        int quizId = 0;
        String action = null;

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
                try {
                    Map<?, ?> map = JsonUtil.fromJson(json, Map.class);
                    if (map != null) {
                        if (map.get("quizId") != null) {
                            quizId = ((Number) map.get("quizId")).intValue();
                        } else if (map.get("id") != null) {
                            quizId = ((Number) map.get("id")).intValue();
                        }
                        if (map.get("action") != null) {
                            action = map.get("action").toString();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (quizId <= 0) {
            String idStr = req.getParameter("quizId");
            if (idStr == null || idStr.isEmpty()) {
                idStr = req.getParameter("id");
            }
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    quizId = Integer.parseInt(idStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (action == null) {
            action = req.getParameter("action");
        }

        String path = req.getRequestURI();
        if (action == null) {
            if (path.endsWith("/reject")) {
                action = "reject";
            } else {
                action = "approve";
            }
        }

        if (quizId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid quizId is required");
            return;
        }

        try {
            boolean isReject = "reject".equalsIgnoreCase(action);
            boolean updated = isReject ? quizService.rejectQuiz(quizId) : quizService.approveQuiz(quizId);

            if (!updated) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
                return;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("quizId", quizId);
            result.put("status", isReject ? "REJECTED" : "APPROVED");
            result.put("message", isReject ? "Quiz rejected successfully" : "Quiz approved successfully");
            JsonUtil.sendSuccess(resp, result);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating quiz status");
        }
    }
}
