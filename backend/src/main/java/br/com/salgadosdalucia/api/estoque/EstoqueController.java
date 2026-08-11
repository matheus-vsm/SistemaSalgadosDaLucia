package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.estoque.dto.AtualizarQuantidadeRequest;
import br.com.salgadosdalucia.api.estoque.dto.EstoqueListagemDto;
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

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Validated
@CrossOrigin
@Tag(name = "Estoque", description = "Endpoints para consulta e ajuste de estoque")
@SecurityRequirement(name = SecurityConfig.SECURITY)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Autenticação necessária ou token inválido."),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para esta operação.")
})
public class EstoqueController {

    private final EstoqueService service;

    @PatchMapping(value = "/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Ajustar quantidade em estoque", description = "Aplica uma variação positiva ou negativa sem permitir saldo negativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Quantidade atualizada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Ajuste inválido ou estoque insuficiente."),
            @ApiResponse(responseCode = "404", description = "Estoque não encontrado.")
    })
    public ResponseEntity<Void> atualizarQuantidade(@PathVariable Long id, @RequestBody @Valid AtualizarQuantidadeRequest request) throws NotFoundException {
        service.atualizarQuantidade(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Listar estoque")
    @ApiResponse(responseCode = "200", description = "Estoque listado com sucesso.")
    public ResponseEntity<Page<EstoqueListagemDto>> listar(@PageableDefault(size = 10) Pageable paginacao) {
        var page = service.listar(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping(value = "/{salgadoId}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Consultar estoque por salgado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque encontrado."),
            @ApiResponse(responseCode = "404", description = "Estoque ou salgado ativo não encontrado.")
    })
    public ResponseEntity<EstoqueListagemDto> buscarPorSalgadoId(@PathVariable Long salgadoId) throws NotFoundException {
        var estoque = service.buscarPorId(salgadoId);
        return ResponseEntity.ok(estoque);
    }

}
