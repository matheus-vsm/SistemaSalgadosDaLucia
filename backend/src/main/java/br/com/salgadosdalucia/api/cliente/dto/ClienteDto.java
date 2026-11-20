package br.com.salgadosdalucia.api.cliente.dto;

import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record ClienteDto(
        @NotBlank(message = "Nome é obrigatório!")
        @Size(max = 100, message = "O campo nome deve ter até 100 caracteres!")
        String nome,
        @NotBlank(message = "Telefone é obrigatório!")
        @Size(max = 20, message = "O campo telefone deve ter até 20 caracteres!")
        String telefone,
        @NotNull
        @Valid
        EnderecoDto endereco
) {
}
