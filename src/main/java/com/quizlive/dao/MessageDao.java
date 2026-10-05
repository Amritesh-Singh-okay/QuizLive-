package com.quizlive.dao;

import com.quizlive.model.Message;

import java.sql.SQLException;
import java.util.List;

public interface MessageDao {

    Message send(Message message) throws SQLException;

    List<Message> getThread(int userA, int userB) throws SQLException;

    List<Message> listByQuiz(int quizId) throws SQLException;

    List<Message> listByUser(int userId) throws SQLException;
}
