package com.quizlive.dao;

import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface QuizDao {

    Quiz createWithQuestions(Quiz quiz) throws SQLException;

    Quiz findById(int id) throws SQLException;

    Quiz findByIdWithQuestions(int id) throws SQLException;

    List<Quiz> listAll() throws SQLException;

    List<Quiz> listApproved() throws SQLException;

    List<Quiz> listByCreator(int creatorId) throws SQLException;

    List<Quiz> listByStatus(QuizStatus status) throws SQLException;

    boolean updateStatus(int quizId, QuizStatus status) throws SQLException;

    default boolean updateHeldStatus(int quizId, boolean isHeld) throws SQLException {
        return false;
    }

    default boolean updateScheduledStart(int quizId, Timestamp scheduledStartAt) throws SQLException {
        return false;
    }

    boolean delete(int id) throws SQLException;
}
