package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.enums.Role;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "UserManagementServlet", urlPatterns = {"/admin/users", "/api/admin/users", "/admin/users/delete", "/api/admin/users/delete", "/admin/users/update-role", "/api/admin/users/update-role", "/admin/users/update-rank", "/api/admin/users/update-rank"})
public class UserManagementServlet extends HttpServlet {

    private final UserDao userDao;

    public UserManagementServlet() {
        this(new UserDaoImpl());
    }

    public UserManagementServlet(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        if (user.getRole() != Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Administrator privileges required");
            return;
        }

        String roleParam = req.getParameter("role");
        try {
            List<AppUser> users;
            if (roleParam != null && !roleParam.trim().isEmpty()) {
                Role role = Role.fromString(roleParam.trim());
                users = (role != null) ? userDao.listByRole(role) : userDao.listAll();
            } else {
                users = userDao.listAll();
            }

            List<Map<String, Object>> sanitizedList = new ArrayList<>();
            for (AppUser u : users) {
                sanitizedList.add(sanitizeUser(u));
            }

            JsonUtil.sendSuccess(resp, sanitizedList);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading users: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        if (user.getRole() != Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Administrator privileges required");
            return;
        }

        Map<String, String> params = parseParameters(req);
        String action = params.get("action");
        String path = req.getRequestURI();

        if (path.endsWith("/delete") || "delete".equalsIgnoreCase(action)) {
            handleDeleteUser(req, resp, user, params);
        } else if (path.endsWith("/update-role") || "update-role".equalsIgnoreCase(action)) {
            handleUpdateRole(req, resp, params);
        } else if (path.endsWith("/update-rank") || "update-rank".equalsIgnoreCase(action) || "approve-rank".equalsIgnoreCase(action)) {
            handleUpdateRank(req, resp, params);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid admin user operation");
        }
    }

    private void handleDeleteUser(HttpServletRequest req, HttpServletResponse resp, AppUser currentUser, Map<String, String> params)
            throws IOException {
        int targetId = parseTargetId(params);
        if (targetId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid userId is required");
            return;
        }

        if (targetId == currentUser.getId()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot delete currently logged-in administrator account");
            return;
        }

        try {
            boolean deleted = userDao.delete(targetId);
            if (!deleted) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "User deleted successfully");
            data.put("userId", targetId);
            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error deleting user: " + e.getMessage());
        }
    }

    private void handleUpdateRole(HttpServletRequest req, HttpServletResponse resp, Map<String, String> params)
            throws IOException {
        int targetId = parseTargetId(params);
        String roleStr = params.get("role");

        if (targetId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid userId is required");
            return;
        }

        Role newRole = Role.fromString(roleStr);
        if (newRole == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid role is required (ADMIN, CREATOR, PARTICIPANT)");
            return;
        }

        try {
            AppUser target = userDao.findById(targetId);
            if (target == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            target.setRole(newRole);
            boolean updated = userDao.update(target);
            if (!updated) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update user role");
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "User role updated successfully");
            data.put("userId", targetId);
            data.put("role", newRole.name());
            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating user: " + e.getMessage());
        }
    }

    private void handleUpdateRank(HttpServletRequest req, HttpServletResponse resp, Map<String, String> params)
            throws IOException {
        int targetId = parseTargetId(params);
        String rankStr = params.get("rank");
        if (rankStr == null || rankStr.trim().isEmpty()) {
            rankStr = "VERIFIED";
        }

        if (targetId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid userId is required");
            return;
        }

        try {
            AppUser target = userDao.findById(targetId);
            if (target == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            String normalizedRank = rankStr.trim().toUpperCase();
            target.setRank(normalizedRank);
            boolean updated = userDao.update(target);
            if (!updated) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update user rank");
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "User rank updated successfully");
            data.put("userId", targetId);
            data.put("rank", normalizedRank);
            data.put("canPublishDirectly", target.canPublishDirectly());
            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating user rank: " + e.getMessage());
        }
    }

    private int parseTargetId(Map<String, String> params) {
        String idStr = params.get("userId");
        if (idStr == null || idStr.isEmpty()) {
            idStr = params.get("id");
        }
        if (idStr != null) {
            try {
                return (int) Double.parseDouble(idStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private Map<String, String> parseParameters(HttpServletRequest req) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String json = sb.toString().trim();
            if (!json.isEmpty()) {
                try {
                    Map<?, ?> jsonMap = JsonUtil.fromJson(json, Map.class);
                    if (jsonMap != null) {
                        for (Map.Entry<?, ?> entry : jsonMap.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                params.put(entry.getKey().toString(), entry.getValue().toString());
                            }
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        req.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0 && !params.containsKey(k)) {
                params.put(k, v[0]);
            }
        });

        return params;
    }

    private Map<String, Object> sanitizeUser(AppUser u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("role", u.getRole().name());
        map.put("rank", u.getRank());
        map.put("canPublishDirectly", u.canPublishDirectly());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }
}
