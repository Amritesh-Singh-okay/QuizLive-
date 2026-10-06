document.addEventListener('DOMContentLoaded', function() {
    var loginForm = document.getElementById('login-form');
    var registerForm = document.getElementById('register-form');
    var errorBanner = document.getElementById('auth-error-banner');

    function showError(msg) {
        if (errorBanner) {
            errorBanner.textContent = msg;
            errorBanner.style.display = 'block';
        }
        if (window.showToast) {
            window.showToast('error', msg);
        }
    }

    function clearError() {
        if (errorBanner) {
            errorBanner.style.display = 'none';
            errorBanner.textContent = '';
        }
    }

    // Quick demo login fill
    window.fillCredentials = function(email, password) {
        var emailInput = document.getElementById('email');
        var passInput = document.getElementById('password');
        if (emailInput && passInput) {
            emailInput.value = email;
            passInput.value = password;
            clearError();
            if (window.showToast) {
                window.showToast('info', 'Filled demo credentials for ' + email);
            }
        }
    };

    // Handle Login
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            clearError();

            var email = document.getElementById('email').value.trim();
            var password = document.getElementById('password').value;
            var submitBtn = document.getElementById('login-submit-btn');

            if (!email || !password) {
                showError('Please provide both email and password.');
                return;
            }

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Signing In...';
            }

            var contextPath = loginForm.getAttribute('data-context-path') || '';
            var apiUrl = contextPath + '/api/auth/login';

            fetch(apiUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ email: email, password: password })
            })
            .then(function(res) {
                return res.json().then(function(data) {
                    return { status: res.status, ok: res.ok, data: data };
                });
            })
            .then(function(result) {
                if (result.ok && result.data && result.data.success) {
                    var userData = result.data.data;
                    if (window.showToast) {
                        window.showToast('success', 'Login successful! Redirecting...');
                    }
                    var targetUrl = userData.redirectUrl || (contextPath + '/index.jsp');
                    setTimeout(function() {
                        window.location.href = targetUrl;
                    }, 600);
                } else {
                    var errorMsg = (result.data && result.data.error) ? result.data.error : 'Invalid credentials';
                    showError(errorMsg);
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = 'Sign In';
                    }
                }
            })
            .catch(function(err) {
                // Fallback to form submit if JSON fetch fails
                showError('API request failed. Falling back to direct login...');
                loginForm.submit();
            });
        });
    }

    // Handle Register
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            clearError();

            var name = document.getElementById('name').value.trim();
            var email = document.getElementById('email').value.trim();
            var password = document.getElementById('password').value;
            var roleSelect = document.getElementById('role');
            var role = roleSelect ? roleSelect.value : 'PARTICIPANT';
            var submitBtn = document.getElementById('register-submit-btn');

            if (!name || !email || !password) {
                showError('Please fill in all required fields.');
                return;
            }

            if (password.length < 6) {
                showError('Password must be at least 6 characters long.');
                return;
            }

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Creating Account...';
            }

            var contextPath = registerForm.getAttribute('data-context-path') || '';
            var apiUrl = contextPath + '/api/auth/register';

            fetch(apiUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ name: name, email: email, password: password, role: role })
            })
            .then(function(res) {
                return res.json().then(function(data) {
                    return { status: res.status, ok: res.ok, data: data };
                });
            })
            .then(function(result) {
                if (result.ok && result.data && result.data.success) {
                    var userData = result.data.data;
                    if (window.showToast) {
                        window.showToast('success', 'Account created! Redirecting to dashboard...');
                    }
                    var targetUrl = userData.redirectUrl || (contextPath + '/index.jsp');
                    setTimeout(function() {
                        window.location.href = targetUrl;
                    }, 700);
                } else {
                    var errorMsg = (result.data && result.data.error) ? result.data.error : 'Registration failed';
                    showError(errorMsg);
                    if (submitBtn) {
                        submitBtn.disabled = false;
                        submitBtn.textContent = 'Create Account';
                    }
                }
            })
            .catch(function(err) {
                showError('API request failed. Falling back to direct register...');
                registerForm.submit();
            });
        });
    }
});
