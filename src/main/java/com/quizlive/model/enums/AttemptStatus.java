package com.quizlive.model.enums;

/**
 * Represents the state of a quiz attempt by a participant.
 */
public enum AttemptStatus {
    IN_PROGRESS("In Progress"),
    SUBMITTED("Submitted by Participant"),
    AUTO_SUBMITTED("Auto-Submitted by System Timeout");

    private final String description;

    AttemptStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Safely parses a string into an AttemptStatus enum constant.
     *
     * @param value the status string
     * @return matching AttemptStatus, or null if invalid
     */
    public static AttemptStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return AttemptStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
