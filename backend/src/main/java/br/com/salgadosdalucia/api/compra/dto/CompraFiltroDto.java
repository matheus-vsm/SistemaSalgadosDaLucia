package br.com.salgadosdalucia.api.compra.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CompraFiltroDto(
        @DateTimeFormat(pattern = "dd-MM-yyyy")
        LocalDate dataInicioCompra,
        @DateTimeFormat(pattern = "dd-MM-yyyy")
        LocalDate dataFimCompra,
        String nomeItem,
        String observacao
) {
}
