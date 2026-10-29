package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.StatusEmpregado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração MySQL: integridade transacional e concorrência")
class TransactionIntegrityIT extends MySqlIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory emf;

    @Test
    @DisplayName("rollback desfaz todas as escritas da transação")
    void rollbackEhAtomico() {
        String prefix = "it.rollback." + UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO configuracao_sistema (chave, valor, descricao) VALUES (?, ?, ?)",
                    prefix + ".1", "A", "rollback");
            jdbc.update("INSERT INTO configuracao_sistema (chave, valor, descricao) VALUES (?, ?, ?)",
                    prefix + ".2", "B", "rollback");
            throw new RuntimeException("falha proposital para validar atomicidade");
        }));

        assertTrue(erro.getMessage().contains("falha proposital"));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM configuracao_sistema WHERE chave LIKE ?", Integer.class, prefix + "%");
        assertEquals(0, count);
    }

    @Test
    @DisplayName("unique constraint rejeita duplicidade no próprio MySQL")
    void uniqueConstraintEhAplicadaPeloBanco() {
        String descricao = identificadorCurto("UNQ");
        try {
            jdbc.update("INSERT INTO status_empregado (descricao, version) VALUES (?, 0)", descricao);

            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("INSERT INTO status_empregado (descricao, version) VALUES (?, 0)", descricao));

            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM status_empregado WHERE descricao = ?", Integer.class, descricao);
            assertEquals(1, count);
        } finally {
            jdbc.update("DELETE FROM status_empregado WHERE descricao = ?", descricao);
        }
    }

    @Test
    @DisplayName("NOT NULL é realmente imposto pelo schema gerado")
    void notNullEhAplicadoPeloBanco() {
        String chave = "it.notnull." + UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO configuracao_sistema (chave, valor) VALUES (?, NULL)", chave));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM configuracao_sistema WHERE chave = ?", Integer.class, chave));
    }

    @Test
    @DisplayName("@Version impede lost update entre duas transações")
    void optimisticLockImpedeLostUpdate() {
        String descricao = identificadorCurto("LCK");
        Long id;
        try {
            jdbc.update("INSERT INTO status_empregado (descricao, version) VALUES (?, 0)", descricao);
            id = jdbc.queryForObject("SELECT id_status FROM status_empregado WHERE descricao = ?", Long.class, descricao);
            assertNotNull(id);

            EntityManager em1 = emf.createEntityManager();
            EntityManager em2 = emf.createEntityManager();
            try {
                em1.getTransaction().begin();
                em2.getTransaction().begin();

                StatusEmpregado a = em1.find(StatusEmpregado.class, id);
                StatusEmpregado b = em2.find(StatusEmpregado.class, id);
                assertNotNull(a);
                assertNotNull(b);
                assertEquals(a.getVersion(), b.getVersion());

                a.setDescricao(descricao + "_A");
                em1.getTransaction().commit();

                b.setDescricao(descricao + "_B");
                RuntimeException conflito = assertThrows(RuntimeException.class, () -> em2.getTransaction().commit());
                assertTrue(temCausa(conflito, OptimisticLockException.class)
                                || conflito instanceof RollbackException,
                        () -> "Era esperado conflito otimista, recebido: " + conflito);
            } finally {
                rollbackSeAtiva(em1);
                rollbackSeAtiva(em2);
                em1.close();
                em2.close();
            }

            String finalValue = jdbc.queryForObject(
                    "SELECT descricao FROM status_empregado WHERE id_status = ?", String.class, id);
            assertEquals(descricao + "_A", finalValue);
            Long version = jdbc.queryForObject(
                    "SELECT version FROM status_empregado WHERE id_status = ?", Long.class, id);
            assertEquals(1L, version);
        } finally {
            jdbc.update("DELETE FROM status_empregado WHERE descricao LIKE ?", descricao + "%");
        }
    }

    @Test
    @DisplayName("corrida de insert mantém unicidade sem indisponibilizar o banco")
    void corridaDeInsertPreservaUniqueEAConexaoContinuaSaudavel() throws Exception {
        String descricao = identificadorCurto("RCE");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        Callable<Void> insert = () -> {
            start.await(5, TimeUnit.SECONDS);
            try {
                jdbc.update("INSERT INTO status_empregado (descricao, version) VALUES (?, 0)", descricao);
                success.incrementAndGet();
            } catch (DataIntegrityViolationException ex) {
                duplicate.incrementAndGet();
            }
            return null;
        };

        try {
            Future<Void> a = pool.submit(insert);
            Future<Void> b = pool.submit(insert);
            start.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);

            assertEquals(1, success.get());
            assertEquals(1, duplicate.get());
            assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
        } finally {
            pool.shutdownNow();
            jdbc.update("DELETE FROM status_empregado WHERE descricao = ?", descricao);
        }
    }

    private static String identificadorCurto(String prefixo) {
        return "IT_" + prefixo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static void rollbackSeAtiva(EntityManager em) {
        if (em != null && em.isOpen() && em.getTransaction().isActive()) em.getTransaction().rollback();
    }

    private static boolean temCausa(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null && current.getCause() != current; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }
}
