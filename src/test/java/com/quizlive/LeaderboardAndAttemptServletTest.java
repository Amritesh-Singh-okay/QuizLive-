package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.model.AppUser;
import com.quizlive.model.Attempt;
import com.quizlive.model.LeaderboardEntry;
import com.quizlive.model.enums.Role;
import com.quizlive.service.LeaderboardService;
import com.quizlive.servlet.AttemptServlet;
import com.quizlive.servlet.LeaderboardServlet;
import com.quizlive.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardAndAttemptServletTest {

    private LeaderboardServlet leaderboardServlet;
    private AttemptServlet attemptServlet;
    private LeaderboardService leaderboardService;
    private AttemptDao attemptDao;

    @BeforeEach
    void setUp() {
        this.leaderboardService = new LeaderboardService() {
            @Override
            public List<LeaderboardEntry> getLeaderboard(int quizId) throws SQLException {
                List<LeaderboardEntry> list = new ArrayList<>();
                list.add(new LeaderboardEntry(1, 10, 1, "Alice", 40, 40, 100.0, 30, "00:30", 0, null));
                return list;
            }
        };

        this.attemptDao = new AttemptDao() {
            @Override
            public Attempt startAttempt(int quizId, int userId) {
                return null;
            }

            @Override
            public Attempt findById(int attemptId) {
                return null;
            }

            @Override
            public Attempt findByQuizAndUser(int quizId, int userId) {
                return null;
            }

            @Override
            public boolean submitAttempt(Attempt attempt, List<com.quizlive.model.AttemptAnswer> answers) {
                return false;
            }

            @Override
            public boolean incrementTabSwitches(int attemptId) {
                return false;
            }

            @Override
            public List<Attempt> getLeaderboard(int quizId) {
                return List.of();
            }

            @Override
            public List<Attempt> listByUser(int userId) {
                Attempt a = new Attempt();
                a.setId(101);
                a.setUserId(userId);
                a.setQuizId(1);
                return List.of(a);
            }

            @Override
            public List<Attempt> listByQuiz(int quizId) {
                Attempt a = new Attempt();
                a.setId(102);
                a.setQuizId(quizId);
                return List.of(a);
            }

            @Override
            public List<Attempt> listByCreator(int creatorId) {
                Attempt a = new Attempt();
                a.setId(103);
                a.setQuizId(1);
                return List.of(a);
            }
        };

        this.leaderboardServlet = new LeaderboardServlet(leaderboardService);
        this.attemptServlet = new AttemptServlet(attemptDao);
    }

    @Test
    void testLeaderboardSuccess() throws Exception {
        TestContext ctx = new TestContext();
        ctx.parameters.put("quizId", new String[]{"1"});

        leaderboardServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        Map<?, ?> body = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertNotNull(data);
        assertEquals(1, data.size());
    }

    @Test
    void testLeaderboardMissingQuizId() throws Exception {
        TestContext ctx = new TestContext();

        leaderboardServlet.service(ctx.request, ctx.response);

        assertEquals(400, ctx.status);
        Map<?, ?> body = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    void testAttemptServletUnauthenticated() throws Exception {
        TestContext ctx = new TestContext();

        attemptServlet.service(ctx.request, ctx.response);

        assertEquals(401, ctx.status);
    }

    @Test
    void testAttemptServletListUserAttempts() throws Exception {
        TestContext ctx = new TestContext();
        AppUser student = AppUser.create(5, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);
        ctx.sessionHolder.attributes.put("user", student);

        attemptServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        Map<?, ?> body = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertEquals(1, data.size());
    }

    @Test
    void testAttemptServletListQuizAttemptsForbiddenForParticipant() throws Exception {
        TestContext ctx = new TestContext();
        AppUser student = AppUser.create(5, "Student", "student@quizlive.com", "h", "s", Role.PARTICIPANT, null);
        ctx.sessionHolder.attributes.put("user", student);
        ctx.parameters.put("quizId", new String[]{"1"});

        attemptServlet.service(ctx.request, ctx.response);

        assertEquals(403, ctx.status);
    }

    @Test
    void testAttemptServletListQuizAttemptsAllowedForCreator() throws Exception {
        TestContext ctx = new TestContext();
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        ctx.sessionHolder.attributes.put("user", creator);
        ctx.parameters.put("quizId", new String[]{"1"});

        attemptServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        Map<?, ?> body = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertEquals(1, data.size());
    }

    @Test
    void testAttemptServletListCreatorSubmissions() throws Exception {
        TestContext ctx = new TestContext();
        AppUser creator = AppUser.create(2, "Creator", "creator@quizlive.com", "h", "s", Role.CREATOR, null);
        ctx.sessionHolder.attributes.put("user", creator);

        attemptServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        Map<?, ?> body = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) body.get("success"));
        List<?> data = (List<?>) body.get("data");
        assertEquals(1, data.size());
    }

    private static class TestContext {
        private String method = "GET";
        private String contextPath = "/quizlive";
        private final Map<String, String[]> parameters = new HashMap<>();
        private int status = 200;
        private final StringWriter responseWriter = new StringWriter();
        private final SessionHolder sessionHolder = new SessionHolder();

        final HttpServletRequest request;
        final HttpServletResponse response;
        final HttpSession session;

        TestContext() {
            this.session = (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getAttribute" -> sessionHolder.attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            sessionHolder.attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        default -> null;
                    }
            );

            this.request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getContextPath" -> contextPath;
                        case "getParameter" -> {
                            String[] vals = parameters.get((String) args[0]);
                            yield (vals != null && vals.length > 0) ? vals[0] : null;
                        }
                        case "getParameterMap" -> parameters;
                        case "getSession" -> sessionHolder.attributes.isEmpty() && !sessionHolder.attributes.containsKey("user") ? (args.length > 0 && !(Boolean) args[0] ? null : session) : session;
                        default -> null;
                    }
            );

            this.response = (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "setStatus" -> {
                            status = (int) args[0];
                            yield null;
                        }
                        case "getStatus" -> status;
                        case "getWriter" -> new PrintWriter(responseWriter);
                        default -> null;
                    }
            );
        }

        String getResponseBody() {
            return responseWriter.toString();
        }
    }

    private static class SessionHolder {
        final Map<String, Object> attributes = new HashMap<>();
    }
}
