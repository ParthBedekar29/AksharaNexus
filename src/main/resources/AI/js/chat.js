document.addEventListener('DOMContentLoaded', async () => {
    if (!api.isLoggedIn()) { window.location.href = 'login.html'; return; }

    const user           = api.getUser();
    const messagesEl     = document.getElementById('chat-messages');
    const textarea       = document.getElementById('chat-input');
    const sendBtn        = document.getElementById('send-btn');
    const userNameEl     = document.getElementById('user-name');
    const logoutBtn      = document.getElementById('logout-btn');
    const themeBox       = document.getElementById('theme-checkbox');
    const htmlEl         = document.documentElement;
    const newChatBtn     = document.getElementById('new-chat-btn');
    const sidebarToggle  = document.getElementById('sidebar-toggle');
    const sidebar        = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');
    const sessionsEl     = document.getElementById('sidebar-sessions');

    let currentSessionId = null;

    if (userNameEl && user) userNameEl.textContent = user.firstName;

    // ── Theme ─────────────────────────────────────────────────────────────────
    const savedTheme = localStorage.getItem('oracle-theme') || 'dark';
    htmlEl.setAttribute('data-theme', savedTheme);
    themeBox.checked = savedTheme === 'light';
    themeBox.addEventListener('change', () => {
        const next = themeBox.checked ? 'light' : 'dark';
        htmlEl.setAttribute('data-theme', next);
        localStorage.setItem('oracle-theme', next);
    });

    // ── Logout ────────────────────────────────────────────────────────────────
    logoutBtn?.addEventListener('click', (e) => {
        e.preventDefault();
        api.clearSession();
        window.location.href = 'index.html';
    });

    // ── Sidebar toggle ────────────────────────────────────────────────────────
    sidebarToggle.addEventListener('click', () => toggleSidebar());
    sidebarOverlay.addEventListener('click', () => closeSidebar());

    function toggleSidebar() {
        sidebar.classList.toggle('open');
        sidebarOverlay.classList.toggle('visible');
    }
    function closeSidebar() {
        sidebar.classList.remove('open');
        sidebarOverlay.classList.remove('visible');
    }

    // ── Load session list ─────────────────────────────────────────────────────
    async function loadSessions() {
        try {
            const sessions = await api.getSessions();
            renderSessions(sessions);
        } catch (e) { /* silently fail */ }
    }

    function renderSessions(sessions) {
        sessionsEl.innerHTML = '';
        if (sessions.length === 0) {
            sessionsEl.innerHTML = '<div class="sidebar-empty">No previous chats</div>';
            return;
        }
        sessions.forEach(s => {
            const item = document.createElement('div');
            item.className = 'session-item' + (s.id === currentSessionId ? ' active' : '');
            item.dataset.id = s.id;
            item.innerHTML = `
                <span class="session-title">${escHtml(s.title)}</span>
                <button class="session-delete" data-id="${s.id}" title="Delete chat">
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>`;

            item.addEventListener('click', (e) => {
                if (e.target.closest('.session-delete')) return;
                loadSession(s.id, s.title);
                closeSidebar();
            });

            item.querySelector('.session-delete').addEventListener('click', async (e) => {
                e.stopPropagation();
                await deleteSession(s.id, item);
            });

            sessionsEl.appendChild(item);
        });
    }

    // ── Load a past session ───────────────────────────────────────────────────
    async function loadSession(sessionId, title) {
        currentSessionId = sessionId;
        clearMessages();

        // Mark active in sidebar
        document.querySelectorAll('.session-item').forEach(el => {
            el.classList.toggle('active', el.dataset.id === sessionId);
        });

        try {
            const messages = await api.getMessages(sessionId);
            if (messages.length === 0) {
                showEmpty();
                return;
            }
            hideEmpty();
            messages.forEach(m => {
                if (m.role === 'USER') {
                    appendUserMessage(m.content, false);
                } else {
                    appendOracleMessage({ answer: m.content, sourceCitations: [], civilizationMatched: null }, false);
                }
            });
            scrollToBottom();
        } catch (e) {
            showEmpty();
        }
    }

    // ── New chat ──────────────────────────────────────────────────────────────
    newChatBtn.addEventListener('click', () => {
        currentSessionId = null;
        clearMessages();
        showEmpty();
        closeSidebar();
        document.querySelectorAll('.session-item').forEach(el => el.classList.remove('active'));
    });

    // ── Delete session ────────────────────────────────────────────────────────
    async function deleteSession(sessionId, itemEl) {
        try {
            await api.deleteSession(sessionId);
            itemEl.remove();
            if (currentSessionId === sessionId) {
                currentSessionId = null;
                clearMessages();
                showEmpty();
            }
            if (sessionsEl.children.length === 0) {
                sessionsEl.innerHTML = '<div class="sidebar-empty">No previous chats</div>';
            }
        } catch (e) { /* silently fail */ }
    }

    // ── Textarea auto-resize ──────────────────────────────────────────────────
    textarea.addEventListener('input', () => {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 130) + 'px';
    });

    textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    sendBtn.addEventListener('click', sendMessage);

    // ── Core send ─────────────────────────────────────────────────────────────
    async function sendMessage() {
        const text = textarea.value.trim();
        if (!text || sendBtn.disabled) return;

        // Create session on first message
        const isNewSession = !currentSessionId;
        if (isNewSession) {
            try {
                const session = await api.createSession(text);
                currentSessionId = session.id;
                // Add to top of sidebar immediately
                prependSession(session);
            } catch (e) {
                // Continue without saving if it fails
            }
        }

        appendUserMessage(text);
        textarea.value = '';
        textarea.style.height = 'auto';
        setSending(true);

        const typingId = appendTyping();

        try {
            const data = await api.query(text);
            removeTyping(typingId);
            appendOracleMessage(data);

            // Save message pair in background — don't await, don't block UI
            if (currentSessionId) {
                api.saveMessages(currentSessionId, text, data.answer).catch(() => {});
                // Update updatedAt in sidebar
                bumpSession(currentSessionId);
            }
        } catch (err) {
            removeTyping(typingId);
            appendOracleMessage({
                answer: err.message,
                sourceCitations: [],
                civilizationMatched: null
            });
        } finally {
            setSending(false);
        }
    }

    // ── Sidebar helpers ───────────────────────────────────────────────────────
    function prependSession(session) {
        // Remove empty state if present
        const empty = sessionsEl.querySelector('.sidebar-empty');
        if (empty) empty.remove();

        const item = document.createElement('div');
        item.className = 'session-item active';
        item.dataset.id = session.id;
        item.innerHTML = `
            <span class="session-title">${escHtml(session.title)}</span>
            <button class="session-delete" data-id="${session.id}" title="Delete chat">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
            </button>`;

        item.addEventListener('click', (e) => {
            if (e.target.closest('.session-delete')) return;
            loadSession(session.id, session.title);
            closeSidebar();
        });
        item.querySelector('.session-delete').addEventListener('click', async (e) => {
            e.stopPropagation();
            await deleteSession(session.id, item);
        });

        // Deactivate others
        document.querySelectorAll('.session-item').forEach(el => el.classList.remove('active'));
        sessionsEl.prepend(item);
    }

    function bumpSession(sessionId) {
        const item = sessionsEl.querySelector(`[data-id="${sessionId}"]`);
        if (item) sessionsEl.prepend(item);
    }

    // ── Render helpers ────────────────────────────────────────────────────────
    function appendUserMessage(text, animate = true) {
        hideEmpty();
        const div = document.createElement('div');
        div.className = 'msg msg-user' + (animate ? '' : ' no-anim');
        div.innerHTML = `<div class="msg-user-bubble">${escHtml(text)}</div>`;
        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function appendOracleMessage(data, animate = true) {
        const div = document.createElement('div');
        div.className = 'msg msg-oracle' + (animate ? '' : ' no-anim');

        const civTag = data.civilizationMatched
            ? `<span class="civ-tag">${escHtml(data.civilizationMatched)}</span>`
            : '';

        const citations = (data.sourceCitations || [])
            .map(c => `<div class="citation-item">${escHtml(c)}</div>`)
            .join('');

        const citationsBlock = citations
            ? `<div class="msg-citations">
                 <div class="citations-label">Sources</div>
                 ${citations}
               </div>`
            : '';

        div.innerHTML = `
            <div class="msg-oracle-meta">
                <span class="oracle-dot"></span>
                <span class="oracle-label">Oracle</span>
                ${civTag}
            </div>
            <div class="msg-oracle-inner">
                <div class="msg-oracle-content">${marked.parse(data.answer)}</div>
                ${citationsBlock}
            </div>`;

        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function appendTyping() {
        const id  = 'typing-' + Date.now();
        const div = document.createElement('div');
        div.className = 'msg msg-oracle';
        div.id = id;
        div.innerHTML = `
        <div class="msg-oracle-meta">
            <span class="oracle-dot"></span>
            <span class="oracle-label">Oracle</span>
        </div>
        <div class="typing-indicator">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <span class="typing-status" id="${id}-status">Analyzing query…</span>
        </div>`;
        messagesEl.appendChild(div);
        scrollToBottom();

        const phases = [
            [0,    'Analyzing query…'],
            [600,  'Searching civilization records…'],
            [1400, 'Ranking historical sources…'],
            [2200, 'Consulting the Oracle…'],
        ];
        const timers = [];
        phases.forEach(([delay, label]) => {
            timers.push(setTimeout(() => {
                const el = document.getElementById(`${id}-status`);
                if (el) el.textContent = label;
            }, delay));
        });
        div._typingTimers = timers;
        return id;
    }

    function removeTyping(id) {
        const el = document.getElementById(id);
        if (el) {
            (el._typingTimers || []).forEach(clearTimeout);
            el.remove();
        }
    }

    function clearMessages() {
        messagesEl.innerHTML = '';
    }

    function showEmpty() {
        if (document.getElementById('chat-empty')) return;
        const div = document.createElement('div');
        div.className = 'chat-empty';
        div.id = 'chat-empty';
        div.innerHTML = `
            <span class="chat-empty-glyph">𓂀</span>
            <h2>Ask the Oracle</h2>
            <p>Query civilizations, events, governance, trade routes, and more — with cited sources.</p>`;
        messagesEl.appendChild(div);
    }

    function hideEmpty() {
        document.getElementById('chat-empty')?.remove();
    }

    function setSending(val) {
        sendBtn.disabled  = val;
        textarea.disabled = val;
    }

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function escHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    // ── Init ──────────────────────────────────────────────────────────────────
// ── Init ──────────────────────────────────────────────────────────────────
    await loadSessions();
    if (window.innerWidth > 768) sidebar.classList.add('open');  // open by default on desktop
});