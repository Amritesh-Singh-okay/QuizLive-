package com.quizlive.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.quizlive.exception.UnauthorizedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
import com.quizlive.util.JsonUtil;
import com.quizlive.websocket.WaitingRoomEndpoint;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for host live controls (Hold/Start quiz, schedule start, and lobby status).
 */
@WebServlet(name = "HostQuizServlet", urlPatterns = {"/api/quizzes/host", "/api/creator/quizzes/host", "/api/quizzes/lobby-status"})
public class HostQuizServlet extends HttpServlet {

    private final QuizService quizService;

    public HostQuizServlet() {
        this(new QuizService());
    }

    public HostQuizServlet(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int quizId = parseQuizId(req, null);
        if (quizId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid quizId is required");
            return;
        }

        try {
            Quiz quiz = quizService.findById(quizId);
            if (quiz == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Quiz not found with ID: " + quizId);
                return;
            }

            boolean canStart = quiz.canParticipantsStartNow();
            int waitingCount = WaitingRoomEndpoint.getWaitingCount(quizId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("quizId", quiz.getId());
            data.put("title", quiz.getTitle());
            data.put("description", quiz.getDescription());
            data.put("durationSeconds", quiz.getDurationSeconds());
            data.put("status", quiz.getStatus() != null ? quiz.getStatus().name() : "PENDING");
            data.put("isHeld", quiz.isHeld());
            data.put("scheduledStartAt", quiz.getScheduledStartAt());
            data.put("canStart", canStart);
            data.put("waitingCount", waitingCount);

            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving lobby status: " + e.getMessage());
        }
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

        if (user.getRole() != Role.CREATOR && user.getRole() != Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only creators or administrators can manage quiz sessions");
            return;
        }

        JsonObject bodyJson = parseJsonBody(req);
        int quizId = parseQuizId(req, bodyJson);
        if (quizId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid quizId is required");
            return;
        }

        String action = parseAction(req, bodyJson);
        if (action == null || action.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Action ('start', 'hold', or 'schedule') is required");
            return;
        }

        boolean isAdmin = (user.getRole() == Role.ADMIN);

        try {
            if ("start".equalsIgnoreCase(action)) {
                boolean success = quizService.startQuizSession(quizId, user.getId(), isAdmin);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("quizId", quizId);
                result.put("isHeld", false);
                result.put("started", success);
                result.put("message", "Quiz session started! Waiting participants have been released into the exam.");
                JsonUtil.sendSuccess(resp, result);
            } else if ("hold".equalsIgnoreCase(action)) {
                boolean success = quizService.holdQuizSession(quizId, user.getId(), isAdmin);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("quizId", quizId);
                result.put("isHeld", true);
                result.put("held", success);
                result.put("message", "Quiz held. New and existing participants are directed to the waiting lobby.");
                JsonUtil.sendSuccess(resp, result);
            } else if ("schedule".equalsIgnoreCase(action)) {
                Timestamp scheduledTime = parseScheduledTime(req, bodyJson);
                boolean success = quizService.updateScheduledStart(quizId, scheduledTime, user.getId(), isAdmin);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("quizId", quizId);
                result.put("scheduledStartAt", scheduledTime);
                result.put("scheduled", success);
                result.put("message", scheduledTime != null ? "Quiz start scheduled successfully." : "Scheduled start time cleared.");
                JsonUtil.sendSuccess(resp, result);
            } else if ("visibility".equalsIgnoreCase(action)) {
                boolean isPublic = parseIsPublic(req, bodyJson);
                boolean success = quizService.updateVisibility(quizId, isPublic, user.getId(), isAdmin);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("quizId", quizId);
                result.put("isPublic", isPublic);
                result.put("updated", success);
                result.put("message", isPublic ? "Quiz is now visible in the public catalog." : "Quiz is now unlisted (private, accessible via access code).");
                JsonUtil.sendSuccess(resp, result);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Unsupported action: " + action + ". Supported actions: start, hold, schedule, visibility.");
            }
        } catch (UnauthorizedException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating quiz host status: " + e.getMessage());
        }
    }

    private JsonObject parseJsonBody(HttpServletRequest req) throws IOException {
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
                    JsonElement elem = JsonParser.parseString(json);
                    if (elem.isJsonObject()) {
                        return elem.getAsJsonObject();
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }
        return null;
    }

    private int parseQuizId(HttpServletRequest req, JsonObject json) {
        if (json != null) {
            if (json.has("quizId") && !json.get("quizId").isJsonNull()) {
                try {
                    return json.get("quizId").getAsInt();
                } catch (Exception ignored) {}
            }
            if (json.has("id") && !json.get("id").isJsonNull()) {
                try {
                    return json.get("id").getAsInt();
                } catch (Exception ignored) {}
            }
        }

        String param = req.getParameter("quizId");
        if (param == null || param.trim().isEmpty()) {
            param = req.getParameter("id");
        }
        if (param != null && !param.trim().isEmpty()) {
            try {
                return Integer.parseInt(param.trim());
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private String parseAction(HttpServletRequest req, JsonObject json) {
        if (json != null && json.has("action") && !json.get("action").isJsonNull()) {
            return json.get("action").getAsString();
        }
        return req.getParameter("action");
    }

    private Timestamp parseScheduledTime(HttpServletRequest req, JsonObject json) {
        String val = null;
        if (json != null) {
            if (json.has("scheduledStartAt") && !json.get("scheduledStartAt").isJsonNull()) {
                val = json.get("scheduledStartAt").getAsString();
            } else if (json.has("scheduledStartTime") && !json.get("scheduledStartTime").isJsonNull()) {
                val = json.get("scheduledStartTime").getAsString();
            }
        }
        if (val == null) {
            val = req.getParameter("scheduledStartAt");
            if (val == null) {
                val = req.getParameter("scheduledStartTime");
            }
        }

        if (val == null || val.trim().isEmpty()) {
            return null;
        }

        val = val.trim();
        try {
            return new Timestamp(Long.parseLong(val));
        } catch (NumberFormatException ignored) {}

        val = val.replace("T", " ");
        if (val.length() == 16) {
            val += ":00";
        }
        try {
            return Timestamp.valueOf(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean parseIsPublic(HttpServletRequest req, JsonObject json) {
        if (json != null && json.has("isPublic") && !json.get("isPublic").isJsonNull()) {
            try {
                return json.get("isPublic").getAsBoolean();
            } catch (Exception ignored) {
                return !"false".equalsIgnoreCase(json.get("isPublic").getAsString());
            }
        }
        String param = req.getParameter("isPublic");
        if (param != null) {
            return !"false".equalsIgnoreCase(param.trim());
        }
        return true;
    }
}
