package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.exception.BadRequestException;
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
    public ResponseEntity<Page<PedidoListagemDto>> listarTodosFiltrado(@RequestParam(required = false) StatusPedido statusPedido,
                                                                       @PageableDefault(size = 10, sort = {"dataEntrega"},
                                                                               direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = service.listarTodosFiltrado(statusPedido, paginacao);
        return ResponseEntity.ok(page);
    }



}
