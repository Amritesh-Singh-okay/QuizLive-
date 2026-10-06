<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String footerContextPath = request.getContextPath();
%>
    </main>

    <footer class="footer">
        <div class="container">
            <div>
                <strong>QuizLive</strong> &copy; <%= java.time.Year.now().getValue() %> &mdash; Real-Time Proctored Quiz Engine. All rights reserved.
            </div>
            <div style="display: flex; align-items: center; gap: 1.25rem;">
                <span class="live-badge" style="font-size: 0.75rem;">
                    <span class="live-dot"></span> System Operational
                </span>
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

                var icon = 'ℹ️';
                if (type === 'success') icon = '✅';
                else if (type === 'error') icon = '❌';
                else if (type === 'warning') icon = '⚠️';

                var iconSpan = document.createElement('span');
                iconSpan.style.fontSize = '1.1rem';
                iconSpan.textContent = icon;

                var msgSpan = document.createElement('span');
                msgSpan.className = 'toast-message';
                msgSpan.textContent = message || '';

                var closeBtn = document.createElement('button');
                closeBtn.className = 'toast-close';
                closeBtn.innerHTML = '&times;';
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
