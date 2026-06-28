package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Salgados da Lucia Kojima")
                    .withSubject(usuario.getUsername())
                    .withExpiresAt(expiracao(60))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new BusinessException("Erro ao gerar token JWT!" + e.getMessage());
        }
    }

    private Instant expiracao(Integer minutos) {
        return LocalDateTime.now().plusMinutes(minutos) // data/hora atual + minutos sem fuso
                .toInstant(ZoneOffset.of("-03:00")); // converte para UTC assumindo UTC-3 (Brasília)
    }

}
