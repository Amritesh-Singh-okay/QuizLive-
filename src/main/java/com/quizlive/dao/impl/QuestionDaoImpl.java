package com.quizlive.dao.impl;

import com.quizlive.dao.QuestionDao;
import com.quizlive.model.Question;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuestionDaoImpl implements QuestionDao {

    private static final String SQL_FIND_BY_ID =
            "SELECT id, quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, points FROM questions WHERE id = ?";

    private static final String SQL_FIND_BY_QUIZ_ID =
            "SELECT id, quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, points FROM questions WHERE quiz_id = ? ORDER BY id ASC";

    private static final String SQL_INSERT =
            "INSERT INTO questions (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_option, points) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_DELETE =
            "DELETE FROM questions WHERE id = ?";

    @Override
    public Question findById(int id) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToQuestion(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Question> findByQuizId(int quizId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_QUIZ_ID)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRowToQuestion(rs));
                }
            }
        }
        return questions;
    }

    @Override
    public List<Question> findByQuizIdSanitized(int quizId) throws SQLException {
        List<Question> raw = findByQuizId(quizId);
        List<Question> sanitized = new ArrayList<>(raw.size());
        for (Question q : raw) {
            sanitized.add(q.sanitized());
        }
        return sanitized;
    }

    @Override
    public void createBatch(Connection conn, int quizId, List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            for (Question q : questions) {
                stmt.setInt(1, quizId);
                stmt.setString(2, q.getQuestionText());
                stmt.setString(3, q.getOptionA());
                stmt.setString(4, q.getOptionB());
                stmt.setString(5, q.getOptionC());
                stmt.setString(6, q.getOptionD());
                stmt.setString(7, String.valueOf(q.getCorrectOption()));
                stmt.setInt(8, q.getPoints());
                stmt.addBatch();
            }

            stmt.executeBatch();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int index = 0;
                while (generatedKeys.next() && index < questions.size()) {
                    questions.get(index).setId(generatedKeys.getInt(1));
                    questions.get(index).setQuizId(quizId);
                    index++;
                }
            }
        }
    }

    @Override
    public void createBatch(int quizId, List<Question> questions) throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection()) {
            createBatch(conn, quizId, questions);
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

    private Question mapRowToQuestion(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int quizId = rs.getInt("quiz_id");
        String text = rs.getString("question_text");
        String a = rs.getString("option_a");
        String b = rs.getString("option_b");
        String c = rs.getString("option_c");
        String d = rs.getString("option_d");
        char correct = rs.getString("correct_option").charAt(0);
        int points = rs.getInt("points");

        return new Question(id, quizId, text, a, b, c, d, correct, points);
    }
}
