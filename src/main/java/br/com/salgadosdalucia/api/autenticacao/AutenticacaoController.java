package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.autenticacao.dto.DadosLogin;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
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
public class AutenticacaoController {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> efetuarLogin(@RequestBody @Valid DadosLogin dadosLogin) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dadosLogin.username(), dadosLogin.senha());
        var authenticaction = authenticationManager.authenticate(authenticationToken);

        String tokenAcesso = tokenService.gerarToken((Usuario) authenticaction.getPrincipal());
        String refreshToken = tokenService.gerarRefreshToken((Usuario) authenticaction.getPrincipal());

        return ResponseEntity.ok(new TokenResponse(tokenAcesso, refreshToken));
    }

    @PostMapping("/atualizar-token")
    public ResponseEntity<TokenResponse> atualizarToken(@RequestBody @Valid DadosRefreshToken dados) {
        var refreshToken = dados.refreshToken();
        Long idUsuario = Long.valueOf(tokenService.verificaToken(refreshToken));
        var usuario = usuarioRepository.findById(idUsuario).orElseThrow();

        String tokenAcesso = tokenService.gerarToken(usuario);
        String refreshTokenAtualizado = tokenService.gerarRefreshToken(usuario);

        return ResponseEntity.ok(new TokenResponse(tokenAcesso, refreshTokenAtualizado));
    }

}
