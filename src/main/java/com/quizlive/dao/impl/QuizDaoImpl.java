package com.quizlive.dao.impl;

import com.quizlive.dao.QuestionDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class QuizDaoImpl implements QuizDao {

    private final QuestionDao questionDao;

    private static final String SQL_INSERT_QUIZ =
            "INSERT INTO quizzes (title, description, creator_id, duration_seconds, status, is_held, scheduled_start_at, access_code, is_public) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.id = ?";

    private static final String SQL_FIND_BY_ACCESS_CODE =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE UPPER(q.access_code) = ?";

    private static final String SQL_LIST_ALL =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id ORDER BY q.id DESC";

    private static final String SQL_LIST_APPROVED =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.status = 'APPROVED' ORDER BY q.id DESC";

    private static final String SQL_LIST_APPROVED_PUBLIC =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.status = 'APPROVED' AND (q.is_public = TRUE OR q.is_public IS NULL) ORDER BY q.id DESC";

    private static final String SQL_LIST_BY_CREATOR =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.creator_id = ? ORDER BY q.id DESC";

    private static final String SQL_LIST_BY_STATUS =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.is_held, q.scheduled_start_at, q.access_code, q.is_public, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.status = ? ORDER BY q.id DESC";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE quizzes SET status = ? WHERE id = ?";

    private static final String SQL_UPDATE_HELD_STATUS =
            "UPDATE quizzes SET is_held = ? WHERE id = ?";

    private static final String SQL_UPDATE_SCHEDULED_START =
            "UPDATE quizzes SET scheduled_start_at = ? WHERE id = ?";

    private static final String SQL_UPDATE_VISIBILITY =
            "UPDATE quizzes SET is_public = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM quizzes WHERE id = ?";

    public QuizDaoImpl() {
        this.questionDao = new QuestionDaoImpl();
    }

    public QuizDaoImpl(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }

    @Override
    public Quiz createWithQuestions(Quiz quiz) throws SQLException {
        Connection conn = null;
        try {
            conn = DbConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_QUIZ, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, quiz.getTitle());
                stmt.setString(2, quiz.getDescription());
                stmt.setInt(3, quiz.getCreatorId());
                stmt.setInt(4, quiz.getDurationSeconds());
                stmt.setString(5, quiz.getStatus().name());
                stmt.setBoolean(6, quiz.isHeld());
                stmt.setTimestamp(7, quiz.getScheduledStartAt());
                if (quiz.getAccessCode() == null || quiz.getAccessCode().trim().isEmpty()) {
                    quiz.setAccessCode(Quiz.generateAccessCode());
                }
                stmt.setString(8, quiz.getAccessCode());
                stmt.setBoolean(9, quiz.isPublic());

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Creating quiz failed, no rows affected.");
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        quiz.setId(keys.getInt(1));
                    } else {
                        throw new SQLException("Creating quiz failed, no ID obtained.");
                    }
                }
            }

            if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                questionDao.createBatch(conn, quiz.getId(), quiz.getQuestions());
            }

            conn.commit();
            quiz.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            return quiz;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.addSuppressed(ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Ignored on close
                }
            }
        }
    }

    @Override
    public Quiz findById(int id) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToQuiz(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Quiz findByIdWithQuestions(int id) throws SQLException {
        Quiz quiz = findById(id);
        if (quiz != null) {
            quiz.setQuestions(questionDao.findByQuizId(id));
        }
        return quiz;
    }

    @Override
    public List<Quiz> listAll() throws SQLException {
        return queryList(SQL_LIST_ALL, null);
    }

    @Override
    public List<Quiz> listApproved() throws SQLException {
        return queryList(SQL_LIST_APPROVED, null);
    }

    @Override
    public List<Quiz> listApprovedPublic() throws SQLException {
        return queryList(SQL_LIST_APPROVED_PUBLIC, null);
    }

    @Override
    public Quiz findByAccessCode(String accessCode) throws SQLException {
        if (accessCode == null || accessCode.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ACCESS_CODE)) {
            stmt.setString(1, accessCode.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToQuiz(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Quiz> listByCreator(int creatorId) throws SQLException {
        return queryList(SQL_LIST_BY_CREATOR, stmt -> stmt.setInt(1, creatorId));
    }

    @Override
    public List<Quiz> listByStatus(QuizStatus status) throws SQLException {
        return queryList(SQL_LIST_BY_STATUS, stmt -> stmt.setString(1, status.name()));
    }

    @Override
    public boolean updateStatus(int quizId, QuizStatus status) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, quizId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateHeldStatus(int quizId, boolean isHeld) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_HELD_STATUS)) {
            stmt.setBoolean(1, isHeld);
            stmt.setInt(2, quizId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateScheduledStart(int quizId, Timestamp scheduledStartAt) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_SCHEDULED_START)) {
            stmt.setTimestamp(1, scheduledStartAt);
            stmt.setInt(2, quizId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateVisibility(int quizId, boolean isPublic) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_VISIBILITY)) {
            stmt.setBoolean(1, isPublic);
            stmt.setInt(2, quizId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @FunctionalInterface
    private interface ParameterSetter {
        void setParameters(PreparedStatement stmt) throws SQLException;
    }

    private List<Quiz> queryList(String sql, ParameterSetter setter) throws SQLException {
        List<Quiz> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.setParameters(stmt);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToQuiz(rs));
                }
            }
        }
        return list;
    }

    private Quiz mapRowToQuiz(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        int creatorId = rs.getInt("creator_id");
        int duration = rs.getInt("duration_seconds");
        QuizStatus status = QuizStatus.fromString(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");

        Quiz quiz = new Quiz(id, title, description, creatorId, duration, status, createdAt);
        quiz.setCreatorName(rs.getString("creator_name"));
        try {
            quiz.setHeld(rs.getBoolean("is_held"));
        } catch (SQLException ignored) {
        }
        try {
            quiz.setScheduledStartAt(rs.getTimestamp("scheduled_start_at"));
        } catch (SQLException ignored) {
        }
        try {
            String code = rs.getString("access_code");
            if (code != null && !code.trim().isEmpty()) {
                quiz.setAccessCode(code.trim().toUpperCase());
            }
        } catch (SQLException ignored) {
        }
        try {
            boolean isPub = rs.getBoolean("is_public");
            quiz.setPublic(rs.wasNull() || isPub);
        } catch (SQLException ignored) {
        }
        return quiz;
    }
}
