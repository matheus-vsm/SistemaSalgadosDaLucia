package br.com.salgadosdalucia.api.pedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long id,
        Long pedidoId,
        String nomeSalgado,
        Integer quantidade,
        TipoPreco tipoPreco,
        BigDecimal precoUnitario,
        BigDecimal subTotal
) {
    public ItemPedidoResponse(ItemPedido itemPedido) {
        this(
                itemPedido.getId(),
                itemPedido.getPedido().getId(),
                itemPedido.getSalgado().getNome(),
                itemPedido.getQuantidade(),
                itemPedido.getTipoPreco(),
                itemPedido.getPrecoUnitario(),
                itemPedido.getSubTotal()
        );
    }
}
