package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraFiltroDto;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CompraResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
@Validated
public class CompraController {

    private final CompraService service;

    @PostMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<CompraResponse> registrar(@RequestBody @Valid CriacaoCompraRequest request, UriComponentsBuilder uriBuilder) {
        CompraResponse compra = service.registrarCompra(request);
        URI uri = uriBuilder.path("/compras/{id}").buildAndExpand(compra.id()).toUri();

        return ResponseEntity.created(uri).body(compra);
    }

    @GetMapping("/filtro")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<Page<CompraResponse>> listar(
            CompraFiltroDto filtro,
            @PageableDefault(sort = "dataCompra") Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    public ResponseEntity<CompraResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        CompraResponse compra = service.buscarPorId(id);
        return ResponseEntity.ok(compra);
    }

}
