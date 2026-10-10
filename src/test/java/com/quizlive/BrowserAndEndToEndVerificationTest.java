package com.quizlive;

import com.quizlive.model.AppUser;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.util.DatabaseInitializer;
import com.quizlive.util.DbConnectionUtil;
import com.quizlive.util.JsonUtil;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserAndEndToEndVerificationTest {

    private static Tomcat tomcat;
    private static final int PORT = 8092;
    private static final String BASE_URL = "http://localhost:" + PORT + "/quizlive";

    private static final List<Integer> createdQuizIds = new ArrayList<>();
    private static final List<Integer> createdUserIds = new ArrayList<>();

    @BeforeAll
    static void startServer() throws Exception {
        DatabaseInitializer.initialize();
        tomcat = AppLauncher.createServer(PORT, "/quizlive");
        tomcat.start();
        // Wait briefly for server ready
        Thread.sleep(1000);
    }

    @AfterAll
    static void stopServer() throws Exception {
        try (Connection conn = DbConnectionUtil.getConnection()) {
            for (int qId : createdQuizIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE quiz_id = ?)")) {
                    ps.setInt(1, qId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attempts WHERE quiz_id = ?")) {
                    ps.setInt(1, qId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM questions WHERE quiz_id = ?")) {
                    ps.setInt(1, qId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM quizzes WHERE id = ?")) {
                    ps.setInt(1, qId);
                    ps.executeUpdate();
                }
            }
            for (int uId : createdUserIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, uId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ignored) {
        }

        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    @DisplayName("Complete end-to-end lifecycle: Register creator -> Admin approves rank -> Direct publish -> Private quiz by code -> Participant joins -> Headless Chrome rendering")
    void testEndToEndLifecycleWithBrowserRendering() throws Exception {
        long stamp = System.currentTimeMillis();

        // 1. Teacher registers as a quiz creator
        CookieManager teacherCookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient teacherClient = HttpClient.newBuilder()
                .cookieHandler(teacherCookies)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String teacherEmail = "teacher_" + stamp + "@quizlive.com";
        String registerBody = "name=" + java.net.URLEncoder.encode("Prof. Smith " + stamp, "UTF-8")
                + "&email=" + java.net.URLEncoder.encode(teacherEmail, "UTF-8")
                + "&password=password123"
                + "&role=CREATOR";

        HttpRequest regReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(registerBody))
                .build();

        HttpResponse<String> regRes = teacherClient.send(regReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(regRes.statusCode() == 302 || regRes.statusCode() == 200 || regRes.statusCode() == 201,
                "Registration status should be 200, 201, or 302, but was " + regRes.statusCode());

        // Fetch teacher user ID from database
        com.quizlive.dao.UserDao userDao = new com.quizlive.dao.impl.UserDaoImpl();
        AppUser teacherUser = userDao.findByEmail(teacherEmail);
        assertNotNull(teacherUser);
        createdUserIds.add(teacherUser.getId());
        assertEquals("STANDARD", teacherUser.getRank());
        assertFalse(teacherUser.canPublishDirectly(), "Newly registered creator should NOT be able to publish directly");

        // Log teacher in
        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=" + java.net.URLEncoder.encode(teacherEmail, "UTF-8") + "&password=password123"))
                .build();
        HttpResponse<String> loginRes = teacherClient.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(loginRes.statusCode() == 302 || loginRes.statusCode() == 200);

        // 2. Teacher authors Quiz 1 under standard rank -> persists as PENDING
        Map<String, Object> quiz1Payload = Map.of(
                "title", "Standard Rank Chemistry",
                "description", "Requires admin moderation",
                "durationSeconds", 300,
                "accessCode", "CHEMSTD" + (stamp % 10000),
                "isPublic", true,
                "questions", List.of(Map.of(
                        "questionText", "What is the atomic number of Carbon?",
                        "optionA", "4", "optionB", "6", "optionC", "8", "optionD", "12",
                        "correctOption", "B", "points", 10
                ))
        );

        HttpRequest createQ1Req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quizzes/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(quiz1Payload)))
                .build();

        HttpResponse<String> createQ1Res = teacherClient.send(createQ1Req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createQ1Res.statusCode());
        Map<?, ?> q1ResMap = JsonUtil.fromJson(createQ1Res.body(), Map.class);
        int quiz1Id = ((Number) ((Map<?, ?>) q1ResMap.get("data")).get("id")).intValue();
        createdQuizIds.add(quiz1Id);

        com.quizlive.dao.QuizDao quizDao = new com.quizlive.dao.impl.QuizDaoImpl();
        com.quizlive.model.Quiz q1 = quizDao.findById(quiz1Id);
        assertNotNull(q1);
        assertEquals(QuizStatus.PENDING, q1.getStatus(), "Standard creator quizzes must default to PENDING");

        // 3. Admin logs in and approves creator rank
        CookieManager adminCookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient adminClient = HttpClient.newBuilder()
                .cookieHandler(adminCookies)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest adminLoginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=admin@quizlive.com&password=password123"))
                .build();
        adminClient.send(adminLoginReq, HttpResponse.BodyHandlers.ofString());

        // Admin updates creator rank to VERIFIED
        HttpRequest updateRankReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/users/update-rank"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(Map.of(
                        "userId", teacherUser.getId(),
                        "rank", "VERIFIED"
                ))))
                .build();

        HttpResponse<String> updateRankRes = adminClient.send(updateRankReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, updateRankRes.statusCode());

        // 4. Teacher can directly publish Quiz 1 without waiting for admin approval
        HttpRequest publishQ1Req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quizzes/approve"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(Map.of(
                        "quizId", quiz1Id,
                        "action", "approve"
                ))))
                .build();

        HttpResponse<String> publishQ1Res = teacherClient.send(publishQ1Req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, publishQ1Res.statusCode(), "Verified creator should be able to publish pending quiz directly");

        Quiz q1Approved = quizDao.findById(quiz1Id);
        assertNotNull(q1Approved);
        assertEquals(QuizStatus.APPROVED, q1Approved.getStatus());

        // 5. Verified teacher authors a Private/Unlisted Quiz 2 with access code
        String privateCode = "CHEMPRV" + (stamp % 10000);
        Map<String, Object> quiz2Payload = Map.of(
                "title", "Private Organic Chemistry Exam",
                "description", "Unlisted assessment accessible only via access code",
                "durationSeconds", 300,
                "accessCode", privateCode,
                "isPublic", false,
                "questions", List.of(Map.of(
                        "questionText", "Which functional group contains -OH?",
                        "optionA", "Alcohol", "optionB", "Ketone", "optionC", "Ester", "optionD", "Amine",
                        "correctOption", "A", "points", 10
                ))
        );

        HttpRequest createQ2Req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quizzes/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(quiz2Payload)))
                .build();

        HttpResponse<String> createQ2Res = teacherClient.send(createQ2Req, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createQ2Res.statusCode());
        Map<?, ?> q2ResMap = JsonUtil.fromJson(createQ2Res.body(), Map.class);
        int quiz2Id = ((Number) ((Map<?, ?>) q2ResMap.get("data")).get("id")).intValue();
        createdQuizIds.add(quiz2Id);

        Quiz q2 = quizDao.findById(quiz2Id);
        assertNotNull(q2);
        assertEquals(QuizStatus.APPROVED, q2.getStatus(), "Verified creator new quiz should be APPROVED directly");
        assertFalse(q2.isPublic(), "Quiz 2 should be private / unlisted");

        // 6. Student views public catalog -> Quiz 2 MUST NOT appear in catalog!
        CookieManager studentCookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient studentClient = HttpClient.newBuilder()
                .cookieHandler(studentCookies)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Login as student Alice
        HttpRequest studentLoginReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("email=alice@quizlive.com&password=password123"))
                .build();
        studentClient.send(studentLoginReq, HttpResponse.BodyHandlers.ofString());

        HttpRequest catalogReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quizzes"))
                .GET()
                .build();
        HttpResponse<String> catalogRes = studentClient.send(catalogReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, catalogRes.statusCode());
        Map<?, ?> catalogData = JsonUtil.fromJson(catalogRes.body(), Map.class);
        List<?> catalogQuizzes = (List<?>) catalogData.get("data");

        boolean q2FoundInCatalog = catalogQuizzes.stream().anyMatch(qz -> {
            Map<?, ?> map = (Map<?, ?>) qz;
            return ((Number) map.get("id")).intValue() == quiz2Id;
        });
        assertFalse(q2FoundInCatalog, "Unlisted quiz 2 must NOT appear in student catalog to prevent clutter");

        // 7. Student looks up Quiz 2 using access code
        HttpRequest codeLookupReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quizzes?code=" + privateCode))
                .GET()
                .build();
        HttpResponse<String> codeLookupRes = studentClient.send(codeLookupReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, codeLookupRes.statusCode());
        Map<?, ?> codeQuiz = JsonUtil.fromJson(codeLookupRes.body(), Map.class);
        assertEquals(quiz2Id, ((Number) ((Map<?, ?>) codeQuiz.get("data")).get("id")).intValue(), "Quiz must be found via code");

        // 8. Student starts attempt using access code
        HttpRequest startAttemptReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/attempts/start"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(Map.of("code", privateCode))))
                .build();
        HttpResponse<String> startAttemptRes = studentClient.send(startAttemptReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, startAttemptRes.statusCode());
        Map<?, ?> attemptData = JsonUtil.fromJson(startAttemptRes.body(), Map.class);
        int attemptId = ((Number) ((Map<?, ?>) attemptData.get("data")).get("attemptId")).intValue();

        // 9. Student submits attempt
        HttpRequest submitReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/attempts/submit"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(Map.of(
                        "attemptId", attemptId,
                        "answers", Map.of("1", "A")
                ))))
                .build();
        HttpResponse<String> submitRes = studentClient.send(submitReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, submitRes.statusCode());

        // 10. Student attempts to start again using access code -> receives 409 Conflict with quizId in payload
        HttpResponse<String> repeatStartRes = studentClient.send(startAttemptReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(409, repeatStartRes.statusCode());
        Map<?, ?> repeatBody = JsonUtil.fromJson(repeatStartRes.body(), Map.class);
        Map<?, ?> repeatInner = (Map<?, ?>) repeatBody.get("data");
        assertNotNull(repeatInner);
        assertEquals(quiz2Id, ((Number) repeatInner.get("quizId")).intValue(), "Conflict data must include quizId");

        // 11. Verify Server-Rendered JSP Pages with Authenticated Sessions
        // A. Creator Dashboard with Teacher Session
        HttpRequest creatorDashReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/creator/dashboard.jsp"))
                .GET()
                .build();
        HttpResponse<String> creatorDashRes = teacherClient.send(creatorDashReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, creatorDashRes.statusCode());
        String creatorDashboardHtml = creatorDashRes.body();
        assertTrue(creatorDashboardHtml.contains("Creator Studio"), "Creator dashboard should render Creator Studio");
        assertTrue(creatorDashboardHtml.contains("Verified Teacher Rank"),
                "Creator dashboard should display Verified Teacher Rank badge");
        assertTrue(creatorDashboardHtml.contains("publishQuizDirectly"),
                "Creator dashboard should include publishQuizDirectly handler");

        // B. Participant Dashboard with Student Session
        HttpRequest studentDashReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/participant/dashboard.jsp"))
                .GET()
                .build();
        HttpResponse<String> studentDashRes = studentClient.send(studentDashReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, studentDashRes.statusCode());
        String studentDashHtml = studentDashRes.body();
        assertTrue(studentDashHtml.contains("Join Quiz with Access Code"),
                "Participant dashboard should contain Join Quiz with Access Code card");
        assertTrue(studentDashHtml.contains("join-access-code"),
                "Participant dashboard should contain access code input");

        // C. Take Quiz Page with Student Session
        HttpRequest takeQuizReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/participant/take-quiz.jsp?code=" + privateCode))
                .GET()
                .build();
        HttpResponse<String> takeQuizRes = studentClient.send(takeQuizReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, takeQuizRes.statusCode());
        String takeQuizHtml = takeQuizRes.body();
        assertTrue(takeQuizHtml.contains("QUIZ_CONFIG"), "take-quiz.jsp should render QUIZ_CONFIG script block");
        assertTrue(takeQuizHtml.contains(privateCode), "take-quiz.jsp should inject privateCode into config");

        // D. Leaderboard Page
        HttpRequest boardReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/leaderboard.jsp?quizId=" + quiz2Id))
                .GET()
                .build();
        HttpResponse<String> boardRes = studentClient.send(boardReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, boardRes.statusCode());
        String boardHtml = boardRes.body();
        assertTrue(boardHtml.contains("Live Standings"), "Leaderboard should render Live Standings");
        assertTrue(boardHtml.contains("LEADERBOARD_CONFIG"), "Leaderboard should render LEADERBOARD_CONFIG");

        // 12. Real headless browser execution via Chrome or Edge
        File chromeExe = new File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        if (!chromeExe.exists()) {
            chromeExe = new File("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        }

        if (chromeExe.exists()) {
            // Render Live Leaderboard in real browser engine
            String browserLeaderboardDom = runBrowserDumpDom(chromeExe.getAbsolutePath(), BASE_URL + "/leaderboard.jsp?quizId=" + quiz2Id);
            assertNotNull(browserLeaderboardDom);
            assertTrue(browserLeaderboardDom.contains("Live Standings"), "Browser should render Live Standings");
            assertTrue(browserLeaderboardDom.contains("quiz-select"), "Browser should render quiz selector dropdown");

            // Render Public Portal in real browser engine
            String browserIndexDom = runBrowserDumpDom(chromeExe.getAbsolutePath(), BASE_URL + "/index.jsp");
            assertNotNull(browserIndexDom);
            assertTrue(browserIndexDom.contains("QuizLive"), "Browser should render QuizLive brand");
            assertTrue(browserIndexDom.contains("Proctored Examination Engine"), "Browser should render hero banner");
        }
    }

    private static String runBrowserDumpDom(String browserPath, String url) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(browserPath);
            cmd.add("--headless=new");
            cmd.add("--disable-gpu");
            cmd.add("--no-sandbox");
            cmd.add("--dump-dom");
            cmd.add(url);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            proc.waitFor();
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
