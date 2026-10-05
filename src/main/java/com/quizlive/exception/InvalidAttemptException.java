package com.quizlive.exception;

/**
 * Thrown when an attempt operation is invalid (e.g. attempting to start
 * a quiz already taken, submitting an already-finalized attempt, etc.).
 */
public class InvalidAttemptException extends Exception {

    public InvalidAttemptException(String message) {
        super(message);
    }

    public InvalidAttemptException(String message, Throwable cause) {
        super(message, cause);
    }
}
