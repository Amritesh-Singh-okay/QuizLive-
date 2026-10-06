<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.quizlive.model.AppUser" %>
<%
    AppUser user = (session != null) ? (AppUser) session.getAttribute("user") : null;
    request.setAttribute("pageTitle", "Messages - QuizLive");
    request.setAttribute("activeNav", "messages");
    String withUserParam = request.getParameter("withUser");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 1050px;">
    <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;">
        <div>
            <h1 style="font-size: 1.85rem; font-weight: 800; color: var(--text-primary);">
                Direct Conversations 💬
            </h1>
            <p style="color: var(--text-secondary); font-size: 0.95rem;">
                Real-time communication between students, quiz creators, and proctors.
            </p>
        </div>
        <button type="button" class="btn btn-primary btn-sm" onclick="openNewMsgModal()">
            ✏️ New Conversation
        </button>
    </div>

    <!-- Chat Box -->
    <div class="chat-layout">
        <!-- Contacts Sidebar -->
        <div class="chat-contacts">
            <div class="chat-contacts-header">
                <span>Recent Threads</span>
            </div>
            <ul class="contact-list" id="contact-list">
                <li style="padding: 1.5rem; text-align: center; color: var(--text-muted); font-size: 0.85rem;">
                    Loading conversation threads...
                </li>
            </ul>
        </div>

        <!-- Chat Stream Pane -->
        <div class="chat-main">
            <div class="chat-header">
                <div>
                    <span id="active-recipient-name" style="font-weight: 700; font-size: 1.1rem; color: var(--text-primary);">
                        Select a conversation
                    </span>
                    <span id="active-recipient-badge" class="badge" style="display: none; margin-left: 0.5rem;"></span>
                </div>
                <div style="font-size: 0.8rem; color: var(--text-muted);" id="thread-quiz-badge"></div>
            </div>

            <div class="chat-messages" id="chat-messages-container">
                <div style="text-align: center; margin: auto; color: var(--text-muted); font-size: 0.9rem;">
                    Choose a contact from the list or start a new thread to view messages.
                </div>
            </div>

            <form id="chat-send-form" class="chat-input-bar">
                <input type="text" id="chat-input" class="form-control" placeholder="Type your message here..." autocomplete="off" disabled required>
                <button type="submit" id="chat-send-btn" class="btn btn-primary" disabled>
                    Send
                </button>
            </form>
        </div>
    </div>
</div>

<!-- New Conversation Modal -->
<div id="new-msg-modal" class="modal-overlay">
    <div class="modal-content">
        <div class="card-header">
            <h3 class="card-title">Start New Conversation</h3>
            <button type="button" class="toast-close" onclick="closeNewMsgModal()">&times;</button>
        </div>
        <form id="new-msg-form">
            <div class="card-body">
                <div class="form-group">
                    <label for="new-recipient-id" class="form-label">Recipient User ID *</label>
                    <input type="number" id="new-recipient-id" class="form-control" placeholder="e.g. 1 (Admin), 2 (Bob), 3 (Alice)" required min="1">
                    <p class="form-help">Enter the numerical ID of the user you wish to message.</p>
                </div>
                <div class="form-group">
                    <label for="new-msg-text" class="form-label">First Message *</label>
                    <textarea id="new-msg-text" class="form-textarea" placeholder="Type your message..." required></textarea>
                </div>
            </div>
            <div class="card-footer" style="justify-content: flex-end; gap: 0.75rem;">
                <button type="button" class="btn btn-secondary" onclick="closeNewMsgModal()">Cancel</button>
                <button type="submit" class="btn btn-primary">Send Message</button>
            </div>
        </form>
    </div>
</div>

<script>
    window.MESSAGES_CONFIG = {
        contextPath: '<%= request.getContextPath() %>',
        currentUserId: <%= (user != null) ? user.getId() : 0 %>,
        initialWithUser: <%= (withUserParam != null && !withUserParam.trim().isEmpty()) ? withUserParam.trim() : "null" %>
    };
</script>
<script src="<%= request.getContextPath() %>/js/messages.js"></script>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
