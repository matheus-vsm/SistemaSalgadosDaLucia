package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.pedido.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.StatusPedido;
import br.com.salgadosdalucia.api.pedido.TipoEntrega;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoFiltroDto(
        StatusPedido statusPedido,
        Long clienteId,
        String nomeCliente,
        LocalDate dataPedido,
        LocalDateTime dataEntrega,
        TipoEntrega tipoEntrega,
        FormaPagamento formaPagamento,
        Long usuarioResponsavelId,
        String nomeUsuarioResponsavel
) {
}
