const API = '/api/v1';
const TOUR_IMAGE_FALLBACK = 'https://picsum.photos/seed/wanderwise-default/800/600';

function tourImageSrc(imageUrl) {
    return imageUrl || TOUR_IMAGE_FALLBACK;
}

function onTourImgError(img) {
    if (!img) return;
    img.onerror = null;
    img.src = TOUR_IMAGE_FALLBACK;
    img.parentElement?.classList.add('no-img');
}

/** Support both PageImpl and PagedModel (`page` sub-object) shapes. */
function normalizePage(data) {
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
        return data;
    }
    if (!Array.isArray(data.content)) {
        return data;
    }
    if (data.page) {
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
    return data;
}

async function api(path, options = {}) {
    const res = await fetch(API + path, {
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options,
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) {
        let msg = json.message || json.error || 'Request failed';
        if (res.status === 502 || res.status === 503) {
            msg = 'Catalog service unavailable — start wanderwise-catalog-ingestion-service on port 8081, then refresh.';
        }
        const err = new Error(msg);
        err.status = res.status;
        throw err;
    }
    const data = json.data !== undefined ? json.data : json;
    return normalizePage(data);
}

function qs(name) {
    return new URLSearchParams(window.location.search).get(name);
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

function tourIcon(type) {
    return { ADVENTURE: '🏜️', ATTRACTION: '🗼', ACTIVITY: '🤿', TOUR: '🚌', DAY_TRIP: '🌅' }[type] || '🎫';
}

function requireLogin(redirectUrl) {
    window.location.href = '/login.html?redirect=' + encodeURIComponent(redirectUrl || window.location.pathname + window.location.search);
}

const CHECKOUT_TIMER_SEC = 60;
const CHECKOUT_DEADLINE_KEY = 'checkoutDeadlineMs';

function ensureCheckoutDeadline() {
    let deadline = parseInt(sessionStorage.getItem(CHECKOUT_DEADLINE_KEY) || '0', 10);
    if (!deadline || deadline <= Date.now()) {
        deadline = Date.now() + CHECKOUT_TIMER_SEC * 1000;
        sessionStorage.setItem(CHECKOUT_DEADLINE_KEY, String(deadline));
    }
    return deadline;
}

function clearCheckoutDeadline() {
    sessionStorage.removeItem(CHECKOUT_DEADLINE_KEY);
}

function checkoutSecondsLeft() {
    const deadline = parseInt(sessionStorage.getItem(CHECKOUT_DEADLINE_KEY) || '0', 10);
    if (!deadline) return CHECKOUT_TIMER_SEC;
    return Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
}

/** 60s hold timer for review → checkout → payment (shared across those pages). */
function mountCheckoutTimer(bannerId, options = {}) {
    ensureCheckoutDeadline();
    const banner = document.getElementById(bannerId);
    if (!banner) return null;

    let expired = false;
    const onExpired = () => {
        if (expired) return;
        expired = true;
        banner.classList.add('timer-expired');
        banner.textContent = 'Booking time expired — please start again from the tour page.';
        options.onExpired?.();
    };

    const tick = () => {
        const left = checkoutSecondsLeft();
        if (left > 0) {
            banner.classList.remove('timer-expired');
            banner.classList.toggle('timer-warning', left <= 15);
            banner.textContent = `Complete your booking within ${left}s`;
        } else {
            onExpired();
        }
    };
    tick();
    return setInterval(tick, 1000);
}
