package br.com.salgadosdalucia.api.cliente;

import br.com.salgadosdalucia.api.cliente.dto.ClienteDto;
import br.com.salgadosdalucia.api.cliente.dto.ClientePedidoDto;
import br.com.salgadosdalucia.api.cliente.dto.ClienteResponse;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;

public class ClienteMapper {

    public static ClienteDto mapToDto(Cliente cliente) {
        return new ClienteDto(cliente.getNome(), cliente.getTelefone(),
                EnderecoMapper.mapToDto(cliente.getEndereco()));
    }

    public static Cliente mapToEntity(ClienteDto dto) {
        return new Cliente(null, dto.nome(), dto.telefone(), true,
                EnderecoMapper.mapToEntity(dto.endereco()));
    }

    public static ClientePedidoDto mapToClientePedido(Cliente cliente) {
        return new ClientePedidoDto(cliente.getId(), cliente.getNome());
    }

    public static ClienteResponse mapToResponse(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getTelefone(), cliente.isAtivo(),
                EnderecoMapper.mapToDto(cliente.getEndereco()));
    }

}
