<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Live Leaderboard - QuizLive");
    request.setAttribute("activeNav", "leaderboard");
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

<div class="container">
    <!-- Header & Controls -->
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;">
        <div>
            <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.35rem;">
                <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary);">
                    Live Standings
                </h1>
                <span id="ws-status-badge" class="live-badge">
                    <span class="live-dot" id="ws-dot"></span>
                    <span id="ws-status-text">Connecting WebSocket...</span>
                </span>
            </div>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Live ranking updates synchronized instantly via WebSockets upon completion.
            </p>
        </div>

        <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;">
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                <label for="quiz-select" style="font-size: 0.85rem; font-weight: 700; color: var(--text-secondary);">Select Quiz:</label>
                <select id="quiz-select" class="form-select" style="width: auto; min-width: 220px;">
                    <option value="1">Loading quizzes...</option>
                </select>
            </div>
            <button type="button" class="btn btn-outline btn-sm" onclick="triggerRefresh()">
                Refresh
            </button>
        </div>
    </div>

    <!-- Podium Section -->
    <div class="podium-container" id="podium-container">
        <!-- Rank 2 -->
        <div class="podium-card podium-rank-2" id="podium-2">
            <div class="podium-title">Rank 2</div>
            <div class="podium-name" id="podium-2-name">--</div>
            <div class="podium-score" id="podium-2-score">-- pts</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.25rem;" id="podium-2-meta">--</div>
        </div>

        <!-- Rank 1 -->
        <div class="podium-card podium-rank-1" id="podium-1">
            <div class="podium-title" style="color: var(--warning);">Rank 1: Lead Participant</div>
            <div class="podium-name" id="podium-1-name" style="font-size: 1.25rem;">--</div>
            <div class="podium-score" id="podium-1-score" style="font-size: 1.75rem; color: var(--warning);">-- pts</div>
            <div style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.25rem;" id="podium-1-meta">--</div>
        </div>

        <!-- Rank 3 -->
        <div class="podium-card podium-rank-3" id="podium-3">
            <div class="podium-title">Rank 3</div>
            <div class="podium-name" id="podium-3-name">--</div>
            <div class="podium-score" id="podium-3-score">-- pts</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.25rem;" id="podium-3-meta">--</div>
        </div>
    </div>

    <!-- Complete Rankings Table -->
    <div class="card" style="margin-bottom: 2.5rem;">
        <div class="card-header">
            <h2 class="card-title">Participant Standings</h2>
            <span style="font-size: 0.85rem; color: var(--text-muted);" id="participant-count-badge">0 Ranked</span>
        </div>

        <div class="table-responsive">
            <table class="table" id="leaderboard-table">
                <thead>
                    <tr>
                        <th style="width: 80px;">Rank</th>
                        <th>Participant</th>
                        <th>Score</th>
                        <th>Accuracy</th>
                        <th>Duration</th>
                        <th>Integrity Flags</th>
                        <th>Submitted At</th>
                    </tr>
                </thead>
                <tbody id="leaderboard-tbody">
                    <tr>
                        <td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2.5rem;">
                            Connecting to live rankings...
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    window.LEADERBOARD_CONFIG = {
        contextPath: '<%= request.getContextPath() %>',
        initialQuizId: <%= (safeQuizId > 0 ? safeQuizId : 1) %>,
        code: '<%= cleanCode %>'
    };
</script>
<script src="<%= request.getContextPath() %>/js/leaderboard.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
