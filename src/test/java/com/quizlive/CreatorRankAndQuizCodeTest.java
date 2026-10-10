package com.quizlive;

import com.quizlive.dao.AttemptDao;
import com.quizlive.dao.QuizDao;
import com.quizlive.dao.UserDao;
import com.quizlive.dao.impl.AttemptDaoImpl;
import com.quizlive.dao.impl.QuizDaoImpl;
import com.quizlive.dao.impl.UserDaoImpl;
import com.quizlive.model.AppUser;
import com.quizlive.model.Question;
import com.quizlive.model.Quiz;
import com.quizlive.model.enums.QuizStatus;
import com.quizlive.model.enums.Role;
import com.quizlive.servlet.ApproveQuizServlet;
import com.quizlive.servlet.CreateQuizServlet;
import com.quizlive.servlet.HostQuizServlet;
import com.quizlive.servlet.ListQuizzesServlet;
import com.quizlive.servlet.StartAttemptServlet;
import com.quizlive.servlet.UserManagementServlet;
import com.quizlive.util.DatabaseInitializer;
import com.quizlive.util.DbConnectionUtil;
import com.quizlive.util.JsonUtil;
import com.quizlive.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatorRankAndQuizCodeTest {

    private UserDao userDao;
    private QuizDao quizDao;
    private AttemptDao attemptDao;

    private UserManagementServlet userManagementServlet;
    private CreateQuizServlet createQuizServlet;
    private ListQuizzesServlet listQuizzesServlet;
    private StartAttemptServlet startAttemptServlet;
    private ApproveQuizServlet approveQuizServlet;
    private HostQuizServlet hostQuizServlet;

    private final List<Integer> createdUserIds = new ArrayList<>();
    private final List<Integer> createdQuizIds = new ArrayList<>();
    private final List<Integer> createdAttemptIds = new ArrayList<>();

    private AppUser adminUser;
    private AppUser creatorUser;
    private AppUser studentUser;

    @BeforeAll
    static void initDb() throws SQLException {
        DatabaseInitializer.initialize();
    }

    @BeforeEach
    void setUp() throws SQLException {
        this.userDao = new UserDaoImpl();
        this.quizDao = new QuizDaoImpl();
        this.attemptDao = new AttemptDaoImpl();

        this.userManagementServlet = new UserManagementServlet(userDao);
        this.createQuizServlet = new CreateQuizServlet();
        this.listQuizzesServlet = new ListQuizzesServlet();
        this.startAttemptServlet = new StartAttemptServlet();
        this.approveQuizServlet = new ApproveQuizServlet(new com.quizlive.service.QuizService(quizDao, new com.quizlive.dao.impl.QuestionDaoImpl()), userDao);
        this.hostQuizServlet = new HostQuizServlet(new com.quizlive.service.QuizService(quizDao, new com.quizlive.dao.impl.QuestionDaoImpl()));

        long stamp = System.currentTimeMillis();

        // 1. Create Admin
        AppUser admin = AppUser.create(0, "Admin " + stamp, "admin_" + stamp + "@test.com",
                "dummyhash", "dummysalt", Role.ADMIN, "ADMIN", null);
        admin = userDao.create(admin);
        adminUser = admin;
        createdUserIds.add(admin.getId());

        // 2. Create Standard Creator
        AppUser creator = AppUser.create(0, "Creator " + stamp, "creator_" + stamp + "@test.com",
                "dummyhash", "dummysalt", Role.CREATOR, "STANDARD", null);
        creator = userDao.create(creator);
        creatorUser = creator;
        createdUserIds.add(creator.getId());

        // 3. Create Student
        AppUser student = AppUser.create(0, "Student " + stamp, "student_" + stamp + "@test.com",
                "dummyhash", "dummysalt", Role.PARTICIPANT, "STANDARD", null);
        student = userDao.create(student);
        studentUser = student;
        createdUserIds.add(student.getId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DbConnectionUtil.getConnection()) {
            for (int attemptId : createdAttemptIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attempt_answers WHERE attempt_id = ?")) {
                    ps.setInt(1, attemptId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attempts WHERE id = ?")) {
                    ps.setInt(1, attemptId);
                    ps.executeUpdate();
                }
            }
            for (int quizId : createdQuizIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM questions WHERE quiz_id = ?")) {
                    ps.setInt(1, quizId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM quizzes WHERE id = ?")) {
                    ps.setInt(1, quizId);
                    ps.executeUpdate();
                }
            }
            for (int userId : createdUserIds) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
        }
        createdAttemptIds.clear();
        createdQuizIds.clear();
        createdUserIds.clear();
    }

    @Test
    @DisplayName("Creator starts with STANDARD rank and direct publish disabled")
    void testStandardCreatorDefaults() {
        assertEquals("STANDARD", creatorUser.getRank());
        assertFalse(creatorUser.isRankApproved());
        assertFalse(creatorUser.canPublishDirectly());
    }

    @Test
    @DisplayName("Standard creator creates quiz -> quiz status is PENDING moderation")
    void testStandardCreatorQuizCreationResultsInPending() throws ServletException, IOException, SQLException {
        TestContext ctx = new TestContext();
        ctx.setMethod("POST");
        ctx.sessionHolder.attributes.put("user", creatorUser);

        Quiz payload = new Quiz();
        payload.setTitle("Standard Biology Exam");
        payload.setDescription("Needs approval");
        payload.setDurationSeconds(300);
        payload.setAccessCode("BIOSTD101");
        payload.setPublic(true);

        Question q = new Question();
        q.setQuestionText("What is DNA?");
        q.setOptionA("Acid");
        q.setOptionB("Base");
        q.setOptionC("Salt");
        q.setOptionD("Sugar");
        q.setCorrectOption('A');
        q.setPoints(10);
        payload.setQuestions(List.of(q));

        ctx.setBody(JsonUtil.toJson(payload));

        createQuizServlet.service(ctx.request, ctx.response);

        assertEquals(201, ctx.status);
        Map<?, ?> resMap = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) resMap.get("success"));

        Map<?, ?> data = (Map<?, ?>) resMap.get("data");
        int quizId = ((Number) data.get("id")).intValue();
        createdQuizIds.add(quizId);

        Quiz createdQuiz = quizDao.findById(quizId);
        assertNotNull(createdQuiz);
        assertEquals(QuizStatus.PENDING, createdQuiz.getStatus(), "Standard creator quizzes must remain PENDING");
        assertEquals("BIOSTD101", createdQuiz.getAccessCode());
        assertTrue(createdQuiz.isPublic());
    }

    @Test
    @DisplayName("Admin promotes creator rank to VERIFIED -> direct publishing enabled")
    void testAdminPromotesCreatorRank() throws ServletException, IOException, SQLException {
        TestContext ctx = new TestContext();
        ctx.setMethod("POST");
        ctx.requestUri = "/api/admin/users/update-rank";
        ctx.sessionHolder.attributes.put("user", adminUser);

        Map<String, Object> body = Map.of(
                "userId", creatorUser.getId(),
                "rank", "VERIFIED"
        );
        ctx.setBody(JsonUtil.toJson(body));

        userManagementServlet.service(ctx.request, ctx.response);

        assertEquals(200, ctx.status);
        Map<?, ?> resMap = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) resMap.get("success"));

        AppUser updated = userDao.findById(creatorUser.getId());
        assertNotNull(updated);
        assertEquals("VERIFIED", updated.getRank());
        assertTrue(updated.isRankApproved());
        assertTrue(updated.canPublishDirectly(), "Verified creator must have canPublishDirectly = true");
    }

    @Test
    @DisplayName("Verified creator creates quiz -> quiz status is APPROVED directly without admin intervention")
    void testVerifiedCreatorDirectPublishing() throws ServletException, IOException, SQLException {
        // 1. Promote creator rank
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);

        // 2. Creator creates quiz
        TestContext ctx = new TestContext();
        ctx.setMethod("POST");
        ctx.sessionHolder.attributes.put("user", creatorUser);

        Quiz payload = new Quiz();
        payload.setTitle("Advanced Biochemistry");
        payload.setDescription("Directly published by verified teacher");
        payload.setDurationSeconds(600);
        payload.setAccessCode("BIOADV202");
        payload.setPublic(true);

        Question q = new Question();
        q.setQuestionText("What is ATP?");
        q.setOptionA("Energy currency");
        q.setOptionB("Lipid");
        q.setOptionC("Vitamin");
        q.setOptionD("Mineral");
        q.setCorrectOption('A');
        q.setPoints(10);
        payload.setQuestions(List.of(q));

        ctx.setBody(JsonUtil.toJson(payload));

        createQuizServlet.service(ctx.request, ctx.response);

        assertEquals(201, ctx.status);
        Map<?, ?> resMap = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertTrue((Boolean) resMap.get("success"));

        Map<?, ?> data = (Map<?, ?>) resMap.get("data");
        int quizId = ((Number) data.get("id")).intValue();
        createdQuizIds.add(quizId);

        Quiz createdQuiz = quizDao.findById(quizId);
        assertNotNull(createdQuiz);
        assertEquals(QuizStatus.APPROVED, createdQuiz.getStatus(), "Verified creator quiz must be APPROVED directly");
    }

    @Test
    @DisplayName("Private unlisted quiz is hidden from public catalog but accessible via access code")
    void testPrivateQuizHiddenFromCatalogAndFoundByCode() throws ServletException, IOException, SQLException {
        // 1. Verified creator creates both a public quiz and a private quiz
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);

        long stamp = System.currentTimeMillis();
        String pubCode = "PUB" + (stamp % 1000000);
        String privCode = "PRV" + (stamp % 1000000);

        // A. Public Quiz
        Quiz pubQuiz = createQuizForTest("Public Anatomy Test", pubCode, true);
        createdQuizIds.add(pubQuiz.getId());

        // B. Private Quiz
        Quiz privQuiz = createQuizForTest("Private Class Test", privCode, false);
        createdQuizIds.add(privQuiz.getId());

        // 2. Query default public catalog
        TestContext catalogCtx = new TestContext();
        catalogCtx.setMethod("GET");
        catalogCtx.sessionHolder.attributes.put("user", studentUser);

        listQuizzesServlet.service(catalogCtx.request, catalogCtx.response);

        assertEquals(200, catalogCtx.status);
        Map<?, ?> catalogRes = JsonUtil.fromJson(catalogCtx.getResponseBody(), Map.class);
        List<?> catalogList = (List<?>) catalogRes.get("data");

        boolean pubFound = false;
        boolean privFound = false;
        for (Object item : catalogList) {
            Map<?, ?> qMap = (Map<?, ?>) item;
            int qId = ((Number) qMap.get("id")).intValue();
            if (qId == pubQuiz.getId()) pubFound = true;
            if (qId == privQuiz.getId()) privFound = true;
        }

        assertTrue(pubFound, "Public quiz must appear in public catalog");
        assertFalse(privFound, "Private quiz MUST NOT appear in public catalog (prevents clutter)");

        // 3. Direct access via code parameter: ?code=...
        TestContext codeCtx = new TestContext();
        codeCtx.setMethod("GET");
        codeCtx.setParameter("code", privCode);
        codeCtx.sessionHolder.attributes.put("user", studentUser);

        listQuizzesServlet.service(codeCtx.request, codeCtx.response);

        assertEquals(200, codeCtx.status);
        Map<?, ?> codeRes = JsonUtil.fromJson(codeCtx.getResponseBody(), Map.class);
        assertTrue((Boolean) codeRes.get("success"));
        Map<?, ?> resolvedData = (Map<?, ?>) codeRes.get("data");
        assertEquals(privQuiz.getId(), ((Number) resolvedData.get("id")).intValue());
        assertEquals(privCode, resolvedData.get("accessCode"));
    }

    @Test
    @DisplayName("Participant starts quiz attempt using access code in StartAttemptServlet")
    void testStartAttemptWithAccessCode() throws ServletException, IOException, SQLException {
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);

        String secretCode = "SEC" + (System.currentTimeMillis() % 1000000);
        Quiz privQuiz = createQuizForTest("Secret Entrance Exam", secretCode, false);
        createdQuizIds.add(privQuiz.getId());

        TestContext startCtx = new TestContext();
        startCtx.setMethod("POST");
        startCtx.sessionHolder.attributes.put("user", studentUser);

        // Pass code in JSON payload
        Map<String, Object> body = Map.of("code", secretCode);
        startCtx.setBody(JsonUtil.toJson(body));

        startAttemptServlet.service(startCtx.request, startCtx.response);

        assertEquals(201, startCtx.status);
        Map<?, ?> resMap = JsonUtil.fromJson(startCtx.getResponseBody(), Map.class);
        assertTrue((Boolean) resMap.get("success"));

        Map<?, ?> data = (Map<?, ?>) resMap.get("data");
        int attemptId = ((Number) data.get("attemptId")).intValue();
        createdAttemptIds.add(attemptId);

        int resolvedQuizId = ((Number) data.get("quizId")).intValue();
        assertEquals(privQuiz.getId(), resolvedQuizId);
        assertNotNull(data.get("questions"));
    }

    @Test
    @DisplayName("Non-admin user cannot promote creator rank (HTTP 403 Forbidden)")
    void testNonAdminCannotPromoteRank() throws ServletException, IOException {
        TestContext ctx = new TestContext();
        ctx.setMethod("POST");
        ctx.requestUri = "/api/admin/users/update-rank";
        ctx.sessionHolder.attributes.put("user", creatorUser); // Creator acting on another user

        Map<String, Object> body = Map.of(
                "userId", creatorUser.getId(),
                "rank", "VERIFIED"
        );
        ctx.setBody(JsonUtil.toJson(body));

        userManagementServlet.service(ctx.request, ctx.response);

        assertEquals(403, ctx.status);
    }

    @Test
    @DisplayName("Invalid or non-existent quiz access code returns 404 in ListQuizzesServlet")
    void testInvalidQuizCodeReturns404() throws ServletException, IOException {
        TestContext ctx = new TestContext();
        ctx.setMethod("GET");
        ctx.setParameter("code", "NONEXISTENT_CODE_XYZ");
        ctx.sessionHolder.attributes.put("user", studentUser);

        listQuizzesServlet.service(ctx.request, ctx.response);

        assertEquals(404, ctx.status);
        Map<?, ?> res = JsonUtil.fromJson(ctx.getResponseBody(), Map.class);
        assertFalse((Boolean) res.get("success"));
    }

    @Test
    @DisplayName("Admin revoking creator rank back to STANDARD disables direct publishing")
    void testCreatorRankRevocationRevokesDirectPublish() throws ServletException, IOException, SQLException {
        // 1. Promote to VERIFIED
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);
        assertTrue(creatorUser.canPublishDirectly());

        // 2. Admin revokes rank to STANDARD
        TestContext revokeCtx = new TestContext();
        revokeCtx.setMethod("POST");
        revokeCtx.requestUri = "/api/admin/users/update-rank";
        revokeCtx.sessionHolder.attributes.put("user", adminUser);
        revokeCtx.setBody(JsonUtil.toJson(Map.of("userId", creatorUser.getId(), "rank", "STANDARD")));

        userManagementServlet.service(revokeCtx.request, revokeCtx.response);
        assertEquals(200, revokeCtx.status);

        AppUser refreshedCreator = userDao.findById(creatorUser.getId());
        assertNotNull(refreshedCreator);
        assertEquals("STANDARD", refreshedCreator.getRank());
        assertFalse(refreshedCreator.canPublishDirectly());

        // 3. Creator tries to create quiz -> must be PENDING
        TestContext createCtx = new TestContext();
        createCtx.setMethod("POST");
        createCtx.sessionHolder.attributes.put("user", refreshedCreator);

        Quiz payload = new Quiz();
        payload.setTitle("After Revocation Quiz");
        payload.setDurationSeconds(300);
        payload.setPublic(true);

        Question q = new Question();
        q.setQuestionText("Q1?");
        q.setOptionA("A");
        q.setOptionB("B");
        q.setOptionC("C");
        q.setOptionD("D");
        q.setCorrectOption('A');
        q.setPoints(10);
        payload.setQuestions(List.of(q));
        createCtx.setBody(JsonUtil.toJson(payload));

        createQuizServlet.service(createCtx.request, createCtx.response);
        assertEquals(201, createCtx.status);

        Map<?, ?> resMap = JsonUtil.fromJson(createCtx.getResponseBody(), Map.class);
        Map<?, ?> data = (Map<?, ?>) resMap.get("data");
        int qId = ((Number) data.get("id")).intValue();
        createdQuizIds.add(qId);

        Quiz persisted = quizDao.findById(qId);
        assertEquals(QuizStatus.PENDING, persisted.getStatus(), "After rank revocation, quiz must be PENDING");
    }

    @Test
    @DisplayName("Pending quiz access code is protected from students but accessible by author and admin")
    void testPendingQuizAccessCodePermissions() throws Exception {
        long stamp = System.currentTimeMillis();
        String pendingCode = "PND" + (stamp % 1000000);

        Quiz q = new Quiz();
        q.setTitle("Unapproved Draft Assessment");
        q.setDurationSeconds(300);
        q.setCreatorId(creatorUser.getId());
        q.setStatus(QuizStatus.PENDING);
        q.setAccessCode(pendingCode);
        q.setPublic(true);

        Question question = new Question();
        question.setQuestionText("Question text?");
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setOptionD("D");
        question.setCorrectOption('A');
        question.setPoints(10);
        q.setQuestions(List.of(question));

        q = quizDao.createWithQuestions(q);
        createdQuizIds.add(q.getId());

        // 1. Student queries pending quiz by code -> 403 Forbidden
        TestContext studentCtx = new TestContext();
        studentCtx.setMethod("GET");
        studentCtx.setParameter("code", pendingCode);
        studentCtx.sessionHolder.attributes.put("user", studentUser);

        listQuizzesServlet.service(studentCtx.request, studentCtx.response);
        assertEquals(403, studentCtx.status);

        // 2. Creator (author) queries pending quiz by code -> 200 OK
        TestContext authorCtx = new TestContext();
        authorCtx.setMethod("GET");
        authorCtx.setParameter("code", pendingCode);
        authorCtx.sessionHolder.attributes.put("user", creatorUser);

        listQuizzesServlet.service(authorCtx.request, authorCtx.response);
        assertEquals(200, authorCtx.status);

        // 3. Admin queries pending quiz by code -> 200 OK
        TestContext adminCtx = new TestContext();
        adminCtx.setMethod("GET");
        adminCtx.setParameter("code", pendingCode);
        adminCtx.sessionHolder.attributes.put("user", adminUser);

        listQuizzesServlet.service(adminCtx.request, adminCtx.response);
        assertEquals(200, adminCtx.status);
    }

    @Test
    @DisplayName("Access code is auto-generated if omitted and auto-uppercased if provided in lowercase")
    void testAccessCodeAutoGenerationAndUppercasing() throws Exception {
        // 1. Provided lowercase code: "lower123"
        Quiz q1 = new Quiz();
        q1.setTitle("Lowercase Code Quiz");
        q1.setDurationSeconds(300);
        q1.setCreatorId(creatorUser.getId());
        q1.setAccessCode("lower" + (System.currentTimeMillis() % 10000));
        q1.setStatus(QuizStatus.APPROVED);

        Question qu1 = new Question();
        qu1.setQuestionText("Prompt?");
        qu1.setOptionA("A");
        qu1.setOptionB("B");
        qu1.setOptionC("C");
        qu1.setOptionD("D");
        qu1.setCorrectOption('A');
        qu1.setPoints(10);
        q1.setQuestions(List.of(qu1));

        com.quizlive.service.QuizService service = new com.quizlive.service.QuizService(quizDao, new com.quizlive.dao.impl.QuestionDaoImpl());
        Quiz created1 = service.createQuiz(q1);
        createdQuizIds.add(created1.getId());
        assertTrue(created1.getAccessCode().startsWith("LOWER"), "Access code should be converted to uppercase");

        // 2. Omitted code -> auto-generated code
        Quiz q2 = new Quiz();
        q2.setTitle("Auto Generated Code Quiz");
        q2.setDurationSeconds(300);
        q2.setCreatorId(creatorUser.getId());
        q2.setAccessCode(null);
        q2.setStatus(QuizStatus.APPROVED);

        Question qu2 = new Question();
        qu2.setQuestionText("Prompt 2?");
        qu2.setOptionA("A");
        qu2.setOptionB("B");
        qu2.setOptionC("C");
        qu2.setOptionD("D");
        qu2.setCorrectOption('A');
        qu2.setPoints(10);
        q2.setQuestions(List.of(qu2));

        Quiz created2 = service.createQuiz(q2);
        createdQuizIds.add(created2.getId());
        assertNotNull(created2.getAccessCode(), "Access code should be automatically generated");
        assertEquals(7, created2.getAccessCode().length(), "Auto-generated access code should be 7 characters (e.g. QZ-XXXX)");
        assertTrue(created2.getAccessCode().startsWith("QZ-"), "Auto-generated access code should start with QZ- prefix");
    }

    @Test
    @DisplayName("Verified creator can directly publish an existing PENDING quiz even with stale session")
    void testVerifiedCreatorApprovesExistingPendingQuizDirectly() throws Exception {
        // 1. Creator creates a quiz under STANDARD rank -> status is PENDING
        Quiz q = new Quiz();
        q.setTitle("Pending Midterm");
        q.setDurationSeconds(300);
        q.setCreatorId(creatorUser.getId());
        q.setStatus(QuizStatus.PENDING);
        q.setAccessCode("MID" + (System.currentTimeMillis() % 1000000));
        q.setPublic(true);

        Question qu = new Question();
        qu.setQuestionText("Test prompt?");
        qu.setOptionA("A");
        qu.setOptionB("B");
        qu.setOptionC("C");
        qu.setOptionD("D");
        qu.setCorrectOption('A');
        qu.setPoints(10);
        q.setQuestions(List.of(qu));

        Quiz created = quizDao.createWithQuestions(q);
        createdQuizIds.add(created.getId());
        assertEquals(QuizStatus.PENDING, created.getStatus());

        // 2. Admin promotes creator to VERIFIED in database
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);

        // 3. Creator session still holds stale user (with STANDARD rank)
        AppUser staleCreatorSessionUser = AppUser.create(creatorUser.getId(), creatorUser.getName(),
                creatorUser.getEmail(), creatorUser.getPasswordHash(), creatorUser.getSalt(),
                Role.CREATOR, "STANDARD", creatorUser.getCreatedAt());

        TestContext approveCtx = new TestContext();
        approveCtx.setMethod("POST");
        approveCtx.requestUri = "/api/quizzes/approve";
        approveCtx.sessionHolder.attributes.put("user", staleCreatorSessionUser);

        Map<String, Object> body = Map.of(
                "quizId", created.getId(),
                "action", "approve"
        );
        approveCtx.setBody(JsonUtil.toJson(body));

        approveQuizServlet.service(approveCtx.request, approveCtx.response);

        // 4. Verify approval succeeds (HTTP 200) and quiz status is APPROVED
        assertEquals(200, approveCtx.status);
        Quiz refreshed = quizDao.findById(created.getId());
        assertNotNull(refreshed);
        assertEquals(QuizStatus.APPROVED, refreshed.getStatus(), "Pending quiz should be approved directly by verified creator");
    }

    @Test
    @DisplayName("Creator cannot approve another creator's quiz -> 403 Forbidden")
    void testCreatorCannotApproveAnotherCreatorsQuiz() throws Exception {
        // 1. Create a second creator
        long stamp = System.currentTimeMillis();
        AppUser otherCreator = AppUser.create(0, "Other Teacher " + stamp, "other_" + stamp + "@test.com",
                "dummyhash", "dummysalt", Role.CREATOR, "VERIFIED", null);
        otherCreator = userDao.create(otherCreator);
        createdUserIds.add(otherCreator.getId());

        // 2. Creator 1 creates a pending quiz
        Quiz q = new Quiz();
        q.setTitle("Teacher 1 Quiz");
        q.setDurationSeconds(300);
        q.setCreatorId(creatorUser.getId());
        q.setStatus(QuizStatus.PENDING);
        q.setAccessCode("TCH" + (stamp % 1000000));
        q.setPublic(true);

        Question qu = new Question();
        qu.setQuestionText("Question?");
        qu.setOptionA("A");
        qu.setOptionB("B");
        qu.setOptionC("C");
        qu.setOptionD("D");
        qu.setCorrectOption('A');
        qu.setPoints(10);
        q.setQuestions(List.of(qu));

        Quiz created = quizDao.createWithQuestions(q);
        createdQuizIds.add(created.getId());

        // 3. Other creator attempts to approve creator 1's quiz
        TestContext ctx = new TestContext();
        ctx.setMethod("POST");
        ctx.requestUri = "/api/quizzes/approve";
        ctx.sessionHolder.attributes.put("user", otherCreator);

        Map<String, Object> body = Map.of(
                "quizId", created.getId(),
                "action", "approve"
        );
        ctx.setBody(JsonUtil.toJson(body));

        approveQuizServlet.service(ctx.request, ctx.response);

        assertEquals(403, ctx.status, "Creator cannot approve another creator's quiz");
    }

    @Test
    @DisplayName("Creator can toggle quiz visibility (public vs private/unlisted) via HostQuizServlet")
    void testToggleQuizVisibilityViaHostServlet() throws Exception {
        creatorUser.setRank("VERIFIED");
        userDao.update(creatorUser);

        long stamp = System.currentTimeMillis();
        String code = "VIS" + (stamp % 1000000);
        Quiz quiz = createQuizForTest("Visibility Toggle Test", code, true);
        createdQuizIds.add(quiz.getId());
        assertTrue(quiz.isPublic());

        // 1. Toggle to Private
        TestContext hideCtx = new TestContext();
        hideCtx.setMethod("POST");
        hideCtx.requestUri = "/api/quizzes/host";
        hideCtx.sessionHolder.attributes.put("user", creatorUser);
        hideCtx.setBody(JsonUtil.toJson(Map.of(
                "quizId", quiz.getId(),
                "action", "visibility",
                "isPublic", false
        )));

        hostQuizServlet.service(hideCtx.request, hideCtx.response);
        assertEquals(200, hideCtx.status);

        Quiz updatedQuiz = quizDao.findById(quiz.getId());
        assertNotNull(updatedQuiz);
        assertFalse(updatedQuiz.isPublic(), "Quiz should now be private/unlisted");

        // Verify unlisted in public catalog
        List<Quiz> publicQuizzes = quizDao.listApprovedPublic();
        boolean foundInPublic = publicQuizzes.stream().anyMatch(qz -> qz.getId() == quiz.getId());
        assertFalse(foundInPublic, "Unlisted quiz must not appear in public catalog");

        // 2. Toggle back to Public
        TestContext pubCtx = new TestContext();
        pubCtx.setMethod("POST");
        pubCtx.requestUri = "/api/quizzes/host";
        pubCtx.sessionHolder.attributes.put("user", creatorUser);
        pubCtx.setBody(JsonUtil.toJson(Map.of(
                "quizId", quiz.getId(),
                "action", "visibility",
                "isPublic", true
        )));

        hostQuizServlet.service(pubCtx.request, pubCtx.response);
        assertEquals(200, pubCtx.status);

        Quiz republishedQuiz = quizDao.findById(quiz.getId());
        assertNotNull(republishedQuiz);
        assertTrue(republishedQuiz.isPublic(), "Quiz should now be public");
    }

    @Test
    @DisplayName("Attempting to join completed quiz via code returns 409 Conflict with quizId in payload")
    void testStartAttemptConflictReturnsQuizIdWhenJoiningByCode() throws Exception {
        long stamp = System.currentTimeMillis();
        String code = "CNF" + (stamp % 1000000);
        Quiz quiz = createQuizForTest("Conflict Test Quiz", code, true);
        createdQuizIds.add(quiz.getId());

        // 1. Student starts attempt
        TestContext startCtx = new TestContext();
        startCtx.setMethod("POST");
        startCtx.requestUri = "/api/attempts/start";
        startCtx.sessionHolder.attributes.put("user", studentUser);
        startCtx.setBody(JsonUtil.toJson(Map.of("code", code)));

        startAttemptServlet.service(startCtx.request, startCtx.response);
        assertEquals(201, startCtx.status);

        Map<?, ?> startRes = JsonUtil.fromJson(startCtx.getResponseBody(), Map.class);
        Map<?, ?> startData = (Map<?, ?>) startRes.get("data");
        int attemptId = ((Number) startData.get("attemptId")).intValue();
        createdAttemptIds.add(attemptId);

        // 2. Student completes attempt
        com.quizlive.model.Attempt attemptToSubmit = attemptDao.findById(attemptId);
        assertNotNull(attemptToSubmit);
        attemptToSubmit.setStatus(com.quizlive.model.enums.AttemptStatus.SUBMITTED);
        attemptToSubmit.setScore(10);
        attemptDao.submitAttempt(attemptToSubmit, List.of());

        // 3. Student tries to start again using access code
        TestContext repeatCtx = new TestContext();
        repeatCtx.setMethod("POST");
        repeatCtx.requestUri = "/api/attempts/start";
        repeatCtx.sessionHolder.attributes.put("user", studentUser);
        repeatCtx.setBody(JsonUtil.toJson(Map.of("code", code)));

        startAttemptServlet.service(repeatCtx.request, repeatCtx.response);

        // 4. Verify 409 Conflict and presence of quizId
        assertEquals(409, repeatCtx.status);
        Map<?, ?> repeatRes = JsonUtil.fromJson(repeatCtx.getResponseBody(), Map.class);
        assertNotNull(repeatRes);
        Map<?, ?> repeatData = (Map<?, ?>) repeatRes.get("data");
        assertNotNull(repeatData, "409 Conflict payload must contain data map");
        assertEquals(quiz.getId(), ((Number) repeatData.get("quizId")).intValue(),
                "409 Conflict payload must include quizId so client can redirect to correct leaderboard");
    }

    private Quiz createQuizForTest(String title, String accessCode, boolean isPublic) throws SQLException {
        Quiz q = new Quiz();
        q.setTitle(title);
        q.setDescription("Test quiz description");
        q.setDurationSeconds(300);
        q.setCreatorId(creatorUser.getId());
        q.setStatus(QuizStatus.APPROVED);
        q.setAccessCode(accessCode);
        q.setPublic(isPublic);

        Question question = new Question();
        question.setQuestionText("What is H2O?");
        question.setOptionA("Water");
        question.setOptionB("Gold");
        question.setOptionC("Iron");
        question.setOptionD("Salt");
        question.setCorrectOption('A');
        question.setPoints(10);
        q.setQuestions(List.of(question));

        return quizDao.createWithQuestions(q);
    }

    private static class TestContext {
        private String method = "GET";
        private String requestUri = "/api/quizzes";
        private final String contextPath = "";
        private String contentType = "application/json";
        private String body = "";
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String[]> parameters = new HashMap<>();
        private int status = 200;
        private String redirectUrl;
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
                        case "removeAttribute" -> {
                            sessionHolder.attributes.remove((String) args[0]);
                            yield null;
                        }
                        case "invalidate" -> {
                            sessionHolder.invalidated = true;
                            sessionHolder.attributes.clear();
                            yield null;
                        }
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );

            this.request = (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, m, args) -> switch (m.getName()) {
                        case "getMethod" -> method;
                        case "getContextPath" -> contextPath;
                        case "getRequestURI" -> requestUri;
                        case "getContentType" -> contentType;
                        case "getHeader" -> headers.get((String) args[0]);
                        case "getParameter" -> {
                            String[] vals = parameters.get((String) args[0]);
                            yield (vals != null && vals.length > 0) ? vals[0] : null;
                        }
                        case "getParameterMap" -> parameters;
                        case "getReader" -> new BufferedReader(new StringReader(body));
                        case "getSession" -> {
                            boolean create = (args != null && args.length > 0) ? (Boolean) args[0] : true;
                            yield session;
                        }
                        default -> defaultPrimitive(m.getReturnType());
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
                        case "sendRedirect" -> {
                            redirectUrl = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> new PrintWriter(responseWriter);
                        case "setContentType", "setCharacterEncoding" -> null;
                        default -> defaultPrimitive(m.getReturnType());
                    }
            );
        }

        void setMethod(String method) {
            this.method = method;
        }

        void setBody(String body) {
            this.body = body;
        }

        void setParameter(String name, String value) {
            parameters.put(name, new String[]{value});
        }

        String getResponseBody() {
            return responseWriter.toString();
        }
    }

    private static class SessionHolder {
        final Map<String, Object> attributes = new HashMap<>();
        boolean invalidated = false;
    }

    private static Object defaultPrimitive(Class<?> returnType) {
        if (returnType.equals(boolean.class)) return false;
        if (returnType.equals(int.class)) return 0;
        if (returnType.equals(long.class)) return 0L;
        if (returnType.equals(double.class)) return 0.0;
        if (returnType.equals(float.class)) return 0.0f;
        return null;
    }
}
