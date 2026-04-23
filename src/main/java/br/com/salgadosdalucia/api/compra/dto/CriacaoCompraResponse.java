package br.com.salgadosdalucia.api.compra.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CriacaoCompraResponse(
        Long id,
        List<ItemCompraResponse> itens,
        BigDecimal valorTotal,
        LocalDate dataCompra,
        String observacao
) {
}
