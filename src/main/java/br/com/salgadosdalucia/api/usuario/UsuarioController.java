package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

}
