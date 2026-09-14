const BASE = '/api';

function headers(tenantId) {
    const h = { 'Content-Type': 'application/json' };
    if (tenantId) h['X-Tenant-Id'] = tenantId;
    return h;
}

// Navigation
document.querySelectorAll('.sidebar nav a').forEach(a => {
    a.addEventListener('click', e => {
        e.preventDefault();
        document.querySelectorAll('.sidebar nav a').forEach(x => x.classList.remove('active'));
        document.querySelectorAll('.section').forEach(x => x.classList.remove('active'));
        a.classList.add('active');
        document.getElementById('sec-' + a.dataset.section).classList.add('active');
    });
});

// Dashboard
async function loadDashboard() {
    try {
        const res = await fetch(BASE + '/admin/dashboard?days=7', { headers: headers(getSelectedTenantId()) });
        if (!res.ok) return;
        const d = await res.json();
        document.getElementById('stat-messages').textContent = d.totalMessages;
        document.getElementById('stat-fallbacks').textContent = d.fallbackCount;
        document.getElementById('stat-handoffs').textContent = d.handoffCount;
        document.getElementById('stat-active').textContent = d.activeConversations;
        const bd = document.getElementById('event-breakdown');
        if (d.eventBreakdown && Object.keys(d.eventBreakdown).length > 0) {
            bd.innerHTML = '<h3>Desglose por evento</h3>' +
                Object.entries(d.eventBreakdown).map(([k,v]) =>
                    `<div class="breakdown-item"><span>${k}</span><span>${v}</span></div>`
                ).join('');
        } else {
            bd.innerHTML = '';
        }
    } catch (e) {
        console.error('Dashboard error', e);
    }
}

function getSelectedTenantId() {
    return localStorage.getItem('selectedTenantId') || '';
}

// Tenants
let allTenants = [];

async function loadTenants() {
    try {
        const res = await fetch(BASE + '/tenants');
        allTenants = await res.json();
        const list = document.getElementById('tenant-list');
        if (allTenants.length === 0) {
            list.innerHTML = '<div class="empty">No hay negocios configurados</div>';
            return;
        }
        list.innerHTML = allTenants.map(t => `
            <div class="card">
                <div class="card-info">
                    <h4>${t.name} <span class="badge">${t.slug}</span></h4>
                    <p>${t.businessName || '-'} | ${t.toneOfVoice} | ${t.active ? 'Activo' : 'Inactivo'}</p>
                </div>
                <div class="card-actions">
                    <button class="btn-primary btn-sm" onclick="editTenant(${t.id})">Editar</button>
                </div>
            </div>
        `).join('');

        // Update config select
        const sel = document.getElementById('config-tenant-select');
        sel.innerHTML = '<option value="">-- Seleccionar --</option>' +
            allTenants.map(t => `<option value="${t.id}">${t.name}</option>`).join('');
    } catch (e) {
        console.error('Tenants error', e);
    }
}

document.getElementById('btn-new-tenant').addEventListener('click', () => {
    document.getElementById('tenant-form').hidden = false;
    document.getElementById('form-title').textContent = 'Nuevo Negocio';
    document.getElementById('tenant-id').value = '';
    document.getElementById('tenant-name').value = '';
    document.getElementById('tenant-slug').value = '';
    document.getElementById('tenant-business').value = '';
    document.getElementById('tenant-tone').value = 'FRIENDLY';
    document.getElementById('tenant-welcome').value = '';
    document.getElementById('tenant-prompt').value = '';
    document.getElementById('tenant-hours').value = '';
});

document.getElementById('btn-cancel-tenant').addEventListener('click', () => {
    document.getElementById('tenant-form').hidden = true;
});

document.getElementById('btn-save-tenant').addEventListener('click', async () => {
    const id = document.getElementById('tenant-id').value;
    const data = {
        name: document.getElementById('tenant-name').value,
        slug: document.getElementById('tenant-slug').value,
        businessName: document.getElementById('tenant-business').value,
        toneOfVoice: document.getElementById('tenant-tone').value,
        welcomeMessage: document.getElementById('tenant-welcome').value,
        systemPrompt: document.getElementById('tenant-prompt').value,
        businessHours: document.getElementById('tenant-hours').value,
        active: true
    };
    const url = id ? BASE + '/tenants/' + id : BASE + '/tenants';
    const method = id ? 'PUT' : 'POST';
    await fetch(url, { method, headers: headers(), body: JSON.stringify(data) });
    document.getElementById('tenant-form').hidden = true;
    loadTenants();
});

window.editTenant = function(id) {
    const t = allTenants.find(x => x.id === id);
    if (!t) return;
    document.getElementById('tenant-form').hidden = false;
    document.getElementById('form-title').textContent = 'Editar Negocio';
    document.getElementById('tenant-id').value = t.id;
    document.getElementById('tenant-name').value = t.name || '';
    document.getElementById('tenant-slug').value = t.slug || '';
    document.getElementById('tenant-business').value = t.businessName || '';
    document.getElementById('tenant-tone').value = t.toneOfVoice || 'FRIENDLY';
    document.getElementById('tenant-welcome').value = t.welcomeMessage || '';
    document.getElementById('tenant-prompt').value = t.systemPrompt || '';
    document.getElementById('tenant-hours').value = t.businessHours || '';
};

// Handoffs
async function loadHandoffs() {
    try {
        const res = await fetch(BASE + '/admin/handoffs', { headers: headers(getSelectedTenantId()) });
        if (!res.ok) return;
        const list = await res.json();
        const el = document.getElementById('handoff-list');
        if (list.length === 0) {
            el.innerHTML = '<div class="empty">No hay handoffs pendientes</div>';
            return;
        }
        el.innerHTML = list.map(h => `
            <div class="card">
                <div class="card-info">
                    <h4>Conversación #${h.conversationId} <span class="badge badge-red">${h.status}</span></h4>
                    <p><strong>Razón:</strong> ${h.reason}</p>
                    <p style="font-size:12px;color:#888;margin-top:4px;">${h.createdAt || ''}</p>
                </div>
                <div class="card-actions">
                    <button class="btn-primary btn-sm" onclick="resolveHandoff(${h.id})">Resolver</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Handoffs error', e);
    }
}

window.resolveHandoff = async function(id) {
    await fetch(BASE + '/admin/handoffs/' + id + '/resolve', { method: 'POST', headers: headers(getSelectedTenantId()) });
    loadHandoffs();
    loadDashboard();
};

// Config
document.getElementById('config-tenant-select').addEventListener('change', async function() {
    const id = this.value;
    if (!id) { document.getElementById('config-fields').hidden = true; return; }
    document.getElementById('config-fields').hidden = false;
    try {
        const res = await fetch(BASE + '/tenants/' + id);
        const t = await res.json();
        document.getElementById('config-offline').value = t.offlineMessage || '';
        document.getElementById('config-primary').value = t.primaryColor || '#075e54';
        document.getElementById('config-secondary').value = t.secondaryColor || '#128c7e';
        document.getElementById('config-logo').value = t.logoUrl || '';
    } catch (e) { console.error(e); }
});

document.getElementById('btn-save-config').addEventListener('click', async () => {
    const id = document.getElementById('config-tenant-select').value;
    if (!id) return;
    const res = await fetch(BASE + '/tenants/' + id);
    const t = await res.json();
    t.offlineMessage = document.getElementById('config-offline').value;
    t.primaryColor = document.getElementById('config-primary').value;
    t.secondaryColor = document.getElementById('config-secondary').value;
    t.logoUrl = document.getElementById('config-logo').value;
    await fetch(BASE + '/tenants/' + id, { method: 'PUT', headers: headers(), body: JSON.stringify(t) });
    alert('Configuración guardada');
});

// Init
loadDashboard();
loadTenants();
loadHandoffs();
