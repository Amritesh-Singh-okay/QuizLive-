package com.quizlive.dao.impl;

import com.quizlive.dao.AttemptAnswerDao;
import com.quizlive.dao.AttemptDao;
import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.model.enums.AttemptStatus;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AttemptDaoImpl implements AttemptDao {

    private final AttemptAnswerDao attemptAnswerDao;

    private static final String SQL_START_ATTEMPT =
            "INSERT INTO attempts (quiz_id, user_id, started_at, status) VALUES (?, ?, ?, 'IN_PROGRESS')";

    private static final String SQL_FIND_BY_ID =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id WHERE a.id = ?";

    private static final String SQL_FIND_BY_QUIZ_AND_USER =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id WHERE a.quiz_id = ? AND a.user_id = ?";

    private static final String SQL_SUBMIT_ATTEMPT =
            "UPDATE attempts SET submitted_at = ?, score = ?, tab_switches = ?, status = ? WHERE id = ?";

    private static final String SQL_INCREMENT_TAB_SWITCH =
            "UPDATE attempts SET tab_switches = tab_switches + 1 WHERE id = ? AND status = 'IN_PROGRESS'";

    private static final String SQL_GET_LEADERBOARD =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id " +
            "WHERE a.quiz_id = ? AND a.status IN ('SUBMITTED', 'AUTO_SUBMITTED') " +
            "ORDER BY a.score DESC, TIMESTAMPDIFF(SECOND, a.started_at, a.submitted_at) ASC";

    private static final String SQL_LIST_BY_USER =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id " +
            "WHERE a.user_id = ? ORDER BY a.started_at DESC";

    private static final String SQL_LIST_BY_QUIZ =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id " +
            "WHERE a.quiz_id = ? ORDER BY a.id DESC";

    private static final String SQL_LIST_BY_CREATOR =
            "SELECT a.id, a.quiz_id, a.user_id, a.started_at, a.submitted_at, a.score, a.tab_switches, a.status, " +
            "u.name AS user_name, q.title AS quiz_title, " +
            "COALESCE((SELECT SUM(points) FROM questions WHERE quiz_id = a.quiz_id), 0) AS max_score " +
            "FROM attempts a JOIN users u ON a.user_id = u.id JOIN quizzes q ON a.quiz_id = q.id " +
            "WHERE q.creator_id = ? ORDER BY a.id DESC";

    public AttemptDaoImpl() {
        this.attemptAnswerDao = new AttemptAnswerDaoImpl();
    }

    public AttemptDaoImpl(AttemptAnswerDao attemptAnswerDao) {
        this.attemptAnswerDao = attemptAnswerDao;
    }

    @Override
    public Attempt startAttempt(int quizId, int userId) throws SQLException {
        Timestamp startedAt = new Timestamp(System.currentTimeMillis());
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_START_ATTEMPT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, startedAt);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Starting attempt failed, no rows affected.");
            }

            int newId;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    newId = keys.getInt(1);
                } else {
                    throw new SQLException("Starting attempt failed, no ID obtained.");
                }
            }

            Attempt attempt = new Attempt();
            attempt.setId(newId);
            attempt.setQuizId(quizId);
            attempt.setUserId(userId);
            attempt.setStartedAt(startedAt);
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
            return attempt;
        }
    }

    @Override
    public Attempt findById(int attemptId) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAttempt(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Attempt findByQuizAndUser(int quizId, int userId) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_QUIZ_AND_USER)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAttempt(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean submitAttempt(Attempt attempt, List<AttemptAnswer> answers) throws SQLException {
        Connection conn = null;
        try {
            conn = DbConnectionUtil.getConnection();
            conn.setAutoCommit(false);

            Timestamp submittedAt = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : new Timestamp(System.currentTimeMillis());
            attempt.setSubmittedAt(submittedAt);

            try (PreparedStatement stmt = conn.prepareStatement(SQL_SUBMIT_ATTEMPT)) {
                stmt.setTimestamp(1, submittedAt);
                stmt.setInt(2, attempt.getScore());
                stmt.setInt(3, attempt.getTabSwitches());
                stmt.setString(4, attempt.getStatus().name());
                stmt.setInt(5, attempt.getId());

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    throw new SQLException("Submitting attempt failed, attempt record not found.");
                }
            }

            if (answers != null && !answers.isEmpty()) {
                attemptAnswerDao.saveBatch(conn, attempt.getId(), answers);
            }

            conn.commit();
            return true;
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
                }
            }
        }
    }

    @Override
    public boolean incrementTabSwitches(int attemptId) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INCREMENT_TAB_SWITCH)) {
            stmt.setInt(1, attemptId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Attempt> getLeaderboard(int quizId) throws SQLException {
        List<Attempt> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_LEADERBOARD)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Attempt> listByUser(int userId) throws SQLException {
        List<Attempt> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_USER)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Attempt> listByQuiz(int quizId) throws SQLException {
        List<Attempt> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_QUIZ)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Attempt> listByCreator(int creatorId) throws SQLException {
        List<Attempt> list = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_LIST_BY_CREATOR)) {
            stmt.setInt(1, creatorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttempt(rs));
                }
            }
        }
        return list;
    }

    private Attempt mapRowToAttempt(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int quizId = rs.getInt("quiz_id");
        int userId = rs.getInt("user_id");
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        int score = rs.getInt("score");
        int maxScore = 0;
        try {
            maxScore = rs.getInt("max_score");
        } catch (SQLException ignored) {
        }
        int tabSwitches = rs.getInt("tab_switches");
        AttemptStatus status = AttemptStatus.fromString(rs.getString("status"));

        Attempt attempt = new Attempt(id, quizId, userId, startedAt, submittedAt, score, maxScore, tabSwitches, status);
        attempt.setUserName(rs.getString("user_name"));
        attempt.setQuizTitle(rs.getString("quiz_title"));
        return attempt;
    }
}
