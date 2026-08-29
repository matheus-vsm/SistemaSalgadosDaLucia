let paginaPedidos = 0;
let statusPedidoSelecionado = 'EM_ANDAMENTO';
let enumsPedido = {formasPagamento: [], tiposEntrega: [], statusPedido: [], tiposPrecos: []};
let clientesPedido = [];
let salgadosPedido = [];
let itensNovoPedido = [];
let pedidoEmEdicao = null;
let filtrarPedidosPorData = false;
let templateCardPedido = '';

const moedaPedido = valor => Number(valor || 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
const textoEnumPedido = (grupo, valor) => enumsPedido[grupo]?.find(item => item.valor === valor)?.descricao || valor || '';
const classeStatusPedido = status => `status-${String(status || '').toLowerCase().replaceAll('_', '-')}`;
const escaparPedido = valor => String(valor ?? '').replace(/[&<>'"]/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
}[c]));

function dataParaFiltroPedido(data, fim = false) {
    const [ano, mes, dia] = data.split('-');
    return `${dia}-${mes}-${ano} ${fim ? '23:59:59' : '00:00:00'}`;
}

function formatarDataPedido(valor) {
    if (!valor) return '';
    const data = new Date(valor);
    return `${data.toLocaleDateString('pt-BR')} - ${data.toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit'
    })}`;
}

async function carregarDadosIniciaisPedido() {
    try {
        pedidoEmEdicao = null;
        itensNovoPedido = [];
        filtrarPedidosPorData = false;
        const [resEnums, resClientes, resSalgados, respostaCard] = await Promise.all([
            apiJson('/enums/pedido'),
            apiJson('/clientes?ativo=true&page=0&size=100'),
            apiJson('/salgados?ativo=true&page=0&size=100'),
            fetch('pedidos/html/card-pedido.html')
        ]);
        if (!resEnums.response.ok || !resClientes.response.ok || !resSalgados.response.ok || !respostaCard.ok) throw new Error('Erro ao carregar dados do cadastro');
        enumsPedido = resEnums.data;
        clientesPedido = resClientes.data.content || [];
        salgadosPedido = resSalgados.data.content || [];
        templateCardPedido = await respostaCard.text();
        montarFiltrosStatusPedido();
        preencherSelectPedido('tipo-entrega-pedido', enumsPedido.tiposEntrega);
        preencherSelectPedido('tipo-preco-pedido', enumsPedido.tiposPrecos);
        preencherSelectPedido('forma-pagamento-pedido', enumsPedido.formasPagamento);
        preencherSelectPedido('cliente-pedido', clientesPedido.map(c => ({valor: c.id, descricao: c.nome})));
        preencherSelectPedido('salgado-pedido', salgadosPedido.map(s => ({valor: s.id, descricao: s.nome})));
        listarPedidos(0);
    } catch (erro) {
        console.error('Erro ao iniciar pedidos:', erro);
        document.getElementById('lista-pedidos').innerHTML = '<div class="estado">Erro ao carregar a tela de pedidos</div>';
    }
}

function preencherSelectPedido(id, itens) {
    const select = document.getElementById(id);
    if (!select) return;
    select.innerHTML = '<option value="">Selecione</option>' + itens.map(item => `<option value="${escaparPedido(item.valor)}">${escaparPedido(item.descricao)}</option>`).join('');
}

function montarFiltrosStatusPedido() {
    const filtro = document.getElementById('filtro-status-pedido');
    filtro.innerHTML = enumsPedido.statusPedido.map(status => `<button type="button" data-status-pedido="${status.valor}" class="btn-status-pedido ${classeStatusPedido(status.valor)} ${status.valor === statusPedidoSelecionado ? 'selecionado' : ''}">${escaparPedido(status.descricao)}</button>`).join('');
}

async function listarPedidos(pagina = 0) {
    const lista = document.getElementById('lista-pedidos');
    if (!lista) return;
    lista.innerHTML = '<div class="estado">Carregando pedidos...</div>';
    const params = new URLSearchParams({statusPedido: statusPedidoSelecionado, page: pagina, size: 4});
    if (filtrarPedidosPorData) {
        const inicio = document.getElementById('data-inicio-pedido')?.value;
        const usarFim = document.getElementById('usar-data-final-pedido')?.checked;
        const fim = usarFim ? document.getElementById('data-fim-pedido')?.value : inicio;
        if (inicio) params.set('dataInicioEntrega', dataParaFiltroPedido(inicio));
        if (fim) params.set('dataFimEntrega', dataParaFiltroPedido(fim, true));
    }
    try {
        const {response, data} = await apiJson(`/pedidos?${params}`);
        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao listar pedidos');
        lista.innerHTML = data.content?.length ? data.content.map(criarCardPedido).join('') : '<div class="estado">Nenhum pedido encontrado</div>';
        paginaPedidos = data.page?.number ?? data.number ?? 0;
        const total = data.page?.totalPages ?? data.totalPages ?? 0;
        document.getElementById('pedido-pagina-atual').textContent = total ? `Página ${paginaPedidos + 1} de ${total}` : 'Página 0 de 0';
        document.getElementById('pedido-anterior').disabled = paginaPedidos === 0;
        document.getElementById('pedido-proximo').disabled = paginaPedidos + 1 >= total;
    } catch (erro) {
        console.error(erro);
        lista.innerHTML = '<div class="estado">Erro ao carregar pedidos</div>';
    }
}

function criarCardPedido(pedido) {
    const endereco = pedido.enderecoEntrega;
    const template = document.createElement('template');
    template.innerHTML = templateCardPedido.trim();
    const card = template.content.firstElementChild;
    card.classList.add(classeStatusPedido(pedido.status));
    card.dataset.pedidoId = pedido.id;
    card.querySelector('.pedido-numero-card').textContent = `Pedido n° ${pedido.id}`;
    const status = card.querySelector('.pedido-status-card');
    status.classList.add(classeStatusPedido(pedido.status));
    status.querySelector('strong').textContent = textoEnumPedido('statusPedido', pedido.status);
    card.querySelector('.pedido-cliente-card').textContent = pedido.cliente?.nome || '';
    card.querySelector('.pedido-total-card').textContent = moedaPedido(pedido.valorTotal);
    card.querySelector('.pedido-pagamento-card').textContent = textoEnumPedido('formasPagamento', pedido.formaPagamento);
    card.querySelector('.pedido-entrega-rotulo-card').textContent = `${pedido.tipoEntrega === 'ENTREGA' ? 'Entrega' : 'Retirada'}:`;
    card.querySelector('.pedido-entrega-card').textContent = endereco ? `${endereco.logradouro}, ${endereco.numero} - ${endereco.bairro}` : 'No local';
    card.querySelector('.card-pedido-data').textContent = `DATA: ${formatarDataPedido(pedido.dataEntrega)}`;
    const listaItens = card.querySelector('.card-pedido-itens');
    (pedido.itens || []).forEach(item => {
        const linha = document.createElement('li');
        linha.textContent = `${item.quantidade} ${item.nomeSalgado} — ${textoEnumPedido('tiposPrecos', item.tipoPreco)}`;
        listaItens.appendChild(linha);
    });
    return card.outerHTML;
}

function alternarEnderecoPedido(tipo) {
    const entrega = tipo === 'ENTREGA';
    const bloco = document.getElementById('endereco-pedido');
    bloco.classList.toggle('hidden', !entrega);
    bloco.querySelectorAll('input').forEach(campo => {
        if (campo.id !== 'complemento-pedido') campo.required = entrega;
    });
    if (!entrega) bloco.querySelectorAll('input').forEach(campo => campo.value = '');
    if (entrega) {
        const clienteId = document.getElementById('cliente-pedido').value;
        if (clienteId) preencherEnderecoClientePedido(clienteId);
    }
    atualizarTotalPedido();
}

function preencherEnderecoClientePedido(id) {
    const endereco = clientesPedido.find(c => String(c.id) === String(id))?.endereco || {};
    ['cep', 'logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf'].forEach(campo => {
        document.getElementById(`${campo}-pedido`).value = endereco[campo] || '';
    });
}

async function preencherEnderecoPedidoPorCep(cep) {
    try {
        const endereco = await buscarEnderecoPorCep(cep);
        if (!endereco) {
            alert('CEP não encontrado.');
            return;
        }
        document.getElementById('logradouro-pedido').value = endereco.logradouro;
        document.getElementById('bairro-pedido').value = endereco.bairro;
        document.getElementById('cidade-pedido').value = endereco.cidade;
        document.getElementById('uf-pedido').value = endereco.uf;
        document.getElementById('numero-pedido').focus();
    } catch (erro) {
        console.error('Erro ao buscar CEP do pedido:', erro);
        alert('Não foi possível consultar o CEP.');
    }
}

function precoItemPedido(salgado, tipo) {
    const precoCento = Number(tipo === 'CONGELADO' ? salgado.precoCentoCongelado : salgado.precoCentoProcessado);
    return precoCento / 100;
}

function adicionarItemPedido() {
    const salgadoId = Number(document.getElementById('salgado-pedido').value);
    const tipoPreco = document.getElementById('tipo-preco-pedido').value;
    const quantidade = Number(document.getElementById('quantidade-pedido').value);
    const salgado = salgadosPedido.find(item => item.id === salgadoId);
    if (!salgado || !tipoPreco || !quantidade) return alert('Selecione o salgado, o tipo e informe a quantidade.');
    itensNovoPedido.push({
        salgadoId,
        nomeSalgado: salgado.nome,
        descricao: salgado.descricao,
        quantidade,
        tipoPreco,
        precoUnitario: precoItemPedido(salgado, tipoPreco)
    });
    renderizarItensPedido();
    document.getElementById('salgado-pedido').value = '';
    document.getElementById('tipo-preco-pedido').value = '';
    document.getElementById('quantidade-pedido').value = '';
    document.getElementById('salgado-pedido').focus();
}

function renderizarItensPedido() {
    const lista = document.getElementById('itens-pedido');
    lista.innerHTML = itensNovoPedido.length ? itensNovoPedido.map((item, indice) => `<div class="item-pedido"><img src="imagens/coxinha.jpg" alt=""><div><h3>${escaparPedido(item.nomeSalgado)}</h3><p>${escaparPedido(item.descricao)}</p><p>Quantidade: ${item.quantidade} | ${escaparPedido(textoEnumPedido('tiposPrecos', item.tipoPreco))}</p><p><strong>Subtotal: ${moedaPedido(item.precoUnitario * item.quantidade)}</strong></p></div><button type="button" class="remover-item-pedido" data-remover-item="${indice}" aria-label="Remover item">🗑</button></div>`).join('') : '<div class="estado">Nenhum salgado adicionado</div>';
    atualizarTotalPedido();
}

function atualizarTotalPedido() {
    const subtotal = itensNovoPedido.reduce((soma, item) => soma + item.precoUnitario * item.quantidade, 0);
    const frete = document.getElementById('tipo-entrega-pedido')?.value === 'ENTREGA' ? Number(document.getElementById('frete-pedido')?.value || 0) : 0;
    document.getElementById('valor-total-pedido').textContent = moedaPedido(subtotal + frete);
}

function idUsuarioLogadoPedido() {
    try {
        return Number(JSON.parse(atob(localStorage.getItem('refreshToken').split('.')[1])).sub);
    } catch (_) {
        return null;
    }
}

function dadosFormularioPedido() {
    const tipoEntrega = document.getElementById('tipo-entrega-pedido').value;
    const enderecoEntrega = tipoEntrega === 'ENTREGA' ? Object.fromEntries(['logradouro', 'numero', 'complemento', 'cep', 'bairro', 'cidade', 'uf'].map(c => [c, document.getElementById(`${c}-pedido`).value])) : null;
    return {
        clienteId: Number(document.getElementById('cliente-pedido').value),
        itens: itensNovoPedido.map(({salgadoId, quantidade, tipoPreco}) => ({salgadoId, quantidade, tipoPreco})),
        enderecoEntrega,
        dataPedido: new Date().toISOString().slice(0, 10),
        dataEntrega: `${document.getElementById('data-entrega-pedido').value}T${document.getElementById('hora-entrega-pedido').value}:00`,
        tipoEntrega,
        formaPagamento: document.getElementById('forma-pagamento-pedido').value,
        usuarioResponsavelId: idUsuarioLogadoPedido(),
        frete: tipoEntrega === 'ENTREGA' ? Number(document.getElementById('frete-pedido').value) : null
    };
}

async function salvarPedido() {
    if (!itensNovoPedido.length) return alert('Adicione pelo menos um salgado ao pedido.');
    const id = pedidoEmEdicao;
    try {
        const {response, data} = await apiJson(id ? `/pedidos/${id}` : '/pedidos', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(dadosFormularioPedido())
        });
        if (!response.ok) return alert(data?.mensagem || `Erro ao ${id ? 'editar' : 'cadastrar'} pedido`);
        alert(`Pedido ${id ? 'editado' : 'cadastrado'} com sucesso!`);
        limparFormularioPedido();
        listarPedidos(0);
    } catch (erro) {
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

function limparFormularioPedido() {
    document.getElementById('form-pedido').reset();
    pedidoEmEdicao = null;
    itensNovoPedido = [];
    alternarEnderecoPedido('');
    renderizarItensPedido();
    document.querySelector('#form-pedido button[type=submit]').textContent = 'Cadastrar Pedido';
    document.getElementById('cancelar-edicao-pedido').classList.add('hidden');
    document.getElementById('titulo-form-pedido').textContent = 'Cadastro de Pedido';
    definirModoEdicaoPedido(false);
}

function definirModoEdicaoPedido(ativo) {
    document.body.classList.toggle('edicao-pedido-ativa', ativo);
    const historico = document.querySelector('.historico-pedidos');
    const navegacao = document.querySelector('.sidebar nav');
    if (historico) historico.inert = ativo;
    if (navegacao) navegacao.inert = ativo;
}

async function obterPedido(id) {
    const {response, data} = await apiJson(`/pedidos/${id}`);
    if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar pedido');
    return data;
}

async function exibirPedido(id) {
    try {
        const [respostaTemplate, p] = await Promise.all([
            fetch('pedidos/html/modal-exibir-pedido.html'),
            obterPedido(id)
        ]);
        if (!respostaTemplate.ok) throw new Error('Erro ao carregar modal de exibição');
        const htmlModal = await respostaTemplate.text();
        abrirModal({
            titulo: `Pedido n° ${p.id}`,
            conteudoHtml: htmlModal
        });
        preencherModalExibicaoPedido(p);
    } catch (e) {
        alert(e.message);
    }
}

function preencherModalExibicaoPedido(pedido) {
    document.getElementById('pedido-id-exibicao').value = pedido.id;
    document.getElementById('pedido-cliente-exibicao').textContent = pedido.cliente?.nome || '';
    document.getElementById('pedido-status-exibicao').textContent = textoEnumPedido('statusPedido', pedido.status);
    document.getElementById('pedido-data-exibicao').textContent = formatarDataPedido(pedido.dataEntrega);
    document.getElementById('pedido-tipo-entrega-exibicao').textContent = textoEnumPedido('tiposEntrega', pedido.tipoEntrega);
    document.getElementById('pedido-pagamento-exibicao').textContent = textoEnumPedido('formasPagamento', pedido.formaPagamento);
    document.getElementById('pedido-frete-exibicao').textContent = moedaPedido(pedido.frete);
    document.getElementById('pedido-total-exibicao').textContent = moedaPedido(pedido.valorTotal);

    const lista = document.getElementById('pedido-itens-exibicao');
    (pedido.itens || []).forEach(item => {
        const linha = document.createElement('li');
        linha.textContent = `${item.quantidade} ${item.nomeSalgado} (${textoEnumPedido('tiposPrecos', item.tipoPreco)}) — ${moedaPedido(item.subTotal)}`;
        lista.appendChild(linha);
    });

    const blocoEndereco = document.getElementById('pedido-endereco-bloco-exibicao');
    if (pedido.tipoEntrega !== 'ENTREGA' || !pedido.enderecoEntrega) {
        blocoEndereco.classList.add('hidden');
        return;
    }
    const endereco = pedido.enderecoEntrega;
    document.getElementById('pedido-endereco-exibicao').textContent = `${endereco.logradouro}, ${endereco.numero}${endereco.complemento ? `, ${endereco.complemento}` : ''} - ${endereco.bairro}, ${endereco.cidade} - ${endereco.uf}, CEP ${endereco.cep}`;
}

async function editarPedidoPelaExibicao() {
    const id = Number(document.getElementById('pedido-id-exibicao').value);
    fecharModal();
    await editarPedido(id);
}

async function abrirStatusPedido(id) {
    try {
        const [respostaTemplate, p] = await Promise.all([
            fetch('pedidos/html/modal-alterar-status-pedido.html'),
            obterPedido(id)
        ]);
        if (!respostaTemplate.ok) throw new Error('Erro ao carregar modal de alteração de status');
        const htmlModal = await respostaTemplate.text();
        abrirModal({
            titulo: 'Alterar status do pedido',
            conteudoHtml: htmlModal
        });
        configurarModalStatusPedido(p);
    } catch (e) {
        alert(e.message);
    }
}

function configurarModalStatusPedido(pedido) {
    document.getElementById('pedido-id-status').value = pedido.id;
    document.querySelector('.mensagem-alterar-status-pedido').textContent = `Selecione o novo status do Pedido n° ${pedido.id}. Status atual: ${textoEnumPedido('statusPedido', pedido.status)}.`;
    const opcoes = enumsPedido.statusPedido.filter(status => status.valor !== pedido.status);
    preencherSelectPedido('novo-status-pedido', opcoes);
    document.querySelector('.btn-confirmar-status-pedido').onclick = () => {
        const status = document.getElementById('novo-status-pedido').value;
        if (!status) return alert('Selecione o novo status do pedido.');
        alterarStatusPedido(pedido.id, status);
    };
}

async function alterarStatusPedido(id, status) {
    const {response, data} = await apiJson(`/pedidos/${id}`, {method: 'PATCH', body: JSON.stringify({status})});
    if (response.ok) {
        fecharModal();
        listarPedidos(paginaPedidos);
    } else alert(data?.mensagem || 'Erro ao alterar status');
}

async function editarPedido(id) {
    try {
        const p = await obterPedido(id);
        pedidoEmEdicao = id;
        document.getElementById('tipo-entrega-pedido').value = p.tipoEntrega;
        alternarEnderecoPedido(p.tipoEntrega);
        document.getElementById('cliente-pedido').value = p.cliente?.id;
        const e = p.enderecoEntrega || {};
        ['cep', 'logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf'].forEach(c => document.getElementById(`${c}-pedido`).value = e[c] || '');
        document.getElementById('frete-pedido').value = p.frete || '';
        document.getElementById('data-entrega-pedido').value = p.dataEntrega?.slice(0, 10);
        document.getElementById('hora-entrega-pedido').value = p.dataEntrega?.slice(11, 16);
        document.getElementById('forma-pagamento-pedido').value = p.formaPagamento;
        itensNovoPedido = p.itens.map(i => ({
            salgadoId: salgadosPedido.find(s => s.nome === i.nomeSalgado)?.id,
            nomeSalgado: i.nomeSalgado,
            descricao: salgadosPedido.find(s => s.nome === i.nomeSalgado)?.descricao || '',
            quantidade: i.quantidade,
            tipoPreco: i.tipoPreco,
            precoUnitario: Number(i.precoUnitario)
        }));
        renderizarItensPedido();
        document.querySelector('#form-pedido button[type=submit]').textContent = 'Salvar alterações';
        document.getElementById('cancelar-edicao-pedido').classList.remove('hidden');
        document.getElementById('titulo-form-pedido').textContent = `Edição do Pedido n° ${p.id}`;
        definirModoEdicaoPedido(true);
        document.getElementById('form-pedido').scrollIntoView({behavior: 'smooth'});
    } catch (e) {
        alert(e.message);
    }
}

const areaPedido = document.getElementById('conteudo-dinamico');
areaPedido.addEventListener('pagina:carregada', e => {
    if (e.detail.modulo === 'pedido') carregarDadosIniciaisPedido();
});

areaPedido.addEventListener('click', e => {
    const botaoAcaoPedido = e.target.closest('[data-acao-pedido]');
    if (botaoAcaoPedido) {
        const id = Number(botaoAcaoPedido.closest('.card-pedido').dataset.pedidoId);
        if (botaoAcaoPedido.dataset.acaoPedido === 'editar') editarPedido(id);
        if (botaoAcaoPedido.dataset.acaoPedido === 'exibir') exibirPedido(id);
        if (botaoAcaoPedido.dataset.acaoPedido === 'status') abrirStatusPedido(id);
    }
    const status = e.target.dataset.statusPedido;
    if (status) {
        statusPedidoSelecionado = status;
        filtrarPedidosPorData = false;
        document.getElementById('form-filtro-pedidos').reset();
        document.getElementById('data-fim-pedido').disabled = true;
        montarFiltrosStatusPedido();
        listarPedidos(0);
    }
    if (e.target.id === 'adicionar-item-pedido') adicionarItemPedido();
    if (e.target.id === 'cancelar-edicao-pedido') {
        limparFormularioPedido();
        document.getElementById('titulo-form-pedido').scrollIntoView({behavior: 'smooth'});
    }
    if (e.target.id === 'buscar-todos-pedidos') {
        filtrarPedidosPorData = false;
        document.getElementById('form-filtro-pedidos').reset();
        document.getElementById('data-fim-pedido').disabled = true;
        listarPedidos(0);
    }
    if (e.target.dataset.removerItem !== undefined) {
        itensNovoPedido.splice(Number(e.target.dataset.removerItem), 1);
        renderizarItensPedido();
    }
    if (e.target.id === 'pedido-anterior') listarPedidos(paginaPedidos - 1);
    if (e.target.id === 'pedido-proximo') listarPedidos(paginaPedidos + 1);
});

areaPedido.addEventListener('change', e => {
    if (e.target.id === 'usar-data-final-pedido') {
        document.getElementById('data-fim-pedido').disabled = !e.target.checked;
        document.getElementById('data-fim-pedido').required = e.target.checked;
    }
    if (e.target.id === 'tipo-entrega-pedido') alternarEnderecoPedido(e.target.value);
    if (e.target.id === 'cliente-pedido') preencherEnderecoClientePedido(e.target.value);
    if (e.target.id === 'frete-pedido') atualizarTotalPedido();
});

areaPedido.addEventListener('focusout', e => {
    if (e.target.id === 'cep-pedido') preencherEnderecoPedidoPorCep(e.target.value);
});

areaPedido.addEventListener('submit', e => {
    if (e.target.id === 'form-filtro-pedidos') {
        e.preventDefault();
        filtrarPedidosPorData = true;
        listarPedidos(0);
    }
    if (e.target.id === 'form-pedido') {
        e.preventDefault();
        salvarPedido();
    }
});
