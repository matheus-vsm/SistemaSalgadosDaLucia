package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.autenticacao.dto.DadosLoginDto;
import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.exception.InvalidCredentialsException;
import br.com.salgadosdalucia.api.exception.NotFoundException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import br.com.salgadosdalucia.api.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacaoService")
class AutenticacaoServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService tokenService;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks AutenticacaoService service;

    private final Usuario usuario = Usuario.builder().id(7L).username("alice").ativo(true).build();

    @Nested
    @DisplayName("Autenticar usuário")
    class Login {
        @DisplayName("Deve autenticar com as credenciais recebidas e emitir os dois tokens para o usuário autenticado")
        @Test
        void autenticaComAsCredenciaisRecebidasEEmiteOsDoisTokensParaOPrincipalAutenticado() {
            when(authenticationManager.authenticate(any())).thenReturn(
                    new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
            when(tokenService.gerarToken(usuario)).thenReturn("acesso");
            when(tokenService.gerarRefreshToken(usuario)).thenReturn("renovacao");

            var resposta = service.autenticar(new DadosLoginDto("ALICE", "senha recebida"));

            var credenciais = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(credenciais.capture());
            assertThat(credenciais.getValue().getPrincipal()).isEqualTo("ALICE");
            assertThat(credenciais.getValue().getCredentials()).isEqualTo("senha recebida");
            assertThat(credenciais.getValue().isAuthenticated()).isFalse();
            assertThat(resposta.tokenAcesso()).isEqualTo("acesso");
            assertThat(resposta.refreshToken()).isEqualTo("renovacao");
            verifyNoInteractions(usuarioRepository);
        }

        @DisplayName("Não deve emitir tokens nem consultar o repositório quando as credenciais forem inválidas")
        @Test
        void credenciaisInvalidasNaoEmitemTokensNemConsultamORepositorio() {
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("detalhe interno"));

            assertThatThrownBy(() -> service.autenticar(new DadosLoginDto("alice", "errada")))
                    .isInstanceOf(InvalidCredentialsException.class).hasMessage("Usuário ou senha inválidos.");
            verifyNoInteractions(tokenService, usuarioRepository);
        }

        @DisplayName("Não deve emitir tokens quando o provider recusar um usuário desabilitado")
        @Test
        void usuarioDesabilitadoPeloProviderNaoRecebeTokens() {
            when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

            assertThatThrownBy(() -> service.autenticar(new DadosLoginDto("alice", "senha")))
                    .isInstanceOf(InvalidCredentialsException.class).hasMessage("Usuário ou senha inválidos.");
            verifyNoInteractions(tokenService, usuarioRepository);
        }

        @DisplayName("Deve preservar falhas internas do provider sem tratá-las como credenciais inválidas")
        @Test
        void indisponibilidadeNaAutenticacaoNaoEConvertidaEmCredenciaisInvalidas() {
            var falha = new InternalAuthenticationServiceException("Serviço indisponível");
            when(authenticationManager.authenticate(any())).thenThrow(falha);

            assertThatThrownBy(() -> service.autenticar(new DadosLoginDto("alice", "senha"))).isSameAs(falha);
            verifyNoInteractions(tokenService, usuarioRepository);
        }

        @DisplayName("Não deve emitir refresh quando a geração do acesso falhar")
        @Test
        void falhaNaEmissaoDoAcessoNaoTentaEmitirRefresh() {
            when(authenticationManager.authenticate(any())).thenReturn(
                    new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
            when(tokenService.gerarToken(usuario)).thenThrow(new BusinessException("Falha na emissão"));

            assertThatThrownBy(() -> service.autenticar(new DadosLoginDto("alice", "senha")))
                    .isInstanceOf(BusinessException.class).hasMessage("Falha na emissão");
            verify(tokenService, never()).gerarRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("Renovar tokens")
    class Renovacao {
        @DisplayName("Deve emitir os dois tokens para o usuário encontrado pelo ID do refresh")
        @Test
        void renovaUsandoOUsuarioEncontradoPeloIdDoRefresh() {
            when(tokenService.verificaRefreshToken("refresh recebido")).thenReturn("7");
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
            when(tokenService.gerarToken(usuario)).thenReturn("novo acesso");
            when(tokenService.gerarRefreshToken(usuario)).thenReturn("novo refresh");

            var resposta = service.atualizarToken(new DadosRefreshToken("refresh recebido"));

            assertThat(resposta.tokenAcesso()).isEqualTo("novo acesso");
            assertThat(resposta.refreshToken()).isEqualTo("novo refresh");
            verifyNoInteractions(authenticationManager);
        }

        @DisplayName("Deve interromper a renovação antes da busca e da emissão quando o refresh for inválido")
        @Test
        void refreshInvalidoInterrompeAntesDaBuscaEEmissao() {
            when(tokenService.verificaRefreshToken("invalido")).thenThrow(new BusinessException("Token inválido"));

            assertThatThrownBy(() -> service.atualizarToken(new DadosRefreshToken("invalido")))
                    .isInstanceOf(BusinessException.class).hasMessage("Token inválido");
            verify(tokenService).verificaRefreshToken("invalido");
            verifyNoMoreInteractions(tokenService);
            verifyNoInteractions(usuarioRepository, authenticationManager);
        }

        @DisplayName("Não deve emitir novos tokens para usuário removido")
        @Test
        void usuarioRemovidoNaoRecebeNovosTokens() {
            when(tokenService.verificaRefreshToken("refresh")).thenReturn("7");
            when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizarToken(new DadosRefreshToken("refresh")))
                    .isInstanceOf(NotFoundException.class).hasMessage("Usuario não encontrado com o ID: 7");
            verify(tokenService).verificaRefreshToken("refresh");
            verifyNoMoreInteractions(tokenService);
        }

        @DisplayName("Deve rejeitar identificação inválida no refresh antes de consultar o usuário")
        @ParameterizedTest // executa o teste varias vezes com os valores fornecidos
        @NullSource
        @ValueSource(strings = {"", " ", "alice", "1.5", "0", "-7", "9223372036854775808"}) // fornece os valores
        void identificacaoInvalidaNoRefreshRetornaErroDeNegocioAntesDaBusca(String subject) {
            when(tokenService.verificaRefreshToken("refresh")).thenReturn(subject);

            assertThatThrownBy(() -> service.atualizarToken(new DadosRefreshToken("refresh")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Refresh token com identificação de usuário inválida.");
            verifyNoInteractions(usuarioRepository);
            verify(tokenService).verificaRefreshToken("refresh");
            verifyNoMoreInteractions(tokenService);
        }

        @DisplayName("Não deve renovar tokens de usuário inativo")
        @Test
        void usuarioDesativadoNaoRenovaTokens() {
            usuario.setAtivo(false);
            when(tokenService.verificaRefreshToken("refresh")).thenReturn("7");
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

            assertThatThrownBy(() -> service.atualizarToken(new DadosRefreshToken("refresh")))
                    .isInstanceOf(InvalidCredentialsException.class).hasMessage("Usuário inativo.");
            verify(tokenService).verificaRefreshToken("refresh");
            verifyNoMoreInteractions(tokenService);
        }

        @DisplayName("Não deve retornar resposta parcial quando a geração do refresh falhar")
        @Test
        void falhaNaEmissaoDoRefreshNaoRetornaRespostaParcial() {
            when(tokenService.verificaRefreshToken("refresh")).thenReturn("7");
            when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
            when(tokenService.gerarToken(usuario)).thenReturn("acesso");
            when(tokenService.gerarRefreshToken(usuario)).thenThrow(new BusinessException("Falha na emissão"));

            assertThatThrownBy(() -> service.atualizarToken(new DadosRefreshToken("refresh")))
                    .isInstanceOf(BusinessException.class).hasMessage("Falha na emissão");
        }
    }

}
