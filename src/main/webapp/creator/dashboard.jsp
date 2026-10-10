<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    boolean canDirectPublish = (user != null && user.canPublishDirectly());
    String rankName = (user != null && user.getRank() != null) ? user.getRank() : "STANDARD";
    request.setAttribute("pageTitle", "Creator Studio - QuizLive");
    request.setAttribute("activeNav", "dashboard");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;">
        <div>
            <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;">
                <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary); margin: 0;">
                    Creator Studio
                </h1>
                <span class="badge" style="<%= canDirectPublish ? "background: rgba(59, 115, 84, 0.15); color: #3B7354; border: 1px solid rgba(59, 115, 84, 0.35); font-weight: 700;" : "background: rgba(184, 115, 51, 0.15); color: #B87333; border: 1px solid rgba(184, 115, 51, 0.35);" %>">
                    <%= canDirectPublish ? "★ Verified Teacher Rank (Direct Publishing Enabled)" : "Standard Creator (" + rankName + " — Moderation Required)" %>
                </span>
            </div>
            <p style="color: var(--text-secondary); font-size: 0.95rem; margin-top: 0.35rem;">
                Design timed assessments, manage access codes, and monitor candidate responses and proctor integrity.
            </p>
        </div>
        <div style="display: flex; gap: 0.75rem;">
            <a href="<%= request.getContextPath() %>/creator/create-quiz.jsp" class="btn btn-primary">
                + Create New Quiz
            </a>
            <a href="<%= request.getContextPath() %>/messages.jsp" class="btn btn-secondary">
                Participant Messages
            </a>
        </div>
    </div>

    <!-- Stat Grid -->
    <div class="stat-grid">
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-my-quizzes">--</div>
                <div class="stat-label">My Quizzes</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
            </div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-approved">--</div>
                <div class="stat-label">Approved &amp; Active</div>
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
                <div class="stat-value" id="stat-pending">--</div>
                <div class="stat-label">Pending Approval</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#B87333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                </svg>
            </div>
        </div>
    </div>

    <!-- Creator Quizzes Table -->
    <div class="card" style="margin-bottom: 2.5rem;">
        <div class="card-header">
            <h2 class="card-title">Authored Quizzes</h2>
            <button class="btn btn-outline btn-sm" onclick="loadMyQuizzes()">Refresh</button>
        </div>

        <div class="table-responsive">
            <table class="table" id="creator-quizzes-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Quiz Title</th>
                        <th>Access Code &amp; Visibility</th>
                        <th>Duration</th>
                        <th>Questions</th>
                        <th>Status &amp; Live Lobby</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="creator-quizzes-tbody">
                    <tr>
                        <td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                            Loading your quizzes...
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Submissions & Anti-Cheat Review Table -->
    <div class="card">
        <div class="card-header">
            <div>
                <h2 class="card-title">Participant Submissions &amp; Proctor Review <span id="current-submissions-quiz-badge" style="font-size: 0.85rem; color: var(--text-muted); font-weight: normal; margin-left: 0.5rem;"></span></h2>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                    Inspect score performance and proctor violation flags (3 or more tab switches).
                </p>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-outline btn-sm" id="clear-submission-filter-btn" style="display: none;" onclick="loadSubmissions()">Show All</button>
                <button class="btn btn-outline btn-sm" onclick="loadSubmissions()">Refresh</button>
            </div>
        </div>

        <div class="table-responsive">
            <table class="table" id="submissions-table">
                <thead>
                    <tr>
                        <th>Attempt #</th>
                        <th>Participant</th>
                        <th>Quiz ID</th>
                        <th>Score</th>
                        <th>Accuracy</th>
                        <th>Duration</th>
                        <th>Anti-Cheat Violations</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="submissions-tbody">
                    <tr>
                        <td colspan="9" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                            Loading participant submissions...
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    var contextPath = '<%= request.getContextPath() %>';
    var canPublishDirectly = <%= canDirectPublish %>;

    function loadMyQuizzes() {
        var tbody = document.getElementById('creator-quizzes-tbody');

        fetch(contextPath + '/api/quizzes?filter=my')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2rem;">You have not created any quizzes yet. Click "+ Create New Quiz" above.</td></tr>';
                    document.getElementById('stat-my-quizzes').textContent = '0';
                    document.getElementById('stat-approved').textContent = '0';
                    document.getElementById('stat-pending').textContent = '0';
                    return;
                }

                var quizzes = resData.data;
                document.getElementById('stat-my-quizzes').textContent = quizzes.length;

                var approvedCount = 0;
                var pendingCount = 0;

                tbody.innerHTML = '';
                quizzes.forEach(function(q) {
                    if (q.status === 'APPROVED') approvedCount++;
                    if (q.status === 'PENDING') pendingCount++;

                    var statusBadge = (q.status === 'APPROVED') ? '<span class="badge badge-approved">Approved</span>' :
                                      (q.status === 'REJECTED') ? '<span class="badge badge-rejected">Rejected</span>' :
                                      '<span class="badge badge-pending">Pending Review</span>';

                    var liveBadge = '';
                    if (q.status === 'APPROVED') {
                        if (q.isHeld) {
                            liveBadge = '<div style="margin-top: 0.35rem;"><span class="badge" style="background: rgba(184, 115, 51, 0.12); color: #B87333; border: 1px solid rgba(184, 115, 51, 0.35);">⏸ In Lobby (Held)</span></div>';
                        } else {
                            liveBadge = '<div style="margin-top: 0.35rem;"><span class="badge" style="background: rgba(59, 115, 84, 0.12); color: #3B7354; border: 1px solid rgba(59, 115, 84, 0.35);">▶ Live &amp; Active</span></div>';
                        }
                    }

                    var schedNote = '';
                    if (q.scheduledStartAt) {
                        schedNote = '<div style="font-size: 0.72rem; color: var(--text-muted); margin-top: 0.2rem;">🕒 ' + escapeHtml(q.scheduledStartAt) + '</div>';
                    }

                    var hostButton = '';
                    if (q.status === 'APPROVED') {
                        if (q.isHeld) {
                            hostButton = '<button class="btn btn-success btn-sm" onclick="toggleQuizHost(' + q.id + ', \'start\')" style="padding: 0.25rem 0.6rem; font-weight: 700;">▶ Start Quiz Now</button>';
                        } else {
                            hostButton = '<button class="btn btn-outline btn-sm" onclick="toggleQuizHost(' + q.id + ', \'hold\')" style="padding: 0.25rem 0.6rem; color: #B87333; border-color: #B87333;">⏸ Hold in Lobby</button>';
                        }
                    }

                    var publishButton = '';
                    if (q.status === 'PENDING' && canPublishDirectly) {
                        publishButton = '<button class="btn btn-primary btn-sm" onclick="publishQuizDirectly(' + q.id + ')" style="padding: 0.25rem 0.6rem; font-weight: 700;">⚡ Publish Now</button>';
                    }

                    var qCount = (q.questions) ? q.questions.length : '--';

                    var codeBadge = q.accessCode ?
                        '<div style="margin-bottom: 0.3rem;"><code style="font-size: 0.82rem; font-weight: 700; background: var(--bg-surface-alt); padding: 0.2rem 0.45rem; border-radius: 4px; border: 1px solid var(--border-color); cursor: pointer;" title="Click to copy code" onclick="if(navigator.clipboard){navigator.clipboard.writeText(\'' + escapeHtml(q.accessCode) + '\');} if(window.showToast) window.showToast(\'info\', \'Copied ' + escapeHtml(q.accessCode) + ' to clipboard\');">🔑 ' + escapeHtml(q.accessCode) + '</code></div>' :
                        '<div style="color: var(--text-muted); font-size: 0.8rem; margin-bottom: 0.25rem;">No Code</div>';

                    var visBadge = q.isPublic ?
                        '<div style="display: flex; align-items: center; gap: 0.35rem; margin-top: 0.25rem;"><span class="badge" style="background: rgba(59, 115, 84, 0.12); color: #3B7354; font-size: 0.72rem; border: 1px solid rgba(59, 115, 84, 0.3);">Public</span> <button class="btn btn-outline btn-sm" style="font-size: 0.68rem; padding: 0.1rem 0.35rem;" onclick="toggleQuizVisibility(' + q.id + ', false)" title="Make Private / Unlisted">Hide</button></div>' :
                        '<div style="display: flex; align-items: center; gap: 0.35rem; margin-top: 0.25rem;"><span class="badge" style="background: rgba(96, 82, 122, 0.12); color: #60527A; font-size: 0.72rem; border: 1px solid rgba(96, 82, 122, 0.3);">Private / Unlisted</span> <button class="btn btn-outline btn-sm" style="font-size: 0.68rem; padding: 0.1rem 0.35rem;" onclick="toggleQuizVisibility(' + q.id + ', true)" title="Make Public">Publish to Catalog</button></div>';

                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>#' + q.id + '</td>' +
                        '<td>' +
                            '<strong>' + escapeHtml(q.title) + '</strong>' +
                            '<div style="font-size: 0.8rem; color: var(--text-muted);">' + escapeHtml(q.description || '') + '</div>' +
                        '</td>' +
                        '<td>' + codeBadge + visBadge + '</td>' +
                        '<td>' + Math.round(q.durationSeconds / 60) + ' min (' + q.durationSeconds + 's)</td>' +
                        '<td>' + qCount + '</td>' +
                        '<td>' + statusBadge + liveBadge + schedNote + '</td>' +
                        '<td>' +
                            '<div style="display: flex; gap: 0.4rem; flex-wrap: wrap; align-items: center;">' +
                                publishButton +
                                hostButton +
                                '<a href="' + contextPath + '/participant/take-quiz.jsp?quizId=' + q.id + '" target="_blank" class="btn btn-outline btn-sm" style="padding: 0.25rem 0.5rem;" title="Preview Lobby as Student">Lobby</a>' +
                                '<a href="' + contextPath + '/leaderboard.jsp?quizId=' + q.id + '" class="btn btn-outline btn-sm" style="padding: 0.25rem 0.5rem;">Leaderboard</a>' +
                                '<button class="btn btn-secondary btn-sm" onclick="filterSubmissionsByQuiz(' + q.id + ')" style="padding: 0.25rem 0.5rem;">Submissions</button>' +
                            '</div>' +
                        '</td>';

                    tbody.appendChild(tr);
                });

                document.getElementById('stat-approved').textContent = approvedCount;
                document.getElementById('stat-pending').textContent = pendingCount;
            })
            .catch(function(err) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">Error loading quizzes.</td></tr>';
            });
    }

    window.toggleQuizHost = function(quizId, action) {
        var actionLabel = (action === 'start') ? 'start this quiz session now and admit waiting students' : 'put this quiz on hold in the lobby';
        if (!confirm('Are you sure you want to ' + actionLabel + '?')) {
            return;
        }

        fetch(contextPath + '/api/quizzes/host', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                quizId: quizId,
                action: action
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                if (window.showToast) {
                    window.showToast('success', resData.data && resData.data.message ? resData.data.message : 'Quiz session updated successfully!');
                }
                loadMyQuizzes();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to update quiz host state';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error updating quiz host status.');
        });
    };

    window.publishQuizDirectly = function(quizId) {
        if (!confirm('Publish Quiz #' + quizId + ' directly now? It will become active immediately without admin moderation.')) {
            return;
        }

        fetch(contextPath + '/api/quizzes/approve', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                quizId: quizId,
                action: 'approve'
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                if (window.showToast) window.showToast('success', 'Quiz #' + quizId + ' published directly! Assessment is now active.');
                loadMyQuizzes();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to publish quiz';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error publishing quiz.');
        });
    };

    window.toggleQuizVisibility = function(quizId, makePublic) {
        var actionLabel = makePublic ? 'make this quiz visible in the public catalog' : 'make this quiz private (unlisted, accessible only via access code)';
        if (!confirm('Are you sure you want to ' + actionLabel + '?')) {
            return;
        }

        fetch(contextPath + '/api/quizzes/host', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                quizId: quizId,
                action: 'visibility',
                isPublic: makePublic
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                var msg = resData.data && resData.data.message ? resData.data.message : 'Quiz visibility updated!';
                if (window.showToast) window.showToast('success', msg);
                loadMyQuizzes();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to update visibility';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error updating visibility.');
        });
    };

    function loadSubmissions(quizId) {
        var tbody = document.getElementById('submissions-tbody');
        var badge = document.getElementById('current-submissions-quiz-badge');
        var clearBtn = document.getElementById('clear-submission-filter-btn');

        if (badge) {
            badge.textContent = quizId ? ('(Filtered to Quiz #' + quizId + ')') : '(All Authored Quizzes)';
        }
        if (clearBtn) {
            clearBtn.style.display = quizId ? 'inline-block' : 'none';
        }

        var url = contextPath + '/api/attempts' + (quizId ? ('?quizId=' + quizId) : '');

        fetch(url)
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No participant attempts recorded yet.</td></tr>';
                    return;
                }

                var attempts = resData.data;
                tbody.innerHTML = '';

                attempts.forEach(function(a) {
                    var tr = document.createElement('tr');
                    var perc = (a.percentage != null) ? a.percentage.toFixed(1) + '%' : (a.maxScore > 0 ? ((a.score / a.maxScore) * 100).toFixed(1) + '%' : '--');

                    var violationsBadge = (a.tabSwitches >= 3)
                        ? '<span class="badge badge-rejected">Flagged (' + a.tabSwitches + ' switches)</span>'
                        : (a.tabSwitches > 0)
                            ? '<span class="badge badge-pending">' + a.tabSwitches + ' switches</span>'
                            : '<span style="color: var(--success); font-weight: 600;">Clean (0)</span>';

                    var statusBadge = (a.status === 'SUBMITTED') ? '<span class="badge badge-approved">Submitted</span>' :
                                      (a.status === 'AUTO_SUBMITTED') ? '<span class="badge badge-pending">Auto-Submitted</span>' :
                                      '<span class="badge badge-participant">' + a.status + '</span>';

                    tr.innerHTML =
                        '<td>#' + a.id + '</td>' +
                        '<td><strong>' + escapeHtml(a.userName || ('User #' + a.userId)) + '</strong></td>' +
                        '<td>Quiz #' + a.quizId + '</td>' +
                        '<td><strong>' + a.score + '</strong> / ' + a.maxScore + '</td>' +
                        '<td>' + perc + '</td>' +
                        '<td>' + (a.formattedDuration || (a.durationSeconds + 's')) + '</td>' +
                        '<td>' + violationsBadge + '</td>' +
                        '<td>' + statusBadge + '</td>' +
                        '<td>' +
                            '<a href="' + contextPath + '/messages.jsp?withUser=' + a.userId + '" class="btn btn-outline btn-sm">Message</a>' +
                        '</td>';

                    tbody.appendChild(tr);
                });
            })
            .catch(function(err) {
                tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--danger); padding: 2rem;">Error loading submissions.</td></tr>';
            });
    }

    window.filterSubmissionsByQuiz = function(quizId) {
        if (window.showToast) window.showToast('info', 'Filtering submissions for Quiz #' + quizId);
        loadSubmissions(quizId);
    };

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
        loadMyQuizzes();
        loadSubmissions();
    });
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
