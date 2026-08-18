let paginaAtualSalgados = 0;
let salgadosAtivos = true;
let termoBuscaSalgados = '';
let categoriasSalgado = [];

const CAMINHO_IMAGEM_SALGADO = 'imagens/coxinha.jpg';

function escaparHtml(valor) {
    const elemento = document.createElement('div');
    elemento.textContent = valor ?? '';
    return elemento.innerHTML;
}

function formatarMoeda(valor) {
    return Number(valor ?? 0).toLocaleString('pt-BR', {style: 'currency', currency: 'BRL'});
}

function descricaoCategoria(valor) {
    return categoriasSalgado.find(categoria => categoria.valor === valor)?.descricao ?? valor ?? '';
}

async function carregarCategoriasSalgado(selectId = 'categoria-salgado', valorSelecionado = '') {
    if (categoriasSalgado.length === 0) {
        const {response, data} = await apiJson('/enums/salgado');
        if (!response.ok) throw new Error('Erro ao carregar categorias');
        categoriasSalgado = data.categorias ?? [];
    }

    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '<option value="">Selecione</option>' + categoriasSalgado.map(categoria =>
        `<option value="${escaparHtml(categoria.valor)}">${escaparHtml(categoria.descricao)}</option>`
    ).join('');
    select.value = valorSelecionado;
}

async function listarSalgados(pagina = 0, ativos = salgadosAtivos, busca = termoBuscaSalgados) {
    const lista = document.getElementById('lista-salgados');
    if (!lista) return;

    salgadosAtivos = ativos;
    termoBuscaSalgados = busca.trim();
    atualizarFiltroSalgados();
    lista.innerHTML = '<div class="estado">Carregando salgados...</div>';

    try {
        const endpoint = termoBuscaSalgados ? '/salgados/nome' : '/salgados';
        const parametros = termoBuscaSalgados
            ? `nome=${encodeURIComponent(termoBuscaSalgados)}&page=${pagina}&size=10`
            : `ativo=${ativos}&page=${pagina}&size=2`;
        const {response, data} = await apiJson(`${endpoint}?${parametros}`);

        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar salgados');

        const salgados = termoBuscaSalgados
            ? (data.content ?? []).filter(salgado => salgado.ativo === ativos)
            : data.content;

        lista.innerHTML = salgados?.length
            ? salgados.map(criarCardSalgadoHTML).join('')
            : '<div class="estado">Nenhum salgado encontrado</div>';

        paginaAtualSalgados = data.page?.number ?? data.number ?? 0;
        const totalPaginas = data.page?.totalPages ?? data.totalPages ?? 0;
        document.getElementById('pagina-atual-salgados').textContent = totalPaginas
            ? `Página ${paginaAtualSalgados + 1} de ${totalPaginas}` : '';
        document.getElementById('btn-salgados-anterior').disabled = paginaAtualSalgados === 0;
        document.getElementById('btn-salgados-proximo').disabled = paginaAtualSalgados + 1 >= totalPaginas;
    } catch (erro) {
        console.error('Erro ao listar salgados:', erro);
        lista.innerHTML = '<div class="estado">Erro ao carregar salgados</div>';
    }
}

function atualizarFiltroSalgados() {
    const ativos = document.getElementById('btn-salgados-ativos');
    const inativos = document.getElementById('btn-salgados-inativos');
    ativos?.classList.toggle('ativo-selecionado', salgadosAtivos);
    inativos?.classList.toggle('inativo-selecionado', !salgadosAtivos);
    ativos?.setAttribute('aria-pressed', String(salgadosAtivos));
    inativos?.setAttribute('aria-pressed', String(!salgadosAtivos));
}

function criarCardSalgadoHTML(salgado) {
    const ativo = salgado.ativo === true;
    return `<article class="card card-salgado">
        <img class="imagem-salgado-card" src="${CAMINHO_IMAGEM_SALGADO}" alt="Imagem de ${escaparHtml(salgado.nome)}">
        <div class="card-info">
            <h2>${escaparHtml(salgado.nome)}</h2>
            <p>${escaparHtml(salgado.descricao)}</p>
            <p>Cento congelado: ${formatarMoeda(salgado.precoCentoCongelado)}</p>
            <p>Cento frito/assado: ${formatarMoeda(salgado.precoCentoProcessado)}</p>
        </div>
        <span class="status ${ativo ? 'ativo' : 'inativo'}">${ativo ? 'Ativo' : 'Inativo'}</span>
        <div class="card-acoes">
            <button type="button" class="btn btn-secundario" onclick="abrirModalEdicaoSalgado(${salgado.id})">Editar</button>
            <button type="button" class="btn btn-secundario" onclick="abrirModalExibicaoSalgado(${salgado.id})">Exibir</button>
            <button type="button" class="btn btn-secundario" onclick="abrirModalStatusSalgado(${salgado.id})">${ativo ? 'Inativar' : 'Ativar'}</button>
        </div>
    </article>`;
}

function obterDadosFormularioSalgado(sufixo = '') {
    return {
        nome: document.getElementById(`nome-salgado${sufixo}`).value.trim(),
        descricao: document.getElementById(`descricao-salgado${sufixo}`).value.trim(),
        categoria: document.getElementById(`categoria-salgado${sufixo}`).value,
        precoCentoCongelado: Number(document.getElementById(`preco-congelado-salgado${sufixo}`).value),
        precoCentoProcessado: Number(document.getElementById(`preco-processado-salgado${sufixo}`).value)
    };
}

async function salvarSalgado(endpoint, method, dados, sucesso) {
    try {
        const {response, data} = await apiJson(endpoint, {method, body: JSON.stringify(dados)});
        if (!response.ok) return alert(data?.mensagem || 'Não foi possível salvar o salgado.');
        alert(sucesso);
        fecharModal();
        await listarSalgados(0, salgadosAtivos, termoBuscaSalgados);
        return true;
    } catch (erro) {
        console.error('Erro ao salvar salgado:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function cadastrarSalgado() {
    const salvo = await salvarSalgado('/salgados', 'POST', obterDadosFormularioSalgado(), 'Salgado cadastrado com sucesso!');
    if (salvo) {
        document.getElementById('form-salgado').reset();
        restaurarPreview('preview-imagem-salgado');
        await carregarCategoriasSalgado();
    }
}

async function carregarFragmento(nome) {
    const response = await fetch(`salgado/html/${nome}.html`);
    if (!response.ok) throw new Error('Erro ao carregar modal');
    return response.text();
}

async function buscarSalgado(id) {
    const {response, data} = await apiJson(`/salgados/${id}`);
    if (!response.ok) throw new Error(data?.mensagem || 'Erro ao buscar salgado');
    return data;
}

async function abrirModalEdicaoSalgado(id) {
    try {
        const [html, salgado] = await Promise.all([carregarFragmento('modal-editar-salgado'), buscarSalgado(id)]);
        abrirModal({titulo: 'Editar Salgado', conteudoHtml: html});
        document.getElementById('salgado-id-edicao').value = salgado.id;
        document.getElementById('nome-salgado-edicao').value = salgado.nome;
        document.getElementById('descricao-salgado-edicao').value = salgado.descricao;
        document.getElementById('preco-congelado-salgado-edicao').value = salgado.precoCentoCongelado;
        document.getElementById('preco-processado-salgado-edicao').value = salgado.precoCentoProcessado;
        await carregarCategoriasSalgado('categoria-salgado-edicao', salgado.categoria);
    } catch (erro) {
        console.error(erro);
        alert('Erro ao abrir a edição do salgado.');
    }
}

async function editarSalgado() {
    const id = document.getElementById('salgado-id-edicao').value;
    await salvarSalgado(`/salgados/${id}`, 'PUT', obterDadosFormularioSalgado('-edicao'), 'Salgado editado com sucesso!');
}

async function abrirModalExibicaoSalgado(id) {
    try {
        const [html, salgado] = await Promise.all([carregarFragmento('modal-exibir-salgado'), buscarSalgado(id)]);
        abrirModal({titulo: 'Exibir Salgado', conteudoHtml: html});
        document.getElementById('salgado-id-exibicao').value = salgado.id;
        document.getElementById('nome-salgado-exibicao').textContent = salgado.nome;
        document.getElementById('descricao-salgado-exibicao').textContent = salgado.descricao;
        document.getElementById('categoria-salgado-exibicao').textContent = descricaoCategoria(salgado.categoria);
        document.getElementById('preco-congelado-salgado-exibicao').textContent = formatarMoeda(salgado.precoCentoCongelado);
        document.getElementById('preco-processado-salgado-exibicao').textContent = formatarMoeda(salgado.precoCentoProcessado);
        document.getElementById('status-salgado-exibicao').textContent = salgado.ativo ? 'Ativo' : 'Inativo';
    } catch (erro) {
        console.error(erro);
        alert('Erro ao exibir o salgado.');
    }
}

async function editarSalgadoPelaExibicao() {
    const id = document.getElementById('salgado-id-exibicao').value;
    fecharModal();
    await abrirModalEdicaoSalgado(id);
}

async function abrirModalStatusSalgado(id) {
    try {
        const [html, salgado] = await Promise.all([carregarFragmento('modal-alterar-status-salgado'), buscarSalgado(id)]);
        const ativar = salgado.ativo !== true;
        const acao = ativar ? 'ativar' : 'inativar';
        abrirModal({titulo: `${ativar ? 'Ativar' : 'Inativar'} Salgado`, conteudoHtml: html});
        document.querySelector('#modal-alterar-status-salgado .mensagem-alterar-status').textContent =
            `Tem certeza que deseja ${acao} o salgado ${salgado.nome}?`;
        document.querySelector('#modal-alterar-status-salgado .btn-confirmar-status').onclick = () => alterarStatusSalgado(id, ativar);
    } catch (erro) {
        console.error(erro);
        alert('Erro ao abrir a alteração de status.');
    }
}

async function alterarStatusSalgado(id, status) {
    await salvarSalgado(`/salgados/${id}`, 'PATCH', {status}, `Salgado ${status ? 'ativado' : 'inativado'} com sucesso!`);
}

function restaurarPreview(id) {
    const preview = document.getElementById(id);
    if (preview) preview.src = CAMINHO_IMAGEM_SALGADO;
}

function atualizarPreviewImagem(input) {
    const previewId = input.id.endsWith('-edicao') ? 'preview-imagem-salgado-edicao' : 'preview-imagem-salgado';
    const arquivo = input.files?.[0];
    if (!arquivo) return restaurarPreview(previewId);
    const leitor = new FileReader();
    leitor.onload = () => { document.getElementById(previewId).src = leitor.result; };
    leitor.readAsDataURL(arquivo);
}

const conteudoSalgados = document.getElementById('conteudo-dinamico');

conteudoSalgados.addEventListener('pagina:carregada', async event => {
    if (event.detail.modulo !== 'salgado') return;
    try {
        await Promise.all([carregarCategoriasSalgado(), listarSalgados(0, true, '')]);
    } catch (erro) {
        console.error(erro);
    }
});

conteudoSalgados.addEventListener('click', event => {
    if (event.target.id === 'btn-salgados-ativos') listarSalgados(0, true, termoBuscaSalgados);
    if (event.target.id === 'btn-salgados-inativos') listarSalgados(0, false, termoBuscaSalgados);
    if (event.target.id === 'btn-salgados-anterior') listarSalgados(paginaAtualSalgados - 1);
    if (event.target.id === 'btn-salgados-proximo') listarSalgados(paginaAtualSalgados + 1);
});

document.addEventListener('change', event => {
    if (event.target.matches('#imagem-salgado, #imagem-salgado-edicao')) atualizarPreviewImagem(event.target);
});

document.addEventListener('submit', async event => {
    if (event.target.id === 'form-busca-salgados') {
        event.preventDefault();
        await listarSalgados(0, salgadosAtivos, document.getElementById('busca-salgados').value);
    } else if (event.target.id === 'form-salgado') {
        event.preventDefault();
        await cadastrarSalgado();
    } else if (event.target.id === 'form-salgado-edicao') {
        event.preventDefault();
        await editarSalgado();
    }
});
