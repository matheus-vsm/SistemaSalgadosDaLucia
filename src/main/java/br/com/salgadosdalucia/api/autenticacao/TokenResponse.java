package br.com.salgadosdalucia.api.autenticacao;

public record TokenResponse(
        String tokenAcesso,
        String refreshToken
) {
}
