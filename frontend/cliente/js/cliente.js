let paginaAtual = 0;
let clientesAtivos = true;

async function listarClientes(pagina = 0, tipo = clientesAtivos) {
    const lista = document.getElementById('lista');
    if (!lista) return;

    clientesAtivos = tipo;
    atualizarFiltroClientes();
    try {
        const {
            response, // info da requisição (status, headers, etc.) - salva na variavel response
            data: clientesPage // os dados retornados (clientes) - salva na variavel clientesPage
        } = await apiJson(`/clientes?page=${pagina}&size=2&ativo=${tipo}`);

        if (!response.ok) {
            lista.innerHTML = '<div class="estado">Erro ao carregar clientes</div>';
            return;
        }

        const clientes = clientesPage.content;

        if (!clientes || clientes.length === 0) {
            lista.innerHTML = '<div class="estado">Nenhum cliente cadastrado</div>';
            return;
        }

        lista.innerHTML = clientes.map(cliente => criarCardHTML(cliente)).join('');

        // Atualiza paginação
        paginaAtual = clientesPage.page.number;
        document.getElementById('pagina-atual').textContent = `Página ${paginaAtual + 1} de ${clientesPage.page.totalPages}`;

        document.getElementById('btn-anterior').disabled = paginaAtual === 0;
        document.getElementById('btn-proximo').disabled = paginaAtual + 1 >= clientesPage.page.totalPages;

        document.getElementById('btn-anterior').onclick = () => listarClientes(paginaAtual - 1, clientesAtivos);
        document.getElementById('btn-proximo').onclick = () => listarClientes(paginaAtual + 1, clientesAtivos);
    } catch (erro) {
        console.error('Erro ao listar clientes:', erro);
        lista.innerHTML = '<div class="estado">Erro ao conectar com o servidor</div>';
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
    const ativo = cliente.ativo === true;
    const statusClasse = ativo ? 'ativo' : 'inativo';
    const statusTexto = ativo ? 'Ativo' : 'Inativo';
    const statusBotao = ativo ? 'Inativar' : 'Ativar';

    return `
      <div class="card">
        <div class="card-info">
          <h2>${cliente.nome}</h2>
          <p>${cliente.telefone ?? ''}</p>
          <p>${cliente.endereco?.logradouro ?? ''} - ${cliente.endereco?.cidade ?? ''}</p>
        </div>
        <span class="status ${statusClasse}">${statusTexto}</span>
        <button type="button" class="btn btn-secundario btn-editar" onclick="abrirModalEdicao(${cliente.id})">Editar</button>
        <button type="button" class="btn btn-secundario btn-exibir" onclick="abrirModalExibicao(${cliente.id})">Exibir</button>
        <button type="button" class="btn btn-secundario btn-alterarStatus" onclick="abrirModalAlterarStatus(${cliente.id})">${statusBotao}</button>
      </div>
    `;
}

async function preencherEndereco(event) {
    const endereco = await buscarEnderecoPorCep(event.target.value);

    if (!endereco) {
        alert('CEP não encontrado');
        return;
    }

    const sufixo = event.target.id === 'cep-edicao' ? '-edicao' : '';

    document.getElementById(`logradouro${sufixo}`).value = endereco.logradouro;
    document.getElementById(`bairro${sufixo}`).value = endereco.bairro;
    document.getElementById(`cidade${sufixo}`).value = endereco.cidade;
    document.getElementById(`uf${sufixo}`).value = endereco.uf;
}

async function cadastrarCliente() {
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

    try {
        const {
            response,
            data: resultado
        } = await apiJson('/clientes', {
            method: 'POST',
            body: JSON.stringify(dados)
        });

        if (response.ok) {
            alert('Cliente cadastrado com sucesso!');
            document.getElementById('form-cliente').reset();
            listarClientes();
        } else {
            alert(resultado?.mensagem || 'Erro ao cadastrar cliente');
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function abrirModalEdicao(id) {
    try {
        const response = await fetch('cliente/html/modal-editar-cliente.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de edição');

        const htmlModal = await response.text();

        abrirModal({
            titulo: 'Editar Cliente',
            conteudoHtml: htmlModal
        });
        await buscarClientePorId(id, 'edicao');
    } catch (erro) {
        console.error('Erro ao abrir modal de edição:', erro);
        alert('Erro ao abrir modal de edição.');
    }
}

async function abrirModalExibicao(id) {
    try {
        const response = await fetch('cliente/html/modal-exibir-cliente.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de exibição');

        const htmlModal = await response.text();

        abrirModal({
            titulo: 'Exibir Cliente',
            conteudoHtml: htmlModal
        });
        await buscarClientePorId(id, 'exibicao');
    } catch (erro) {
        console.error('Erro ao abrir modal de exibição:', erro);
        alert('Erro ao abrir modal de exibição.');
    }
}

async function abrirModalAlterarStatus(id) {
    try {
        const response = await fetch('cliente/html/modal-alterar-status-cliente.html');

        if (!response.ok) throw new Error('Erro ao carregar modal de alteração de status');

        const htmlModal = await response.text();
        const {response: respostaCliente, data: cliente} = await apiJson(`/clientes/${id}`);

        if (!respostaCliente.ok) {
            throw new Error('Erro ao carregar dados do cliente');
        }

        const ativar = cliente.ativo !== true;
        const estado = ativar ? 'ativar' : 'inativar';
        abrirModal({
            titulo: `${estado[0].toUpperCase()}${estado.slice(1)} Cliente`,
            conteudoHtml: htmlModal
        });

        configurarModalAlterarStatus(id, cliente, ativar, estado);
    } catch (erro) {
        console.error('Erro ao abrir modal de alteração de status:', erro);
        alert('Erro ao abrir modal de alteração de status.');
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
        const {response, data: cliente} = await apiJson(`/clientes/${id}`);

        if (!response.ok) {
            alert('Erro ao carregar dados do cliente');
            return;
        }
        preencherDadosCliente(cliente, tipo);
    } catch (erro) {
        console.error('Erro ao buscar cliente para edição:', erro);
        alert('Erro ao conectar com o servidor.');
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

async function editarCliente() {
    const id = document.getElementById('cliente-id-edicao').value;
    const dados = {
        nome: document.getElementById('nome-edicao').value,
        telefone: document.getElementById('telefone-edicao').value,
        endereco: {
            logradouro: document.getElementById('logradouro-edicao').value,
            numero: document.getElementById('numero-edicao').value,
            complemento: document.getElementById('complemento-edicao').value,
            cep: document.getElementById('cep-edicao').value,
            bairro: document.getElementById('bairro-edicao').value,
            cidade: document.getElementById('cidade-edicao').value,
            uf: document.getElementById('uf-edicao').value
        }
    };

    try {
        const {response, data: resultado} = await apiJson(`/clientes/${id}`, {
            method: 'PUT',
            body: JSON.stringify(dados)
        });

        if (response.ok) {
            alert('Cliente editado com sucesso!');
            document.getElementById('form-cliente-edicao').reset();
            fecharModal();
            listarClientes();
        } else {
            alert(resultado?.mensagem || 'Erro ao editar cliente');
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

async function editarClientePelaExibicao() {
    const id = document.getElementById('cliente-id-exibicao').value;
    fecharModal();
    await abrirModalEdicao(id);
}

async function alterarStatusCliente(id, estado) {
    try {
        const {response, data: resultado} = await apiJson(`/clientes/${id}`, {
            method: 'PATCH',
            body: JSON.stringify({status: estado})
        });

        if (response.ok) {
            alert(`Cliente ${estado ? 'ativado' : 'inativado'} com sucesso!`);
            fecharModal();
            listarClientes();
        } else {
            alert(resultado?.mensagem || `Erro ao ${estado ? 'ativar' : 'inativar'} cliente`);
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert(erro.message || 'Erro ao conectar com o servidor.');
    }
}

const conteudoDinamico = document.getElementById('conteudo-dinamico');

conteudoDinamico.addEventListener('pagina:carregada', (event) => {
    if (event.detail.modulo === 'cliente') {
        listarClientes(0, true);
    }
});

conteudoDinamico.addEventListener('click', (event) => {
    if (event.target.id === 'btn-clientes-ativos') {
        listarClientes(0, true);
    } else if (event.target.id === 'btn-clientes-inativos') {
        listarClientes(0, false);
    }
})

document.addEventListener('focusout', (event) => {
    if (!event.target.id.includes('cep')) return;
    preencherEndereco(event);
});

conteudoDinamico.addEventListener('submit', async (event) => {
    if (event.target.id === 'form-cliente') {
        event.preventDefault();
        await cadastrarCliente();
    } else if (event.target.id === 'form-cliente-edicao') {
        event.preventDefault();
        await editarCliente();
    } else {
        return;
    }
});
