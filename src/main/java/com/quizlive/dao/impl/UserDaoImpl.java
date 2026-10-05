package com.quizlive.dao.impl;

import com.quizlive.dao.UserDao;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, email, password_hash, salt, role, created_at FROM users WHERE id = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT id, name, email, password_hash, salt, role, created_at FROM users WHERE email = ?";

    private static final String SQL_INSERT =
            "INSERT INTO users (name, email, password_hash, salt, role) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE users SET name = ?, email = ?, password_hash = ?, salt = ?, role = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM users WHERE id = ?";

    private static final String SQL_LIST_ALL =
            "SELECT id, name, email, password_hash, salt, role, created_at FROM users ORDER BY id ASC";

    private static final String SQL_LIST_BY_ROLE =
            "SELECT id, name, email, password_hash, salt, role, created_at FROM users WHERE role = ? ORDER BY id ASC";

    @Override
    public AppUser findById(int id) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public AppUser findByEmail(String email) throws SQLException {
        if (email == null) {
            return null;
        }
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public AppUser create(AppUser user) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getSalt());
            stmt.setString(5, user.getRole().name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    user.setId(newId);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            return user;
        }
    }

    @Override
    public boolean update(AppUser user) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getSalt());
            stmt.setString(5, user.getRole().name());
            stmt.setInt(6, user.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<AppUser> listAll() throws SQLException {
        List<AppUser> users = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    @Override
    public List<AppUser> listByRole(Role role) throws SQLException {
        List<AppUser> users = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_ROLE)) {
            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }
        return users;
    }

    private AppUser mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String salt = rs.getString("salt");
        Role role = Role.fromString(rs.getString("role"));
        Timestamp createdAt = rs.getTimestamp("created_at");

        return AppUser.create(id, name, email, passwordHash, salt, role, createdAt);
    }
}
