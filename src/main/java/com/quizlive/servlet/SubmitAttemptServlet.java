package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.concurrency.QuizSchedulerService;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.exception.InvalidAttemptException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.service.ScoringService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "SubmitAttemptServlet", urlPatterns = {"/attempts/submit", "/participant/attempts/submit"})
public class SubmitAttemptServlet extends HttpServlet {

    private final ScoringService scoringService;
    private final QuizSchedulerService schedulerService;
    private final AttemptDao attemptDao;

    public SubmitAttemptServlet() {
        this(new ScoringService(), new QuizSchedulerService(), new AttemptDaoImpl());
    }

    public SubmitAttemptServlet(ScoringService scoringService, QuizSchedulerService schedulerService, AttemptDao attemptDao) {
        this.scoringService = scoringService;
        this.schedulerService = schedulerService;
        this.attemptDao = attemptDao;
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

        SubmitRequest submitRequest = parseSubmitRequest(req);
        if (submitRequest == null || submitRequest.attemptId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid attemptId is required");
            return;
        }

        try {
            Attempt attempt = attemptDao.findById(submitRequest.attemptId);
            if (attempt == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Attempt not found");
                return;
            }

            if (attempt.getUserId() != user.getId() && user.getRole() != Role.ADMIN) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Cannot submit another user's attempt");
                return;
            }

            if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Attempt has already been finalized");
                return;
            }

            schedulerService.cancelScheduledAutoSubmit(submitRequest.attemptId);

            Attempt scored = scoringService.scoreAndSubmit(submitRequest.attemptId, submitRequest.answers);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("attemptId", scored.getId());
            result.put("quizId", scored.getQuizId());
            result.put("score", scored.getScore());
            result.put("maxScore", scored.getMaxScore());
            result.put("percentage", scored.getPercentage());
            result.put("status", scored.getStatus().name());
            result.put("tabSwitches", scored.getTabSwitches());
            result.put("submittedAt", scored.getSubmittedAt());

            JsonUtil.sendSuccess(resp, result);
        } catch (InvalidAttemptException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error submitting attempt: " + e.getMessage());
        }
    }

    private SubmitRequest parseSubmitRequest(HttpServletRequest req) throws IOException {
        SubmitRequest result = new SubmitRequest();
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
                        if (map.get("attemptId") != null) {
                            result.attemptId = ((Number) map.get("attemptId")).intValue();
                        } else if (map.get("id") != null) {
                            result.attemptId = ((Number) map.get("id")).intValue();
                        }

                        Object answersObj = map.get("answers");
                        if (answersObj instanceof Map<?, ?> ansMap) {
                            for (Map.Entry<?, ?> entry : ansMap.entrySet()) {
                                if (entry.getKey() != null && entry.getValue() != null) {
                                    try {
                                        int qId = Integer.parseInt(entry.getKey().toString());
                                        String optStr = entry.getValue().toString().trim();
                                        if (!optStr.isEmpty()) {
                                            result.answers.put(qId, Character.toUpperCase(optStr.charAt(0)));
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        } else if (answersObj instanceof List<?> ansList) {
                            for (Object item : ansList) {
                                if (item instanceof Map<?, ?> itemMap) {
                                    Object qIdObj = itemMap.get("questionId");
                                    Object optObj = itemMap.get("selectedOption");
                                    if (qIdObj != null && optObj != null) {
                                        int qId = ((Number) qIdObj).intValue();
                                        String optStr = optObj.toString().trim();
                                        if (!optStr.isEmpty()) {
                                            result.answers.put(qId, Character.toUpperCase(optStr.charAt(0)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        if (result.attemptId <= 0) {
            String idParam = req.getParameter("attemptId");
            if (idParam == null || idParam.isEmpty()) {
                idParam = req.getParameter("id");
            }
            if (idParam != null && !idParam.trim().isEmpty()) {
                try {
                    result.attemptId = Integer.parseInt(idParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

    private static class SubmitRequest {
        int attemptId = 0;
        final Map<Integer, Character> answers = new HashMap<>();
    }
}
