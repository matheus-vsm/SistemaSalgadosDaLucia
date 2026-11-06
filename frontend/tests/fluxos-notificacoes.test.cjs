const test = require('node:test');
const assert = require('node:assert/strict');
const {ambiente, resposta} = require('./helpers/ambiente.cjs');

const mensagem = cartao => cartao.querySelector('.lucia-notificacao-mensagem').textContent;
const titulo = cartao => cartao.querySelector('.lucia-notificacao-titulo').textContent;

test('imagem do pedido acompanha a seleção mantendo a moldura visível', () => {
    const a = ambiente('pedidos');
    a.executar("salgadosPedido = [{id:1, nome:'Coxinha', precoCentoCongelado:100}]; renderizarItensPedido = () => {};");
    const select = a.elemento('salgado-pedido');
    const moldura = a.elemento('preview-salgado-pedido');
    const preview = moldura.querySelector('img');
    const mudar = valor => {
        select.value = valor;
        a.elemento('conteudo-dinamico').emitir('change', {target: select});
    };
    mudar('');
    assert.equal(preview.classList.contains('hidden'), true);
    assert.equal(moldura.classList.contains('hidden'), false);
    mudar('1');
    assert.equal(preview.classList.contains('hidden'), false);
    a.contexto.adicionarItemPedido();
    assert.equal(preview.classList.contains('hidden'), false, 'validação não deve esconder o salgado selecionado');
    a.elemento('tipo-preco-pedido').value = 'CONGELADO';
    a.elemento('quantidade-pedido').value = '10';
    a.contexto.adicionarItemPedido();
    assert.equal(a.executar('itensNovoPedido.length'), 1);
    assert.equal(select.value, '');
    assert.equal(preview.classList.contains('hidden'), true);
    mudar('1');
    a.elemento('form-pedido').reset = () => { select.value = ''; };
    a.contexto.limparFormularioPedido();
    assert.equal(preview.classList.contains('hidden'), true);
    assert.equal(moldura.classList.contains('hidden'), false);
});

for (const [modulo, salvar, formulario, campo, inicializar] of [
    ['cliente', 'salvarCliente', 'form-cliente', 'nome', "templateCardCliente = 'cache';"],
    ['salgado', 'salvarSalgado', 'form-salgado', 'nome-salgado', "templateCardSalgado = 'cache';"],
    ['compras', 'salvarCompra', 'form-compra', 'observacao-compra', "itensNovaCompra = [{nome:'Óleo', quantidade:1, valorUnitario:10}]; limparFormularioCompra = () => document.getElementById('form-compra').reset();"],
    ['pedidos', 'salvarPedido', 'form-pedido', 'cliente-pedido', "itensNovoPedido = [{salgadoId:1, quantidade:10, tipoPreco:'CONGELADO'}]; limparFormularioPedido = () => document.getElementById('form-pedido').reset();"]
]) {
    test(`${modulo}: message chega ao cartão e falha mantém formulário e libera envio`, async () => {
        const a = ambiente(modulo);
        a.executar(inicializar);
        a.elemento(campo).value = 'Dado preenchido';
        const texto = 'Operação não permitida para este cadastro.';
        a.respostas.push(resposta(400, {message: texto}));
        await a.contexto[salvar]();
        assert.equal(a.cartoes().length, 1);
        assert.equal(mensagem(a.cartoes()[0]), texto);
        assert.ok(a.cartoes()[0].className.includes('lucia-notificacao-erro'));
        assert.equal(a.elemento(campo).value, 'Dado preenchido');
        assert.equal(a.elemento(formulario).resets, 0);
        assert.equal(a.elemento(formulario).querySelector('button[type=submit]').disabled, false);
        assert.equal(a.elemento(formulario).inert, false);
    });

    test(`${modulo}: gravação concluída mantém sucesso quando a atualização da lista falha`, async () => {
        const a = ambiente(modulo);
        a.executar(inicializar);
        a.respostas.push(new Response(null, {status: 204}), resposta(503, {message: 'Listagem temporariamente indisponível.'}));
        await a.contexto[salvar]();
        // Pedidos inicia a atualização sem aguardar seu retorno.
        await new Promise(setImmediate);
        assert.equal(a.cartoes().length, 2);
        assert.ok(a.cartoes()[0].className.includes('lucia-notificacao-sucesso'));
        assert.match(titulo(a.cartoes()[0]), /com sucesso/);
        assert.ok(a.cartoes()[1].className.includes('lucia-notificacao-erro'));
        assert.equal(mensagem(a.cartoes()[1]), 'Listagem temporariamente indisponível.');
        assert.equal(a.elemento(formulario).resets, 1);
    });
}

for (const [modulo, invocar, preparar, texto] of [
    ['cliente', 'alterarStatusCliente(1, false)', '', 'Cliente possui pedidos em andamento.'],
    ['salgado', 'alterarStatusSalgado(1, false)', '', 'Salgado já está inativo.'],
    ['salgado', 'atualizarEstoqueSalgado()', "document.getElementById('ajuste-estoque').value = '-10';", 'A quantidade no estoque não pode ser negativa.'],
    ['pedidos', "alterarStatusPedido(1, 'FINALIZADO')", '', 'O pedido já está com o status FINALIZADO'],
    ['usuarios', 'cadastrarUsuario()', "document.getElementById('form-usuario').reportValidity = () => true; for (const id of ['usuario-nome','usuario-username','usuario-senha']) document.getElementById(id).value = 'teste';", 'Usuário já cadastrado.'],
    ['usuarios', 'desativarUsuario(1)', '', 'Usuário não encontrado com id: 1'],
    ['usuarios', 'alterarSenhaUsuario({})', "usuarioLogado = () => true; document.getElementById('form-senha-usuario').reportValidity = () => true; document.getElementById('usuario-nova-senha').value = 'nova'; document.getElementById('usuario-confirmar-senha').value = 'nova';", 'Senha atual incorreta!']
]) {
    test(`${invocar}: exceção da API é apresentada uma única vez`, async () => {
        const a = ambiente(modulo);
        a.executar(preparar);
        a.respostas.push(resposta(400, {message: texto}));
        await a.executar(invocar);
        assert.equal(a.cartoes().length, 1);
        assert.equal(mensagem(a.cartoes()[0]), texto);
    });
}

test('listagem mostra a mesma mensagem do backend no cartão e no estado de erro', async () => {
    const a = ambiente('cliente');
    a.executar("templateCardCliente = 'cache';");
    a.respostas.push(resposta(403, {message: 'Acesso não permitido.'}));
    await a.contexto.listarClientes();
    assert.equal(a.elemento('lista').querySelector('span').textContent, mensagem(a.cartoes()[0]));
});

test('inicialização de pedidos propaga message das consultas auxiliares uma única vez', async () => {
    const a = ambiente('pedidos');
    a.respostas.push(resposta(403, {message: 'Sem permissão para consultar clientes.'}), resposta(200, {}), resposta(200, {}), new Response('card'), new Response('item'));
    await a.contexto.carregarDadosIniciaisPedido();
    assert.equal(a.cartoes().length, 1);
    assert.equal(mensagem(a.cartoes()[0]), 'Sem permissão para consultar clientes.');
});

test('login inválido mantém mensagem do backend; login válido exibe sucesso e tokens', async () => {
    const a = ambiente();
    a.carregar('app.js');
    a.documento.eventos.DOMContentLoaded[0]();
    const enviar = a.elemento('form-login').eventos.submit[0];
    a.respostas.push(resposta(401, {message: 'Usuário ou senha inválidos.'}));
    await enviar({preventDefault() {}});
    assert.equal(mensagem(a.cartoes()[0]), 'Usuário ou senha inválidos.');
    assert.equal(a.storage.size, 0);
    a.respostas.push(resposta(200, {tokenAcesso: 'acesso', refreshToken: 'renovacao'}), new Response('<h1>Início</h1>'));
    await enviar({preventDefault() {}});
    assert.ok(a.cartoes()[1].className.includes('lucia-notificacao-sucesso'));
    assert.equal(a.storage.get('accessToken'), 'acesso');
    assert.equal(a.storage.get('refreshToken'), 'renovacao');
    assert.equal(a.elemento('tela-login').classList.contains('hidden'), true);
    assert.equal(a.cartoes().length, 2);
});
