package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.Cliente;
import br.com.salgadosdalucia.api.cliente.ClienteRepository;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.pedido.dto.*;
import br.com.salgadosdalucia.api.pedido.enums.StatusPedido;
import br.com.salgadosdalucia.api.pedido.enums.TipoEntrega;
import br.com.salgadosdalucia.api.pedido.enums.TipoPreco;
import br.com.salgadosdalucia.api.salgado.Salgado;
import br.com.salgadosdalucia.api.salgado.SalgadoRepository;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public CriacaoPedidoResponse registrar(CriacaoPedidoRequest request) throws NotFoundException {
        Cliente cliente = ValidacaoEntidadeHelper.buscarEntidadePorId(clienteRepository, request.clienteId(), "Cliente");
        ValidacaoEntidadeHelper.validarStatusAtivo(cliente.isAtivo(), cliente.getId(), "Cliente", "não pode realizar pedidos.");
        Usuario usuario = ValidacaoEntidadeHelper.buscarEntidadePorId(usuarioRepository, request.usuarioResponsavelId(), "Usuario");
        ValidacaoEntidadeHelper.validarStatusAtivo(usuario.isAtivo(), usuario.getId(), "Usuario", "não pode ser responsável por pedidos.");

        List<ItemPedido> itens = new ArrayList<>();
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .dataEntrega(request.dataEntrega())
                .dataPedido(request.dataPedido())
                .status(StatusPedido.EM_ANDAMENTO)
                .tipoEntrega(request.tipoEntrega())
                .formaPagamento(request.formaPagamento())
                .usuarioResponsavel(usuario)
                .frete(request.frete())
                .build();

        for (ItemPedidoDto item : request.itens()) {
            Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, item.salgadoId(), "Salgado");
            ValidacaoEntidadeHelper.validarStatusAtivo(salgado.isAtivo(), salgado.getId(), "Salgado", "não pode ser adicionado ao pedido.");

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

    public Page<PedidoListagemDto> listarComFiltro(PedidoFiltroDto filtro, Pageable paginacao) {
        LocalDateTime inicioEntrega = null;
        LocalDateTime fimEntrega = null;

        if (filtro.dataEntrega() != null) {
            inicioEntrega = filtro.dataEntrega().toLocalDate().atStartOfDay();
            fimEntrega = filtro.dataEntrega().toLocalDate().atTime(23, 59, 59);
        }

        return pedidoRepository.findWithFiltros(filtro.statusPedido(), filtro.clienteId(), filtro.nomeCliente(),
                        filtro.dataPedido(), inicioEntrega, fimEntrega, filtro.tipoEntrega(), filtro.formaPagamento(),
                        filtro.usuarioResponsavelId(), filtro.nomeUsuarioResponsavel(), paginacao)
                .map(PedidoMapper::mapToPedidoListagemDto);
    }

    public PedidoListagemDto buscarPorId(Long id) throws NotFoundException {
        Pedido pedido = ValidacaoEntidadeHelper.buscarEntidadePorId(pedidoRepository, id, "Pedido");
        return PedidoMapper.mapToPedidoListagemDto(pedido);
    }

    @Transactional(rollbackFor = Exception.class)
    public CriacaoPedidoResponse atualizar(Long id, CriacaoPedidoRequest request) throws NotFoundException {
        Pedido pedido = ValidacaoEntidadeHelper.buscarEntidadePorId(pedidoRepository, id, "Pedido");
        if (pedido.getStatus() != StatusPedido.EM_ANDAMENTO) {
            throw new BusinessException("Somente pedidos com status EM_ANDAMENTO podem ser atualizados. Pedido ID " + id + " tem status " + pedido.getStatus());
        }

        Cliente cliente = ValidacaoEntidadeHelper.buscarEntidadePorId(clienteRepository, request.clienteId(), "Cliente");
        ValidacaoEntidadeHelper.validarStatusAtivo(cliente.isAtivo(), cliente.getId(), "Cliente", "não pode realizar pedidos.");
        Usuario usuario = ValidacaoEntidadeHelper.buscarEntidadePorId(usuarioRepository, request.usuarioResponsavelId(), "Usuario");
        ValidacaoEntidadeHelper.validarStatusAtivo(usuario.isAtivo(), usuario.getId(), "Usuario", "não pode ser responsável por pedidos.");
        
        pedido.setCliente(cliente);
        pedido.setDataEntrega(request.dataEntrega());
        pedido.setDataPedido(request.dataPedido());
        pedido.setTipoEntrega(request.tipoEntrega());
        pedido.setEnderecoEntrega(EnderecoMapper.mapToEntity(request.enderecoEntrega()));
        pedido.setFormaPagamento(request.formaPagamento());
        pedido.setUsuarioResponsavel(usuario);
        atualizarItensPedido(pedido, request.itens());

        calcularValorTotal(pedido);
        defineEnderecoEntrega(pedido);

        pedidoRepository.save(pedido);

        return PedidoMapper.mapToCriacaoPedidoResponse(pedido);
    }

    @Transactional(rollbackFor = Exception.class)
    public void alterarStatus(Long id, AlterarStatusPedidoDto status) throws NotFoundException {
        Pedido pedido = ValidacaoEntidadeHelper.buscarEntidadePorId(pedidoRepository, id, "Pedido");
        if (pedido.getStatus() == status.status()) {
            throw new BusinessException("O pedido já está com o status " + status.status());
        }

        pedido.setStatus(status.status());
    }

    private void calcularValorTotal(Pedido pedido) {
        if (pedido.getItens().isEmpty()) {
            pedido.setValorTotal(BigDecimal.ZERO);
            return;
        }

        pedido.setValorTotal(pedido.getItens().stream()
                .filter(item -> item.getSubTotal() != null)
                .map(ItemPedido::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)); // começa com 0 e vai somando todos os valores

        if (pedido.getFrete() != null) {
            pedido.setValorTotal(pedido.getValorTotal().add(pedido.getFrete()));
        }
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

    private void atualizarItensPedido(Pedido pedido, List<ItemPedidoDto> itensDto) throws NotFoundException {
        pedido.getItens().clear(); // remove antigos

        List<ItemPedido> novosItens = new ArrayList<>();

        for (ItemPedidoDto item : itensDto) {
            Salgado salgado = ValidacaoEntidadeHelper.buscarEntidadePorId(salgadoRepository, item.salgadoId(), "Salgado");
            ValidacaoEntidadeHelper.validarStatusAtivo(salgado.isAtivo(), salgado.getId(), "Salgado", "não pode ser adicionado ao pedido.");
            
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setSalgado(salgado);
            itemPedido.setQuantidade(item.quantidade());
            itemPedido.setTipoPreco(item.tipoPreco());

            calcularPrecos(itemPedido);

            novosItens.add(itemPedido);
        }

        pedido.getItens().addAll(novosItens);
    }

}
