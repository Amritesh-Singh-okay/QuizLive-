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
                Welcome back, <%= user != null ? user.getName() : "Student" %>
            </h1>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Select an active quiz below to participate in proctored timed assessments.
            </p>
        </div>
        <div>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-secondary">
                View All Leaderboards
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
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                </svg>
            </div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-completed">--</div>
                <div class="stat-label">My Attempts</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#3B7354" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
            </div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-avg-score">--</div>
                <div class="stat-label">Average Score</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
            </div>
        </div>
    </div>

    <!-- Anti-Cheat Notice -->
    <div class="alert alert-warning" style="margin-bottom: 2rem;">
        <span style="display: inline-flex; align-items: center; flex-shrink: 0;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#B87333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
        </span>
        <div>
            <strong>Proctored Environment Active:</strong>
            Switching browser tabs, minimizing the exam window, or navigating away during an active test is recorded server-side.
            Multiple violations (3 or more) flag your submission for academic review.
        </div>
    </div>

    <!-- Join Quiz with Access Code Card -->
    <div class="card" style="margin-bottom: 2.5rem; background: var(--bg-card); border-left: 4px solid var(--primary); box-shadow: var(--shadow-sm);">
        <div class="card-body" style="padding: 1.5rem;">
            <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1.25rem;">
                <div>
                    <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem; display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                        </svg>
                        Join Quiz with Access Code
                    </h3>
                    <p style="font-size: 0.875rem; color: var(--text-secondary); margin: 0;">
                        Enter a private assessment code or classroom code provided by your instructor to launch the exam.
                    </p>
                </div>
                <form id="join-code-form" onsubmit="joinQuizByCode(event)" style="display: flex; gap: 0.5rem; width: 100%; max-width: 400px;">
                    <input type="text" id="join-access-code" class="form-control" placeholder="Enter Access Code (e.g. BIO101)" maxlength="32" style="font-weight: 700; text-transform: uppercase; font-family: monospace; letter-spacing: 0.04em;" required>
                    <button type="submit" id="join-code-btn" class="btn btn-primary" style="white-space: nowrap;">
                        Join Quiz &rarr;
                    </button>
                </form>
            </div>
        </div>
    </div>

    <!-- Available Quizzes Section -->
    <div style="margin-bottom: 3rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem;">
            <h2 style="font-size: 1.35rem; font-weight: 700;">Active Quiz Catalog</h2>
            <button class="btn btn-outline btn-sm" onclick="loadQuizzes()">Refresh</button>
        </div>

        <div id="quiz-list-loading" style="text-align: center; padding: 2rem; color: var(--text-secondary);">
            Loading quizzes...
        </div>

        <div id="quiz-cards-grid" class="stat-grid" style="display: none; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));">
            <!-- Quiz cards rendered via JS -->
        </div>

        <div id="no-quizzes-msg" style="display: none; text-align: center; padding: 3rem; background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border-color);">
            <p style="font-size: 1rem; color: var(--text-secondary); margin-bottom: 1rem;">No active quizzes found right now.</p>
        </div>
    </div>

    <!-- Past Attempts Section -->
    <div>
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem;">
            <h2 style="font-size: 1.35rem; font-weight: 700;">My Submission History</h2>
            <button class="btn btn-outline btn-sm" onclick="loadAttempts()">Refresh</button>
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
                            '<h3 class="card-title" style="font-size: 1.05rem;">' + escapeHtml(q.title) + '</h3>' +
                            '<span class="badge badge-approved">Approved</span>' +
                        '</div>' +
                        '<div class="card-body" style="flex: 1;">' +
                            '<p style="color: var(--text-secondary); font-size: 0.875rem; margin-bottom: 1rem; min-height: 40px; line-height: 1.5;">' +
                                escapeHtml(q.description || 'Proctored live assessment.') +
                            '</p>' +
                            '<div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.5rem;">' +
                                '<span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary);">' +
                                    durationMin + ' min (' + q.durationSeconds + 's)' +
                                '</span>' +
                                '<span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary);">' +
                                    qCount + ' Questions' +
                                '</span>' +
                                (q.accessCode ? '<span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary); font-family: monospace;">🔑 ' + escapeHtml(q.accessCode) + '</span>' : '') +
                            '</div>' +
                        '</div>' +
                        '<div class="card-footer">' +
                            '<a href="' + contextPath + '/leaderboard.jsp?quizId=' + q.id + '" class="btn btn-outline btn-sm">' +
                                'Leaderboard' +
                            '</a>' +
                            '<a href="' + contextPath + '/participant/take-quiz.jsp?quizId=' + q.id + '" class="btn btn-primary btn-sm">' +
                                'Start Quiz' +
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

    window.joinQuizByCode = function(e) {
        if (e) e.preventDefault();
        var codeInput = document.getElementById('join-access-code');
        var code = codeInput ? codeInput.value.trim().toUpperCase() : '';
        if (!code) {
            if (window.showToast) window.showToast('warning', 'Please enter a quiz access code.');
            return;
        }

        var btn = document.getElementById('join-code-btn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Joining...';
        }

        fetch(contextPath + '/api/quizzes?code=' + encodeURIComponent(code))
            .then(function(res) {
                return res.json().then(function(data) {
                    return { status: res.status, ok: res.ok, data: data };
                });
            })
            .then(function(result) {
                if (result.ok && result.data && result.data.success && result.data.data) {
                    var quiz = result.data.data;
                    if (window.showToast) {
                        window.showToast('success', 'Found quiz: ' + quiz.title + '! Launching exam room...');
                    }
                    setTimeout(function() {
                        window.location.href = contextPath + '/participant/take-quiz.jsp?code=' + encodeURIComponent(code);
                    }, 400);
                } else {
                    var err = (result.data && result.data.error) ? result.data.error : 'Quiz not found with access code: ' + code;
                    if (window.showToast) window.showToast('error', err);
                    if (btn) {
                        btn.disabled = false;
                        btn.textContent = 'Join Quiz \u2192';
                    }
                }
            })
            .catch(function(err) {
                if (window.showToast) window.showToast('error', 'Network error checking access code.');
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = 'Join Quiz \u2192';
                }
            });
    };

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
                        : '<span style="color: var(--success); font-weight: 600;">0</span>';

                    var statusBadge = (a.status === 'SUBMITTED') ? '<span class="badge badge-approved">Submitted</span>' :
                                      (a.status === 'AUTO_SUBMITTED') ? '<span class="badge badge-pending">Auto-Submitted</span>' :
                                      '<span class="badge badge-participant">' + a.status + '</span>';

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
