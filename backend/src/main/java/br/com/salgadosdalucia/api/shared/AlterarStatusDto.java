package br.com.salgadosdalucia.api.shared;

import jakarta.validation.constraints.NotNull;

public record AlterarStatusDto(
        @NotNull(message = "True ou False obrigatório")
        Boolean status
) {
}
