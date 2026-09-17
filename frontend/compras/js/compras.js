let paginaCompras = 0;
let itensNovaCompra = [];
let compraEmEdicao = null;
let filtroCompras = null;
let consultaCompras = 0;
let salvandoCompra = false;

const areaCompra = document.getElementById('conteudo-dinamico');
const campoCompra = id => document.getElementById(id);
const moedaCompra = valor => Number(valor || 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
const escaparCompra = valor => String(valor ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
}[c]));
const formatarDataCompra = data => data ? data.slice(0, 10).split('-').reverse().join('/') : '';
const dataFiltroCompra = data => data.split('-').reverse().join('-');
const subtotalCompra = item => Math.round(Number(item.valorUnitario) * 100) * item.quantidade / 100;

function definirModoEdicaoCompra(ativo) {
    document.body.classList.toggle('edicao-compra-ativa', ativo);
    const historico = document.querySelector('.historico-compras');
    const navegacao = document.querySelector('.sidebar nav');
    if (historico) historico.inert = ativo;
    if (navegacao) navegacao.inert = ativo;
}

function limparFormularioCompra() {
    campoCompra('form-compra').reset();
    compraEmEdicao = null;
    itensNovaCompra = [];
    const hoje = new Date();
    campoCompra('data-compra').value = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`;
    campoCompra('titulo-form-compra').textContent = 'Cadastro de Compra';
    document.querySelector('#form-compra button[type=submit]').textContent = 'Cadastrar Compra';
    campoCompra('cancelar-edicao-compra').classList.add('hidden');
    definirModoEdicaoCompra(false);
    renderizarItensCompra();
}

async function listarCompras(pagina = 0) {
    const lista = campoCompra('lista-compras');
    if (!lista) return;
    const consulta = ++consultaCompras;
    lista.innerHTML = '<div class="estado">Carregando compras...</div>';
    campoCompra('compra-anterior').disabled = true;
    campoCompra('compra-proximo').disabled = true;
    campoCompra('compra-pagina-atual').textContent = '';
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
        campoCompra('compra-pagina-atual').textContent = total ? `Página ${paginaCompras + 1} de ${total}` : 'Página 0 de 0';
        campoCompra('compra-anterior').disabled = paginaCompras === 0;
        campoCompra('compra-proximo').disabled = paginaCompras + 1 >= total;
    } catch (erro) {
        if (consulta !== consultaCompras || !lista.isConnected) return;
        lista.innerHTML = `<div class="estado">${escaparCompra(erro.message)} <button type="button" class="btn btn-secundario" id="tentar-compras">Tentar novamente</button></div>`;
    }
}

function criarCardCompra(compra) {
    return `<article class="card-compra" data-compra-id="${escaparCompra(compra.id)}" aria-label="Compra ${escaparCompra(compra.id)}">
        <div class="card-compra-corpo"><h3>Itens</h3><ul class="card-compra-itens">${(compra.itens || []).map(item => `<li><strong>${escaparCompra(item.quantidade)} ${escaparCompra(item.nome)}</strong><small>${moedaCompra(item.valorUnitario)} por unidade</small></li>`).join('')}</ul>
        <p class="card-compra-data">Data da compra: ${formatarDataCompra(compra.dataCompra)}</p></div>
        <div class="card-compra-total"><span>Valor Total:</span><strong>${moedaCompra(compra.valorTotal)}</strong></div>
        <div class="card-compra-acoes"><button type="button" class="btn btn-secundario" data-acao-compra="editar">Editar</button><button type="button" class="btn btn-secundario" data-acao-compra="exibir">Exibir</button></div></article>`;
}

function adicionarItemCompra() {
    const nome = campoCompra('nome-produto-compra');
    const quantidade = campoCompra('quantidade-compra');
    const valor = campoCompra('valor-unitario-compra');
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
    campoCompra('itens-compra').innerHTML = itensNovaCompra.length ? itensNovaCompra.map((item, indice) => `<div class="item-compra"><div><h4>${escaparCompra(item.nome)}</h4><p>Quantidade: ${item.quantidade} · Unitário: ${moedaCompra(item.valorUnitario)}</p><p><strong>Total: ${moedaCompra(subtotalCompra(item))}</strong></p></div><button type="button" class="remover-item-compra" data-remover-compra="${indice}" aria-label="Remover ${escaparCompra(item.nome)}">&times;</button></div>`).join('') : '<div class="estado">Nenhum item adicionado</div>';
    campoCompra('valor-total-compra').textContent = moedaCompra(itensNovaCompra.reduce((soma, item) => soma + Math.round(subtotalCompra(item) * 100), 0) / 100);
}

async function salvarCompra() {
    if (salvandoCompra) return;
    if (!itensNovaCompra.length) return alert('Adicione pelo menos um item à compra.');
    if (['nome-produto-compra', 'quantidade-compra', 'valor-unitario-compra'].some(id => campoCompra(id).value)) {
        alert('Há um item em preenchimento. Adicione-o ao carrinho ou limpe os campos antes de salvar.');
        return;
    }
    const id = compraEmEdicao;
    const form = campoCompra('form-compra');
    const dados = {
        itens: itensNovaCompra.map(({nome, quantidade, valorUnitario}) => ({
            nome,
            quantidade,
            valorUnitario
        })), dataCompra: campoCompra('data-compra').value, observacao: campoCompra('observacao-compra').value.trim()
    };
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
    const form = campoCompra('form-compra');
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
        campoCompra('data-compra').value = compra.dataCompra;
        campoCompra('observacao-compra').value = compra.observacao || '';
        campoCompra('titulo-form-compra').textContent = `Edição da Compra n° ${compra.id}`;
        form.querySelector('button[type=submit]').textContent = 'Salvar alterações';
        campoCompra('cancelar-edicao-compra').classList.remove('hidden');
        renderizarItensCompra();
        definirModoEdicaoCompra(true);
        document.querySelector('.cadastro-compra').scrollIntoView({behavior: 'smooth'});
        campoCompra('nome-produto-compra').focus({preventScroll: true});
    } catch (erro) {
        alert(erro.message);
    }
}

async function exibirCompra(id) {
    const form = campoCompra('form-compra');
    try {
        const compra = await obterCompra(id);
        if (!form?.isConnected || compraEmEdicao !== null) return;
        abrirModal({
            titulo: `Compra n° ${compra.id}`,
            conteudoHtml: `<div class="detalhes-compra"><p><strong>Data da compra:</strong> ${formatarDataCompra(compra.dataCompra)}</p>
            <div class="compra-tabela-container"><table class="compra-tabela"><thead><tr><th scope="col">Produto</th><th scope="col">Quantidade</th><th scope="col">Unitário</th><th scope="col">Total</th></tr></thead><tbody>${compra.itens.map(item => `<tr><td>${escaparCompra(item.nome)}</td><td>${escaparCompra(item.quantidade)}</td><td>${moedaCompra(item.valorUnitario)}</td><td>${moedaCompra(item.subTotal)}</td></tr>`).join('')}</tbody></table></div>
            <p class="total-compra">Valor Total: <strong>${moedaCompra(compra.valorTotal)}</strong></p><p><strong>Observação:</strong></p><p class="observacao">${escaparCompra(compra.observacao || 'Nenhuma observação informada.')}</p>
            <div class="compra-modal-acoes"><button type="button" class="btn btn-secundario" id="fechar-exibicao-compra">Fechar</button><button type="button" class="btn btn-primario" id="editar-exibicao-compra">Editar compra</button></div></div>`
        });
        campoCompra('fechar-exibicao-compra').onclick = fecharModal;
        campoCompra('editar-exibicao-compra').onclick = () => {
            fecharModal();
            editarCompra(compra.id);
        };
    } catch (erro) {
        alert(erro.message);
    }
}

areaCompra.addEventListener('pagina:carregada', e => {
    ++consultaCompras;
    if (e.detail.modulo !== 'compra') {
        definirModoEdicaoCompra(false);
        return;
    }
    paginaCompras = 0;
    filtroCompras = null;
    limparFormularioCompra();
    listarCompras(0);
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
        campoCompra('form-filtro-compras').reset();
        campoCompra('data-fim-compra').disabled = true;
        campoCompra('data-fim-compra').required = false;
        listarCompras(0);
    }
});

areaCompra.addEventListener('change', e => {
    if (e.target.id === 'usar-data-final-compra') {
        campoCompra('data-fim-compra').disabled = !e.target.checked;
        campoCompra('data-fim-compra').required = e.target.checked;
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
        const inicio = campoCompra('data-inicio-compra').value;
        const fim = campoCompra('usar-data-final-compra').checked ? campoCompra('data-fim-compra').value : inicio;
        if (fim < inicio) return alert('A data final deve ser igual ou posterior à data inicial.');
        filtroCompras = {inicio, fim};
        listarCompras(0);
    }
});
