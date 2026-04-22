package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.cliente.dto.ClientePedidoDto;
import br.com.salgadosdalucia.api.pedido.enums.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.shared.endereco.Endereco;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioPedidoDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CriacaoPedidoResponse(
        Long id,
        ClientePedidoDto cliente,
        List<ItemPedidoResponse> itens,
        Endereco enderecoEntrega,
        BigDecimal valorTotal,
        LocalDate dataPedido,
        LocalDateTime dataEntrega,
        StatusPedido status,
        TipoEntrega tipoEntrega,
        FormaPagamento formaPagamento,
        UsuarioPedidoDto usuarioResponsavel
) {
}
