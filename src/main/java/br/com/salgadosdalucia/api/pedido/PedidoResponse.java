package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.shared.endereco.Endereco;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long clienteid,
        List<ItemPedidoResponse> itens,
        Endereco enderecoEntrega,
        BigDecimal valorTotal,
        LocalDate dataPedido,
        LocalDateTime dataEntrega,
        StatusPedido status,
        TipoEntrega tipoEntrega,
        FormaPagamento formaPagamento,
        Long usuarioResponsavelId,
        String usuarioNome
) {
    public PedidoResponse(Pedido pedido) {
        this(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getItens().stream().map(ItemPedidoResponse::new).toList(),
                pedido.getEnderecoEntrega(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getDataEntrega(),
                pedido.getStatus(),
                pedido.getTipoEntrega(),
                pedido.getFormaPagamento(),
                pedido.getUsuarioResponsavel().getId(),
                pedido.getUsuarioResponsavel().getNome()
        );
    }
}
