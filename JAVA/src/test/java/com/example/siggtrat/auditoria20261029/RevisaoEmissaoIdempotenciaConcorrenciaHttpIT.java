package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** REL005/AUD005: duas requisições reais, transações próprias e disputa no PostgreSQL. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevisaoEmissaoIdempotenciaConcorrenciaHttpIT extends ContratosHttpBase {
    @Autowired DataSource dataSourceObservador;

    @Test
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void mesmaChaveConcorrenteConfirmaUmaEmissaoEDevolveMesmaProvaAosDoisClientes() throws Exception {
        var c = criarCenarioPersistido();
        String chave = "emissao-idempotente-mysql-" + UUID.randomUUID();
        var pedido = mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", DATA, "fim", DATA, "contratoIds", List.of(c.contratoA.getId()),
                "chaveIdempotencia", chave);
        // As duas requisições reutilizam a mesma sessão legítima, como ocorre
        // quando um cliente reenvia o comando antes de receber a resposta.
        var primeira = autenticar(post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(pedido)), c.localA);
        var segunda = autenticar(post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(pedido)), c.localA);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch prontas = new CountDownLatch(2), iniciar = new CountDownLatch(1);
        var atorA = new Ator("emissao-idempotente-A");
        var atorB = new Ator("emissao-idempotente-B");
        List<Map<String, Object>> provaEsperas = new ArrayList<>();
        try {
            List<Future<MvcResult>> respostas = emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                long conexao = ((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue();
                var a = executor.submit(() -> atorA.executar(() -> {
                    prontas.countDown(); assertTrue(iniciar.await(10, TimeUnit.SECONDS));
                    return executar(c.localA, primeira, 201, "Primeiro envio concorrente da mesma intenção");
                }));
                var b = executor.submit(() -> atorB.executar(() -> {
                    prontas.countDown(); assertTrue(iniciar.await(10, TimeUnit.SECONDS));
                    return executar(c.localA, segunda, 201, "Reenvio concorrente não duplica a emissão");
                }));
                try {
                    assertTrue(prontas.await(10, TimeUnit.SECONDS), "Ambas as threads chegaram à barreira");
                    iniciar.countDown();
                    // PID + SQL/bind de cada ator conhecido, com fila de lock
                    // restrita a esses atores e à raiz; nenhum waiter anônimo basta.
                    provaEsperas.addAll(EsperaJdbcPostgres.aguardarAmbos(
                            new JdbcTemplate(dataSourceObservador), conexao,
                            atorA, sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                            atorB, sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                            "contrato/" + c.contratoA.getId(), a, b));
                    assertEquals(2, provaEsperas.stream().map(p -> ((Number) p.get("solicitante")).longValue())
                            .distinct().count(), "As duas emissões estão em backends distintos no alvo da fixture");
                    assertFalse(a.isDone()); assertFalse(b.isDone());
                    return List.of(a, b);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                } catch (Exception ex) {
                    throw new IllegalStateException("A fixture não comprovou ambas as esperas da emissão", ex);
                }
            }); // Commit libera o lock; os serviços confirmam as próprias transações.
            JsonNode a = dados(respostas.get(0).get(15, TimeUnit.SECONDS));
            JsonNode b = dados(respostas.get(1).get(15, TimeUnit.SECONDS));
            long id = a.path("metadados").path("id").asLong();
            assertTrue(id > 0);
            assertEquals(id, b.path("metadados").path("id").asLong());
            assertEquals(a.path("conteudo"), b.path("conteudo"));
            assertEquals(a.path("metadados").path("hashConteudo"), b.path("metadados").path("hashConteudo"));
            emTransacao(() -> {
                var gravadas = em.createQuery("select e from EmissaoRelatorioAdministrativo e "
                                + "where e.usuarioId=:u and e.chaveIdempotencia=:chave", EmissaoRelatorioAdministrativo.class)
                        .setParameter("u", c.localA.getId()).setParameter("chave", chave).getResultList();
                assertEquals(1, gravadas.size(), "Nova transação confirma exatamente uma emissão persistida");
                assertEquals(id, gravadas.get(0).getId());
                assertEquals(1, gravadas.get(0).getNumeroVersao());
                assertEquals(a.path("metadados").path("hashConteudo").asText(), gravadas.get(0).getHashConteudo());
                return null;
            });
            JsonNode replay = enviar(c.localA, "/api/relatorios/administrativos/emissoes", pedido, 201,
                    "Reenvio depois dos commits também recupera a mesma emissão");
            assertEquals(id, replay.path("metadados").path("id").asLong());
            assertEquals(a.path("conteudo"), replay.path("conteudo"));
            Path recibo = Path.of("target/auditoria-http/rel005-emissao-concorrente-esperas.json");
            Files.createDirectories(recibo.getParent());
            Files.writeString(recibo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "recorded_at", Instant.now().toString(), "audit_round", System.getenv("SGT_AUDITORIA_RODADA"),
                    "suite", getClass().getSimpleName(), "contratoFixture", c.contratoA.getId(),
                    "atores", List.of(atorA.nome(), atorB.nome()), "esperas", provaEsperas,
                    "emissaoId", id, "emissoesPersistidasParaIntencao", 1,
                    "limite", "Observação de locks com SQL preparado e bindings opacos; não é captura de pacotes HTTP")));
        } finally {
            iniciar.countDown(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
