package com.quizlive.servlet;

import com.quizlive.model.LeaderboardEntry;
import com.quizlive.service.LeaderboardService;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "LeaderboardServlet", urlPatterns = {"/leaderboard/data", "/api/leaderboard"})
public class LeaderboardServlet extends HttpServlet {

    private final LeaderboardService leaderboardService;

    public LeaderboardServlet() {
        this(new LeaderboardService());
    }

    public LeaderboardServlet(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("quizId");
        if (idStr == null || idStr.trim().isEmpty()) {
            idStr = req.getParameter("id");
        }

        if (idStr == null || idStr.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "quizId parameter is required");
            return;
        }

        int quizId;
        try {
            quizId = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid quizId format");
            return;
        }

        try {
            List<LeaderboardEntry> leaderboard = leaderboardService.getLeaderboard(quizId);
            JsonUtil.sendSuccess(resp, leaderboard);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving leaderboard");
        }
    }
}
