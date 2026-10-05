package com.quizlive.model.enums;

/**
 * Represents the moderation lifecycle states of a quiz.
 */
public enum QuizStatus {
    PENDING("Pending Review"),
    APPROVED("Approved & Live"),
    REJECTED("Rejected");

    private final String description;

    QuizStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Safely parses a string into a QuizStatus enum constant.
     *
     * @param value the status string
     * @return matching QuizStatus, or null if invalid
     */
    public static QuizStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return QuizStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
