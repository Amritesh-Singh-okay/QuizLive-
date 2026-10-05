package com.quizlive.dao;

import com.quizlive.model.Question;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface QuestionDao {

    Question findById(int id) throws SQLException;

    List<Question> findByQuizId(int quizId) throws SQLException;

    List<Question> findByQuizIdSanitized(int quizId) throws SQLException;

    void createBatch(Connection conn, int quizId, List<Question> questions) throws SQLException;

    void createBatch(int quizId, List<Question> questions) throws SQLException;

    boolean delete(int id) throws SQLException;
}
