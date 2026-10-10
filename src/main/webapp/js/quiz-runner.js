document.addEventListener('DOMContentLoaded', function() {
    var config = window.QUIZ_CONFIG || { contextPath: '', quizId: 0, code: '' };
    var contextPath = config.contextPath;
    var quizId = config.quizId || 0;
    var quizCode = config.code || '';

    var attemptId = 0;
    var questions = [];
    var currentQuestionIndex = 0;
    var answers = {};
    var durationSeconds = 300;
    var remainingSeconds = 300;
    var timerInterval = null;
    var tabSwitches = 0;
    var isSubmitted = false;
    var lastBlurTime = 0;

    var titleEl = document.getElementById('quiz-title');
    var descEl = document.getElementById('quiz-desc');
    var timerEl = document.getElementById('timer-display');
    var paletteEl = document.getElementById('question-palette');
    var promptEl = document.getElementById('question-prompt');
    var pointsEl = document.getElementById('question-points');
    var currentQNumEl = document.getElementById('current-q-num');
    var totalQNumEl = document.getElementById('total-q-num');
    var optionsContainer = document.getElementById('options-container');
    var prevBtn = document.getElementById('prev-btn');
    var nextBtn = document.getElementById('next-btn');
    var submitQuizBtn = document.getElementById('submit-quiz-btn');
    var answeredCountEl = document.getElementById('answered-count');
    var tabSwitchCountEl = document.getElementById('tab-switch-count');
    var proctorBanner = document.getElementById('proctor-flagged-banner');
    var confirmModal = document.getElementById('confirm-modal');
    var resultsModal = document.getElementById('results-modal');

    // Waiting Room Elements
    var waitingContainer = document.getElementById('waiting-room-container');
    var examContainer = document.getElementById('exam-active-container');
    var waitingCountEl = document.getElementById('waiting-room-count');
    var waitingTitleEl = document.getElementById('waiting-quiz-title');
    var waitingDescEl = document.getElementById('waiting-quiz-desc');
    var scheduledCard = document.getElementById('scheduled-start-card');
    var scheduledCountdownEl = document.getElementById('scheduled-countdown');
    var lobbyStatusText = document.getElementById('lobby-status-text');

    var lobbyWs = null;
    var lobbyPollTimer = null;
    var scheduledTimer = null;
    var inWaitingRoom = false;

    function enterWaitingRoom(lobbyData) {
        inWaitingRoom = true;
        if (waitingContainer) waitingContainer.style.display = 'block';
        if (examContainer) examContainer.style.display = 'none';

        if (waitingTitleEl && lobbyData && lobbyData.title) {
            waitingTitleEl.textContent = lobbyData.title;
        }
        if (waitingDescEl && lobbyData && lobbyData.description) {
            waitingDescEl.textContent = lobbyData.description;
        }
        if (waitingCountEl && lobbyData && lobbyData.waitingCount != null) {
            waitingCountEl.textContent = Math.max(1, lobbyData.waitingCount);
        }

        if (lobbyData && lobbyData.scheduledStartAt) {
            setupScheduledCountdown(lobbyData.scheduledStartAt);
        }

        connectWaitingWs();
        startLobbyPolling();
    }

    function exitWaitingRoomAndLaunch() {
        if (!inWaitingRoom) return;
        inWaitingRoom = false;

        if (lobbyStatusText) {
            lobbyStatusText.textContent = 'Host started the quiz! Launching exam...';
        }
        if (window.showToast) {
            window.showToast('success', 'Host started the quiz! Launching assessment...');
        }

        if (lobbyWs) {
            try { lobbyWs.close(); } catch (e) {}
            lobbyWs = null;
        }
        if (lobbyPollTimer) {
            clearInterval(lobbyPollTimer);
            lobbyPollTimer = null;
        }
        if (scheduledTimer) {
            clearInterval(scheduledTimer);
            scheduledTimer = null;
        }

        setTimeout(function() {
            if (waitingContainer) waitingContainer.style.display = 'none';
            if (examContainer) examContainer.style.display = 'block';
            startQuiz();
        }, 700);
    }

    function connectWaitingWs() {
        if (lobbyWs) return;
        var protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
        var wsUrl = protocol + location.host + contextPath + '/ws/waiting-room/' + quizId;

        try {
            lobbyWs = new WebSocket(wsUrl);
            lobbyWs.onopen = function() {
                if (lobbyStatusText) lobbyStatusText.textContent = 'Connected & Live — Waiting for Host';
            };
            lobbyWs.onmessage = function(event) {
                try {
                    var data = JSON.parse(event.data);
                    if (data.type === 'LOBBY_UPDATE') {
                        if (waitingCountEl && data.waitingCount != null) {
                            waitingCountEl.textContent = Math.max(1, data.waitingCount);
                        }
                    } else if (data.type === 'QUIZ_STARTED') {
                        exitWaitingRoomAndLaunch();
                    } else if (data.type === 'QUIZ_HELD') {
                        if (lobbyStatusText) lobbyStatusText.textContent = 'Quiz held in lobby by host';
                    }
                } catch (e) {}
            };
            lobbyWs.onerror = function() {
                // Polling fallback operates in background
            };
            lobbyWs.onclose = function() {
                lobbyWs = null;
            };
        } catch (e) {
            // Polling fallback operates in background
        }
    }

    function startLobbyPolling() {
        if (lobbyPollTimer) return;
        lobbyPollTimer = setInterval(function() {
            if (!inWaitingRoom) {
                clearInterval(lobbyPollTimer);
                lobbyPollTimer = null;
                return;
            }

            fetch(contextPath + '/api/quizzes/lobby-status?quizId=' + quizId)
                .then(function(res) { return res.json(); })
                .then(function(resData) {
                    if (resData && resData.success && resData.data) {
                        var data = resData.data;
                        if (waitingCountEl && data.waitingCount != null) {
                            waitingCountEl.textContent = Math.max(1, data.waitingCount);
                        }
                        if (data.canStart === true) {
                            exitWaitingRoomAndLaunch();
                        } else if (data.scheduledStartAt) {
                            setupScheduledCountdown(data.scheduledStartAt);
                        }
                    }
                })
                .catch(function() {});
        }, 2500);
    }

    function setupScheduledCountdown(scheduledStartAtStr) {
        if (!scheduledStartAtStr) {
            if (scheduledCard) scheduledCard.style.display = 'none';
            return;
        }

        var targetMs = (typeof scheduledStartAtStr === 'number') ? scheduledStartAtStr : (new Date(scheduledStartAtStr)).getTime();
        if (isNaN(targetMs) || targetMs <= Date.now()) {
            if (scheduledCard) scheduledCard.style.display = 'none';
            return;
        }

        if (scheduledCard) scheduledCard.style.display = 'block';

        if (scheduledTimer) clearInterval(scheduledTimer);
        scheduledTimer = setInterval(function() {
            var diff = Math.floor((targetMs - Date.now()) / 1000);
            if (diff <= 0) {
                clearInterval(scheduledTimer);
                scheduledTimer = null;
                if (scheduledCountdownEl) scheduledCountdownEl.textContent = '00:00';
                exitWaitingRoomAndLaunch();
                return;
            }

            var m = Math.floor(diff / 60);
            var s = diff % 60;
            if (scheduledCountdownEl) {
                scheduledCountdownEl.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
            }
        }, 1000);
    }

    // Initialize Quiz
    function startQuiz() {
        var bodyPayload = {};
        if (quizId && quizId > 0) {
            bodyPayload.quizId = quizId;
        }
        if (quizCode) {
            bodyPayload.code = quizCode;
        }

        fetch(contextPath + '/api/attempts/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(bodyPayload)
        })
        .then(function(res) {
            return res.json().then(function(data) {
                return { status: res.status, ok: res.ok, data: data };
            });
        })
        .then(function(result) {
            if (result.status === 423 || (result.data && (result.data.waitingRoom || (result.data.data && result.data.data.waitingRoom)))) {
                var lobbyData = (result.data && result.data.data) ? result.data.data : {};
                if (lobbyData.quizId) {
                    quizId = lobbyData.quizId;
                }
                enterWaitingRoom(lobbyData);
                return;
            }

            if (result.status === 409) {
                var resolvedQuizId = (result.data && result.data.data && result.data.data.quizId) || (result.data && result.data.quizId) || quizId;
                if (resolvedQuizId) {
                    quizId = resolvedQuizId;
                }
                if (window.showToast) {
                    window.showToast('warning', 'You have already completed this quiz. Redirecting to leaderboard...');
                }
                setTimeout(function() {
                    window.location.href = contextPath + '/leaderboard.jsp?quizId=' + (quizId && quizId > 0 ? quizId : 1);
                }, 1500);
                return;
            }

            if (!result.ok || !result.data || !result.data.success) {
                var err = (result.data && result.data.error) ? result.data.error : 'Failed to start quiz attempt';
                if (titleEl) titleEl.textContent = 'Error: ' + err;
                if (window.showToast) window.showToast('error', err);
                return;
            }

            // Successfully started! Ensure waiting room components hidden
            if (waitingContainer) waitingContainer.style.display = 'none';
            if (examContainer) examContainer.style.display = 'block';
            if (lobbyWs) {
                try { lobbyWs.close(); } catch (e) {}
                lobbyWs = null;
            }
            if (lobbyPollTimer) {
                clearInterval(lobbyPollTimer);
                lobbyPollTimer = null;
            }
            if (scheduledTimer) {
                clearInterval(scheduledTimer);
                scheduledTimer = null;
            }

            var attemptData = result.data.data;
            attemptId = attemptData.attemptId;
            quizId = attemptData.quizId || quizId;
            durationSeconds = attemptData.durationSeconds || 300;
            remainingSeconds = durationSeconds;
            questions = attemptData.questions || [];

            if (titleEl) titleEl.textContent = attemptData.title || ('Quiz #' + quizId);
            if (descEl) descEl.textContent = attemptData.description || 'Proctored live exam session.';
            if (totalQNumEl) totalQNumEl.textContent = questions.length;

            if (questions.length === 0) {
                if (promptEl) promptEl.textContent = 'No questions available for this quiz.';
                if (submitQuizBtn) submitQuizBtn.disabled = true;
                return;
            }

            renderPalette();
            renderQuestion(0);
            startTimer();
            initAntiCheatListeners();
        })
        .catch(function(err) {
            if (titleEl) titleEl.textContent = 'Network error connecting to exam server.';
        });
    }

    // Timer Countdown
    function startTimer() {
        updateTimerDisplay();
        timerInterval = setInterval(function() {
            remainingSeconds--;
            updateTimerDisplay();

            if (remainingSeconds <= 0) {
                clearInterval(timerInterval);
                if (!isSubmitted) {
                    if (window.showToast) {
                        window.showToast('warning', 'Time expired! Auto-submitting quiz...');
                    }
                    executeSubmission(true);
                }
            }
        }, 1000);
    }

    function updateTimerDisplay() {
        if (!timerEl) return;
        var displaySeconds = Math.max(0, remainingSeconds);
        var m = Math.floor(displaySeconds / 60);
        var s = displaySeconds % 60;
        timerEl.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;

        if (displaySeconds <= 15) {
            timerEl.className = 'timer-digits critical';
        } else if (displaySeconds <= 60) {
            timerEl.className = 'timer-digits warning';
        } else {
            timerEl.className = 'timer-digits';
        }
    }

    // Anti-Cheat Proctoring
    function initAntiCheatListeners() {
        document.addEventListener('visibilitychange', function() {
            if (document.hidden && !isSubmitted && attemptId > 0) {
                handleViolation('Tab switch detected (page hidden)');
            }
        });

        window.addEventListener('blur', function() {
            var now = Date.now();
            if (now - lastBlurTime > 1500 && !isSubmitted && attemptId > 0) {
                lastBlurTime = now;
                handleViolation('Window lost focus');
            }
        });
    }

    function handleViolation(reason) {
        if (isSubmitted || attemptId <= 0) return;

        fetch(contextPath + '/api/attempts/tab-switch', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ attemptId: attemptId })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success && resData.data) {
                tabSwitches = resData.data.tabSwitches;
            } else {
                tabSwitches++;
            }
            updateViolationUI();
        })
        .catch(function(err) {
            tabSwitches++;
            updateViolationUI();
        });
    }

    function updateViolationUI() {
        if (tabSwitchCountEl) {
            tabSwitchCountEl.textContent = tabSwitches;
        }

        if (window.showToast) {
            window.showToast('error', 'Anti-Cheat Warning: Tab switch incident logged. (Count: ' + tabSwitches + ')', 5000);
        }

        if (tabSwitches >= 3 && proctorBanner) {
            proctorBanner.style.display = 'flex';
        }
    }

    // Question Rendering
    function renderPalette() {
        if (!paletteEl) return;
        paletteEl.innerHTML = '';

        questions.forEach(function(q, idx) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'palette-btn' + (idx === currentQuestionIndex ? ' current' : '') + (answers[q.id] ? ' answered' : '');
            btn.textContent = idx + 1;
            btn.onclick = function() {
                renderQuestion(idx);
            };
            paletteEl.appendChild(btn);
        });

        updateAnsweredCount();
    }

    function renderQuestion(index) {
        if (index < 0 || index >= questions.length) return;
        currentQuestionIndex = index;

        var q = questions[index];
        if (currentQNumEl) currentQNumEl.textContent = index + 1;
        if (promptEl) promptEl.textContent = q.questionText;
        if (pointsEl) pointsEl.textContent = (q.points || 10) + ' Points';

        if (optionsContainer) {
            optionsContainer.innerHTML = '';
            var opts = [
                { key: 'A', text: q.optionA },
                { key: 'B', text: q.optionB },
                { key: 'C', text: q.optionC },
                { key: 'D', text: q.optionD }
            ];

            opts.forEach(function(opt) {
                var card = document.createElement('label');
                var isSelected = answers[q.id] === opt.key;
                card.className = 'option-card' + (isSelected ? ' selected' : '');

                card.innerHTML =
                    '<input type="radio" name="question_' + q.id + '" value="' + opt.key + '"' + (isSelected ? ' checked' : '') + ' class="option-radio">' +
                    '<div class="option-prefix">' + opt.key + '</div>' +
                    '<div class="option-text">' + escapeHtml(opt.text) + '</div>';

                card.onclick = function(e) {
                    answers[q.id] = opt.key;
                    renderQuestion(currentQuestionIndex);
                    renderPalette();
                };

                optionsContainer.appendChild(card);
            });
        }

        if (prevBtn) prevBtn.disabled = (index === 0);
        if (nextBtn) nextBtn.disabled = (index === questions.length - 1);

        renderPalette();
    }

    function updateAnsweredCount() {
        var count = Object.keys(answers).length;
        if (answeredCountEl) {
            answeredCountEl.textContent = count + ' of ' + questions.length + ' answered';
        }
    }

    // Navigation Handlers
    if (prevBtn) {
        prevBtn.addEventListener('click', function() {
            if (currentQuestionIndex > 0) {
                renderQuestion(currentQuestionIndex - 1);
            }
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener('click', function() {
            if (currentQuestionIndex < questions.length - 1) {
                renderQuestion(currentQuestionIndex + 1);
            }
        });
    }

    // Submission Logic
    if (submitQuizBtn) {
        submitQuizBtn.addEventListener('click', function() {
            var unansweredWarning = document.getElementById('unanswered-warning');
            var answeredCount = Object.keys(answers).length;
            if (unansweredWarning) {
                if (answeredCount < questions.length) {
                    unansweredWarning.style.display = 'block';
                    unansweredWarning.textContent = 'You have only answered ' + answeredCount + ' of ' + questions.length + ' questions.';
                } else {
                    unansweredWarning.style.display = 'none';
                }
            }
            if (confirmModal) confirmModal.classList.add('show');
        });
    }

    window.closeConfirmModal = function() {
        if (confirmModal) confirmModal.classList.remove('show');
    };

    window.executeSubmission = function(isAutoSubmit) {
        if (isSubmitted) return;
        isSubmitted = true;
        if (timerInterval) clearInterval(timerInterval);

        var finalBtn = document.getElementById('final-submit-btn');
        if (finalBtn) {
            finalBtn.disabled = true;
            finalBtn.textContent = 'Grading...';
        }

        fetch(contextPath + '/api/attempts/submit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                attemptId: attemptId,
                answers: answers
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (confirmModal) confirmModal.classList.remove('show');

            if (resData && resData.success && resData.data) {
                var result = resData.data;
                document.getElementById('final-percentage').textContent = (result.percentage != null ? result.percentage.toFixed(1) : 0) + '%';
                document.getElementById('final-score').textContent = result.score;
                document.getElementById('final-max-score').textContent = result.maxScore;
                document.getElementById('final-status').textContent = result.status;
                document.getElementById('final-violations').textContent = result.tabSwitches + ' switches';

                var boardBtn = document.getElementById('view-leaderboard-btn');
                if (boardBtn) {
                    boardBtn.href = contextPath + '/leaderboard.jsp?quizId=' + quizId;
                }

                if (resultsModal) resultsModal.classList.add('show');
                if (window.showToast) {
                    window.showToast('success', 'Quiz submitted successfully! Score: ' + result.score + '/' + result.maxScore);
                }
            } else {
                var errMsg = (resData && resData.error) ? resData.error : 'Submission error';
                alert('Submission result: ' + errMsg);
                window.location.href = contextPath + '/leaderboard.jsp?quizId=' + quizId;
            }
        })
        .catch(function(err) {
            alert('Failed to submit attempt over network. Redirecting to dashboard.');
            window.location.href = contextPath + '/participant/dashboard.jsp';
        });
    };

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    startQuiz();
});
