const API_BASE_URL = 'http://localhost:8080/api/salgados-da-lucia-kojima';

function getAccessToken() {
    return localStorage.getItem('accessToken');
}

async function apiFetch(endpoint, opcoes = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(opcoes.headers || {})
    };

    const token = getAccessToken();
    if (token && !opcoes.skipAuth) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...opcoes,
        headers
    });

    if (response.status === 401 && !opcoes.skipAuth) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        throw new Error('Sessão expirada. Faça login novamente.');
    }

    return response;
}

async function apiJson(endpoint, opcoes = {}) {
    const response = await apiFetch(endpoint, opcoes);

    if (response.status === 204 || response.headers.get('content-length') === '0') {
        return { response, data: null };
    }

    const data = await response.json();
    return { response, data };
}
