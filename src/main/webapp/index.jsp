<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "QuizLive: Proctored Timed Assessments");
    request.setAttribute("activeNav", "home");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <!-- Active User Banner -->
    <% if (user != null) { %>
        <div class="alert alert-success" style="margin-bottom: 2rem; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
                <div>
                    <strong>Welcome back, <%= user.getName() %>:</strong> You are signed in as
                    <span class="badge <%= user.getRole().name().equals("ADMIN") ? "badge-admin" : user.getRole().name().equals("CREATOR") ? "badge-creator" : "badge-participant" %>">
                        <%= user.getRole().name() %>
                    </span>
                </div>
            </div>
            <a href="<%= request.getContextPath() %><%= user.getDashboardUrl() %>" class="btn btn-primary btn-sm">
                Open Dashboard &rarr;
            </a>
        </div>
    <% } %>

    <!-- Hero Section -->
    <div style="text-align: center; padding: 3.5rem 1rem 3rem; max-width: 820px; margin: 0 auto;">
        <div style="margin-bottom: 1.25rem;">
            <span class="live-badge">
                <span class="live-dot"></span> Proctored Examination Engine
            </span>
        </div>
        <h1 style="font-size: 2.75rem; font-weight: 800; line-height: 1.2; margin-bottom: 1.25rem; color: var(--text-primary); letter-spacing: -0.02em;">
            Timed Assessments with Quiet Precision and Real-Time Integrity
        </h1>
        <p style="font-size: 1.125rem; color: var(--text-secondary); line-height: 1.65; margin-bottom: 2rem;">
            A proctored assessment environment providing automated visibility tracking, thread-safe server countdowns, instantaneous grading, and synchronized live leaderboards.
        </p>

        <div style="display: flex; align-items: center; justify-content: center; gap: 0.85rem; flex-wrap: wrap;">
            <% if (user == null) { %>
                <a href="<%= request.getContextPath() %>/register.jsp" class="btn btn-primary btn-lg">
                    Create an Account
                </a>
                <a href="<%= request.getContextPath() %>/login.jsp" class="btn btn-secondary btn-lg">
                    Sign In
                </a>
            <% } else { %>
                <a href="<%= request.getContextPath() %><%= user.getDashboardUrl() %>" class="btn btn-primary btn-lg">
                    Launch Dashboard
                </a>
            <% } %>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-outline btn-lg">
                View Live Leaderboards
            </a>
        </div>
    </div>

    <!-- Feature Pillars -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-bottom: 3.5rem;">
        <div class="card" style="padding: 1.75rem;">
            <div style="width: 44px; height: 44px; border-radius: var(--radius-sm); background-color: var(--primary-light); display: flex; align-items: center; justify-content: center; margin-bottom: 1.25rem;">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    <path d="M9 12l2 2 4-4"/>
                </svg>
            </div>
            <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Proctored Integrity
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.6;">
                Browser Page Visibility API monitors detect unfocused windows and tab switches in real time. Incidents are logged server-side and reviewed on creator dashboards.
            </p>
        </div>

        <div class="card" style="padding: 1.75rem;">
            <div style="width: 44px; height: 44px; border-radius: var(--radius-sm); background-color: var(--primary-light); display: flex; align-items: center; justify-content: center; margin-bottom: 1.25rem;">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                </svg>
            </div>
            <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Server-Enforced Timers
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.6;">
                High-precision server countdowns guard each attempt. Tests automatically submit at expiration with a calibrated grace buffer to prevent late responses.
            </p>
        </div>

        <div class="card" style="padding: 1.75rem;">
            <div style="width: 44px; height: 44px; border-radius: var(--radius-sm); background-color: var(--primary-light); display: flex; align-items: center; justify-content: center; margin-bottom: 1.25rem;">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="20" x2="18" y2="10"/>
                    <line x1="12" y1="20" x2="12" y2="4"/>
                    <line x1="6" y1="20" x2="6" y2="14"/>
                </svg>
            </div>
            <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Synchronized Rankings
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.6;">
                WebSocket endpoints push updated rankings to all connected observers the moment an exam completes, with immediate accuracy and duration metrics.
            </p>
        </div>
    </div>

    <!-- Featured Live Quizzes Section -->
    <div style="margin-bottom: 3.5rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem;">
            <div>
                <h2 style="font-size: 1.45rem; font-weight: 700; color: var(--text-primary);">Active Quiz Catalog</h2>
                <p style="color: var(--text-secondary); font-size: 0.875rem;">Approved assessments currently open for participation</p>
            </div>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-outline btn-sm">
                View All Leaderboards &rarr;
            </a>
        </div>

        <div id="home-quizzes-grid" class="stat-grid" style="grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));">
            <div style="text-align: center; padding: 2rem; color: var(--text-muted); grid-column: 1 / -1;">
                Loading available quizzes...
            </div>
        </div>
    </div>

    <!-- Quick Demo Accounts Section -->
    <div class="card" style="margin-bottom: 3.5rem;">
        <div class="card-header" style="justify-content: flex-start; flex-direction: column; align-items: flex-start; gap: 0.25rem;">
            <h2 class="card-title">Quick Demo Sign-In</h2>
            <p style="font-size: 0.85rem; color: var(--text-secondary);">
                Select any predefined profile below to fill credentials and explore role-specific permissions:
            </p>
        </div>
        <div class="card-body">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem;">
                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; text-align: left; background-color: var(--bg-surface-alt);">
                    <div style="font-weight: 700; color: var(--text-primary); margin-bottom: 0.2rem;">Alice (Participant)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">alice@quizlive.com</div>
                    <span class="badge badge-participant">Take Quizzes</span>
                </a>

                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; text-align: left; background-color: var(--bg-surface-alt);">
                    <div style="font-weight: 700; color: var(--text-primary); margin-bottom: 0.2rem;">Bob (Creator)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">bob@quizlive.com</div>
                    <span class="badge badge-creator">Author Tests</span>
                </a>

                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; text-align: left; background-color: var(--bg-surface-alt);">
                    <div style="font-weight: 700; color: var(--text-primary); margin-bottom: 0.2rem;">Administrator</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.75rem;">admin@quizlive.com</div>
                    <span class="badge badge-admin">System Oversight</span>
                </a>
            </div>
        </div>
    </div>
</div>

<script>
    var contextPath = '<%= request.getContextPath() %>';

    function loadHomeQuizzes() {
        var grid = document.getElementById('home-quizzes-grid');

        fetch(contextPath + '/api/quizzes')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    grid.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--text-muted); grid-column: 1 / -1;">No active quizzes published yet.</div>';
                    return;
                }

                grid.innerHTML = '';
                resData.data.forEach(function(q) {
                    var card = document.createElement('div');
                    card.className = 'card';
                    card.style.display = 'flex';
                    card.style.flexDirection = 'column';

                    var qCount = (q.questions) ? q.questions.length : (q.totalPoints ? (q.totalPoints / 10) : 'Multiple');
                    var durationMin = Math.round(q.durationSeconds / 60);

                    card.innerHTML =
                        '<div class="card-header">' +
                            '<h3 class="card-title" style="font-size: 1.05rem;">' + escapeHtml(q.title) + '</h3>' +
                            '<span class="badge badge-approved">Active</span>' +
                        '</div>' +
                        '<div class="card-body" style="flex: 1;">' +
                            '<p style="color: var(--text-secondary); font-size: 0.875rem; margin-bottom: 1rem; min-height: 40px; line-height: 1.5;">' +
                                escapeHtml(q.description || 'Proctored timed assessment.') +
                            '</p>' +
                            '<div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">' +
                                '<span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary);">' +
                                    durationMin + ' min' +
                                '</span>' +
                                '<span class="badge" style="background: var(--bg-surface-alt); border: 1px solid var(--border-color); color: var(--text-secondary);">' +
                                    qCount + ' Questions' +
                                '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="card-footer">' +
                            '<a href="' + contextPath + '/leaderboard.jsp?quizId=' + q.id + '" class="btn btn-outline btn-sm">' +
                                'Leaderboard' +
                            '</a>' +
                            '<a href="' + contextPath + '/participant/take-quiz.jsp?quizId=' + q.id + '" class="btn btn-primary btn-sm">' +
                                'Participate' +
                            '</a>' +
                        '</div>';

                    grid.appendChild(card);
                });
            })
            .catch(function(err) {
                grid.innerHTML = '<div style="text-align: center; padding: 2rem; color: var(--text-muted); grid-column: 1 / -1;">Unable to load quizzes.</div>';
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

    document.addEventListener('DOMContentLoaded', loadHomeQuizzes);
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
