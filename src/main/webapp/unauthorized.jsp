<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Access Denied - QuizLive");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 500px; text-align: center; margin-top: 2rem;">
    <div class="card" style="padding: 2.25rem 2rem;">
        <div style="width: 56px; height: 56px; margin: 0 auto 1.25rem; border-radius: var(--radius-sm); background-color: var(--danger-bg); display: flex; align-items: center; justify-content: center;">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#A3383B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
        </div>
        <h1 style="font-size: 1.5rem; font-weight: 800; color: var(--danger); margin-bottom: 0.5rem;">
            Access Denied
        </h1>
        <p style="color: var(--text-secondary); margin-bottom: 1.75rem; font-size: 0.925rem; line-height: 1.6;">
            Your account does not possess the required authorization or role to view this area.
        </p>
        <div style="display: flex; gap: 0.75rem; justify-content: center; flex-wrap: wrap;">
            <a href="<%= request.getContextPath() %>/index.jsp" class="btn btn-secondary">
                Return Home
            </a>
            <a href="<%= request.getContextPath() %>/login.jsp" class="btn btn-primary">
                Sign In With Another Account
            </a>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
