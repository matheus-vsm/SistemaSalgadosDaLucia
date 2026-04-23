package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CompraFiltroDto;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
@Validated
public class CompraController {

    private final CompraService service;

    @PostMapping
    public ResponseEntity<CriacaoCompraResponse> registrarCompra(@RequestBody @Valid CriacaoCompraRequest request) {
        CriacaoCompraResponse compra = service.registrarCompra(request);
        return ResponseEntity.ok(compra);
    }

    @GetMapping("/filtro")
    public ResponseEntity<Page<CriacaoCompraResponse>> listar(
            CompraFiltroDto filtro,
            @PageableDefault(size = 10, sort = "dataCompra") Pageable paginacao) {
        var page = service.listarComFiltro(filtro, paginacao);
        return ResponseEntity.ok(page);
    }

}
