let paginaAtual = 0;

async function listarClientes(pagina = 0) {
    const lista = document.getElementById('lista');
    if (!lista) return;

    try {
        const {
            response, // info da requisição (status, headers, etc.) - salva na variavel response
            data: clientesPage // os dados retornados (clientes) - salva na variavel clientesPage
        } = await apiJson(`/clientes?page=${pagina}&size=2`);

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

        document.getElementById('btn-anterior').onclick = () => listarClientes(paginaAtual - 1);
        document.getElementById('btn-proximo').onclick = () => listarClientes(paginaAtual + 1);
    } catch (erro) {
        console.error('Erro ao listar clientes:', erro);
        lista.innerHTML = '<div class="estado">Erro ao conectar com o servidor</div>';
    }
}

function criarCardHTML(cliente) {
    const ativo = cliente.ativo === true;
    const statusClasse = ativo ? 'ativo' : 'inativo';
    const statusTexto = ativo ? 'Ativo' : 'Inativo';

    return `
      <div class="card">
        <div class="card-info">
          <h2>${cliente.nome}</h2>
          <p>${cliente.telefone ?? ''}</p>
          <p>${cliente.endereco?.logradouro ?? ''} - ${cliente.endereco?.cidade ?? ''}</p>
        </div>
        <span class="status ${statusClasse}">${statusTexto}</span>
        <button type="button" class="btn btn-secundario btn-editar" onclick="abrirModalEdicao(${cliente.id})">Editar</button>
      </div>
    `;
}

async function preencherEndereco(event) {
    const endereco = await buscarEnderecoPorCep(event.target.value);

    if (!endereco) {
        alert('CEP não encontrado');
        return;
    }

    document.getElementById('logradouro').value = endereco.logradouro;
    document.getElementById('bairro').value = endereco.bairro;
    document.getElementById('cidade').value = endereco.cidade;
    document.getElementById('uf').value = endereco.uf;
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
        await buscarClientePorId(id);
    } catch (erro) {
        console.error('Erro ao abrir modal de edição:', erro);
        alert('Erro ao abrir modal de edição.');
    }
}

async function buscarClientePorId(id) {
    try {
        const { response, data: cliente } = await apiJson(`/clientes/${id}`);

        if (!response.ok) {
            alert('Erro ao carregar dados do cliente');
            return;
        }
        preencherDadosCliente(cliente);
    } catch (erro) {
        console.error('Erro ao buscar cliente para edição:', erro);
        alert('Erro ao conectar com o servidor.');
    }
}
function preencherDadosCliente(cliente) {
    document.getElementById('cliente-id').value = cliente.id ?? '';
    document.getElementById('nome-edicao').value = cliente.nome ?? '';
    document.getElementById('telefone-edicao').value = cliente.telefone ?? '';
    document.getElementById('cep-edicao').value = cliente.endereco?.cep ?? '';
    document.getElementById('logradouro-edicao').value = cliente.endereco?.logradouro ?? '';
    document.getElementById('numero-edicao').value = cliente.endereco?.numero ?? '';
    document.getElementById('complemento-edicao').value = cliente.endereco?.complemento ?? '';
    document.getElementById('bairro-edicao').value = cliente.endereco?.bairro ?? '';
    document.getElementById('cidade-edicao').value = cliente.endereco?.cidade ?? '';
    document.getElementById('uf-edicao').value = cliente.endereco?.uf ?? '';
}

async function editarCliente() {
    const id = document.getElementById('cliente-id').value;
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

const conteudoDinamico = document.getElementById('conteudo-dinamico');

conteudoDinamico.addEventListener('pagina:carregada', (event) => {
    if (event.detail.modulo === 'cliente') {
        listarClientes();
    }
});

conteudoDinamico.addEventListener('focusout', (event) => {
    if (event.target.id !== 'cep') return;
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
