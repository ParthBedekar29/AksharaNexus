const API_BASE = 'https://aksharanexus-production.up.railway.app';

const api = {
    getToken: () => localStorage.getItem('akshara_token'),
    getUser:  () => JSON.parse(localStorage.getItem('akshara_user') || 'null'),

    saveSession(data) {
        localStorage.setItem('akshara_token', data.token);
        localStorage.setItem('akshara_user', JSON.stringify({
            firstName: data.firstName,
            email:     data.email,
            role:      data.role
        }));
    },

    clearSession() {
        localStorage.removeItem('akshara_token');
        localStorage.removeItem('akshara_user');
    },

    isLoggedIn: () => !!localStorage.getItem('akshara_token'),

    // ── Auth ─────────────────────────────────────────────────────────────────

    async login(email, password) {
        const res = await fetch(`${API_BASE}/ai/auth/login`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ email, password })
        });
        if (res.status === 403) throw new Error('Please verify your email before signing in.');
        if (res.status === 401) throw new Error('Invalid credentials');
        if (!res.ok)            throw new Error('Login failed. Please try again.');
        return res.json();
    },

    async register(firstName, lastName, email, password) {
        const res = await fetch(`${API_BASE}/ai/auth/register`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ firstName, lastName, email, password })
        });
        if (res.status === 409) throw new Error('An account with this email already exists.');
        if (!res.ok)            throw new Error('Registration failed. Please try again.');
        // Backend returns plain text on 201 — don't parse as JSON
        return true;
    },

    async verifyEmail(token) {
        const res = await fetch(`${API_BASE}/ai/auth/verify?token=${encodeURIComponent(token)}`);
        if (!res.ok) {
            const msg = await res.text();
            throw new Error(msg || 'Verification failed.');
        }
        return true;
    },

    async forgotPassword(email) {
        const res = await fetch(`${API_BASE}/ai/auth/forgot-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ email })
        });
        if (!res.ok) throw new Error('Request failed. Please try again.');
        return true;
    },

    async resetPassword(token, newPassword) {
        const res = await fetch(`${API_BASE}/ai/auth/reset-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ token, newPassword })
        });
        if (res.status === 400) {
            const msg = await res.text();
            throw new Error(msg || 'Reset link is invalid or expired.');
        }
        if (!res.ok) throw new Error('Password reset failed. Please try again.');
        return true;
    },
    async getProfile() {
        const res = await fetch(`${API_BASE}/ai/account/me`, {
            headers: { 'Authorization': `Bearer ${this.getToken()}` }
        });
        if (res.status === 401 || res.status === 403) {
            this.clearSession(); window.location.href = 'login.html';
            throw new Error('Session expired');
        }
        if (!res.ok) throw new Error('Failed to load profile.');
        return res.json();
    },

    async changePassword(currentPassword, newPassword) {
        const res = await fetch(`${API_BASE}/ai/account/change-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.getToken()}` },
            body:    JSON.stringify({ currentPassword, newPassword })
        });
        if (res.status === 400) { const msg = await res.text(); throw new Error(msg); }
        if (res.status === 401 || res.status === 403) {
            this.clearSession(); window.location.href = 'login.html';
            throw new Error('Session expired');
        }
        if (!res.ok) throw new Error('Failed to update password.');
        return true;
    },
    async resendVerification(email) {
        const res = await fetch(`${API_BASE}/ai/auth/resend-verification`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ email })
        });
        if (!res.ok) throw new Error('Failed to resend. Please try again.');
        return true;
    },
    async deleteAccount(password) {
        const res = await fetch(`${API_BASE}/ai/account/delete`, {
            method:  'DELETE',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.getToken()}` },
            body:    JSON.stringify({ password })
        });
        if (res.status === 400) { const msg = await res.text(); throw new Error(msg); }
        if (res.status === 401 || res.status === 403) {
            this.clearSession(); window.location.href = 'login.html';
            throw new Error('Session expired');
        }
        if (!res.ok) throw new Error('Failed to delete account.');
        return true;
    },
    // ── Oracle ────────────────────────────────────────────────────────────────

    async query(question) {
        const res = await fetch(`${API_BASE}/oracle/query`, {
            method:  'POST',
            headers: {
                'Content-Type':  'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            },
            body: JSON.stringify({ query: question })
        });
        if (res.status === 401 || res.status === 403) {
            this.clearSession();
            window.location.href = 'login.html';
            throw new Error('Session expired');
        }
        if (!res.ok) throw new Error('Query failed');
        return res.json();
    }
};