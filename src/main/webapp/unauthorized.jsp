<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Access Denied - QuizLive");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 520px; text-align: center; margin-top: 2rem;">
    <div class="card" style="padding: 2rem;">
        <div style="font-size: 3.5rem; margin-bottom: 1rem;">🚫</div>
        <h1 style="font-size: 1.6rem; font-weight: 800; color: var(--danger); margin-bottom: 0.5rem;">
            Access Denied
        </h1>
        <p style="color: var(--text-secondary); margin-bottom: 1.5rem;">
            You do not have the required permissions or role to access this resource.
        </p>
        <div style="display: flex; gap: 0.75rem; justify-content: center;">
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
