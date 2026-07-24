package br.com.salgadosdalucia.api.usuario;

import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.security.SecurityConfig;
import br.com.salgadosdalucia.api.usuario.dto.AlterarSenhaUsuarioDto;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioRequest;
import br.com.salgadosdalucia.api.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Validated
@Tag(name = "Usuários", description = "Endpoints para administração de usuários e credenciais")
@SecurityRequirement(name = SecurityConfig.SECURITY)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastrar usuário", description = "Cria um usuário com senha criptografada e perfil de acesso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.")
    })
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid UsuarioRequest dados,
                                                     UriComponentsBuilder uriBuilder) {
        Usuario usuario = usuarioService.cadastrar(dados);
        URI uri = uriBuilder.path("/{nomeUsuario}").buildAndExpand(usuario.getUsername()).toUri();
        return ResponseEntity.created(uri).body(UsuarioMapper.mapToUsuarioResponse(usuario));
    }

    @GetMapping
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Listar usuários", description = "Retorna os usuários ativos de forma paginada.")
    @ApiResponse(responseCode = "200", description = "Usuários listados com sucesso.")
    public ResponseEntity<Page<UsuarioResponse>> listar(@PageableDefault(sort = {"nome"},
            direction = Sort.Direction.ASC) Pageable paginacao) {
        var page = usuarioService.listar(paginacao);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FUNCIONARIO')")
    @Operation(summary = "Buscar usuário por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) throws NotFoundException {
        var usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @PatchMapping("/alterar-senha")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Alterar senha do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou confirmação divergente.")
    })
    public ResponseEntity<Void> alterarSenha(@RequestBody @Valid AlterarSenhaUsuarioDto dados,
                                             @AuthenticationPrincipal Usuario usuarioLogado) {
        usuarioService.alterarSenha(dados, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/desativar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar usuário", description = "Desativa o usuário sem removê-lo do histórico do sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso."),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.")
    })
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

}
