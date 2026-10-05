package com.quizlive.model;

import com.quizlive.model.enums.Role;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Abstract base class representing any authenticated user in QuizLive.
 * Demonstrates OOP Inheritance and Polymorphism (Rubric Milestone).
 */
public abstract class AppUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String salt;
    private Role role;
    private Timestamp createdAt;

    /**
     * Default constructor.
     */
    protected AppUser() {
    }

    /**
     * Parameterized constructor.
     */
    protected AppUser(int id, String name, String email, String passwordHash, String salt, Role role, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role;
        this.createdAt = createdAt;
    }

    // ==========================================
    // Abstract Polymorphic Methods
    // ==========================================

    /**
     * Checks if this user has permission to create quizzes.
     *
     * @return true if permitted, false otherwise
     */
    public abstract boolean canCreateQuiz();

    /**
     * Checks if this user has permission to approve/reject quizzes.
     *
     * @return true if permitted, false otherwise
     */
    public abstract boolean canApproveQuiz();

    /**
     * Checks if this user has permission to attempt quizzes.
     *
     * @return true if permitted, false otherwise
     */
    public abstract boolean canTakeQuiz();

    /**
     * Returns the relative URL for this user's default dashboard.
     *
     * @return dashboard URL path
     */
    public abstract String getDashboardUrl();

    // ==========================================
    // Factory Method
    // ==========================================

    /**
     * Polymorphic Factory Method: Instantiates the appropriate AppUser subclass
     * based on the specified security role.
     */
    public static AppUser create(int id, String name, String email, String passwordHash, String salt, Role role, Timestamp createdAt) {
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }
        return switch (role) {
            case ADMIN -> new Admin(id, name, email, passwordHash, salt, createdAt);
            case CREATOR -> new QuizCreator(id, name, email, passwordHash, salt, createdAt);
            case PARTICIPANT -> new Participant(id, name, email, passwordHash, salt, createdAt);
        };
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
