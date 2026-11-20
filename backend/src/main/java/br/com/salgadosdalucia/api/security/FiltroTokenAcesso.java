package br.com.salgadosdalucia.api.security;

import br.com.salgadosdalucia.api.autenticacao.TokenService;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.InvalidCredentialsException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FiltroTokenAcesso extends OncePerRequestFilter {
    private final TokenService tokenService;

    private final UsuarioRepository usuarioRepository;
    private final HandlerExceptionResolver exceptionResolver;

    public FiltroTokenAcesso(TokenService tokenService, UsuarioRepository usuarioRepository,
                             @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = recuperarTokenRequisicao(request);
            if (token != null) {
                String username = tokenService.verificaToken(token);
                Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username).orElseThrow(() ->
                        new NotFoundException("Usuário não encontrado com username: " + username));
                if (!usuario.isAtivo()) {
                    throw new BusinessException("Usuário inativo.");
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (BusinessException | NotFoundException ex) {
            SecurityContextHolder.clearContext();
            exceptionResolver.resolveException(request, response, null,
                    new InvalidCredentialsException("Token de acesso inválido ou expirado."));
            return;
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            exceptionResolver.resolveException(request, response, null, ex);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String recuperarTokenRequisicao(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null) {
            return null;
        }
        if (!authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new BusinessException("Cabeçalho Authorization inválido.");
        }
        String token = authorizationHeader.substring(7);
        if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
            throw new BusinessException("Cabeçalho Authorization inválido.");
        }
        return token;
    }
}
