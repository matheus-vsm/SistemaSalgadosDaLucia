package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraResponse;

public class CompraMapper {

    public static CriacaoCompraResponse mapToResponse(Compra compra) {
        return new CriacaoCompraResponse(
                compra.getId(),
                ItemCompraMapper.mapToResponse(compra.getItens()),
                compra.getValorTotal(),
                compra.getDataCompra(),
                compra.getObservacao()
        );
    }

}
