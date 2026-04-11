package br.com.salgadosdalucia.api.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoDto(
        @NotBlank(message = "Logradouro é obrigatório!")
        String logradouro,
        @NotBlank(message = "Numero é obrigatório!")
        String numero,
        String complemento,
        @NotBlank(message = "CEP é obrigatório!")
        String cep,
        @NotBlank(message = "Bairro é obrigatório!")
        String bairro,
        @NotBlank(message = "Cidade é obrigatório!")
        String cidade,
        @NotBlank(message = "UF é obrigatório!")
        @Size(min = 2, max = 2)
        String uf
) {
}
