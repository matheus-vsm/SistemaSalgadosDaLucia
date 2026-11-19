package br.com.salgadosdalucia.api.autenticacao;

import br.com.salgadosdalucia.api.exception.BusinessException;
import br.com.salgadosdalucia.api.usuario.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
                    .withClaim("tipo", "acesso")
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
                    .withClaim("tipo", "refresh")
                    .withExpiresAt(expiracao(120))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new BusinessException("Erro ao gerar token JWT!" + e.getMessage());
        }
    }

    public String verificaToken(String token) {
        return verificar(token, "acesso");
    }

    public String verificaRefreshToken(String token) {
        return verificar(token, "refresh");
    }

    private String verificar(String token, String tipo) {
        try {
            if (token == null || token.isBlank()) {
                throw new JWTVerificationException("Token ausente.");
            }
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .withClaim("tipo", tipo)
                    .build();
            DecodedJWT decodedJWT = verifier.verify(token);
            if (decodedJWT.getSubject() == null || decodedJWT.getSubject().isBlank()
                    || decodedJWT.getExpiresAtAsInstant() == null) {
                throw new JWTVerificationException("Identificação e expiração são obrigatórias.");
            }
            return decodedJWT.getSubject();
        } catch (JWTVerificationException e) {
            throw new BusinessException("Token JWT inválido ou expirado!" + e.getMessage());
        }
    }

    private Instant expiracao(Integer minutos) {
        return Instant.now().plus(minutos, ChronoUnit.MINUTES);
    }

}
