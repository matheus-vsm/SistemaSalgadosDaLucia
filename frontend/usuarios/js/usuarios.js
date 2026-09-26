(() => {
    const conteudo = document.getElementById('conteudo-dinamico');
    let pagina = 0;
    let ativos = true;
    let perfis = [];
    let usuarios = [];
    let requisicao = 0;
    const campo = id => document.getElementById(id);
    const escapar = valor => String(valor ?? '').replace(/[&<>"']/g, caractere => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[caractere]);

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
        const lista = campo('lista-usuarios');
        if (!lista) return;
        lista.innerHTML = usuarios.length ? usuarios.map(usuario => {
            const nomesPerfis = (usuario.perfis || []).map(perfil => {
                const valor = (perfil.nome || perfil.authority || '').replace(/^ROLE_/, '');
                return perfis.find(item => item.valor === valor)?.descricao || valor;
            }).filter(Boolean).join(', ');
            return `<div class="card">
                <div class="card-info">
                    <h2>${escapar(usuario.nome)}</h2>
                    <p>Username: ${escapar(usuario.username)}</p>
                    <p>Perfil: ${escapar(nomesPerfis || 'Não informado')}</p>
                </div>
                <span class="status ${usuario.ativo ? 'ativo' : 'inativo'}">${usuario.ativo ? 'Ativo' : 'Inativo'}</span>
                <div class="acoes-usuario">
                    <button type="button" class="btn btn-secundario" data-desativar-usuario="${escapar(usuario.id)}" ${usuario.ativo ? '' : 'disabled'}>Desativar</button>
                    <button type="button" class="btn btn-secundario" data-senha-usuario="${escapar(usuario.id)}" ${usuarioLogado(usuario) ? '' : 'disabled title="Disponível apenas para o usuário logado"'}>Alterar senha</button>
                </div>
            </div>`;
        }).join('') : '<div class="estado">Nenhum usuário encontrado.</div>';
    }

    async function listarUsuarios(numero = 0) {
        const lista = campo('lista-usuarios');
        if (!lista) return;
        const chamada = ++requisicao;
        campo('usuarios-anterior').disabled = true;
        campo('usuarios-proximo').disabled = true;
        campo('usuarios-pagina').textContent = '';
        lista.innerHTML = '<div class="estado">Carregando usuários...</div>';
        try {
            const {response, data} = await apiJson(`/usuarios?ativo=${ativos}&page=${numero}&size=4`);
            if (!response.ok) throw new Error(data?.mensagem || 'Erro ao carregar usuários.');
            if (chamada !== requisicao || !lista.isConnected) return;
            usuarios = data.content || [];
            const detalhes = data.page || data;
            if (!usuarios.length && numero > 0) return listarUsuarios(numero - 1);
            pagina = detalhes.number ?? numero;
            renderizarUsuarios();
            campo('usuarios-pagina').textContent = detalhes.totalPages
                ? `Página ${pagina + 1} de ${detalhes.totalPages}` : 'Nenhum resultado';
            campo('usuarios-anterior').disabled = pagina === 0;
            campo('usuarios-proximo').disabled = pagina + 1 >= (detalhes.totalPages || 0);
        } catch (erro) {
            if (chamada !== requisicao || !lista.isConnected) return;
            lista.innerHTML = `<div class="estado">${escapar(erro.message || 'Erro ao conectar com o servidor.')}
                <button type="button" class="btn btn-secundario" id="usuarios-recarregar">Tentar novamente</button></div>`;
        }
    }

    async function carregarPerfis() {
        const select = campo('usuario-perfil');
        if (!select) return;
        select.disabled = true;
        campo('usuario-cadastrar').disabled = true;
        campo('usuarios-erro-perfis').hidden = true;
        try {
            const {response, data} = await apiJson('/enums/usuario');
            if (!response.ok || !data?.perfis?.length) throw new Error('Não foi possível carregar os perfis.');
            if (!select.isConnected) return;
            perfis = data.perfis;
            select.innerHTML = '<option value="">Selecione o perfil</option>' + perfis.map(perfil =>
                `<option value="${escapar(perfil.valor)}">${escapar(perfil.descricao)}</option>`).join('');
            select.disabled = false;
            campo('usuario-cadastrar').disabled = false;
            if (usuarios.length) renderizarUsuarios();
        } catch (erro) {
            if (!select.isConnected) return;
            select.innerHTML = '<option value="">Perfis indisponíveis</option>';
            campo('usuarios-erro-perfis').textContent = erro.message;
            campo('usuarios-erro-perfis').hidden = false;
        }
    }

    function filtrar(tipo) {
        ativos = tipo;
        campo('btn-usuarios-ativos').classList.toggle('ativo-selecionado', ativos);
        campo('btn-usuarios-inativos').classList.toggle('inativo-selecionado', !ativos);
        campo('btn-usuarios-ativos').setAttribute('aria-pressed', String(ativos));
        campo('btn-usuarios-inativos').setAttribute('aria-pressed', String(!ativos));
        listarUsuarios(0);
    }

    conteudo.addEventListener('pagina:carregada', event => {
        if (event.detail.modulo !== 'usuario') return;
        usuarios = [];
        perfis = [];
        filtrar(true);
        carregarPerfis();
    });

    conteudo.addEventListener('click', event => {
        const botao = event.target.closest('button');
        if (botao?.disabled) return;
        if (botao?.dataset.desativarUsuario) abrirDesativacao(botao.dataset.desativarUsuario);
        if (botao?.dataset.senhaUsuario) abrirAlteracaoSenha(botao.dataset.senhaUsuario);
        switch (event.target.id) {
            case 'btn-usuarios-ativos':
                filtrar(true);
                break;
            case 'btn-usuarios-inativos':
                filtrar(false);
                break;
            case 'usuarios-anterior':
                listarUsuarios(pagina - 1);
                break;
            case 'usuarios-proximo':
                listarUsuarios(pagina + 1);
                break;
            case 'usuarios-recarregar':
                listarUsuarios();
                break;
        }
    });

    conteudo.addEventListener('submit', async event => {
        if (event.target.id !== 'form-usuario') return;
        event.preventDefault();
        const form = event.target;
        const botao = campo('usuario-cadastrar');
        if (botao.disabled || !form.reportValidity()) return;
        const dados = {
            nome: campo('usuario-nome').value.trim(),
            username: campo('usuario-username').value.trim(),
            senha: campo('usuario-senha').value,
            perfilUsuarioNome: campo('usuario-perfil').value
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
            filtrar(true);
        } catch (erro) {
            alert(erro.message || 'Erro ao conectar com o servidor.');
        } finally {
            botao.disabled = false;
            botao.textContent = 'Cadastrar Usuário';
        }
    });

    async function carregarModalUsuario(arquivo, titulo) {
        const area = campo('area-usuarios');
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

    async function abrirDesativacao(id) {
        const usuario = usuarios.find(item => String(item.id) === id);
        if (!usuario?.ativo) return;
        if (!await carregarModalUsuario('modal-desativar-usuario.html', 'Desativar Usuário')) return;
        campo('usuario-mensagem-desativacao').textContent = `Tem certeza que deseja desativar o usuário ${usuario.nome}?`;
        campo('usuario-cancelar').onclick = fecharModal;
        campo('usuario-confirmar-desativacao').onclick = async event => {
            const botao = event.currentTarget;
            botao.disabled = true;
            try {
                const {response, data} = await apiJson(`/usuarios/${encodeURIComponent(id)}`, {method: 'PATCH'});
                if (!response.ok) throw new Error(data?.mensagem || 'Erro ao desativar usuário.');
                if (botao.isConnected) fecharModal();
                alert('Usuário desativado com sucesso!');
                listarUsuarios(pagina);
            } catch (erro) {
                alert(erro.message || 'Erro ao conectar com o servidor.');
                botao.disabled = false;
            }
        };
    }

    async function abrirAlteracaoSenha(id) {
        const usuario = usuarios.find(item => String(item.id) === id);
        if (!usuario || !usuarioLogado(usuario)) return;
        if (!await carregarModalUsuario('modal-alterar-senha.html', 'Alterar minha senha')) return;
        campo('usuario-cancelar').onclick = fecharModal;
        campo('form-senha-usuario').onsubmit = async event => {
            event.preventDefault();
            if (!usuarioLogado(usuario)) return;
            const form = event.currentTarget;
            const botao = form.querySelector('[type="submit"]');
            if (botao.disabled || !form.reportValidity()) return;
            const dados = {
                senhaAtual: campo('usuario-senha-atual').value,
                novaSenha: campo('usuario-nova-senha').value,
                novaSenhaConfirmacao: campo('usuario-confirmar-senha').value
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
        };
        campo('usuario-senha-atual').focus();
    }
})();
