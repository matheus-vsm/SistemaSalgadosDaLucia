package br.com.salgadosdalucia.api.compra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemCompraRequest(
        @NotBlank(message = "O nome do item é obrigatório!")
        String nome,
        @NotNull(message = "A quantidade do item é obrigatória!")
        Integer quantidade,
        @NotNull(message = "O preço unitário do item é obrigatório!")
        BigDecimal precoUnitario
) {
}
