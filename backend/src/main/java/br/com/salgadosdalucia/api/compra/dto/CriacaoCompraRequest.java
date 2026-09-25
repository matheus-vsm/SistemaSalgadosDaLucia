package br.com.salgadosdalucia.api.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CriacaoCompraRequest(
        @NotEmpty(message = "Adicione pelo menos um item à compra!")
        List<@Valid ItemCompraRequest> itens,
        LocalDate dataCompra,
        @Size(max = 255, message = "A observação deve ter até 255 caracteres!")
        String observacao
) {
}
