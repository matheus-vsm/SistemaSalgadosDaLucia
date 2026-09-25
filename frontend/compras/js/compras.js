let paginaCompras = 0;
let itensNovaCompra = [];
let compraEmEdicao = null;
let filtroCompras = null;
let consultaCompras = 0;
let salvandoCompra = false;
let templateCardCompra = '';
let templateItemCompra = '';

const moedaCompra = valor => Number(valor || 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
const escaparCompra = valor => String(valor ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
}[c]));

function formatarDataCompra(data) {
    return data ? data.slice(0, 10).split('-').reverse().join('/') : '';
}

function dataFiltroCompra(data) {
    return data.split('-').reverse().join('-');
}

function subtotalCompra(item) {
    return Math.round(Number(item.valorUnitario) * 100) * item.quantidade / 100;
}

async function carregarDadosIniciaisCompra() {
    const form = document.getElementById('form-compra');
    paginaCompras = 0;
    filtroCompras = null;
    form.inert = true;
    try {
        const [respostaCard, respostaItem] = await Promise.all([
            fetch('compras/html/card-compra.html'),
            fetch('compras/html/item-compra.html')
        ]);
        if (!respostaCard.ok || !respostaItem.ok) throw new Error('Erro ao carregar templates de compras');
        templateCardCompra = await respostaCard.text();
        templateItemCompra = await respostaItem.text();
        if (!form.isConnected) return;
        limparFormularioCompra();
        form.inert = false;
        listarCompras(0);
    } catch (erro) {
        console.error('Erro ao iniciar compras:', erro);
        if (form.isConnected) document.getElementById('lista-compras').innerHTML = '<div class="estado">Erro ao carregar a tela de compras</div>';
    }
}

function definirModoEdicaoCompra(ativo) {
    document.body.classList.toggle('edicao-compra-ativa', ativo);
    const historico = document.querySelector('.historico-compras');
    const navegacao = document.querySelector('.sidebar nav');
    if (historico) historico.inert = ativo;
    if (navegacao) navegacao.inert = ativo;
}

function limparFormularioCompra() {
    document.getElementById('form-compra').reset();
    compraEmEdicao = null;
    itensNovaCompra = [];
    const hoje = new Date();
    document.getElementById('data-compra').value = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;
    document.getElementById('titulo-form-compra').textContent = 'Cadastro de Compra';
    document.querySelector('#form-compra button[type=submit]').textContent = 'Cadastrar Compra';
    document.getElementById('cancelar-edicao-compra').classList.add('hidden');
    definirModoEdicaoCompra(false);
    renderizarItensCompra();
}

async function listarCompras(pagina = 0) {
    const lista = document.getElementById('lista-compras');
    if (!lista) return;
    const consulta = ++consultaCompras;
    lista.innerHTML = '<div class="estado">Carregando compras...</div>';
    document.getElementById('compra-anterior').disabled = true;
    document.getElementById('compra-proximo').disabled = true;
    document.getElementById('compra-pagina-atual').textContent = '';
    const params = new URLSearchParams({page: Math.max(0, pagina), size: 4, sort: 'dataCompra,desc'});
    if (filtroCompras) {
        params.set('dataInicioCompra', dataFiltroCompra(filtroCompras.inicio));
        params.set('dataFimCompra', dataFiltroCompra(filtroCompras.fim));
    }
    try {
        const {response, data} = await apiJson(`/compras?${params}`);
        if (consulta !== consultaCompras || !lista.isConnected) return;
        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar compras.');
        lista.innerHTML = data.content?.length ? data.content.map(criarCardCompra).join('') : '<div class="estado">Nenhuma compra encontrada</div>';
        paginaCompras = data.page?.number ?? data.number ?? 0;
        const total = data.page?.totalPages ?? data.totalPages ?? 0;
        document.getElementById('compra-pagina-atual').textContent = total ? `Página ${paginaCompras + 1} de ${total}` : 'Página 0 de 0';
        document.getElementById('compra-anterior').disabled = paginaCompras === 0;
        document.getElementById('compra-proximo').disabled = paginaCompras + 1 >= total;
    } catch (erro) {
        if (consulta !== consultaCompras || !lista.isConnected) return;
        lista.innerHTML = `<div class="estado">${escaparCompra(erro.message)} <button type="button" class="btn btn-secundario" id="tentar-compras">Tentar novamente</button></div>`;
    }
}

function criarCardCompra(compra) {
    const template = document.createElement('template');
    template.innerHTML = templateCardCompra.trim();
    const card = template.content.firstElementChild;
    card.dataset.compraId = compra.id;
    card.setAttribute('aria-label', `Compra ${compra.id}`);
    card.querySelector('.card-compra-data').textContent = `Data da compra: ${formatarDataCompra(compra.dataCompra)}`;
    card.querySelector('.card-compra-total strong').textContent = moedaCompra(compra.valorTotal);
    const lista = card.querySelector('.card-compra-itens');
    const modeloItem = lista.querySelector('li');
    modeloItem.remove();
    (compra.itens || []).forEach(item => {
        const linha = modeloItem.cloneNode(true);
        linha.querySelector('strong').textContent = `${item.quantidade} ${item.nome}`;
        linha.querySelector('small').textContent = `${moedaCompra(item.valorUnitario)} por unidade`;
        lista.appendChild(linha);
    });
    return card.outerHTML;
}

function adicionarItemCompra() {
    const nome = document.getElementById('nome-produto-compra');
    const quantidade = document.getElementById('quantidade-compra');
    const valor = document.getElementById('valor-unitario-compra');
    if (!nome.value.trim() || !quantidade.value || !valor.value) {
        alert('Informe o nome do produto, a quantidade e o valor unitário.');
        (!nome.value.trim() ? nome : !quantidade.value ? quantidade : valor).focus();
        return;
    }
    if (![nome, quantidade, valor].every(campo => campo.reportValidity())) return;
    itensNovaCompra.push({
        nome: nome.value.trim(),
        quantidade: Number(quantidade.value),
        valorUnitario: Number(valor.value)
    });
    renderizarItensCompra();
    nome.value = quantidade.value = valor.value = '';
    nome.focus();
}

function renderizarItensCompra() {
    const template = document.createElement('template');
    template.innerHTML = templateItemCompra.trim();
    const modeloItem = template.content.querySelector('.item-compra');
    const lista = document.getElementById('itens-compra');
    lista.replaceChildren();
    if (!itensNovaCompra.length) lista.appendChild(template.content.querySelector('.estado'));
    itensNovaCompra.forEach((item, indice) => {
        const linha = modeloItem.cloneNode(true);
        linha.querySelector('h4').textContent = item.nome;
        linha.querySelector('.item-compra-quantidade').textContent = `Quantidade: ${item.quantidade} · Unitário: ${moedaCompra(item.valorUnitario)}`;
        linha.querySelector('.item-compra-total').textContent = `Total: ${moedaCompra(subtotalCompra(item))}`;
        const botao = linha.querySelector('.remover-item-compra');
        botao.dataset.removerCompra = indice;
        botao.setAttribute('aria-label', `Remover ${item.nome}`);
        lista.appendChild(linha);
    });
    atualizarTotalCompra();
}

function atualizarTotalCompra() {
    document.getElementById('valor-total-compra').textContent = moedaCompra(itensNovaCompra.reduce((soma, item) => soma + Math.round(subtotalCompra(item) * 100), 0) / 100);
}

function dadosFormularioCompra() {
    return {
        itens: itensNovaCompra.map(({nome, quantidade, valorUnitario}) => ({
            nome,
            quantidade,
            valorUnitario
        })),
        dataCompra: document.getElementById('data-compra').value,
        observacao: document.getElementById('observacao-compra').value.trim()
    };
}

async function salvarCompra() {
    if (salvandoCompra) return;
    if (!itensNovaCompra.length) return alert('Adicione pelo menos um item à compra.');
    if (['nome-produto-compra', 'quantidade-compra', 'valor-unitario-compra'].some(id => document.getElementById(id).value)) {
        alert('Há um item em preenchimento. Adicione-o ao carrinho ou limpe os campos antes de salvar.');
        return;
    }
    const id = compraEmEdicao;
    const form = document.getElementById('form-compra');
    const dados = dadosFormularioCompra();
    salvandoCompra = true;
    form.inert = true;
    form.setAttribute('aria-busy', 'true');
    const botao = form.querySelector('button[type=submit]');
    botao.disabled = true;
    botao.textContent = 'Salvando...';
    try {
        const {response, data} = await apiJson(id ? `/compras/${id}` : '/compras', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(dados)
        });
        if (!response.ok) throw new Error(data?.mensagem || 'Não foi possível salvar a compra.');
        if (!form.isConnected) return;
        limparFormularioCompra();
        await listarCompras(0);
        alert(`Compra ${id ? 'editada' : 'cadastrada'} com sucesso!`);
    } catch (erro) {
        alert(erro.message || 'Erro ao conectar com o servidor.');
    } finally {
        salvandoCompra = false;
        form.inert = false;
        form.removeAttribute('aria-busy');
        botao.disabled = false;
        botao.textContent = compraEmEdicao ? 'Salvar alterações' : 'Cadastrar Compra';
    }
}

async function obterCompra(id) {
    const {response, data} = await apiJson(`/compras/${id}`);
    if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar compra.');
    return data;
}

async function editarCompra(id) {
    const form = document.getElementById('form-compra');
    try {
        const compra = await obterCompra(id);
        if (!form?.isConnected || salvandoCompra || compraEmEdicao !== null) return;
        form.reset();
        compraEmEdicao = compra.id;
        itensNovaCompra = compra.itens.map(({nome, quantidade, valorUnitario}) => ({
            nome,
            quantidade,
            valorUnitario: Number(valorUnitario)
        }));
        document.getElementById('data-compra').value = compra.dataCompra;
        document.getElementById('observacao-compra').value = compra.observacao || '';
        document.getElementById('titulo-form-compra').textContent = `Edição da Compra n° ${compra.id}`;
        form.querySelector('button[type=submit]').textContent = 'Salvar alterações';
        document.getElementById('cancelar-edicao-compra').classList.remove('hidden');
        renderizarItensCompra();
        definirModoEdicaoCompra(true);
        document.querySelector('.cadastro-compra').scrollIntoView({behavior: 'smooth'});
        document.getElementById('nome-produto-compra').focus({preventScroll: true});
    } catch (erro) {
        alert(erro.message);
    }
}

async function exibirCompra(id) {
    const form = document.getElementById('form-compra');
    try {
        const [respostaTemplate, compra] = await Promise.all([
            fetch('compras/html/modal-exibir-compra.html'),
            obterCompra(id)
        ]);
        if (!respostaTemplate.ok) throw new Error('Erro ao carregar modal de exibição');
        const htmlModal = await respostaTemplate.text();
        if (!form?.isConnected || compraEmEdicao !== null) return;
        abrirModal({
            titulo: `Compra n° ${compra.id}`,
            conteudoHtml: htmlModal
        });
        preencherModalExibicaoCompra(compra);
    } catch (erro) {
        alert(erro.message);
    }
}

function preencherModalExibicaoCompra(compra) {
    document.getElementById('compra-data-exibicao').textContent = formatarDataCompra(compra.dataCompra);
    document.getElementById('compra-total-exibicao').textContent = moedaCompra(compra.valorTotal);
    document.getElementById('compra-observacao-exibicao').textContent = compra.observacao || 'Nenhuma observação informada.';

    const lista = document.getElementById('compra-itens-exibicao');
    compra.itens.forEach(item => {
        const linha = document.createElement('tr');
        [item.nome, item.quantidade, moedaCompra(item.valorUnitario), moedaCompra(item.subTotal)].forEach(valor => {
            const coluna = document.createElement('td');
            coluna.textContent = valor ?? '';
            linha.appendChild(coluna);
        });
        lista.appendChild(linha);
    });

    document.getElementById('fechar-exibicao-compra').onclick = fecharModal;
    document.getElementById('editar-exibicao-compra').onclick = () => editarCompraPelaExibicao(compra.id);
}

function editarCompraPelaExibicao(id) {
    fecharModal();
    editarCompra(id);
}

const areaCompra = document.getElementById('conteudo-dinamico');
areaCompra.addEventListener('pagina:carregada', e => {
    ++consultaCompras;
    if (e.detail.modulo !== 'compra') {
        definirModoEdicaoCompra(false);
        return;
    }
    carregarDadosIniciaisCompra();
});

areaCompra.addEventListener('click', e => {
    const botao = e.target.closest('button');
    if (!botao || botao.disabled || salvandoCompra) return;
    if (botao.dataset.acaoCompra && compraEmEdicao === null) {
        const id = Number(botao.closest('.card-compra').dataset.compraId);
        if (botao.dataset.acaoCompra === 'editar') editarCompra(id);
        if (botao.dataset.acaoCompra === 'exibir') exibirCompra(id);
    }
    if (botao.id === 'adicionar-item-compra') adicionarItemCompra();
    if (botao.id === 'cancelar-edicao-compra') limparFormularioCompra();
    if (botao.dataset.removerCompra !== undefined) {
        itensNovaCompra.splice(Number(botao.dataset.removerCompra), 1);
        renderizarItensCompra();
    }
    if (botao.id === 'compra-anterior') listarCompras(paginaCompras - 1);
    if (botao.id === 'compra-proximo') listarCompras(paginaCompras + 1);
    if (botao.id === 'tentar-compras') listarCompras(paginaCompras);
    if (botao.id === 'buscar-todos-compras') {
        filtroCompras = null;
        document.getElementById('form-filtro-compras').reset();
        document.getElementById('data-fim-compra').disabled = true;
        document.getElementById('data-fim-compra').required = false;
        listarCompras(0);
    }
});

areaCompra.addEventListener('change', e => {
    if (e.target.id === 'usar-data-final-compra') {
        document.getElementById('data-fim-compra').disabled = !e.target.checked;
        document.getElementById('data-fim-compra').required = e.target.checked;
    }
});

areaCompra.addEventListener('keydown', e => {
    if (e.key === 'Enter' && ['nome-produto-compra', 'quantidade-compra', 'valor-unitario-compra'].includes(e.target.id)) {
        e.preventDefault();
        adicionarItemCompra();
    }
});

areaCompra.addEventListener('submit', e => {
    if (e.target.id === 'form-compra') {
        e.preventDefault();
        salvarCompra();
    }
    if (e.target.id === 'form-filtro-compras') {
        e.preventDefault();
        const inicio = document.getElementById('data-inicio-compra').value;
        const fim = document.getElementById('usar-data-final-compra').checked ? document.getElementById('data-fim-compra').value : inicio;
        if (fim < inicio) return alert('A data final deve ser igual ou posterior à data inicial.');
        filtroCompras = {inicio, fim};
        listarCompras(0);
    }
});
