package br.com.salgadosdalucia.api.cliente.dto;

import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;

public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        boolean ativo,
        EnderecoDto endereco
) {
}
