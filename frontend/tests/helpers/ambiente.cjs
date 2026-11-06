// DOM mínimo para testar contratos e eventos sem instalar dependências no frontend.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const frontend = path.resolve(__dirname, '../..');

function resposta(status, data) {
    return new Response(JSON.stringify(data), {status, headers: {'Content-Type': 'application/json'}});
}

function ambiente(modulo) {
    let agora = 0;
    let numeroTimer = 0;
    const timers = new Map();
    const elementos = new Map();
    const respostas = [];
    const consultas = [];
    const storage = new Map();
    let documento;

    class Elemento {
        constructor(id = '') {
            this.id = id;
            this.parent = null;
            this.children = [];
            this.partes = new Map();
            this.eventos = {};
            this.dataset = {};
            this.value = '';
            this.textContent = '';
            this.atributos = {};
            this.conectado = true;
            this.disabled = false;
            this.inert = false;
            this.resets = 0;
            this.classes = new Set();
            this.style = {setProperty() {}};
            this.classList = {
                toggle: (nome, ativo = !this.classes.has(nome)) => ativo ? this.classes.add(nome) : this.classes.delete(nome),
                add: nome => this.classes.add(nome), remove: nome => this.classes.delete(nome), contains: nome => this.classes.has(nome)
            };
        }
        get isConnected() { return this.conectado && (!this.parent || this.parent.isConnected); }
        set isConnected(valor) { this.conectado = valor; }
        set innerHTML(html) { this.html = html; this.children = []; this.partes.clear(); }
        get innerHTML() { return this.html || ''; }
        setAttribute(nome, valor) { this.atributos[nome] = valor; }
        getAttribute(nome) { return this.atributos[nome]; }
        removeAttribute(nome) { delete this.atributos[nome]; }
        querySelector(seletor) {
            if (!this.partes.has(seletor)) {
                const parte = new Elemento(seletor);
                parte.parent = this;
                this.partes.set(seletor, parte);
            }
            return this.partes.get(seletor);
        }
        querySelectorAll(seletor) { return seletor === 'button' ? [this.querySelector('button[type=submit]')] : []; }
        append(filho) { filho.parent = this; this.children.push(filho); }
        appendChild(filho) { this.append(filho); }
        remove() { this.conectado = false; if (this.parent) this.parent.children = this.parent.children.filter(item => item !== this); }
        contains(filho) { return Boolean(filho && (filho === this || this.contains(filho.parent))); }
        addEventListener(tipo, callback) { (this.eventos[tipo] ||= []).push(callback); }
        emitir(tipo, extra = {}) {
            const evento = {target: this, defaultPrevented: false, cancelBubble: false, preventDefault() { this.defaultPrevented = true; }, stopPropagation() { this.cancelBubble = true; }, ...extra};
            for (let atual = this; atual; atual = evento.cancelBubble ? null : atual.parent) {
                for (const callback of atual.eventos[tipo] || []) callback(evento);
            }
            return evento;
        }
        dispatchEvent(evento) { return this.emitir(evento.type, evento); }
        focus() {
            const anterior = documento.activeElement;
            documento.activeElement = this;
            anterior?.emitir('focusout');
            this.emitir('focusin');
        }
        matches() { return false; }
        closest() { return null; }
        getClientRects() { return [{}]; }
        reset() { this.resets++; }
        scrollIntoView() {}
        getBoundingClientRect() { return {height: 200}; }
        get nextElementSibling() { return this.parent?.children[this.parent.children.indexOf(this) + 1]; }
        get previousElementSibling() { return this.parent?.children[this.parent.children.indexOf(this) - 1]; }
    }

    function elemento(id) {
        if (!elementos.has(id)) elementos.set(id, new Elemento(id));
        return elementos.get(id);
    }
    documento = new Elemento('document');
    documento.body = elemento('body');
    documento.getElementById = elemento;
    documento.createElement = tag => new Elemento(tag);
    documento.querySelector = seletor => elemento(seletor);
    documento.querySelectorAll = () => [];
    documento.activeElement = elemento('origem');
    elemento('lucia-notificacoes').parent = documento;
    const contexto = vm.createContext({
        document: documento, console: {error() {}}, URLSearchParams,
        CustomEvent: class { constructor(type, opcoes) { this.type = type; Object.assign(this, opcoes); } },
        scrollTo() {},
        localStorage: {getItem: chave => storage.get(chave), setItem: (chave, valor) => storage.set(chave, valor), removeItem: chave => storage.delete(chave)},
        fetch: async (url, opcoes) => {
            consultas.push({url, opcoes});
            if (!respostas.length) throw new Error(`Resposta não preparada: ${url}`);
            const valor = respostas.shift();
            if (valor instanceof Error) throw valor;
            return await valor;
        },
        performance: {now: () => agora},
        setTimeout: (callback, delay) => { timers.set(++numeroTimer, {callback, prazo: agora + delay}); return numeroTimer; },
        clearTimeout: id => timers.delete(id), queueMicrotask
    });
    contexto.window = contexto;
    const executar = codigo => vm.runInContext(codigo, contexto);
    const carregar = arquivo => executar(fs.readFileSync(path.join(frontend, arquivo), 'utf8'));
    carregar('js/api.js');
    carregar('js/notificacoes.js');
    if (modulo) {
        carregar('js/listagem.js');
        carregar(`${modulo}/js/${modulo}.js`);
    }
    function avancar(ms) {
        const fim = agora + ms;
        while (true) {
            const proximo = [...timers].sort((a, b) => a[1].prazo - b[1].prazo).find(([, timer]) => timer.prazo <= fim);
            if (!proximo) break;
            agora = proximo[1].prazo;
            timers.delete(proximo[0]);
            proximo[1].callback();
        }
        agora = fim;
    }
    return {contexto, documento, elemento, respostas, consultas, storage, executar, carregar, avancar,
        cartoes: () => elemento('lucia-notificacoes').children};
}

module.exports = {ambiente, resposta};
