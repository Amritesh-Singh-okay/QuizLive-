<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Create Account - QuizLive");
    request.setAttribute("activeNav", "register");
    String rawError = request.getParameter("error");
    String safeError = (rawError != null) ? rawError.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;") : "";
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 520px; margin-top: 1rem;">
    <div class="card">
        <div class="card-header" style="flex-direction: column; align-items: flex-start; gap: 0.35rem;">
            <h2 class="card-title" style="font-size: 1.4rem;">Create Your Account</h2>
            <p style="font-size: 0.85rem; color: var(--text-secondary);">Compete in live timed quizzes or author your own tests.</p>
        </div>

        <div class="card-body">
            <div id="auth-error-banner" class="alert alert-danger" style="<%= (!safeError.isEmpty()) ? "" : "display: none;" %>">
                <%= safeError %>
            </div>

            <form id="register-form" action="<%= request.getContextPath() %>/register" method="POST" data-context-path="<%= request.getContextPath() %>">
                <div class="form-group">
                    <label for="name" class="form-label">Full Name</label>
                    <input type="text" id="name" name="name" class="form-control" placeholder="e.g. Charlie Brown" required autocomplete="name">
                </div>

                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" placeholder="charlie@example.com" required autocomplete="email">
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Password (Min. 6 Characters)</label>
                    <input type="password" id="password" name="password" class="form-control" placeholder="••••••••" required minlength="6" autocomplete="new-password">
                </div>

                <div class="form-group">
                    <label for="role" class="form-label">Account Role</label>
                    <select id="role" name="role" class="form-select">
                        <option value="PARTICIPANT" selected>Student / Participant (Take live quizzes &amp; climb ranks)</option>
                        <option value="CREATOR">Quiz Creator (Author quizzes, manage questions &amp; review scores)</option>
                        <option value="ADMIN">Administrator (System approvals &amp; user management)</option>
                    </select>
                    <p class="form-help">Select the primary capability for your account.</p>
                </div>

                <div style="margin-top: 1.5rem;">
                    <button type="submit" id="register-submit-btn" class="btn btn-primary btn-block btn-lg">Create Account</button>
                </div>
            </form>
        </div>

        <div class="card-footer" style="justify-content: center;">
            <p style="font-size: 0.875rem; color: var(--text-secondary);">
                Already have an account? <a href="<%= request.getContextPath() %>/login.jsp" style="font-weight: 600;">Sign In</a>
            </p>
        </div>
    </div>
</div>

<script src="<%= request.getContextPath() %>/js/auth.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
