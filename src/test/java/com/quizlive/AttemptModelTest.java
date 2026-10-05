package com.quizlive;

import com.quizlive.model.Attempt;
import com.quizlive.model.AttemptAnswer;
import com.quizlive.model.Gradeable;
import com.quizlive.model.enums.AttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttemptModelTest {

    @Test
    @DisplayName("Verify Attempt implements Gradeable and computes scores accurately")
    void testGradeableContract() {
        Timestamp start = new Timestamp(System.currentTimeMillis() - 120000);
        Timestamp end = new Timestamp(System.currentTimeMillis());

        Attempt attempt = new Attempt(1, 10, 5, start, end, 8, 10, 0, AttemptStatus.SUBMITTED);

        assertInstanceOf(Gradeable.class, attempt);
        assertEquals(8, attempt.computeScore());
        assertEquals(80.0, attempt.getPercentage());
        assertTrue(attempt.isPassed(75.0));
        assertFalse(attempt.isPassed(85.0));
        assertEquals(120, attempt.getDurationSeconds());
        assertEquals("2m 0s", attempt.getFormattedDuration());
    }

    @Test
    @DisplayName("Verify AttemptAnswer collection inside Attempt")
    void testAttemptAnswers() {
        Attempt attempt = new Attempt();
        AttemptAnswer a1 = new AttemptAnswer(1, 10, 101, 'A', true);
        AttemptAnswer a2 = new AttemptAnswer(2, 10, 102, 'B', false);

        attempt.addAnswer(a1);
        attempt.addAnswer(a2);

        assertEquals(2, attempt.getAnswers().size());
        assertTrue(attempt.getAnswers().get(0).isCorrect());
        assertFalse(attempt.getAnswers().get(1).isCorrect());
    }
}
