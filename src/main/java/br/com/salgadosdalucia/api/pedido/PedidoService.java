package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.cliente.ClienteRepository;
import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.pedido.dto.ItemPedidoDto;
import br.com.salgadosdalucia.api.pedido.dto.CriacaoPedidoRequest;
import br.com.salgadosdalucia.api.pedido.dto.CriacaoPedidoResponse;
import br.com.salgadosdalucia.api.pedido.dto.PedidoListagemDto;
import br.com.salgadosdalucia.api.salgado.Salgado;
import br.com.salgadosdalucia.api.salgado.SalgadoRepository;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public CriacaoPedidoResponse cadastrar(CriacaoPedidoRequest request) throws BadRequestException {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new BadRequestException("Cliente não encontrado com ID: " + request.clienteId()));
        Usuario usuario = usuarioRepository.findById(request.usuarioResponsavelId())
                .orElseThrow(() -> new BadRequestException("Usuário não encontrado com ID: " + request.usuarioResponsavelId()));
        if (!cliente.isAtivo()) {
            throw new BadRequestException("Cliente com ID " + request.clienteId() + " está inativo e não pode realizar pedidos.");
        }
        if (!usuario.isAtivo()) {
            throw new BadRequestException("Usuário com ID " + request.usuarioResponsavelId() + " está inativo e não pode ser responsável por pedidos.");
        }

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
            if (!salgado.isAtivo()) {
                throw new BadRequestException("Salgado com ID " + item.salgadoId() + " está inativo e não pode ser adicionado ao pedido.");
            }

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

        return PedidoMapper.mapToCriacaoPedidoResponse(pedido);
    }

    public Page<PedidoListagemDto> listarTodos(Pageable paginacao) {
        return pedidoRepository.findAll(paginacao).map(PedidoMapper::mapToPedidoListagemDto);
    }

    public Page<PedidoListagemDto> listarTodosFiltrado(StatusPedido statusPedido,
                                                       Long clienteId,
                                                       String nomeCliente,
                                                       LocalDate dataPedido,
                                                       LocalDateTime dataEntrega,
                                                       TipoEntrega tipoEntrega,
                                                       FormaPagamento formaPagamento,
                                                       Long usuarioResponsavelId,
                                                       String nomeUsuarioResponsavel,
                                                       Pageable paginacao) {
        LocalDateTime inicioEntrega = null;
        LocalDateTime fimEntrega = null;

        if (dataEntrega != null) {
            inicioEntrega = dataEntrega.toLocalDate().atStartOfDay();
            fimEntrega = dataEntrega.toLocalDate().atTime(23, 59, 59);
        }

        return pedidoRepository.findWithFiltros(statusPedido, clienteId, nomeCliente, dataPedido,
                inicioEntrega, fimEntrega, tipoEntrega, formaPagamento, usuarioResponsavelId,
                nomeUsuarioResponsavel, paginacao).map(PedidoMapper::mapToPedidoListagemDto);
    }

    public PedidoListagemDto buscarPorId(Long id) throws BadRequestException {
        return PedidoMapper.mapToPedidoListagemDto(pedidoRepository.findById(id).orElseThrow(() -> new BadRequestException("Pedido não encontrado com ID: " + id)));
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
                .reduce(BigDecimal.ZERO, BigDecimal::add)); // começa com 0 e vai somando todos os valores
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

        item.setPrecoUnitario(
                precoCento.divide(BigDecimal.valueOf(100),
                        2, // casas decimais
                        RoundingMode.HALF_UP)); // divide por 100 e arredonda para normalmente (0.5 pra cima)
        item.setSubTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))); // = item.getPrecoUnitario() * item.getQuantidade()
    }

}
