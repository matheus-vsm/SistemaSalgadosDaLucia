// Altere esta URL caso a API seja publicada em outro endereço.
const API_BASE_URL = "http://localhost:8080";

const menuButton = document.querySelector(".menu-button");
const menu = document.querySelector(".sidebar");
const newOrderButton = document.querySelector("#new-order-button");

menuButton.addEventListener("click", () => {
  const isOpen = menu.classList.toggle("open");
  menuButton.setAttribute("aria-expanded", String(isOpen));
});

newOrderButton.addEventListener("click", () => {
  window.alert(`A tela de novo pedido será conectada à API em ${API_BASE_URL}.`);
});
