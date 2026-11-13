package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TokenServiceTest {
    @Test
    void validadeEAssinaturaIndependemDoFuso() {
        String secret = UUID.randomUUID().toString();
        TokenService service = new TokenService(secret, "teste");
        Usuario usuario = Usuario.builder().id(1L).username("admin-teste").build();
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "America/Sao_Paulo"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                Instant before = Instant.now();
                String access = service.gerarToken(usuario);
                String refresh = service.gerarRefreshToken(usuario);
                assertThat(JWT.decode(access).getExpiresAtAsInstant()).isBetween(before.plusSeconds(3598), before.plusSeconds(3602));
                assertThat(JWT.decode(refresh).getExpiresAtAsInstant()).isBetween(before.plusSeconds(7198), before.plusSeconds(7202));
                assertThat(service.verificaToken(access)).isEqualTo("admin-teste");
                JWT.require(Algorithm.HMAC256(secret)).withIssuer("teste").build().verify(access);
                assertThatThrownBy(() -> new TokenService(UUID.randomUUID().toString(), "teste").verificaToken(access))
                        .isInstanceOf(RuntimeException.class);
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
