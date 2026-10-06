package com.quizlive.concurrency;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class AttemptSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int attemptId;
    private final int quizId;
    private final int userId;
    private final long startedAtEpochMs;
    private final int durationSeconds;
    private final int gracePeriodSeconds;
    private final AtomicInteger tabSwitches;

    public AttemptSession(int attemptId, int quizId, int userId, int durationSeconds, int gracePeriodSeconds) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.userId = userId;
        this.startedAtEpochMs = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.tabSwitches = new AtomicInteger(0);
    }

    public int incrementTabSwitches() {
        return this.tabSwitches.incrementAndGet();
    }

    public int getTabSwitches() {
        return this.tabSwitches.get();
    }

    public long getRemainingSeconds() {
        long deadline = startedAtEpochMs + (durationSeconds * 1000L);
        long remaining = (deadline - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public boolean isExpired() {
        long allowedDeadline = startedAtEpochMs + ((durationSeconds + gracePeriodSeconds) * 1000L);
        return System.currentTimeMillis() > allowedDeadline;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startedAtEpochMs) / 1000;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public int getQuizId() {
        return quizId;
    }

    public int getUserId() {
        return userId;
    }

    public long getStartedAtEpochMs() {
        return startedAtEpochMs;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getGracePeriodSeconds() {
        return gracePeriodSeconds;
    }

    @Override
    public String toString() {
        return "AttemptSession{" +
                "attemptId=" + attemptId +
                ", remaining=" + getRemainingSeconds() + "s" +
                ", tabSwitches=" + tabSwitches.get() +
                '}';
    }
}
