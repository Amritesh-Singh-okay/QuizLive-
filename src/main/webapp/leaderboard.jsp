<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Live Leaderboard - QuizLive");
    request.setAttribute("activeNav", "leaderboard");
    String quizIdParam = request.getParameter("quizId");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <!-- Header & Controls -->
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;">
        <div>
            <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.35rem;">
                <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary);">
                    Real-Time Live Leaderboard
                </h1>
                <span id="ws-status-badge" class="live-badge">
                    <span class="live-dot" id="ws-dot"></span>
                    <span id="ws-status-text">Connecting WebSocket...</span>
                </span>
            </div>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Live ranking updates pushed instantly via WebSockets upon quiz submission.
            </p>
        </div>

        <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;">
            <div style="display: flex; align-items: center; gap: 0.5rem;">
                <label for="quiz-select" style="font-size: 0.85rem; font-weight: 700; color: var(--text-secondary);">Quiz:</label>
                <select id="quiz-select" class="form-select" style="width: auto; min-width: 200px;">
                    <option value="1">Loading quizzes...</option>
                </select>
            </div>
            <button type="button" class="btn btn-outline btn-sm" onclick="triggerRefresh()">
                🔄 Refresh
            </button>
        </div>
    </div>

    <!-- Podium Section -->
    <div class="podium-container" id="podium-container">
        <!-- Rank 2: Silver -->
        <div class="podium-card podium-rank-2" id="podium-2">
            <div class="podium-crown">🥈</div>
            <div class="badge" style="background: rgba(148, 163, 184, 0.2); color: #cbd5e1; margin-bottom: 0.5rem;">Rank #2</div>
            <div class="podium-name" id="podium-2-name">--</div>
            <div class="podium-score" id="podium-2-score">-- pts</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.25rem;" id="podium-2-meta">--</div>
        </div>

        <!-- Rank 1: Gold -->
        <div class="podium-card podium-rank-1" id="podium-1">
            <div class="podium-crown">👑</div>
            <div class="badge" style="background: rgba(245, 158, 11, 0.2); color: #fbbf24; margin-bottom: 0.5rem;">Champion #1</div>
            <div class="podium-name" id="podium-1-name" style="font-size: 1.3rem;">--</div>
            <div class="podium-score" id="podium-1-score" style="font-size: 1.85rem; color: #fbbf24;">-- pts</div>
            <div style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.25rem;" id="podium-1-meta">--</div>
        </div>

        <!-- Rank 3: Bronze -->
        <div class="podium-card podium-rank-3" id="podium-3">
            <div class="podium-crown">🥉</div>
            <div class="badge" style="background: rgba(217, 119, 6, 0.2); color: #f59e0b; margin-bottom: 0.5rem;">Rank #3</div>
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
                        <th>Tab Violations</th>
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
        initialQuizId: <%= (quizIdParam != null && !quizIdParam.trim().isEmpty()) ? quizIdParam.trim() : "1" %>
    };
</script>
<script src="<%= request.getContextPath() %>/js/leaderboard.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
