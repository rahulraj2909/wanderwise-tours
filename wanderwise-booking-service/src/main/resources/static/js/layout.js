async function renderHeader() {
    const el = document.getElementById('site-header');
    if (!el) return;

    let user = null;
    try {
        user = await api('/auth/me');
    } catch (_) {}

    const authBlock = user
        ? `<span class="user-pill">Hi, ${escapeHtml(user.name.split(' ')[0])}</span>
           <a href="/bookings.html">My Bookings</a>
           <button type="button" class="btn-link" id="btn-logout">Logout</button>`
        : `<a href="/login.html">Login</a>
           <a href="/register.html" class="btn btn-sm btn-primary">Sign up</a>`;

    el.innerHTML = `
        <div class="container header-inner">
            <a href="/" class="logo"><span class="logo-icon">✈</span> Wander<strong>Wise</strong></a>
            <nav class="nav">${authBlock}</nav>
        </div>`;

    document.getElementById('btn-logout')?.addEventListener('click', async () => {
        await api('/auth/logout', { method: 'POST' });
        window.location.href = '/';
    });
}

function renderFooter() {
    const el = document.getElementById('site-footer');
    if (!el) return;
    el.innerHTML = `
        <div class="container footer-inner">
            <p class="footer-brand">© ${new Date().getFullYear()} WanderWise</p>
            <p class="footer-tagline">Tours, attractions &amp; experiences — book with confidence.</p>
            <nav class="footer-links">
                <a href="/">Explore</a>
                <a href="/login.html">Login</a>
                <a href="/bookings.html">My Bookings</a>
            </nav>
        </div>`;
}

document.addEventListener('DOMContentLoaded', () => {
    renderHeader();
    renderFooter();
});
