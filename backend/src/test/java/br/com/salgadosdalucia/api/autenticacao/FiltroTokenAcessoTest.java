package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.InvalidCredentialsException;
import br.com.salgadosdalucia.api.security.FiltroTokenAcesso;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("FiltroTokenAcesso")
class FiltroTokenAcessoTest {

    private final TokenService tokens = mock(TokenService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final FiltroTokenAcesso filtro = new FiltroTokenAcesso(tokens, usuarios, resolver);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/protegida");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Preencher contexto de autenticação")
    class Autenticacao {
        @DisplayName("Deve usar o usuário atual do repositório como principal sem armazenar credenciais")
        @Test
        void principalDoContextoEUsuarioAtualDoRepositorioSemArmazenarCredenciais() throws Exception {
            Usuario usuario = Usuario.builder().id(7L).username("alice").ativo(true).build();
            request.addHeader("Authorization", "Bearer acesso");

            when(tokens.verificaToken("acesso")).thenReturn("alice");
            when(usuarios.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(usuario));
            doAnswer(invocation -> {
                var autenticacao = SecurityContextHolder.getContext().getAuthentication();

                assertThat(autenticacao.getPrincipal()).isSameAs(usuario);
                assertThat(autenticacao.getCredentials()).isNull();
                assertThat(autenticacao.isAuthenticated()).isTrue();
                return null;
            }).when(chain).doFilter(request, response);

            filtro.doFilter(request, response, chain);

            verify(chain).doFilter(request, response);
            verifyNoInteractions(resolver);
        }
    }

    @Nested
    @DisplayName("Tratar falhas e controlar a continuidade da requisição")
    class Falhas {
        @DisplayName("Deve limpar o contexto e responder sem continuar a cadeia quando a credencial falhar")
        @Test
        void falhaNaCredencialLimpaContextoPreexistenteERespondeSemContinuarARequisicao() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("anterior", null, List.of()));
            request.addHeader("Authorization", "Bearer invalido");

            when(tokens.verificaToken("invalido")).thenThrow(new BusinessException("inválido"));

            filtro.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(resolver).resolveException(eq(request), eq(response), isNull(),
                    argThat(ex -> ex instanceof InvalidCredentialsException
                            && ex.getMessage().equals("Token de acesso inválido ou expirado.")));
            verifyNoInteractions(chain, usuarios);
        }

        @Test
        @DisplayName("Deve limpar o contexto e encaminhar a falha técnica original sem continuar a requisição")
        void deveEncaminharFalhaTecnica() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("anterior", null, List.of()));
            request.addHeader("Authorization", "Bearer acesso");
            when(tokens.verificaToken("acesso")).thenReturn("alice");
            var falha = new org.springframework.dao.DataAccessResourceFailureException("Banco indisponível");
            when(usuarios.findByUsernameIgnoreCase("alice")).thenThrow(falha);
            filtro.doFilter(request, response, chain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(resolver).resolveException(request, response, null, falha);
            verifyNoInteractions(chain);
        }

        @DisplayName("Não deve converter erro de negócio posterior ao filtro em falha de autenticação")
        @Test
        void erroDeNegocioPosteriorAoFiltroNaoEViradoErroDeAutenticacao() throws Exception {
            var falha = new BusinessException("Falha do domínio atendido");

            doThrow(falha).when(chain).doFilter(request, response);

            assertThatThrownBy(() -> filtro.doFilter(request, response, chain)).isSameAs(falha);
            verifyNoInteractions(tokens, usuarios, resolver);
        }
    }

}
