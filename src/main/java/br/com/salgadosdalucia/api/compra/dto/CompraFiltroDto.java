package br.com.salgadosdalucia.api.compra.dto;

import java.time.LocalDate;

public record CompraFiltroDto(
        LocalDate dataCompra,
        LocalDate dataInicioCompra,
        LocalDate dataFimCompra,
        String nomeItem,
        String observacao
) {
}
