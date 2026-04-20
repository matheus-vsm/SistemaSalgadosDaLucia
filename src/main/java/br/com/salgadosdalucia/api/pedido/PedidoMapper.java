package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.cliente.ClienteMapper;
import br.com.salgadosdalucia.api.pedido.dto.CriacaoPedidoResponse;
import br.com.salgadosdalucia.api.pedido.dto.PedidoListagemDto;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoMapper;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioMapper;

public class PedidoMapper {

    public static CriacaoPedidoResponse mapToCriacaoPedidoResponse(Pedido pedido) {
        return new CriacaoPedidoResponse(
                pedido.getId(),
                ClienteMapper.mapToClientePedido(pedido.getCliente()),
                ItemPedidoMapper.mapToItemPedidoResponse(pedido.getItens()),
                pedido.getEnderecoEntrega(),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getDataEntrega(),
                pedido.getStatus(),
                pedido.getTipoEntrega(),
                pedido.getFormaPagamento(),
                UsuarioMapper.mapToUsuarioPedidoDto(pedido.getUsuarioResponsavel())
        );
    }

    public static PedidoListagemDto mapToPedidoListagemDto(Pedido pedido) {
        return new PedidoListagemDto(
                pedido.getId(),
                ClienteMapper.mapToClientePedido(pedido.getCliente()),
                pedido.getStatus(),
                ItemPedidoMapper.mapToItemPedidoResponse(pedido.getItens()),
                pedido.getValorTotal(),
                pedido.getDataPedido(),
                pedido.getDataEntrega(),
                pedido.getTipoEntrega(),
                pedido.getEnderecoEntrega() != null ? EnderecoMapper.mapToDto(pedido.getEnderecoEntrega()) : null ,
                pedido.getFormaPagamento(),
                UsuarioMapper.mapToUsuarioPedidoDto(pedido.getUsuarioResponsavel())
        );
    }
}
