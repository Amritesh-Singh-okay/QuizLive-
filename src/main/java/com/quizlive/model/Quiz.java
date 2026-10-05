package com.quizlive.model;

import com.quizlive.model.enums.QuizStatus;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Quiz with its metadata and embedded collection of Questions.
 * Demonstrates Collections & Generics (List<Question>).
 */
public class Quiz implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String title;
    private String description;
    private int creatorId;
    private String creatorName; // Populated via SQL JOIN for convenience
    private int durationSeconds;
    private QuizStatus status;
    private Timestamp createdAt;
    private List<Question> questions;

    public Quiz() {
        this.status = QuizStatus.PENDING;
        this.questions = new ArrayList<>();
    }

    public Quiz(int id, String title, String description, int creatorId, int durationSeconds, QuizStatus status, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.creatorId = creatorId;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.questions = new ArrayList<>();
    }

    /**
     * Adds a question to this quiz.
     */
    public void addQuestion(Question question) {
        if (question != null) {
            this.questions.add(question);
        }
    }

    /**
     * Calculates the sum of all question points.
     */
    public int getTotalPoints() {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        return questions.stream().mapToInt(Question::getPoints).sum();
    }

    /**
     * Returns total question count.
     */
    public int getQuestionCount() {
        return (questions == null) ? 0 : questions.size();
    }

    /**
     * Formats duration in minutes and seconds (e.g. "5 mins" or "2m 30s").
     */
    public String getFormattedDuration() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        if (seconds == 0) {
            return minutes + " mins";
        }
        return minutes + "m " + seconds + "s";
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public QuizStatus getStatus() {
        return status;
    }

    public void setStatus(QuizStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = (questions != null) ? questions : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", durationSeconds=" + durationSeconds +
                ", status=" + status +
                ", questionsCount=" + getQuestionCount() +
                '}';
    }
}
