document.addEventListener("DOMContentLoaded", function () {
    // 1. Busca o arquivo do menu
    fetch("html/menu.html")
        .then(response => response.text())
        .then(html => {
            // 2. Insere o menu na página atual
            document.getElementById("container-do-menu").innerHTML = html;

            // 3. Destaca a página atual no menu automaticamente
            marcarPaginaAtiva();
        });
});

function marcarPaginaAtiva() {
    // Pega o nome do arquivo atual (ex: "pedidos.html")
    const paginaAtual = window.location.pathname.split("/").pop();

    // Procura todos os links do menu
    const links = document.querySelectorAll(".nav-link");

    links.forEach(link => {
        // Se o href do link terminar com o nome da página atual, adiciona a classe active
        if (link.getAttribute("href") === paginaAtual || (paginaAtual === "" && link.getAttribute("href") === "index.html")) {
            link.classList.add("active");
        }
    });
}
