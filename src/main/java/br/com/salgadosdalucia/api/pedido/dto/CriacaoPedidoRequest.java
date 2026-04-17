package br.com.salgadosdalucia.api.pedido.dto;

import br.com.salgadosdalucia.api.pedido.FormaPagamento;
import br.com.salgadosdalucia.api.pedido.TipoEntrega;
import br.com.salgadosdalucia.api.shared.endereco.EnderecoDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CriacaoPedidoRequest(
        @NotNull(message = "ID no Cliente é obrigatório!")
        Long clienteId,
        @NotEmpty(message = "É necessário adicionar pelo menos um item ao pedido!")
        List<ItemPedidoDto> itens,
        @Valid
        EnderecoDto enderecoEntrega,
        LocalDate dataPedido,
        @NotNull(message = "Data de entrega é obrigatória!")
        @Future(message = "Data de Entrega deve ser no futuro!")
        LocalDateTime dataEntrega,
        @NotNull(message = "Tipo de entrega é obrigatório!")
        TipoEntrega tipoEntrega,
        @NotNull(message = "Forma de pagamento é obrigatória!")
        FormaPagamento formaPagamento,
        @NotNull(message = "ID do Usuário Responsável é obrigatório!")
        Long usuarioResponsavelId
) {
}
