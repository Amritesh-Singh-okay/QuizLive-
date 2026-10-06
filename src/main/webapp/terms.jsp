<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Terms and Conditions - QuizLive");
    request.setAttribute("activeNav", "");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 860px; padding: 2rem 1.5rem 4rem;">
    <div style="margin-bottom: 2.5rem;">
        <span class="badge badge-participant" style="margin-bottom: 0.75rem;">Platform Governance</span>
        <h1 style="font-size: 2.25rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.5rem; letter-spacing: -0.02em;">
            Terms and Conditions
        </h1>
        <p style="color: var(--text-muted); font-size: 0.95rem;">
            Effective: October 2026. Please read these terms carefully before accessing the QuizLive examination platform.
        </p>
    </div>

    <div class="card" style="margin-bottom: 2rem;">
        <div class="card-body" style="padding: 2rem;">
            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    1. Acceptance of Agreement
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    By creating an account or accessing timed examinations on QuizLive, you agree to be bound by these Terms and Conditions and our Privacy Policy. If you do not agree to these terms, you may not use the service.
                </p>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    2. User Roles &amp; Account Security
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7; margin-bottom: 1rem;">
                    QuizLive maintains strict role separation to protect academic integrity:
                </p>
                <ul style="color: var(--text-secondary); line-height: 1.7; padding-left: 1.5rem;">
                    <li><strong>Participant:</strong> Accounts created for individuals taking scheduled or open assessments.</li>
                    <li><strong>Quiz Creator:</strong> Accounts authorized to author test questions, configure durations, and review candidate results.</li>
                    <li><strong>Administrator:</strong> System governance accounts. Administrator privileges cannot be self-registered and may only be assigned by existing system administrators.</li>
                </ul>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    3. Academic Integrity &amp; Conduct Rules
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7; margin-bottom: 1rem;">
                    Participants must complete all examinations honestly and independently. Prohibited conduct includes:
                </p>
                <ul style="color: var(--text-secondary); line-height: 1.7; padding-left: 1.5rem;">
                    <li>Navigating away from the exam window or switching browser tabs during an active proctored quiz.</li>
                    <li>Using automated tools, bots, or script injection to manipulate timer countdowns or scores.</li>
                    <li>Sharing answers, test questions, or unauthorized assistance during live competitions.</li>
                    <li>Attempting to bypass security filters or access administrative endpoints.</li>
                </ul>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    4. Timed Assessments &amp; Automated Submissions
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    Each assessment includes an authoritative server-side timer. When the designated time limit expires, the system automatically finalizes and scores all saved answers. The server time stamp serves as the official record of completion.
                </p>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    5. Content Moderation &amp; Quiz Approval
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    All quizzes authored by creators are submitted to a platform moderation queue. Quizzes become visible on the public leaderboard only following administrator approval. We reserve the right to remove any content that violates ethical or academic standards.
                </p>
            </section>

            <section>
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    6. Limitation of Liability
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    QuizLive provides assessment tools on an as-available basis. While we strive for continuous service and sub-second leaderboard synchronization, QuizLive is not liable for client-side connection interruptions or hardware failures during an examination.
                </p>
            </section>
        </div>
    </div>

    <div style="text-align: center;">
        <a href="<%= request.getContextPath() %>/index.jsp" class="btn btn-secondary">Return to Home</a>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
