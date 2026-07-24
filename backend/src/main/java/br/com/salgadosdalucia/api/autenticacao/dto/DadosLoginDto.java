package br.com.salgadosdalucia.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record DadosLoginDto(
        @NotBlank(message = "Username é obrigatório!")
        String username,
        @NotBlank(message = "Senha é obrigatória!")
        String senha) {
}
