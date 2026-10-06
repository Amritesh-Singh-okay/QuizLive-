package com.quizlive.servlet;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.enums.Role;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "AttemptServlet", urlPatterns = {"/attempts", "/api/attempts"})
public class AttemptServlet extends HttpServlet {

    private final AttemptDao attemptDao;

    public AttemptServlet() {
        this(new AttemptDaoImpl());
    }

    public AttemptServlet(AttemptDao attemptDao) {
        this.attemptDao = attemptDao;
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

        String quizIdParam = req.getParameter("quizId");
        try {
            if (quizIdParam != null && !quizIdParam.trim().isEmpty()) {
                if (user.getRole() != Role.ADMIN && user.getRole() != Role.CREATOR) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only creators or admins can view all submissions for a quiz");
                    return;
                }
                int quizId = Integer.parseInt(quizIdParam.trim());
                List<Attempt> attempts = attemptDao.listByQuiz(quizId);
                JsonUtil.sendSuccess(resp, attempts);
            } else {
                List<Attempt> attempts = attemptDao.listByUser(user.getId());
                JsonUtil.sendSuccess(resp, attempts);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid quizId format");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving attempts");
        }
    }
}
