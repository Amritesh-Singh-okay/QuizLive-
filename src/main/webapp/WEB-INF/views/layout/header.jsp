<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%@ page import="com.quizlive.model.enums.Role" %>
<%
    String contextPath = request.getContextPath();
    AppUser currentUser = (session != null) ? (AppUser) session.getAttribute("user") : null;
    String pageTitle = (String) request.getAttribute("pageTitle");
    if (pageTitle == null || pageTitle.trim().isEmpty()) {
        pageTitle = "QuizLive: Proctored Timed Assessments";
    }
    String activeNav = (String) request.getAttribute("activeNav");
    if (activeNav == null) {
        activeNav = "";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= pageTitle %></title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/favicon.svg">
    <link rel="alternate icon" href="${pageContext.request.contextPath}/favicon.ico">
    <link rel="stylesheet" href="<%= contextPath %>/css/style.css">
</head>
<body>
    <div id="toast-container" class="toast-container"></div>

    <header class="navbar">
        <div class="container">
            <a href="<%= contextPath %>/index.jsp" class="nav-brand">
                <span class="brand-icon">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 2v20M2 12h20M4.93 4.93l14.14 14.14M4.93 19.07L19.07 4.93" stroke="#60527A" stroke-width="1.8" />
                        <circle cx="12" cy="12" r="4" fill="#60527A" />
                    </svg>
                </span>
                <span class="brand-name">QuizLive</span>
                <span class="brand-badge">Clinical Engine</span>
            </a>

            <button class="nav-toggle" id="nav-toggle" aria-label="Toggle navigation">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="3" y1="6" x2="21" y2="6"/>
                    <line x1="3" y1="12" x2="21" y2="12"/>
                    <line x1="3" y1="18" x2="21" y2="18"/>
                </svg>
            </button>

            <ul class="nav-menu" id="nav-menu">
                <li><a href="<%= contextPath %>/index.jsp" class="nav-link <%= "home".equals(activeNav) ? "active" : "" %>">Home</a></li>
                <li><a href="<%= contextPath %>/leaderboard.jsp" class="nav-link <%= "leaderboard".equals(activeNav) ? "active" : "" %>">Leaderboards</a></li>

                <% if (currentUser == null) { %>
                    <li><a href="<%= contextPath %>/login.jsp" class="nav-link <%= "login".equals(activeNav) ? "active" : "" %>">Sign In</a></li>
                    <li><a href="<%= contextPath %>/register.jsp" class="btn btn-primary btn-sm">Get Started</a></li>
                <% } else { %>
                    <% if (currentUser.getRole() == Role.PARTICIPANT) { %>
                        <li><a href="<%= contextPath %>/participant/dashboard.jsp" class="nav-link <%= "dashboard".equals(activeNav) ? "active" : "" %>">My Quizzes</a></li>
                        <li><a href="<%= contextPath %>/messages.jsp" class="nav-link <%= "messages".equals(activeNav) ? "active" : "" %>">Messages</a></li>
                    <% } else if (currentUser.getRole() == Role.CREATOR) { %>
                        <li><a href="<%= contextPath %>/creator/dashboard.jsp" class="nav-link <%= "dashboard".equals(activeNav) ? "active" : "" %>">Creator Hub</a></li>
                        <li><a href="<%= contextPath %>/creator/create-quiz.jsp" class="nav-link <%= "create-quiz".equals(activeNav) ? "active" : "" %>">New Quiz</a></li>
                        <li><a href="<%= contextPath %>/messages.jsp" class="nav-link <%= "messages".equals(activeNav) ? "active" : "" %>">Messages</a></li>
                    <% } else if (currentUser.getRole() == Role.ADMIN) { %>
                        <li><a href="<%= contextPath %>/admin/dashboard.jsp" class="nav-link <%= "dashboard".equals(activeNav) ? "active" : "" %>">Admin Console</a></li>
                        <li><a href="<%= contextPath %>/creator/dashboard.jsp" class="nav-link">Creator Hub</a></li>
                        <li><a href="<%= contextPath %>/participant/dashboard.jsp" class="nav-link">Participant View</a></li>
                        <li><a href="<%= contextPath %>/messages.jsp" class="nav-link <%= "messages".equals(activeNav) ? "active" : "" %>">Messages</a></li>
                    <% } %>

                    <li class="user-chip">
                        <span><%= currentUser.getName() %></span>
                        <% if (currentUser.getRole() == Role.ADMIN) { %>
                            <span class="badge badge-admin">Admin</span>
                        <% } else if (currentUser.getRole() == Role.CREATOR) { %>
                            <span class="badge badge-creator">Creator</span>
                        <% } else { %>
                            <span class="badge badge-participant">Participant</span>
                        <% } %>
                    </li>
                    <li><a href="<%= contextPath %>/logout" class="btn btn-outline btn-sm">Logout</a></li>
                <% } %>
            </ul>
        </div>
    </header>

    <main class="main-content">
