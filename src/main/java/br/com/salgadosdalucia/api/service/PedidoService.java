package br.com.salgadosdalucia.api.service;

import br.com.salgadosdalucia.api.model.ItemPedido;
import br.com.salgadosdalucia.api.model.Pedido;
import br.com.salgadosdalucia.api.model.enums.TipoEntrega;
import br.com.salgadosdalucia.api.model.enums.TipoPreco;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private void calcularValorTotal(Pedido pedido) {
        if (pedido == null) return;

        if (pedido.getItens().isEmpty()) {
            pedido.setValorTotal(0.0);
            return;
        }

        pedido.setValorTotal(pedido.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .mapToDouble(ItemPedido::getSubTotal)
                .sum());
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
        double precoCento;

        if (item.getTipoPreco() == TipoPreco.CONGELADO) {
            precoCento = item.getSalgado().getPrecoCentoCongelado();
        } else {
            precoCento = item.getSalgado().getPrecoCentoProcessado();
        }

        item.setPrecoUnitario(precoCento / 100.0);
        item.setSubTotal(item.getPrecoUnitario() * item.getQuantidade());
    }

}
