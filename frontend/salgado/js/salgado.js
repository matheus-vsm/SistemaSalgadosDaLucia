let paginaAtualSalgados = 0;
let salgadosAtivos = true;
let termoBuscaSalgados = '';
let categoriasSalgados = [];

async function listarSalgados(pagina = 0, tipo = salgadosAtivos, busca = termoBuscaSalgados) {
    const lista = document.getElementById('lista-salgados');
    if (!lista) return;

    salgadosAtivos = tipo;
    termoBuscaSalgados = busca.trim();
    atualizarFiltroSalgados();
    try {
        const endpoint = termoBuscaSalgados ? '/salgados/nome' : '/salgados';
        const parametros = termoBuscaSalgados
            ? `nome=${encodeURIComponent(termoBuscaSalgados)}&page=${pagina}&size=2`
            : `page=${pagina}&size=2&ativo=${tipo}`;
        const {
            response,
            data: salgadosPage
        } = await apiJson(`${endpoint}?${parametros}`);

        if (!response.ok) {
            lista.innerHTML = '<div class="estado">Erro ao carregar salgados</div>';
            return;
        }

        const salgados = termoBuscaSalgados
            ? salgadosPage.content.filter(salgado => salgado.ativo === tipo)
            : salgadosPage.content;

        if (!salgados || salgados.length === 0) {
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
    const ativo = salgado.ativo === true;
    const statusClasse = ativo ? 'ativo' : 'inativo';
    const statusTexto = ativo ? 'Ativo' : 'Inativo';
    const statusBotao = ativo ? 'Inativar' : 'Ativar';
    const tipo = salgado.categoria === "FRITO";
    const tipoTexto = tipo ? 'Frito' : 'Assado';

    return `
      <div class="card">
        <img class="imagem-salgado" src="imagens/coxinha.jpg" alt="Imagem do salgado">
        <div class="card-info">
          <h2>${salgado.nome}</h2>
          <p>${salgado.descricao ?? ''}</p>
          <p>Cento Congelado: ${formatarPrecoSalgado(salgado.precoCentoCongelado)}</p>
          <p>Cento ${tipoTexto}: ${formatarPrecoSalgado(salgado.precoCentoProcessado)}</p>
        </div>
        <span class="status ${statusClasse}">${statusTexto}</span>
        <button type="button" class="btn btn-secundario btn-editar" onclick="abrirModalEdicaoSalgado(${salgado.id})">Editar</button>
        <button type="button" class="btn btn-secundario btn-exibir" onclick="abrirModalExibicaoSalgado(${salgado.id})">Exibir</button>
        <button type="button" class="btn btn-secundario btn-alterarStatus" onclick="abrirModalAlterarStatusSalgado(${salgado.id})">${statusBotao}</button>
      </div>
    `;
}

function formatarPrecoSalgado(preco) {
    return Number(preco).toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });
}

async function carregarCategoriasSalgado(idSelect, categoriaSelecionada = '') {
    try {
        if (categoriasSalgados.length === 0) {
            const {response, data: enums} = await apiJson('/enums/salgado');

            if (!response.ok) {
                throw new Error('Erro ao carregar categorias');
            }

            categoriasSalgados = enums.categorias;
        }

        const select = document.getElementById(idSelect);
        if (!select) return;

        select.innerHTML = '<option value="">Selecione</option>';
        categoriasSalgados.forEach(categoria => {
            const option = document.createElement('option');
            option.value = categoria.valor; // define o valor JS do option - <option value="FRITO"></option>
            option.textContent = categoria.descricao; // define o texto do option que o usuário ve - <option value="FRITO">Frito</option>
            select.appendChild(option);
        });
        select.value = categoriaSelecionada;
    } catch (erro) {
        console.error('Erro ao carregar categorias:', erro);
        alert('Erro ao carregar categorias de salgado.');
    }
}

async function cadastrarSalgado() {
    const dados = {
        nome: document.getElementById('nome-salgado').value,
        descricao: document.getElementById('descricao-salgado').value,
        categoria: document.getElementById('categoria-salgado').value,
        precoCentoCongelado: document.getElementById('preco-congelado-salgado').value,
        precoCentoProcessado: document.getElementById('preco-processado-salgado').value
    };

    try {
        const {
            response,
            data: resultado
        } = await apiJson('/salgados', {
            method: 'POST',
            body: JSON.stringify(dados)
        });

        if (response.ok) {
            alert('Salgado cadastrado com sucesso!');
            document.getElementById('form-salgado').reset();
            document.getElementById('imagem-preview-salgado').removeAttribute('src');
            document.getElementById('imagem-preview-salgado').classList.add('hidden');
            document.getElementById('texto-selecao-imagem').classList.remove('hidden');
            document.getElementById('nome-arquivo-imagem').textContent = '';
            carregarCategoriasSalgado('categoria-salgado');
            listarSalgados();
        } else {
            alert(resultado?.mensagem || 'Erro ao cadastrar salgado');
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function abrirModalEdicaoSalgado(id) {
    try {
        const response = await fetch('salgado/html/modal-editar-salgado.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de edição');

        const htmlModal = await response.text();

        abrirModal({
            titulo: 'Editar Salgado',
            conteudoHtml: htmlModal
        });
        await buscarSalgadoPorId(id, 'edicao');
    } catch (erro) {
        console.error('Erro ao abrir modal de edição:', erro);
        alert('Erro ao abrir modal de edição.');
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
    preencherCampoSalgado(`preco-congelado-salgado-${tipo}`, tipo === 'exibicao' ? formatarPrecoSalgado(salgado.precoCentoCongelado) : salgado.precoCentoCongelado);
    preencherCampoSalgado(`preco-processado-salgado-${tipo}`, tipo === 'exibicao' ? formatarPrecoSalgado(salgado.precoCentoProcessado) : salgado.precoCentoProcessado);

    if (tipo === 'edicao') {
        await carregarCategoriasSalgado('categoria-salgado-edicao', salgado.categoria);
    } else {
        const categoria = categoriasSalgados.find(item => item.valor === salgado.categoria);
        preencherCampoSalgado('categoria-salgado-exibicao', categoria?.descricao ?? salgado.categoria);
    }
}

async function editarSalgado() {
    const id = document.getElementById('salgado-id-edicao').value;
    const dados = {
        nome: document.getElementById('nome-salgado-edicao').value,
        descricao: document.getElementById('descricao-salgado-edicao').value,
        categoria: document.getElementById('categoria-salgado-edicao').value,
        precoCentoCongelado: document.getElementById('preco-congelado-salgado-edicao').value,
        precoCentoProcessado: document.getElementById('preco-processado-salgado-edicao').value
    };

    try {
        const {response, data: resultado} = await apiJson(`/salgados/${id}`, {
            method: 'PUT',
            body: JSON.stringify(dados)
        });

        if (response.ok) {
            alert('Salgado editado com sucesso!');
            document.getElementById('form-salgado-edicao').reset();
            fecharModal();
            listarSalgados();
        } else {
            alert(resultado?.mensagem || 'Erro ao editar salgado');
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function editarSalgadoPelaExibicao() {
    const id = document.getElementById('salgado-id-exibicao').value;
    fecharModal();
    await abrirModalEdicaoSalgado(id);
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

function exibirPreviewImagemSalgado(event) {
    const arquivo = event.target.files[0];
    if (!arquivo) return;

    const idPreview = event.target.id === 'imagem-salgado-edicao'
        ? 'imagem-preview-salgado-edicao'
        : 'imagem-preview-salgado';
    const preview = document.getElementById(idPreview);
    preview.src = URL.createObjectURL(arquivo);
    preview.classList.remove('hidden');

    if (event.target.id === 'imagem-salgado') {
        document.getElementById('texto-selecao-imagem').classList.add('hidden');
        document.getElementById('nome-arquivo-imagem').textContent = arquivo.name;
    }
}

const conteudoDinamicoSalgados = document.getElementById('conteudo-dinamico');

conteudoDinamicoSalgados.addEventListener('pagina:carregada', (event) => {
    if (event.detail.modulo === 'salgado') {
        carregarCategoriasSalgado('categoria-salgado');
        listarSalgados(0, true);
    }
});

conteudoDinamicoSalgados.addEventListener('click', (event) => {
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
    if (event.target.id !== 'imagem-salgado' && event.target.id !== 'imagem-salgado-edicao') return;
    exibirPreviewImagemSalgado(event);
});

conteudoDinamicoSalgados.addEventListener('submit', async (event) => {
    if (event.target.id === 'form-busca-salgados') {
        event.preventDefault();
        const busca = document.getElementById('busca-salgados').value;
        await listarSalgados(0, true, busca);
    } else if (event.target.id === 'form-salgado') {
        event.preventDefault();
        await cadastrarSalgado();
    } else if (event.target.id === 'form-salgado-edicao') {
        event.preventDefault();
        await editarSalgado();
    } else {
        return;
    }
});
