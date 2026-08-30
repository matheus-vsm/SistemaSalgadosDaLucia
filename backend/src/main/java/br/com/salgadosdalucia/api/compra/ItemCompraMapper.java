package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.ItemCompraResponse;

import java.util.List;

public class ItemCompraMapper {

    public static List<ItemCompraResponse> mapToResponse(List<ItemCompra> itensCompra) {
        return itensCompra.stream()
                .map(item -> new ItemCompraResponse(
                        item.getId(),
                        item.getNome(),
                        item.getQuantidade(),
                        item.getValorUnitario(),
                        item.getSubTotal()
                )).toList();
    }

}
