package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.usuario.dto.AlterarSenhaUsuarioDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid UsuarioRequest dados,
                                                     UriComponentsBuilder uriBuilder) {
        Usuario usuario = usuarioService.cadastrar(dados);
        URI uri = uriBuilder.path("/{nomeUsuario}").buildAndExpand(usuario.getUsername()).toUri();
        return ResponseEntity.created(uri).body(UsuarioMapper.mapToUsuarioResponse(usuario));
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(@PageableDefault(sort = {"nome"},
            direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = usuarioService.listar(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        var usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @PatchMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody @Valid AlterarSenhaUsuarioDto dados,
                                             @AuthenticationPrincipal Usuario usuarioLogado) {
        usuarioService.alterarSenha(dados, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/desativar/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

}
