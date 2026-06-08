// ── AksharaNexus Reviewer API ─────────────────────────────────────────────
const BASE = 'http://localhost:8080';
const TOKEN_KEY = 'reviewer_token';

// ── Token helpers ─────────────────────────────────────────────────────────
function getToken()   { return localStorage.getItem(TOKEN_KEY); }
function setToken(t)  { localStorage.setItem(TOKEN_KEY, t); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

function getPayload() {
    const t = getToken();
    if (!t) return null;
    try {
        const b64 = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(b64));
    } catch { return null; }
}

export function isLoggedIn()       { return !!getToken(); }
export function getReviewerEmail() { return getPayload()?.sub || ''; }

// ── Core fetch ────────────────────────────────────────────────────────────
async function req(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const opts = { method, headers };
    if (body !== undefined) opts.body = JSON.stringify(body);

    const res = await fetch(BASE + path, opts);

    if (res.status === 401) {
        clearToken();
        window.location.href = 'login.html';
        return;
    }

    const text = await res.text();
    let data;
    try { data = JSON.parse(text); } catch { data = text; }

    if (!res.ok) {
        throw new Error(typeof data === 'string' ? data : (data?.message || JSON.stringify(data)));
    }
    return data;
}

// ── Auth ──────────────────────────────────────────────────────────────────
export async function register(firstName, lastName, email, password, code) {
    return req('POST', '/reviewer/auth/register', {
        firstName, lastName, email, password, code
    });
}

export async function login(email, password) {
    const data = await req('POST', '/reviewer/auth/login', { email, password });
    const token = typeof data === 'string' ? data : (data.token || data.accessToken || data);
    setToken(token);
    return token;
}

export function logout() {
    clearToken();
    window.location.href = 'login.html';
}

// ── Civilization browse (latest version per civ, all universities) ────────
// Replaces old getPendingVersions / getReviewedVersions
export const getAllLatestVersions = () => req('GET', '/reviewer/civilizations/latest');
export const getVersionDetail     = (id) => req('GET', `/reviewer/version/${id}`);

// ── Entry marking ─────────────────────────────────────────────────────────
// markStatus: 'APPROVED' | 'REJECTED' | 'REVISION_REQUESTED'
export const markEntry         = (dto) => req('POST', '/reviewer/entry/mark', dto);
export const getMyMarks        = ()    => req('GET',  '/reviewer/entry/marks');
// ── Central civilizations ─────────────────────────────────────────────────
export const getCentralCivilizations   = ()               => req('GET',  '/reviewer/central/civilizations');
export const getCentralDetail          = (id)             => req('GET',  `/reviewer/central/${id}`);
export const createCentralCivilization = (dto)            => req('POST', '/reviewer/central/civilization', dto);
export const addVolume                 = (cId, dto)       => req('POST', `/reviewer/central/${cId}/volume`, dto);
export const addEntry                  = (cId, vId, dto)  => req('POST', `/reviewer/central/${cId}/volume/${vId}/entry`, dto);
export const flagDivergence            = (cId, dto)       => req('POST', `/reviewer/central/${cId}/divergence`, dto);
export const getCentralCivilizationsForReviewer = ()          => req('GET',  '/reviewer/central/civilizations/my-registry'); // <--- ADD THIS ENDPOINT
// Add this method alongside your existing entry marking exports in api.js
// Add this inside your './api.js' file
export const markEntryForCentral = (dto) => req('POST', '/reviewer/entry/mark-for-central', dto);
// ── Wikidata ──────────────────────────────────────────────────────────────
export const getCivMetadata = (title) => req('GET', `/reviewer/central/civ-metadata?title=${encodeURIComponent(title)}`);
export const deleteEntry  = (cId, vId, eId) => req('DELETE', `/reviewer/central/${cId}/volume/${vId}/entry/${eId}`);
export const deleteVolume = (cId, vId)       => req('DELETE', `/reviewer/central/${cId}/volume/${vId}`);
export const addEntriesBatch = (cId, vId, dto) => req('POST', `/reviewer/central/${cId}/volume/${vId}/entries/batch`, dto);
export const deleteMark = (markId) => req('DELETE', `/reviewer/entry/mark/${markId}`);
export const getVersionMarks = (versionId) => req('GET', `/reviewer/version/${versionId}/marks`);