package br.com.salgadosdalucia.api.autenticacao;


import br.com.salgadosdalucia.api.autenticacao.dto.DadosLoginDto;
import br.com.salgadosdalucia.api.autenticacao.dto.TokenResponse;
import br.com.salgadosdalucia.api.exception.InvalidCredentialsException;
import br.com.salgadosdalucia.api.shared.helper.ValidacaoEntidadeHelper;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final UsuarioRepository usuarioRepository;

    public TokenResponse autenticar(DadosLoginDto dadosLoginDto) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(dadosLoginDto.username(), dadosLoginDto.senha());

        try {
            var authentication = authenticationManager.authenticate(authenticationToken);
            var usuario = (Usuario) authentication.getPrincipal();

            return new TokenResponse(tokenService.gerarToken(usuario), tokenService.gerarRefreshToken(usuario));
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Usuário ou senha inválidos.");
        }
    }

    public TokenResponse atualizarToken(DadosRefreshToken dados) {
        var refreshToken = dados.refreshToken();
        Long idUsuario = Long.valueOf(tokenService.verificaToken(refreshToken));
        var usuario = ValidacaoEntidadeHelper.buscarEntidadePorId(usuarioRepository, idUsuario, "Usuario");

        String tokenAcesso = tokenService.gerarToken(usuario);
        String refreshTokenAtualizado = tokenService.gerarRefreshToken(usuario);

        return new TokenResponse(tokenAcesso, refreshTokenAtualizado);
    }

}
