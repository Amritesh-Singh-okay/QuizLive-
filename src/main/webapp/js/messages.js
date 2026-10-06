document.addEventListener('DOMContentLoaded', function() {
    var config = window.MESSAGES_CONFIG || { contextPath: '', currentUserId: 0, initialWithUser: null };
    var contextPath = config.contextPath;
    var currentUserId = config.currentUserId;
    var activePartnerId = config.initialWithUser;

    var contactList = document.getElementById('contact-list');
    var messagesContainer = document.getElementById('chat-messages-container');
    var activeRecipientName = document.getElementById('active-recipient-name');
    var activeRecipientBadge = document.getElementById('active-recipient-badge');
    var chatInput = document.getElementById('chat-input');
    var chatSendBtn = document.getElementById('chat-send-btn');
    var chatSendForm = document.getElementById('chat-send-form');
    var newMsgModal = document.getElementById('new-msg-modal');
    var newMsgForm = document.getElementById('new-msg-form');

    var pollInterval = null;

    // Load recent messages / contacts
    function loadContacts() {
        fetch(contextPath + '/api/messages')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!contactList) return;

                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    if (!activePartnerId) {
                        contactList.innerHTML = '<li style="padding: 1.5rem; text-align: center; color: var(--text-muted); font-size: 0.85rem;">No recent messages found. Click "+ New Conversation" to start one.</li>';
                    }
                    return;
                }

                var allMsgs = resData.data;
                var partners = {};

                allMsgs.forEach(function(m) {
                    var partnerId = (m.fromUserId === currentUserId) ? m.toUserId : m.fromUserId;
                    if (!partners[partnerId]) {
                        partners[partnerId] = {
                            id: partnerId,
                            lastContent: m.content,
                            lastTime: m.sentAt
                        };
                    }
                });

                contactList.innerHTML = '';
                var partnerIds = Object.keys(partners);

                partnerIds.forEach(function(pIdStr) {
                    var pId = parseInt(pIdStr, 10);
                    var pData = partners[pId];

                    var li = document.createElement('li');
                    li.className = 'contact-item' + (pId === activePartnerId ? ' active' : '');
                    li.innerHTML =
                        '<div>' +
                            '<div style="font-weight: 700; font-size: 0.95rem;">User #' + pId + '</div>' +
                            '<div style="font-size: 0.8rem; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px;">' +
                                escapeHtml(pData.lastContent || 'Start talking...') +
                            '</div>' +
                        '</div>' +
                        '<div style="font-size: 0.7rem; color: var(--text-muted);">' +
                            (pData.lastTime ? formatTime(pData.lastTime) : '') +
                        '</div>';

                    li.onclick = function() {
                        openThread(pId);
                    };

                    contactList.appendChild(li);
                });

                if (activePartnerId && !partners[activePartnerId]) {
                    var li = document.createElement('li');
                    li.className = 'contact-item active';
                    li.innerHTML =
                        '<div>' +
                            '<div style="font-weight: 700; font-size: 0.95rem;">User #' + activePartnerId + '</div>' +
                            '<div style="font-size: 0.8rem; color: var(--text-secondary);">Direct thread</div>' +
                        '</div>';
                    contactList.prepend(li);
                }
            })
            .catch(function(err) {});
    }

    function openThread(partnerId) {
        activePartnerId = partnerId;
        if (activeRecipientName) activeRecipientName.textContent = 'Conversation with User #' + partnerId;
        if (activeRecipientBadge) {
            activeRecipientBadge.textContent = 'Active Partner';
            activeRecipientBadge.className = 'badge badge-creator';
            activeRecipientBadge.style.display = 'inline-block';
        }

        if (chatInput) chatInput.disabled = false;
        if (chatSendBtn) chatSendBtn.disabled = false;

        // Highlight active contact
        var items = contactList.querySelectorAll('.contact-item');
        items.forEach(function(item) {
            item.classList.remove('active');
        });

        loadThreadMessages(partnerId);

        // Start polling for this thread
        if (pollInterval) clearInterval(pollInterval);
        pollInterval = setInterval(function() {
            if (activePartnerId) {
                loadThreadMessages(activePartnerId, true);
            }
        }, 3000);
    }

    function loadThreadMessages(partnerId, isPoll) {
        fetch(contextPath + '/api/messages?withUser=' + partnerId)
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!messagesContainer) return;

                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    if (!isPoll) {
                        messagesContainer.innerHTML = '<div style="text-align: center; margin: auto; color: var(--text-muted); font-size: 0.9rem;">No messages exchanged yet with User #' + partnerId + '. Send a message to start the conversation.</div>';
                    }
                    return;
                }

                var messages = resData.data;
                var html = '';

                messages.forEach(function(m) {
                    var isSent = (m.fromUserId === currentUserId);
                    var bubbleClass = isSent ? 'chat-bubble chat-bubble-sent' : 'chat-bubble chat-bubble-received';

                    html +=
                        '<div class="' + bubbleClass + '">' +
                            '<div>' + escapeHtml(m.content) + '</div>' +
                            '<div class="chat-time">' + formatTime(m.sentAt) + '</div>' +
                        '</div>';
                });

                messagesContainer.innerHTML = html;
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            })
            .catch(function(err) {});
    }

    // Send Message
    if (chatSendForm) {
        chatSendForm.addEventListener('submit', function(e) {
            e.preventDefault();
            if (!activePartnerId) return;

            var text = chatInput.value.trim();
            if (!text) return;

            chatInput.value = '';

            fetch(contextPath + '/api/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    toUserId: activePartnerId,
                    content: text
                })
            })
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success) {
                    loadThreadMessages(activePartnerId);
                    loadContacts();
                } else {
                    var err = (resData && resData.error) ? resData.error : 'Failed to send message';
                    if (window.showToast) window.showToast('error', err);
                }
            })
            .catch(function(err) {
                if (window.showToast) window.showToast('error', 'Network error sending message.');
            });
        });
    }

    // New Conversation Modal
    window.openNewMsgModal = function() {
        if (newMsgModal) newMsgModal.classList.add('show');
    };

    window.closeNewMsgModal = function() {
        if (newMsgModal) newMsgModal.classList.remove('show');
    };

    if (newMsgForm) {
        newMsgForm.addEventListener('submit', function(e) {
            e.preventDefault();
            var targetId = parseInt(document.getElementById('new-recipient-id').value, 10);
            var text = document.getElementById('new-msg-text').value.trim();

            if (!targetId || !text) return;

            fetch(contextPath + '/api/messages', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    toUserId: targetId,
                    content: text
                })
            })
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success) {
                    closeNewMsgModal();
                    openThread(targetId);
                    loadContacts();
                    if (window.showToast) window.showToast('success', 'Message sent!');
                } else {
                    var err = (resData && resData.error) ? resData.error : 'Failed to send message';
                    if (window.showToast) window.showToast('error', err);
                }
            })
            .catch(function(err) {
                if (window.showToast) window.showToast('error', 'Network error sending message.');
            });
        });
    }

    function formatTime(ts) {
        if (!ts) return '';
        try {
            var d = new Date(ts);
            return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            return '';
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

    loadContacts();
    if (activePartnerId) {
        openThread(activePartnerId);
    }
});
