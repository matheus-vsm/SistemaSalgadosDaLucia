package br.com.salgadosdalucia.api.compra.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CriacaoCompraRequest(
        @NotNull(message = "Os itens da compra são obrigatórios!")
        List<ItemCompraRequest> itens,
        LocalDate dataCompra,
        String observacao
) {
}
