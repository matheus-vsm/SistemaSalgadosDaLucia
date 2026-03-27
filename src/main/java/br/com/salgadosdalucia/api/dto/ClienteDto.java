package br.com.salgadosdalucia.api.dto;

import br.com.salgadosdalucia.api.model.Endereco;
import jakarta.validation.constraints.NotBlank;

public record ClienteDto(
        @NotBlank
        String nome,
        @NotBlank
        String telefone,
        @NotBlank
        Endereco endereco
) {
}
