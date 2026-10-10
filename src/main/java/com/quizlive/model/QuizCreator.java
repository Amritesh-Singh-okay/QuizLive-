package com.quizlive.model;

import com.quizlive.model.enums.Role;

import java.sql.Timestamp;

/**
 * Represents a Quiz Creator (instructor/teacher) who authors quizzes and questions.
 * Subclass of AppUser (OOP Inheritance).
 */
public class QuizCreator extends AppUser {

    private static final long serialVersionUID = 1L;

    public QuizCreator() {
        setRole(Role.CREATOR);
    }

    public QuizCreator(int id, String name, String email, String passwordHash, String salt, Timestamp createdAt) {
        super(id, name, email, passwordHash, salt, Role.CREATOR, "STANDARD", createdAt);
    }

    public QuizCreator(int id, String name, String email, String passwordHash, String salt, String rank, Timestamp createdAt) {
        super(id, name, email, passwordHash, salt, Role.CREATOR, rank, createdAt);
    }

    @Override
    public boolean canCreateQuiz() {
        return true;
    }

    @Override
    public boolean canApproveQuiz() {
        return isRankApproved();
    }

    @Override
    public boolean canPublishDirectly() {
        return isRankApproved();
    }

    @Override
    public boolean canTakeQuiz() {
        return false;
    }

    @Override
    public String getDashboardUrl() {
        return "/creator/dashboard.jsp";
    }
}
