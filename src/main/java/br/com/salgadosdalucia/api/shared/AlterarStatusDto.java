package br.com.salgadosdalucia.api.shared;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusDto(
        @NotNull(message = "true ou false obrigatório")
        Boolean status
) {
}
