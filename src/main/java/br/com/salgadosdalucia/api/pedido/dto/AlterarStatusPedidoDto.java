package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.pedido.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusPedidoDto(
        @NotNull(message = "Status é obrigatório!")
        StatusPedido status
) {
}
