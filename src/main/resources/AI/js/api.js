const API_BASE = 'https://aksharanexus-production.up.railway.app';

const api = {
    getToken: () => localStorage.getItem('akshara_token'),
    getUser: () => JSON.parse(localStorage.getItem('akshara_user') || 'null'),

    saveSession(data) {
        localStorage.setItem('akshara_token', data.token);
        localStorage.setItem('akshara_user', JSON.stringify({
            firstName: data.firstName,
            email: data.email,
            role: data.role
        }));
    },

    clearSession() {
        localStorage.removeItem('akshara_token');
        localStorage.removeItem('akshara_user');
    },

    isLoggedIn: () => !!localStorage.getItem('akshara_token'),

    async login(email, password) {
        const res = await fetch(`${API_BASE}/ai/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        if (!res.ok) throw new Error(res.status === 401 ? 'Invalid credentials' : 'Login failed');
        return res.json();
    },

    async register(firstName, lastName, email, password) {
        const res = await fetch(`${API_BASE}/ai/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ firstName, lastName, email, password })
        });
        if (res.status === 409) throw new Error('Email already registered');
        if (!res.ok) throw new Error('Registration failed');
        return res.json();
    },

    async query(question) {
        const res = await fetch(`${API_BASE}/oracle/query`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.getToken()}`
            },
            body: JSON.stringify({ query: question })
        });
        if (res.status === 401 || res.status === 403) {  // ← add 403
            this.clearSession();
            window.location.href = 'login.html';
            throw new Error('Session expired');
        }
        if (!res.ok) throw new Error('Query failed');
        return res.json();
    }
};
