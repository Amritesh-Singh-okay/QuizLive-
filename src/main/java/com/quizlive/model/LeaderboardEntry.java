package com.quizlive.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class LeaderboardEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private int rank;
    private int attemptId;
    private int userId;
    private String userName;
    private int score;
    private int maxScore;
    private double percentage;
    private long durationSeconds;
    private String formattedDuration;
    private int tabSwitches;
    private Timestamp submittedAt;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(int rank, int attemptId, int userId, String userName, int score,
                            int maxScore, double percentage, long durationSeconds,
                            String formattedDuration, int tabSwitches, Timestamp submittedAt) {
        this.rank = rank;
        this.attemptId = attemptId;
        this.userId = userId;
        this.userName = userName;
        this.score = score;
        this.maxScore = maxScore;
        this.percentage = percentage;
        this.durationSeconds = durationSeconds;
        this.formattedDuration = formattedDuration;
        this.tabSwitches = tabSwitches;
        this.submittedAt = submittedAt;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFormattedDuration() {
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }

    public int getTabSwitches() {
        return tabSwitches;
    }

    public void setTabSwitches(int tabSwitches) {
        this.tabSwitches = tabSwitches;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Override
    public String toString() {
        return "#" + rank + " " + userName + " (" + score + " pts, " + formattedDuration + ")";
    }
}
