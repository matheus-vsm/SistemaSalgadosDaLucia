package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraFiltroDto;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraResponse;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CriacaoCompraResponse> registrar(@RequestBody @Valid CriacaoCompraRequest request, UriComponentsBuilder uriBuilder) {
        CriacaoCompraResponse compra = service.registrarCompra(request);
        URI uri = uriBuilder.path("/compras/{id}").buildAndExpand(compra.id()).toUri();

        return ResponseEntity.created(uri).body(compra);
    }

    @GetMapping("/filtro")
    public ResponseEntity<Page<CriacaoCompraResponse>> listar(
            CompraFiltroDto filtro,
            @PageableDefault(size = 10, sort = "dataCompra") Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CriacaoCompraResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        CriacaoCompraResponse compra = service.buscarPorId(id);
        return ResponseEntity.ok(compra);
    }

}
