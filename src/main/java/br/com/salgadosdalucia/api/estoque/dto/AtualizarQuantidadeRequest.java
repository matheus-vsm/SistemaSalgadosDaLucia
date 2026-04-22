package br.com.salgadosdalucia.api.estoque.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarQuantidadeRequest(
        @NotNull(message = "Quantidade é obrigatória!")
        Integer quantidade
) {
}
