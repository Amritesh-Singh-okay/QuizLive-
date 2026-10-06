package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.concurrency.QuizSchedulerService;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.exception.QuizClosedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.AttemptStatus;
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

@WebServlet(name = "StartAttemptServlet", urlPatterns = {"/attempts/start", "/participant/attempts/start"})
public class StartAttemptServlet extends HttpServlet {

    private final AttemptDao attemptDao;
    private final QuizService quizService;
    private final QuizSchedulerService schedulerService;

    public StartAttemptServlet() {
        this(new AttemptDaoImpl(), new QuizService(), new QuizSchedulerService());
    }

    public StartAttemptServlet(AttemptDao attemptDao, QuizService quizService, QuizSchedulerService schedulerService) {
        this.attemptDao = attemptDao;
        this.quizService = quizService;
        this.schedulerService = schedulerService;
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

        int quizId = parseQuizId(req);
        if (quizId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid quizId is required");
            return;
        }

        try {
            Quiz quiz = quizService.getQuizForTaking(quizId);

            Attempt existing = attemptDao.findByQuizAndUser(quizId, user.getId());
            if (existing != null) {
                if (existing.getStatus() == AttemptStatus.SUBMITTED || existing.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "You have already completed this quiz");
                    return;
                }

                if (!schedulerService.isScheduled(existing.getId())) {
                    schedulerService.scheduleAutoSubmit(existing.getId(), quizId, user.getId(), quiz.getDurationSeconds());
                }

                Map<String, Object> data = buildAttemptData(existing, quiz);
                JsonUtil.sendSuccess(resp, data);
                return;
            }

            Attempt attempt = attemptDao.startAttempt(quizId, user.getId());
            schedulerService.scheduleAutoSubmit(attempt.getId(), quizId, user.getId(), quiz.getDurationSeconds());

            Map<String, Object> data = buildAttemptData(attempt, quiz);
            JsonUtil.sendCreated(resp, data);
        } catch (QuizClosedException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error starting attempt: " + e.getMessage());
        }
    }

    private int parseQuizId(HttpServletRequest req) throws IOException {
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
                            return ((Number) map.get("quizId")).intValue();
                        } else if (map.get("id") != null) {
                            return ((Number) map.get("id")).intValue();
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        String idParam = req.getParameter("quizId");
        if (idParam == null || idParam.isEmpty()) {
            idParam = req.getParameter("id");
        }
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                return Integer.parseInt(idParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return 0;
    }

    private Map<String, Object> buildAttemptData(Attempt attempt, Quiz quiz) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("attemptId", attempt.getId());
        data.put("quizId", quiz.getId());
        data.put("title", quiz.getTitle());
        data.put("description", quiz.getDescription());
        data.put("durationSeconds", quiz.getDurationSeconds());
        data.put("startedAt", attempt.getStartedAt());
        data.put("questions", quiz.getQuestions());
        return data;
    }
}
