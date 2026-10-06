package com.quizlive.servlet;

import com.google.gson.JsonSyntaxException;
import com.quizlive.dao.MessageDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.MessageDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Message;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@WebServlet(name = "MessageServlet", urlPatterns = {"/messages", "/api/messages", "/messages/thread"})
public class MessageServlet extends HttpServlet {

    private final MessageDao messageDao;
    private final UserDao userDao;

    public MessageServlet() {
        this(new MessageDaoImpl(), new UserDaoImpl());
    }

    public MessageServlet(MessageDao messageDao, UserDao userDao) {
        this.messageDao = messageDao;
        this.userDao = userDao;
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

        String withUserParam = req.getParameter("withUser");
        if (withUserParam == null || withUserParam.isEmpty()) {
            withUserParam = req.getParameter("userId");
        }

        String quizIdParam = req.getParameter("quizId");

        try {
            List<Message> messages;
            if (withUserParam != null && !withUserParam.trim().isEmpty()) {
                int partnerId = Integer.parseInt(withUserParam.trim());
                messages = messageDao.getThread(user.getId(), partnerId);
            } else if (quizIdParam != null && !quizIdParam.trim().isEmpty()) {
                int quizId = Integer.parseInt(quizIdParam.trim());
                messages = messageDao.listByQuiz(quizId);
            } else {
                messages = messageDao.listByUser(user.getId());
            }

            JsonUtil.sendSuccess(resp, messages);
        } catch (NumberFormatException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid user or quiz ID format");
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error loading messages: " + e.getMessage());
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

        MessagePayload payload = parsePayload(req);
        if (payload == null || payload.toUserId <= 0) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Valid recipient (toUserId) is required");
            return;
        }

        if (payload.content == null || payload.content.trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Message content cannot be empty");
            return;
        }

        if (payload.toUserId == user.getId()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cannot send message to yourself");
            return;
        }

        try {
            AppUser recipient = userDao.findById(payload.toUserId);
            if (recipient == null) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Recipient user not found");
                return;
            }

            Message msg = new Message(
                    0,
                    user.getId(),
                    payload.toUserId,
                    payload.quizId != null && payload.quizId > 0 ? payload.quizId : null,
                    payload.content.trim(),
                    new Timestamp(System.currentTimeMillis())
            );

            Message sent = messageDao.send(msg);
            JsonUtil.sendCreated(resp, sent);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error sending message: " + e.getMessage());
        }
    }

    private MessagePayload parsePayload(HttpServletRequest req) throws IOException {
        MessagePayload payload = new MessagePayload();
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
                        if (map.get("toUserId") != null) {
                            payload.toUserId = ((Number) map.get("toUserId")).intValue();
                        }
                        if (map.get("content") != null) {
                            payload.content = map.get("content").toString();
                        }
                        if (map.get("quizId") != null) {
                            payload.quizId = ((Number) map.get("quizId")).intValue();
                        }
                    }
                } catch (JsonSyntaxException ignored) {
                }
            }
        }

        if (payload.toUserId <= 0) {
            String toParam = req.getParameter("toUserId");
            if (toParam != null && !toParam.trim().isEmpty()) {
                try {
                    payload.toUserId = Integer.parseInt(toParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (payload.content == null) {
            payload.content = req.getParameter("content");
        }

        if (payload.quizId == null) {
            String qParam = req.getParameter("quizId");
            if (qParam != null && !qParam.trim().isEmpty()) {
                try {
                    payload.quizId = Integer.parseInt(qParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return payload;
    }

    private static class MessagePayload {
        int toUserId = 0;
        String content;
        Integer quizId;
    }
}
