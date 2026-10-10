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
                Quiz existingQuiz = quizService.findById(quizId);
                if (existingQuiz == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
                    return;
                }
                boolean canViewAnswers = (user != null && (user.getRole() == Role.ADMIN || (user.getRole() == Role.CREATOR && existingQuiz.getCreatorId() == user.getId())));
                if (canViewAnswers) {
                    Quiz quiz = quizService.getQuizWithAnswers(quizId);
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

        String codeParam = req.getParameter("code");
        if (codeParam != null && !codeParam.trim().isEmpty()) {
            try {
                Quiz quiz = quizService.findByAccessCode(codeParam.trim());
                if (quiz == null) {
                    try {
                        int potentialId = Integer.parseInt(codeParam.trim());
                        quiz = quizService.findById(potentialId);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (quiz == null) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "No quiz found matching code: " + codeParam.trim());
                    return;
                }

                if (quiz.getStatus() != com.quizlive.model.enums.QuizStatus.APPROVED && (user == null || (user.getRole() != Role.ADMIN && user.getId() != quiz.getCreatorId()))) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Quiz has not been approved yet");
                    return;
                }

                boolean canViewAnswers = (user != null && (user.getRole() == Role.ADMIN || (user.getRole() == Role.CREATOR && quiz.getCreatorId() == user.getId())));
                if (canViewAnswers) {
                    Quiz fullQuiz = quizService.getQuizWithAnswers(quiz.getId());
                    JsonUtil.sendSuccess(resp, fullQuiz != null ? fullQuiz : quiz);
                } else {
                    Quiz takingQuiz = quizService.getQuizForTaking(quiz.getId());
                    JsonUtil.sendSuccess(resp, takingQuiz);
                }
            } catch (QuizClosedException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } catch (SQLException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error retrieving quiz: " + e.getMessage());
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
            } else if ("all".equalsIgnoreCase(filter) || "all".equalsIgnoreCase(status)) {
                if (user == null || user.getRole() != Role.ADMIN) {
                    JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Admin privileges required to view all quizzes");
                    return;
                }
                quizzes = quizService.getApprovedQuizzes();
            } else {
                quizzes = quizService.getApprovedPublicQuizzes();
            }

            JsonUtil.sendSuccess(resp, quizzes);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid creatorId format");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading quizzes");
        }
    }
}
