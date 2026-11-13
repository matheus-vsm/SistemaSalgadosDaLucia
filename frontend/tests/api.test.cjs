const test = require('node:test');
const assert = require('node:assert/strict');
const {ambiente, resposta} = require('./helpers/ambiente.cjs');

test('apiJson mantém o contrato de sucesso e não envia skipAuth ao fetch', async () => {
    const a = ambiente();
    a.storage.set('accessToken', 'token-de-teste');
    a.respostas.push(resposta(200, {id: 1}));
    const resultado = await a.contexto.apiJson('/clientes?page=1&ativo=true');
    assert.equal(resultado.response.status, 200);
    assert.equal(resultado.data.id, 1);
    assert.equal(a.consultas[0].url, '/api/salgados-da-lucia-kojima/clientes?page=1&ativo=true');
    assert.equal(a.consultas[0].opcoes.headers.Authorization, 'Bearer token-de-teste');
    assert.equal('skipAuth' in a.consultas[0].opcoes, false);
});

for (const [nome, body, esperado] of [
    ['message tem prioridade', {message: 'Cliente está inativo.', mensagem: 'Outra mensagem'}, 'Cliente está inativo.'],
    ['mensagem mantém compatibilidade', {mensagem: 'Pedido já finalizado.'}, 'Pedido já finalizado.'],
    ['message vazio usa mensagem', {message: '  ', mensagem: 'Senha atual incorreta!'}, 'Senha atual incorreta!'],
    ['validações não são truncadas', {message: 'nome: Nome é obrigatório!, telefone: Telefone é obrigatório!'}, 'nome: Nome é obrigatório!, telefone: Telefone é obrigatório!']
]) {
    test(`apiJson: ${nome}`, async () => {
        const a = ambiente();
        a.respostas.push(resposta(400, body));
        await assert.rejects(a.contexto.apiJson('/teste'), erro => {
            assert.equal(erro.name, 'ApiError');
            assert.equal(erro.status, 400);
            assert.equal(erro.message, esperado);
            assert.equal(erro.data.message, body.message);
            return true;
        });
        assert.equal(a.cartoes().length, 0, 'a camada de API não exibe cartões');
    });
}

for (const body of ['', '<html>erro do proxy</html>', '{invalido', JSON.stringify({message: {}, mensagem: ' '})]) {
    test(`erro HTTP sem mensagem válida usa contexto: ${body}`, async () => {
        const a = ambiente();
        a.respostas.push(new Response(body, {status: 500}));
        const erro = await a.contexto.apiJson('/clientes').catch(erro => erro);
        assert.equal(erro.status, 500);
        assert.equal(a.contexto.obterMensagemErro(erro, 'Não foi possível salvar o cliente'), 'Não foi possível salvar o cliente');
    });
}

test('401 autenticado limpa tokens e mantém message do backend', async () => {
    const a = ambiente();
    a.storage.set('accessToken', 'acesso');
    a.storage.set('refreshToken', 'renovacao');
    a.respostas.push(resposta(401, {message: 'Token JWT inválido ou expirado!'}));
    await assert.rejects(a.contexto.apiJson('/clientes'), {message: 'Token JWT inválido ou expirado!'});
    assert.equal(a.storage.size, 0);
});

test('401 vazio usa sessão expirada; login preserva credenciais inválidas e não envia token', async () => {
    const a = ambiente();
    a.respostas.push(new Response(null, {status: 401}));
    await assert.rejects(a.contexto.apiJson('/clientes'), {message: 'Sessão expirada. Faça login novamente.'});
    a.storage.set('accessToken', 'anterior');
    a.respostas.push(resposta(401, {message: 'Usuário ou senha inválidos.'}));
    await assert.rejects(a.contexto.apiJson('/autenticacao/login', {skipAuth: true}), {message: 'Usuário ou senha inválidos.'});
    assert.equal(a.storage.get('accessToken'), 'anterior');
    assert.equal(a.consultas[1].opcoes.headers.Authorization, undefined);
});

test('204 e 200 vazios são sucessos; JSON inválido não expõe o parser', async () => {
    const a = ambiente();
    for (const response of [new Response(null, {status: 204}), new Response('', {status: 200}), new Response(null, {headers: {'content-length': '0'}})]) {
        a.respostas.push(response);
        assert.equal((await a.contexto.apiJson('/teste')).data, null);
    }
    a.respostas.push(new Response('<html>OK</html>'));
    await assert.rejects(a.contexto.apiJson('/teste'), {message: 'O servidor retornou uma resposta inválida. Tente novamente.'});
});

test('falhas de rede e leitura têm mensagem em português; cancelamento é preservado', async () => {
    const a = ambiente();
    a.respostas.push(new TypeError('Failed to fetch'));
    await assert.rejects(a.contexto.apiJson('/teste'), /Não foi possível conectar com o servidor/);
    a.respostas.push({
        status: 200, headers: new Headers(), text: async () => {
            throw new Error('stream error');
        }
    });
    await assert.rejects(a.contexto.apiJson('/teste'), /Não foi possível receber a resposta/);
    const cancelamento = new Error('cancelado');
    cancelamento.name = 'AbortError';
    a.respostas.push(cancelamento);
    await assert.rejects(a.contexto.apiJson('/teste'), erro => erro === cancelamento);
});
