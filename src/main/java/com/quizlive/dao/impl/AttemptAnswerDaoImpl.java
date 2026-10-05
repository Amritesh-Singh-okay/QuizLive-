package com.quizlive.dao.impl;

import com.quizlive.dao.AttemptAnswerDao;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.util.DbConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AttemptAnswerDaoImpl implements AttemptAnswerDao {

    private static final String SQL_FIND_BY_ATTEMPT_ID =
            "SELECT id, attempt_id, question_id, selected_option, is_correct FROM attempt_answers WHERE attempt_id = ? ORDER BY id ASC";

    private static final String SQL_INSERT =
            "INSERT INTO attempt_answers (attempt_id, question_id, selected_option, is_correct) VALUES (?, ?, ?, ?)";

    @Override
    public List<AttemptAnswer> findByAttemptId(int attemptId) throws SQLException {
        List<AttemptAnswer> answers = new ArrayList<>();
        try (Connection conn = DbConnectionUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_FIND_BY_ATTEMPT_ID)) {
            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int attId = rs.getInt("attempt_id");
                    int qId = rs.getInt("question_id");
                    String opt = rs.getString("selected_option");
                    Character selected = (opt != null && !opt.isEmpty()) ? opt.charAt(0) : null;
                    boolean isCorrect = rs.getBoolean("is_correct");

                    answers.add(new AttemptAnswer(id, attId, qId, selected, isCorrect));
                }
            }
        }
        return answers;
    }

    @Override
    public void saveBatch(Connection conn, int attemptId, List<AttemptAnswer> answers) throws SQLException {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            for (AttemptAnswer a : answers) {
                stmt.setInt(1, attemptId);
                stmt.setInt(2, a.getQuestionId());
                if (a.getSelectedOption() != null) {
                    stmt.setString(3, String.valueOf(a.getSelectedOption()));
                } else {
                    stmt.setNull(3, java.sql.Types.CHAR);
                }
                stmt.setBoolean(4, a.isCorrect());
                stmt.addBatch();
            }

            stmt.executeBatch();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int index = 0;
                while (generatedKeys.next() && index < answers.size()) {
                    answers.get(index).setId(generatedKeys.getInt(1));
                    answers.get(index).setAttemptId(attemptId);
                    index++;
                }
            }
        }
    }
}
