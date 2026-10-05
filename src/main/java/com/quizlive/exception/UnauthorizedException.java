package com.quizlive.exception;

/**
 * Thrown when an unauthenticated or unauthorized user attempts
 * to access a restricted resource or perform an unauthorized action.
 */
public class UnauthorizedException extends Exception {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
