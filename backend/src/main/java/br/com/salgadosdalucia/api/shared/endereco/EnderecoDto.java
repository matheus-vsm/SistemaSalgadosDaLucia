package br.com.salgadosdalucia.api.shared.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoDto(
        @NotBlank(message = "Logradouro é obrigatório!")
        @Size(max = 150, message = "O campo logradouro deve ter até 150 caracteres!")
        String logradouro,
        @NotBlank(message = "Numero é obrigatório!")
        @Size(max = 10, message = "O campo numero deve ter até 10 caracteres!")
        String numero,
        @Size(max = 100, message = "O campo complemento deve ter até 100 caracteres!")
        String complemento,
        @NotBlank(message = "CEP é obrigatório!")
        @Size(max = 10, message = "O campo cep deve ter até 10 caracteres!")
        String cep,
        @NotBlank(message = "Bairro é obrigatório!")
        @Size(max = 100, message = "O campo bairro deve ter até 100 caracteres!")
        String bairro,
        @NotBlank(message = "Cidade é obrigatório!")
        @Size(max = 100, message = "O campo cidade deve ter até 100 caracteres!")
        String cidade,
        @NotBlank(message = "UF é obrigatório!")
        @Size(min = 2, max = 2)
        String uf
) {
}
