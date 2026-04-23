package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.pedido.dto.*;
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
    public ResponseEntity<CriacaoPedidoResponse> registrar(@RequestBody @Valid CriacaoPedidoRequest request,
                                                                 UriComponentsBuilder uriBuilder) throws NotFoundException {
        CriacaoPedidoResponse pedido = service.registrar(request);
        URI uri = uriBuilder.path("/pedidos/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(uri).body(pedido);
    }

    @GetMapping
    public ResponseEntity<Page<PedidoListagemDto>> listar(
            PedidoFiltroDto filtro,
            @PageableDefault(size = 10, sort = {"dataEntrega"},
                    direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoListagemDto> buscarPorId(@PathVariable Long id) throws NotFoundException {
        PedidoListagemDto pedido = service.buscarPorId(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CriacaoPedidoResponse> atualizar(@PathVariable Long id, @RequestBody @Valid CriacaoPedidoRequest request) throws NotFoundException {
        CriacaoPedidoResponse pedido = service.atualizar(id, request);
        return ResponseEntity.ok(pedido);
    }

    @PatchMapping("{id}/status")
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id, @RequestBody @Valid AlterarStatusPedidoDto status) throws NotFoundException {
        service.alterarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
