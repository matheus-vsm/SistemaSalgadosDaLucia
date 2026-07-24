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
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    public String gerarToken(Usuario usuario) {
        try {
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getUsername())
                    .withExpiresAt(expiracao(60))
                    .sign(algoritmo());
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
                    .sign(algoritmo());
        } catch (JWTCreationException e) {
            throw new BusinessException("Erro ao gerar token JWT!" + e.getMessage());
        }
    }

    public String verificaToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algoritmo())
                    .withIssuer(issuer)
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            throw new BusinessException("Token JWT invalido ou expirado!" + e.getMessage());
        }
    }

    private Instant expiracao(Integer minutos) {
        return LocalDateTime.now().plusMinutes(minutos)
                .toInstant(ZoneOffset.of("-03:00"));
    }

    private Algorithm algoritmo() {
        return Algorithm.HMAC256(secret);
    }
}
