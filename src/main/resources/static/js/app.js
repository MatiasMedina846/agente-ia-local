const API_URL = '/api/chat';

const messagesEl = document.getElementById('messages');
const inputEl = document.getElementById('input');
const sendBtn = document.getElementById('send');
const typingEl = document.getElementById('typing');

function appendMessage(role, text) {
    const wrapper = document.createElement('div');
    wrapper.className = 'message ' + role;
    const bubble = document.createElement('div');
    bubble.className = 'bubble';
    bubble.textContent = text;
    wrapper.appendChild(bubble);
    messagesEl.appendChild(wrapper);
    scrollToBottom();
}

function scrollToBottom() {
    messagesEl.scrollTop = messagesEl.scrollHeight;
}

function setTyping(on) {
    typingEl.hidden = !on;
    sendBtn.disabled = on;
    scrollToBottom();
}

async function sendMessage(prompt) {
    setTyping(true);
    try {
        const res = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ prompt })
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.message || 'Error al procesar el mensaje');
        }
        appendMessage('assistant', data.response);
    } catch (err) {
        appendMessage('assistant', 'Error: ' + err.message);
    } finally {
        setTyping(false);
    }
}

async function getHistory() {
    try {
        const res = await fetch(API_URL + '/history');
        if (!res.ok) {
            return;
        }
        const history = await res.json();
        history.forEach(item => {
            if (item.prompt) {
                appendMessage('user', item.prompt);
            }
            if (item.response) {
                appendMessage('assistant', item.response);
            }
        });
        scrollToBottom();
    } catch (err) {
        console.error('No se pudo cargar el historial', err);
    }
}

function handleSend() {
    const text = inputEl.value.trim();
    if (!text) {
        return;
    }
    appendMessage('user', text);
    inputEl.value = '';
    inputEl.focus();
    sendMessage(text);
}

sendBtn.addEventListener('click', handleSend);
inputEl.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        handleSend();
    }
});

getHistory();
