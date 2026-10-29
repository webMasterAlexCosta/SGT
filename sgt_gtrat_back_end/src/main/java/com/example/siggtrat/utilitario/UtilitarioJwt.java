package com.example.siggtrat.utilitario;

import com.example.siggtrat.configuracao.propriedades.PropriedadesTokenJwt;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class UtilitarioJwt {

    private static final Logger log = LoggerFactory.getLogger(UtilitarioJwt.class);

    private final long expirationSeconds;
    private final SecretKey key;

    public UtilitarioJwt(PropriedadesTokenJwt properties) {
        this.expirationSeconds = properties.expiration();
        try {
            this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException e) {
            log.error("ERRO CRÍTICO DE SEGURANÇA: a chave JWT configurada é muito fraca para HS512.");
            log.error("Use uma string com pelo menos 64 caracteres em jwt.secret.");
            throw e;
        }
    }

    public String gerarToken(Long userId) {
        return gerarToken(userId, false);
    }

    /**
     * Gera o JWT da sessão registrando se o segundo fator foi efetivamente
     * validado neste autenticar. O claim é assinado junto com o restante do token,
     * portanto não pode ser habilitado pelo front-end.
     */
    public String gerarToken(Long userId, boolean twoFactorVerified) {
        long nowMs = System.currentTimeMillis();
        long expMs = nowMs + (expirationSeconds * 1000L);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("twoFactorVerified", twoFactorVerified)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(expMs))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public long obterSegundosExpiracao() {
        return expirationSeconds;
    }

    public Long extrairIdUsuario(String token) {
        return Long.parseLong(extrairReivindicacao(token, Claims::getSubject));
    }

    public Date extrairExpiracao(String token) {
        return extrairReivindicacao(token, Claims::getExpiration);
    }

    public boolean segundoFatorVerificado(String token) {
        try {
            Boolean value = extrairReivindicacao(token, claims -> claims.get("twoFactorVerified", Boolean.class));
            return Boolean.TRUE.equals(value);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public <T> T extrairReivindicacao(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extrairTodasReivindicacoes(token));
    }

    private Claims extrairTodasReivindicacoes(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Assinatura JWT (HS512) inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (WeakKeyException e) {
            log.error("Chave JWT fraca detectada durante validação.");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Erro na validação do JWT: {}", e.getMessage());
        }
        return false;
    }
}
