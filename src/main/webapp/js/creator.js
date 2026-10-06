document.addEventListener('DOMContentLoaded', function() {
    var form = document.getElementById('create-quiz-form');
    var container = document.getElementById('questions-container');
    var addBtnTop = document.getElementById('add-question-btn');
    var addBtnBottom = document.getElementById('add-question-btn-bottom');
    var countBadge = document.getElementById('question-count-badge');

    var questionCounter = 0;

    function addQuestion(initialData) {
        questionCounter++;
        var qId = questionCounter;

        var card = document.createElement('div');
        card.className = 'card question-form-card';
        card.id = 'question-card-' + qId;

        var prompt = initialData ? initialData.prompt : '';
        var points = initialData ? initialData.points : 10;
        var optA = initialData ? initialData.optA : '';
        var optB = initialData ? initialData.optB : '';
        var optC = initialData ? initialData.optC : '';
        var optD = initialData ? initialData.optD : '';
        var correct = initialData ? initialData.correct : 'A';

        card.innerHTML =
            '<div class="card-header">' +
                '<h3 class="card-title question-number-title" style="font-size: 1.05rem;">Question #' + qId + '</h3>' +
                '<button type="button" class="btn btn-danger btn-sm delete-q-btn" style="padding: 0.25rem 0.6rem;">&times; Remove</button>' +
            '</div>' +
            '<div class="card-body">' +
                '<div class="form-group">' +
                    '<label class="form-label">Question Text *</label>' +
                    '<textarea class="form-textarea q-text-input" placeholder="Type question prompt..." required>' + prompt + '</textarea>' +
                '</div>' +
                '<div class="form-group" style="max-width: 200px;">' +
                    '<label class="form-label">Points</label>' +
                    '<input type="number" class="form-control q-points-input" value="' + points + '" min="1" max="100">' +
                '</div>' +
                '<div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem;">' +
                    '<div class="form-group">' +
                        '<label class="form-label">Option A *</label>' +
                        '<input type="text" class="form-control q-opt-a" placeholder="Option A text" value="' + optA + '" required>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Option B *</label>' +
                        '<input type="text" class="form-control q-opt-b" placeholder="Option B text" value="' + optB + '" required>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Option C *</label>' +
                        '<input type="text" class="form-control q-opt-c" placeholder="Option C text" value="' + optC + '" required>' +
                    '</div>' +
                    '<div class="form-group">' +
                        '<label class="form-label">Option D *</label>' +
                        '<input type="text" class="form-control q-opt-d" placeholder="Option D text" value="' + optD + '" required>' +
                    '</div>' +
                '</div>' +
                '<div class="form-group" style="margin-top: 1rem;">' +
                    '<label class="form-label" style="color: var(--secondary);">Correct Option *</label>' +
                    '<select class="form-select q-correct-select" style="max-width: 260px;">' +
                        '<option value="A"' + (correct === 'A' ? ' selected' : '') + '>Option A</option>' +
                        '<option value="B"' + (correct === 'B' ? ' selected' : '') + '>Option B</option>' +
                        '<option value="C"' + (correct === 'C' ? ' selected' : '') + '>Option C</option>' +
                        '<option value="D"' + (correct === 'D' ? ' selected' : '') + '>Option D</option>' +
                    '</select>' +
                '</div>' +
            '</div>';

        var delBtn = card.querySelector('.delete-q-btn');
        delBtn.onclick = function() {
            var cards = container.querySelectorAll('.question-form-card');
            if (cards.length <= 1) {
                if (window.showToast) window.showToast('warning', 'A quiz must contain at least one question.');
                return;
            }
            card.remove();
            renumberQuestions();
        };

        container.appendChild(card);
        renumberQuestions();
    }

    function renumberQuestions() {
        var cards = container.querySelectorAll('.question-form-card');
        cards.forEach(function(card, idx) {
            var title = card.querySelector('.question-number-title');
            if (title) title.textContent = 'Question #' + (idx + 1);
        });
        if (countBadge) countBadge.textContent = cards.length;
    }

    if (addBtnTop) addBtnTop.addEventListener('click', function() { addQuestion(); });
    if (addBtnBottom) addBtnBottom.addEventListener('click', function() { addQuestion(); });

    // Initialize with 2 sample questions if container empty
    if (container && container.children.length === 0) {
        addQuestion({
            prompt: 'Which Java collection class is thread-safe and implements the Map interface?',
            points: 10,
            optA: 'HashMap',
            optB: 'ConcurrentHashMap',
            optC: 'TreeMap',
            optD: 'LinkedHashMap',
            correct: 'B'
        });
        addQuestion({
            prompt: 'What happens when visibilitychange fires with document.hidden === true during a QuizLive exam?',
            points: 10,
            optA: 'Nothing happens',
            optB: 'The server logs an anti-cheat tab-switch violation',
            optC: 'The browser closes immediately',
            optD: 'The question is automatically marked incorrect',
            correct: 'B'
        });
    }

    // Form Submit
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();

            var title = document.getElementById('quiz-title').value.trim();
            var desc = document.getElementById('quiz-desc').value.trim();
            var duration = parseInt(document.getElementById('quiz-duration').value, 10);
            var submitBtn = document.getElementById('save-quiz-btn');

            if (!title) {
                if (window.showToast) window.showToast('error', 'Please provide a quiz title');
                return;
            }

            var questionCards = container.querySelectorAll('.question-form-card');
            if (questionCards.length === 0) {
                if (window.showToast) window.showToast('error', 'Please add at least one question');
                return;
            }

            var questionsList = [];
            var hasValidationError = false;

            questionCards.forEach(function(card, idx) {
                if (hasValidationError) return;

                var prompt = card.querySelector('.q-text-input').value.trim();
                var points = parseInt(card.querySelector('.q-points-input').value, 10) || 10;
                var optA = card.querySelector('.q-opt-a').value.trim();
                var optB = card.querySelector('.q-opt-b').value.trim();
                var optC = card.querySelector('.q-opt-c').value.trim();
                var optD = card.querySelector('.q-opt-d').value.trim();
                var correct = card.querySelector('.q-correct-select').value;

                if (!prompt || !optA || !optB || !optC || !optD) {
                    hasValidationError = true;
                    if (window.showToast) {
                        window.showToast('error', 'Please fill all options and prompt for Question #' + (idx + 1));
                    }
                    return;
                }

                questionsList.push({
                    questionText: prompt,
                    points: points,
                    optionA: optA,
                    optionB: optB,
                    optionC: optC,
                    optionD: optD,
                    correctOption: correct
                });
            });

            if (hasValidationError) return;

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Publishing Quiz...';
            }

            var contextPath = form.getAttribute('data-context-path') || '';

            var payload = {
                title: title,
                description: desc,
                durationSeconds: duration,
                questions: questionsList
            };

            fetch(contextPath + '/api/quizzes/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            })
            .then(function(res) {
                return res.json().then(function(data) {
                    return { status: res.status, ok: res.ok, data: data };
                });
            })
            .then(function(result) {
                if (result.ok && result.data && result.data.success) {
                    if (window.showToast) {
                        window.showToast('success', 'Quiz created successfully! Submitted for administrator approval.');
                    }
                    setTimeout(function() {
                        window.location.href = contextPath + '/creator/dashboard.jsp';
                    }, 800);
                } else {
                    var err = (result.data && result.data.error) ? result.data.error : 'Failed to create quiz';
                    if (window.showToast) window.showToast('error', err);
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = 'Publish Quiz for Approval';
                    }
                }
            })
            .catch(function(err) {
                if (window.showToast) window.showToast('error', 'Network error creating quiz.');
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Publish Quiz for Approval';
                }
            });
        });
    }
});
