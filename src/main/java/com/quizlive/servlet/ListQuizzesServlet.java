package com.quizlive.servlet;

import com.quizlive.exception.QuizClosedException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
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

@WebServlet(name = "ListQuizzesServlet", urlPatterns = {"/quizzes", "/api/quizzes"})
public class ListQuizzesServlet extends HttpServlet {

    private final QuizService quizService;

    public ListQuizzesServlet() {
        this(new QuizService());
    }

    public ListQuizzesServlet(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                int quizId = Integer.parseInt(idParam.trim());
                if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.CREATOR)) {
                    Quiz quiz = quizService.getQuizWithAnswers(quizId);
                    if (quiz == null) {
                        JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
                        return;
                    }
                    JsonUtil.sendSuccess(resp, quiz);
                } else {
                    Quiz quiz = quizService.getQuizForTaking(quizId);
                    JsonUtil.sendSuccess(resp, quiz);
                }
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid quiz ID format");
            } catch (QuizClosedException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } catch (SQLException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving quiz");
            }
            return;
        }

        String filter = req.getParameter("filter");
        String status = req.getParameter("status");
        String creatorIdParam = req.getParameter("creatorId");

        try {
            List<Quiz> quizzes;
            if ("pending".equalsIgnoreCase(filter) || "PENDING".equalsIgnoreCase(status)) {
                if (user == null || user.getRole() != Role.ADMIN) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required to view pending quizzes");
                    return;
                }
                quizzes = quizService.getPendingQuizzes();
            } else if ("my".equalsIgnoreCase(filter)) {
                if (user == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                    return;
                }
                quizzes = quizService.getQuizzesByCreator(user.getId());
            } else if (creatorIdParam != null && !creatorIdParam.trim().isEmpty()) {
                int creatorId = Integer.parseInt(creatorIdParam.trim());
                quizzes = quizService.getQuizzesByCreator(creatorId);
            } else {
                quizzes = quizService.getApprovedQuizzes();
            }

            JsonUtil.sendSuccess(resp, quizzes);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid creatorId format");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading quizzes");
        }
    }
}
