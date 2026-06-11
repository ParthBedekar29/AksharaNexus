document.addEventListener('DOMContentLoaded', async () => {
    if (!api.isLoggedIn()) { window.location.href = 'login.html'; return; }

    // ── Password toggles ──────────────────────────────────────────────────────
    document.querySelectorAll('.toggle-pw').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;
            const visible = input.type === 'text';
            input.type = visible ? 'password' : 'text';
            btn.textContent = visible ? '👁' : '🙈';
        });
    });

    // ── Load profile ──────────────────────────────────────────────────────────
    try {
        const profile = await api.getProfile();
        document.getElementById('profile-name').textContent =
            [profile.firstName, profile.lastName].filter(Boolean).join(' ');
        document.getElementById('profile-email').textContent  = profile.email;
        document.getElementById('profile-since').textContent  = profile.memberSince;
        document.getElementById('avatar-initials').textContent =
            (profile.firstName?.[0] ?? '') + (profile.lastName?.[0] ?? '');
    } catch (e) {
        // silently fall through — session guard above handles 401
    }

    // ── Change password ───────────────────────────────────────────────────────
    const changePwBtn = document.getElementById('change-pw-btn');
    changePwBtn.addEventListener('click', async () => {
        clearMsgs();
        const current = document.getElementById('current-pw').value;
        const next    = document.getElementById('new-pw').value;
        const confirm = document.getElementById('confirm-pw').value;

        if (!current || !next || !confirm) { showPwError('All fields are required.'); return; }
        if (next.length < 6)               { showPwError('New password must be at least 6 characters.'); return; }
        if (next !== confirm)              { showPwError('Passwords do not match.'); return; }

        setLoading(changePwBtn, true);
        try {
            await api.changePassword(current, next);
            document.getElementById('current-pw').value = '';
            document.getElementById('new-pw').value     = '';
            document.getElementById('confirm-pw').value = '';
            showPwSuccess('Password updated successfully.');
        } catch (err) {
            showPwError(err.message);
        } finally {
            setLoading(changePwBtn, false);
        }
    });

    // ── Delete modal ──────────────────────────────────────────────────────────
    document.getElementById('open-delete-modal').addEventListener('click', () => {
        document.getElementById('delete-pw').value = '';
        clearField('del-error');
        document.getElementById('delete-modal').style.display = 'flex';
    });
    document.getElementById('cancel-delete').addEventListener('click', () => {
        document.getElementById('delete-modal').style.display = 'none';
    });

    const confirmDeleteBtn = document.getElementById('confirm-delete');
    confirmDeleteBtn.addEventListener('click', async () => {
        const pw = document.getElementById('delete-pw').value;
        if (!pw) { showDelError('Please enter your password.'); return; }

        setLoading(confirmDeleteBtn, true);
        try {
            await api.deleteAccount(pw);
            api.clearSession();
            window.location.href = 'index.html';
        } catch (err) {
            showDelError(err.message);
            setLoading(confirmDeleteBtn, false);
        }
    });

    // ── Helpers ───────────────────────────────────────────────────────────────
    function showPwError(msg)   { const el = document.getElementById('pw-error');   el.textContent = msg; el.classList.add('visible'); }
    function showPwSuccess(msg) { const el = document.getElementById('pw-success'); el.textContent = msg; el.classList.add('visible'); }
    function showDelError(msg)  { const el = document.getElementById('del-error');  el.textContent = msg; el.classList.add('visible'); }
    function clearMsgs() {
        ['pw-error','pw-success'].forEach(id => { const el = document.getElementById(id); el.textContent = ''; el.classList.remove('visible'); });
    }
    function clearField(id)     { const el = document.getElementById(id); el.textContent = ''; el.classList.remove('visible'); }
    function setLoading(btn, on) {
        btn.disabled   = on;
        btn.innerHTML  = on ? '<span class="spinner"></span> Please wait…' : btn.dataset.label;
    }
});