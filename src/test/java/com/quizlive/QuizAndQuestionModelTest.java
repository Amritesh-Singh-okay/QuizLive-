package com.quizlive;

import com.quizlive.model.Message;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuizAndQuestionModelTest {

    @Test
    @DisplayName("Verify Question creation and answer key sanitization")
    void testQuestionModelAndSanitization() {
        Question q = new Question(1, 10, "What is Java?", "Language", "Fruit", "Car", "City", 'A', 2);
        assertEquals('A', q.getCorrectOption());
        assertEquals(2, q.getPoints());

        // Test security sanitization
        Question safe = q.sanitized();
        assertEquals(' ', safe.getCorrectOption(), "Correct option must be wiped in sanitized copy");
        assertEquals("What is Java?", safe.getQuestionText());
        assertEquals(2, safe.getPoints());
        assertNotEquals(q.getCorrectOption(), safe.getCorrectOption());
    }

    @Test
    @DisplayName("Verify Quiz calculations, duration formatting, and collections")
    void testQuizModel() {
        Quiz quiz = new Quiz(1, "Java Basics", "Intro quiz", 2, 330, QuizStatus.APPROVED, new Timestamp(System.currentTimeMillis()));

        Question q1 = new Question(1, 1, "Q1", "A", "B", "C", "D", 'A', 2);
        Question q2 = new Question(2, 1, "Q2", "A", "B", "C", "D", 'B', 3);

        quiz.addQuestion(q1);
        quiz.addQuestion(q2);

        assertEquals(2, quiz.getQuestionCount());
        assertEquals(5, quiz.getTotalPoints(), "Total points should be sum of 2 + 3 = 5");
        assertEquals("5m 30s", quiz.getFormattedDuration());
    }

    @Test
    @DisplayName("Verify Message model creation")
    void testMessageModel() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Message msg = new Message(1, 3, 2, 1, "Great quiz!", now);
        assertEquals(3, msg.getFromUserId());
        assertEquals(2, msg.getToUserId());
        assertEquals(1, msg.getQuizId());
        assertEquals("Great quiz!", msg.getContent());
        assertNotNull(msg.getSentAt());
    }
}
