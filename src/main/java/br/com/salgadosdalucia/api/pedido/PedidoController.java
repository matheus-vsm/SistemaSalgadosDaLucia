package br.com.salgadosdalucia.api.pedido;

import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.pedido.dto.*;
import br.com.salgadosdalucia.api.security.SecurityConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pedidos", description = "Endpoints para o ciclo de vida dos pedidos")
@SecurityRequirement(name = SecurityConfig.SECURITY)
public class PedidoController {

    private final PedidoService service;

    @PostMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Registrar pedido", description = "Registra um pedido, calcula os valores dos itens e define o status inicial como EM_ANDAMENTO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido registrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio não atendida."),
            @ApiResponse(responseCode = "404", description = "Cliente, usuário responsável ou salgado não encontrado.")
    })
    public ResponseEntity<CriacaoPedidoResponse> registrar(@RequestBody @Valid CriacaoPedidoRequest request,
                                                           UriComponentsBuilder uriBuilder) throws NotFoundException {
        CriacaoPedidoResponse pedido = service.registrar(request);
        URI uri = uriBuilder.path("/pedidos/{id}").buildAndExpand(pedido.id()).toUri();
        return ResponseEntity.created(uri).body(pedido);
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Listar pedidos com filtros", description = "Permite filtrar por status, cliente, datas, entrega, pagamento e responsável.")
    @ApiResponse(responseCode = "200", description = "Pedidos listados com sucesso.")
    public ResponseEntity<Page<PedidoListagemDto>> listar(
            PedidoFiltroDto filtro,
            @PageableDefault(size = 10, sort = {"dataEntrega"},
                    direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Buscar pedido por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado."),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.")
    })
    public ResponseEntity<PedidoListagemDto> buscarPorId(@PathVariable Long id) throws NotFoundException {
        PedidoListagemDto pedido = service.buscarPorId(id);
        return ResponseEntity.ok(pedido);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Atualizar pedido", description = "Atualiza dados e itens de um pedido que esteja em andamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou pedido não pode ser atualizado."),
            @ApiResponse(responseCode = "404", description = "Pedido ou entidade relacionada não encontrada.")
    })
    public ResponseEntity<CriacaoPedidoResponse> atualizar(@PathVariable Long id,
                                                           @RequestBody @Valid CriacaoPedidoRequest request) throws NotFoundException {
        CriacaoPedidoResponse pedido = service.atualizar(id, request);
        return ResponseEntity.ok(pedido);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Alterar status do pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Status inválido ou já aplicado."),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.")
    })
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id,
                                              @RequestBody @Valid AlterarStatusPedidoDto status) throws NotFoundException {
        service.alterarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
