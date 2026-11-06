function ajustarAlturaListagem(lista) {
    if (lista.dataset.recalcularAltura === 'true') {
        lista.style.height = '';
        delete lista.dataset.recalcularAltura;
    }
    if (lista.style.height || !lista.querySelector('.card, .card-pedido, .card-compra')) return;

    // Reserve only the space occupied by this screen's first set of cards.
    // Subsequent pages scroll inside that space instead of moving pagination.
    lista.style.height = `${Math.ceil(lista.getBoundingClientRect().height)}px`;

    if (lista.dataset.observandoLargura || typeof ResizeObserver === 'undefined') return;
    lista.dataset.observandoLargura = 'true';
    let larguraAnterior;
    const observador = new ResizeObserver(([entrada]) => {
        if (!lista.isConnected) {
            observador.disconnect();
            return;
        }
        const largura = entrada.contentRect.width;
        if (larguraAnterior !== undefined && largura !== larguraAnterior) {
            lista.dataset.recalcularAltura = 'true';
            if (lista.getAttribute('aria-busy') !== 'true') ajustarAlturaListagem(lista);
        }
        larguraAnterior = largura;
    });
    observador.observe(lista);
}

function iniciarCarregamentoListagem(lista) {
    const consulta = Number(lista.dataset.consultaListagem || 0) + 1;
    lista.dataset.consultaListagem = String(consulta);
    lista.setAttribute('aria-busy', 'true');
    lista.inert = true;
    if (!lista.children.length) {
        lista.innerHTML = '<div class="estado">Carregando registros...</div>';
    }

    const atual = () => lista.isConnected && Number(lista.dataset.consultaListagem) === consulta;
    return {
        atual,
        finalizar() {
            if (!atual()) return;
            lista.setAttribute('aria-busy', 'false');
            lista.inert = false;
            ajustarAlturaListagem(lista);
            lista.scrollTop = 0;
        }
    };
}
