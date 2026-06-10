/**
 * auth.js — handles login, register, forgot-password, reset-password, verify-email
 * Reads data-page from <body> to know which flow to run.
 */

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;

    // Password toggle — works on any auth page
    document.querySelectorAll('.toggle-pw').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;
            const visible = input.type === 'text';
            input.type   = visible ? 'password' : 'text';
            btn.textContent = visible ? '👁' : '🙈';
        });
    });

    switch (page) {
        case 'login':          initLogin();          break;
        case 'register':       initRegister();       break;
        case 'forgot-password':initForgotPassword(); break;
        case 'reset-password': initResetPassword();  break;
        case 'verify-email':   initVerifyEmail();    break;
    }
});

// ── Helpers ──────────────────────────────────────────────────────────────────

function showError(msg) {
    const el = document.getElementById('auth-error');
    if (!el) return;
    el.textContent = msg;
    el.classList.add('visible');
}

function hideError() {
    const el = document.getElementById('auth-error');
    if (el) el.classList.remove('visible');
}

function setLoading(btn, loading) {
    btn.disabled = loading;
    btn.innerHTML = loading
        ? '<span class="spinner"></span> Please wait…'
        : btn.dataset.label;
}

function show(id)   { const el = document.getElementById(id); if (el) el.style.display = ''; }
function hide(id)   { const el = document.getElementById(id); if (el) el.style.display = 'none'; }

// ── Login ─────────────────────────────────────────────────────────────────────

function initLogin() {
    if (api.isLoggedIn()) { window.location.href = 'chat.html'; return; }

    const form      = document.getElementById('auth-form');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();
        setLoading(submitBtn, true);

        const email    = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;

        try {
            const data = await api.login(email, password);
            api.saveSession(data);
            window.location.href = 'chat.html';
        } catch (err) {
            showError(err.message);
            setLoading(submitBtn, false);
        }
    });
}

// ── Register ──────────────────────────────────────────────────────────────────

function initRegister() {
    if (api.isLoggedIn()) { window.location.href = 'chat.html'; return; }

    const form      = document.getElementById('auth-form');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();

        const firstName = document.getElementById('firstName').value.trim();
        const lastName  = document.getElementById('lastName').value.trim();
        const email     = document.getElementById('email').value.trim();
        const password  = document.getElementById('password').value;

        if (password.length < 6) {
            showError('Password must be at least 6 characters.');
            return;
        }

        setLoading(submitBtn, true);

        try {
            await api.register(firstName, lastName, email, password);
            // Show email-sent state
            document.getElementById('success-email-msg').innerHTML =
                `We sent a verification link to <strong>${email}</strong>.<br>Click it to activate your account.`;
            hide('form-card');
            show('success-card');
        } catch (err) {
            showError(err.message);
            setLoading(submitBtn, false);
        }
    });
}

// ── Forgot password ───────────────────────────────────────────────────────────

function initForgotPassword() {
    const form      = document.getElementById('auth-form');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();

        const email = document.getElementById('email').value.trim();
        if (!email) { showError('Email is required.'); return; }

        setLoading(submitBtn, true);

        try {
            await api.forgotPassword(email);
            // Always show success (backend doesn't reveal whether email exists)
            document.getElementById('success-email-msg').innerHTML =
                `If <strong>${email}</strong> is registered, a reset link is on its way.`;
            hide('form-card');
            show('success-card');
        } catch (err) {
            showError(err.message);
            setLoading(submitBtn, false);
        }
    });
}

// ── Reset password ────────────────────────────────────────────────────────────

function initResetPassword() {
    const params = new URLSearchParams(window.location.search);
    const token  = params.get('token');

    if (!token) {
        show('invalid-card');
        return;
    }

    show('form-card');

    const form      = document.getElementById('auth-form');
    const submitBtn = document.getElementById('submit-btn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();

        const password = document.getElementById('password').value;
        const confirm  = document.getElementById('confirm').value;

        if (password.length < 6) {
            showError('Password must be at least 6 characters.');
            return;
        }
        if (password !== confirm) {
            showError('Passwords do not match.');
            return;
        }

        setLoading(submitBtn, true);

        try {
            await api.resetPassword(token, password);
            hide('form-card');
            show('success-card');
        } catch (err) {
            // Token expired / already used — show the dedicated card
            if (err.message.toLowerCase().includes('invalid') || err.message.toLowerCase().includes('expired')) {
                hide('form-card');
                show('invalid-card');
            } else {
                showError(err.message);
                setLoading(submitBtn, false);
            }
        }
    });
}

// ── Verify email ──────────────────────────────────────────────────────────────

async function initVerifyEmail() {
    const params = new URLSearchParams(window.location.search);
    const token  = params.get('token');

    if (!token) {
        hide('loading-card');
        document.getElementById('error-msg').textContent = 'No verification token found in the link.';
        show('error-card');
        return;
    }

    try {
        await api.verifyEmail(token);
        hide('loading-card');
        show('success-card');
    } catch (err) {
        hide('loading-card');
        document.getElementById('error-msg').textContent = err.message || 'This link is invalid or has already been used.';
        show('error-card');
    }
}