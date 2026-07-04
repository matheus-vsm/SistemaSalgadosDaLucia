package br.com.salgadosdalucia.api.autenticacao.dto;

public record TokenResponse(
        String tokenAcesso,
        String refreshToken
) {
}
