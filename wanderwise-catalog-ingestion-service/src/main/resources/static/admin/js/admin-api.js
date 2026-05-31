const ADMIN_API = '/api/v1';

function getAdminSecret() {
    return sessionStorage.getItem('adminSecret') || '';
}

function requireAdminLogin() {
    if (!getAdminSecret()) {
        window.location.href = '/admin/login.html?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
        return false;
    }
    return true;
}

function normalizePage(data) {
    if (!data || !Array.isArray(data.content) || !data.page) {
        return data;
    }
    const p = data.page;
    return {
        content: data.content,
        totalElements: p.totalElements,
        totalPages: p.totalPages,
        number: p.number,
        size: p.size,
        first: p.number === 0,
        last: p.number >= p.totalPages - 1,
    };
}

async function adminApi(path, options = {}) {
    const res = await fetch(ADMIN_API + path, {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'X-Admin-Secret': getAdminSecret(),
            ...(options.headers || {}),
        },
        ...options,
    });
    const json = await res.json().catch(() => ({}));
    if (res.status === 401 || res.status === 400 || (json.message && String(json.message).toLowerCase().includes('admin secret'))) {
        sessionStorage.removeItem('adminSecret');
        requireAdminLogin();
        throw new Error('Please sign in to the admin portal');
    }
    if (!res.ok) {
        let msg = json.message || json.error || 'Request failed';
        if (res.status === 502 || res.status === 503) {
            msg = 'Catalog service error — check the server on port 8081 is running.';
        }
        throw new Error(msg);
    }
    const data = json.data !== undefined ? json.data : json;
    return normalizePage(data);
}

function escapeHtml(s) {
    if (!s) return '';
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}

function formatPrice(n) {
    return Number(n).toLocaleString('en-IN', { maximumFractionDigits: 0 });
}

function renderAdminNav(active) {
    const el = document.getElementById('admin-nav');
    if (!el) return;
    const links = [
        { href: '/admin/index.html', label: 'Dashboard', key: 'dashboard' },
        { href: '/admin/products.html', label: 'Products', key: 'products' },
        { href: '/admin/ingestion.html', label: 'Vendor ingestion', key: 'ingestion' },
        { href: 'http://localhost:8080/', label: 'Customer site ↗', key: 'customer', external: true },
    ];
    el.innerHTML = links.map(l =>
        `<a href="${l.href}" class="${active === l.key ? 'active' : ''}"${l.external ? ' target="_blank"' : ''}>${l.label}</a>`
    ).join('') + `<button type="button" class="btn btn-secondary btn-sm" id="btn-logout">Logout</button>`;
    document.getElementById('btn-logout')?.addEventListener('click', () => {
        sessionStorage.removeItem('adminSecret');
        window.location.href = '/admin/login.html';
    });
}
