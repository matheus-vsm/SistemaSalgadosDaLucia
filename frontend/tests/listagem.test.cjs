const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const frontend = path.resolve(__dirname, '..');
const modulos = [
    ['cliente', 'cliente', 'lista', 'listarClientes', 'criarCardHTML', 'templateCardCliente'],
    ['salgado', 'salgado', 'lista-salgados', 'listarSalgados', 'criarCardSalgadoHTML', 'templateCardSalgado'],
    ['pedidos', 'pedidos', 'lista-pedidos', 'listarPedidos', 'criarCardPedido', 'templateCardPedido'],
    ['compras', 'compras', 'lista-compras', 'listarCompras', 'criarCardCompra', 'templateCardCompra'],
    ['usuarios', 'usuarios', 'lista-usuarios', 'listarUsuarios', 'criarCardUsuario', 'templateCardUsuario']
];

function ambiente([pasta, arquivo, idLista, listar, criarCard, template]) {
    const elementos = new Map();
    function elemento(id) {
        if (!elementos.has(id)) elementos.set(id, {
            dataset: {}, style: {setProperty(nome, valor) { this[nome] = valor; }}, children: [{}], isConnected: true, inert: false,
            alturaNatural: 210,
            alturasCards: [160, 210],
            innerHTML: 'cards existentes', textContent: 'Página 1 de 3', scrollTop: 120,
            setAttribute(nome, valor) { this[nome] = valor; },
            getAttribute(nome) { return this[nome]; },
            getBoundingClientRect() { return {height: this.alturaNatural}; },
            addEventListener() {},
            querySelector() { return elemento(`${id}-filho`); },
            querySelectorAll() {
                return this.alturasCards.map(altura => ({
                    getBoundingClientRect: () => ({height: Math.max(altura, parseFloat(this.style['--altura-card']) || 0)})
                }));
            },
            classList: {toggle() {}}
        });
        return elementos.get(id);
    }
    const consultas = [];
    const observadores = [];
    const contexto = vm.createContext({
        document: {getElementById: elemento, addEventListener() {}},
        URLSearchParams,
        ResizeObserver: class {
            constructor(callback) { observadores.push(callback); }
            observe() {}
            disconnect() {}
        },
        console: {error() {}},
        apiJson(url) {
            return new Promise((resolve, reject) => consultas.push({url, resolve, reject}));
        }
    });
    const html = fs.readFileSync(path.join(frontend, pasta, 'html', `${arquivo}.html`), 'utf8');
    if (html.includes(`id="${idLista}" data-altura-cards="uniforme"`)) {
        elemento(idLista).dataset.alturaCards = 'uniforme';
    }
    for (const arquivoFonte of ['js/listagem.js', `${pasta}/js/${arquivo}.js`]) {
        vm.runInContext(fs.readFileSync(path.join(frontend, arquivoFonte), 'utf8'), contexto);
    }
    vm.runInContext(`${template} = 'template em cache';`, contexto);
    contexto[criarCard] = item => `card-${item.id}`;
    return {lista: elemento(idLista), elemento, consultas, observadores, listar: pagina => contexto[listar](pagina)};
}

function pagina(id, numero = 1) {
    return {response: {ok: true}, data: {content: [{id, ativo: true}], page: {number: numero, totalPages: 3}}};
}

for (const modulo of modulos) {
    test(`${modulo[0]}: altura segue os cards da tela e se adapta à largura`, async () => {
        const a = ambiente(modulo);
        const altura = 180 + modulos.indexOf(modulo) * 30;
        a.lista.alturaNatural = altura;
        a.lista.alturasCards = [altura - 20, altura];
        const propriedade = a.lista.dataset.alturaCards ? '--altura-card' : 'height';
        const primeira = a.listar(0);
        a.consultas[0].resolve(pagina(1, 0));
        await primeira;
        assert.equal(a.lista.style[propriedade], `${altura}px`);

        a.lista.alturaNatural = altura - 50;
        a.lista.alturasCards = [altura - 80, altura - 50];
        const segunda = a.listar(1);
        a.consultas[1].resolve(pagina(2));
        await segunda;
        assert.equal(a.lista.style[propriedade], `${altura}px`);

        a.observadores[0]([{contentRect: {width: 1000}}]);
        a.lista.alturaNatural = altura + 100;
        a.lista.alturasCards = [altura, altura + 100];
        a.observadores[0]([{contentRect: {width: 600}}]);
        assert.equal(a.lista.style[propriedade], `${altura + 100}px`);
    });

    if (['salgado', 'pedidos', 'compras'].includes(modulo[0])) {
        test(`${modulo[0]}: cresce para o maior card sem prender a altura da lista`, async () => {
            const a = ambiente(modulo);
            for (const [indice, alturas] of [[160, 210, 180, 170], [180, 380, 150, 200], [150, 180, 200, 190]].entries()) {
                a.lista.alturasCards = alturas;
                const requisicao = a.listar(indice);
                a.consultas[indice].resolve(pagina(indice + 1, indice));
                await requisicao;
                const esperado = indice === 0 ? 210 : 380;
                assert.equal(a.lista.style['--altura-card'], `${esperado}px`);
                assert.equal(a.lista.style.height, '');
                for (const card of a.lista.querySelectorAll()) {
                    assert.equal(card.getBoundingClientRect().height, esperado);
                }
            }
        });
    }

    test(`${modulo[0]}: mantém cards e indicador de página enquanto consulta a API`, async () => {
        const a = ambiente(modulo);
        const requisicao = a.listar(1);
        assert.equal(a.lista.innerHTML, 'cards existentes');
        assert.equal(a.lista['aria-busy'], 'true');
        assert.equal(a.lista.inert, true);
        for (const id of ['compra-pagina-atual', 'usuarios-pagina']) {
            assert.equal(a.elemento(id).textContent, 'Página 1 de 3');
        }
        assert.match(a.consultas[0].url, /page=1/);
        a.consultas[0].resolve(pagina(10));
        await requisicao;
        assert.equal(a.lista.innerHTML, 'card-10');
        assert.equal(a.lista['aria-busy'], 'false');
        assert.equal(a.lista.inert, false);
        assert.equal(a.lista.scrollTop, 0);
    });

    test(`${modulo[0]}: resposta antiga não substitui a página mais recente`, async () => {
        const a = ambiente(modulo);
        const antiga = a.listar(1);
        const recente = a.listar(2);
        a.consultas[0].resolve(pagina(10));
        await antiga;
        assert.equal(a.lista.innerHTML, 'cards existentes');
        assert.equal(a.lista['aria-busy'], 'true');
        a.consultas[1].resolve(pagina(20, 2));
        await recente;
        assert.equal(a.lista.innerHTML, 'card-20');
        assert.equal(a.lista['aria-busy'], 'false');
    });

    test(`${modulo[0]}: falha libera a listagem e permite nova consulta`, async () => {
        const a = ambiente(modulo);
        const falha = a.listar(1);
        a.consultas[0].reject(new Error('Falha de conexão'));
        await falha;
        assert.equal(a.lista['aria-busy'], 'false');
        assert.equal(a.lista.inert, false);
        const tentativa = a.listar(1);
        a.consultas[1].resolve(pagina(30));
        await tentativa;
        assert.equal(a.lista.innerHTML, 'card-30');
    });

    test(`${modulo[0]}: ignora resposta após sair da tela`, async () => {
        const a = ambiente(modulo);
        const requisicao = a.listar(1);
        a.lista.isConnected = false;
        a.consultas[0].resolve(pagina(40));
        await requisicao;
        assert.equal(a.lista.innerHTML, 'cards existentes');
    });
}
