<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "Exam Runner - QuizLive");
    request.setAttribute("activeNav", "dashboard");
    String quizIdParam = request.getParameter("quizId");
    String codeParam = request.getParameter("code");
    if (codeParam == null || codeParam.trim().isEmpty()) {
        codeParam = request.getParameter("accessCode");
    }
    int safeQuizId = 0;
    if (quizIdParam != null && !quizIdParam.trim().isEmpty()) {
        try {
            safeQuizId = Integer.parseInt(quizIdParam.trim());
        } catch (NumberFormatException ignored) {}
    }
    String cleanCode = (codeParam != null) ? codeParam.trim().toUpperCase().replaceAll("[^a-zA-Z0-9_-]", "") : "";
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<style>
@keyframes pulse-ring {
    0% { transform: scale(0.92); opacity: 0.8; }
    50% { transform: scale(1.35); opacity: 0.2; }
    100% { transform: scale(0.92); opacity: 0.8; }
}
@keyframes lobby-blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.35; }
}
</style>

<div class="container" style="max-width: 880px;">
    <!-- Waiting Room (Lobby) Section -->
    <div id="waiting-room-container" style="display: none;">
        <div class="card" style="text-align: center; padding: 3.5rem 2rem; margin-bottom: 2rem; box-shadow: var(--shadow-lg);">
            <!-- Live Pulse Beacon -->
            <div style="position: relative; width: 84px; height: 84px; margin: 0 auto 1.75rem; display: flex; align-items: center; justify-content: center;">
                <div style="position: absolute; width: 100%; height: 100%; border-radius: 50%; background: rgba(96, 82, 122, 0.25); animation: pulse-ring 2s infinite cubic-bezier(0.215, 0.61, 0.355, 1);"></div>
                <div style="position: relative; width: 56px; height: 56px; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; color: #fff; box-shadow: 0 4px 14px rgba(96, 82, 122, 0.4);">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                </div>
            </div>

            <div style="display: inline-block; padding: 0.35rem 0.85rem; border-radius: var(--radius-sm); background: var(--primary-light); color: var(--primary); font-size: 0.8rem; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase; margin-bottom: 0.85rem;">
                Classroom Waiting Room
            </div>

            <h1 id="waiting-quiz-title" style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.5rem;">
                Waiting for Quiz Host...
            </h1>
            <p id="waiting-quiz-desc" style="color: var(--text-secondary); max-width: 580px; margin: 0 auto 2rem; font-size: 1rem; line-height: 1.6;">
                The instructor is currently letting students join the room. Please keep this screen open. Your assessment will launch automatically the moment the host clicks <strong>Start Quiz</strong>.
            </p>

            <!-- Live Stats Row -->
            <div style="display: flex; justify-content: center; gap: 1.5rem; flex-wrap: wrap; margin-bottom: 2rem;">
                <div style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem 2rem; min-width: 170px;">
                    <div style="font-size: 2.5rem; font-weight: 800; color: var(--primary); line-height: 1;" id="waiting-room-count">
                        1
                    </div>
                    <div style="font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-muted); font-weight: 600; margin-top: 0.35rem;">
                        Students in Lobby
                    </div>
                </div>

                <div id="scheduled-start-card" style="display: none; background: var(--bg-surface-alt); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem 2rem; min-width: 170px;">
                    <div style="font-size: 1.75rem; font-weight: 800; color: var(--text-primary); line-height: 1.2;" id="scheduled-countdown">
                        --:--
                    </div>
                    <div style="font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-muted); font-weight: 600; margin-top: 0.35rem;">
                        Scheduled Start
                    </div>
                </div>
            </div>

            <!-- Status Indicator -->
            <div style="display: inline-flex; align-items: center; gap: 0.6rem; background: var(--success-bg); border: 1px solid var(--success-border); padding: 0.5rem 1.25rem; border-radius: var(--radius-sm); color: var(--success); font-size: 0.9rem; font-weight: 600;">
                <span style="width: 8px; height: 8px; border-radius: 50%; background: var(--success); display: inline-block; animation: lobby-blink 1.2s infinite ease-in-out;"></span>
                <span id="lobby-status-text">Connected &mdash; Waiting for Host</span>
            </div>
        </div>
    </div>

    <!-- Active Exam Container -->
    <div id="exam-active-container">
        <!-- Anti-Cheat Status Header -->
        <div class="anti-cheat-bar" id="anti-cheat-bar">
            <div class="anti-cheat-indicator">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
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
            <span style="display: inline-flex; align-items: center; flex-shrink: 0;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#A3383B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
            </span>
            <div>
                <strong>FLAGGED FOR REVIEW:</strong>
                You have switched tabs 3 or more times. Your session has been flagged on the proctor dashboard for review.
            </div>
        </div>

        <!-- Exam Info & Timer Header -->
        <div class="card" style="margin-bottom: 1.5rem;">
            <div class="card-body" style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; padding: 1.25rem 1.5rem;">
                <div>
                    <h1 id="quiz-title" style="font-size: 1.35rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.25rem;">
                        Loading Quiz...
                    </h1>
                    <p id="quiz-desc" style="font-size: 0.875rem; color: var(--text-secondary);">
                        Please wait while your session initializes...
                    </p>
                </div>
                <div class="timer-box">
                    <div style="font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--text-muted); font-weight: 700; margin-bottom: 0.2rem;">
                        Time Remaining
                    </div>
                    <div class="timer-digits" id="timer-display">--:--</div>
                </div>
            </div>
        </div>

        <!-- Question Palette -->
        <div style="margin-bottom: 1rem;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem;">
                <span style="font-size: 0.8rem; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.04em;">
                    Question Navigator
                </span>
                <span style="font-size: 0.85rem; color: var(--text-muted);" id="answered-count">0 answered</span>
            </div>
            <div class="question-palette" id="question-palette">
                <!-- Palette buttons rendered by JS -->
            </div>
        </div>

        <!-- Active Question Card -->
        <div class="card" id="question-card" style="margin-bottom: 1.5rem;">
            <div class="card-header" style="justify-content: space-between;">
                <div style="font-weight: 700; font-size: 0.95rem; color: var(--text-secondary);">
                    Question <span id="current-q-num">1</span> of <span id="total-q-num">--</span>
                </div>
                <div>
                    <span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary);" id="question-points">
                        10 Points
                    </span>
                </div>
            </div>

            <div class="card-body">
                <h2 id="question-prompt" style="font-size: 1.2rem; font-weight: 600; line-height: 1.45; margin-bottom: 1.5rem; color: var(--text-primary);">
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
                        Submit Quiz
                    </button>
                </div>
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
            <p style="color: var(--text-secondary); margin-bottom: 1rem; line-height: 1.6;">
                Are you sure you want to finalize and submit your answers? Once submitted, your score will be calculated and broadcast immediately to the live leaderboard.
            </p>
            <p id="unanswered-warning" style="display: none; color: var(--warning); font-size: 0.9rem; font-weight: 600;">
                You still have unanswered questions.
            </p>
        </div>
        <div class="card-footer" style="justify-content: flex-end; gap: 0.75rem;">
            <button type="button" class="btn btn-secondary" onclick="closeConfirmModal()">Keep Working</button>
            <button type="button" class="btn btn-success" id="final-submit-btn" onclick="executeSubmission()">Submit Now</button>
        </div>
    </div>
</div>

<!-- Results Modal -->
<div id="results-modal" class="modal-overlay">
    <div class="modal-content" style="max-width: 480px; text-align: center;">
        <div class="card-header" style="justify-content: center; background: var(--success-bg);">
            <h2 class="card-title" style="color: var(--success); font-size: 1.35rem;">Assessment Completed</h2>
        </div>
        <div class="card-body">
            <div style="font-size: 3rem; font-weight: 800; color: var(--text-primary); line-height: 1; margin: 1rem 0 0.5rem;" id="final-percentage">
                --%
            </div>
            <p style="font-size: 1rem; color: var(--text-secondary); margin-bottom: 1.5rem;">
                Your Score: <strong id="final-score" style="color: var(--text-primary);">--</strong> / <span id="final-max-score">--</span> points
            </p>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; text-align: left;">
                <div style="background: var(--bg-surface-alt); padding: 0.85rem 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
                    <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Status</div>
                    <div style="font-weight: 700; color: var(--success); font-size: 0.95rem;" id="final-status">SUBMITTED</div>
                </div>
                <div style="background: var(--bg-surface-alt); padding: 0.85rem 1rem; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
                    <div style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase;">Tab Switches</div>
                    <div style="font-weight: 700; font-size: 0.95rem;" id="final-violations">0</div>
                </div>
            </div>
        </div>
        <div class="card-footer" style="flex-direction: column; gap: 0.65rem;">
            <a id="view-leaderboard-btn" href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-primary btn-block btn-lg">
                View Live Leaderboard
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
        quizId: <%= safeQuizId %>,
        code: '<%= cleanCode %>'
    };
</script>
<script src="<%= request.getContextPath() %>/js/quiz-runner.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
