package br.com.salgadosdalucia.api.handler;

import br.com.salgadosdalucia.api.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ProviderNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Tratar erros da API sem expor detalhes internos")
class GlobalHandlerExceptionTest {

    private final GlobalHandlerException handler = new GlobalHandlerException();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/usuarios");

    @Nested
    @DisplayName("Falhas inesperadas e de configuração")
    class FalhasInesperadas {
        @ParameterizedTest
        @ValueSource(strings = {"Falha SQL na tabela usuarios", "Perfil de usuário não configurado: ADMIN"})
        @DisplayName("Deve retornar 500 genérico e registrar os detalhes para diagnóstico no servidor")
        void deveOcultarDetalhes(String detalhe, CapturedOutput output) {
            var resposta = handler.handleException(new IllegalStateException(detalhe), request);

            assertThat(resposta.getStatusCode().value()).isEqualTo(500);
            var erro = resposta.getBody();
            assertThat(erro).isNotNull();
            assertThat(erro.message()).isEqualTo("Ocorreu um erro interno. Tente novamente mais tarde.");
            assertThat(erro.status()).isEqualTo(500);
            assertThat(erro.path()).isEqualTo("/usuarios");
            assertThat(erro.dateTime()).isNotNull();
            assertThat(output).contains(detalhe);
        }
    }

    @Nested
    @DisplayName("Distinguir indisponibilidade de credenciais inválidas")
    class Autenticacao {
        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        @DisplayName("Deve retornar 503 e registrar falhas técnicas de autenticação sem publicar detalhes")
        void deveRetornarIndisponibilidade(boolean interna, CapturedOutput output) {
            AuthenticationServiceException falha = interna
                    ? new InternalAuthenticationServiceException("Banco de autenticação indisponível")
                    : new AuthenticationServiceException("Banco de autenticação indisponível");
            var resposta = handler.handleAuthenticationService(falha, request);

            assertThat(resposta.getStatusCode().value()).isEqualTo(503);
            assertThat(resposta.getBody().message())
                    .isEqualTo("Não foi possível autenticar no momento. Tente novamente mais tarde.");
            assertThat(output).contains("Banco de autenticação indisponível");
        }

        @Test
        @DisplayName("Deve tratar ausência de provider como falha técnica, sem culpar as credenciais")
        void deveTratarProviderAusente() {
            var resposta = handler.handleAuthenticationService(new ProviderNotFoundException("Provider ausente"), request);
            assertThat(resposta.getStatusCode().value()).isEqualTo(503);
            assertThat(resposta.getBody().message())
                    .isEqualTo("Não foi possível autenticar no momento. Tente novamente mais tarde.");
        }

        @Test
        @DisplayName("Deve manter 401 para credenciais inválidas com mensagem pública segura")
        void deveManterCredenciaisInvalidas() {
            var resposta = handler.handleAuthentication(request);
            assertThat(resposta.getStatusCode().value()).isEqualTo(401);
            assertThat(resposta.getBody().message()).isEqualTo("Usuário ou senha inválidos.");
        }
    }

    @Nested
    @DisplayName("Preservar mensagens de negócio")
    class RegrasDeNegocio {
        @Test
        @DisplayName("Deve preservar a mensagem específica de negócio e o status 400")
        void devePreservarMensagemDeNegocio() {
            var resposta = handler.handleBusiness(new BusinessException("Senha atual incorreta!"), request);
            assertThat(resposta.getStatusCode().value()).isEqualTo(400);
            assertThat(resposta.getBody().message()).isEqualTo("Senha atual incorreta!");
        }
    }
}
