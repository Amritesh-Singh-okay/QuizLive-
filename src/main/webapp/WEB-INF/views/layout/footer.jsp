<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String footerContextPath = request.getContextPath();
%>
    </main>

    <footer class="footer">
        <div class="container">
            <div style="display: flex; flex-direction: column; gap: 0.35rem;">
                <div>
                    <strong>QuizLive</strong> &copy; <%= java.time.Year.now().getValue() %>: Proctored Assessment Platform. All rights reserved.
                </div>
                <div style="font-size: 0.8rem; color: var(--text-muted);">
                    Dedicated to transparent, reliable, and proctored examination workflows.
                </div>
            </div>
            <div style="display: flex; align-items: center; gap: 1.25rem; flex-wrap: wrap;">
                <span class="live-badge" style="font-size: 0.75rem;">
                    <span class="live-dot"></span> System Operational
                </span>
                <a href="<%= footerContextPath %>/privacy.jsp" style="color: var(--text-muted); font-size: 0.8rem;">Privacy Policy</a>
                <a href="<%= footerContextPath %>/terms.jsp" style="color: var(--text-muted); font-size: 0.8rem;">Terms of Service</a>
                <a href="<%= footerContextPath %>/health" target="_blank" style="color: var(--text-muted); font-size: 0.8rem;">Health Check</a>
                <a href="<%= footerContextPath %>/leaderboard.jsp" style="color: var(--text-muted); font-size: 0.8rem;">Live Board</a>
            </div>
        </div>
    </footer>

    <script>
        (function() {
            var toggle = document.getElementById('nav-toggle');
            var menu = document.getElementById('nav-menu');
            if (toggle && menu) {
                toggle.addEventListener('click', function() {
                    menu.classList.toggle('show');
                });
            }

            window.showToast = function(type, message, duration) {
                duration = duration || 4000;
                var container = document.getElementById('toast-container');
                if (!container) return;

                var toast = document.createElement('div');
                toast.className = 'toast toast-' + (type || 'info');

                var iconSvg = '';
                if (type === 'success') {
                    iconSvg = '<svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="#3B7354" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 10l3 3 7-7"/><circle cx="10" cy="10" r="9"/></svg>';
                } else if (type === 'error') {
                    iconSvg = '<svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="#A3383B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="10" cy="10" r="9"/><line x1="10" y1="6" x2="10" y2="10"/><line x1="10" y1="14" x2="10.01" y2="14"/></svg>';
                } else if (type === 'warning') {
                    iconSvg = '<svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="#B87333" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 2L1 18h18L10 2z"/><line x1="10" y1="8" x2="10" y2="12"/><line x1="10" y1="15" x2="10.01" y2="15"/></svg>';
                } else {
                    iconSvg = '<svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="#60527A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="10" cy="10" r="9"/><line x1="10" y1="10" x2="10" y2="14"/><line x1="10" y1="6" x2="10.01" y2="6"/></svg>';
                }

                var iconSpan = document.createElement('span');
                iconSpan.style.display = 'inline-flex';
                iconSpan.style.alignItems = 'center';
                iconSpan.style.flexShrink = '0';
                iconSpan.innerHTML = iconSvg;

                var msgSpan = document.createElement('span');
                msgSpan.className = 'toast-message';
                msgSpan.textContent = message || '';

                var closeBtn = document.createElement('button');
                closeBtn.className = 'toast-close';
                closeBtn.innerHTML = '&times;';
                closeBtn.setAttribute('aria-label', 'Close toast');
                closeBtn.onclick = function() {
                    toast.classList.add('toast-hiding');
                    setTimeout(function() { toast.remove(); }, 300);
                };

                toast.appendChild(iconSpan);
                toast.appendChild(msgSpan);
                toast.appendChild(closeBtn);

                container.appendChild(toast);

                setTimeout(function() {
                    if (toast.parentElement) {
                        toast.classList.add('toast-hiding');
                        setTimeout(function() { toast.remove(); }, 300);
                    }
                }, duration);
            };
        })();
    </script>
</body>
</html>
