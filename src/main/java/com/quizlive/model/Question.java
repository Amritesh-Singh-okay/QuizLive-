package com.quizlive.model;

import java.io.Serializable;

/**
 * Represents a multiple-choice question within a quiz.
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int quizId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private char correctOption; // 'A', 'B', 'C', or 'D'
    private int points;

    public Question() {
        this.points = 1;
    }

    public Question(int id, int quizId, String questionText, String optionA, String optionB,
                    String optionC, String optionD, char correctOption, int points) {
        this.id = id;
        this.quizId = quizId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = Character.toUpperCase(correctOption);
        this.points = points;
    }

    /**
     * Security sanitizer: Returns a copy of this Question with the correct answer
     * stripped away so it can be safely sent to a participant taking the quiz.
     */
    public Question sanitized() {
        Question safe = new Question();
        safe.setId(this.id);
        safe.setQuizId(this.quizId);
        safe.setQuestionText(this.questionText);
        safe.setOptionA(this.optionA);
        safe.setOptionB(this.optionB);
        safe.setOptionC(this.optionC);
        safe.setOptionD(this.optionD);
        safe.setPoints(this.points);
        safe.setCorrectOption(' '); // Blank out answer key
        return safe;
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

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public char getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(char correctOption) {
        this.correctOption = Character.toUpperCase(correctOption);
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", questionText='" + questionText + '\'' +
                ", points=" + points +
                '}';
    }
}
