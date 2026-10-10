<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "Admin Console - QuizLive");
    request.setAttribute("activeNav", "dashboard");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container">
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 2rem;">
        <div>
            <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary);">
                Administrator Console
            </h1>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Platform governance, quiz moderation queue, user account management, and system configuration.
            </p>
        </div>
        <div>
            <a href="<%= request.getContextPath() %>/leaderboard.jsp" class="btn btn-secondary btn-sm">
                Global Leaderboards
            </a>
        </div>
    </div>

    <!-- Stat Grid -->
    <div class="stat-grid">
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-pending-approvals">--</div>
                <div class="stat-label">Pending Approvals</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#B87333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 14 14"/>
                </svg>
            </div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-total-users">--</div>
                <div class="stat-label">Total Users</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
            </div>
        </div>
        <div class="stat-card">
            <div>
                <div class="stat-value" id="stat-total-quizzes">--</div>
                <div class="stat-label">Active Quizzes</div>
            </div>
            <div class="stat-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#3B7354" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                </svg>
            </div>
        </div>
    </div>

    <!-- Section 1: Pending Quiz Approval Queue -->
    <div class="card" style="margin-bottom: 2.5rem;">
        <div class="card-header">
            <div>
                <h2 class="card-title">Pending Quiz Moderation Queue</h2>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                    Approve or reject quizzes authored by creators before they go live for participants.
                </p>
            </div>
            <button class="btn btn-outline btn-sm" onclick="loadPendingQuizzes()">Refresh</button>
        </div>

        <div class="table-responsive">
            <table class="table" id="pending-quizzes-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Description</th>
                        <th>Duration</th>
                        <th>Creator ID</th>
                        <th>Decision</th>
                    </tr>
                </thead>
                <tbody id="pending-quizzes-tbody">
                    <tr>
                        <td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                            Loading pending approval queue...
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Section 2: User Management -->
    <div class="card" style="margin-bottom: 2.5rem;">
        <div class="card-header">
            <div>
                <h2 class="card-title">User Account Governance</h2>
                <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">
                    Manage permissions, assign administrative roles, or remove accounts.
                </p>
            </div>
            <button class="btn btn-outline btn-sm" onclick="loadUsers()">Refresh</button>
        </div>

        <div class="table-responsive">
            <table class="table" id="users-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Full Name</th>
                        <th>Email</th>
                        <th>Current Role</th>
                        <th>Creator Rank &amp; Publish</th>
                        <th>Change Role</th>
                        <th>Created At</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="users-tbody">
                    <tr>
                        <td colspan="8" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
                            Loading user accounts...
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Section 3: Platform Settings -->
    <div class="card" style="margin-bottom: 3rem;">
        <div class="card-header">
            <h2 class="card-title">System Settings &amp; Anti-Cheat Policies</h2>
        </div>
        <div class="card-body">
            <form id="settings-form">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem;">
                    <div class="form-group">
                        <label for="setting-tab-switches" class="form-label">Max Allowed Tab Switches Before Flagging</label>
                        <input type="number" id="setting-tab-switches" class="form-control" value="3" min="1" max="10">
                        <p class="form-help">Number of violations before proctor alert is flagged.</p>
                    </div>

                    <div class="form-group">
                        <label for="setting-timeout-buffer" class="form-label">Auto-Submit Grace Period Buffer (Seconds)</label>
                        <input type="number" id="setting-timeout-buffer" class="form-control" value="5" min="0" max="60">
                        <p class="form-help">Extra seconds granted before hard auto-submit terminates exam.</p>
                    </div>

                    <div class="form-group">
                        <label for="setting-maintenance" class="form-label">Maintenance Mode</label>
                        <select id="setting-maintenance" class="form-select">
                            <option value="false">Disabled (System Operational)</option>
                            <option value="true">Enabled (Maintenance Banner)</option>
                        </select>
                    </div>
                </div>

                <div style="margin-top: 1.5rem; text-align: right;">
                    <button type="submit" id="save-settings-btn" class="btn btn-primary">
                        Save System Settings
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    var contextPath = '<%= request.getContextPath() %>';
</script>
<script src="<%= request.getContextPath() %>/js/admin.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
