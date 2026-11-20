const test = require('node:test');
const assert = require('node:assert/strict');
const {ambiente} = require('./helpers/ambiente.cjs');

for (const tipo of ['CONGELADO', 'PROCESSADO']) {
    for (const [cento, quantidade, unitario, subtotal] of [
        [60.49, 3, 0.6049, 1.81],
        [60.50, 3, 0.605, 1.82],
        [60.51, 3, 0.6051, 1.82],
        [55.55, 150, 0.5555, 83.33],
        [60.50, 50, 0.605, 30.25],
        [60.50, 100, 0.605, 60.50],
        [3.35, 30, 0.0335, 1.01],
        [0.01, 49, 0.0001, 0.00],
        [0.01, 50, 0.0001, 0.01]
    ]) {
        test(`pedido ${tipo}: cento ${cento}, quantidade ${quantidade}, subtotal ${subtotal}`, () => {
            const a = ambiente('pedidos');
            const salgado = {
                precoCentoCongelado: tipo === 'CONGELADO' ? cento : 135,
                precoCentoProcessado: tipo === 'PROCESSADO' ? cento : 246
            };
            const precoUnitario = a.contexto.precoItemPedido(salgado, tipo);
            assert.ok(Math.abs(precoUnitario - unitario) < 1e-12);
            assert.equal(a.contexto.subtotalItemPedido({precoUnitario, quantidade}), subtotal);
        });
    }
}

for (const [tipoEntrega, total] of [['RETIRADA', 0.02], ['ENTREGA', 2.37]]) {
    test(`pedido ${tipoEntrega}: soma subtotais arredondados individualmente e aplica o frete uma vez`, () => {
        const a = ambiente('pedidos');
        a.executar('itensNovoPedido = [{precoUnitario: 0.0001, quantidade: 50}, {precoUnitario: 0.0001, quantidade: 50}];');
        a.elemento('tipo-entrega-pedido').value = tipoEntrega;
        a.elemento('frete-pedido').value = '2.35';
        a.contexto.atualizarTotalPedido();
        assert.equal(a.elemento('valor-total-pedido').textContent, a.executar(`moedaPedido(${total})`));
    });
}

test('pedido: total de item carregado para edição preserva as casas do unitário', () => {
    const a = ambiente('pedidos');
    a.executar('itensNovoPedido = [{precoUnitario: 0.6050, quantidade: 50}];');
    a.elemento('tipo-entrega-pedido').value = 'ENTREGA';
    a.elemento('frete-pedido').value = '10.00';
    a.contexto.atualizarTotalPedido();
    assert.equal(a.elemento('valor-total-pedido').textContent, a.executar('moedaPedido(40.25)'));
});

for (const quantidade of ['1.5', '-1', '0']) {
    test(`pedido: quantidade inválida ${quantidade} não chega ao cálculo`, () => {
        const a = ambiente('pedidos');
        a.executar('salgadosPedido = [{id: 1, nome: "Coxinha", precoCentoCongelado: 60.50}];');
        a.elemento('salgado-pedido').value = '1';
        a.elemento('tipo-preco-pedido').value = 'CONGELADO';
        a.elemento('quantidade-pedido').value = quantidade;
        assert.doesNotThrow(() => a.contexto.adicionarItemPedido());
        assert.equal(a.executar('itensNovoPedido.length'), 0);
    });
}
