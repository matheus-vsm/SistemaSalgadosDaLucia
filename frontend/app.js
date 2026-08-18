document.addEventListener('DOMContentLoaded', function () {
    const formLogin = document.getElementById('form-login');
    const telaLogin = document.getElementById('tela-login');
    const painelSistema = document.getElementById('painel-sistema');
    const conteudoDinamico = document.getElementById('conteudo-dinamico');

    const MODULOS = {
        inicio: {
            html: 'inicio/html/inicio.html'
        },
        pedido: {
            html: 'pedidos/html/pedidos.html'
        },
        salgado: {
            html: 'salgado/html/salgado.html',
            css: 'salgado/css/salgado.css',
            js: 'salgado/js/salgado.js'
        },
        cliente: {
            html: 'cliente/html/cliente.html',
            css: 'cliente/css/cliente.css',
            js: 'cliente/js/cliente.js'
        },
        compra: {
            html: 'compras/html/compras.html'
        },
        estoque: {
            html: 'estoque/html/estoque.html'
        },
        usuario: {
            html: 'usuarios/html/usuarios.html'
        }
    };

    const scriptsCarregados = new Set();

    function carregarCss(href) {
        const anterior = document.getElementById('page-css');
        if (anterior) anterior.remove();

        if (!href) return;

        const link = document.createElement('link');
        link.id = 'page-css';
        link.rel = 'stylesheet';
        link.href = href;
        document.head.appendChild(link);
    }

    function carregarScript(src) {
        if (scriptsCarregados.has(src)) {
            return Promise.resolve();
        }

        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = () => {
                scriptsCarregados.add(src);
                resolve();
            };
            script.onerror = () => reject(new Error(`Erro ao carregar script: ${src}`));
            document.body.appendChild(script);
        });
    }

    async function carregarModulo(nomeModulo) {
        if (!conteudoDinamico) return;

        const modulo = MODULOS[nomeModulo];
        if (!modulo) {
            conteudoDinamico.innerHTML = '<h1>Erro</h1><p>Módulo não encontrado.</p>';
            return;
        }

        carregarCss(modulo.css);

        try {
            const response = await fetch(modulo.html);
            if (!response.ok) throw new Error('Erro ao carregar o arquivo HTML');

            const html = await response.text();
            conteudoDinamico.innerHTML = html;

            if (modulo.js) {
                await carregarScript(modulo.js);
            }

            conteudoDinamico.dispatchEvent(new CustomEvent('pagina:carregada', {
                detail: {modulo: nomeModulo, url: modulo.html}
            }));
        } catch (erro) {
            conteudoDinamico.innerHTML = '<h1>Erro 404</h1><p>Não foi possível carregar a tela correspondente.</p>';
            console.error(erro);
        }
    }

    if (formLogin) {
        formLogin.addEventListener('submit', async (event) => {
            event.preventDefault();

            const dadosLogin = {
                username: document.getElementById('usuario').value,
                senha: document.getElementById('senha').value
            };

            try {
                const {response, data: result} = await apiJson('/autenticacao/login', {
                    method: 'POST',
                    body: JSON.stringify(dadosLogin),
                    skipAuth: true
                });

                if (response.ok) {
                    localStorage.setItem('accessToken', result.tokenAcesso);
                    localStorage.setItem('refreshToken', result.refreshToken);

                    telaLogin.classList.add('hidden');
                    painelSistema.classList.remove('hidden');

                    await carregarModulo('inicio');
                    alert('Login realizado com sucesso!');
                } else {
                    alert('Usuário ou senha inválidos');
                }
            } catch (erro) {
                console.error('Erro na requisição:', erro);
                alert('Erro ao conectar com o servidor.');
            }
        });
    }

    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function (event) {
            event.preventDefault();

            const linkAtivoAnterior = document.querySelector('.nav-link.active');
            if (linkAtivoAnterior) linkAtivoAnterior.classList.remove('active');
            this.classList.add('active');

            carregarModulo(this.dataset.module);
        });
    });
});
