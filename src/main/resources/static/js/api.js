// ── NexusA API Layer ──────────────────────────────────────────────────────────
const API_BASE = 'http://localhost:8080';
const TOKEN_KEY = 'nexusa_token';

// ── Token Helpers ─────────────────────────────────────────────────────────────
const Auth = {
    getToken()  { return localStorage.getItem(TOKEN_KEY); },
    setToken(t) { localStorage.setItem(TOKEN_KEY, t); },
    removeToken(){ localStorage.removeItem(TOKEN_KEY); },

    getPayload() {
        const token = this.getToken();
        if (!token) return null;
        try { return JSON.parse(atob(token.split('.')[1])); }
        catch { return null; }
    },

    getRole()   { return this.getPayload()?.role || null; },
    isAdmin()   { return this.getRole() === 'ADMIN'; },
    isEditor()  { return this.getRole() === 'EDITOR'; },
    isViewer()  { return this.getRole() === 'VIEWER'; },

    getUserId() {
        const p = this.getPayload();
        return p?.sub || p?.userId || p?.id || null;
    },

    requireAuth(redirectTo = 'login.html') {
        if (!this.getToken()) { window.location.href = redirectTo; return false; }
        return true;
    },

    logout() { this.removeToken(); window.location.href = 'login.html'; }
};

// ── Retry / Timeout config ────────────────────────────────────────────────────
const FETCH_TIMEOUT_MS  = 12000;   // 12 s per attempt
const RETRY_ATTEMPTS    = 3;
const RETRY_BASE_DELAY  = 800;     // ms — doubles each retry (800 → 1600 → 3200)
const SAVE_QUEUE_KEY    = 'nexusa_save_queue';

// ── Core Fetch (with timeout + retry) ─────────────────────────────────────────
async function apiFetch(path, options = {}, attempt = 1) {
    const token = Auth.getToken();
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    // Attach AbortController for timeout
    const controller = new AbortController();
    const timeoutId  = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);

    let res, text, data;
    try {
        res  = await fetch(`${API_BASE}${path}`, {
            ...options,
            headers,
            signal: controller.signal,
            body: options.body ? JSON.stringify(options.body) : undefined
        });
        clearTimeout(timeoutId);
    } catch (err) {
        clearTimeout(timeoutId);

        const isAbort   = err.name === 'AbortError';
        const isNetwork = err instanceof TypeError; // "Failed to fetch"

        if ((isAbort || isNetwork) && attempt < RETRY_ATTEMPTS) {
            const delay = RETRY_BASE_DELAY * Math.pow(2, attempt - 1);
            await new Promise(r => setTimeout(r, delay));
            return apiFetch(path, options, attempt + 1);
        }

        // Out of retries — surface a clear error
        const msg = isAbort
            ? `Request timed out after ${FETCH_TIMEOUT_MS / 1000}s (attempt ${attempt}/${RETRY_ATTEMPTS})`
            : `Cannot reach server — check that the backend is running on ${API_BASE}`;
        throw new NetworkError(msg, { path, attempt });
    }

    text = await res.text();
    try { data = JSON.parse(text); } catch { data = text; }

    if (!res.ok) {
        // 401 → token expired
        if (res.status === 401) {
            Auth.removeToken();
            window.location.href = 'login.html';
            return;
        }
        const msg = typeof data === 'string'
            ? data
            : (data?.message || `Request failed (${res.status})`);
        throw new ApiError(msg, res.status, { path });
    }

    return data;
}

// ── Custom error types ────────────────────────────────────────────────────────
class NetworkError extends Error {
    constructor(msg, meta = {}) {
        super(msg); this.name = 'NetworkError'; this.meta = meta;
    }
}
class ApiError extends Error {
    constructor(msg, status, meta = {}) {
        super(msg); this.name = 'ApiError'; this.status = status; this.meta = meta;
    }
}

// ── Offline Save Queue ────────────────────────────────────────────────────────
// When a save fails due to network, we enqueue it and replay on reconnect.
const SaveQueue = {
    _q: [],

    load() {
        try { this._q = JSON.parse(localStorage.getItem(SAVE_QUEUE_KEY) || '[]'); }
        catch { this._q = []; }
    },

    persist() {
        try { localStorage.setItem(SAVE_QUEUE_KEY, JSON.stringify(this._q)); }
        catch { /* storage full — ignore */ }
    },

    enqueue(civId, nodeId, payload) {
        // Replace any existing pending save for the same node
        this._q = this._q.filter(e => !(e.civId === civId && e.nodeId === nodeId));
        this._q.push({ civId, nodeId, payload, queuedAt: Date.now() });
        this.persist();
    },

    dequeue(civId, nodeId) {
        this._q = this._q.filter(e => !(e.civId === civId && e.nodeId === nodeId));
        this.persist();
    },

    hasPending(civId, nodeId) {
        return this._q.some(e => e.civId === civId && e.nodeId === nodeId);
    },

    size() { return this._q.length; },

    all() { return [...this._q]; }
};

// Replay queue when connection returns
async function flushSaveQueue() {
    SaveQueue.load();
    const pending = SaveQueue.all();
    if (!pending.length) return;

    for (const item of pending) {
        try {
            await apiFetch(
                `/civilization/${item.civId}/node/${item.nodeId}`,
                { method: 'PUT', body: item.payload }
            );
            SaveQueue.dequeue(item.civId, item.nodeId);
            showToast('Offline save synced ✓', 'success');
        } catch {
            break; // still offline — stop trying
        }
    }
}

// Listen for reconnection
window.addEventListener('online',  () => {
    showToast('Back online — syncing…', 'info');
    flushSaveQueue();
});
window.addEventListener('offline', () => showToast('You\'re offline — saves will queue', 'info'));

// ── Auth API ──────────────────────────────────────────────────────────────────
const AuthAPI = {
    async login(email, password) {
        const token = await apiFetch('/auth/login', { method: 'POST', body: { email, password } });
        Auth.setToken(token);
        return token;
    },
    async register(payload) {
        const token = await apiFetch('/auth/register', { method: 'POST', body: payload });
        Auth.setToken(token);
        return token;
    },
    async getUniversities() { return apiFetch('/auth/universities'); },
    async forgotPassword(email, adminCode = null) {
        return apiFetch('/auth/forgot-password', {
            method: 'POST',
            body: { email, adminCode }
        });
    },

    async resetPassword(token, newPassword) {
        return apiFetch('/auth/reset-password', {
            method: 'POST',
            body: { token, newPassword }
        });
    }
};

// ── Civilization API ──────────────────────────────────────────────────────────
const CivAPI = {
    async create(data)           { return apiFetch('/civilization/create', { method: 'POST', body: data }); },
    async getMy()                { return apiFetch('/civilization/my'); },
    async getAll()               { return apiFetch('/civilization/all'); },
    async getLatest(civId)       { return apiFetch(`/civilization/${civId}/latest`); },
    async getVersions(civId)     { return apiFetch(`/civilization/${civId}/versions`); },
    async addVolume(civId, data) { return apiFetch(`/civilization/${civId}/volume`, { method: 'POST', body: data }); },
    async addEntry(civId, data)  { return apiFetch(`/civilization/${civId}/entry`,  { method: 'POST', body: data }); },
    async delete(civId) { return apiFetch(`/civilization/${civId}`, { method: 'DELETE' }); },
    async updateNode(civId, nodeId, data) {
        try {
            const result = await apiFetch(
                `/civilization/${civId}/node/${nodeId}`,
                { method: 'PUT', body: data }
            );
            // Success — remove from queue if it was there
            SaveQueue.dequeue(civId, nodeId);
            return result;
        } catch (err) {
            if (err instanceof NetworkError || !navigator.onLine) {
                // Queue for later
                SaveQueue.enqueue(civId, nodeId, data);
                throw new NetworkError('Queued — will save when connection returns');
            }
            throw err;
        }
    },

    async rollback(civId, hash)         { return apiFetch(`/civilization/${civId}/rollback`, { method: 'POST', body: { hash } }); },
    async assignEditor(civId, userId)   { return apiFetch(`/civilization/${civId}/editors`,  { method: 'POST', body: { userId } }); },
    async getEditors(civId)             { return apiFetch(`/civilization/${civId}/editors`); },
    async getUniversityUsers()          { return apiFetch('/civilization/users'); },
    async submitForReview(civId, versionId) {
        return apiFetch(`/civilization/${civId}/version/${versionId}/submit`, { method: 'PATCH' });
    },
    // Add inside CivAPI object:
    async getEntryMarks(civId) { return apiFetch(`/civilization/${civId}/entry-marks`); },
};

// ── Toast System ──────────────────────────────────────────────────────────────
function initToasts() {
    if (!document.getElementById('toast-container')) {
        const el = document.createElement('div');
        el.id = 'toast-container';
        el.className = 'toast-container';
        document.body.appendChild(el);
    }
}

function showToast(message, type = 'info', duration = 3500) {
    initToasts();
    const container = document.getElementById('toast-container');
    const icons = { success: '✓', error: '✕', info: 'ℹ' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span><span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity    = '0';
        toast.style.transform  = 'translateX(20px)';
        toast.style.transition = '0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ── Modal Helpers ─────────────────────────────────────────────────────────────
function openModal(id)  { document.getElementById(id)?.classList.add('active'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('active'); }

document.addEventListener('click', e => {
    if (e.target.classList.contains('modal-overlay')) e.target.classList.remove('active');
});

// ── University Dropdown ───────────────────────────────────────────────────────
async function loadUniversities(selectId, selectedId = null) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="">Loading universities…</option>';
    try {
        const unis = await AuthAPI.getUniversities();
        select.innerHTML = '<option value="">Select your university</option>';
        unis.forEach(u => {
            const opt = document.createElement('option');
            opt.value = u.id; opt.textContent = u.name;
            if (selectedId && u.id === selectedId) opt.selected = true;
            select.appendChild(opt);
        });
    } catch {
        select.innerHTML = '<option value="">Failed to load universities</option>';
        showToast('Could not load universities', 'error');
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────
function roleBadge(role) {
    const cls = { ADMIN: 'badge-admin', EDITOR: 'badge-editor', VIEWER: 'badge-viewer' };
    return `<span class="badge ${cls[role] || 'badge-viewer'}">${role}</span>`;
}

function formatYear(year) {
    if (year == null) return '—';
    return year < 0 ? `${Math.abs(year)} BCE` : `${year} CE`;
}



function shortHash(hash) { return hash ? hash.substring(0, 8) : ''; }

// ── Init ──────────────────────────────────────────────────────────────────────
SaveQueue.load();