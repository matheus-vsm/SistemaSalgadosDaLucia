package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraResponse;

public class CompraMapper {

    public static CompraResponse mapToResponse(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                ItemCompraMapper.mapToResponse(compra.getItens()),
                compra.getValorTotal(),
                compra.getDataCompra(),
                compra.getObservacao()
        );
    }

}
