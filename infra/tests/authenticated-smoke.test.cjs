const test = require('node:test');
const assert = require('node:assert/strict');
const {randomUUID} = require('node:crypto');
const base = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:15500';
const prefix = '/api/salgados-da-lucia-kojima';

// Executar somente em banco descartavel: este teste cria registros de negocio.
test('login, renovacao, operacoes principais e permissoes no MySQL real', {
    skip: process.env.SMOKE_ALLOW_WRITES !== 'true'
}, async () => {
    assert.ok(process.env.SMOKE_ADMIN_USERNAME && process.env.SMOKE_ADMIN_PASSWORD);
    let token;
    async function api(endpoint, method = 'GET', body, expected = 200, access = token) {
        const response = await fetch(base + prefix + endpoint, {
            method, headers: {'Content-Type': 'application/json', ...(access ? {Authorization: `Bearer ${access}`} : {})},
            body: body === undefined ? undefined : JSON.stringify(body)
        });
        const text = await response.text();
        assert.equal(response.status, expected, `${method} ${endpoint}: ${text.slice(0, 300)}`);
        return text ? JSON.parse(text) : null;
    }
    const credentials = {username: process.env.SMOKE_ADMIN_USERNAME, senha: process.env.SMOKE_ADMIN_PASSWORD};
    const tokens = await api('/autenticacao/login', 'POST', credentials);
    token = tokens.tokenAcesso;
    assert.ok(token && tokens.refreshToken);
    const refreshed = await api('/autenticacao/atualizar-token', 'POST', {refreshToken: tokens.refreshToken});
    assert.ok(refreshed.tokenAcesso);
    token = refreshed.tokenAcesso;
    const users = await api('/usuarios?ativo=true&size=100');
    const admin = users.content.find(user => user.username === credentials.username);
    assert.ok(admin);
    const suffix = randomUUID().slice(0, 8);
    const address = {logradouro: 'Rua Teste', numero: '1', cep: '01001000', bairro: 'Centro', cidade: 'Sao Paulo', uf: 'SP'};
    const customerBody = {nome: `Smoke ${suffix}`, telefone: '11999999999', endereco: address};
    const customer = await api('/clientes', 'POST', customerBody, 201);
    await api(`/clientes/${customer.id}`, 'PUT', {...customerBody, nome: `Smoke editado ${suffix}`});
    const productBody = {nome: `Smoke ${suffix}`, descricao: 'Integracao', categoria: 'FRITO', precoCentoCongelado: 100, precoCentoProcessado: 150};
    const product = await api('/salgados', 'POST', productBody, 201);
    await api(`/salgados/${product.id}`, 'PUT', {...productBody, precoCentoCongelado: 110});
    await api(`/estoque/${product.id}`, 'PATCH', {quantidade: 100}, 204);
    await api(`/estoque/${product.id}`);
    const date = new Date().toISOString().slice(0, 10);
    const orderBody = {clienteId: customer.id, enderecoEntrega: address, itens: [{salgadoId: product.id, quantidade: 10, tipoPreco: 'CONGELADO'}],
        dataPedido: date, dataEntrega: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 19),
        tipoEntrega: 'RETIRADA', formaPagamento: 'PIX', usuarioResponsavelId: admin.id, frete: 1};
    const order = await api('/pedidos', 'POST', orderBody, 201);
    await api(`/pedidos/${order.id}`, 'PUT', {...orderBody, frete: 2});
    const purchaseBody = {itens: [{nome: 'Farinha smoke', quantidade: 2, valorUnitario: 10}], dataCompra: date, observacao: suffix};
    const purchase = await api('/compras', 'POST', purchaseBody, 201);
    await api(`/compras/${purchase.id}`, 'PUT', {...purchaseBody, observacao: 'Editado ' + suffix});
    for (const endpoint of ['/clientes?ativo=true', '/salgados?ativo=true', '/estoque', '/pedidos', '/compras', '/usuarios?ativo=true']) await api(endpoint);
    const employee = {nome: `Smoke funcionario ${suffix}`, username: `smoke-${suffix}`, senha: randomUUID(), perfilUsuarioNome: 'FUNCIONARIO'};
    await api('/usuarios/cadastrar', 'POST', employee, 201);
    const employeeTokens = await api('/autenticacao/login', 'POST', {username: employee.username, senha: employee.senha});
    await api('/salgados', 'POST', productBody, 403, employeeTokens.tokenAcesso);
});
