package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.model.AppUser;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.service.QuizService;
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

@WebServlet(name = "CreateQuizServlet", urlPatterns = {"/quizzes/create", "/creator/quizzes/create"})
public class CreateQuizServlet extends HttpServlet {

    private final QuizService quizService;

    public CreateQuizServlet() {
        this(new QuizService());
    }

    public CreateQuizServlet(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        if (!user.canCreateQuiz() && user.getRole() != Role.CREATOR && user.getRole() != Role.ADMIN) {
            resp.sendRedirect(req.getContextPath() + user.getDashboardUrl());
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/creator/create-quiz.jsp");
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

        if (!user.canCreateQuiz() && user.getRole() != Role.CREATOR && user.getRole() != Role.ADMIN) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Only creators or admins can create quizzes");
            return;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String json = sb.toString().trim();
        if (json.isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body cannot be empty");
            return;
        }

        Quiz quiz;
        try {
            quiz = JsonUtil.fromJson(json, Quiz.class);
        } catch (JsonSyntaxException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request body");
            return;
        }

        if (quiz == null) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid quiz data");
            return;
        }

        quiz.setCreatorId(user.getId());
        quiz.setStatus(QuizStatus.PENDING);

        try {
            Quiz created = quizService.createQuiz(quiz);
            JsonUtil.sendCreated(resp, created);
        } catch (IllegalArgumentException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error creating quiz: " + e.getMessage());
        }
    }
}
