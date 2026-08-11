package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long id,
        Long pedidoId,
        String nomeSalgado,
        Integer quantidade,
        TipoPreco tipoPreco,
        BigDecimal precoUnitario,
        BigDecimal subTotal
) {
}
