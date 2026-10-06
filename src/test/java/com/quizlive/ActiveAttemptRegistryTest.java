package com.quizlive;

import com.quizlive.concurrency.ActiveAttemptRegistry;
import com.quizlive.concurrency.AttemptSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveAttemptRegistryTest {

    private ActiveAttemptRegistry registry;

    @BeforeEach
    void setUp() {
        this.registry = new ActiveAttemptRegistry();
        this.registry.clear();
    }

    @Test
    @DisplayName("Verify register, get, remaining time, and remove")
    void testSessionLifecycle() {
        AttemptSession session = registry.registerSession(101, 1, 3, 300);
        assertNotNull(session);
        assertEquals(101, session.getAttemptId());
        assertTrue(session.getRemainingSeconds() <= 300);
        assertTrue(session.getRemainingSeconds() >= 298);
        assertFalse(session.isExpired());

        assertTrue(registry.isAttemptActive(101));
        assertEquals(1, registry.getActiveCount());

        AttemptSession removed = registry.removeSession(101);
        assertNotNull(removed);
        assertFalse(registry.isAttemptActive(101));
        assertNull(registry.getSession(101));
    }

    @Test
    @DisplayName("Verify thread-safe atomic tab switch increments under concurrent threads")
    void testConcurrentTabSwitches() throws InterruptedException {
        registry.registerSession(202, 1, 4, 300);

        int threadCount = 10;
        int incrementsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    registry.incrementTabSwitches(202);
                }
                latch.countDown();
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "All threads should finish within 5 seconds");
        executor.shutdown();

        AttemptSession session = registry.getSession(202);
        assertNotNull(session);
        assertEquals(200, session.getTabSwitches(), "AtomicInteger must guarantee exact sum of 10 * 20 = 200");
    }

    @Test
    @DisplayName("Verify expiration detection for expired session")
    void testExpirationCheck() {
        // Attempt with 0 seconds duration and 0 seconds grace period
        registry.registerSession(303, 1, 5, 0, 0);

        // Allow 50ms to pass
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }

        assertTrue(registry.isExpired(303));
    }
}
