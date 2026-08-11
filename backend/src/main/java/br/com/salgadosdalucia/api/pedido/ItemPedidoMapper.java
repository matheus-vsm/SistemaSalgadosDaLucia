package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.pedido.dto.ItemPedidoResponse;

import java.util.List;

public class ItemPedidoMapper {

    public static List<ItemPedidoResponse> mapToItemPedidoResponse(List<ItemPedido> itensPedido) {
        return itensPedido.stream()
                .map(itemPedido -> new ItemPedidoResponse(
                        itemPedido.getId(),
                        itemPedido.getPedido().getId(),
                        itemPedido.getSalgado().getNome(),
                        itemPedido.getQuantidade(),
                        itemPedido.getTipoPreco(),
                        itemPedido.getPrecoUnitario(),
                        itemPedido.getSubTotal()
                ))
                .toList();
    }

}
