let templateCardCliente = '';
let clienteEmEdicao = null;
let salvandoCliente = false;
let carregandoEdicaoCliente = false;
let versaoFormularioCliente = 0;
let paginaAtual = 0;
let clientesAtivos = true;
let termoBusca = '';

async function listarClientes(pagina = 0, tipo = clientesAtivos, busca = termoBusca) {
    const lista = document.getElementById('lista');
    if (!lista) return;
    const carregamento = iniciarCarregamentoListagem(lista);

    clientesAtivos = tipo;
    termoBusca = busca.trim();
    atualizarFiltroClientes();

    try {
        if (!templateCardCliente) {
            const respostaCard = await fetch('cliente/html/card-cliente.html');
            if (!respostaCard.ok) throw new Error('Erro ao carregar o card de cliente.');
            templateCardCliente = await respostaCard.text();
        }

        const endpoint = termoBusca ? '/clientes/nome' : '/clientes';
        const parametros = termoBusca
            ? `nome=${encodeURIComponent(termoBusca)}&ativo=${tipo}&page=${pagina}&size=4`
            : `page=${pagina}&size=4&ativo=${tipo}`;
        const {
            data: clientesPage // os dados retornados (clientes) - salva na variavel clientesPage
        } = await apiJson(`${endpoint}?${parametros}`);

        if (!carregamento.atual()) return;

        const clientes = termoBusca
            ? clientesPage.content.filter(cliente => cliente.ativo === tipo)
            : clientesPage.content;

        paginaAtual = clientesPage.page.number;

        if (!clientes || clientes.length === 0) {
            if (pagina > 0) return listarClientes(pagina - 1, tipo, busca);
            document.getElementById('pagina-atual').textContent = 'Página 1 de 1';
            document.getElementById('btn-anterior').disabled = true;
            document.getElementById('btn-proximo').disabled = true;
            lista.innerHTML = '<div class="estado">Nenhum cliente cadastrado</div>';

            return;
        }

        lista.innerHTML = clientes.map(cliente => criarCardHTML(cliente)).join('');

        // Atualiza paginação
        paginaAtual = clientesPage.page.number;
        document.getElementById('pagina-atual').textContent = `Página ${paginaAtual + 1} de ${clientesPage.page.totalPages}`;

        document.getElementById('btn-anterior').disabled = paginaAtual === 0;
        document.getElementById('btn-proximo').disabled = paginaAtual + 1 >= clientesPage.page.totalPages;

        document.getElementById('btn-anterior').onclick = () => listarClientes(paginaAtual - 1, clientesAtivos, termoBusca);
        document.getElementById('btn-proximo').onclick = () => listarClientes(paginaAtual + 1, clientesAtivos, termoBusca);
    } catch (erro) {
        if (!carregamento.atual()) return;
        console.error('Erro ao listar clientes:', erro);
        lista.innerHTML = '<div class="estado"><span></span></div>';
        lista.querySelector('span').textContent = notificarErro('Não foi possível carregar os clientes', erro);
    } finally {
        carregamento.finalizar();
    }
}

function atualizarFiltroClientes() {
    const btnAtivos = document.getElementById('btn-clientes-ativos');
    const btnInativos = document.getElementById('btn-clientes-inativos');

    if (!btnAtivos || !btnInativos) return;

    btnAtivos.classList.toggle('ativo-selecionado', clientesAtivos);
    btnInativos.classList.toggle('inativo-selecionado', !clientesAtivos);
    btnAtivos.setAttribute('aria-pressed', String(clientesAtivos));
    btnInativos.setAttribute('aria-pressed', String(!clientesAtivos));
}

function criarCardHTML(cliente) {
    const template = document.createElement('template');
    template.innerHTML = templateCardCliente.trim();

    const card = template.content.firstElementChild;
    card.dataset.clienteId = cliente.id;
    card.querySelector('.cliente-nome-card').textContent = cliente.nome;
    card.querySelector('.cliente-telefone-card').textContent = cliente.telefone ?? '';
    card.querySelector('.cliente-endereco-card').textContent = `${cliente.endereco?.logradouro ?? ''} - ${cliente.endereco?.cidade ?? ''}`;

    const ativo = cliente.ativo === true;
    const status = card.querySelector('.status');
    status.classList.add(ativo ? 'ativo' : 'inativo');
    status.textContent = ativo ? 'Ativo' : 'Inativo';

    card.querySelector('[data-acao-cliente="status"]').textContent = ativo ? 'Desativar' : 'Ativar';

    return card.outerHTML;
}

async function preencherEndereco(event) {
    const campoCep = event.target;
    const form = campoCep.closest('form');
    const cep = campoCep.value;
    const versao = versaoFormularioCliente;

    try {
        const endereco = await buscarEnderecoPorCep(cep);

        if (!form?.isConnected || campoCep.value !== cep || versao !== versaoFormularioCliente || salvandoCliente) return;
        if (!endereco) {
            mostrarNotificacao({ tipo: 'aviso', titulo: 'Confira os dados', mensagem: 'CEP não encontrado' });
            return;
        }
        ['logradouro', 'bairro', 'cidade', 'uf'].forEach(campo => {
            document.getElementById(campo).value = endereco[campo];
        });
    } catch (erro) {
        if (form?.isConnected && versao === versaoFormularioCliente) notificarErro('Não foi possível consultar o CEP', erro);
    }
}

async function salvarCliente() {
    const form = document.getElementById('form-cliente');
    if (!form || salvandoCliente || carregandoEdicaoCliente) return;

    const id = clienteEmEdicao;
    const versao = versaoFormularioCliente;
    const dados = {
        nome: document.getElementById('nome').value,
        telefone: document.getElementById('telefone').value,
        endereco: {
            logradouro: document.getElementById('logradouro').value,
            numero: document.getElementById('numero').value,
            complemento: document.getElementById('complemento').value,
            cep: document.getElementById('cep').value,
            bairro: document.getElementById('bairro').value,
            cidade: document.getElementById('cidade').value,
            uf: document.getElementById('uf').value
        }
    };

    salvandoCliente = true;
    form.querySelectorAll('button').forEach(botao => botao.disabled = true);
    form.querySelector('fieldset').disabled = true;

    try {
        await apiJson(id ? `/clientes/${id}` : '/clientes', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(dados)
        });

        if (!form.isConnected || versao !== versaoFormularioCliente) return;

        mostrarNotificacao({ tipo: 'sucesso', titulo: id ? 'Cliente editado com sucesso!' : 'Cliente cadastrado com sucesso!', mensagem: '' });
        limparFormularioCliente();

        await listarClientes(paginaAtual, clientesAtivos, termoBusca);
    } catch (erro) {
        if (form.isConnected && versao === versaoFormularioCliente) {
            notificarErro('Não foi possível salvar o cliente', erro);
        }
    } finally {
        if (form.isConnected) {
            salvandoCliente = false;
            form.querySelectorAll('button').forEach(botao => botao.disabled = false);
            form.querySelector('fieldset').disabled = false;
        }
    }
}

function definirModoEdicaoCliente(ativo) {
    document.body.classList.toggle('edicao-cliente-ativa', ativo);
    const lista = document.querySelector('.listagem-clientes');
    const navegacao = document.querySelector('.sidebar nav');

    if (lista) lista.inert = ativo;
    if (navegacao) navegacao.inert = ativo;
}

function limparFormularioCliente() {
    versaoFormularioCliente++;
    clienteEmEdicao = null;
    const form = document.getElementById('form-cliente');

    if (!form) return;

    form.reset();
    document.getElementById('titulo-form-cliente').textContent = 'Cadastro';
    form.querySelector('button[type=submit]').textContent = 'Cadastrar Cliente';
    document.getElementById('cancelar-edicao-cliente').classList.add('hidden');
    definirModoEdicaoCliente(false);
}

async function iniciarEdicaoCliente(id) {
    const form = document.getElementById('form-cliente');
    if (!form || salvandoCliente || carregandoEdicaoCliente || clienteEmEdicao !== null) return;

    const versao = versaoFormularioCliente;
    carregandoEdicaoCliente = true;
    form.querySelector('button[type=submit]').disabled = true;

    try {
        const {data: cliente} = await apiJson(`/clientes/${id}`);

        if (!form.isConnected || versao !== versaoFormularioCliente) return;

        limparFormularioCliente();
        document.getElementById('nome').value = cliente.nome ?? '';
        document.getElementById('telefone').value = cliente.telefone ?? '';
        ['cep', 'logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'uf'].forEach(campo => {
            document.getElementById(campo).value = cliente.endereco?.[campo] ?? '';
        });
        clienteEmEdicao = cliente.id;
        document.getElementById('titulo-form-cliente').textContent = `Edição do Cliente n° ${cliente.id}`;

        form.querySelector('button[type=submit]').textContent = 'Salvar alterações';
        document.getElementById('cancelar-edicao-cliente').classList.remove('hidden');
        definirModoEdicaoCliente(true);
        form.scrollIntoView({behavior: 'smooth'});
        document.getElementById('nome').focus({preventScroll: true});
    } catch (erro) {
        if (form.isConnected && versao === versaoFormularioCliente) notificarErro('Não foi possível carregar o cliente para edição', erro);
    } finally {
        if (form.isConnected) {
            carregandoEdicaoCliente = false;
            form.querySelector('button[type=submit]').disabled = false;
        }
    }
}

async function abrirModalExibicao(id) {
    try {
        const response = await fetch('cliente/html/modal-exibir-cliente.html');
        if (!response.ok) throw new Error('Erro ao carregar modal de exibição.');

        const htmlModal = await response.text();

        abrirModal({
            titulo: 'Exibir Cliente',
            conteudoHtml: htmlModal
        });

        await buscarClientePorId(id, 'exibicao');
    } catch (erro) {
        console.error('Erro ao abrir modal de exibição:', erro);
        notificarErro('Não foi possível exibir o cliente', erro);
    }
}

async function abrirModalAlterarStatus(id) {
    try {
        const response = await fetch('cliente/html/modal-alterar-status-cliente.html');
        if (!response.ok) throw new Error('Erro ao carregar modal de alteração de status.');

        const htmlModal = await response.text();
        const {data: cliente} = await apiJson(`/clientes/${id}`);

        const ativar = cliente.ativo !== true;
        const estado = ativar ? 'ativar' : 'desativar';

        abrirModal({
            titulo: `${estado[0].toUpperCase()}${estado.slice(1)} Cliente`,
            conteudoHtml: htmlModal
        });

        configurarModalAlterarStatus(id, cliente, ativar, estado);
    } catch (erro) {
        console.error('Erro ao abrir modal de alteração de status:', erro);
        notificarErro('Não foi possível abrir a alteração de status do cliente', erro);
    }
}

function configurarModalAlterarStatus(id, cliente, ativar, estado) {
    const conteudoStatus = document.getElementById('modal-alterar-status');
    const mensagem = conteudoStatus.querySelector('.mensagem-alterar-status');
    const botaoConfirmar = conteudoStatus.querySelector('.btn-confirmar-status');

    mensagem.textContent = `Tem certeza que deseja ${estado} o cliente ${cliente?.nome}?`;
    botaoConfirmar.onclick = () => alterarStatusCliente(id, ativar);
}

async function buscarClientePorId(id, tipo) {
    try {
        const {data: cliente} = await apiJson(`/clientes/${id}`);

        preencherDadosCliente(cliente, tipo);
    } catch (erro) {
        console.error('Erro ao buscar cliente para edição:', erro);
        notificarErro('Não foi possível carregar o cliente', erro);
    }
}

function preencherCampo(idCampo, valor) {
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

function preencherDadosCliente(cliente, tipo) {
    preencherCampo(`cliente-id-${tipo}`, cliente.id);
    preencherCampo(`nome-${tipo}`, cliente.nome);
    preencherCampo(`telefone-${tipo}`, cliente.telefone);
    preencherCampo(`cep-${tipo}`, cliente.endereco?.cep);
    preencherCampo(`logradouro-${tipo}`, cliente.endereco?.logradouro);
    preencherCampo(`numero-${tipo}`, cliente.endereco?.numero);
    preencherCampo(`complemento-${tipo}`, cliente.endereco?.complemento);
    preencherCampo(`bairro-${tipo}`, cliente.endereco?.bairro);
    preencherCampo(`cidade-${tipo}`, cliente.endereco?.cidade);
    preencherCampo(`uf-${tipo}`, cliente.endereco?.uf);

    if (tipo === 'exibicao') {
        preencherCampo(`endereco-${tipo}`, `${cliente.endereco?.logradouro ?? ''}, 
        ${cliente.endereco?.numero ?? ''}${cliente.endereco?.complemento ? ', ' +
            cliente.endereco.complemento : ''} - ${cliente.endereco?.bairro ?? ''}, 
            ${cliente.endereco?.cidade ?? ''} - ${cliente.endereco?.uf ?? ''}`);
    }
}

async function editarClientePelaExibicao() {
    const id = document.getElementById('cliente-id-exibicao').value;
    fecharModal();

    await iniciarEdicaoCliente(id);
}

async function alterarStatusCliente(id, estado) {
    try {
        await apiJson(`/clientes/${id}`, {
            method: 'PATCH',
            body: JSON.stringify({status: estado})
        });

        mostrarNotificacao({ tipo: 'sucesso', titulo: `Cliente ${estado ? 'ativado' : 'inativado'} com sucesso!`, mensagem: '' });
        fecharModal();
        listarClientes();
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        notificarErro('Não foi possível alterar o status do cliente', erro);
    }
}

const conteudoDinamico = document.getElementById('conteudo-dinamico');

conteudoDinamico.addEventListener('pagina:carregada', (event) => {
    versaoFormularioCliente++;
    clienteEmEdicao = null;
    salvandoCliente = false;
    carregandoEdicaoCliente = false;

    if (document.body.classList.contains('edicao-cliente-ativa')) definirModoEdicaoCliente(false);
    if (event.detail.modulo === 'cliente') {
        limparFormularioCliente();
        termoBusca = '';
        paginaAtual = 0;
        listarClientes(0, true);
    }
});

conteudoDinamico.addEventListener('click', (event) => {
    if (event.target.id === 'cancelar-edicao-cliente' && !salvandoCliente) {
        limparFormularioCliente();
        document.getElementById('nome').focus({preventScroll: true});
    }
    if (event.target.id === 'btn-clientes-ativos') {
        listarClientes(0, true);
    } else if (event.target.id === 'btn-clientes-inativos') {
        termoBusca = '';
        const campoBusca = document.getElementById('busca-clientes');
        if (campoBusca) campoBusca.value = '';
        listarClientes(0, false, '');
    }
})

document.addEventListener('focusout', (event) => {
    if (event.target.id !== 'cep' || event.target.closest('form')?.id !== 'form-cliente') return;
    preencherEndereco(event);
});

conteudoDinamico.addEventListener('submit', async (event) => {
    if (event.target.id === 'form-busca-clientes') {
        event.preventDefault();
        const busca = document.getElementById('busca-clientes').value;

        await listarClientes(0, true, busca);
    } else if (event.target.id === 'form-cliente') {
        event.preventDefault();

        await salvarCliente();
    } else {
        return;
    }
});

document.getElementById('conteudo-dinamico').addEventListener('click', event => {
    const botao = event.target.closest('[data-acao-cliente]');
    if (!botao) return;

    const id = Number(botao.closest('[data-cliente-id]').dataset.clienteId);

    if (botao.dataset.acaoCliente === 'editar') iniciarEdicaoCliente(id);
    if (botao.dataset.acaoCliente === 'exibir') abrirModalExibicao(id);
    if (botao.dataset.acaoCliente === 'status') abrirModalAlterarStatus(id);
});
