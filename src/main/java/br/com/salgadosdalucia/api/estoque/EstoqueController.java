package br.com.salgadosdalucia.api.estoque;

import br.com.salgadosdalucia.api.estoque.dto.AtualizarQuantidadeRequest;
import br.com.salgadosdalucia.api.estoque.dto.EstoqueListagemDto;
import br.com.salgadosdalucia.api.exception.NotFoundException;
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
public class EstoqueController {

    private final EstoqueService service;

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Void> atualizarQuantidade(@PathVariable Long id, @RequestBody @Valid AtualizarQuantidadeRequest request) throws NotFoundException {
        service.atualizarQuantidade(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Page<EstoqueListagemDto>> listar(@PageableDefault(size = 10) Pageable paginacao) {
        var page = service.listar(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{salgadoId}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<EstoqueListagemDto> buscarPorSalgadoId(@PathVariable Long salgadoId) throws NotFoundException {
        var estoque = service.buscarPorId(salgadoId);
        return ResponseEntity.ok(estoque);
    }

}
