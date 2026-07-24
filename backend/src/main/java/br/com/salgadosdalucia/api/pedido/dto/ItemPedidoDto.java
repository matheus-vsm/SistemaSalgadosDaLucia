package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoDto(
        @NotNull(message = "ID do Salgado é obrigatório!")
        Long salgadoId,
        @NotNull(message = "Quantidade do Item é obrigatória!")
        @Positive(message = "Quantidade do Item deve ser um número positivo!")
        Integer quantidade,
        @NotNull(message = "Tipo de Preço do Item é obrigatório!")
        TipoPreco tipoPreco
) {
}
