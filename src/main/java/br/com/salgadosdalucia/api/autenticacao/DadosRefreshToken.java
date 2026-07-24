package br.com.salgadosdalucia.api.autenticacao;

import jakarta.validation.constraints.NotBlank;

public record DadosRefreshToken(
        @NotBlank
        String refreshToken
) {
}
