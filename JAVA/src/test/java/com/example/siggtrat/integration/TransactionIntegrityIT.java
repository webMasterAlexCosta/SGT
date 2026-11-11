package com.example.siggtrat.integration;

import com.example.siggtrat.entidade.ConfiguracaoSistema;
import com.example.siggtrat.entidade.StatusEmpregado;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import com.example.siggtrat.repositorio.StatusEmpregadoRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.RollbackException;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração PostgreSQL: integridade transacional e concorrência")
class TransactionIntegrityIT extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private ConfiguracaoSistemaRepositorio configuracoes;

    @Autowired
    private StatusEmpregadoRepositorio statusEmpregados;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("rollback desfaz todas as escritas da transação")
    void rollbackEhAtomico() {
        String prefix = "it.rollback." + UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(status -> {
            configuracoes.saveAndFlush(new ConfiguracaoSistema(prefix + ".1", "A", "rollback"));
            configuracoes.saveAndFlush(new ConfiguracaoSistema(prefix + ".2", "B", "rollback"));
            throw new RuntimeException("falha proposital para validar atomicidade");
        }));

        assertTrue(erro.getMessage().contains("falha proposital"));
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM configuracao_sistema WHERE chave LIKE ?", Integer.class, prefix + "%");
        assertEquals(0, count);
    }

    @Test
    @DisplayName("unique constraint rejeita duplicidade no próprio PostgreSQL")
    void uniqueConstraintEhAplicadaPeloBanco() {
        String descricao = identificadorCurto("UNQ");
        try {
            statusEmpregados.saveAndFlush(new StatusEmpregado(descricao));

            DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                    () -> statusEmpregados.saveAndFlush(new StatusEmpregado(descricao)));
            assertErroPostgres(erro, "23505");

            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM status_empregado WHERE descricao = ?", Integer.class, descricao);
            assertEquals(1, count);
        } finally {
            removerStatusCriados(descricao);
        }
    }

    @Test
    @DisplayName("NOT NULL é realmente imposto pelo schema gerado")
    void notNullEhAplicadoPeloBanco() {
        String chave = "it.notnull." + UUID.randomUUID();
        // HQL de mutação mantém o INSERT inválido no schema mapeado. Persistir
        // uma entidade nula poderia falhar na validação Java antes do PostgreSQL;
        // não desabilitamos essa validação nem aceitamos tal falha como prova.
        // A operação é traduzida pelo Hibernate, sem SQL nativo/manual.
        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    try {
                        entityManager.unwrap(Session.class).createMutationQuery("""
                                insert into ConfiguracaoSistema (chave, valor)
                                values (:chave, null)
                                """).setParameter("chave", chave).executeUpdate();
                    } catch (RuntimeException failure) {
                        // Mesma tradução padrão usada na integração Spring/JPA;
                        // a causa física continua obrigatória nas asserções abaixo.
                        DataAccessException translated = new HibernateJpaDialect()
                                .translateExceptionIfPossible(failure);
                        if (translated != null) throw translated;
                        throw failure;
                    }
                }));
        assertErroPostgres(erro, "23502");
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM configuracao_sistema WHERE chave = ?", Integer.class, chave));
    }

    @Test
    @DisplayName("@Version impede lost update entre duas transações")
    void optimisticLockImpedeLostUpdate() {
        String descricao = identificadorCurto("LCK");
        Long id;
        try {
            id = statusEmpregados.saveAndFlush(new StatusEmpregado(descricao)).getId();
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
            removerStatusCriados(descricao);
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
                statusEmpregados.saveAndFlush(new StatusEmpregado(descricao));
                success.incrementAndGet();
            } catch (DataIntegrityViolationException ex) {
                assertErroPostgres(ex, "23505");
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
            removerStatusCriados(descricao);
        }
    }

    private void removerStatusCriados(String descricao) {
        var ids = jdbc.queryForList("SELECT id_status FROM status_empregado WHERE descricao LIKE ?",
                Long.class, descricao + "%");
        statusEmpregados.deleteAllById(ids);
    }

    private static void assertErroPostgres(Throwable erro, String estadoEsperado) {
        SQLException sql = null;
        for (Throwable causa = erro; causa != null && causa.getCause() != causa; causa = causa.getCause()) {
            if (causa instanceof SQLException exception) {
                sql = exception;
                break;
            }
        }
        assertNotNull(sql, "A rejeição precisa ter origem no PostgreSQL, não apenas na validação Java/Hibernate.");
        assertEquals(0, sql.getErrorCode(), "pgJDBC transmite o SQLState da restrição.");
        assertEquals(estadoEsperado, sql.getSQLState(), "SQLState de integridade física no PostgreSQL.");
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
