package com.quizlive.model;

import com.quizlive.model.enums.AttemptStatus;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Attempt implements Gradeable, Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int quizId;
    private String quizTitle;
    private int userId;
    private String userName;
    private Timestamp startedAt;
    private Timestamp submittedAt;
    private int score;
    private int maxScore;
    private int tabSwitches;
    private AttemptStatus status;
    private List<AttemptAnswer> answers;

    public Attempt() {
        this.status = AttemptStatus.IN_PROGRESS;
        this.answers = new ArrayList<>();
    }

    public Attempt(int id, int quizId, int userId, Timestamp startedAt, Timestamp submittedAt,
                   int score, int maxScore, int tabSwitches, AttemptStatus status) {
        this.id = id;
        this.quizId = quizId;
        this.userId = userId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.score = score;
        this.maxScore = maxScore;
        this.tabSwitches = tabSwitches;
        this.status = status;
        this.answers = new ArrayList<>();
    }

    @Override
    public int computeScore() {
        return this.score;
    }

    @Override
    public double getPercentage() {
        if (maxScore <= 0) {
            return 0.0;
        }
        return Math.round(((double) score / maxScore) * 10000.0) / 100.0;
    }

    @Override
    public boolean isPassed(double passingPercentage) {
        return getPercentage() >= passingPercentage;
    }

    public long getDurationSeconds() {
        if (startedAt == null) {
            return 0;
        }
        long end = (submittedAt != null) ? submittedAt.getTime() : System.currentTimeMillis();
        return Math.max(0, (end - startedAt.getTime()) / 1000);
    }

    public String getFormattedDuration() {
        long totalSeconds = getDurationSeconds();
        long mins = totalSeconds / 60;
        long secs = totalSeconds % 60;
        return mins + "m " + secs + "s";
    }

    public void addAnswer(AttemptAnswer answer) {
        if (answer != null) {
            this.answers.add(answer);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
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

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
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

    public int getTabSwitches() {
        return tabSwitches;
    }

    public void setTabSwitches(int tabSwitches) {
        this.tabSwitches = tabSwitches;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public List<AttemptAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AttemptAnswer> answers) {
        this.answers = (answers != null) ? answers : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Attempt{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", userId=" + userId +
                ", score=" + score +
                ", percentage=" + getPercentage() + "%" +
                ", status=" + status +
                '}';
    }
}
