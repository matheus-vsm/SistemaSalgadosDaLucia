const test = require('node:test');
const assert = require('node:assert/strict');
const {ambiente} = require('./helpers/ambiente.cjs');
const seletorFechar = '.lucia-notificacao-fechar';

test('saída aguarda a animação antes de remover o cartão e liberar a fila', async () => {
    const a = ambiente();
    for (let i = 0; i < 4; i++) a.contexto.mostrarNotificacao({tipo: 'erro', titulo: `Erro ${i}`});
    const cartao = a.cartoes()[0];
    let terminar;
    let animacoes = 0;
    cartao.animate = (quadros, opcoes) => {
        animacoes++;
        assert.equal(opcoes.duration, 220);
        assert.equal(quadros[1].opacity, 0);
        return {finished: new Promise(resolve => { terminar = resolve; })};
    };
    cartao.querySelector(seletorFechar).emitir('click');
    cartao.querySelector(seletorFechar).emitir('click');
    assert.equal(animacoes, 1);
    assert.equal(a.cartoes()[0], cartao);
    assert.equal(a.cartoes().length, 3);
    terminar();
    await Promise.resolve();
    assert.equal(cartao.isConnected, false);
    assert.equal(a.cartoes().length, 3);
    assert.equal(a.cartoes()[2].querySelector('.lucia-notificacao-titulo').textContent, 'Erro 3');
});

test('sucesso fecha após quatro segundos de exposição', () => {
    const a = ambiente();
    a.contexto.mostrarNotificacao({tipo: 'sucesso', titulo: 'Salvo'});
    a.avancar(3999);
    assert.equal(a.cartoes().length, 1);
    a.avancar(1);
    assert.equal(a.cartoes().length, 0);
    assert.equal(a.elemento('lucia-anuncios-sucesso').children.length, 0);
});

test('pausa por mouse e foco combinados preserva o tempo restante', async () => {
    const a = ambiente();
    a.contexto.mostrarNotificacao({tipo: 'sucesso', titulo: 'Salvo'});
    const cartao = a.cartoes()[0];
    a.avancar(1000);
    cartao.emitir('mouseenter');
    a.avancar(6000);
    cartao.querySelector(seletorFechar).focus();
    cartao.emitir('mouseleave');
    a.avancar(6000);
    assert.equal(a.cartoes().length, 1);
    a.elemento('origem').focus();
    await Promise.resolve();
    a.avancar(2999);
    assert.equal(a.cartoes().length, 1);
    a.avancar(1);
    assert.equal(a.cartoes().length, 0);
});

for (const tipo of ['erro', 'aviso']) {
    test(`${tipo} fecha após dez segundos de exposição`, () => {
        const a = ambiente();
        a.contexto.mostrarNotificacao({tipo, titulo: 'Atenção'});
        a.avancar(9999);
        assert.equal(a.cartoes().length, 1);
        a.avancar(1);
        assert.equal(a.cartoes().length, 0);
    });

    test(`${tipo} pausa durante leitura com mouse ou teclado`, async () => {
        const a = ambiente();
        a.contexto.mostrarNotificacao({tipo, titulo: 'Atenção'});
        const cartao = a.cartoes()[0];
        a.avancar(2000);
        cartao.emitir('mouseenter');
        a.avancar(11000);
        cartao.querySelector(seletorFechar).focus();
        cartao.emitir('mouseleave');
        a.avancar(11000);
        assert.equal(a.cartoes().length, 1);
        a.elemento('origem').focus();
        await Promise.resolve();
        a.avancar(7999);
        assert.equal(a.cartoes().length, 1);
        a.avancar(1);
        assert.equal(a.cartoes().length, 0);
    });
}

test('a fila mantém a ordem e só conta tempo ao exibir', () => {
    const a = ambiente();
    for (const [i, tipo] of ['erro', 'aviso', 'erro', 'sucesso', 'aviso'].entries()) {
        a.contexto.mostrarNotificacao({tipo, titulo: `Mensagem ${i + 1}`});
    }
    const titulo = cartao => cartao.querySelector('.lucia-notificacao-titulo').textContent;
    a.avancar(1000);
    assert.deepEqual(a.cartoes().map(titulo), ['Mensagem 1', 'Mensagem 2', 'Mensagem 3']);
    a.cartoes()[0].querySelector(seletorFechar).emitir('click');
    assert.deepEqual(a.cartoes().map(titulo), ['Mensagem 2', 'Mensagem 3', 'Mensagem 4']);
    a.avancar(3999);
    assert.equal(a.cartoes()[2].querySelector('.lucia-notificacao-titulo').textContent, 'Mensagem 4');
    a.avancar(1);
    assert.deepEqual(a.cartoes().map(titulo), ['Mensagem 2', 'Mensagem 3', 'Mensagem 5']);
});

test('Escape no cartão fecha só a notificação e restaura o foco', () => {
    const a = ambiente();
    let modalFechado = false;
    a.documento.addEventListener('keydown', () => { modalFechado = true; });
    a.contexto.mostrarNotificacao({tipo: 'erro', titulo: 'Falha'});
    const fechar = a.cartoes()[0].querySelector(seletorFechar);
    fechar.focus();
    const evento = fechar.emitir('keydown', {key: 'Escape'});
    assert.equal(evento.defaultPrevented, true);
    assert.equal(modalFechado, false);
    assert.equal(a.cartoes().length, 0);
    assert.equal(a.documento.activeElement, a.elemento('origem'));
});

test('fechamento transfere foco ao cartão vizinho sem removê-lo', () => {
    const a = ambiente();
    a.contexto.mostrarNotificacao({tipo: 'erro', titulo: 'Primeira'});
    a.contexto.mostrarNotificacao({tipo: 'aviso', titulo: 'Segunda'});
    const proximo = a.cartoes()[1].querySelector(seletorFechar);
    a.cartoes()[0].querySelector(seletorFechar).focus();
    a.cartoes()[0].querySelector(seletorFechar).emitir('click');
    assert.equal(a.documento.activeElement, proximo);
    assert.equal(a.cartoes().length, 1);
});

test('mensagens longas e HTML são exibidos integralmente como texto', () => {
    const a = ambiente();
    const mensagem = '<img src=x onerror=alert(1)>' + ' validação'.repeat(150);
    a.contexto.mostrarNotificacao({tipo: 'erro', titulo: '<b>Erro</b>', mensagem});
    const cartao = a.cartoes()[0];
    assert.equal(cartao.querySelector('.lucia-notificacao-mensagem').textContent, mensagem);
    assert.equal(cartao.querySelector('.lucia-notificacao-titulo').textContent, '<b>Erro</b>');
    assert.ok(!cartao.innerHTML.includes(mensagem));
    assert.equal(a.elemento('lucia-anuncios-urgentes').children.length, 1);
});
