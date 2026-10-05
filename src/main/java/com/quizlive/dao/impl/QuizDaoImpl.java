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
            "INSERT INTO quizzes (title, description, creator_id, duration_seconds, status) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.id = ?";

    private static final String SQL_LIST_ALL =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id ORDER BY q.id DESC";

    private static final String SQL_LIST_APPROVED =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.status = 'APPROVED' ORDER BY q.id DESC";

    private static final String SQL_LIST_BY_CREATOR =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.creator_id = ? ORDER BY q.id DESC";

    private static final String SQL_LIST_BY_STATUS =
            "SELECT q.id, q.title, q.description, q.creator_id, q.duration_seconds, q.status, q.created_at, u.name AS creator_name " +
            "FROM quizzes q JOIN users u ON q.creator_id = u.id WHERE q.status = ? ORDER BY q.id DESC";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE quizzes SET status = ? WHERE id = ?";

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
        return quiz;
    }
}
