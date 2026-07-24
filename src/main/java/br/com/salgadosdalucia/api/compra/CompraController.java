package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraFiltroDto;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CompraResponse;
import br.com.salgadosdalucia.api.exception.NotFoundException;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
@Validated
@Tag(name = "Compras", description = "Endpoints para registro e consulta de compras de insumos")
@SecurityRequirement(name = SecurityConfig.SECURITY)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido."),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para esta operação.")
})
public class CompraController {

    private final CompraService service;

    @PostMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Registrar compra", description = "Registra uma compra e calcula seu valor total a partir dos itens.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra registrada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<CompraResponse> registrar(@RequestBody @Valid CriacaoCompraRequest request, UriComponentsBuilder uriBuilder) {
        CompraResponse compra = service.registrarCompra(request);
        URI uri = uriBuilder.path("/compras/{id}").buildAndExpand(compra.id()).toUri();

        return ResponseEntity.created(uri).body(compra);
    }

    @GetMapping("/filtro")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Listar compras com filtros", description = "Permite filtrar por data, período, nome do item ou observação.")
    @ApiResponse(responseCode = "200", description = "Compras listadas com sucesso.")
    public ResponseEntity<Page<CompraResponse>> listar(
            CompraFiltroDto filtro,
            @PageableDefault(sort = "dataCompra") Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Buscar compra por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compra encontrada."),
            @ApiResponse(responseCode = "404", description = "Compra não encontrada.")
    })
    public ResponseEntity<CompraResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        CompraResponse compra = service.buscarPorId(id);
        return ResponseEntity.ok(compra);
    }

}
