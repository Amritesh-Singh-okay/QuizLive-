package com.quizlive.service;

import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.exception.UnauthorizedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.util.PasswordUtil;

import java.sql.SQLException;

public class AuthService {

    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDaoImpl();
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public AppUser register(String name, String email, String password, Role role) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userDao.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        AppUser newUser = AppUser.create(0, name.trim(), normalizedEmail, passwordHash, salt, role, null);
        return userDao.create(newUser);
    }

    public AppUser login(String email, String password) throws SQLException, UnauthorizedException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new UnauthorizedException("Email and password are required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        AppUser user = userDao.findByEmail(normalizedEmail);

        if (user == null) {
            throw new UnauthorizedException("Invalid email or password");
        }

        boolean valid = PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getSalt());
        if (!valid) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return user;
    }
}
