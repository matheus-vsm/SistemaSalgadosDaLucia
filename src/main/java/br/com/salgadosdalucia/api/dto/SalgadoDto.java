package br.com.salgadosdalucia.api.dto;

import br.com.salgadosdalucia.api.model.enums.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SalgadoDto(
        @NotBlank(message = "Nome do Salgado é obrigatório!")
        String nome,
        @NotBlank(message = "Descrição do Salgado é obrigatório!")
        String descricao,
        @NotNull(message = "É necessário definir uma Categoria para o Salgado!")
        Categoria categoria,
        @NotNull(message = "Preço do Cento Congelado do Salgado é obrigatório")
        BigDecimal precoCentoCongelado,
        @NotNull(message = "Preço do Cento Frito/Assado do Salgado é obrigatório")
        BigDecimal precoCentoProcessado
) {
}
