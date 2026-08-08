let paginaAtual = 0;

async function listarClientes(pagina = 0) {
    const lista = document.getElementById('lista');
    if (!lista) return;

    try {
        const { response, data: clientesPage } = await apiJson(`/clientes?page=${pagina}&size=2`);

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
        <button class="btn btn-secundario btn-editar" onclick="abrirModalEdicao(${cliente.id})">Editar</button>
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
        const { response, data: resultado } = await apiJson('/clientes', {
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

function abrirModalEdicao(id) {
    abrirModal({
        titulo: 'Editar Cliente',
        conteudoHtml: '<p class="estado">Modal de edição será implementado em breve.</p>'
    });
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
    if (event.target.id !== 'form-cliente') return;
    event.preventDefault();
    await cadastrarCliente();
});
