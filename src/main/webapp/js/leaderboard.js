document.addEventListener('DOMContentLoaded', function() {
    var config = window.LEADERBOARD_CONFIG || { contextPath: '', initialQuizId: 1 };
    var contextPath = config.contextPath;
    var currentQuizId = config.initialQuizId || 1;

    var socket = null;
    var reconnectTimeout = null;
    var reconnectAttempts = 0;

    var quizSelect = document.getElementById('quiz-select');
    var statusBadge = document.getElementById('ws-status-badge');
    var statusText = document.getElementById('ws-status-text');
    var statusDot = document.getElementById('ws-dot');
    var tbody = document.getElementById('leaderboard-tbody');
    var countBadge = document.getElementById('participant-count-badge');

    // Load Quizzes Dropdown
    function loadQuizzes() {
        fetch(contextPath + '/api/quizzes')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success && resData.data && resData.data.length > 0) {
                    if (quizSelect) {
                        quizSelect.innerHTML = '';
                        var foundCurrent = false;

                        resData.data.forEach(function(q) {
                            var opt = document.createElement('option');
                            opt.value = q.id;
                            opt.textContent = '#' + q.id + ': ' + q.title;
                            if (q.id === currentQuizId) {
                                opt.selected = true;
                                foundCurrent = true;
                            }
                            quizSelect.appendChild(opt);
                        });

                        if (!foundCurrent && resData.data.length > 0) {
                            currentQuizId = resData.data[0].id;
                            quizSelect.value = currentQuizId;
                        }
                    }
                }
                connectWebSocket(currentQuizId);
            })
            .catch(function(err) {
                connectWebSocket(currentQuizId);
            });
    }

    if (quizSelect) {
        quizSelect.addEventListener('change', function() {
            var selectedId = parseInt(this.value, 10);
            if (selectedId && selectedId !== currentQuizId) {
                currentQuizId = selectedId;
                if (socket) {
                    socket.close();
                }
                clearTimeout(reconnectTimeout);
                tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2.5rem;">Connecting to Quiz #' + currentQuizId + ' rankings...</td></tr>';
                connectWebSocket(currentQuizId);
            }
        });
    }

    // Connect WebSocket
    function connectWebSocket(quizId) {
        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
            socket.close();
        }

        var protocol = (window.location.protocol === 'https:') ? 'wss://' : 'ws://';
        var wsUrl = protocol + window.location.host + contextPath + '/ws/leaderboard/' + quizId;

        updateStatus('connecting', 'Connecting WebSocket...');

        // Fallback HTTP immediate load
        fetchHttpLeaderboard(quizId);

        try {
            socket = new WebSocket(wsUrl);

            socket.onopen = function() {
                reconnectAttempts = 0;
                updateStatus('connected', 'Live (WebSocket Push Active)');
            };

            socket.onmessage = function(event) {
                try {
                    var payload = JSON.parse(event.data);
                    if (payload && payload.type === 'LEADERBOARD_UPDATE') {
                        renderLeaderboard(payload.data || []);
                    }
                } catch (e) {
                }
            };

            socket.onclose = function(e) {
                updateStatus('reconnecting', 'Reconnecting...');
                scheduleReconnect(quizId);
            };

            socket.onerror = function(e) {
                updateStatus('reconnecting', 'Connection error');
            };
        } catch (e) {
            updateStatus('reconnecting', 'Reconnecting...');
            scheduleReconnect(quizId);
        }
    }

    function scheduleReconnect(quizId) {
        clearTimeout(reconnectTimeout);
        reconnectAttempts++;
        var delay = Math.min(1000 * Math.pow(1.5, reconnectAttempts), 8000);
        reconnectTimeout = setTimeout(function() {
            connectWebSocket(quizId);
        }, delay);
    }

    function updateStatus(state, msg) {
        if (!statusBadge || !statusText) return;
        statusText.textContent = msg;

        if (state === 'connected') {
            statusBadge.className = 'live-badge';
            statusDot.style.backgroundColor = 'var(--success)';
            statusDot.style.boxShadow = '0 0 8px var(--success)';
        } else if (state === 'connecting' || state === 'reconnecting') {
            statusBadge.className = 'live-badge';
            statusBadge.style.color = 'var(--warning)';
            statusBadge.style.borderColor = 'rgba(245, 158, 11, 0.4)';
            statusBadge.style.backgroundColor = 'rgba(245, 158, 11, 0.15)';
            statusDot.style.backgroundColor = 'var(--warning)';
            statusDot.style.boxShadow = '0 0 8px var(--warning)';
        } else {
            statusBadge.className = 'live-badge';
            statusDot.style.backgroundColor = 'var(--danger)';
            statusDot.style.boxShadow = '0 0 8px var(--danger)';
        }
    }

    // Fallback HTTP Load
    function fetchHttpLeaderboard(quizId) {
        fetch(contextPath + '/api/leaderboard?quizId=' + quizId)
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success && Array.isArray(resData.data)) {
                    renderLeaderboard(resData.data);
                }
            })
            .catch(function(err) {
            });
    }

    // Render Podium & Table
    function renderLeaderboard(entries) {
        if (!entries || !tbody) return;

        if (countBadge) {
            countBadge.textContent = entries.length + ' Participant' + (entries.length === 1 ? '' : 's');
        }

        // Reset Podium
        resetPodium();

        if (entries.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2.5rem;">No submissions yet for this quiz. Be the first to compete!</td></tr>';
            return;
        }

        // Render Podium Cards
        if (entries[0]) setPodiumCard(1, entries[0]);
        if (entries[1]) setPodiumCard(2, entries[1]);
        if (entries[2]) setPodiumCard(3, entries[2]);

        // Render Table Rows
        tbody.innerHTML = '';
        entries.forEach(function(entry) {
            var tr = document.createElement('tr');
            tr.className = 'row-highlight';

            var rankBadge = '';
            if (entry.rank === 1) rankBadge = '<span class="badge" style="background: var(--warning-bg); color: var(--warning); border: 1px solid var(--warning-border);">Rank 1</span>';
            else if (entry.rank === 2) rankBadge = '<span class="badge" style="background: var(--primary-light); color: var(--primary); border: 1px solid var(--lavender-border);">Rank 2</span>';
            else if (entry.rank === 3) rankBadge = '<span class="badge" style="background: #F8F1EB; color: #9C6738; border: 1px solid #E5D0C0;">Rank 3</span>';
            else rankBadge = '<strong>#' + entry.rank + '</strong>';

            var perc = (entry.percentage != null) ? entry.percentage.toFixed(1) + '%' : '--';

            var switchBadge = (entry.tabSwitches > 0)
                ? '<span class="badge ' + (entry.tabSwitches >= 3 ? 'badge-rejected' : 'badge-pending') + '">' + entry.tabSwitches + ' switches</span>'
                : '<span style="color: var(--success);">0</span>';

            var dateStr = entry.submittedAt ? formatTimestamp(entry.submittedAt) : 'Just now';

            tr.innerHTML =
                '<td>' + rankBadge + '</td>' +
                '<td><strong>' + escapeHtml(entry.userName || 'Anonymous') + '</strong></td>' +
                '<td><span style="font-weight: 800; color: var(--text-primary);">' + entry.score + '</span> / ' + entry.maxScore + '</td>' +
                '<td>' + perc + '</td>' +
                '<td>' + (entry.formattedDuration || '00:00') + '</td>' +
                '<td>' + switchBadge + '</td>' +
                '<td style="color: var(--text-secondary); font-size: 0.85rem;">' + dateStr + '</td>';

            tbody.appendChild(tr);
        });
    }

    function resetPodium() {
        for (var i = 1; i <= 3; i++) {
            var nameEl = document.getElementById('podium-' + i + '-name');
            var scoreEl = document.getElementById('podium-' + i + '-score');
            var metaEl = document.getElementById('podium-' + i + '-meta');
            if (nameEl) nameEl.textContent = '--';
            if (scoreEl) scoreEl.textContent = '-- pts';
            if (metaEl) metaEl.textContent = '--';
        }
    }

    function setPodiumCard(rank, entry) {
        var nameEl = document.getElementById('podium-' + rank + '-name');
        var scoreEl = document.getElementById('podium-' + rank + '-score');
        var metaEl = document.getElementById('podium-' + rank + '-meta');

        if (nameEl) nameEl.textContent = entry.userName || 'Participant';
        if (scoreEl) scoreEl.textContent = entry.score + ' pts';
        if (metaEl) {
            var perc = (entry.percentage != null) ? entry.percentage.toFixed(1) + '%' : '';
            var dur = entry.formattedDuration || '';
            metaEl.textContent = perc + ' accuracy • ' + dur;
        }
    }

    window.triggerRefresh = function() {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send("refresh");
            if (window.showToast) window.showToast('info', 'Sent live refresh request to server');
        } else {
            fetchHttpLeaderboard(currentQuizId);
            if (window.showToast) window.showToast('info', 'Refreshed leaderboard data');
        }
    };

    function formatTimestamp(ts) {
        try {
            var d = new Date(ts);
            return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        } catch (e) {
            return String(ts);
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    loadQuizzes();
});
