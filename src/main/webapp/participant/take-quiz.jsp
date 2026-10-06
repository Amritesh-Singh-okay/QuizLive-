<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "Live Exam Runner - QuizLive");
    request.setAttribute("activeNav", "dashboard");
    String quizIdParam = request.getParameter("quizId");
    int safeQuizId = 1;
    if (quizIdParam != null && !quizIdParam.trim().isEmpty()) {
        try {
            safeQuizId = Integer.parseInt(quizIdParam.trim());
        } catch (NumberFormatException ignored) {}
    }
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 900px;">
    <!-- Anti-Cheat Status Header -->
    <div class="anti-cheat-bar" id="anti-cheat-bar">
        <div class="anti-cheat-indicator">
            <span style="font-size: 1.3rem;">🛡️</span>
            <span>PROCTORED LIVE ENVIRONMENT</span>
        </div>
        <div style="display: flex; align-items: center; gap: 0.75rem;">
            <span style="font-size: 0.85rem; color: var(--text-secondary);">Tab Violations:</span>
            <span class="violation-counter" id="tab-switch-count">0</span>
            <span style="font-size: 0.8rem; color: var(--text-muted);">/ 3 allowed</span>
        </div>
    </div>

    <!-- Proctor Flag Alert Banner -->
    <div id="proctor-flagged-banner" class="alert alert-danger" style="display: none; margin-bottom: 1.5rem;">
        <span style="font-size: 1.4rem;">🚨</span>
        <div>
            <strong>FLAGGED FOR REVIEW:</strong>
            You have switched tabs 3 or more times. Your quiz session has been marked with a cheat violation on the proctor dashboard!
        </div>
    </div>

    <!-- Exam Info & Timer Header -->
    <div class="card" style="margin-bottom: 1.5rem;">
        <div class="card-body" style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.25rem 1.5rem;">
            <div>
                <h1 id="quiz-title" style="font-size: 1.45rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.25rem;">
                    Loading Quiz...
                </h1>
                <p id="quiz-desc" style="font-size: 0.875rem; color: var(--text-secondary);">
                    Please wait while your session initializes...
                </p>
            </div>
            <div class="timer-box">
                <div style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); font-weight: 700; margin-bottom: 0.2rem;">
                    Time Remaining
                </div>
                <div class="timer-digits" id="timer-display">--:--</div>
            </div>
        </div>
    </div>

    <!-- Question Palette -->
    <div style="margin-bottom: 1rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem;">
            <span style="font-size: 0.85rem; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.05em;">
                Question Navigator
            </span>
            <span style="font-size: 0.85rem; color: var(--text-muted);" id="answered-count">0 answered</span>
        </div>
        <div class="question-palette" id="question-palette">
            <!-- Pill buttons rendered by JS -->
        </div>
    </div>

    <!-- Active Question Card -->
    <div class="card" id="question-card" style="margin-bottom: 1.5rem;">
        <div class="card-header" style="justify-content: space-between;">
            <div style="font-weight: 700; font-size: 0.95rem; color: var(--text-secondary);">
                Question <span id="current-q-num">1</span> of <span id="total-q-num">--</span>
            </div>
            <div>
                <span class="badge" style="background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--secondary);" id="question-points">
                    10 Points
                </span>
            </div>
        </div>

        <div class="card-body">
            <h2 id="question-prompt" style="font-size: 1.25rem; font-weight: 600; line-height: 1.4; margin-bottom: 1.5rem; color: var(--text-primary);">
                Loading question...
            </h2>

            <div id="options-container">
                <!-- Option cards rendered by JS -->
            </div>
        </div>

        <div class="card-footer">
            <button type="button" id="prev-btn" class="btn btn-secondary" disabled>
                &larr; Previous
            </button>
            <div style="display: flex; gap: 0.75rem;">
                <button type="button" id="next-btn" class="btn btn-secondary">
                    Next &rarr;
                </button>
                <button type="button" id="submit-quiz-btn" class="btn btn-success">
                    ✓ Submit Quiz
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Submit Confirmation Modal -->
<div id="confirm-modal" class="modal-overlay">
    <div class="modal-content">
        <div class="card-header">
            <h3 class="card-title">Confirm Quiz Submission</h3>
        </div>
        <div class="card-body">
            <p style="color: var(--text-secondary); margin-bottom: 1rem;">
                Are you sure you want to finalize and submit your answers? Once submitted, your score will be calculated and broadcast immediately to the live leaderboard.
            </p>
            <p id="unanswered-warning" style="display: none; color: var(--warning); font-size: 0.9rem; font-weight: 600;">
                ⚠️ You still have unanswered questions!
            </p>
        </div>
        <div class="card-footer" style="justify-content: flex-end; gap: 0.75rem;">
            <button type="button" class="btn btn-secondary" onclick="closeConfirmModal()">Keep Working</button>
            <button type="button" class="btn btn-success" id="final-submit-btn" onclick="executeSubmission()">Yes, Submit Now</button>
        </div>
    </div>
</div>

<!-- Results Modal -->
<div id="results-modal" class="modal-overlay">
    <div class="modal-content" style="max-width: 480px; text-align: center;">
        <div class="card-header" style="justify-content: center; background: rgba(16, 185, 129, 0.1);">
            <h2 class="card-title" style="color: var(--success); font-size: 1.4rem;">🎉 Quiz Completed!</h2>
        </div>
        <div class="card-body">
            <div style="font-size: 3.5rem; font-weight: 900; color: var(--text-primary); line-height: 1; margin: 1rem 0 0.5rem;" id="final-percentage">
                --%
            </div>
            <p style="font-size: 1.1rem; color: var(--text-secondary); margin-bottom: 1.5rem;">
                Your Score: <strong id="final-score" style="color: var(--text-primary);">--</strong> / <span id="final-max-score">--</span> points
            </p>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; text-align: left;">
                <div style="background: var(--bg-surface); padding: 0.9rem; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
                    <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Status</div>
                    <div style="font-weight: 700; color: var(--success); font-size: 1rem;" id="final-status">SUBMITTED</div>
                </div>
                <div style="background: var(--bg-surface); padding: 0.9rem; border-radius: var(--radius-md); border: 1px solid var(--border-color);">
                    <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Tab Switches</div>
                    <div style="font-weight: 700; font-size: 1rem;" id="final-violations">0</div>
                </div>
            </div>
        </div>
        <div class="card-footer" style="flex-direction: column; gap: 0.75rem;">
            <a id="view-leaderboard-btn" href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-primary btn-block btn-lg">
                🏆 View Live Leaderboard
            </a>
            <a href="<%= request.getContextPath() %>/participant/dashboard.jsp" class="btn btn-outline btn-block">
                Return to Dashboard
            </a>
        </div>
    </div>
</div>

<script>
    window.QUIZ_CONFIG = {
        contextPath: '<%= request.getContextPath() %>',
        quizId: <%= safeQuizId %>
    };
</script>
<script src="<%= request.getContextPath() %>/js/quiz-runner.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
