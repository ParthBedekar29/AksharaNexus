import {
    isLoggedIn, logout,
    getAllLatestVersions,
    getMyMarks, markEntry,
    getCentralCivilizations, getCentralDetail,
    createCentralCivilization, addVolume, addEntry, addEntriesBatch,
    deleteEntry, deleteVolume,
    flagDivergence, getCivMetadata, deleteMark,
    addVolumeFromMark, getMyVolumeMarks
} from './api.js';

let isBatchMode = false;

// ── Auth Guard ────────────────────────────────────────────────────────────
if (!isLoggedIn()) { window.location.href = 'login.html'; }

// ── State Management ──────────────────────────────────────────────────────
let currentView                  = 'civilizations';
let currentCivId                 = null;
let currentCivTitle              = '';
let currentVolId                 = null;
let approvedMarks                = [];
let selectedEntriesForDivergence = [];
let selectedCivMetadata          = null;
let selectedMarkIds              = new Set();

// ── Volume modal state ────────────────────────────────────────────────────
let activeVolTab         = 'marks';   // 'marks' | 'manual'
let selectedVolumeMarkId = null;

// ── JWT Payload Extraction ────────────────────────────────────────────────
function decodeJwt(token) {
    try {
        return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    } catch { return {}; }
}
const token   = localStorage.getItem('reviewer_token');
const payload = decodeJwt(token || '');
const revEmail = payload.sub || '';

document.getElementById('nav-reviewer-name').textContent = revEmail;
const initial = revEmail.charAt(0).toUpperCase();
document.getElementById('sidebar-avatar').textContent = initial;
document.getElementById('sidebar-name').textContent   = revEmail;

// ── Toast Notifications ───────────────────────────────────────────────────
function toast(msg, type = 'info') {
    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    document.getElementById('toasts').appendChild(el);
    setTimeout(() => el.remove(), 3400);
}

// ── View Routing ──────────────────────────────────────────────────────────
function showView(name) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById('view-' + name).classList.add('active');
    document.querySelectorAll('.sidebar-link').forEach(l => {
        l.classList.toggle('active', l.dataset.view === name);
    });
    document.getElementById('panel-central').classList.remove('open');
    currentView = name;
}

document.querySelectorAll('[data-view]').forEach(btn => {
    btn.addEventListener('click', () => {
        showView(btn.dataset.view);
        if (btn.dataset.view === 'civilizations') loadCivilizations();
        if (btn.dataset.view === 'marks')         loadMarks();
        if (btn.dataset.view === 'central')        loadCentral();
    });
});

// ── Sign Out ──────────────────────────────────────────────────────────────
document.getElementById('btn-signout').addEventListener('click', logout);

// ── Modal Handlers ────────────────────────────────────────────────────────
function openModal(id)  { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', () => closeModal(btn.dataset.close));
});
document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
        if (e.target === overlay) overlay.classList.remove('open');
    });
});

// ── Formatting Utilities ──────────────────────────────────────────────────
function formatYear(y) {
    if (y == null) return '?';
    return y < 0 ? `${Math.abs(y)} BCE` : `${y} CE`;
}
function fmtDate(ts) {
    if (!ts) return '—';
    return new Date(ts).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}
function statusBadge(s) {
    const standardStatus = s || 'PENDING';
    return `<span class="status-badge status-${standardStatus}">${standardStatus.replace(/_/g, ' ')}</span>`;
}
function skeleton(n = 3) {
    return `<div class="skeleton-list">${'<div class="skeleton-row"></div>'.repeat(n)}</div>`;
}
function emptyState(icon, title, sub) {
    return `<div class="empty-state"><div class="empty-icon">${icon}</div><h3>${title}</h3><p>${sub}</p></div>`;
}
function escapeAttr(s) { return (s || '').replace(/"/g, '&quot;'); }
function numOrNull(id) {
    const v = document.getElementById(id)?.value.trim();
    return v === '' || v == null ? null : parseInt(v);
}

// ── Ingress Funnel: Incoming University Branches ──────────────────────────
async function loadCivilizations() {
    const el = document.getElementById('civs-list');
    el.innerHTML = skeleton();
    try {
        const versions = await getAllLatestVersions();
        document.getElementById('stat-civs').textContent = versions.length;
        document.getElementById('pill-civs').textContent = versions.length;
        if (!versions.length) {
            el.innerHTML = emptyState('◉', 'No incoming branches', 'No universities have submitted timeline proposals yet.');
            return;
        }
        el.innerHTML = versions.map(v => civRow(v)).join('');
        el.querySelectorAll('.version-row').forEach(row => {
            row.addEventListener('click', () =>
                openVersionDetail(row.dataset.id, row.dataset.civTitle));
        });
    } catch (e) {
        el.innerHTML = emptyState('⚠', 'Failed to load data stream', e.message);
    }
}

function civRow(v) {
    return `
    <div class="version-row" data-id="${v.versionId}" data-civ-title="${escapeAttr(v.civTitle || '')}">
      <div class="version-main">
        <div class="version-civ-name">${v.civTitle || 'Untitled Dataset'}</div>
        <div class="version-meta">
          <span class="prov-uni">Institution: ${v.universityName || 'Unknown University'}</span>
          <span>Committed By: ${v.committedByName || '—'}</span>
          <span>Received: ${fmtDate(v.commitTimestamp)}</span>
          ${v.commitMessage ? `<span class="commit-msg">"${v.commitMessage}"</span>` : ''}
        </div>
      </div>
      <span class="version-hash">${(v.hash || '').substring(0, 10)}</span>
      ${statusBadge(v.reviewStatus)}
      <span class="version-arrow">→</span>
    </div>`;
}

function openVersionDetail(versionId, civTitle) {
    window.location.href = `reviewer-civilization.html?versionId=${encodeURIComponent(versionId)}&civTitle=${encodeURIComponent(civTitle || '')}`;
}

document.getElementById('btn-refresh-civs').addEventListener('click', loadCivilizations);

// ── My Marks ──────────────────────────────────────────────────────────────
async function loadMarks() {
    const el = document.getElementById('marks-list');
    el.innerHTML = skeleton();
    selectedMarkIds.clear();
    try {
        const marks = await getMyMarks();
        document.getElementById('stat-marks').textContent = marks.length;
        if (!marks.length) {
            el.innerHTML = emptyState('✓', 'Audit ledger empty', 'Granular decisions will appear here as you verify individual nodes.');
            return;
        }

        const toolbar = `
        <div class="marks-toolbar" id="marks-toolbar">
            <label class="marks-select-all-wrap">
                <input type="checkbox" id="marks-select-all">
                <span>Select all</span>
            </label>
            <button class="btn-delete-selected" id="btn-delete-selected" disabled>
                Delete selected (<span id="selected-count">0</span>)
            </button>
        </div>`;

        el.innerHTML = toolbar + marks.map(m => markRow(m)).join('');

        document.getElementById('marks-select-all').addEventListener('change', e => {
            const checked = e.target.checked;
            document.querySelectorAll('.mark-row-checkbox').forEach(cb => {
                cb.checked = checked;
                checked ? selectedMarkIds.add(cb.dataset.markId) : selectedMarkIds.delete(cb.dataset.markId);
            });
            syncDeleteButton();
        });

        document.querySelectorAll('.mark-row-checkbox').forEach(cb => {
            cb.addEventListener('change', e => {
                e.target.checked
                    ? selectedMarkIds.add(cb.dataset.markId)
                    : selectedMarkIds.delete(cb.dataset.markId);
                const all         = document.querySelectorAll('.mark-row-checkbox');
                const allChecked  = [...all].every(c => c.checked);
                const noneChecked = [...all].every(c => !c.checked);
                const selectAllCb = document.getElementById('marks-select-all');
                selectAllCb.checked       = allChecked;
                selectAllCb.indeterminate = !allChecked && !noneChecked;
                syncDeleteButton();
            });
        });

        document.querySelectorAll('.btn-delete-mark').forEach(btn => {
            btn.addEventListener('click', async e => {
                e.stopPropagation();
                if (!confirm('Delete this mark?')) return;
                try {
                    await deleteMark(btn.dataset.markId);
                    toast('Mark deleted', 'success');
                    loadMarks();
                } catch (err) {
                    toast(err.message, 'error');
                }
            });
        });

        document.getElementById('btn-delete-selected').addEventListener('click', async () => {
            if (!selectedMarkIds.size) return;
            if (!confirm(`Delete ${selectedMarkIds.size} mark${selectedMarkIds.size > 1 ? 's' : ''}? This cannot be undone.`)) return;

            const btn = document.getElementById('btn-delete-selected');
            btn.disabled    = true;
            btn.textContent = 'Deleting…';

            const ids     = [...selectedMarkIds];
            const results = await Promise.allSettled(ids.map(id => deleteMark(id)));
            const failed  = results.filter(r => r.status === 'rejected').length;
            const deleted = results.filter(r => r.status === 'fulfilled').length;

            toast(`${deleted} mark${deleted !== 1 ? 's' : ''} deleted${failed ? `, ${failed} failed` : ''}`,
                failed ? 'info' : 'success');
            loadMarks();
        });

    } catch (e) {
        el.innerHTML = emptyState('⚠', 'Failed to load ledger', e.message);
    }
}

function syncDeleteButton() {
    const btn = document.getElementById('btn-delete-selected');
    if (!btn) return;
    const count = selectedMarkIds.size;
    btn.disabled = count === 0;
    document.getElementById('selected-count').textContent = count;
}

function markRow(m) {
    const statusColors = {
        APPROVED:           'background:#D1FAE5;color:#065F46;border:1px solid #A7F3D0',
        REJECTED:           'background:#FEE2E2;color:#991B1B;border:1px solid #FCA5A5',
        REVISION_REQUESTED: 'background:#FEF3C7;color:#92400E;border:1px solid #FDE68A',
    };
    const style = statusColors[m.markStatus] || '';
    return `
    <div class="version-row readonly mark-row" data-mark-id="${m.markId}">
      <input type="checkbox" class="mark-row-checkbox" data-mark-id="${m.markId}"
             style="flex-shrink:0;width:15px;height:15px;cursor:pointer"
             onclick="event.stopPropagation()">
      <div class="version-main">
        <div class="version-civ-name">${m.entryTitle || 'Untitled Node'}</div>
        <div class="version-meta">
          <span>Audited: ${fmtDate(m.markedAt)}</span>
          ${m.reviewerNote ? `<span>Note: ${m.reviewerNote}</span>` : ''}
        </div>
      </div>
      <span class="status-badge" style="${style}">${m.markStatus.replace(/_/g, ' ')}</span>
      <button class="icon-btn btn-delete-mark" data-mark-id="${m.markId}" title="Delete mark">✕</button>
    </div>`;
}

document.getElementById('btn-refresh-marks').addEventListener('click', loadMarks);

// ── Central Master Compiler Index ─────────────────────────────────────────
async function loadCentral() {
    const el = document.getElementById('central-grid');
    el.innerHTML = skeleton(4);
    try {
        const civs = await getCentralCivilizations();
        if (!civs.length) {
            el.innerHTML = emptyState('⊞', 'Master Archive Empty', 'No standardized cross-institutional records compiled yet.');
            return;
        }
        el.innerHTML = civs.map(c => centralCard(c)).join('');
        el.querySelectorAll('.central-card').forEach(card => {
            card.addEventListener('click', () => openCentralDetail(card.dataset.id));
        });
    } catch (e) {
        el.innerHTML = emptyState('⚠', 'Failed to read index', e.message);
    }
}

function centralCard(c) {
    return `
    <div class="central-card" data-id="${c.centralCivId}">
      <div class="central-card-top">
        <span class="central-card-title">${c.title}</span>
      </div>
      <p class="central-card-desc">${c.description || '<em>No description provided.</em>'}</p>
      <div class="central-card-footer">
        <span class="years-badge">${formatYear(c.startYear)} – ${formatYear(c.endYear)}</span>
        <div class="central-card-counts">
          <span>${c.volumeCount} Vol${c.volumeCount !== 1 ? 's' : ''}</span>
          <span>${c.entryCount} Node${c.entryCount !== 1 ? 's' : ''}</span>
        </div>
      </div>
    </div>`;
}

// ── Central Panel ─────────────────────────────────────────────────────────
async function openCentralDetail(centralCivId) {
    currentCivId = centralCivId;
    selectedEntriesForDivergence = [];
    const panel   = document.getElementById('panel-central');
    const content = document.getElementById('central-detail-content');
    panel.classList.add('open');
    content.innerHTML = skeleton(3);
    try {
        const civ = await getCentralDetail(centralCivId);
        currentCivTitle       = civ.title || '';
        content.innerHTML     = renderCentralDetail(civ);
        wireCentralDetailButtons(civ);
    } catch (e) {
        content.innerHTML = `<p style="padding:20px;color:#991B1B;font-weight:500;">${e.message}</p>`;
    }
}

document.getElementById('btn-back-central').addEventListener('click', () => {
    document.getElementById('panel-central').classList.remove('open');
});

function renderCentralDetail(civ) {
    const volumes = (civ.volumes || []).map(vol => `
    <div class="volume-block">
      <div class="volume-header">
        <div style="display:flex;flex-direction:column;gap:2px">
          <span class="volume-title">${vol.title}</span>
          <span class="volume-years">${formatYear(vol.startYear)} – ${formatYear(vol.endYear)}</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px">
          <button class="btn-primary" style="padding:5px 12px;font-size:0.78rem"
                  data-add-entry-vol="${vol.volumeId}">+ Link Entry</button>
          <button class="btn-primary" style="padding:5px 12px;font-size:0.78rem;background:#0F766E"
                  data-batch-add-vol="${vol.volumeId}">+ Batch Link</button>
          <button class="btn-delete-volume icon-btn"
                  data-vol-id="${vol.volumeId}"
                  title="Delete volume and all its entries">🗑</button>
        </div>
      </div>
      ${vol.entries && vol.entries.length
        ? vol.entries.map(e => entryRow(e, civ.centralCivId, vol.volumeId)).join('')
        : '<div style="padding:14px 16px;color:var(--faint);font-size:0.82rem;font-style:italic">No entries linked yet.</div>'}
    </div>`).join('');

    return `
    <div class="detail-header">
      <div>
        <h2 class="detail-title">${civ.title}</h2>
        <div class="detail-meta">
          <span>${formatYear(civ.startYear)} – ${formatYear(civ.endYear)}</span>
          <span>Curator: ${civ.createdByName || '—'}</span>
          <span>Last updated: ${fmtDate(civ.lastUpdatedAt)}</span>
        </div>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="btn-primary" id="btn-add-volume">+ Add Volume</button>
        <button class="btn-primary"
                style="background:#4B5563;opacity:0.6;cursor:not-allowed"
                id="btn-flag-div" disabled>⚑ Flag Divergence (Select 2)</button>
      </div>
    </div>
    ${civ.description ? `<p class="central-detail-description-block" style="margin-bottom:20px;color:var(--muted);font-size:0.875rem;line-height:1.6">${civ.description}</p>` : ''}
    <div class="detail-section">
      <div class="section-context-bar" style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
        <span class="section-label">Compiled Chronology — oldest to newest</span>
        <small style="color:var(--faint);font-size:0.75rem">Select two entries to flag a divergence.</small>
      </div>
      ${volumes || emptyState('📚', 'No volumes', 'Add a volume to begin building the chronology.')}
    </div>`;
}

function entryRow(e, civId, volId) {
    const divBadge = e.isDivergent ? `<span class="divergence-badge">⚑ Divergent</span>` : '';
    const title    = e.title || e.entryTitle || 'Unnamed Node';
    const years    = (e.startYear != null || e.endYear != null)
        ? `${formatYear(e.startYear)} – ${formatYear(e.endYear)}`
        : '<span style="color:var(--faint);font-style:italic">No date range</span>';

    return `
    <div class="entry-row dynamic-selectable-entry"
         data-entry-id="${e.centralEntryId}"
         data-entry-title="${escapeAttr(title)}"
         data-civ-id="${civId}"
         data-vol-id="${volId}">
      <div style="display:flex;align-items:center;gap:12px;min-width:0;flex:1">
        <input type="checkbox" class="divergence-selector-checkbox" style="pointer-events:none;">
        <div style="min-width:0">
          <div class="entry-title">${title}</div>
          <div style="font-family:var(--mono);font-size:0.68rem;color:var(--faint);margin-top:2px">${years}</div>
        </div>
      </div>
      <span class="entry-meta">${e.sourceUniversityName || 'Unknown Institution'}</span>
      ${divBadge}
      <button class="btn-delete-entry icon-btn"
              data-entry-id="${e.centralEntryId}"
              data-civ-id="${civId}"
              data-vol-id="${volId}"
              title="Remove entry from archive">✕</button>
    </div>`;
}

function wireCentralDetailButtons(civ) {

    // ── Add Volume — opens two-tab modal ──────────────────────────────────
    document.getElementById('btn-add-volume').addEventListener('click', () => {
        openVolumeModal();
    });

    // ── Delete entry ──────────────────────────────────────────────────────
    document.querySelectorAll('.btn-delete-entry').forEach(btn => {
        btn.addEventListener('click', async e => {
            e.stopPropagation();
            if (!confirm('Remove this entry from the archive?')) return;
            const { entryId, civId, volId } = btn.dataset;
            try {
                await deleteEntry(civId, volId, entryId);
                toast('Entry removed from archive', 'success');
                openCentralDetail(currentCivId);
            } catch (err) {
                toast(err.message, 'error');
            }
        });
    });

    // ── Delete volume ─────────────────────────────────────────────────────
    document.querySelectorAll('.btn-delete-volume').forEach(btn => {
        btn.addEventListener('click', async e => {
            e.stopPropagation();
            if (!confirm('Delete this volume and ALL its entries? This cannot be undone.')) return;
            try {
                await deleteVolume(currentCivId, btn.dataset.volId);
                toast('Volume deleted', 'success');
                openCentralDetail(currentCivId);
            } catch (err) {
                toast(err.message, 'error');
            }
        });
    });

    // ── Single entry link ─────────────────────────────────────────────────
    document.querySelectorAll('[data-add-entry-vol]').forEach(btn => {
        btn.addEventListener('click', e => {
            e.stopPropagation();
            currentVolId = btn.dataset.addEntryVol;
            isBatchMode  = false;
            openAddEntryModal();
        });
    });

    // ── Batch link ────────────────────────────────────────────────────────
    document.querySelectorAll('[data-batch-add-vol]').forEach(btn => {
        btn.addEventListener('click', e => {
            e.stopPropagation();
            currentVolId = btn.dataset.batchAddVol;
            isBatchMode  = true;
            openAddEntryModal();
        });
    });

    // ── Divergence selector ───────────────────────────────────────────────
    const divergenceSubmitBtn = document.getElementById('btn-flag-div');
    divergenceSubmitBtn.addEventListener('click', () => {
        if (selectedEntriesForDivergence.length === 2) {
            openModal('modal-flag-divergence');
            document.getElementById('div-primary-id').value        = selectedEntriesForDivergence[0].id;
            document.getElementById('div-primary-name').textContent = selectedEntriesForDivergence[0].title;
            document.getElementById('div-conflict-id').value       = selectedEntriesForDivergence[1].id;
            document.getElementById('div-conflict-name').textContent = selectedEntriesForDivergence[1].title;
        }
    });

    document.querySelectorAll('.dynamic-selectable-entry').forEach(row => {
        row.addEventListener('click', e => {
            if (e.target.classList.contains('btn-delete-entry')) return;
            const entryId    = row.dataset.entryId;
            const entryTitle = row.dataset.entryTitle;
            const checkbox   = row.querySelector('.divergence-selector-checkbox');
            const existingIndex = selectedEntriesForDivergence.findIndex(i => i.id === entryId);

            if (existingIndex > -1) {
                selectedEntriesForDivergence.splice(existingIndex, 1);
                row.classList.remove('selected-for-divergence');
                if (checkbox) checkbox.checked = false;
            } else {
                if (selectedEntriesForDivergence.length >= 2) {
                    toast('Deselect an entry first.', 'info');
                    return;
                }
                selectedEntriesForDivergence.push({ id: entryId, title: entryTitle });
                row.classList.add('selected-for-divergence');
                if (checkbox) checkbox.checked = true;
            }

            const count = selectedEntriesForDivergence.length;
            if (count === 2) {
                divergenceSubmitBtn.removeAttribute('disabled');
                divergenceSubmitBtn.style.cssText = 'background:var(--rev-ink);opacity:1;cursor:pointer';
                divergenceSubmitBtn.textContent   = '⚑ Flag Divergence';
            } else {
                divergenceSubmitBtn.setAttribute('disabled', 'true');
                divergenceSubmitBtn.style.cssText = 'background:#4B5563;opacity:0.6;cursor:not-allowed';
                divergenceSubmitBtn.textContent   = `⚑ Flag Divergence (${count}/2)`;
            }
        });
    });
}

// ── Volume Modal: Two-tab (From Marks / Manual) ───────────────────────────

function setVolTab(tab) {
    activeVolTab = tab;
    document.querySelectorAll('.vol-tab').forEach(t => {
        const active = t.dataset.tab === tab;
        t.style.color        = active ? 'var(--teal)' : 'var(--muted)';
        t.style.borderBottom = active ? '2px solid var(--teal)' : '2px solid transparent';
        t.style.fontWeight   = active ? '500' : '400';
    });
    document.getElementById('vol-panel-marks').style.display  = tab === 'marks'  ? 'block' : 'none';
    document.getElementById('vol-panel-manual').style.display = tab === 'manual' ? 'block' : 'none';
}

function openVolumeModal() {
    // Reset state
    selectedVolumeMarkId = null;
    document.getElementById('vol-mark-position').value = '';
    ['vol-title', 'vol-start', 'vol-end', 'vol-position'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });

    // Default to marks tab and load
    setVolTab('marks');
    loadVolumeMarksIntoModal();
    openModal('modal-add-volume');
}

// Wire tab buttons (runs once at boot since they live in the static HTML)
document.querySelectorAll('.vol-tab').forEach(tab => {
    tab.addEventListener('click', () => setVolTab(tab.dataset.tab));
});

async function loadVolumeMarksIntoModal() {
    const el = document.getElementById('vol-marks-list');
    el.innerHTML = '<div style="padding:20px;text-align:center;color:var(--faint);font-size:0.82rem">Loading saved volume marks…</div>';
    try {
        const marks = await getMyVolumeMarks();
        if (!marks.length) {
            el.innerHTML = `
                <div style="padding:24px 16px;text-align:center;color:var(--faint);font-size:0.82rem;line-height:1.6">
                    No volume marks yet.<br>
                    Open a civilization in the read-only view and click <strong style="color:var(--teal)">+ Mark Volume</strong> on any volume node.
                </div>`;
            return;
        }

        el.innerHTML = marks.map(m => `
            <div class="vol-mark-pick-row" data-mark-id="${m.markId}"
                 style="display:flex;flex-direction:column;gap:4px;padding:12px 14px;cursor:pointer;
                        border-radius:6px;margin-bottom:6px;border:1px solid var(--rule);
                        transition:background 0.12s,border-color 0.12s">
              <div style="display:flex;align-items:center;gap:8px">
                <span style="font-size:0.88rem;font-weight:500;color:var(--ink);flex:1">${m.volumeTitle}</span>
                <span style="font-family:var(--mono);font-size:0.65rem;color:var(--faint);white-space:nowrap">
                  ${formatYear(m.startYear)} – ${formatYear(m.endYear)}
                </span>
              </div>
              <div style="font-family:var(--mono);font-size:0.7rem;color:var(--faint)">
                ${m.civTitle || '—'} · ${m.universityName || '—'}
              </div>
              ${m.reviewerNote ? `<div style="font-size:0.72rem;color:var(--muted);font-style:italic">${m.reviewerNote}</div>` : ''}
            </div>`).join('');

        el.querySelectorAll('.vol-mark-pick-row').forEach(row => {
            row.addEventListener('click', () => {
                el.querySelectorAll('.vol-mark-pick-row').forEach(r => {
                    r.style.background   = '';
                    r.style.borderColor  = 'var(--rule)';
                });
                row.style.background  = 'var(--teal-bg, rgba(13,148,136,0.07))';
                row.style.borderColor = 'var(--teal)';
                selectedVolumeMarkId  = row.dataset.markId;
            });
        });
    } catch (e) {
        el.innerHTML = `<div style="padding:16px;color:var(--rej-ink);font-size:0.82rem">${e.message}</div>`;
    }
}

// ── Submit Volume (handles both tabs) ─────────────────────────────────────
document.getElementById('btn-submit-volume').addEventListener('click', async () => {
    const btn = document.getElementById('btn-submit-volume');
    btn.disabled    = true;
    btn.textContent = 'Structuring…';

    try {
        if (activeVolTab === 'marks') {
            // ── From Marks path ───────────────────────────────────────────
            if (!selectedVolumeMarkId) {
                toast('Select a volume mark from the list', 'error');
                return;
            }
            const position = parseInt(document.getElementById('vol-mark-position').value) || 1;
            await addVolumeFromMark(currentCivId, {
                volumeMarkId: selectedVolumeMarkId,
                position,
            });
            toast('Volume added from mark', 'success');

        } else {
            // ── Manual path ───────────────────────────────────────────────
            const dto = {
                title:     document.getElementById('vol-title').value.trim(),
                startYear: numOrNull('vol-start'),
                endYear:   numOrNull('vol-end'),
                position:  parseInt(document.getElementById('vol-position').value) || 1,
            };
            if (!dto.title) {
                toast('Volume title is required', 'error');
                return;
            }
            await addVolume(currentCivId, dto);
            toast('Volume added', 'success');
        }

        closeModal('modal-add-volume');
        openCentralDetail(currentCivId);

        // Reset both sets of fields
        selectedVolumeMarkId = null;
        document.getElementById('vol-mark-position').value = '';
        ['vol-title', 'vol-start', 'vol-end', 'vol-position'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });

    } catch (e) {
        toast(e.message, 'error');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Add Volume';
    }
});

// ── Add Entry Modal ───────────────────────────────────────────────────────
async function openAddEntryModal() {
    const modalTitle = document.querySelector('#modal-add-entry .modal-title');
    if (modalTitle) modalTitle.textContent = isBatchMode
        ? 'Batch Link Approved Entries' : 'Link Approved Entry to Archive';

    openModal('modal-add-entry');
    document.getElementById('entry-position').value = '';

    const listEl = document.getElementById('approved-marks-list');
    listEl.innerHTML = '<div style="padding:20px;text-align:center;color:var(--faint);font-size:0.82rem">Loading approved entries…</div>';

    try {
        const marks      = await getMyMarks();
        const allApproved = marks.filter(m => m.markStatus?.toUpperCase() === 'APPROVED');
        approvedMarks    = allApproved.length ? allApproved : [];

        if (!approvedMarks.length) {
            listEl.innerHTML = `<div style="padding:24px;text-align:center;color:var(--faint);font-size:0.82rem">
                No approved entries found. Mark entries as Approved on a civilization's review page first.</div>`;
            return;
        }

        const hint = isBatchMode
            ? '<div style="padding:8px 12px;background:var(--teal-bg);border-bottom:1px solid var(--rule);font-size:0.78rem;color:var(--teal)">Click to select multiple entries. All selected will be added to this volume.</div>'
            : '';

        listEl.innerHTML = hint + approvedMarks.map((m, i) => `
            <div class="browser-row approved-entry-pick-row" data-idx="${i}"
                 style="flex-direction:column;align-items:flex-start;gap:6px;padding:14px 16px">
              <div style="display:flex;width:100%;align-items:center;gap:10px">
                ${isBatchMode ? `<input type="checkbox" class="batch-checkbox" style="flex-shrink:0;width:15px;height:15px">` : ''}
                <div style="font-size:0.9rem;font-weight:500;color:var(--ink);flex:1">${m.entryTitle || 'Untitled Entry'}</div>
                <span style="font-family:var(--mono);font-size:0.65rem;background:var(--pub-bg);color:var(--pub-ink);padding:2px 8px;border-radius:3px;flex-shrink:0">APPROVED</span>
              </div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:4px 16px;width:100%;padding-left:${isBatchMode ? '25px' : '0'}">
                <div style="font-family:var(--mono);font-size:0.72rem;color:var(--faint)">
                  <span style="color:var(--teal)">Civilization</span><br>${m.civTitle || '—'}
                </div>
                <div style="font-family:var(--mono);font-size:0.72rem;color:var(--faint)">
                  <span style="color:var(--teal)">Institution</span><br>${m.universityName || '—'}
                </div>
                <div style="font-family:var(--mono);font-size:0.72rem;color:var(--faint)">
                  <span style="color:var(--teal)">Period</span><br>
                  ${m.civStartYear != null ? formatYear(m.civStartYear) + ' – ' + formatYear(m.civEndYear) : '—'}
                </div>
                <div style="font-family:var(--mono);font-size:0.72rem;color:var(--faint)">
                  <span style="color:var(--teal)">Committed by</span><br>${m.committedByName || '—'}
                </div>
              </div>
              ${m.reviewerNote ? `<div style="font-size:0.75rem;color:var(--muted);font-style:italic;padding-left:${isBatchMode ? '25px' : '0'}">Note: ${m.reviewerNote}</div>` : ''}
            </div>`).join('');

        listEl.querySelectorAll('.approved-entry-pick-row').forEach(row => {
            row.addEventListener('click', () => {
                if (isBatchMode) {
                    row.classList.toggle('selected');
                    const cb = row.querySelector('.batch-checkbox');
                    if (cb) cb.checked = row.classList.contains('selected');
                } else {
                    listEl.querySelectorAll('.approved-entry-pick-row').forEach(r => r.classList.remove('selected'));
                    row.classList.add('selected');
                }
            });
        });
    } catch (e) {
        listEl.innerHTML = `<div style="padding:16px;color:var(--rej-ink);font-size:0.82rem">${e.message}</div>`;
    }
}

document.getElementById('btn-submit-entry').addEventListener('click', async () => {
    const btn      = document.getElementById('btn-submit-entry');
    const position = parseInt(document.getElementById('entry-position').value) || 1;

    if (isBatchMode) {
        const selectedRows = [...document.querySelectorAll('.approved-entry-pick-row.selected')];
        if (!selectedRows.length) { toast('Select at least one entry', 'error'); return; }

        btn.disabled    = true;
        btn.textContent = 'Linking…';
        try {
            const entries = selectedRows.map((row, idx) => {
                const m = approvedMarks[parseInt(row.dataset.idx, 10)];
                return {
                    sourceVersionId: m.versionId,
                    sourceNodeId:    m.nodeId,
                    position:        position + idx,
                };
            });
            const result = await addEntriesBatch(currentCivId, currentVolId, { entries });
            const added  = result.addedEntryIds?.length || 0;
            const failed = result.failures?.length       || 0;
            toast(`${added} entr${added !== 1 ? 'ies' : 'y'} linked${failed ? `, ${failed} failed` : ''}`,
                failed ? 'info' : 'success');
            if (failed) result.failures.forEach(f => console.warn('Batch fail:', f.nodeId, f.reason));
            closeModal('modal-add-entry');
            openCentralDetail(currentCivId);
        } catch (e) {
            toast(e.message, 'error');
        } finally {
            btn.disabled    = false;
            btn.textContent = 'Add to Archive';
        }
    } else {
        const selectedRow = document.querySelector('.approved-entry-pick-row.selected');
        if (!selectedRow) { toast('Select an entry from the list', 'error'); return; }
        const m = approvedMarks[parseInt(selectedRow.dataset.idx, 10)];
        if (!m) { toast('Could not resolve entry. Try again.', 'error'); return; }

        btn.disabled    = true;
        btn.textContent = 'Linking…';
        try {
            await addEntry(currentCivId, currentVolId, {
                sourceVersionId: m.versionId,
                sourceNodeId:    m.nodeId,
                position,
            });
            toast('Entry linked successfully', 'success');
            closeModal('modal-add-entry');
            openCentralDetail(currentCivId);
        } catch (e) {
            toast(e.message, 'error');
        } finally {
            btn.disabled    = false;
            btn.textContent = 'Add to Archive';
        }
    }
});

// ── New Central Civilization ──────────────────────────────────────────────
let searchTimeout = null;
const searchInput = document.getElementById('new-civ-search');
const pickerEl    = document.getElementById('civ-metadata-picker');

document.getElementById('btn-new-civ').addEventListener('click', () => {
    searchInput.value      = '';
    pickerEl.innerHTML     = '';
    pickerEl.style.display = 'none';
    selectedCivMetadata    = null;
    openModal('modal-new-civ');
});

searchInput.addEventListener('input', () => {
    clearTimeout(searchTimeout);
    const query = searchInput.value.trim();
    if (!query) {
        pickerEl.innerHTML     = '';
        pickerEl.style.display = 'none';
        selectedCivMetadata    = null;
        return;
    }
    searchTimeout = setTimeout(async () => {
        try {
            pickerEl.innerHTML     = '<div style="padding:10px;text-align:center;color:var(--faint);">Searching university records...</div>';
            pickerEl.style.display = 'block';

            const results = await getCivMetadata(query);
            if (!results || results.length === 0) {
                pickerEl.innerHTML  = `<div style="padding:16px;text-align:center;color:var(--faint);font-size:0.85rem;">No matching university submissions found for "${query}"</div>`;
                selectedCivMetadata = null;
                return;
            }

            pickerEl.innerHTML = `
                <div style="font-size:0.75rem;text-transform:uppercase;color:var(--faint);font-weight:500;margin-bottom:8px;font-family:var(--mono);">Select University Source Version</div>
                <div class="metadata-card-grid">
                    ${results.map((r, idx) => `
                    <div class="metadata-card" data-idx="${idx}">
                        <div class="metadata-card-header">
                            <span class="metadata-card-uni">${r.universityName || 'Unknown University'}</span>
                            <span class="metadata-card-years">${formatYear(r.startYear)} – ${formatYear(r.endYear)}</span>
                        </div>
                        <div style="font-weight:600;font-size:0.9rem;margin-bottom:4px;color:var(--ink)">${r.title}</div>
                        <p class="metadata-card-desc">${r.description || '<em>No description provided.</em>'}</p>
                        <div class="metadata-card-detail">
                            <div class="metadata-card-detail-item"><span>Institution</span>${r.universityName || '—'}</div>
                            <div class="metadata-card-detail-item"><span>Period</span>${formatYear(r.startYear)} – ${formatYear(r.endYear)}</div>
                            <div class="metadata-card-detail-item"><span>Submitted entries</span>${r.entryCount ?? '—'}</div>
                        </div>
                    </div>`).join('')}
                </div>`;

            pickerEl.querySelectorAll('.metadata-card').forEach(card => {
                card.addEventListener('click', () => {
                    pickerEl.querySelectorAll('.metadata-card').forEach(c => c.classList.remove('selected'));
                    card.classList.add('selected');
                    selectedCivMetadata = results[parseInt(card.dataset.idx, 10)];
                });
            });

        } catch (e) {
            pickerEl.innerHTML  = `<div style="padding:16px;text-align:center;color:var(--rej-ink);font-size:0.85rem;">Failed to fetch records: ${e.message}</div>`;
            selectedCivMetadata = null;
        }
    }, 300);
});

document.getElementById('btn-submit-new-civ').addEventListener('click', async () => {
    if (!selectedCivMetadata) {
        toast('Please select a university source card first', 'error');
        return;
    }
    const dto = {
        title:       selectedCivMetadata.title,
        description: selectedCivMetadata.description,
        startYear:   selectedCivMetadata.startYear,
        endYear:     selectedCivMetadata.endYear,
    };
    const btn       = document.getElementById('btn-submit-new-civ');
    btn.disabled    = true;
    btn.textContent = 'Generating Matrix…';
    try {
        await createCentralCivilization(dto);
        toast('Standardized Central Civilization Generated', 'success');
        closeModal('modal-new-civ');
        showView('central');
        loadCentral();
    } catch (e) {
        toast(e.message, 'error');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Create';
    }
});

// ── Flag Divergence ───────────────────────────────────────────────────────
document.getElementById('btn-submit-divergence').addEventListener('click', async () => {
    const dto = {
        primaryEntryId:     document.getElementById('div-primary-id').value.trim(),
        conflictingEntryId: document.getElementById('div-conflict-id').value.trim(),
        divergenceNote:     document.getElementById('div-note').value.trim(),
    };
    if (!dto.primaryEntryId || !dto.conflictingEntryId || !dto.divergenceNote) {
        toast('All divergence fields are required', 'error');
        return;
    }
    const btn       = document.getElementById('btn-submit-divergence');
    btn.disabled    = true;
    btn.textContent = 'Registering Assertion…';
    try {
        await flagDivergence(currentCivId, dto);
        toast('Divergence flagged successfully', 'success');
        closeModal('modal-flag-divergence');
        openCentralDetail(currentCivId);
        ['div-primary-id', 'div-conflict-id', 'div-note'].forEach(id => {
            document.getElementById(id).value = '';
        });
    } catch (e) {
        toast(e.message, 'error');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Flag Divergence';
    }
});

// ── System Boot ───────────────────────────────────────────────────────────
loadCivilizations();