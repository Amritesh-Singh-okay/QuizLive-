<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%@ page import="com.quizlive.model.enums.Role" %>
<%
    String contextPath = request.getContextPath();
    AppUser currentUser = (session != null) ? (AppUser) session.getAttribute("user") : null;
    String pageTitle = (String) request.getAttribute("pageTitle");
    if (pageTitle == null || pageTitle.trim().isEmpty()) {
        pageTitle = "QuizLive - Real-Time Timed Quizzes";
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
    <link rel="stylesheet" href="<%= contextPath %>/css/style.css">
    <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>⚡</text></svg>">
</head>
<body>
    <div id="toast-container" class="toast-container"></div>

    <header class="navbar">
        <div class="container">
            <a href="<%= contextPath %>/index.jsp" class="nav-brand">
                <span class="brand-icon">⚡</span>
                <span>QuizLive</span>
                <span class="brand-badge">Live</span>
            </a>

            <button class="nav-toggle" id="nav-toggle" aria-label="Toggle navigation">☰</button>

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
                        <li><a href="<%= contextPath %>/creator/create-quiz.jsp" class="nav-link <%= "create-quiz".equals(activeNav) ? "active" : "" %>">+ New Quiz</a></li>
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
