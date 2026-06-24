package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.cliente.dto.ClientePedidoDto;
import br.com.salgadosdalucia.api.pedido.enums.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioPedidoDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoListagemDto(
        Long id,
        ClientePedidoDto cliente,
        StatusPedido status,
        List<ItemPedidoResponse> itens,
        BigDecimal frete,
        BigDecimal valorTotal,
        LocalDate dataPedido,
        LocalDateTime dataEntrega,
        TipoEntrega tipoEntrega,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        EnderecoDto enderecoEntrega,
        FormaPagamento formaPagamento,
        UsuarioPedidoDto usuarioResponsavel
) {
}
