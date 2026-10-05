package com.quizlive.model;

import java.io.Serializable;

public class AttemptAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int attemptId;
    private int questionId;
    private Character selectedOption;
    private boolean isCorrect;
    private int pointsEarned;

    public AttemptAnswer() {
    }

    public AttemptAnswer(int id, int attemptId, int questionId, Character selectedOption, boolean isCorrect) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.isCorrect = isCorrect;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public Character getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(Character selectedOption) {
        this.selectedOption = (selectedOption != null) ? Character.toUpperCase(selectedOption) : null;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    @Override
    public String toString() {
        return "AttemptAnswer{" +
                "questionId=" + questionId +
                ", selectedOption=" + selectedOption +
                ", isCorrect=" + isCorrect +
                '}';
    }
}
