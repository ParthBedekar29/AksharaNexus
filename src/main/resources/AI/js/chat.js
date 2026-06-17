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

        // Everything visible happens synchronously before any await
        setSending(true);
        textarea.value = '';
        textarea.style.height = 'auto';
        appendUserMessage(text);
        const typingId = appendTyping();

        const isNewSession = !currentSessionId;
        if (isNewSession) {
            try {
                const session = await api.createSession(text);
                currentSessionId = session.id;
                prependSession(session);
            } catch (e) { /* continue without saving */ }
        }

        try {
            const data = await api.query(text);
            removeTyping(typingId);
            appendOracleMessage(data);

            if (currentSessionId) {
                api.saveMessages(currentSessionId, text, data.answer).catch(() => {});
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

        // Citation fix: only render if array is non-empty
        const citations = (data.sourceCitations || []);
        const citationsBlock = citations.length
            ? `<div class="msg-citations">
                 <div class="citations-label">Sources</div>
                 ${citations.map(c => `<div class="citation-item">${escHtml(c)}</div>`).join('')}
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

        const diagrams = (data.diagrams || []).filter(d => d && d.nodes && d.nodes.length);
        if (diagrams.length === 1) {
            div.appendChild(buildDiagram(diagrams[0]));
        } else if (diagrams.length > 1) {
            const grid = document.createElement('div');
            grid.className = 'diagrams-grid';
            diagrams.forEach(diagram => grid.appendChild(buildDiagram(diagram)));
            div.appendChild(grid);
        }

// Timeline — same
        if (data.timeline && data.timeline.length) {
            div.appendChild(buildTimeline(data.timeline));
        }

        scrollToBottom();
    }

    // ── Diagram builder ───────────────────────────────────────────────────────
    function buildDiagram(diagram) {
        const wrap = document.createElement('div');
        wrap.className = 'diagram-wrap';

        const header = document.createElement('div');
        header.className = 'diagram-header';
        header.innerHTML = `<span class="diagram-accent-bar"></span>
            <span class="diagram-title">${escHtml((diagram.title || 'DIAGRAM').toUpperCase())}</span>`;
        wrap.appendChild(header);

        const body = document.createElement('div');
        body.className = 'diagram-body';

        if (diagram.type === 'hierarchy') {
            body.appendChild(buildHierarchyDiagram(diagram));
        } else if (diagram.type === 'comparison') {
            body.appendChild(buildComparisonDiagram(diagram));
        } else {
            body.appendChild(buildProcessDiagram(diagram));
        }

        wrap.appendChild(body);
        return wrap;
    }

    function buildProcessDiagram(diagram) {
        const nodes = diagram.nodes || [];
        const edges = diagram.edges || [];
        const frag  = document.createDocumentFragment();

        // Build edge label map
        const edgeLabels = {};
        edges.forEach(e => { edgeLabels[`${e.from}→${e.to}`] = e.label || ''; });

        // Topological order
        const ordered = topoSort(nodes, edges);

        ordered.forEach((node, i) => {
            const nodeEl = document.createElement('div');
            nodeEl.className = 'process-node';
            nodeEl.innerHTML = `
                <div class="process-index">${i + 1}</div>
                <div class="process-text">
                    <div class="process-label">${escHtml(node.label)}</div>
                    ${node.description ? `<div class="process-desc">${escHtml(node.description)}</div>` : ''}
                </div>`;
            frag.appendChild(nodeEl);

            if (i < ordered.length - 1) {
                const connEl = document.createElement('div');
                connEl.className = 'process-connector';
                const edgeKey = `${ordered[i].id}→${ordered[i + 1].id}`;
                const edgeLabel = edgeLabels[edgeKey] || '';
                connEl.innerHTML = `
                    <div class="process-connector-line"></div>
                    ${edgeLabel ? `<span class="process-edge-label">${escHtml(edgeLabel)}</span>` : ''}`;
                frag.appendChild(connEl);
            }
        });

        const wrap = document.createElement('div');
        wrap.className = 'process-diagram';
        wrap.appendChild(frag);
        return wrap;
    }

    function buildHierarchyDiagram(diagram) {
        const nodes   = diagram.nodes || [];
        const edges   = diagram.edges || [];
        const nodeMap = Object.fromEntries(nodes.map(n => [n.id, n]));
        const children = Object.fromEntries(nodes.map(n => [n.id, []]));
        const hasParent = Object.fromEntries(nodes.map(n => [n.id, false]));
        edges.forEach(e => {
            if (children[e.from]) children[e.from].push(e.to);
            hasParent[e.to] = true;
        });
        const roots = nodes.filter(n => !hasParent[n.id]);

        const wrap = document.createElement('div');
        wrap.className = 'hierarchy-diagram';
        roots.forEach(r => wrap.appendChild(buildHierarchyNode(r, children, nodeMap, 0)));
        return wrap;
    }

    function buildHierarchyNode(node, children, nodeMap, depth) {
        const childIds  = children[node.id] || [];
        const hasChildren = childIds.length > 0;

        const container = document.createElement('div');
        container.className = 'hierarchy-node-wrap';

        const nodeEl = document.createElement('div');
        nodeEl.className = 'hierarchy-node' + (depth === 0 ? ' hierarchy-root' : '');
        nodeEl.style.marginLeft = (depth * 20) + 'px';
        nodeEl.innerHTML = `
            <div class="hierarchy-node-inner">
                ${hasChildren ? `<span class="hierarchy-chevron expanded">▾</span>` : `<span class="hierarchy-dot"></span>`}
                <div class="hierarchy-text">
                    <div class="hierarchy-label">${escHtml(node.label)}</div>
                    ${node.description ? `<div class="hierarchy-desc">${escHtml(node.description)}</div>` : ''}
                </div>
            </div>`;
        container.appendChild(nodeEl);

        if (hasChildren) {
            const childrenWrap = document.createElement('div');
            childrenWrap.className = 'hierarchy-children';
            childIds.forEach(id => {
                const child = nodeMap[id];
                if (child) childrenWrap.appendChild(buildHierarchyNode(child, children, nodeMap, depth + 1));
            });
            container.appendChild(childrenWrap);

            nodeEl.addEventListener('click', () => {
                const chevron = nodeEl.querySelector('.hierarchy-chevron');
                const isExpanded = childrenWrap.style.display !== 'none';
                childrenWrap.style.display = isExpanded ? 'none' : '';
                if (chevron) chevron.textContent = isExpanded ? '▸' : '▾';
                chevron?.classList.toggle('expanded', !isExpanded);
            });
        }

        return container;
    }

    function buildComparisonDiagram(diagram) {
        const nodes = diagram.nodes || [];
        const edges = diagram.edges || [];
        const left  = nodes.filter((_, i) => i % 2 === 0);
        const right = nodes.filter((_, i) => i % 2 !== 0);
        const count = Math.max(left.length, right.length);

        const wrap = document.createElement('div');
        wrap.className = 'comparison-diagram';

        if (edges.length) {
            const headers = document.createElement('div');
            headers.className = 'comparison-headers';
            headers.innerHTML = `
                <div class="comparison-header">${escHtml(edges[0].from.toUpperCase())}</div>
                <div class="comparison-header">${escHtml(edges[0].to.toUpperCase())}</div>`;
            wrap.appendChild(headers);
        }

        for (let i = 0; i < count; i++) {
            const row = document.createElement('div');
            row.className = 'comparison-row';
            const l = left[i], r = right[i];
            row.innerHTML = `
                ${l ? `<div class="comparison-cell"><div class="comparison-cell-label">${escHtml(l.label)}</div>${l.description ? `<div class="comparison-cell-desc">${escHtml(l.description)}</div>` : ''}</div>` : '<div class="comparison-cell comparison-cell-empty"></div>'}
                ${r ? `<div class="comparison-cell"><div class="comparison-cell-label">${escHtml(r.label)}</div>${r.description ? `<div class="comparison-cell-desc">${escHtml(r.description)}</div>` : ''}</div>` : '<div class="comparison-cell comparison-cell-empty"></div>'}`;
            wrap.appendChild(row);
        }

        return wrap;
    }

    // ── Timeline builder ──────────────────────────────────────────────────────
    function buildTimeline(events) {
        const wrap = document.createElement('div');
        wrap.className = 'timeline-wrap';

        const header = document.createElement('div');
        header.className = 'diagram-header';
        header.innerHTML = `<span class="diagram-accent-bar"></span><span class="diagram-title">TIMELINE</span>`;
        wrap.appendChild(header);

        const list = document.createElement('div');
        list.className = 'timeline-list';

        events.forEach((e, i) => {
            const isLast = i === events.length - 1;
            const item = document.createElement('div');
            item.className = 'timeline-item';
            item.innerHTML = `
                <div class="timeline-spine">
                    <div class="timeline-dot"></div>
                    ${!isLast ? '<div class="timeline-line"></div>' : ''}
                </div>
                <div class="timeline-content">
                    <div class="timeline-date">${escHtml(e.date?.toString() || '?')}</div>
                    <div class="timeline-event-title">${escHtml(e.title?.toString() || '')}</div>
                    ${e.description ? `<div class="timeline-event-desc">${escHtml(e.description.toString())}</div>` : ''}
                </div>`;
            list.appendChild(item);
        });

        wrap.appendChild(list);
        return wrap;
    }

    // ── Topological sort for process diagrams ─────────────────────────────────
    function topoSort(nodes, edges) {
        if (!edges.length) return nodes;
        const nodeMap   = Object.fromEntries(nodes.map(n => [n.id, n]));
        const inEdges   = Object.fromEntries(nodes.map(n => [n.id, []]));
        const outEdges  = Object.fromEntries(nodes.map(n => [n.id, []]));
        edges.forEach(e => {
            if (outEdges[e.from]) outEdges[e.from].push(e.to);
            if (inEdges[e.to])   inEdges[e.to].push(e.from);
        });
        const roots   = nodes.filter(n => !inEdges[n.id].length);
        const ordered = [];
        const seen    = new Set();
        const queue   = [...roots];
        while (queue.length) {
            const curr = queue.shift();
            if (seen.has(curr.id)) continue;
            seen.add(curr.id);
            ordered.push(curr);
            (outEdges[curr.id] || []).forEach(nextId => {
                if (!seen.has(nextId) && nodeMap[nextId]) queue.push(nodeMap[nextId]);
            });
        }
        nodes.forEach(n => { if (!seen.has(n.id)) ordered.push(n); });
        return ordered;
    }

    // ── Typing indicator ──────────────────────────────────────────────────────
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

    function clearMessages()  { messagesEl.innerHTML = ''; }

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

    function hideEmpty()   { document.getElementById('chat-empty')?.remove(); }
    function setSending(v) { sendBtn.disabled = v; textarea.disabled = v; }
    function scrollToBottom() { messagesEl.scrollTop = messagesEl.scrollHeight; }
    function escHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    await loadSessions();
    if (window.innerWidth > 768) sidebar.classList.add('open');
});