package com.quizlive.dao.impl;

import com.quizlive.dao.SettingsDao;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDaoImpl implements SettingsDao {

    private static final String SQL_GET =
            "SELECT setting_value FROM system_settings WHERE setting_key = ?";

    private static final String SQL_UPSERT =
            "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";

    private static final String SQL_GET_ALL =
            "SELECT setting_key, setting_value FROM system_settings ORDER BY setting_key ASC";

    @Override
    public String getSetting(String key, String defaultValue) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        }
        return defaultValue;
    }

    @Override
    public boolean setSetting(String key, String value) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPSERT)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public Map<String, String> getAllSettings() throws SQLException {
        Map<String, String> map = new LinkedHashMap<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return map;
    }
}
