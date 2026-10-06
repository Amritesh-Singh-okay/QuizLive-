document.addEventListener('DOMContentLoaded', function() {
    var pendingTbody = document.getElementById('pending-quizzes-tbody');
    var usersTbody = document.getElementById('users-tbody');
    var settingsForm = document.getElementById('settings-form');

    window.loadPendingQuizzes = function() {
        if (!pendingTbody) return;

        fetch(contextPath + '/api/quizzes?filter=pending')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    pendingTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No quizzes pending approval. All caught up! 🎉</td></tr>';
                    document.getElementById('stat-pending-approvals').textContent = '0';
                    return;
                }

                var quizzes = resData.data;
                document.getElementById('stat-pending-approvals').textContent = quizzes.length;
                pendingTbody.innerHTML = '';

                quizzes.forEach(function(q) {
                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>#' + q.id + '</td>' +
                        '<td><strong>' + escapeHtml(q.title) + '</strong></td>' +
                        '<td>' + escapeHtml(q.description || 'No description') + '</td>' +
                        '<td>' + Math.round(q.durationSeconds / 60) + ' min (' + q.durationSeconds + 's)</td>' +
                        '<td>User #' + q.creatorId + '</td>' +
                        '<td>' +
                            '<div style="display: flex; gap: 0.5rem;">' +
                                '<button class="btn btn-success btn-sm" onclick="approveQuiz(' + q.id + ', \'approve\')">✓ Approve</button>' +
                                '<button class="btn btn-danger btn-sm" onclick="approveQuiz(' + q.id + ', \'reject\')">&times; Reject</button>' +
                            '</div>' +
                        '</td>';

                    pendingTbody.appendChild(tr);
                });
            })
            .catch(function(err) {
                pendingTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--danger); padding: 2rem;">Error loading pending quizzes.</td></tr>';
            });
    };

    window.approveQuiz = function(quizId, action) {
        var isApprove = (action === 'approve');
        fetch(contextPath + '/api/quizzes/approve', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                quizId: quizId,
                action: action
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                if (window.showToast) {
                    window.showToast('success', isApprove ? 'Quiz #' + quizId + ' approved successfully!' : 'Quiz #' + quizId + ' rejected.');
                }
                loadPendingQuizzes();
                loadTotalQuizzes();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to update quiz';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error updating quiz status');
        });
    };

    window.loadUsers = function() {
        if (!usersTbody) return;

        fetch(contextPath + '/api/admin/users')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (!resData || !resData.success || !resData.data || resData.data.length === 0) {
                    usersTbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No registered users found.</td></tr>';
                    document.getElementById('stat-total-users').textContent = '0';
                    return;
                }

                var users = resData.data;
                document.getElementById('stat-total-users').textContent = users.length;
                usersTbody.innerHTML = '';

                users.forEach(function(u) {
                    var roleBadge = (u.role === 'ADMIN') ? '<span class="badge badge-admin">Admin</span>' :
                                    (u.role === 'CREATOR') ? '<span class="badge badge-creator">Creator</span>' :
                                    '<span class="badge badge-participant">Participant</span>';

                    var dateStr = u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '--';

                    var tr = document.createElement('tr');
                    tr.innerHTML =
                        '<td>#' + u.id + '</td>' +
                        '<td><strong>' + escapeHtml(u.name) + '</strong></td>' +
                        '<td>' + escapeHtml(u.email) + '</td>' +
                        '<td>' + roleBadge + '</td>' +
                        '<td>' +
                            '<select class="form-select" style="padding: 0.3rem 0.5rem; font-size: 0.8rem; width: auto;" onchange="updateUserRole(' + u.id + ', this.value)">' +
                                '<option value="PARTICIPANT"' + (u.role === 'PARTICIPANT' ? ' selected' : '') + '>Participant</option>' +
                                '<option value="CREATOR"' + (u.role === 'CREATOR' ? ' selected' : '') + '>Creator</option>' +
                                '<option value="ADMIN"' + (u.role === 'ADMIN' ? ' selected' : '') + '>Admin</option>' +
                            '</select>' +
                        '</td>' +
                        '<td style="font-size: 0.85rem; color: var(--text-muted);">' + dateStr + '</td>' +
                        '<td>' +
                            '<button class="btn btn-outline btn-sm" style="color: var(--danger); border-color: rgba(239, 68, 68, 0.4);" onclick="deleteUser(' + u.id + ', \'' + escapeHtml(u.name) + '\')">' +
                                '🗑️ Delete' +
                            '</button>' +
                        '</td>';

                    usersTbody.appendChild(tr);
                });
            })
            .catch(function(err) {
                usersTbody.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--danger); padding: 2rem;">Error loading users.</td></tr>';
            });
    };

    window.updateUserRole = function(userId, newRole) {
        fetch(contextPath + '/api/admin/users/update-role', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                userId: userId,
                role: newRole
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                if (window.showToast) window.showToast('success', 'User role updated to ' + newRole);
                loadUsers();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to update role';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error updating user role');
        });
    };

    window.deleteUser = function(userId, userName) {
        if (!confirm('Are you sure you want to delete user account "' + userName + '" (# ' + userId + ')? This action is permanent.')) {
            return;
        }

        fetch(contextPath + '/api/admin/users/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({
                userId: userId
            })
        })
        .then(function(res) { return res.json(); })
        .then(function(resData) {
            if (resData && resData.success) {
                if (window.showToast) window.showToast('success', 'User #' + userId + ' deleted successfully.');
                loadUsers();
            } else {
                var err = (resData && resData.error) ? resData.error : 'Failed to delete user';
                if (window.showToast) window.showToast('error', err);
            }
        })
        .catch(function(err) {
            if (window.showToast) window.showToast('error', 'Network error deleting user');
        });
    };

    function loadTotalQuizzes() {
        fetch(contextPath + '/api/quizzes')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success && Array.isArray(resData.data)) {
                    document.getElementById('stat-total-quizzes').textContent = resData.data.length;
                }
            })
            .catch(function(err) {});
    }

    function loadSettings() {
        fetch(contextPath + '/api/admin/settings')
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (resData && resData.success && resData.data) {
                    var settings = resData.data;
                    if (settings.max_tab_switches) {
                        document.getElementById('setting-tab-switches').value = settings.max_tab_switches;
                    }
                    if (settings.timeout_buffer_seconds) {
                        document.getElementById('setting-timeout-buffer').value = settings.timeout_buffer_seconds;
                    }
                    if (settings.maintenance_mode) {
                        document.getElementById('setting-maintenance').value = settings.maintenance_mode;
                    }
                }
            })
            .catch(function(err) {});
    }

    if (settingsForm) {
        settingsForm.addEventListener('submit', function(e) {
            e.preventDefault();

            var maxSwitches = document.getElementById('setting-tab-switches').value;
            var buffer = document.getElementById('setting-timeout-buffer').value;
            var maintenance = document.getElementById('setting-maintenance').value;
            var saveBtn = document.getElementById('save-settings-btn');

            if (saveBtn) {
                saveBtn.disabled = true;
                saveBtn.textContent = 'Saving...';
            }

            var payload = {
                max_tab_switches: maxSwitches,
                timeout_buffer_seconds: buffer,
                maintenance_mode: maintenance
            };

            fetch(contextPath + '/api/admin/settings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(payload)
            })
            .then(function(res) { return res.json(); })
            .then(function(resData) {
                if (saveBtn) {
                    saveBtn.disabled = false;
                    saveBtn.textContent = '💾 Save System Settings';
                }
                if (resData && resData.success) {
                    if (window.showToast) window.showToast('success', 'System settings saved successfully!');
                } else {
                    var err = (resData && resData.error) ? resData.error : 'Failed to save settings';
                    if (window.showToast) window.showToast('error', err);
                }
            })
            .catch(function(err) {
                if (saveBtn) {
                    saveBtn.disabled = false;
                    saveBtn.textContent = '💾 Save System Settings';
                }
                if (window.showToast) window.showToast('error', 'Network error saving settings.');
            });
        });
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

    loadPendingQuizzes();
    loadUsers();
    loadTotalQuizzes();
    loadSettings();
});
