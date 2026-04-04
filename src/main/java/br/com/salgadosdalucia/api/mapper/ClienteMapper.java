package br.com.salgadosdalucia.api.mapper;

import br.com.salgadosdalucia.api.dto.ClienteDto;
import br.com.salgadosdalucia.api.model.Cliente;

public class ClienteMapper {

    public static ClienteDto mapToDto(Cliente cliente) {
        return new ClienteDto(cliente.getNome(), cliente.getTelefone(),
                EnderecoMapper.mapToDto(cliente.getEndereco()));
    }

    public static Cliente mapToEntity(ClienteDto dto) {
        return new Cliente(null, dto.nome(), dto.telefone(), true,
                EnderecoMapper.mapToEntity(dto.endereco()));
    }

}
