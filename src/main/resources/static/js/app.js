const API_BASE = '/api';

// --- Auth Helpers ---

function getToken() {
    return localStorage.getItem('accessToken');
}

function setToken(token) {
    localStorage.setItem('accessToken', token);
}

function setRefreshToken(token) {
    localStorage.setItem('refreshToken', token);
}

function clearAuth() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
}

function parseJwt(token) {
    try {
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

// Redirect if already logged in (for login page)
function checkAlreadyLoggedIn() {
    const token = getToken();
    if (token) {
        // Optional: verify token expiration
        const claims = parseJwt(token);
        if (claims && claims.exp * 1000 > Date.now()) {
            window.location.href = '/dashboard';
        } else {
            clearAuth(); // Expired
        }
    }
}

// Redirect if not logged in (for protected pages)
function requireAuth() {
    const token = getToken();
    if (!token) {
        window.location.href = '/login';
        return null;
    }
    // Simple expiration check
    const claims = parseJwt(token);
    if (!claims || claims.exp * 1000 < Date.now()) {
        clearAuth();
        window.location.href = '/login';
        return null;
    }
    return claims;
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

        // Handle 401 Unauthorized
        if (response.status === 401) {
            // Try refresh logic if we implemented it, otherwise logout
            clearAuth();
            window.location.href = '/login';
            throw new Error('Session expired');
        }

        if (!response.ok) {
            // Try to parse error JSON
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'API request failed');
        }

        // Return void for empty responses (204 usually)
        if (response.status === 204) return null;

        // Some endpoints might return empty body even with 200
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
        btn.textContent = text;
        btn.disabled = true;
    } else {
        btn.textContent = btn.dataset.originalText || 'Submit';
        btn.disabled = false;
    }
}

function showMessage(id, message, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = message;
    el.className = type === 'error' ? 'alert alert-error' : 'alert alert-success';
    el.classList.remove('hidden');
}

function hideMessage(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('hidden');
}
