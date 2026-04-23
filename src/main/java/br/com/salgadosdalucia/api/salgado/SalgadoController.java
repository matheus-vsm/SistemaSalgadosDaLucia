package br.com.salgadosdalucia.api.salgado;

import br.com.salgadosdalucia.api.salgado.dto.SalgadoDto;
import br.com.salgadosdalucia.api.salgado.dto.SalgadoResponse;
import br.com.salgadosdalucia.api.shared.AlterarStatusDto;
import br.com.salgadosdalucia.api.exception.BadRequestException;
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

import java.util.List;

@RestController
@RequestMapping("/salgados")
@RequiredArgsConstructor
@Validated
public class SalgadoController {

    private final SalgadoService salgadoService;

    @PostMapping
    public ResponseEntity<SalgadoResponse> cadastrar(@RequestBody @Valid SalgadoDto salgado, UriComponentsBuilder uriBuilder) {
        SalgadoResponse novoSalgado = salgadoService.cadastrar(salgado);
        var uri = uriBuilder.path("/salgados/{id}").buildAndExpand(novoSalgado.id()).toUri();

        return ResponseEntity.created(uri).body(novoSalgado);
    }

    @GetMapping
    public ResponseEntity<Page<Salgado>> listarSalgados(@PageableDefault(size = 10, sort = {"nome"}) Pageable paginacao) {
        var page = salgadoService.listarSalgados(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salgado> buscarPorId(@PathVariable Long id) throws NotFoundException {
        Salgado salgado = salgadoService.buscarPorId(id);
        return ResponseEntity.ok(salgado);
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<List<Salgado>> buscarPorNome(@PathVariable String nome) throws NotFoundException {
        List<Salgado> salgados = salgadoService.buscarPorNome(nome);
        return ResponseEntity.ok(salgados);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Salgado> atualizar(@Valid @PathVariable Long id, @RequestBody SalgadoDto salgado)
            throws NotFoundException {
        Salgado salgadoAtualizado = salgadoService.atualizar(id, salgado);
        return ResponseEntity.ok(salgadoAtualizado);
    }

    @PatchMapping("/atualizar-status/{id}")
    public ResponseEntity<Void> atualizarStatus(@Valid @PathVariable Long id, @RequestBody AlterarStatusDto status)
            throws NotFoundException, BadRequestException {
        salgadoService.atualizarStatus(id, status);
        return ResponseEntity.noContent().build();
    }

}
