package com.quizlive.exception;

/**
 * Thrown when an action is performed on a quiz that is not open,
 * not approved, or whose time limit has expired.
 */
public class QuizClosedException extends Exception {

    public QuizClosedException(String message) {
        super(message);
    }

    public QuizClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
