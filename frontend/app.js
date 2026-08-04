document.addEventListener("DOMContentLoaded", function () {
    const formLogin = document.getElementById('form-login');
    const telaLogin = document.getElementById('tela-login');
    const painelSistema = document.getElementById('painel-sistema');

    // Alvo onde o HTML externo vai ser injetado (Certifique-se de ter essa tag no seu HTML)
    const conteudoDinamico = document.getElementById('conteudo-dinamico');

    // --- FUNÇÃO PARA BUSCAR E INJETAR AS PÁGINAS HTML ---
    function carregarPagina(urlDoArquivo) {
        if (!conteudoDinamico) return;

        fetch(urlDoArquivo)
            .then(response => {
                if (!response.ok) throw new Error("Erro ao carregar o arquivo HTML");
                return response.text();
            })
            .then(html => {
                conteudoDinamico.innerHTML = html;
                // avisa o resto da aplicação que uma nova página foi injetada
                conteudoDinamico.dispatchEvent(new CustomEvent('pagina:carregada', {
                    detail: { url: urlDoArquivo }
                }));
            })
            .catch(erro => {
                conteudoDinamico.innerHTML = `<h1>Erro 404</h1><p>Não foi possível carregar a tela correspondente.</p>`;
                console.error(erro);
            });
    }

    // --- CONTROLE DE LOGIN (MANTIDO SEU BACKEND REAL) ---
    if (formLogin) {
        formLogin.addEventListener('submit', async (event) => {
            event.preventDefault();

            const dadosLogin = {
                username: document.getElementById('usuario').value,
                senha: document.getElementById('senha').value
            };

            try {
                const response = await fetch('http://localhost:8080/api/salgados-da-lucia-kojima/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dadosLogin)
                });

                const result = await response.json();

                if (response.ok) {
                    localStorage.setItem('accessToken', result.tokenAcesso);
                    localStorage.setItem('refreshToken', result.refreshToken);

                    telaLogin.classList.add('hidden');
                    painelSistema.classList.remove('hidden');

                    // Carrega a página inicial padrão logo após o login de sucesso
                    carregarPagina('inicio/html/inicio.html');

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

    // --- CONTROLE DE NAVEGAÇÃO COMPONENTIZADA (SEU ASIDE) ---
    const links = document.querySelectorAll('.nav-link');

    links.forEach(link => {
        link.addEventListener('click', function (event) {
            event.preventDefault();

            // 1. Remove classe ativa do link anterior e adiciona no atual
            const linkAtivoAnterior = document.querySelector('.nav-link.active');
            if (linkAtivoAnterior) linkAtivoAnterior.classList.remove('active');
            this.classList.add('active');

            // 2. Captura o caminho do arquivo HTML (Ex: paginas/pedidos.html)
            const arquivoParaCarregar = this.getAttribute('data-target');

            // 3. Executa a requisição da página para jogá-la no main
            carregarPagina(arquivoParaCarregar);
        });
    });
});