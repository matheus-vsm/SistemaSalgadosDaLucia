let templateCardSalgado = '';
let salgadoEmEdicao = null;
let salvandoSalgado = false;
let carregandoEdicaoSalgado = false;
let versaoFormularioSalgado = 0;
let paginaAtualSalgados = 0;
let salgadosAtivos = true;
let termoBuscaSalgados = '';
let categoriasSalgados = [];
let urlPreviewImagemSalgado = null;

async function listarSalgados(pagina = 0, tipo = salgadosAtivos, busca = termoBuscaSalgados) {
    const lista = document.getElementById('lista-salgados');
    if (!lista) return;

    salgadosAtivos = tipo;
    termoBuscaSalgados = busca.trim();
    atualizarFiltroSalgados();
    try {
        if (!templateCardSalgado) {
            const respostaCard = await fetch('salgado/html/card-salgado.html');
            if (!respostaCard.ok) throw new Error('Erro ao carregar o card de salgado.');
            templateCardSalgado = await respostaCard.text();
        }

        const endpoint = termoBuscaSalgados ? '/salgados/nome' : '/salgados';
        const parametros = termoBuscaSalgados
            ? `nome=${encodeURIComponent(termoBuscaSalgados)}&page=${pagina}&size=4`
            : `page=${pagina}&size=4&ativo=${tipo}`;
        const {
            response,
            data: salgadosPage
        } = await apiJson(`${endpoint}?${parametros}`);

        if (!lista.isConnected) return;

        if (!response.ok) {
            lista.innerHTML = '<div class="estado">Erro ao carregar salgados</div>';
            return;
        }

        const salgados = termoBuscaSalgados
            ? salgadosPage.content.filter(salgado => salgado.ativo === tipo)
            : salgadosPage.content;

        paginaAtualSalgados = salgadosPage.page.number;
        if (!salgados || salgados.length === 0) {
            if (pagina > 0) return listarSalgados(pagina - 1, tipo, busca);
            document.getElementById('pagina-atual-salgados').textContent = 'Página 1 de 1';
            document.getElementById('btn-anterior-salgados').disabled = true;
            document.getElementById('btn-proximo-salgados').disabled = true;
            lista.innerHTML = '<div class="estado">Nenhum salgado cadastrado</div>';
            return;
        }

        lista.innerHTML = salgados.map(salgado => criarCardSalgadoHTML(salgado)).join('');

        paginaAtualSalgados = salgadosPage.page.number;
        document.getElementById('pagina-atual-salgados').textContent = `Página ${paginaAtualSalgados + 1} de ${salgadosPage.page.totalPages}`;

        document.getElementById('btn-anterior-salgados').disabled = paginaAtualSalgados === 0;
        document.getElementById('btn-proximo-salgados').disabled = paginaAtualSalgados + 1 >= salgadosPage.page.totalPages;

        document.getElementById('btn-anterior-salgados').onclick = () => listarSalgados(paginaAtualSalgados - 1, salgadosAtivos, termoBuscaSalgados);
        document.getElementById('btn-proximo-salgados').onclick = () => listarSalgados(paginaAtualSalgados + 1, salgadosAtivos, termoBuscaSalgados);
    } catch (erro) {
        console.error('Erro ao listar salgados:', erro);
        lista.innerHTML = '<div class="estado">Erro ao conectar com o servidor</div>';
    }
}

function atualizarFiltroSalgados() {
    const btnAtivos = document.getElementById('btn-salgados-ativos');
    const btnInativos = document.getElementById('btn-salgados-inativos');

    if (!btnAtivos || !btnInativos) return;

    btnAtivos.classList.toggle('ativo-selecionado', salgadosAtivos);
    btnInativos.classList.toggle('inativo-selecionado', !salgadosAtivos);
    btnAtivos.setAttribute('aria-pressed', String(salgadosAtivos));
    btnInativos.setAttribute('aria-pressed', String(!salgadosAtivos));
}

function criarCardSalgadoHTML(salgado) {
    const template = document.createElement('template');
    template.innerHTML = templateCardSalgado.trim();

    const card = template.content.firstElementChild;
    card.dataset.salgadoId = salgado.id;
    card.querySelector('.salgado-nome-card').textContent = salgado.nome;
    card.querySelector('.salgado-descricao-card').textContent = salgado.descricao ?? '';
    card.querySelector('.salgado-congelado-card').textContent = `Cento Congelado: ${formatarPrecoSalgado(salgado.precoCentoCongelado)}`;
    card.querySelector('.salgado-processado-card').textContent = `Cento ${salgado.categoria === 'FRITO' ? 'Frito' : 'Assado'}: ${formatarPrecoSalgado(salgado.precoCentoProcessado)}`;
    card.querySelector('.estoque-card strong').textContent = salgado.estoque?.quantidade ?? 0;

    const ativo = salgado.ativo === true;
    const status = card.querySelector('.status');
    status.classList.add(ativo ? 'ativo' : 'inativo');
    status.textContent = ativo ? 'Ativo' : 'Inativo';

    card.querySelector('[data-acao-salgado="status"]').textContent = ativo ? 'Inativar' : 'Ativar';

    return card.outerHTML;
}

function formatarPrecoSalgado(preco) {
    return Number(preco).toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });
}

async function carregarCategoriasSalgado(idSelect) {
    const select = document.getElementById(idSelect);
    if (!select) return false;
    try {
        if (categoriasSalgados.length === 0) {
            const {response, data: enums} = await apiJson('/enums/salgado');

            if (!response.ok) {
                throw new Error('Erro ao carregar categorias');
            }

            categoriasSalgados = enums.categorias;
        }

        if (!select.isConnected) return false;
        const categoriaSelecionada = select.value;

        select.innerHTML = '<option value="">Selecione</option>';
        categoriasSalgados.forEach(categoria => {
            const option = document.createElement('option');
            option.value = categoria.valor; // define o valor JS do option - <option value="FRITO"></option>
            option.textContent = categoria.descricao; // define o texto do option que o usuário ve - <option value="FRITO">Frito</option>
            select.appendChild(option);
        });
        select.value = categoriaSelecionada;
        return true;
    } catch (erro) {
        console.error('Erro ao carregar categorias:', erro);
        if (select.isConnected) alert('Erro ao carregar categorias de salgado.');
        return false;
    }
}

async function salvarSalgado() {
    const form = document.getElementById('form-salgado');
    if (!form || salvandoSalgado || carregandoEdicaoSalgado) return;
    const id = salgadoEmEdicao;
    const versao = versaoFormularioSalgado;
    const dados = {
        nome: document.getElementById('nome-salgado').value,
        descricao: document.getElementById('descricao-salgado').value,
        categoria: document.getElementById('categoria-salgado').value,
        precoCentoCongelado: document.getElementById('preco-congelado-salgado').value,
        precoCentoProcessado: document.getElementById('preco-processado-salgado').value
    };
    salvandoSalgado = true;
    form.querySelectorAll('button').forEach(botao => botao.disabled = true);
    form.querySelector('fieldset').disabled = true;
    try {
        const {response, data: resultado} = await apiJson(id ? `/salgados/${id}` : '/salgados', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(dados)
        });
        if (!form.isConnected || versao !== versaoFormularioSalgado) return;
        if (!response.ok) throw new Error(resultado?.mensagem || 'Erro ao salvar salgado');
        alert(id ? 'Salgado editado com sucesso!' : 'Salgado cadastrado com sucesso!');
        limparFormularioSalgado();
        await listarSalgados(paginaAtualSalgados, salgadosAtivos, termoBuscaSalgados);
    } catch (erro) {
        if (form.isConnected && versao === versaoFormularioSalgado) {
            alert(erro.message || 'Erro ao conectar com o servidor.');
        }
    } finally {
        if (form.isConnected) {
            salvandoSalgado = false;
            form.querySelectorAll('button').forEach(botao => botao.disabled = false);
            form.querySelector('fieldset').disabled = false;
        }
    }
}

function definirModoEdicaoSalgado(ativo) {
    document.body.classList.toggle('edicao-salgado-ativa', ativo);
    const lista = document.querySelector('.listagem-salgados');
    const navegacao = document.querySelector('.sidebar nav');
    if (lista) lista.inert = ativo;
    if (navegacao) navegacao.inert = ativo;
}

function limparFormularioSalgado() {
    versaoFormularioSalgado++;
    salgadoEmEdicao = null;
    const form = document.getElementById('form-salgado');
    if (!form) return;
    form.reset();
    document.getElementById('titulo-form-salgado').textContent = 'Cadastro de Salgado';
    form.querySelector('button[type=submit]').textContent = 'Cadastrar Salgado';
    document.getElementById('cancelar-edicao-salgado').classList.add('hidden');
    definirModoEdicaoSalgado(false);
    limparPreviewImagemSalgado();
}

async function iniciarEdicaoSalgado(id) {
    const form = document.getElementById('form-salgado');
    if (!form || salvandoSalgado || carregandoEdicaoSalgado || salgadoEmEdicao !== null) return;
    const versao = versaoFormularioSalgado;
    carregandoEdicaoSalgado = true;
    form.querySelector('button[type=submit]').disabled = true;
    try {
        const {response, data: salgado} = await apiJson(`/salgados/${id}`);
        if (!form.isConnected || versao !== versaoFormularioSalgado) return;
        if (!response.ok) throw new Error('Erro ao carregar dados do salgado');
        if (!await carregarCategoriasSalgado('categoria-salgado')) return;
        if (!form.isConnected || versao !== versaoFormularioSalgado) return;
        limparFormularioSalgado();
        document.getElementById('nome-salgado').value = salgado.nome ?? '';
        document.getElementById('descricao-salgado').value = salgado.descricao ?? '';
        document.getElementById('preco-congelado-salgado').value = salgado.precoCentoCongelado ?? '';
        document.getElementById('preco-processado-salgado').value = salgado.precoCentoProcessado ?? '';
        document.getElementById('categoria-salgado').value = salgado.categoria;
        salgadoEmEdicao = salgado.id;
        document.getElementById('titulo-form-salgado').textContent = `Edição do Salgado n° ${salgado.id}`;
        form.querySelector('button[type=submit]').textContent = 'Salvar alterações';
        document.getElementById('cancelar-edicao-salgado').classList.remove('hidden');
        definirModoEdicaoSalgado(true);
        form.scrollIntoView({behavior: 'smooth'});
        document.getElementById('nome-salgado').focus({preventScroll: true});
    } catch (erro) {
        if (form.isConnected && versao === versaoFormularioSalgado) alert(erro.message);
    } finally {
        if (form.isConnected) {
            carregandoEdicaoSalgado = false;
            form.querySelector('button[type=submit]').disabled = false;
        }
    }
}

async function abrirModalExibicaoSalgado(id) {
    try {
        const response = await fetch('salgado/html/modal-exibir-salgado.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de exibição');

        const htmlModal = await response.text();

        abrirModal({
            titulo: 'Exibir Salgado',
            conteudoHtml: htmlModal
        });
        await buscarSalgadoPorId(id, 'exibicao');
    } catch (erro) {
        console.error('Erro ao abrir modal de exibição:', erro);
        alert('Erro ao abrir modal de exibição.');
    }
}

async function abrirModalAtualizarEstoque(id) {
    try {
        const response = await fetch('salgado/html/modal-atualizar-estoque.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de estoque');

        const htmlModal = await response.text();
        const {response: respostaSalgado, data: salgado} = await apiJson(`/salgados/${id}`);

        if (!respostaSalgado.ok) {
            throw new Error('Erro ao carregar dados do salgado');
        }

        abrirModal({
            titulo: 'Atualizar Estoque',
            conteudoHtml: htmlModal
        });

        document.getElementById('salgado-id-estoque').value = salgado.id;
        document.getElementById('nome-salgado-estoque').textContent = salgado.nome;
        document.getElementById('quantidade-atual-estoque').textContent = salgado.estoque?.quantidade ?? 0;
    } catch (erro) {
        console.error('Erro ao abrir modal de estoque:', erro);
        alert('Erro ao abrir modal de atualização de estoque.');
    }
}

async function abrirModalAlterarStatusSalgado(id) {
    try {
        const response = await fetch('salgado/html/modal-alterar-status-salgado.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de alteração de status');

        const htmlModal = await response.text();
        const {response: respostaSalgado, data: salgado} = await apiJson(`/salgados/${id}`);

        if (!respostaSalgado.ok) {
            throw new Error('Erro ao carregar dados do salgado');
        }

        const ativar = salgado.ativo !== true;
        const estado = ativar ? 'ativar' : 'inativar';
        abrirModal({
            titulo: `${estado[0].toUpperCase()}${estado.slice(1)} Salgado`,
            conteudoHtml: htmlModal
        });

        configurarModalAlterarStatusSalgado(id, salgado, ativar, estado);
    } catch (erro) {
        console.error('Erro ao abrir modal de alteração de status:', erro);
        alert('Erro ao abrir modal de alteração de status.');
    }
}

function configurarModalAlterarStatusSalgado(id, salgado, ativar, estado) {
    const conteudoStatus = document.getElementById('modal-alterar-status-salgado');
    const mensagem = conteudoStatus.querySelector('.mensagem-alterar-status');
    const botaoConfirmar = conteudoStatus.querySelector('.btn-confirmar-status');

    mensagem.textContent = `Tem certeza que deseja ${estado} o salgado ${salgado?.nome}?`;
    botaoConfirmar.onclick = () => alterarStatusSalgado(id, ativar);
}

async function buscarSalgadoPorId(id, tipo) {
    try {
        const {response, data: salgado} = await apiJson(`/salgados/${id}`);

        if (!response.ok) {
            alert('Erro ao carregar dados do salgado');
            return;
        }
        await preencherDadosSalgado(salgado, tipo);
    } catch (erro) {
        console.error('Erro ao buscar salgado para edição:', erro);
        alert('Erro ao conectar com o servidor.');
    }
}

function preencherCampoSalgado(idCampo, valor) {
    const campo = document.getElementById(idCampo);

    if (!campo) {
        console.warn(`Campo não encontrado: ${idCampo}`);
        return;
    }

    if (campo.tagName === 'SPAN') {
        campo.textContent = valor ?? '';
    } else {
        campo.value = valor ?? '';
    }
}

async function preencherDadosSalgado(salgado, tipo) {
    preencherCampoSalgado(`salgado-id-${tipo}`, salgado.id);
    preencherCampoSalgado(`nome-salgado-${tipo}`, salgado.nome);
    preencherCampoSalgado(`descricao-salgado-${tipo}`, salgado.descricao);
    preencherCampoSalgado(`preco-congelado-salgado-${tipo}`, formatarPrecoSalgado(salgado.precoCentoCongelado));
    preencherCampoSalgado(`preco-processado-salgado-${tipo}`, formatarPrecoSalgado(salgado.precoCentoProcessado));

    const categoria = categoriasSalgados.find(item => item.valor === salgado.categoria);
    preencherCampoSalgado('categoria-salgado-exibicao', categoria?.descricao ?? salgado.categoria);
    preencherCampoSalgado('quantidade-estoque-exibicao', salgado.estoque?.quantidade ?? 0);
}

async function editarSalgadoPelaExibicao() {
    const id = document.getElementById('salgado-id-exibicao').value;
    fecharModal();
    await iniciarEdicaoSalgado(id);
}

async function alterarStatusSalgado(id, estado) {
    try {
        const {response, data: resultado} = await apiJson(`/salgados/${id}`, {
            method: 'PATCH',
            body: JSON.stringify({status: estado})
        });

        if (response.ok) {
            alert(`Salgado ${estado ? 'ativado' : 'inativado'} com sucesso!`);
            fecharModal();
            listarSalgados();
        } else {
            alert(resultado?.mensagem || `Erro ao ${estado ? 'ativar' : 'inativar'} salgado`);
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function atualizarEstoqueSalgado() {
    const id = document.getElementById('salgado-id-estoque').value;
    const quantidade = Number(document.getElementById('ajuste-estoque').value);

    if (!Number.isInteger(quantidade) || quantidade === 0) {
        alert('Informe um número inteiro diferente de zero. Use um valor negativo para remover do estoque.');
        return;
    }

    try {
        const {response, data: resultado} = await apiJson(`/estoque/${id}`, {
            method: 'PATCH',
            body: JSON.stringify({quantidade})
        });

        if (response.ok) {
            alert('Estoque atualizado com sucesso!');
            fecharModal();
            listarSalgados(paginaAtualSalgados, salgadosAtivos, termoBuscaSalgados);
        } else {
            alert(resultado?.message || 'Erro ao atualizar estoque');
        }
    } catch (erro) {
        console.error('Erro ao atualizar estoque:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

function limparPreviewImagemSalgado() {
    if (urlPreviewImagemSalgado) URL.revokeObjectURL(urlPreviewImagemSalgado);
    urlPreviewImagemSalgado = null;
    const preview = document.getElementById('imagem-preview-salgado');
    if (!preview) return;
    preview.removeAttribute('src');
    preview.classList.add('hidden');
    document.getElementById('texto-selecao-imagem').classList.remove('hidden');
    document.getElementById('nome-arquivo-imagem').textContent = '';
}

function exibirPreviewImagemSalgado(event) {
    limparPreviewImagemSalgado();
    const arquivo = event.target.files[0];
    if (!arquivo) return;
    const preview = document.getElementById('imagem-preview-salgado');
    urlPreviewImagemSalgado = URL.createObjectURL(arquivo);
    preview.src = urlPreviewImagemSalgado;
    preview.classList.remove('hidden');
    document.getElementById('texto-selecao-imagem').classList.add('hidden');
    document.getElementById('nome-arquivo-imagem').textContent = arquivo.name;
}

const conteudoDinamicoSalgados = document.getElementById('conteudo-dinamico');

conteudoDinamicoSalgados.addEventListener('pagina:carregada', (event) => {
    limparPreviewImagemSalgado();
    versaoFormularioSalgado++;
    salgadoEmEdicao = null;
    salvandoSalgado = false;
    carregandoEdicaoSalgado = false;
    if (document.body.classList.contains('edicao-salgado-ativa')) definirModoEdicaoSalgado(false);
    if (event.detail.modulo === 'salgado') {
        limparFormularioSalgado();
        termoBuscaSalgados = '';
        paginaAtualSalgados = 0;
        carregarCategoriasSalgado('categoria-salgado');
        listarSalgados(0, true);
    }
});

conteudoDinamicoSalgados.addEventListener('click', (event) => {
    if (event.target.id === 'cancelar-edicao-salgado' && !salvandoSalgado) {
        limparFormularioSalgado();
        document.getElementById('nome-salgado').focus({preventScroll: true});
    }
    if (event.target.id === 'btn-salgados-ativos') {
        listarSalgados(0, true);
    } else if (event.target.id === 'btn-salgados-inativos') {
        termoBuscaSalgados = '';
        const campoBusca = document.getElementById('busca-salgados');
        if (campoBusca) campoBusca.value = '';
        listarSalgados(0, false, '');
    }
});

document.addEventListener('change', (event) => {
    if (event.target.id !== 'imagem-salgado') return;
    exibirPreviewImagemSalgado(event);
});

conteudoDinamicoSalgados.addEventListener('submit', async (event) => {
    if (event.target.id === 'form-busca-salgados') {
        event.preventDefault();
        const busca = document.getElementById('busca-salgados').value;
        await listarSalgados(0, true, busca);
    } else if (event.target.id === 'form-salgado') {
        event.preventDefault();
        await salvarSalgado();
    } else {
        return;
    }
});

document.getElementById('conteudo-dinamico').addEventListener('click', event => {
    const botao = event.target.closest('[data-acao-salgado]');
    if (!botao) return;
    const id = Number(botao.closest('[data-salgado-id]').dataset.salgadoId);
    if (botao.dataset.acaoSalgado === 'status') abrirModalAlterarStatusSalgado(id);
    if (botao.dataset.acaoSalgado === 'exibir') abrirModalExibicaoSalgado(id);
    if (botao.dataset.acaoSalgado === 'estoque') abrirModalAtualizarEstoque(id);
    if (botao.dataset.acaoSalgado === 'editar') iniciarEdicaoSalgado(id);
});
