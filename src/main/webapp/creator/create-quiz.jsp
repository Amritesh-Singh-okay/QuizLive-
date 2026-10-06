<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Create Quiz - QuizLive");
    request.setAttribute("activeNav", "create-quiz");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 900px;">
    <div style="margin-bottom: 2rem;">
        <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.35rem;">
            Dynamic Quiz Builder ✍️
        </h1>
        <p style="color: var(--text-secondary); font-size: 0.95rem;">
            Define assessment parameters, author multiple-choice questions, and set the proctored duration.
        </p>
    </div>

    <form id="create-quiz-form" data-context-path="<%= request.getContextPath() %>">
        <!-- Basic Settings Card -->
        <div class="card" style="margin-bottom: 2rem;">
            <div class="card-header">
                <h2 class="card-title">1. Quiz General Information</h2>
            </div>
            <div class="card-body">
                <div class="form-group">
                    <label for="quiz-title" class="form-label">Quiz Title *</label>
                    <input type="text" id="quiz-title" class="form-control" placeholder="e.g. Java Concurrency &amp; Multithreading Masterclass" required>
                </div>

                <div class="form-group">
                    <label for="quiz-desc" class="form-label">Description / Instructions</label>
                    <textarea id="quiz-desc" class="form-textarea" placeholder="Provide overview or instructions for test takers..."></textarea>
                </div>

                <div class="form-group">
                    <label for="quiz-duration" class="form-label">Proctored Duration (Seconds) *</label>
                    <input type="number" id="quiz-duration" class="form-control" value="300" min="30" max="7200" required>
                    <p class="form-help">Enter total test duration in seconds (e.g. 300 = 5 minutes, 600 = 10 minutes).</p>
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

        <div style="display: flex; align-items: center; justify-content: space-between; padding: 1.5rem; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border-color); margin-bottom: 3rem;">
            <a href="<%= request.getContextPath() %>/creator/dashboard.jsp" class="btn btn-outline">
                Cancel
            </a>
            <div style="display: flex; gap: 1rem;">
                <button type="button" class="btn btn-secondary" id="add-question-btn-bottom">
                    + Add Question
                </button>
                <button type="submit" id="save-quiz-btn" class="btn btn-primary btn-lg">
                    🚀 Publish Quiz for Approval
                </button>
            </div>
        </div>
    </form>
</div>

<script src="<%= request.getContextPath() %>/js/creator.js"></script>
<script src="<%= request.getContextPath() %>/js/create-quiz.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
