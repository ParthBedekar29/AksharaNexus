document.addEventListener('DOMContentLoaded', () => {
    if (api.isLoggedIn()) { window.location.href = 'chat.html'; return; }

    const form = document.getElementById('auth-form');
    const errorEl = document.getElementById('auth-error');
    const submitBtn = document.getElementById('submit-btn');
    const isRegister = document.body.dataset.page === 'register';

    // Password toggle
    document.querySelectorAll('.toggle-pw').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = btn.previousElementSibling;
            const isText = input.type === 'text';
            input.type = isText ? 'password' : 'text';
            btn.textContent = isText ? '👁' : '🙈';
        });
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorEl.classList.remove('visible');
        setLoading(true);

        try {
            let data;
            if (isRegister) {
                const firstName = document.getElementById('firstName').value.trim();
                const lastName = document.getElementById('lastName').value.trim();
                const email = document.getElementById('email').value.trim();
                const password = document.getElementById('password').value;
                if (password.length < 6) throw new Error('Password must be at least 6 characters');
                data = await api.register(firstName, lastName, email, password);
            } else {
                const email = document.getElementById('email').value.trim();
                const password = document.getElementById('password').value;
                data = await api.login(email, password);
            }
            api.saveSession(data);
            window.location.href = 'chat.html';
        } catch (err) {
            showError(err.message);
        } finally {
            setLoading(false);
        }
    });

    function showError(msg) {
        errorEl.textContent = msg;
        errorEl.classList.add('visible');
    }

    function setLoading(loading) {
        submitBtn.disabled = loading;
        submitBtn.innerHTML = loading
            ? '<span class="spinner"></span> Please wait...'
            : submitBtn.dataset.label;
    }
});