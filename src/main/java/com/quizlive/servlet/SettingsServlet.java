package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.dao.SettingsDao;
import com.quizlive.dao.impl.SettingsDaoImpl;
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
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings", "/admin/settings", "/api/admin/settings"})
public class SettingsServlet extends HttpServlet {

    private final SettingsDao settingsDao;

    public SettingsServlet() {
        this(new SettingsDaoImpl());
    }

    public SettingsServlet(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String key = req.getParameter("key");

        try {
            if (key != null && !key.trim().isEmpty()) {
                String value = settingsDao.getSetting(key.trim(), "");
                Map<String, String> result = new LinkedHashMap<>();
                result.put("key", key.trim());
                result.put("value", value);
                JsonUtil.sendSuccess(resp, result);
            } else {
                Map<String, String> all = settingsDao.getAllSettings();
                JsonUtil.sendSuccess(resp, all);
            }
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error reading settings: " + e.getMessage());
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
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only administrators can update platform settings");
            return;
        }

        Map<String, String> updates = parseUpdates(req);
        if (updates.isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No settings provided for update");
            return;
        }

        try {
            for (Map.Entry<String, String> entry : updates.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    settingsDao.setSetting(entry.getKey().trim(), entry.getValue().trim());
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "Settings updated successfully");
            data.put("settings", settingsDao.getAllSettings());
            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error updating settings: " + e.getMessage());
        }
    }

    private Map<String, String> parseUpdates(HttpServletRequest req) throws IOException {
        Map<String, String> updates = new LinkedHashMap<>();
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
                    Map<?, ?> map = JsonUtil.fromJson(json, Map.class);
                    if (map != null) {
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                updates.put(entry.getKey().toString(), entry.getValue().toString());
                            }
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        req.getParameterMap().forEach((k, v) -> {
            if (v != null && v.length > 0 && !updates.containsKey(k)) {
                updates.put(k, v[0]);
            }
        });

        return updates;
    }
}
