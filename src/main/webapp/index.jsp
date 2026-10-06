<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "QuizLive - Real-Time Timed Quizzes & Live Leaderboard");
    request.setAttribute("activeNav", "home");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <!-- Active User Banner -->
    <% if (user != null) { %>
        <div class="alert alert-success" style="margin-bottom: 2rem; justify-content: space-between;">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
                <span style="font-size: 1.4rem;">👋</span>
                <div>
                    <strong>Welcome back, <%= user.getName() %>!</strong> You are signed in as
                    <span class="badge <%= user.getRole().name().equals("ADMIN") ? "badge-admin" : user.getRole().name().equals("CREATOR") ? "badge-creator" : "badge-participant" %>">
                        <%= user.getRole().name() %>
                    </span>.
                </div>
            </div>
            <a href="<%= request.getContextPath() %><%= user.getDashboardUrl() %>" class="btn btn-primary btn-sm">
                Open My Dashboard &rarr;
            </a>
        </div>
    <% } %>

    <!-- Hero Section -->
    <div style="text-align: center; padding: 3.5rem 1rem 3rem; max-width: 850px; margin: 0 auto;">
        <span class="live-badge" style="margin-bottom: 1.25rem;">
            <span class="live-dot"></span> Next-Gen Real-Time Assessment Platform
        </span>
        <h1 style="font-size: 3.25rem; font-weight: 900; line-height: 1.15; margin-bottom: 1.25rem; background: linear-gradient(135deg, #ffffff 40%, var(--secondary) 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">
            Compete. Learn. Master in Real-Time ⚡
        </h1>
        <p style="font-size: 1.2rem; color: var(--text-secondary); line-height: 1.6; margin-bottom: 2rem;">
            Experience live timed assessments with automated anti-cheat proctoring, high-precision countdown engines, instant grading, and real-time WebSocket push leaderboards.
        </p>

        <div style="display: flex; align-items: center; justify-content: center; gap: 1rem; flex-wrap: wrap;">
            <% if (user == null) { %>
                <a href="<%= request.getContextPath() %>/register.jsp" class="btn btn-primary btn-lg">
                    🚀 Get Started Free
                </a>
                <a href="<%= request.getContextPath() %>/login.jsp" class="btn btn-secondary btn-lg">
                    Sign In
                </a>
            <% } else { %>
                <a href="<%= request.getContextPath() %><%= user.getDashboardUrl() %>" class="btn btn-primary btn-lg">
                    🎯 Launch Dashboard
                </a>
            <% } %>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-outline btn-lg">
                🏆 View Live Leaderboards
            </a>
        </div>
    </div>

    <!-- Feature Pillars -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-bottom: 4rem;">
        <div class="card" style="padding: 1.5rem;">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">🛡️</div>
            <h3 style="font-size: 1.2rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Anti-Cheat Proctoring
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.5;">
                Client-side visibility listeners detect tab switching and window minimization in real-time. Incidents are logged and flagged on creator dashboards.
            </p>
        </div>

        <div class="card" style="padding: 1.5rem;">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">⏱️</div>
            <h3 style="font-size: 1.2rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Timed Exam Engine
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.5;">
                Thread-safe server timers automatically enforce quiz deadlines and auto-submit answers with grace buffers to prevent late submissions.
            </p>
        </div>

        <div class="card" style="padding: 1.5rem;">
            <div style="font-size: 2.5rem; margin-bottom: 1rem;">⚡</div>
            <h3 style="font-size: 1.2rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);">
                Real-Time Push Leaderboards
            </h3>
            <p style="color: var(--text-secondary); font-size: 0.9rem; line-height: 1.5;">
                Jakarta WebSocket endpoints push instant ranking updates and podium highlights to all connected spectators the moment a test is submitted.
            </p>
        </div>
    </div>

    <!-- Featured Live Quizzes Section -->
    <div style="margin-bottom: 4rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem;">
            <div>
                <h2 style="font-size: 1.6rem; font-weight: 800;">Featured Active Quizzes</h2>
                <p style="color: var(--text-secondary); font-size: 0.9rem;">Join these live assessments now</p>
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

    <!-- Demo Fast-Pass Evaluation Section -->
    <div class="card" style="margin-bottom: 4rem; background: linear-gradient(180deg, var(--bg-surface) 0%, var(--bg-card) 100%);">
        <div class="card-header" style="justify-content: center; text-align: center; flex-direction: column;">
            <h2 class="card-title" style="font-size: 1.3rem;">⚡ Quick Demo Fast-Pass</h2>
            <p style="font-size: 0.85rem; color: var(--text-secondary); margin-top: 0.25rem;">
                Click any role below to prefill demo credentials and test all role features immediately:
            </p>
        </div>
        <div class="card-body">
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem;">
                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; border-color: rgba(6, 182, 212, 0.4); text-align: center;">
                    <div style="font-size: 1.8rem; margin-bottom: 0.35rem;">👤</div>
                    <div style="font-weight: 700; color: var(--text-primary);">Alice (Participant)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">alice@quizlive.com</div>
                    <span class="badge badge-participant" style="margin-top: 0.75rem;">Take Quizzes</span>
                </a>

                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; border-color: rgba(99, 102, 241, 0.4); text-align: center;">
                    <div style="font-size: 1.8rem; margin-bottom: 0.35rem;">✍️</div>
                    <div style="font-weight: 700; color: var(--text-primary);">Bob (Creator)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">bob@quizlive.com</div>
                    <span class="badge badge-creator" style="margin-top: 0.75rem;">Build Assessments</span>
                </a>

                <a href="<%= request.getContextPath() %>/login.jsp" class="card" style="padding: 1.25rem; text-decoration: none; border-color: rgba(168, 85, 247, 0.4); text-align: center;">
                    <div style="font-size: 1.8rem; margin-bottom: 0.35rem;">🛡️</div>
                    <div style="font-weight: 700; color: var(--text-primary);">Admin (Administrator)</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">admin@quizlive.com</div>
                    <span class="badge badge-admin" style="margin-top: 0.75rem;">Full Governance</span>
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
                            '<h3 class="card-title" style="font-size: 1.15rem;">' + escapeHtml(q.title) + '</h3>' +
                            '<span class="badge badge-approved">Live</span>' +
                        '</div>' +
                        '<div class="card-body" style="flex: 1;">' +
                            '<p style="color: var(--text-secondary); font-size: 0.875rem; margin-bottom: 1rem; min-height: 40px;">' +
                                escapeHtml(q.description || 'Test your knowledge in this timed competition.') +
                            '</p>' +
                            '<div style="display: flex; gap: 0.6rem; flex-wrap: wrap;">' +
                                '<span class="badge" style="background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-primary);">' +
                                    '⏱️ ' + durationMin + ' min' +
                                '</span>' +
                                '<span class="badge" style="background: var(--bg-surface); border: 1px solid var(--border-color); color: var(--text-primary);">' +
                                    '❓ ' + qCount + ' Questions' +
                                '</span>' +
                            '</div>' +
                        '</div>' +
                        '<div class="card-footer">' +
                            '<a href="' + contextPath + '/leaderboard.jsp?quizId=' + q.id + '" class="btn btn-outline btn-sm">' +
                                '🏆 Leaderboard' +
                            '</a>' +
                            '<a href="' + contextPath + '/participant/take-quiz.jsp?quizId=' + q.id + '" class="btn btn-primary btn-sm">' +
                                '🚀 Participate' +
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
