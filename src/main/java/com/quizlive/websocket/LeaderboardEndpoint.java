package com.quizlive.websocket;

import com.quizlive.model.LeaderboardEntry;
import com.quizlive.service.LeaderboardService;
import com.quizlive.util.JsonUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws/leaderboard/{quizId}")
public class LeaderboardEndpoint {

    private static final Map<Integer, Set<Session>> QUIZ_SESSIONS = new ConcurrentHashMap<>();

    private final LeaderboardService leaderboardService;

    public LeaderboardEndpoint() {
        this(new LeaderboardService());
    }

    public LeaderboardEndpoint(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("quizId") String quizIdStr) {
        int quizId = parseQuizId(quizIdStr);
        if (quizId <= 0) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid quiz ID"));
            } catch (IOException ignored) {
            }
            return;
        }

        QUIZ_SESSIONS.computeIfAbsent(quizId, k -> ConcurrentHashMap.newKeySet()).add(session);

        try {
            List<LeaderboardEntry> leaderboard = leaderboardService.getLeaderboard(quizId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "LEADERBOARD_UPDATE");
            payload.put("quizId", quizId);
            payload.put("data", leaderboard);
            session.getBasicRemote().sendText(JsonUtil.toJson(payload));
        } catch (Exception ignored) {
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("quizId") String quizIdStr) {
        int quizId = parseQuizId(quizIdStr);
        if (quizId <= 0) {
            return;
        }

        if (message != null && ("refresh".equalsIgnoreCase(message.trim()) || "ping".equalsIgnoreCase(message.trim()))) {
            try {
                List<LeaderboardEntry> leaderboard = leaderboardService.getLeaderboard(quizId);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("type", "LEADERBOARD_UPDATE");
                payload.put("quizId", quizId);
                payload.put("data", leaderboard);
                session.getBasicRemote().sendText(JsonUtil.toJson(payload));
            } catch (Exception ignored) {
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("quizId") String quizIdStr) {
        int quizId = parseQuizId(quizIdStr);
        if (quizId > 0) {
            Set<Session> sessions = QUIZ_SESSIONS.get(quizId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    QUIZ_SESSIONS.remove(quizId);
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable, @PathParam("quizId") String quizIdStr) {
        int quizId = parseQuizId(quizIdStr);
        if (quizId > 0) {
            Set<Session> sessions = QUIZ_SESSIONS.get(quizId);
            if (sessions != null) {
                sessions.remove(session);
            }
        }
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
    }

    public static void broadcast(int quizId, String jsonPayload) {
        Set<Session> sessions = QUIZ_SESSIONS.get(quizId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (Session s : sessions) {
            if (s != null && s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(jsonPayload);
                } catch (IOException e) {
                    sessions.remove(s);
                }
            }
        }
    }

    public static void broadcastLeaderboard(int quizId, LeaderboardService service) {
        try {
            List<LeaderboardEntry> leaderboard = service.getLeaderboard(quizId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "LEADERBOARD_UPDATE");
            payload.put("quizId", quizId);
            payload.put("data", leaderboard);
            broadcast(quizId, JsonUtil.toJson(payload));
        } catch (SQLException ignored) {
        }
    }

    public static int getActiveSessionsCount(int quizId) {
        Set<Session> sessions = QUIZ_SESSIONS.get(quizId);
        return (sessions != null) ? sessions.size() : 0;
    }

    public static void clearAllSessions() {
        QUIZ_SESSIONS.clear();
    }

    private static int parseQuizId(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
