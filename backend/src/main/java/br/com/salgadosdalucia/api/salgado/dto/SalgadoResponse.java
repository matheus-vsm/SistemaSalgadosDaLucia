package br.com.salgadosdalucia.api.salgado.dto;

import br.com.salgadosdalucia.api.salgado.Categoria;

import java.math.BigDecimal;

public record SalgadoResponse(
        Long id,
        String nome,
        String descricao,
        Categoria categoria,
        BigDecimal precoCentoCongelado,
        BigDecimal precoCentoProcessado,
        boolean ativo
) {
}
