Auth.requireAuth();

const role    = Auth.getRole();
const payload = Auth.getPayload();

// ─── Review state ─────────────────────────────────────────────────────────────
let allReviewMarks  = [];   // flat array of marks enriched with civId/civTitle
let reviewCivs      = [];   // [{civId, title}] for populating filter
let activeStatusFilter = '';
let reviewLoaded    = false;

/* ─── Init UI ─── */
function initUI() {
    const name   = payload?.sub || 'User';
    const letter = name.charAt(0).toUpperCase();

    document.getElementById('user-avatar').textContent     = letter;
    document.getElementById('user-name').textContent       = name;
    document.getElementById('user-role-label').textContent = role || '—';
    document.getElementById('nav-role').innerHTML =
        `<span class="role-badge role-${(role||'viewer').toLowerCase()}">${role}</span>`;

    if (role === 'ADMIN') {
        document.getElementById('page-title').textContent = 'University Civilizations';
        document.getElementById('page-sub').textContent   = 'All civilizations managed by your institution';

        document.getElementById('users-sidebar-btn').style.display = 'flex';
        document.getElementById('users-tab-btn').style.display     = 'inline-block';

        document.getElementById('review-sidebar-btn').style.display = 'flex';
        document.getElementById('review-tab-btn').style.display     = 'inline-block';

        document.getElementById('header-actions').innerHTML =
            `<button class="btn-new" onclick="openModal('modal-create-civ')">+ New Civilization</button>`;

    } else if (role === 'EDITOR') {
        document.getElementById('page-title').textContent = 'My Civilizations';
        document.getElementById('page-sub').textContent   = 'Civilization trees you have been assigned to edit';

        // Editors can see their own review feedback
        document.getElementById('review-sidebar-btn').style.display = 'flex';
        document.getElementById('review-tab-btn').style.display     = 'inline-block';

    } else {
        document.getElementById('page-title').textContent = 'Civilizations';
        document.getElementById('page-sub').textContent   = 'Browse civilizations from your institution';
    }
}

/* ─── Tab switching ─── */
function switchTab(name, clickedBtn) {
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-btn, .sidebar-link').forEach(b => b.classList.remove('active'));

    document.getElementById(`tab-${name}`).classList.add('active');
    clickedBtn.classList.add('active');

    if (name === 'users') {
        document.getElementById('users-tab-btn').classList.add('active');
        document.getElementById('users-sidebar-btn').classList.add('active');
        loadUsers();
    } else if (name === 'review') {
        document.getElementById('review-tab-btn').classList.add('active');
        document.getElementById('review-sidebar-btn').classList.add('active');
        loadReviewStatus();
    } else {
        document.querySelectorAll('.tab-btn')[0].classList.add('active');
        document.querySelectorAll('.sidebar-link')[0].classList.add('active');
    }
}

/* ─── Load civilizations ─── */
async function loadCivilizations() {
    const wrap = document.getElementById('civ-wrap');
    try {
        const civs = role === 'ADMIN' ? await CivAPI.getAll() : await CivAPI.getMy();

        if (!civs || !civs.length) {
            wrap.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">◈</div>
                    <h3>${role === 'ADMIN' ? 'No civilizations yet' : 'No civilizations assigned'}</h3>
                    <p>${role === 'ADMIN'
                ? 'Create your first civilization tree to get started.'
                : 'Contact your institution admin to be assigned as an editor.'}</p>
                </div>`;
            return;
        }

        wrap.innerHTML = `<div class="civ-grid">${civs.map(civCard).join('')}</div>`;
    } catch (err) {
        wrap.innerHTML = `<div class="empty-state"><h3>Failed to load</h3><p>${err.message}</p></div>`;
    }
}

function civCard(c) {
    const start = formatYear(c.startDate ?? c.startYear);
    const end   = formatYear(c.endDate ?? c.endYear);

    const deleteBtn = role === 'ADMIN'
        ? `<button class="btn-delete-civ" data-id="${c.civId}" onclick="deleteCivilization(event, '${c.civId}')">Delete</button>`
        : '';

    return `
        <a class="civ-card" href="civilization.html?civId=${c.civId}">
            <div class="civ-card-top">
                <div class="civ-title">${c.title || 'Untitled'}</div>
                <div class="civ-years">${start} – ${end}</div>
            </div>
            <p class="civ-desc">${c.description || 'No description provided.'}</p>
            <div class="civ-card-footer">
                <span class="link-btn">Open →</span>
                ${deleteBtn}
            </div>
        </a>`;
}

async function deleteCivilization(event, civId) {
    event.preventDefault();
    event.stopPropagation();
    if (!confirm('Permanently delete this civilization and all its versions?')) return;
    try {
        await CivAPI.delete(civId);
        showToast('Civilization deleted', 'success');
        loadCivilizations();
    } catch (err) {
        showToast(err.message || 'Delete failed', 'error');
    }
}

/* ─── Load users (Admin) ─── */
let usersLoaded = false;
async function loadUsers() {
    if (usersLoaded) return;
    const wrap = document.getElementById('users-wrap');
    try {
        const users = await CivAPI.getUniversityUsers();
        if (!users || !users.length) {
            wrap.innerHTML = `<div class="empty-state"><h3>No users found</h3></div>`;
            usersLoaded = true;
            return;
        }
        wrap.innerHTML = `
            <div class="table-wrap">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Role</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${users.map(u => `
                        <tr>
                            <td class="td-name">${[u.firstName, u.lastName].filter(Boolean).join(' ') || '—'}</td>
                            <td class="td-email">${u.email || '—'}</td>
                            <td><span class="role-badge role-${(u.role||'viewer').toLowerCase()}">${u.role || 'VIEWER'}</span></td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>`;
        usersLoaded = true;
    } catch (err) {
        wrap.innerHTML = `<div class="empty-state"><h3>Failed to load users</h3><p>${err.message}</p></div>`;
    }
}

/* ══════════════════════════════════════════════════════
   REVIEW STATUS
══════════════════════════════════════════════════════ */

async function loadReviewStatus() {
    if (reviewLoaded) { renderReviewPanel(); return; }

    const wrap = document.getElementById('review-wrap');
    wrap.innerHTML = `<div class="loading-row"><div class="spin"></div> Loading review data…</div>`;

    try {
        // Fetch all civilizations the user can access
        const civs = role === 'ADMIN' ? await CivAPI.getAll() : await CivAPI.getMy();

        if (!civs || !civs.length) {
            wrap.innerHTML = `<div class="empty-state"><div class="empty-icon">◑</div><h3>No civilizations</h3><p>No civilizations to review yet.</p></div>`;
            return;
        }

        // Normalise civ objects — backend may return civId or id
        const normCivs = civs.map(c => ({
            civId: c.civId || c.id,
            title: c.title || 'Untitled'
        }));

        reviewCivs = normCivs;

        // Populate civilization filter dropdown
        const civSelect = document.getElementById('review-civ-filter');
        civSelect.innerHTML = '<option value="">All civilizations</option>';
        reviewCivs.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.civId;
            opt.textContent = c.title;
            civSelect.appendChild(opt);
        });

        // Fetch entry marks for all civs in parallel — allSettled so one failure doesn't kill the rest
        const results = await Promise.allSettled(
            normCivs.map(c =>
                CivAPI.getEntryMarks(c.civId)
                    .then(marks => ({ civId: c.civId, civTitle: c.title, marks: Array.isArray(marks) ? marks : [] }))
            )
        );

        allReviewMarks = [];
        results.forEach(r => {
            if (r.status === 'fulfilled') {
                const { civId, civTitle, marks } = r.value;
                marks.forEach(m => {
                    allReviewMarks.push({ ...m, civId, civTitle });
                });
            } else {
                console.warn('getEntryMarks failed for one civ:', r.reason);
            }
        });

        console.log(`[Review] loaded ${allReviewMarks.length} total marks across ${normCivs.length} civs`);

        reviewLoaded = true;
        renderReviewPanel();

    } catch (err) {
        console.error('[Review] loadReviewStatus error:', err);
        wrap.innerHTML = `<div class="empty-state"><h3>Failed to load review data</h3><p>${err.message}</p></div>`;
    }
}

function setStatusFilter(btn, status) {
    document.querySelectorAll('.status-pill').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    activeStatusFilter = status;
    renderReviewPanel();
}

function applyReviewFilters() {
    renderReviewPanel();
}

function renderReviewPanel() {
    const wrap        = document.getElementById('review-wrap');
    const civFilter   = document.getElementById('review-civ-filter').value;
    const metaEl      = document.getElementById('review-filter-meta');

    // Apply filters
    let filtered = allReviewMarks;
    if (civFilter)           filtered = filtered.filter(m => m.civId === civFilter);
    if (activeStatusFilter)  filtered = filtered.filter(m => m.markStatus === activeStatusFilter);

    // Update meta count
    metaEl.textContent = `${filtered.length} entr${filtered.length !== 1 ? 'ies' : 'y'}`;

    if (!filtered.length) {
        wrap.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">◑</div>
                <h3>No review marks found</h3>
                <p>${allReviewMarks.length ? 'Try adjusting your filters.' : 'No entries have been reviewed yet.'}</p>
            </div>`;
        return;
    }

    // Summary stats (unfiltered by status so totals are global for civ filter)
    const base        = civFilter ? allReviewMarks.filter(m => m.civId === civFilter) : allReviewMarks;
    const countApproved  = base.filter(m => m.markStatus === 'APPROVED').length;
    const countRejected  = base.filter(m => m.markStatus === 'REJECTED').length;
    const countRevision  = base.filter(m => m.markStatus === 'REVISION_REQUESTED').length;

    const summaryHTML = `
        <div class="review-summary-bar">
            <div class="review-stat-card">
                <div class="review-stat-label">Total reviewed</div>
                <div class="review-stat-value">${base.length}</div>
            </div>
            <div class="review-stat-card stat-approved">
                <div class="review-stat-label">Approved</div>
                <div class="review-stat-value">${countApproved}</div>
            </div>
            <div class="review-stat-card stat-rejected">
                <div class="review-stat-label">Rejected</div>
                <div class="review-stat-value">${countRejected}</div>
            </div>
            <div class="review-stat-card stat-revision">
                <div class="review-stat-label">Revision needed</div>
                <div class="review-stat-value">${countRevision}</div>
            </div>
        </div>`;

    // Group filtered marks by civilization
    const grouped = {};
    filtered.forEach(m => {
        if (!grouped[m.civId]) grouped[m.civId] = { civTitle: m.civTitle, civId: m.civId, marks: [] };
        grouped[m.civId].marks.push(m);
    });

    const statusIcons = { APPROVED: '✓', REJECTED: '✕', REVISION_REQUESTED: '↺' };

    const groupsHTML = Object.values(grouped).map(group => {
        const cardsHTML = group.marks
            .slice().sort((a, b) => new Date(b.markedAt || 0) - new Date(a.markedAt || 0))
            .map(m => {
                const dateStr = m.markedAt
                    ? new Date(m.markedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
                    : '';
                const noteHTML = m.reviewerNote
                    ? `<div class="review-entry-note">"${escHtml(m.reviewerNote)}"</div>`
                    : '';
                return `
                    <div class="review-entry-card status-${m.markStatus}">
                        <div class="review-entry-top">
                            <div class="review-entry-title">${escHtml(m.entryTitle || 'Untitled Entry')}</div>
                            <span class="review-mark-badge badge-${m.markStatus}">
                                ${statusIcons[m.markStatus] || ''} ${m.markStatus.replace(/_/g,' ')}
                            </span>
                        </div>
                        ${noteHTML}
                        <div class="review-entry-meta">
                            ${m.reviewerName ? `<span>${escHtml(m.reviewerName)}</span><span class="review-entry-meta-sep">·</span>` : ''}
                            ${dateStr ? `<span>${dateStr}</span>` : ''}
                        </div>
                    </div>`;
            }).join('');

        return `
            <div class="review-civ-group">
                <div class="review-civ-header">
                    <div class="review-civ-name">${escHtml(group.civTitle)}</div>
                    <a class="review-civ-link" href="civilization.html?civId=${group.civId}">View civilization →</a>
                    <span class="review-civ-count">${group.marks.length} entr${group.marks.length !== 1 ? 'ies' : 'y'}</span>
                </div>
                <div class="review-entries-grid">
                    ${cardsHTML}
                </div>
            </div>`;
    }).join('');

    wrap.innerHTML = summaryHTML + groupsHTML;
}

function escHtml(str) {
    return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

/* ─── Create civilization ─── */
document.getElementById('create-civ-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('create-civ-btn');
    btn.disabled = true;
    btn.textContent = 'Creating…';
    try {
        const civId = await CivAPI.create({
            title:       document.getElementById('civ-title').value.trim(),
            description: document.getElementById('civ-desc').value.trim(),
            startYear:   parseInt(document.getElementById('civ-start').value),
            endYear:     parseInt(document.getElementById('civ-end').value),
            commitMsg:   document.getElementById('civ-commit').value.trim()
        });
        showToast('Civilization created', 'success');
        closeModal('modal-create-civ');
        setTimeout(() => window.location.href = `civilization.html?civId=${civId}`, 500);
    } catch (err) {
        showToast(err.message || 'Failed to create', 'error');
        btn.disabled = false;
        btn.textContent = 'Create';
    }
});

/* ─── Modal helpers ─── */
function openModal(id)  { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

document.addEventListener('click', e => {
    if (e.target.classList.contains('modal-overlay')) e.target.classList.remove('open');
});

/* ─── Toast ─── */
function showToast(message, type = 'info') {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.textContent = message;
    c.appendChild(t);
    setTimeout(() => { t.style.opacity='0'; t.style.transition='0.2s'; setTimeout(()=>t.remove(),200); }, 3500);
}

/* ─── Boot ─── */
initUI();
loadCivilizations();