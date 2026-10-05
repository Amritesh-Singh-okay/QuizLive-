package com.quizlive.dao.impl;

import com.quizlive.dao.MessageDao;
import com.quizlive.model.Message;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class MessageDaoImpl implements MessageDao {

    private static final String SQL_INSERT =
            "INSERT INTO messages (from_user_id, to_user_id, quiz_id, content, sent_at) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_GET_THREAD =
            "SELECT m.id, m.from_user_id, m.to_user_id, m.quiz_id, m.content, m.sent_at, " +
            "u1.name AS from_name, u2.name AS to_name, q.title AS quiz_title " +
            "FROM messages m " +
            "JOIN users u1 ON m.from_user_id = u1.id " +
            "JOIN users u2 ON m.to_user_id = u2.id " +
            "LEFT JOIN quizzes q ON m.quiz_id = q.id " +
            "WHERE (m.from_user_id = ? AND m.to_user_id = ?) OR (m.from_user_id = ? AND m.to_user_id = ?) " +
            "ORDER BY m.sent_at ASC, m.id ASC";

    private static final String SQL_LIST_BY_QUIZ =
            "SELECT m.id, m.from_user_id, m.to_user_id, m.quiz_id, m.content, m.sent_at, " +
            "u1.name AS from_name, u2.name AS to_name, q.title AS quiz_title " +
            "FROM messages m " +
            "JOIN users u1 ON m.from_user_id = u1.id " +
            "JOIN users u2 ON m.to_user_id = u2.id " +
            "LEFT JOIN quizzes q ON m.quiz_id = q.id " +
            "WHERE m.quiz_id = ? ORDER BY m.sent_at ASC";

    private static final String SQL_LIST_BY_USER =
            "SELECT m.id, m.from_user_id, m.to_user_id, m.quiz_id, m.content, m.sent_at, " +
            "u1.name AS from_name, u2.name AS to_name, q.title AS quiz_title " +
            "FROM messages m " +
            "JOIN users u1 ON m.from_user_id = u1.id " +
            "JOIN users u2 ON m.to_user_id = u2.id " +
            "LEFT JOIN quizzes q ON m.quiz_id = q.id " +
            "WHERE m.from_user_id = ? OR m.to_user_id = ? ORDER BY m.sent_at DESC";

    @Override
    public Message send(Message message) throws SQLException {
        Timestamp sentAt = message.getSentAt() != null ? message.getSentAt() : new Timestamp(System.currentTimeMillis());
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, message.getFromUserId());
            stmt.setInt(2, message.getToUserId());
            if (message.getQuizId() != null) {
                stmt.setInt(3, message.getQuizId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, message.getContent());
            stmt.setTimestamp(5, sentAt);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Sending message failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setId(keys.getInt(1));
                } else {
                    throw new SQLException("Sending message failed, no ID obtained.");
                }
            }

            message.setSentAt(sentAt);
            return message;
        }
    }

    @Override
    public List<Message> getThread(int userA, int userB) throws SQLException {
        List<Message> thread = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_THREAD)) {
            stmt.setInt(1, userA);
            stmt.setInt(2, userB);
            stmt.setInt(3, userB);
            stmt.setInt(4, userA);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    thread.add(mapRowToMessage(rs));
                }
            }
        }
        return thread;
    }

    @Override
    public List<Message> listByQuiz(int quizId) throws SQLException {
        List<Message> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_QUIZ)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Message> listByUser(int userId) throws SQLException {
        List<Message> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_USER)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
        }
        return list;
    }

    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int fromUserId = rs.getInt("from_user_id");
        int toUserId = rs.getInt("to_user_id");
        int qId = rs.getInt("quiz_id");
        Integer quizId = rs.wasNull() ? null : qId;
        String content = rs.getString("content");
        Timestamp sentAt = rs.getTimestamp("sent_at");

        Message msg = new Message(id, fromUserId, toUserId, quizId, content, sentAt);
        msg.setFromUserName(rs.getString("from_name"));
        msg.setToUserName(rs.getString("to_name"));
        msg.setQuizTitle(rs.getString("quiz_title"));
        return msg;
    }
}
