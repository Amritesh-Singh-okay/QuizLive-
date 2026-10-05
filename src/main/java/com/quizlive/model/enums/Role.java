package com.quizlive.model.enums;

/**
 * Defines the security roles available in the QuizLive platform.
 */
public enum Role {
    ADMIN("Admin"),
    CREATOR("Quiz Creator"),
    PARTICIPANT("Participant");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Safely parses a string into a Role enum constant.
     *
     * @param value the role name string
     * @return matching Role, or null if invalid
     */
    public static Role fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
