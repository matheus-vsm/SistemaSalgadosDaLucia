let paginaUsuarios = 0;
let usuariosAtivos = true;
let perfisUsuario = [];
let usuariosListados = [];
let requisicaoUsuarios = 0;
const escaparUsuario = valor => String(valor ?? '').replace(/[&<>"']/g, caractere => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
})[caractere]);

async function carregarDadosIniciaisUsuario() {
    usuariosListados = [];
    perfisUsuario = [];
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
    const nomesPerfis = (usuario.perfis || []).map(perfil => {
        const valor = (perfil.nome || perfil.authority || '').replace(/^ROLE_/, '');
        return perfisUsuario.find(item => item.valor === valor)?.descricao || valor;
    }).filter(Boolean).join(', ');
    return `<div class="card">
        <div class="card-info">
            <h2>${escaparUsuario(usuario.nome)}</h2>
            <p>Username: ${escaparUsuario(usuario.username)}</p>
            <p>Perfil: ${escaparUsuario(nomesPerfis || 'Não informado')}</p>
        </div>
        <span class="status ${usuario.ativo ? 'ativo' : 'inativo'}">${usuario.ativo ? 'Ativo' : 'Inativo'}</span>
        <div class="acoes-usuario">
            <button type="button" class="btn btn-secundario" data-desativar-usuario="${escaparUsuario(usuario.id)}" ${usuario.ativo ? '' : 'disabled'}>Desativar</button>
            <button type="button" class="btn btn-secundario" data-senha-usuario="${escaparUsuario(usuario.id)}" ${usuarioLogado(usuario) ? '' : 'disabled title="Disponível apenas para o usuário logado"'}>Alterar senha</button>
        </div>
    </div>`;

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
        const {response, data} = await apiJson(`/usuarios?ativo=${usuariosAtivos}&page=${numero}&size=4`);
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
        lista.innerHTML = `<div class="estado">${escaparUsuario(erro.message || 'Erro ao conectar com o servidor.')}
            <button type="button" class="btn btn-secundario" id="usuarios-recarregar">Tentar novamente</button></div>`;
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
        select.innerHTML = '<option value="">Selecione o perfil</option>' + perfisUsuario.map(perfil =>
            `<option value="${escaparUsuario(perfil.valor)}">${escaparUsuario(perfil.descricao)}</option>`).join('');
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
    if (event.target.id === 'form-usuario') {
        event.preventDefault();
        cadastrarUsuario();
    }
});
