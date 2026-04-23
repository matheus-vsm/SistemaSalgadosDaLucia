package br.com.salgadosdalucia.api.compra.dto;

import java.math.BigDecimal;

public record ItemCompraResponse(
        String nome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subTotal
) {
}
