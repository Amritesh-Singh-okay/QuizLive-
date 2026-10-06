<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("pageTitle", "Privacy Policy - QuizLive");
    request.setAttribute("activeNav", "");
%>
<jsp:include page="/WEB-INF/views/layout/header.jsp" />

<div class="container" style="max-width: 860px; padding: 2rem 1.5rem 4rem;">
    <div style="margin-bottom: 2.5rem;">
        <span class="badge badge-participant" style="margin-bottom: 0.75rem;">Compliance &amp; Data Transparency</span>
        <h1 style="font-size: 2.25rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.5rem; letter-spacing: -0.02em;">
            Privacy Policy
        </h1>
        <p style="color: var(--text-muted); font-size: 0.95rem;">
            Last updated: October 2026. This policy describes how QuizLive handles personal information and examination data.
        </p>
    </div>

    <div class="card" style="margin-bottom: 2rem;">
        <div class="card-body" style="padding: 2rem;">
            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    1. Information We Collect
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7; margin-bottom: 1rem;">
                    QuizLive collects only the minimal data required to provide a reliable, proctored assessment experience. When you register or participate in tests, we process:
                </p>
                <ul style="color: var(--text-secondary); line-height: 1.7; padding-left: 1.5rem; margin-bottom: 1rem;">
                    <li><strong>Account Credentials:</strong> Full name, verified email address, role designation (Participant or Quiz Creator), and cryptographically salted password hashes.</li>
                    <li><strong>Assessment Responses:</strong> Selected answer options, timestamps of submission, total score, and calculated accuracy percentages.</li>
                    <li><strong>Integrity Telemetry:</strong> Page visibility events (such as tab switches or loss of window focus) detected exclusively during active quiz attempts.</li>
                </ul>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    2. Anti-Cheat Monitoring &amp; Proctoring Transparency
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7; margin-bottom: 1rem;">
                    To maintain academic integrity in timed assessments, QuizLive utilizes standard browser Page Visibility APIs. When an examination session begins:
                </p>
                <ul style="color: var(--text-secondary); line-height: 1.7; padding-left: 1.5rem; margin-bottom: 1rem;">
                    <li>Events are logged only while an assessment attempt is actively in progress. No background monitoring occurs before an exam starts or after it is submitted.</li>
                    <li>We do not record audio, video, webcams, keystrokes, or peripheral screen contents.</li>
                    <li>Tab switch incidents are recorded as a count to assist instructors and administrators in fair evaluation.</li>
                </ul>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    3. How We Use Your Data
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7; margin-bottom: 1rem;">
                    Your data is used solely to:
                </p>
                <ul style="color: var(--text-secondary); line-height: 1.7; padding-left: 1.5rem; margin-bottom: 1rem;">
                    <li>Authenticate account access and enforce role permissions.</li>
                    <li>Score test responses instantly using server-side grading algorithms.</li>
                    <li>Provide real-time leaderboard rankings to authorized participants and spectators.</li>
                    <li>Allow quiz creators to review performance analytics and investigate integrity flags.</li>
                </ul>
            </section>

            <section style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    4. Data Security &amp; Storage
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    Passwords are never stored in plaintext; each account utilizes a unique cryptographic salt combined with SHA-256 hashing. All application sessions are isolated using secure HTTP cookies, and access to administrative consoles is restricted to verified administrators.
                </p>
            </section>

            <section>
                <h2 style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.75rem;">
                    5. Participant Rights &amp; Contact
                </h2>
                <p style="color: var(--text-secondary); line-height: 1.7;">
                    Participants may review their submission history at any time from the participant dashboard. For questions regarding your personal information or account removal requests, contact your institutional platform administrator or write to compliance@quizlive.internal.
                </p>
            </section>
        </div>
    </div>

    <div style="text-align: center;">
        <a href="<%= request.getContextPath() %>/index.jsp" class="btn btn-secondary">Return to Home</a>
    </div>
</div>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
