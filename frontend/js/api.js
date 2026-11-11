const API_BASE_URL = '/api/salgados-da-lucia-kojima';

class ApiError extends Error {
    constructor(message, status = 0, data = null, mensagemEspecifica = true) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
        this.mensagemEspecifica = mensagemEspecifica;
    }
}

function obterMensagemErro(erro, mensagemPadrao) {
    if (erro instanceof ApiError && !erro.mensagemEspecifica) return mensagemPadrao;
    if (erro instanceof TypeError || erro instanceof SyntaxError) return mensagemPadrao;
    return typeof erro?.message === 'string' && erro.message.trim() ? erro.message : mensagemPadrao;
}

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

    const {skipAuth, ...opcoesFetch} = opcoes;
    let response;
    try {
        response = await fetch(`${API_BASE_URL}${endpoint}`, {...opcoesFetch, headers});
    } catch (erro) {
        if (erro.name === 'AbortError') throw erro;
        throw new ApiError('Não foi possível conectar com o servidor. Verifique sua conexão e tente novamente.');
    }

    if (response.status === 401 && !opcoes.skipAuth) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
    }

    return response;
}

async function apiJson(endpoint, opcoes = {}) {
    const response = await apiFetch(endpoint, opcoes);

    let data = null;
    let respostaInvalida = false;
    if (response.status !== 204 && response.headers.get('content-length') !== '0') {
        const texto = await response.text().catch(() => {
            throw new ApiError('Não foi possível receber a resposta do servidor. Tente novamente.', response.status);
        });
        if (texto.trim()) {
            try {
                data = JSON.parse(texto);
            } catch {
                respostaInvalida = true;
            }
        }
    }

    if (!response.ok) {
        const mensagem = [data?.message, data?.mensagem].find(valor => typeof valor === 'string' && valor.trim());
        const sessaoExpirada = response.status === 401 && !opcoes.skipAuth;
        throw new ApiError(
            mensagem || (sessaoExpirada ? 'Sessão expirada. Faça login novamente.' : 'Não foi possível concluir a solicitação.'),
            response.status, data, Boolean(mensagem) || sessaoExpirada
        );
    }
    if (respostaInvalida) {
        throw new ApiError('O servidor retornou uma resposta inválida. Tente novamente.', response.status);
    }
    return { response, data };
}
