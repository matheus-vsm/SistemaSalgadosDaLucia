package br.com.salgadosdalucia.api.pedido;

import jakarta.validation.constraints.NotNull;

public record ItemPedidoDto(
        @NotNull(message = "ID do Salgado é obrigatório!")
        Long salgadoId,
        @NotNull(message = "Quantidade do Item é obrigatória!")
        Integer quantidade,
        @NotNull(message = "Tipo de Preço do Item é obrigatório!")
        TipoPreco tipoPreco
) {
}
