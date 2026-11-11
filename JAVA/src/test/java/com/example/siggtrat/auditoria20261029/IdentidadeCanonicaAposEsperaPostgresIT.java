package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.example.siggtrat.servico.BloqueioEscritaFrequenciaServico;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Proposta P03: observar o ator na PK real do Contrato e revalidar associação após a espera. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdentidadeCanonicaAposEsperaPostgresIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired BloqueioEscritaFrequenciaServico bloqueios;

    @ParameterizedTest(name = "associacaoRetiradaDuranteEspera={0}")
    @ValueSource(booleans = {false, true})
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void revalidaAssociacaoExplicitaMesmoQuandoCpfPermaneceIgual(boolean retirarAssociacao) throws Exception {
        Cenario c = criarCenarioPersistido();
        String cpfOriginal = c.pessoaA.getCpf();
        var pool = Executors.newFixedThreadPool(2);
        var liberarContrato = new CountDownLatch(1);
        var pidBloqueador = new CompletableFuture<Long>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                pidBloqueador.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(30, TimeUnit.SECONDS)) throw new AssertionError("Contrato não liberado pela fixture");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return null;
            }));
            long pid = pidBloqueador.get(10, TimeUnit.SECONDS);
            Ator ator = new Ator("identidade-frequencia-empregado");
            Future<Resultado> tentativa = pool.submit(() -> ator.executar(() -> {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
                try {
                    return tx.execute(status -> {
                        Empregado e = bloqueios.bloquearEmpregados(List.of(c.empregadoA.getId())).get(c.empregadoA.getId());
                        return new Resultado(200, e.getId(), e.getPessoaCanonica() == null ? null : e.getPessoaCanonica().getId());
                    });
                } catch (ResponseStatusException ex) { return new Resultado(ex.getStatusCode().value(), null, null); }
            }));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), pid, ator,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                    "Contrato conhecido da associação canônica em revalidação", tentativa);
            if (retirarAssociacao) emTransacao(() -> {
                Empregado e = em.find(Empregado.class, c.empregadoA.getId());
                assertEquals(c.pessoaA.getId(), e.getPessoaCanonica().getId());
                e.setPessoaCanonica(null); em.flush();
                assertEquals(cpfOriginal, e.getCpf(), "CPF propositalmente permanece igual"); return null;
            });
            liberarContrato.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            Resultado resultado = tentativa.get(15, TimeUnit.SECONDS);
            assertEquals(retirarAssociacao ? 409 : 200, resultado.status());
            if (!retirarAssociacao) {
                assertEquals(c.empregadoA.getId(), resultado.empregado());
                assertEquals(c.pessoaA.getId(), resultado.pessoa());
            }
            emTransacao(() -> {
                Empregado e = em.find(Empregado.class, c.empregadoA.getId());
                assertEquals(cpfOriginal, e.getCpf());
                if (retirarAssociacao) assertNull(e.getPessoaCanonica());
                else assertEquals(c.pessoaA.getId(), e.getPessoaCanonica().getId());
                assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.empregado.id=:id", Long.class)
                        .setParameter("id", e.getId()).getSingleResult(), "Adquirir/rejeitar locks não cria fonte");
                return null;
            });
            Path evidencia = Path.of("target/auditoria-http/identidade-canonica-apos-espera-" + retirarAssociacao + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "esperaPostgresObservada", espera, "contratoFixture", c.contratoA.getId(), "empregadoFixture", c.empregadoA.getId(),
                    "pessoaFixture", c.pessoaA.getId(), "associacaoRetiradaDuranteEspera", retirarAssociacao,
                    "resultado", resultado, "origem", "ator identificado + SQL/bindings/PK + pg_locks/pg_blocking_pids")));
        } finally {
            liberarContrato.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private record Resultado(int status, Long empregado, Long pessoa) {}
}
