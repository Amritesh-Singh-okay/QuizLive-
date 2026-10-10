package com.quizlive.websocket;

import com.quizlive.util.JsonUtil;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time WebSocket endpoint for the Quiz Waiting Room (Lobby).
 * Allows student participants to wait in a live lobby until the host starts the quiz,
 * broadcasting live participant counts and triggering instantaneous start signals to all screens.
 */
@ServerEndpoint(value = "/ws/waiting-room/{quizId}")
public class WaitingRoomEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingRoomEndpoint.class);
    private static final Map<Integer, Set<Session>> QUIZ_WAITING_SESSIONS = new ConcurrentHashMap<>();

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

        QUIZ_WAITING_SESSIONS.computeIfAbsent(quizId, k -> ConcurrentHashMap.newKeySet()).add(session);
        LOGGER.info("Session {} joined waiting room for quiz {}. Total waiting: {}", session.getId(), quizId, getWaitingCount(quizId));

        broadcastLobbyCount(quizId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("quizId") String quizIdStr) {
        int quizId = parseQuizId(quizIdStr);
        if (quizId > 0) {
            Set<Session> sessions = QUIZ_WAITING_SESSIONS.get(quizId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    QUIZ_WAITING_SESSIONS.remove(quizId);
                }
            }
            broadcastLobbyCount(quizId);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOGGER.debug("Waiting room WebSocket error: {}", throwable.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("quizId") String quizIdStr) {
        // Heartbeat / ping handling
        if ("ping".equalsIgnoreCase(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (IOException ignored) {
            }
        }
    }

    public static int getWaitingCount(int quizId) {
        Set<Session> sessions = QUIZ_WAITING_SESSIONS.get(quizId);
        return sessions != null ? sessions.size() : 0;
    }

    public static void broadcastLobbyCount(int quizId) {
        Set<Session> sessions = QUIZ_WAITING_SESSIONS.get(quizId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        int count = sessions.size();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "LOBBY_UPDATE");
        payload.put("quizId", quizId);
        payload.put("waitingCount", count);
        String json = JsonUtil.toJson(payload);

        broadcast(sessions, json);
    }

    public static void broadcastQuizStarted(int quizId) {
        Set<Session> sessions = QUIZ_WAITING_SESSIONS.get(quizId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "QUIZ_STARTED");
        payload.put("quizId", quizId);
        payload.put("message", "The host has started the quiz! Launching exam session now...");
        String json = JsonUtil.toJson(payload);

        broadcast(sessions, json);
    }

    public static void broadcastQuizHeld(int quizId) {
        Set<Session> sessions = QUIZ_WAITING_SESSIONS.get(quizId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "QUIZ_HELD");
        payload.put("quizId", quizId);
        payload.put("message", "The quiz has been placed on hold by the host.");
        String json = JsonUtil.toJson(payload);

        broadcast(sessions, json);
    }

    private static void broadcast(Set<Session> sessions, String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    LOGGER.debug("Failed to send waiting room broadcast to session: {}", e.getMessage());
                }
            }
        }
    }

    private static int parseQuizId(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
