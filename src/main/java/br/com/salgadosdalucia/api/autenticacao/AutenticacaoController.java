package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.autenticacao.dto.DadosLoginDto;
import br.com.salgadosdalucia.api.autenticacao.dto.TokenResponse;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para autenticação e renovação de tokens")
public class AutenticacaoController {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    @Operation(summary = "Efetuar login", description = "Valida as credenciais e retorna tokens de acesso e renovação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso."),
            @ApiResponse(responseCode = "400", description = "Dados de login inválidos."),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas.")
    })
    public ResponseEntity<TokenResponse> efetuarLogin(@RequestBody @Valid DadosLoginDto dadosLoginDto) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dadosLoginDto.username(), dadosLoginDto.senha());
        var authenticaction = authenticationManager.authenticate(authenticationToken);

        String tokenAcesso = tokenService.gerarToken((Usuario) authenticaction.getPrincipal());
        String refreshToken = tokenService.gerarRefreshToken((Usuario) authenticaction.getPrincipal());

        return ResponseEntity.ok(new TokenResponse(tokenAcesso, refreshToken));
    }

    @PostMapping("/atualizar-token")
    @Operation(summary = "Atualizar tokens", description = "Gera um novo token de acesso e um novo refresh token a partir de um refresh token válido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens atualizados com sucesso."),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido ou expirado."),
            @ApiResponse(responseCode = "404", description = "Usuário associado ao token não encontrado.")
    })
    public ResponseEntity<TokenResponse> atualizarToken(@RequestBody @Valid DadosRefreshToken dados) throws NotFoundException {
        var refreshToken = dados.refreshToken();
        Long idUsuario = Long.valueOf(tokenService.verificaToken(refreshToken));
        var usuario = ValidacaoEntidadeHelper.buscarEntidadePorId(usuarioRepository, idUsuario, "Usuario");

        String tokenAcesso = tokenService.gerarToken(usuario);
        String refreshTokenAtualizado = tokenService.gerarRefreshToken(usuario);

        return ResponseEntity.ok(new TokenResponse(tokenAcesso, refreshTokenAtualizado));
    }

}
