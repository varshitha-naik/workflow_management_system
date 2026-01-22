const API_BASE = '/api';

// --- Auth Helpers ---

function getToken() {
    return localStorage.getItem('accessToken');
}

function setToken(token) {
    if (!token) return;
    localStorage.setItem('accessToken', token);
}

function setRefreshToken(token) {
    if (!token) return;
    localStorage.setItem('refreshToken', token);
}

function clearAuth() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
}

function parseJwt(token) {
    try {
        if (!token) return null;
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

function getAuthContext() {
    const token = getToken();
    if (!token) return null;
    const claims = parseJwt(token);
    if (!claims) return null;

    // Check expiration
    if (claims.exp * 1000 < Date.now()) {
        clearAuth();
        return null;
    }

    return {
        username: claims.sub,
        role: claims.role,
        tenantId: claims.tenantId,
        tenantName: claims.tenantName,
        ...claims
    };
}

function hasRole(allowedRoles) {
    const ctx = getAuthContext();
    if (!ctx || !ctx.role) return false;
    if (Array.isArray(allowedRoles)) {
        return allowedRoles.includes(ctx.role);
    }
    return ctx.role === allowedRoles;
}

// Redirect if already logged in (for login page)
function checkAlreadyLoggedIn() {
    const ctx = getAuthContext();
    if (ctx) {
        window.location.href = '/dashboard';
    }
}

// Redirect if not logged in (for protected pages)
function requireAuth(allowedRoles = null) {
    const ctx = getAuthContext();
    if (!ctx) {
        clearAuth();
        return null;
    }

    if (allowedRoles && !hasRole(allowedRoles)) {
        // Role mismatch - redirect to dashboard typically, or show forbidden?
        // For now, redirect to dashboard as a safe fallback
        window.location.href = '/dashboard';
        return null;
    }

    // Update UI with user info if element exists
    const userDisplay = document.getElementById('navUserDisplay');
    if (userDisplay) {
        userDisplay.textContent = `${ctx.username} (${ctx.role}) ${ctx.tenantName ? '@ ' + ctx.tenantName : ''}`;
    }

    return ctx;
}

// --- API Client ---

async function apiCall(endpoint, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method,
        headers
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(API_BASE + endpoint, options);

        // Handle 401 Unauthorized or 403 Forbidden
        if (response.status === 401) {
            // If this is a login attempt, don't auto-redirect, let the error bubble up
            if (!endpoint.includes('/auth/login')) {
                clearAuth(); // Strict Logout
                throw new Error('Session expired');
            }
        }

        if (response.status === 403) {
            throw new Error('Access Denied: You do not have permission for this action.');
        }

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'API request failed');
        }

        if (response.status === 204) return null;

        const text = await response.text();
        return text ? JSON.parse(text) : null;

    } catch (error) {
        throw error;
    }
}

// --- UI Helpers ---

function setLoading(btnId, isLoading, text = 'Loading...') {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    if (isLoading) {
        btn.dataset.originalText = btn.textContent;
        // Add spinner html if you want, or just text
        btn.innerHTML = `<span style="display:inline-block;animation:spin 1s linear infinite;margin-right:5px">⟳</span> ${text}`;
        btn.disabled = true;
    } else {
        btn.innerHTML = btn.dataset.originalText || 'Submit';
        btn.disabled = false;
    }
}

// CSS for spinner - added dynamically if not present
if (!document.getElementById('spinner-style')) {
    const style = document.createElement('style');
    style.id = 'spinner-style';
    style.innerHTML = `@keyframes spin { 100% { transform: rotate(360deg); } }`;
    document.head.appendChild(style);
}

function showMessage(id, message, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;

    // Icon based on type
    const icon = type === 'error'
        ? '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>'
        : '<svg width="20" height="20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>';

    el.innerHTML = `${icon} <span>${message}</span>`;
    el.className = type === 'error' ? 'alert alert-error' : 'alert alert-success';
    el.classList.remove('hidden');
}

function hideMessage(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('hidden');
}

function renderTable(containerId, columns, data, actions = []) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!data || data.length === 0) {
        container.innerHTML = '<div class="text-center p-4 text-muted">No data available</div>';
        return;
    }

    let html = '<div class="table-container"><table><thead><tr>';
    columns.forEach(col => html += `<th>${col.label}</th>`);
    if (actions.length > 0) html += '<th>Actions</th>';
    html += '</tr></thead><tbody>';

    data.forEach(row => {
        html += '<tr>';
        columns.forEach(col => {
            let val = row[col.key];
            if (col.format) val = col.format(val, row);
            html += `<td>${val === undefined || val === null ? '-' : val}</td>`;
        });

        if (actions.length > 0) {
            html += '<td class="flex gap-2">';
            actions.forEach(action => {
                if (action.condition && !action.condition(row)) return;
                html += `<button class="btn btn-sm ${action.class || ''}" onclick="${action.handler}(${row.id})">${action.label}</button>`;
            });
            html += '</td>';
        }
        html += '</tr>';
    });

    html += '</tbody></table></div>';
    container.innerHTML = html;
}
