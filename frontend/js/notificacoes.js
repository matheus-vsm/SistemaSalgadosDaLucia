// Componente único, independente dos módulos carregados dinamicamente.
(() => {
    const icones = {
        sucesso: '<svg viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></svg>',
        erro: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg>',
        aviso: '<svg viewBox="0 0 24 24"><path d="m10.3 4-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3l-8-14a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4m0 4h.01"/></svg>'
    };
    const regiao = document.getElementById('lucia-notificacoes');
    const cabecalho = document.querySelector('#painel-sistema > header');
    const conteudo = document.getElementById('conteudo-dinamico');
    const fila = [];
    const ativas = new Set();
    const LIMITE = 3;

    function atualizarPosicao() {
        const altura = cabecalho?.getBoundingClientRect().height || 0;
        regiao.style.setProperty('--lucia-cabecalho-altura', `${altura}px`);
        const linha = conteudo?.querySelector(':scope > hr');
        const primeiro = regiao.children[0];
        const limite = linha?.getBoundingClientRect().top;
        const alturaCartao = primeiro?.getBoundingClientRect().height;
        // Centraliza o primeiro cartão no espaço real; mensagens maiores ficam abaixo do header.
        const deslocamento = altura > 0 && Number.isFinite(limite) && alturaCartao
            ? Math.max(4, (limite - altura - alturaCartao) / 2 - 4)
            : 12;
        regiao.style.setProperty('--lucia-notificacao-deslocamento', `${deslocamento}px`);
    }

    if (cabecalho && typeof ResizeObserver !== 'undefined') {
        // Acompanha mudanças de altura, inclusive ao exibir o painel após o login.
        const observador = new ResizeObserver(atualizarPosicao);
        observador.observe(cabecalho);
        observador.observe(regiao);
        if (conteudo) observador.observe(conteudo);
    }
    conteudo?.addEventListener('pagina:carregada', atualizarPosicao);
    window.addEventListener?.('scroll', () => { if (ativas.size) atualizarPosicao(); }, {passive: true});

    function mostrarNotificacao({ tipo, titulo, mensagem = '' }) {
        if (!Object.hasOwn(icones, tipo)) throw new TypeError('Tipo de notificação inválido.');
        atualizarPosicao();
        fila.push({ tipo, titulo, mensagem });
        processarFila();
    }

    function processarFila() {
        while (ativas.size < LIMITE && fila.length) criarNotificacao(fila.shift());
    }

    function criarNotificacao({ tipo, titulo, mensagem }) {
        const cartao = document.createElement('article');
        cartao.className = `lucia-notificacao lucia-notificacao-${tipo}`;
        cartao.setAttribute('aria-label', `${tipo}: ${titulo}`);
        // Apenas o SVG estático é inserido como HTML. Textos são sempre conteúdo literal.
        cartao.innerHTML = `<span class="lucia-notificacao-icone" aria-hidden="true">${icones[tipo]}</span><div class="lucia-notificacao-conteudo"><strong class="lucia-notificacao-titulo"></strong><p class="lucia-notificacao-mensagem"></p></div><button type="button" class="lucia-notificacao-fechar"><span aria-hidden="true">×</span></button>`;
        cartao.querySelector('.lucia-notificacao-titulo').textContent = titulo;
        cartao.querySelector('.lucia-notificacao-mensagem').textContent = mensagem;
        cartao.querySelector('.lucia-notificacao-mensagem').hidden = !mensagem;
        const fechar = cartao.querySelector('.lucia-notificacao-fechar');
        fechar.setAttribute('aria-label', `Fechar notificação: ${titulo}`);
        const anuncio = document.createElement('p');
        anuncio.textContent = `${tipo}: ${titulo}${mensagem ? `. ${mensagem}` : ''}`;

        const duracao = tipo === 'sucesso' ? 4000 : 10000;
        cartao.style.setProperty('--lucia-notificacao-duracao', `${duracao}ms`);
        let restante = duracao;
        let inicio = 0;
        let temporizador = null;
        let ponteiroDentro = false;
        let fechada = false;
        const origem = document.activeElement;
        const registro = { cartao, remover };

        function remover() {
            if (fechada) return;
            fechada = true;
            clearTimeout(temporizador);
            const tinhaFoco = cartao.contains(document.activeElement);
            const vizinho = cartao.nextElementSibling || cartao.previousElementSibling;
            function concluirRemocao() {
                cartao.remove();
                anuncio.remove();
                ativas.delete(registro);
                processarFila();
                atualizarPosicao();
                if (tinhaFoco) {
                    if (vizinho?.isConnected) vizinho.querySelector('.lucia-notificacao-fechar').focus();
                    else if (origem?.isConnected && !origem.closest('[inert], .hidden') && origem.getClientRects().length) origem.focus();
                }
            }
            const movimentoReduzido = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
            if (cartao.animate && !movimentoReduzido) {
                cartao.style.pointerEvents = 'none';
                const animacao = cartao.animate([
                    {opacity: 1, transform: 'translateY(0)'},
                    {opacity: 0, transform: 'translateY(-8px)'}
                ], {duration: 220, easing: 'ease-in', fill: 'forwards'});
                animacao.finished.then(concluirRemocao, concluirRemocao);
            } else {
                concluirRemocao();
            }
        }

        function atualizarPausa() {
            if (fechada) return;
            const pausada = ponteiroDentro || cartao.contains(document.activeElement);
            cartao.classList.toggle('lucia-notificacao-pausada', pausada);
            if (pausada && temporizador !== null) {
                clearTimeout(temporizador);
                temporizador = null;
                restante = Math.max(0, restante - (performance.now() - inicio));
            } else if (!pausada && temporizador === null) {
                inicio = performance.now();
                temporizador = setTimeout(() => remover(), restante);
            }
        }

        fechar.addEventListener('click', () => remover());
        cartao.addEventListener('mouseenter', () => { ponteiroDentro = true; atualizarPausa(); });
        cartao.addEventListener('mouseleave', () => { ponteiroDentro = false; atualizarPausa(); });
        cartao.addEventListener('focusin', atualizarPausa);
        cartao.addEventListener('focusout', () => queueMicrotask(atualizarPausa));
        cartao.addEventListener('keydown', evento => {
            if (evento.key === 'Escape') {
                evento.preventDefault();
                evento.stopPropagation();
                remover();
            }
        });
        const progresso = document.createElement('span');
        progresso.className = 'lucia-notificacao-progresso';
        progresso.setAttribute('aria-hidden', 'true');
        cartao.append(progresso);
        ativas.add(registro);
        regiao.append(cartao);
        atualizarPosicao();
        document.getElementById(tipo === 'sucesso' ? 'lucia-anuncios-sucesso' : 'lucia-anuncios-urgentes').append(anuncio);
        ponteiroDentro = cartao.matches(':hover');
        atualizarPausa();
    }
    window.mostrarNotificacao = mostrarNotificacao;
})();

/** Exibe uma falha e devolve a mesma mensagem para o estado de erro da tela. */
function notificarErro(titulo, erro) {
    const mensagem = obterMensagemErro(erro, titulo);
    mostrarNotificacao({ tipo: 'erro', titulo, mensagem });
    return mensagem;
}
