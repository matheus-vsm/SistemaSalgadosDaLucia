package br.com.salgadosdalucia.api.compra;

import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraRequest;
import br.com.salgadosdalucia.api.compra.dto.CriacaoCompraResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
