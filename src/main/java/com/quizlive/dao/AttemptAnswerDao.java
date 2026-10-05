package com.quizlive.dao;

import com.quizlive.model.AttemptAnswer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface AttemptAnswerDao {

    List<AttemptAnswer> findByAttemptId(int attemptId) throws SQLException;

    void saveBatch(Connection conn, int attemptId, List<AttemptAnswer> answers) throws SQLException;
}
