package com.quizlive.model;

public interface Gradeable {
    int computeScore();
    double getPercentage();
    boolean isPassed(double passingPercentage);
}
