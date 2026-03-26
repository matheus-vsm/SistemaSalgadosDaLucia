package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.model.Compra;
import br.com.salgadosdalucia.api.model.ItemCompra;
import org.springframework.stereotype.Service;

@Service
public class CompraService {

    private void calcularValorTotal(Compra compra) {
        if (compra == null) return;

        if (compra.getItens().isEmpty()) {
            compra.setValorTotal(0.0);
            return;
        }

        compra.setValorTotal(compra.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .mapToDouble(ItemCompra::getSubTotal)
                .sum());
    }

    private void calcularSubTotal(ItemCompra item) {
        item.setSubTotal(item.getPrecoUnitario() * item.getQuantidade());
    }

}
