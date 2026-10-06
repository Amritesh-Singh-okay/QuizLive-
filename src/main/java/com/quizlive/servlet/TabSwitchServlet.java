package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.concurrency.ActiveAttemptRegistry;
import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
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

@WebServlet(name = "TabSwitchServlet", urlPatterns = {"/attempts/tab-switch", "/participant/attempts/tab-switch"})
public class TabSwitchServlet extends HttpServlet {

    private final ActiveAttemptRegistry registry;
    private final AttemptDao attemptDao;

    public TabSwitchServlet() {
        this(ActiveAttemptRegistry.getInstance(), new AttemptDaoImpl());
    }

    public TabSwitchServlet(ActiveAttemptRegistry registry, AttemptDao attemptDao) {
        this.registry = registry;
        this.attemptDao = attemptDao;
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

        int attemptId = parseAttemptId(req);
        if (attemptId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid attemptId is required");
            return;
        }

        try {
            int inMemoryCount = registry.incrementTabSwitches(attemptId);
            attemptDao.incrementTabSwitches(attemptId);

            Attempt attempt = attemptDao.findById(attemptId);
            int finalCount = (attempt != null) ? attempt.getTabSwitches() : (inMemoryCount > 0 ? inMemoryCount : 1);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("attemptId", attemptId);
            data.put("tabSwitches", finalCount);
            if (finalCount >= 3) {
                data.put("warning", "Excessive tab switching detected: proctor flagged");
            }

            JsonUtil.sendSuccess(resp, data);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error recording tab switch: " + e.getMessage());
        }
    }

    private int parseAttemptId(HttpServletRequest req) throws IOException {
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
                        if (map.get("attemptId") != null) {
                            return ((Number) map.get("attemptId")).intValue();
                        } else if (map.get("id") != null) {
                            return ((Number) map.get("id")).intValue();
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        String idParam = req.getParameter("attemptId");
        if (idParam == null || idParam.isEmpty()) {
            idParam = req.getParameter("id");
        }
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                return Integer.parseInt(idParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return 0;
    }
}
