<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Sign In - QuizLive");
    request.setAttribute("activeNav", "login");
    String rawError = request.getParameter("error");
    String safeError = (rawError != null) ? rawError.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;") : "";
    String loggedOut = request.getParameter("loggedOut");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 480px; margin-top: 1rem;">
    <div class="card">
        <div class="card-header" style="flex-direction: column; align-items: flex-start; gap: 0.35rem;">
            <h2 class="card-title" style="font-size: 1.4rem;">Sign In to QuizLive</h2>
            <p style="font-size: 0.85rem; color: var(--text-secondary);">Enter your credentials or choose a quick demo account.</p>
        </div>

        <div class="card-body">
            <% if (loggedOut != null) { %>
                <div class="alert alert-success" style="margin-bottom: 1rem;">
                    <span>✅</span>
                    <span>You have been signed out successfully.</span>
                </div>
            <% } %>

            <div id="auth-error-banner" class="alert alert-danger" style="<%= (!safeError.isEmpty()) ? "" : "display: none;" %>">
                <%= safeError %>
            </div>

            <form id="login-form" action="<%= request.getContextPath() %>/login" method="POST" data-context-path="<%= request.getContextPath() %>">
                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="name@example.com" required autocomplete="email">
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" id="password" name="password" class="form-control" placeholder="••••••••" required autocomplete="current-password">
                </div>

                <div style="margin-top: 1.5rem;">
                    <button type="submit" id="login-submit-btn" class="btn btn-primary btn-block btn-lg">Sign In</button>
                </div>
            </form>

            <div style="margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--border-color);">
                <p style="font-size: 0.8rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); margin-bottom: 0.75rem;">
                    ⚡ Quick Demo Logins
                </p>
                <div style="display: flex; flex-direction: column; gap: 0.5rem;">
                    <button type="button" class="btn btn-secondary btn-sm" onclick="fillCredentials('alice@quizlive.com', 'password123')">
                        👤 Participant (Alice) &mdash; alice@quizlive.com
                    </button>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="fillCredentials('bob@quizlive.com', 'creator123')">
                        ✍️ Creator (Bob) &mdash; bob@quizlive.com
                    </button>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="fillCredentials('admin@quizlive.com', 'admin123')">
                        🛡️ Administrator &mdash; admin@quizlive.com
                    </button>
                </div>
            </div>
        </div>

        <div class="card-footer" style="justify-content: center;">
            <p style="font-size: 0.875rem; color: var(--text-secondary);">
                Don't have an account? <a href="<%= request.getContextPath() %>/register.jsp" style="font-weight: 600;">Create one free</a>
            </p>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/auth.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
