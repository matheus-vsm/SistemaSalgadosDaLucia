let paginaUsuarios = 0;
let usuariosAtivos = true;
let perfisUsuario = [];
let usuariosListados = [];
let requisicaoUsuarios = 0;
let templateCardUsuario = '';
let termoBuscaUsuarios = '';

async function carregarDadosIniciaisUsuario() {
    usuariosListados = [];
    perfisUsuario = [];
    termoBuscaUsuarios = '';
    filtrarUsuarios(true);
    await carregarPerfisUsuario();
}

function usuarioLogado(usuario) {
    try {
        const payload = getAccessToken().split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const dados = JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(payload), c => c.charCodeAt(0))));

        return typeof dados.sub === 'string' && dados.sub.toLowerCase() === usuario.username.toLowerCase();
    } catch {
        return false;
    }
}

function renderizarUsuarios() {
    const lista = document.getElementById('lista-usuarios');
    if (!lista) return;

    lista.innerHTML = usuariosListados.length ? usuariosListados.map(criarCardUsuario).join('') : '<div class="estado">Nenhum usuário encontrado.</div>';
}

function criarCardUsuario(usuario) {
    const template = document.createElement('template');
    template.innerHTML = templateCardUsuario.trim();

    const card = template.content.firstElementChild;
    const nomesPerfis = (usuario.perfis || []).map(perfil => {
        const valor = (perfil.nome || perfil.authority || '').replace(/^ROLE_/, '');
        return perfisUsuario.find(item => item.valor === valor)?.descricao || valor;
    }).filter(Boolean).join(', ');

    card.querySelector('.usuario-nome-card').textContent = usuario.nome ?? '';
    card.querySelector('.usuario-username-card').textContent = usuario.username ?? '';
    card.querySelector('.usuario-perfil-card').textContent = nomesPerfis || 'Não informado';

    const status = card.querySelector('.usuario-status-card');
    status.classList.add(usuario.ativo ? 'ativo' : 'inativo');
    status.textContent = usuario.ativo ? 'Ativo' : 'Inativo';

    const desativar = card.querySelector('[data-desativar-usuario]');
    desativar.dataset.desativarUsuario = usuario.id;
    desativar.disabled = !usuario.ativo;

    const senha = card.querySelector('[data-senha-usuario]');
    senha.dataset.senhaUsuario = usuario.id;
    senha.disabled = !usuarioLogado(usuario);

    if (senha.disabled) senha.title = 'Disponível apenas para o usuário logado';
    return card.outerHTML;
}

async function listarUsuarios(numero = 0) {
    const lista = document.getElementById('lista-usuarios');
    if (!lista) return;

    const chamada = ++requisicaoUsuarios;
    document.getElementById('usuarios-anterior').disabled = true;
    document.getElementById('usuarios-proximo').disabled = true;
    document.getElementById('usuarios-pagina').textContent = '';
    lista.innerHTML = '<div class="estado">Carregando usuários...</div>';

    try {
        if (!templateCardUsuario) {
            const respostaCard = await fetch('usuarios/html/card-usuario.html');
            if (!respostaCard.ok) throw new Error('Erro ao carregar o card de usuário.');

            templateCardUsuario = await respostaCard.text();
        }
        const endpoint = termoBuscaUsuarios ? '/usuarios/nome' : '/usuarios';
        const parametros = new URLSearchParams({ativo: usuariosAtivos, page: numero, size: 4});
        if (termoBuscaUsuarios) parametros.set('nome', termoBuscaUsuarios);
        const {response, data} = await apiJson(`${endpoint}?${parametros}`);
        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar usuários.');

        if (chamada !== requisicaoUsuarios || !lista.isConnected) return;
        usuariosListados = data.content || [];
        const detalhes = data.page || data;
        if (!usuariosListados.length && numero > 0) return listarUsuarios(numero - 1);

        paginaUsuarios = detalhes.number ?? numero;
        renderizarUsuarios();

        document.getElementById('usuarios-pagina').textContent = detalhes.totalPages
            ? `Página ${paginaUsuarios + 1} de ${detalhes.totalPages}` : 'Nenhum resultado';
        document.getElementById('usuarios-anterior').disabled = paginaUsuarios === 0;
        document.getElementById('usuarios-proximo').disabled = paginaUsuarios + 1 >= (detalhes.totalPages || 0);
    } catch (erro) {
        if (chamada !== requisicaoUsuarios || !lista.isConnected) return;
        lista.innerHTML = '<div class="estado"><span></span> <button type="button" class="btn btn-secundario" id="usuarios-recarregar">Tentar novamente</button></div>';
        lista.querySelector('span').textContent = erro.message || 'Erro ao conectar com o servidor.';
    }
}

async function carregarPerfisUsuario() {
    const select = document.getElementById('usuario-perfil');
    if (!select) return;
    select.disabled = true;

    document.getElementById('usuario-cadastrar').disabled = true;
    document.getElementById('usuarios-erro-perfis').hidden = true;

    try {
        const {response, data} = await apiJson('/enums/usuario');
        if (!response.ok || !data?.perfis?.length) throw new Error('Não foi possível carregar os perfis.');
        if (!select.isConnected) return;

        perfisUsuario = data.perfis;
        select.innerHTML = '<option value="">Selecione o perfil</option>';
        perfisUsuario.forEach(perfil => {
            const option = document.createElement('option');
            option.value = perfil.valor;
            option.textContent = perfil.descricao;
            select.appendChild(option);
        });
        select.disabled = false;

        document.getElementById('usuario-cadastrar').disabled = false;
        if (usuariosListados.length) renderizarUsuarios();
    } catch (erro) {
        if (!select.isConnected) return;

        select.innerHTML = '<option value="">Perfis indisponíveis</option>';
        document.getElementById('usuarios-erro-perfis').textContent = erro.message;
        document.getElementById('usuarios-erro-perfis').hidden = false;
    }
}

function filtrarUsuarios(tipo) {
    usuariosAtivos = tipo;
    document.getElementById('btn-usuarios-ativos').classList.toggle('ativo-selecionado', usuariosAtivos);
    document.getElementById('btn-usuarios-inativos').classList.toggle('inativo-selecionado', !usuariosAtivos);
    document.getElementById('btn-usuarios-ativos').setAttribute('aria-pressed', String(usuariosAtivos));
    document.getElementById('btn-usuarios-inativos').setAttribute('aria-pressed', String(!usuariosAtivos));
    listarUsuarios(0);
}

async function cadastrarUsuario() {
    const form = document.getElementById('form-usuario');
    const botao = document.getElementById('usuario-cadastrar');

    if (botao.disabled || !form.reportValidity()) return;

    const dados = {
        nome: document.getElementById('usuario-nome').value.trim(),
        username: document.getElementById('usuario-username').value.trim(),
        senha: document.getElementById('usuario-senha').value,
        perfilUsuarioNome: document.getElementById('usuario-perfil').value
    };

    if (!dados.nome || !dados.username || !dados.senha.trim()) {
        alert('Preencha nome, username e senha.');
        return;
    }

    botao.disabled = true;
    botao.textContent = 'Cadastrando...';

    try {
        const {response, data} = await apiJson('/usuarios/cadastrar', {
            method: 'POST', body: JSON.stringify(dados)
        });

        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao cadastrar usuário.');
        if (!form.isConnected) return;

        form.reset();
        alert('Usuário cadastrado com sucesso!');
        filtrarUsuarios(true);
    } catch (erro) {
        alert(erro.message || 'Erro ao conectar com o servidor.');
    } finally {
        botao.disabled = false;
        botao.textContent = 'Cadastrar Usuário';
    }
}

async function carregarModalUsuario(arquivo, titulo) {
    const area = document.getElementById('area-usuarios');

    try {
        const response = await fetch(`usuarios/html/${arquivo}`);
        if (!response.ok) throw new Error('Erro ao carregar o modal.');

        const conteudoHtml = await response.text();
        if (!area?.isConnected) return false;

        abrirModal({titulo, conteudoHtml});
        return true;
    } catch (erro) {
        alert(erro.message || 'Erro ao carregar o modal.');
        return false;
    }
}

async function abrirDesativacaoUsuario(id) {
    const usuario = usuariosListados.find(item => String(item.id) === id);
    if (!usuario?.ativo) return;
    if (!await carregarModalUsuario('modal-desativar-usuario.html', 'Desativar Usuário')) return;

    document.getElementById('usuario-mensagem-desativacao').textContent = `Tem certeza que deseja desativar o usuário ${usuario.nome}?`;
    document.getElementById('usuario-cancelar').onclick = fecharModal;
    document.getElementById('usuario-confirmar-desativacao').onclick = () => desativarUsuario(id);
}

async function desativarUsuario(id) {
    const botao = document.getElementById('usuario-confirmar-desativacao');
    if (botao.disabled) return;
    botao.disabled = true;

    try {
        const {response, data} = await apiJson(`/usuarios/${encodeURIComponent(id)}`, {method: 'PATCH'});
        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao desativar usuário.');
        if (botao.isConnected) fecharModal();

        alert('Usuário desativado com sucesso!');
        listarUsuarios(paginaUsuarios);
    } catch (erro) {
        alert(erro.message || 'Erro ao conectar com o servidor.');
        botao.disabled = false;
    }
}

async function abrirAlteracaoSenhaUsuario(id) {
    const usuario = usuariosListados.find(item => String(item.id) === id);
    if (!usuario || !usuarioLogado(usuario)) return;
    if (!await carregarModalUsuario('modal-alterar-senha.html', 'Alterar minha senha')) return;

    document.getElementById('usuario-cancelar').onclick = fecharModal;
    document.getElementById('form-senha-usuario').onsubmit = event => {
        event.preventDefault();
        alterarSenhaUsuario(usuario);
    };
    document.getElementById('usuario-senha-atual').focus();
}

async function alterarSenhaUsuario(usuario) {
    if (!usuarioLogado(usuario)) return;
    const form = document.getElementById('form-senha-usuario');
    const botao = form.querySelector('[type="submit"]');
    if (botao.disabled || !form.reportValidity()) return;

    const dados = {
        senhaAtual: document.getElementById('usuario-senha-atual').value,
        novaSenha: document.getElementById('usuario-nova-senha').value,
        novaSenhaConfirmacao: document.getElementById('usuario-confirmar-senha').value
    };
    if (!dados.novaSenha.trim() || dados.novaSenha !== dados.novaSenhaConfirmacao) {
        alert('Preencha a nova senha e confirme com o mesmo valor.');
        return;
    }
    botao.disabled = true;

    try {
        const {response, data} = await apiJson('/usuarios', {method: 'PATCH', body: JSON.stringify(dados)});
        if (!response.ok) throw new Error(data?.mensagem || 'Erro ao alterar senha.');
        if (form.isConnected) fecharModal();
        alert('Senha alterada com sucesso!');
    } catch (erro) {
        alert(erro.message || 'Erro ao conectar com o servidor.');
    } finally {
        botao.disabled = false;
    }
}

const areaUsuario = document.getElementById('conteudo-dinamico');
areaUsuario.addEventListener('pagina:carregada', event => {
    if (event.detail.modulo === 'usuario') carregarDadosIniciaisUsuario();
});

areaUsuario.addEventListener('click', event => {
    const botao = event.target.closest('button');
    if (botao?.disabled) return;
    if (botao?.dataset.desativarUsuario) abrirDesativacaoUsuario(botao.dataset.desativarUsuario);
    if (botao?.dataset.senhaUsuario) abrirAlteracaoSenhaUsuario(botao.dataset.senhaUsuario);

    switch (event.target.id) {
        case 'btn-usuarios-ativos':
            filtrarUsuarios(true);
            break;
        case 'btn-usuarios-inativos':
            filtrarUsuarios(false);
            break;
        case 'usuarios-anterior':
            listarUsuarios(paginaUsuarios - 1);
            break;
        case 'usuarios-proximo':
            listarUsuarios(paginaUsuarios + 1);
            break;
        case 'usuarios-recarregar':
            listarUsuarios();
            break;
    }
});

areaUsuario.addEventListener('submit', event => {
    if (event.target.id === 'form-busca-usuarios') {
        event.preventDefault();
        termoBuscaUsuarios = document.getElementById('busca-usuarios').value.trim();
        listarUsuarios(0);
    }
    if (event.target.id === 'form-usuario') {
        event.preventDefault();
        cadastrarUsuario();
    }
});
