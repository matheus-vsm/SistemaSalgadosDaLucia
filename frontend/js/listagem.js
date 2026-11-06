function ajustarAlturaListagem(lista) {
    const uniformizarCards = lista.dataset.alturaCards === 'uniforme';
    if (lista.dataset.recalcularAltura === 'true') {
        lista.style.height = '';
        delete lista.dataset.maiorAlturaCard;
        delete lista.dataset.recalcularAltura;
    }
    if (!lista.querySelector('.card, .card-pedido, .card-compra')) return;

    if (uniformizarCards) {
        // Measure natural content again, before applying the largest height seen
        // at this width. The list itself remains free to grow with every row.
        lista.style.height = '';
        lista.style.setProperty('--altura-card', '0px');
        const cards = lista.querySelectorAll(':scope > .card, :scope > .card-pedido, :scope > .card-compra');
        const maiorAltura = Math.ceil(Math.max(
            Number(lista.dataset.maiorAlturaCard || 0),
            ...Array.from(cards, card => card.getBoundingClientRect().height)
        ));
        lista.dataset.maiorAlturaCard = String(maiorAltura);
        lista.style.setProperty('--altura-card', `${maiorAltura}px`);
    } else {
        // Keep the compact sizing already used by Clients and Users.
        if (lista.style.height) return;
        lista.style.height = `${Math.ceil(lista.getBoundingClientRect().height)}px`;
    }

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
