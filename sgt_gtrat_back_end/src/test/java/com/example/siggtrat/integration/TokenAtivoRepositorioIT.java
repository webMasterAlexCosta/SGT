package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.TokenAtivo;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração MySQL: TokenAtivoRepositorio")
class TokenAtivoRepositorioIT extends MySqlIntegrationTestSupport {

    @Autowired
    private TokenAtivoRepositorio repositorio;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final String prefix = "it-token-" + UUID.randomUUID();

    @AfterEach
    void cleanup() {
        repositorio.deleteAll(repositorio.findAll().stream()
                .filter(t -> t.getUsername() != null && t.getUsername().startsWith(prefix))
                .toList());
    }

    @Test
    @DisplayName("hash único impede dois tokens persistidos com a mesma identidade")
    void tokenHashEhUnico() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 12, 0);
        String hash = "a".repeat(64);
        repositorio.saveAndFlush(new TokenAtivo(hash, prefix + "-a", now, now.plusHours(1)));

        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(new TokenAtivo(hash, prefix + "-b", now, now.plusHours(1))));
    }

    @Test
    @DisplayName("DELETE nativo com LIMIT respeita lote e não remove token válido")
    void deleteExpiredTokensBatchRespeitaLimite() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 12, 0);
        repositorio.saveAndFlush(token("1", now.minusHours(3), now.minusHours(2)));
        repositorio.saveAndFlush(token("2", now.minusHours(2), now.minusHours(1)));
        repositorio.saveAndFlush(token("3", now.minusHours(1), now.minusMinutes(1)));
        TokenAtivo valido = repositorio.saveAndFlush(token("4", now, now.plusHours(1)));

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int primeira = tx.execute(status -> repositorio.excluirLoteTokensExpirados(now, 2));
        int segunda = tx.execute(status -> repositorio.excluirLoteTokensExpirados(now, 2));

        assertEquals(2, primeira);
        assertEquals(1, segunda);
        assertTrue(repositorio.findById(valido.getId()).isPresent());
    }

    @Test
    @DisplayName("UPDATE nativo de lastSeen altera somente o token informado")
    void updateLastSeenEhPontual() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 12, 0);
        TokenAtivo a = repositorio.saveAndFlush(token("5", now, now.plusHours(2)));
        TokenAtivo b = repositorio.saveAndFlush(token("6", now, now.plusHours(2)));
        LocalDateTime novo = now.plusMinutes(17);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> repositorio.atualizarUltimoAcesso(a.getId(), novo));

        TokenAtivo atualA = repositorio.findById(a.getId()).orElseThrow();
        TokenAtivo atualB = repositorio.findById(b.getId()).orElseThrow();
        assertEquals(novo, atualA.getLastSeenAt());
        assertNull(atualB.getLastSeenAt());
    }

    private TokenAtivo token(String suffix, LocalDateTime criado, LocalDateTime expires) {
        String seed = prefix + suffix;
        String hash = Integer.toHexString(seed.hashCode());
        hash = (hash + "0".repeat(64)).substring(0, 64);
        return new TokenAtivo(hash, prefix + "-" + suffix, criado, expires);
    }
}
