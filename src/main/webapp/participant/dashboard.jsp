<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "Participant Dashboard - QuizLive");
    request.setAttribute("activeNav", "dashboard");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;">
        <div>
            <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary);">
                Welcome back, <%= user != null ? user.getName() : "Student" %> 👋
            </h1>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Select an active quiz below to participate in real-time competitions.
            </p>
        </div>
        <div>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-secondary">
                🏆 View All Leaderboards
            </a>
        </div>
    </div>

    <!-- Quick Stats -->
    <div class="stat-grid">
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-available">--</div>
                <div class="stat-label">Available Quizzes</div>
            </div>
            <div class="stat-icon">📚</div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-completed">--</div>
                <div class="stat-label">My Attempts</div>
            </div>
            <div class="stat-icon">🎯</div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-avg-score">--</div>
                <div class="stat-label">Average Score</div>
            </div>
            <div class="stat-icon">⚡</div>
        </div>
    </div>

    <!-- Anti-Cheat Notice -->
    <div class="alert alert-warning" style="margin-bottom: 2rem;">
        <span style="font-size: 1.3rem;">🛡️</span>
        <div>
            <strong>Anti-Cheat Proctored Environment Active:</strong>
            Switching browser tabs, minimizing the test window, or leaving the page during an active quiz is tracked on the server.
            Excessive violations (3+) will flag your submission for review.
        </div>
    </div>

    <!-- Available Quizzes Section -->
    <div style="margin-bottom: 3rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem;">
            <h2 style="font-size: 1.35rem; font-weight: 700;">Active Quiz Catalog</h2>
            <button class="btn btn-outline btn-sm" onclick="loadQuizzes()">🔄 Refresh</button>
        </div>

        <div id="quiz-list-loading" style="text-align: center; padding: 2rem; color: var(--text-secondary);">
            Loading quizzes...
        </div>

        <div id="quiz-cards-grid" class="stat-grid" style="display: none; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));">
            <!-- Quiz cards rendered via JS -->
        </div>

        <div id="no-quizzes-msg" style="display: none; text-align: center; padding: 3rem; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border-color);">
            <p style="font-size: 1.1rem; color: var(--text-secondary); margin-bottom: 1rem;">No active quizzes found right now.</p>
        </div>
    </div>

    <!-- Past Attempts Section -->
    <div>
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem;">
            <h2 style="font-size: 1.35rem; font-weight: 700;">My Submission History</h2>
            <button class="btn btn-outline btn-sm" onclick="loadAttempts()">🔄 Refresh</button>
        </div>

        <div class="card">
            <div class="table-responsive">
                <table class="table" id="attempts-table">
                    <thead>
                        <tr>
                            <th>Attempt #</th>
                            <th>Quiz Title / ID</th>
                            <th>Score</th>
                            <th>Percentage</th>
                            <th>Duration</th>
                            <th>Tab Switches</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody id="attempts-tbody">
                        <tr>
                            <td colspan="8" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                                Loading past attempts...
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script>
    var contextPath = '<%= request.getContextPath() %>';

    function formatDuration(sec) {
        if (!sec || sec < 0) return '00:00';
        var m = Math.floor(sec / 60);
        var s = sec % 60;
        return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
    }

    function loadQuizzes() {
        var loading = document.getElementById('quiz-list-loading');
        var grid = document.getElementById('quiz-cards-grid');
        var empty = document.getElementById('no-quizzes-msg');

        loading.style.display = 'block';
        grid.style.display = 'none';
        empty.style.display = 'none';

        fetch(contextPath + '/api/quizzes')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                loading.style.display = 'none';
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    empty.style.display = 'block';
                    document.getElementById('stat-available').textContent = '0';
                    return;
                }

                var quizzes = resData.data;
                document.getElementById('stat-available').textContent = quizzes.length;
                grid.innerHTML = '';

                quizzes.forEach(function(q) {
                    var card = document.createElement('div');
                    card.className = 'card';
                    card.style.display = 'flex';
                    card.style.flexDirection = 'column';

                    var qCount = (q.questions && q.questions.length) ? q.questions.length : (q.totalPoints ? (q.totalPoints / 10) : 'Multiple');
                    var durationMin = Math.round(q.durationSeconds / 60);

                    card.innerHTML =
                        '<div class="card-header">' +
                            '<h3 class="card-title" style="font-size: 1.15rem;">' + escapeHtml(q.title) + '</h3>' +
                            '<span class="badge badge-approved">Approved</span>' +
                        '</div>' +
                        '<div class="card-body" style="flex: 1;">' +
                            '<p style="color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 1rem; min-height: 40px;">' +
                                escapeHtml(q.description || 'Test your knowledge in this live timed quiz.') +
                            '</p>' +
                            '<div style="display: flex; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 0.5rem;">' +
                                '<span class="badge" style="background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-primary);">' +
                                    '⏱️ ' + durationMin + ' min (' + q.durationSeconds + 's)' +
                                '</span>' +
                                '<span class="badge" style="background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-primary);">' +
                                    '❓ ' + qCount + ' Questions' +
                                '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="card-footer">' +
                            '<a href="' + contextPath + '/leaderboard.jsp?quizId=' + q.id + '" class="btn btn-outline btn-sm">' +
                                '🏆 Ranks' +
                            '</a>' +
                            '<a href="' + contextPath + '/participant/take-quiz.jsp?quizId=' + q.id + '" class="btn btn-primary btn-sm">' +
                                '🚀 Start Quiz' +
                            '</a>' +
                        '</div>';

                    grid.appendChild(card);
                });

                grid.style.display = 'grid';
            })
            .catch(function(err) {
                loading.textContent = 'Failed to load quizzes. Please refresh.';
            });
    }

    function loadAttempts() {
        var tbody = document.getElementById('attempts-tbody');

        fetch(contextPath + '/api/attempts')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No previous attempts recorded.</td></tr>';
                    document.getElementById('stat-completed').textContent = '0';
                    document.getElementById('stat-avg-score').textContent = 'N/A';
                    return;
                }

                var attempts = resData.data;
                document.getElementById('stat-completed').textContent = attempts.length;

                var totalPerc = 0;
                var submittedCount = 0;

                tbody.innerHTML = '';
                attempts.forEach(function(a) {
                    var tr = document.createElement('tr');
                    var perc = (a.percentage != null) ? a.percentage.toFixed(1) + '%' : (a.maxScore > 0 ? ((a.score / a.maxScore) * 100).toFixed(1) + '%' : '--');
                    if (a.status === 'SUBMITTED' || a.status === 'AUTO_SUBMITTED') {
                        totalPerc += (a.percentage != null) ? a.percentage : 0;
                        submittedCount++;
                    }

                    var switchesBadge = (a.tabSwitches > 0)
                        ? '<span class="badge ' + (a.tabSwitches >= 3 ? 'badge-rejected' : 'badge-pending') + '">' + a.tabSwitches + ' switches</span>'
                        : '<span style="color: var(--success);">0</span>';

                    var statusBadge = (a.status === 'SUBMITTED') ? '<span class="badge badge-approved">Submitted</span>' :
                                      (a.status === 'AUTO_SUBMITTED') ? '<span class="badge badge-warning">Auto-Submitted</span>' :
                                      '<span class="badge badge-pending">' + a.status + '</span>';

                    tr.innerHTML =
                        '<td>#' + a.id + '</td>' +
                        '<td><strong>Quiz #' + a.quizId + '</strong></td>' +
                        '<td><strong>' + a.score + '</strong> / ' + a.maxScore + '</td>' +
                        '<td>' + perc + '</td>' +
                        '<td>' + (a.formattedDuration || formatDuration(a.durationSeconds)) + '</td>' +
                        '<td>' + switchesBadge + '</td>' +
                        '<td>' + statusBadge + '</td>' +
                        '<td><a href="' + contextPath + '/leaderboard.jsp?quizId=' + a.quizId + '" class="btn btn-outline btn-sm">Leaderboard</a></td>';

                    tbody.appendChild(tr);
                });

                if (submittedCount > 0) {
                    document.getElementById('stat-avg-score').textContent = (totalPerc / submittedCount).toFixed(0) + '%';
                } else {
                    document.getElementById('stat-avg-score').textContent = '--';
                }
            })
            .catch(function(err) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--danger); padding: 2rem;">Error loading history.</td></tr>';
            });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    document.addEventListener('DOMContentLoaded', function() {
        loadQuizzes();
        loadAttempts();
    });
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
