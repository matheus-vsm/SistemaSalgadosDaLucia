package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    private final Algorithm algorithm;
    private final String issuer;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer) {

        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
    }

    public String gerarToken(Usuario usuario) {
        try {
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getUsername())
                    .withExpiresAt(expiracao(60))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new BusinessException("Erro ao gerar token JWT!" + e.getMessage());
        }
    }

    public String gerarRefreshToken(Usuario usuario) {
        try {
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getId().toString())
                    .withExpiresAt(expiracao(120))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new BusinessException("Erro ao gerar token JWT!" + e.getMessage());
        }
    }

    public String verificaToken(String token) {
        DecodedJWT decodedJWT;
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build();
            decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            throw new BusinessException("Token JWT inválido ou expirado!" + e.getMessage());
        }
    }

    private Instant expiracao(Integer minutos) {
        return LocalDateTime.now().plusMinutes(minutos) // data/hora atual + minutos sem fuso
                .toInstant(ZoneOffset.of("-03:00")); // converte para UTC assumindo UTC-3 (Brasília)
    }

}
