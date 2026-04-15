package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.cliente.ClienteRepository;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.salgado.Salgado;
import br.com.salgadosdalucia.api.salgado.SalgadoRepository;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    private final ClienteRepository clienteRepository;

    private final SalgadoRepository salgadoRepository;

    private final UsuarioRepository usuarioRepository;

    @Transactional(rollbackFor = Exception.class)
    public PedidoResponse cadastrar(PedidoRequest request) throws BadRequestException {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new BadRequestException("Cliente não encontrado com ID: " + request.clienteId()));
        Usuario usuario = usuarioRepository.findById(request.usuarioResponsavelId())
                .orElseThrow(() -> new BadRequestException("Usuário não encontrado com ID: " + request.usuarioResponsavelId()));

        List<ItemPedido> itens = new ArrayList<>();
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .dataEntrega(request.dataEntrega())
                .dataPedido(request.dataPedido())
                .status(StatusPedido.EM_ANDAMENTO)
                .tipoEntrega(request.tipoEntrega())
                .formaPagamento(request.formaPagamento())
                .usuarioResponsavel(usuario)
                .build();

        for (ItemPedidoDto item : request.itens()) {
            Salgado salgado = salgadoRepository.findById(item.salgadoId())
                    .orElseThrow(() -> new BadRequestException("Salgado não encontrado com ID: " + item.salgadoId()));
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setSalgado(salgado);
            itemPedido.setQuantidade(item.quantidade());
            itemPedido.setTipoPreco(item.tipoPreco());
            itemPedido.setPedido(pedido); // Associa o ItemPedido ao Pedido
            calcularPrecos(itemPedido);
            itens.add(itemPedido);
        }

        pedido.setItens(itens);
        calcularValorTotal(pedido);
        defineEnderecoEntrega(pedido);

        pedidoRepository.save(pedido);

        return new PedidoResponse(pedido);
    }

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
