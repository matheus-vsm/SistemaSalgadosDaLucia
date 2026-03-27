package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.model.ItemPedido;
import br.com.salgadosdalucia.api.model.Pedido;
import br.com.salgadosdalucia.api.model.enums.TipoEntrega;
import br.com.salgadosdalucia.api.model.enums.TipoPreco;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PedidoService {

    private void calcularValorTotal(Pedido pedido) {
        if (pedido == null) return;

        if (pedido.getItens().isEmpty()) {
            pedido.setValorTotal(BigDecimal.ZERO);
            return;
        }

        pedido.setValorTotal(pedido.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .map(ItemPedido::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void defineEnderecoEntrega(Pedido pedido) {
        if (pedido.getTipoEntrega() == TipoEntrega.RETIRADA) {
            pedido.setEnderecoEntrega(null); // Retirada não tem endereço de entrega
        } else if (pedido.getTipoEntrega() == TipoEntrega.ENTREGA && pedido.getEnderecoEntrega() == null) {
            // Se for entrega e o endereço não foi definido, pode-se lançar uma exceção ou definir um endereço padrão
            pedido.setEnderecoEntrega(pedido.getCliente().getEndereco());
        }
    }

    private void calcularPrecos(ItemPedido item) {
        BigDecimal precoCento;

        if (item.getTipoPreco() == TipoPreco.CONGELADO) {
            precoCento = item.getSalgado().getPrecoCentoCongelado();
        } else {
            precoCento = item.getSalgado().getPrecoCentoProcessado();
        }

        item.setPrecoUnitario(precoCento.divide(BigDecimal.valueOf(100))); // = precoCento / 100.0
        item.setSubTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))); // = item.getPrecoUnitario() * item.getQuantidade()
    }

}
