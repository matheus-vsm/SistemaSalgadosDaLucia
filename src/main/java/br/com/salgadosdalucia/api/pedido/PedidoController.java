package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.pedido.dto.AlterarStatusPedidoDto;
import br.com.salgadosdalucia.api.pedido.dto.CriacaoPedidoRequest;
import br.com.salgadosdalucia.api.pedido.dto.CriacaoPedidoResponse;
import br.com.salgadosdalucia.api.pedido.dto.PedidoListagemDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {

    private final PedidoService service;

    @PostMapping
    public ResponseEntity<CriacaoPedidoResponse> cadastrarPedido(@RequestBody @Valid CriacaoPedidoRequest request,
                                                                 UriComponentsBuilder uriBuilder) throws BadRequestException {
        CriacaoPedidoResponse pedido = service.cadastrar(request);
        URI uri = uriBuilder.path("/pedidos/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(uri).body(pedido);
    }

    @GetMapping
    public ResponseEntity<Page<PedidoListagemDto>> listarTodos(@PageableDefault(size = 10, sort = {"dataEntrega"},
            direction = Sort.Direction.DESC) Pageable paginacao) {
        var page = service.listarTodos(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/filtro")
    public ResponseEntity<Page<PedidoListagemDto>> listarTodosFiltrado(
            @RequestParam(required = false) StatusPedido statusPedido,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) String nomeCliente,
            @RequestParam(required = false) LocalDate dataPedido,
            @RequestParam(required = false) LocalDateTime dataEntrega,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(required = false) Long usuarioResponsavelId,
            @RequestParam(required = false) String nomeUsuarioResponsavel,
            @PageableDefault(size = 10, sort = {"dataEntrega"},
                    direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = service.listarTodosFiltrado(statusPedido, clienteId, nomeCliente, dataPedido, dataEntrega,
                tipoEntrega, formaPagamento, usuarioResponsavelId, nomeUsuarioResponsavel, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoListagemDto> buscarPorId(@PathVariable Long id) throws BadRequestException {
        PedidoListagemDto pedido = service.buscarPorId(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CriacaoPedidoResponse> atualizar(@PathVariable Long id, @RequestBody @Valid CriacaoPedidoRequest request) throws BadRequestException {
        CriacaoPedidoResponse pedido = service.atualizar(id, request);
        return ResponseEntity.ok(pedido);
    }

    @PatchMapping("{id}/status")
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id, @RequestBody @Valid AlterarStatusPedidoDto status) throws BadRequestException {
        service.alterarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
