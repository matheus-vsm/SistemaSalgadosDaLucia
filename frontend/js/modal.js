document.addEventListener('DOMContentLoaded', () => {
    const overlay = document.getElementById('modal-overlay');
    const btnFechar = document.getElementById('modal-fechar');

    if (!overlay) return;

    btnFechar?.addEventListener('click', fecharModal);

    overlay.addEventListener('click', (event) => {
        if (event.target === overlay) {
            fecharModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !overlay.classList.contains('hidden')) {
            fecharModal();
        }
    });
});

function abrirModal({ titulo = '', conteudoHtml = '', aoFechar = null }) {
    const overlay = document.getElementById('modal-overlay');
    const tituloEl = document.getElementById('modal-titulo');
    const corpoEl = document.getElementById('modal-corpo');

    if (!overlay || !tituloEl || !corpoEl) return;

    tituloEl.textContent = titulo;
    corpoEl.innerHTML = conteudoHtml;
    overlay._aoFechar = aoFechar;
    overlay.classList.remove('hidden');
    document.body.classList.add('modal-aberto');
}

function fecharModal() {
    const overlay = document.getElementById('modal-overlay');
    const corpoEl = document.getElementById('modal-corpo');

    if (!overlay) return;

    if (typeof overlay._aoFechar === 'function') {
        overlay._aoFechar();
    }

    overlay._aoFechar = null;
    overlay.classList.add('hidden');
    if (corpoEl) corpoEl.innerHTML = '';
    document.body.classList.remove('modal-aberto');
}
