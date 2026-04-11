package br.com.salgadosdalucia.api.compra;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CompraService {

    private void calcularValorTotal(Compra compra) {
        if (compra == null) return;

        if (compra.getItens().isEmpty()) {
            compra.setValorTotal(BigDecimal.ZERO);
            return;
        }

        compra.setValorTotal(compra.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .map(ItemCompra::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void calcularSubTotal(ItemCompra item) {
        item.setSubTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
    }

}
