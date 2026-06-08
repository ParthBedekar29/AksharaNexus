document.addEventListener('DOMContentLoaded', () => {
    if (api.isLoggedIn()) {
        document.querySelectorAll('.cta-register').forEach(el => {
            el.textContent = 'Go to Oracle';
            el.href = 'chat.html';
        });
        document.querySelectorAll('.cta-login').forEach(el => {
            el.style.display = 'none';
        });
    }
});