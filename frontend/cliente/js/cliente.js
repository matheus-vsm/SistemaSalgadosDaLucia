const conteudoDinamico = document.getElementById('conteudo-dinamico');
let paginaAtual = 0;

async function listarClientes(pagina = 0) {
    const corpoTabela = document.getElementById('corpo-tabela-clientes');
    const paginacaoDiv = document.getElementById('paginacao-clientes');
    if (!corpoTabela) return; // não é a página de cliente, ignora

    console.log('Buscando clientes...');
    try {
        const response = await fetch(`http://localhost:8080/api/salgados-da-lucia-kojima/clientes?page=${pagina}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            }
        });

        if (!response.ok) {
            corpoTabela.innerHTML = '<tr><td colspan="4">Erro ao carregar clientes</td></tr>';
            return;
        }

        const clientesPage = await response.json();
        const clientes = clientesPage.content; // extrai o array

        if (!clientes || clientes.length === 0) {
            corpoTabela.innerHTML = '<tr><td colspan="4">Nenhum cliente cadastrado</td></tr>';
            return;
        }

        corpoTabela.innerHTML = clientes.map(cliente => `
            <tr>
                <td>${cliente.nome}</td>
                <td>${cliente.telefone}</td>
                <td>${cliente.endereco?.cidade ?? ''}</td>
                <td>${cliente.endereco?.logradouro ?? ''}</td>
            </tr>
        `).join('');

        // Atualiza paginação
        paginaAtual = clientesPage.page.number;
        document.getElementById('pagina-atual').textContent = `Página ${paginaAtual + 1} de ${clientesPage.page.totalPages}`;

        document.getElementById('btn-anterior').disabled = paginaAtual === 0;
        document.getElementById('btn-proximo').disabled = paginaAtual + 1 >= clientesPage.page.totalPages;

        document.getElementById('btn-anterior').onclick = () => listarClientes(paginaAtual - 1);
        document.getElementById('btn-proximo').onclick = () => listarClientes(paginaAtual + 1);
    } catch (erro) {
        console.error('Erro ao listar clientes:', erro);
        corpoTabela.innerHTML = '<tr><td colspan="4">Erro ao conectar com o servidor</td></tr>';
    }
}

async function preencherEndereco() {
    const cep = event.target.value;
    const endereco = await buscarEnderecoPorCep(cep);

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
    }

    try {
        const response = await fetch('http://localhost:8080/api/salgados-da-lucia-kojima/clientes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
            },
            body: JSON.stringify(dados)
        });

        if (response.ok) {
            alert('Cliente cadastrado com sucesso!');
            listarClientes();
        } else {
            const resultado = await response.json();
            alert(resultado.mensagem || 'Erro ao cadastrar cliente');
        }
    } catch (erro) {
        console.error('Erro na requisição:', erro);
        alert('Erro ao conectar com o servidor.');
    }
}

// Dispara a listagem toda vez que uma página é injetada
conteudoDinamico.addEventListener('pagina:carregada', (event) => {
    if (event.detail.url === 'cliente/html/cliente.html') {
        listarClientes();
    }
});

// endereco
conteudoDinamico.addEventListener('focusout', (event) => {
    if (event.target.id !== 'cep') return;
    preencherEndereco();
});

// cadastrar cliente
conteudoDinamico.addEventListener('submit', async (event) => {
   if (event.target.id !== 'form-cliente') return;
   event.preventDefault();
   await cadastrarCliente();
   event.target.reset();
});
