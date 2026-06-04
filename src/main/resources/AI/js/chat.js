document.addEventListener('DOMContentLoaded', () => {
    if (!api.isLoggedIn()) { window.location.href = 'login.html'; return; }

    const user = api.getUser();
    const messagesEl = document.getElementById('chat-messages');
    const textarea = document.getElementById('chat-input');
    const sendBtn = document.getElementById('send-btn');
    const userNameEl = document.getElementById('user-name');
    const logoutBtn = document.getElementById('logout-btn');

    if (userNameEl && user) userNameEl.textContent = user.firstName;

    logoutBtn?.addEventListener('click', () => {
        api.clearSession();
        window.location.href = 'index.html';
    });

    // Auto-resize textarea
    textarea.addEventListener('input', () => {
        textarea.style.height = 'auto';
        textarea.style.height = Math.min(textarea.scrollHeight, 140) + 'px';
    });

    // Send on Enter (Shift+Enter for newline)
    textarea.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
    });

    sendBtn.addEventListener('click', sendMessage);

    async function sendMessage() {
        const text = textarea.value.trim();
        if (!text || sendBtn.disabled) return;

        appendUserMessage(text);
        textarea.value = '';
        textarea.style.height = 'auto';
        setSending(true);

        const typingId = appendTyping();

        try {
            const data = await api.query(text);
            removeTyping(typingId);
            appendOracleMessage(data);
        } catch (err) {
            removeTyping(typingId);
            appendOracleMessage({ answer: err.message, sourceCitations: [], civilizationMatched: null });
        } finally {
            setSending(false);
        }
    }

    function appendUserMessage(text) {
        hideEmpty();
        const div = document.createElement('div');
        div.className = 'msg msg-user';
        div.innerHTML = `<div class="msg-user-bubble">${escHtml(text)}</div>`;
        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function appendOracleMessage(data) {
        const div = document.createElement('div');
        div.className = 'msg msg-oracle';

        const civTag = data.civilizationMatched
            ? `<span class="civ-tag">${escHtml(data.civilizationMatched)}</span>` : '';

        const citations = (data.sourceCitations || []).map(c =>
            `<div class="citation-item">${escHtml(c)}</div>`).join('');

        const citationsBlock = citations
            ? `<div class="msg-citations"><div class="citations-label">SOURCES</div>${citations}</div>` : '';

        div.innerHTML = `
      <div class="msg-oracle-meta">
        <span class="oracle-dot"></span>
        <span class="oracle-label">ORACLE</span>
        ${civTag}
      </div>
      <div class="msg-oracle-content">${marked.parse(data.answer)}</div>
      ${citationsBlock}
    `;
        messagesEl.appendChild(div);
        scrollToBottom();
    }

    function appendTyping() {
        const id = 'typing-' + Date.now();
        const div = document.createElement('div');
        div.className = 'msg msg-oracle';
        div.id = id;
        div.innerHTML = `
      <div class="msg-oracle-meta">
        <span class="oracle-dot"></span>
        <span class="oracle-label">ORACLE</span>
      </div>
      <div class="typing-indicator">
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
      </div>`;
        messagesEl.appendChild(div);
        scrollToBottom();
        return id;
    }

    function removeTyping(id) {
        document.getElementById(id)?.remove();
    }

    function hideEmpty() {
        document.getElementById('chat-empty')?.remove();
    }

    function setSending(val) {
        sendBtn.disabled = val;
        textarea.disabled = val;
    }

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function escHtml(str) {
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }
});