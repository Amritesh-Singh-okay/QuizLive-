package com.quizlive;

import com.quizlive.model.LeaderboardEntry;
import com.quizlive.service.LeaderboardService;
import com.quizlive.util.JsonUtil;
import com.quizlive.websocket.LeaderboardEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardEndpointTest {

    private LeaderboardEndpoint endpoint;
    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        this.leaderboardService = new LeaderboardService();
        this.endpoint = new LeaderboardEndpoint(leaderboardService);
        LeaderboardEndpoint.clearAllSessions();
    }

    @AfterEach
    void tearDown() {
        LeaderboardEndpoint.clearAllSessions();
    }

    @Test
    @DisplayName("LeaderboardEndpoint registers session and delivers initial leaderboard upon connection")
    void testOnOpenValidQuiz() {
        TestWsSession wsSession = new TestWsSession();
        endpoint.onOpen(wsSession.session, "1");

        assertEquals(1, LeaderboardEndpoint.getActiveSessionsCount(1));
        assertFalse(wsSession.sentMessages.isEmpty());

        String initialMessage = wsSession.sentMessages.get(0);
        Map<?, ?> parsed = JsonUtil.fromJson(initialMessage, Map.class);
        assertEquals("LEADERBOARD_UPDATE", parsed.get("type"));
        assertEquals(1.0, ((Number) parsed.get("quizId")).doubleValue());
        List<?> data = (List<?>) parsed.get("data");
        assertNotNull(data);
    }

    @Test
    @DisplayName("LeaderboardEndpoint responds to refresh message with updated rankings")
    void testOnMessageRefresh() {
        TestWsSession wsSession = new TestWsSession();
        endpoint.onOpen(wsSession.session, "1");
        wsSession.sentMessages.clear();

        endpoint.onMessage("refresh", wsSession.session, "1");
        assertFalse(wsSession.sentMessages.isEmpty());

        String response = wsSession.sentMessages.get(0);
        assertTrue(response.contains("LEADERBOARD_UPDATE"));
    }

    @Test
    @DisplayName("LeaderboardEndpoint broadcasts updates to all connected subscribers of a quiz")
    void testBroadcast() {
        TestWsSession client1 = new TestWsSession();
        TestWsSession client2 = new TestWsSession();

        endpoint.onOpen(client1.session, "1");
        endpoint.onOpen(client2.session, "1");

        client1.sentMessages.clear();
        client2.sentMessages.clear();

        LeaderboardEndpoint.broadcast(1, "{\"event\":\"UPDATE\"}");

        assertEquals(1, client1.sentMessages.size());
        assertEquals(1, client2.sentMessages.size());
        assertEquals("{\"event\":\"UPDATE\"}", client1.sentMessages.get(0));
        assertEquals("{\"event\":\"UPDATE\"}", client2.sentMessages.get(0));

        LeaderboardEndpoint.broadcastLeaderboard(1, leaderboardService);

        assertEquals(2, client1.sentMessages.size());
        assertEquals(2, client2.sentMessages.size());
        assertTrue(client1.sentMessages.get(1).contains("LEADERBOARD_UPDATE"));
    }

    @Test
    @DisplayName("LeaderboardEndpoint removes session on disconnect or close")
    void testOnClose() {
        TestWsSession client = new TestWsSession();
        endpoint.onOpen(client.session, "1");
        assertEquals(1, LeaderboardEndpoint.getActiveSessionsCount(1));

        endpoint.onClose(client.session, "1");
        assertEquals(0, LeaderboardEndpoint.getActiveSessionsCount(1));
    }

    @Test
    @DisplayName("LeaderboardEndpoint rejects invalid quizId with close reason")
    void testInvalidQuizIdRejection() {
        TestWsSession client = new TestWsSession();
        endpoint.onOpen(client.session, "invalid_id");

        assertEquals(0, LeaderboardEndpoint.getActiveSessionsCount(0));
        assertFalse(client.open);
        assertNotNull(client.closeReason);
    }

    private static class TestWsSession {
        boolean open = true;
        CloseReason closeReason;
        final List<String> sentMessages = new ArrayList<>();

        final Session session;
        final RemoteEndpoint.Basic basicRemote;

        TestWsSession() {
            this.basicRemote = (RemoteEndpoint.Basic) Proxy.newProxyInstance(
                    RemoteEndpoint.Basic.class.getClassLoader(),
                    new Class<?>[]{RemoteEndpoint.Basic.class},
                    (proxy, method, args) -> {
                        if ("sendText".equals(method.getName())) {
                            sentMessages.add((String) args[0]);
                            return null;
                        }
                        return null;
                    }
            );

            this.session = (Session) Proxy.newProxyInstance(
                    Session.class.getClassLoader(),
                    new Class<?>[]{Session.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isOpen" -> open;
                        case "getBasicRemote" -> basicRemote;
                        case "close" -> {
                            open = false;
                            if (args != null && args.length > 0 && args[0] instanceof CloseReason cr) {
                                closeReason = cr;
                            }
                            yield null;
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "TestWsSession@" + Integer.toHexString(System.identityHashCode(proxy));
                        default -> null;
                    }
            );
        }
    }
}
