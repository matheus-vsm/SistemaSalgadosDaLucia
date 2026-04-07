package br.com.salgadosdalucia.api.controller;

import br.com.salgadosdalucia.api.dto.SalgadoDto;
import br.com.salgadosdalucia.api.model.Salgado;
import br.com.salgadosdalucia.api.service.SalgadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/salgados")
@RequiredArgsConstructor
@Validated
public class SalgadoController {

    private final SalgadoService salgadoService;

    @PostMapping
    public ResponseEntity<Salgado> cadastrar(@RequestBody @Valid SalgadoDto salgado, UriComponentsBuilder uriBuilder) {
        var novoSalgado = salgadoService.cadastrar(salgado);
        var uri = uriBuilder.path("/salgados/{id}").buildAndExpand(novoSalgado.getId()).toUri();
        return ResponseEntity.created(uri).body(novoSalgado);
    }

}
