package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.exception.BadRequestException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoRequest;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.security.SecurityConfig;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/salgados")
@RequiredArgsConstructor
@Validated
@Tag(name = "Salgados", description = "Endpoints relacionados aos salgados")
@SecurityRequirement(name = SecurityConfig.SECURITY)
public class SalgadoController {

    private final SalgadoService salgadoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastrar um novo salgado", description = "Permite cadastrar um novo salgado no sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Salgado cadastrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<SalgadoResponse> cadastrar(@Valid @RequestBody SalgadoRequest salgado, UriComponentsBuilder uriBuilder) {
        SalgadoResponse novoSalgado = salgadoService.cadastrar(salgado);
        var uri = uriBuilder.path("/salgados/{id}").buildAndExpand(novoSalgado.id()).toUri();

        return ResponseEntity.created(uri).body(novoSalgado);
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Listar salgados", description = "Retorna os salgados ativos de forma paginada.")
    @ApiResponse(responseCode = "200", description = "Salgados listados com sucesso.")
    public ResponseEntity<Page<SalgadoResponse>> listarSalgados(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao) {
        var page = salgadoService.listarSalgados(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Buscar salgado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salgado encontrado."),
            @ApiResponse(responseCode = "404", description = "Salgado não encontrado.")
    })
    public ResponseEntity<SalgadoResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        SalgadoResponse salgado = salgadoService.buscarPorId(id);
        return ResponseEntity.ok(salgado);
    }

    @GetMapping("/nome")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Buscar salgados por nome", description = "Retorna salgados cujo nome contenha o termo informado.")
    @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso.")
    public ResponseEntity<Page<SalgadoResponse>> buscarPorNome(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao, @RequestParam String nome) {
        var salgados = salgadoService.buscarPorNome(paginacao, nome);
        return ResponseEntity.ok(salgados);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar salgado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salgado atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos."),
            @ApiResponse(responseCode = "404", description = "Salgado não encontrado.")
    })
    public ResponseEntity<SalgadoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody SalgadoRequest salgado)
            throws NotFoundException {
        SalgadoResponse salgadoAtualizado = salgadoService.atualizar(id, salgado);
        return ResponseEntity.ok(salgadoAtualizado);
    }

    @PatchMapping(value = "/atualizar-status/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar status do salgado", description = "Ativa ou inativa um salgado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status atualizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Status ou dados inválidos."),
            @ApiResponse(responseCode = "404", description = "Salgado não encontrado.")
    })
    public ResponseEntity<Void> atualizarStatus(@PathVariable Long id, @Valid @RequestBody AlterarStatusDto status)
            throws NotFoundException, BadRequestException {
        salgadoService.atualizarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
