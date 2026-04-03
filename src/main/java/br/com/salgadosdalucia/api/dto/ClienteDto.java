package br.com.salgadosdalucia.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteDto(
        @NotBlank(message = "Nome é obrigatório!")
        String nome,
        @NotBlank(message = "Telefone é obrigatório!")
        String telefone,
        @NotNull
        @Valid
        EnderecoDto endereco
) {
}
