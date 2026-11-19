package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenService")
class TokenServiceTest {

    static final String SECRET = "segredo-exclusivo-dos-testes-de-autenticacao";
    static final String ISSUER = "autenticacao-teste";
    private final TokenService service = new TokenService(SECRET, ISSUER);
    private final Usuario usuario = Usuario.builder().id(7L).username("alice").ativo(true).build();

    @Nested
    @DisplayName("Gerar tokens e definir validade")
    class Geracao {
        @DisplayName("Deve gerar acesso assinado com username, emissor e validade de uma hora")
        @Test
        void acessoAssinadoContemUsernameEmissorEValidadeDeUmaHora() {
            Instant antes = Instant.now();
            String token = service.gerarToken(usuario);
            Instant depois = Instant.now();

            var jwt = JWT.require(Algorithm.HMAC256(SECRET)).withIssuer(ISSUER).build().verify(token);
            assertThat(jwt.getSubject()).isEqualTo("alice");
            assertThat(jwt.getExpiresAtAsInstant()).isBetween(antes.plusSeconds(3599), depois.plusSeconds(3600));
            assertThat(service.verificaToken(token)).isEqualTo("alice");
        }

        @DisplayName("Deve gerar refresh assinado com ID, emissor e validade de duas horas")
        @Test
        void refreshAssinadoContemIdEmissorEValidadeDeDuasHoras() {
            Instant antes = Instant.now();
            String token = service.gerarRefreshToken(usuario);
            Instant depois = Instant.now();

            var jwt = JWT.require(Algorithm.HMAC256(SECRET)).withIssuer(ISSUER).build().verify(token);
            assertThat(jwt.getSubject()).isEqualTo("7");
            assertThat(jwt.getExpiresAtAsInstant()).isBetween(antes.plusSeconds(7199), depois.plusSeconds(7200));
            assertThat(service.verificaRefreshToken(token)).isEqualTo("7");
        }

        @DisplayName("Deve preservar a validade e a verificação dos dois tokens em diferentes fusos")
        @Test
        @ResourceLock("java.util.TimeZone.default")
        void validadeDosDoisTokensIndependeDoFuso() {
            TimeZone original = TimeZone.getDefault();
            try {
                for (String zone : new String[]{"UTC", "America/Sao_Paulo"}) {
                    TimeZone.setDefault(TimeZone.getTimeZone(zone));
                    acessoAssinadoContemUsernameEmissorEValidadeDeUmaHora();
                    refreshAssinadoContemIdEmissorEValidadeDeDuasHoras();
                }
            } finally {
                TimeZone.setDefault(original);
            }
        }
    }

    @Nested
    @DisplayName("Verificar integridade e validade dos tokens")
    class Verificacao {
        @DisplayName("Deve rejeitar assinatura produzida com outro segredo")
        @Test
        void rejeitaAssinaturaDeOutroSegredo() {
            assertTokenInvalido(JWT.create().withClaim("tipo", "acesso").withIssuer(ISSUER).withSubject("alice")
                    .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256("outro segredo")));
        }

        @DisplayName("Deve rejeitar emissor diferente do configurado")
        @Test
        void rejeitaEmissorDiferente() {
            assertTokenInvalido(JWT.create().withClaim("tipo", "acesso").withIssuer("outro emissor").withSubject("alice")
                    .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(SECRET)));
        }

        @DisplayName("Deve rejeitar token sem emissor")
        @Test
        void rejeitaTokenSemEmissor() {
            assertTokenInvalido(JWT.create().withClaim("tipo", "acesso").withSubject("alice")
                    .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(SECRET)));
        }

        @DisplayName("Deve rejeitar token expirado")
        @Test
        void rejeitaTokenExpirado() {
            assertTokenInvalido(JWT.create().withClaim("tipo", "acesso").withIssuer(ISSUER).withSubject("alice")
                    .withExpiresAt(Instant.now().minusSeconds(1)).sign(Algorithm.HMAC256(SECRET)));
        }

        @DisplayName("Deve rejeitar payload adulterado mesmo com JWT bem formado")
        @Test
        void rejeitaPayloadAdulteradoMesmoQuandoOJwtContinuaBemFormado() {
            String original = service.gerarToken(usuario);
            String outroPayload = JWT.create().withClaim("tipo", "acesso").withIssuer(ISSUER).withSubject("administrador")
                    .withExpiresAt(Instant.now().plusSeconds(60)).sign(Algorithm.HMAC256(SECRET)).split("\\.")[1];
            String[] partes = original.split("\\.");
            assertTokenInvalido(partes[0] + "." + outroPayload + "." + partes[2]);
        }

        @DisplayName("Deve rejeitar tokens malformados")
        @ParameterizedTest
        @ValueSource(strings = {"", " ", "nao-e-jwt", "a.b.c"})
        void rejeitaTokensMalformados(String token) {
            assertTokenInvalido(token);
        }

        @DisplayName("Deve rejeitar identificação ausente ou em branco")
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void rejeitaSubjectAusenteOuEmBranco(String subject) {
            assertTokenInvalido(JWT.create().withIssuer(ISSUER).withSubject(subject)
                    .withClaim("tipo", "acesso").withExpiresAt(Instant.now().plusSeconds(60))
                    .sign(Algorithm.HMAC256(SECRET)));
        }

        @DisplayName("Deve rejeitar token sem expiração")
        @Test
        void rejeitaTokenSemExpiracao() {
            assertTokenInvalido(JWT.create().withIssuer(ISSUER).withSubject("alice")
                    .withClaim("tipo", "acesso").sign(Algorithm.HMAC256(SECRET)));
        }

        @DisplayName("Deve rejeitar token nulo")
        @Test
        void rejeitaTokenNulo() {
            assertTokenInvalido(null);
        }
    }

    @Nested
    @DisplayName("Distinguir acesso e renovação")
    class Finalidade {
        @DisplayName("Não deve permitir usar acesso como refresh nem refresh como acesso")
        @Test
        void naoPermiteTrocarAFinalidadeDosTokens() {
            assertTokenInvalido(service.gerarRefreshToken(usuario));
            assertThatThrownBy(() -> service.verificaRefreshToken(service.gerarToken(usuario)))
                    .isInstanceOf(BusinessException.class).hasMessageStartingWith("Token JWT inválido ou expirado!");
        }

        @DisplayName("Deve rejeitar finalidade ausente, em branco ou desconhecida")
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", " ", "outro"})
        void rejeitaFinalidadeAusenteOuDesconhecida(String tipo) {
            var builder = JWT.create().withIssuer(ISSUER).withSubject("alice")
                    .withExpiresAt(Instant.now().plusSeconds(60));
            if (tipo != null) builder.withClaim("tipo", tipo);
            assertTokenInvalido(builder.sign(Algorithm.HMAC256(SECRET)));
        }
    }

    private void assertTokenInvalido(String token) {
        assertThatThrownBy(() -> service.verificaToken(token))
                .isInstanceOf(BusinessException.class).hasMessageStartingWith("Token JWT inválido ou expirado!");
    }
}
