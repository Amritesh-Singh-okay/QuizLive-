<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Create Quiz - QuizLive");
    request.setAttribute("activeNav", "create-quiz");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 880px;">
    <div style="margin-bottom: 2rem;">
        <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.35rem;">
            Assessment Authoring Studio
        </h1>
        <p style="color: var(--text-secondary); font-size: 0.95rem;">
            Define assessment parameters, author multiple-choice questions, and set the proctored duration.
        </p>
    </div>

    <form id="create-quiz-form" data-context-path="<%= request.getContextPath() %>">
        <!-- Basic Settings Card -->
        <div class="card" style="margin-bottom: 2rem;">
            <div class="card-header">
                <h2 class="card-title">1. General Information</h2>
            </div>
            <div class="card-body">
                <div class="form-group">
                    <label for="quiz-title" class="form-label">Quiz Title *</label>
                    <input type="text" id="quiz-title" class="form-control" placeholder="e.g. Clinical Pharmacology Assessment" required>
                </div>

                <div class="form-group">
                    <label for="quiz-desc" class="form-label">Description / Instructions</label>
                    <textarea id="quiz-desc" class="form-textarea" placeholder="Provide test instructions, topics covered, or clinical notes..."></textarea>
                </div>

                <div class="form-group">
                    <label for="quiz-duration" class="form-label">Proctored Duration (Seconds) *</label>
                    <input type="number" id="quiz-duration" class="form-control" value="300" min="30" max="7200" required>
                    <p class="form-help">Total test duration in seconds (e.g. 300 = 5 minutes, 600 = 10 minutes).</p>
                </div>

                <div style="margin-top: 1.5rem; padding: 1.25rem; background: var(--bg-surface-alt); border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
                    <div style="font-weight: 700; font-size: 0.95rem; color: var(--text-primary); margin-bottom: 0.75rem; display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                            <line x1="8" y1="21" x2="16" y2="21"></line>
                            <line x1="12" y1="17" x2="12" y2="21"></line>
                        </svg>
                        Live Classroom &amp; Waiting Room Mode
                    </div>

                    <div style="display: flex; align-items: flex-start; gap: 0.6rem; margin-bottom: 1rem;">
                        <input type="checkbox" id="quiz-is-held" style="margin-top: 0.25rem; width: 16px; height: 16px; accent-color: var(--primary);">
                        <label for="quiz-is-held" style="font-size: 0.9rem; color: var(--text-primary); cursor: pointer;">
                            <strong>Hold Quiz in Waiting Room (Live Classroom Mode)</strong>
                            <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 0.2rem;">
                                Students will wait on a lobby screen until you click "Start Quiz Now" from your dashboard (or until the scheduled start time arrives).
                            </div>
                        </label>
                    </div>

                    <div class="form-group" style="margin-bottom: 0;">
                        <label for="quiz-scheduled-time" class="form-label" style="font-size: 0.85rem;">Optional: Scheduled Start Date &amp; Time</label>
                        <input type="datetime-local" id="quiz-scheduled-time" class="form-control" style="max-width: 320px;">
                        <p class="form-help">If specified, students wait in lobby until this exact time or until you start manually.</p>
                    </div>
                </div>
            </div>
        </div>

        <!-- Questions Builder Section -->
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem;">
            <h2 style="font-size: 1.35rem; font-weight: 700;">2. Questions (<span id="question-count-badge">0</span>)</h2>
            <button type="button" class="btn btn-secondary btn-sm" id="add-question-btn">
                + Add Another Question
            </button>
        </div>

        <div id="questions-container" style="display: flex; flex-direction: column; gap: 1.5rem; margin-bottom: 2rem;">
            <!-- Question cards appended dynamically -->
        </div>

        <div style="display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color); margin-bottom: 3rem;">
            <a href="<%= request.getContextPath() %>/creator/dashboard.jsp" class="btn btn-outline">
                Cancel
            </a>
            <div style="display: flex; gap: 0.75rem;">
                <button type="button" class="btn btn-secondary" id="add-question-btn-bottom">
                    + Add Question
                </button>
                <button type="submit" id="save-quiz-btn" class="btn btn-primary btn-lg">
                    Publish Quiz for Approval
                </button>
            </div>
        </div>
    </form>
</div>

<script src="<%= request.getContextPath() %>/js/creator.js"></script>
<script src="<%= request.getContextPath() %>/js/create-quiz.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
