package br.com.salgadosdalucia.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record DadosLogin(
        @NotBlank
        String username,
        @NotBlank
        String senha) {
}
