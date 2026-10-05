package com.quizlive.model;

import com.quizlive.model.enums.Role;

import java.sql.Timestamp;

/**
 * Represents a Student/Participant who takes live timed quizzes and views leaderboards.
 * Subclass of AppUser (OOP Inheritance).
 */
public class Participant extends AppUser {

    private static final long serialVersionUID = 1L;

    public Participant() {
        setRole(Role.PARTICIPANT);
    }

    public Participant(int id, String name, String email, String passwordHash, String salt, Timestamp createdAt) {
        super(id, name, email, passwordHash, salt, Role.PARTICIPANT, createdAt);
    }

    @Override
    public boolean canCreateQuiz() {
        return false;
    }

    @Override
    public boolean canApproveQuiz() {
        return false;
    }

    @Override
    public boolean canTakeQuiz() {
        return true;
    }

    @Override
    public String getDashboardUrl() {
        return "/participant/dashboard.jsp";
    }
}
