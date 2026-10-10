package com.quizlive.model;

import com.quizlive.model.enums.Role;

import java.sql.Timestamp;

/**
 * Represents a Platform Administrator with full system governance capabilities.
 * Subclass of AppUser (OOP Inheritance).
 */
public class Admin extends AppUser {

    private static final long serialVersionUID = 1L;

    public Admin() {
        setRole(Role.ADMIN);
    }

    public Admin(int id, String name, String email, String passwordHash, String salt, Timestamp createdAt) {
        super(id, name, email, passwordHash, salt, Role.ADMIN, "VERIFIED", createdAt);
    }

    public Admin(int id, String name, String email, String passwordHash, String salt, String rank, Timestamp createdAt) {
        super(id, name, email, passwordHash, salt, Role.ADMIN, rank, createdAt);
    }

    @Override
    public boolean canCreateQuiz() {
        return false;
    }

    @Override
    public boolean canApproveQuiz() {
        return true;
    }

    @Override
    public boolean canPublishDirectly() {
        return true;
    }

    @Override
    public boolean canTakeQuiz() {
        return false;
    }

    @Override
    public String getDashboardUrl() {
        return "/admin/dashboard.jsp";
    }
}
