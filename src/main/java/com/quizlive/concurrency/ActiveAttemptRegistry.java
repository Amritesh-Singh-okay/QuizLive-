package com.quizlive.concurrency;

import java.util.concurrent.ConcurrentHashMap;

public class ActiveAttemptRegistry {

    private static final int DEFAULT_GRACE_PERIOD_SECONDS = 5;
    private static final ActiveAttemptRegistry INSTANCE = new ActiveAttemptRegistry();

    private final ConcurrentHashMap<Integer, AttemptSession> activeSessions = new ConcurrentHashMap<>();

    public ActiveAttemptRegistry() {
    }

    public static ActiveAttemptRegistry getInstance() {
        return INSTANCE;
    }

    public AttemptSession registerSession(int attemptId, int quizId, int userId, int durationSeconds) {
        return registerSession(attemptId, quizId, userId, durationSeconds, DEFAULT_GRACE_PERIOD_SECONDS);
    }

    public AttemptSession registerSession(int attemptId, int quizId, int userId, int durationSeconds, int gracePeriodSeconds) {
        AttemptSession session = new AttemptSession(attemptId, quizId, userId, durationSeconds, gracePeriodSeconds);
        activeSessions.put(attemptId, session);
        return session;
    }

    public AttemptSession getSession(int attemptId) {
        return activeSessions.get(attemptId);
    }

    public AttemptSession removeSession(int attemptId) {
        return activeSessions.remove(attemptId);
    }

    public int incrementTabSwitches(int attemptId) {
        AttemptSession session = activeSessions.get(attemptId);
        if (session != null) {
            return session.incrementTabSwitches();
        }
        return -1;
    }

    public boolean isExpired(int attemptId) {
        AttemptSession session = activeSessions.get(attemptId);
        return session != null && session.isExpired();
    }

    public boolean isAttemptActive(int attemptId) {
        return activeSessions.containsKey(attemptId);
    }

    public int getActiveCount() {
        return activeSessions.size();
    }

    public void clear() {
        activeSessions.clear();
    }
}
