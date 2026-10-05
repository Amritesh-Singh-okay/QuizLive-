package com.quizlive.dao;

import java.sql.SQLException;
import java.util.Map;

public interface SettingsDao {

    String getSetting(String key, String defaultValue) throws SQLException;

    boolean setSetting(String key, String value) throws SQLException;

    Map<String, String> getAllSettings() throws SQLException;
}
