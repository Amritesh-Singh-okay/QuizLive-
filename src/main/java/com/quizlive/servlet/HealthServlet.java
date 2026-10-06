package com.quizlive.servlet;

import com.quizlive.util.DbConnectionUtil;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "HealthServlet", urlPatterns = {"/health", "/api/health"})
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        boolean dbConnected = false;
        try (Connection conn = DbConnectionUtil.getConnection()) {
            dbConnected = (conn != null && !conn.isClosed());
        } catch (Exception ignored) {
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", dbConnected ? "UP" : "DEGRADED");
        status.put("database", dbConnected ? "CONNECTED" : "DISCONNECTED");
        status.put("timestamp", System.currentTimeMillis());

        JsonUtil.sendSuccess(resp, status);
    }
}
