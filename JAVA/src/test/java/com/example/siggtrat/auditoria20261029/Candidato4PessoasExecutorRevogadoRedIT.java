package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria independente: /root/revisao_final_pessoas. Hipótese C4-PES-H04.
 * C4 congelado 13c84fd30b668335228704fd1028ddea11adad6f607458a7b7fa993dcbc64a89.
 * Teste proposto, não compilado/executado pelo revisor. Isola duas fronteiras de
 * confirmação com executor desativado depois da autenticação e antes do lock.
 * Fontes do helper publicado são explicitamente sintéticas; não recuperam acervo.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class Candidato4PessoasExecutorRevogadoRedIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void revogacaoDuranteEsperaNoContratoImpedeMatriculaEAlocacao(boolean alocacao) throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).isEqualTo("8.0.45");
        }
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 10, 5, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        JsonNode criado = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa sintética C4 revogação", "cpf",
                        cpf(690000000 + Math.toIntExact(contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(),
                "dataInicio", RelogioSistema.hoje(), "matricula", "MAT-C4-ORIGINAL-" + contrato.id()),
                201, "Pessoa e Vínculo contemporâneos em publicação sintética integral");
        long vinculoId = criado.path("id").asLong();
        String rota = "/api/vinculos-contratuais/" + vinculoId;
        Map<String, Object> req = mapa("justificativa", "Ato sintético C4 sob revogação concorrente",
                "evidencia", "SEI-SINTETICO-C4-REVOGACAO", "autorizacao", "ATO-SINTETICO-C4");
        if (alocacao) {
            req.put("dataEfetiva", RelogioSistema.hoje().plusDays(1));
            req.put("composicaoId", contrato.composicao2());
            JsonNode previa = enviar(c.localA, rota + "/previas/alocacoes", req, 200,
                    "Prévia válida antes de o executor ser desativado");
            assertThat(previa.path("incompatibilidades")).isEmpty();
            req.put("tokenPrevia", previa.path("token").asText());
            req.put("confirmarImpacto", true);
        } else {
            req.put("dataEfetiva", RelogioSistema.hoje());
            req.put("novaMatricula", "MAT-C4-NOVA-" + contrato.id());
        }
        var pedido = autenticar(post(rota + (alocacao ? "/alocacoes" : "/alterar-matricula"))
                .contentType(MediaType.APPLICATION_JSON).content(json(req)), c.localA);
        Estado antes = estado(vinculoId);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> bloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, contrato.id(), LockModeType.PESSIMISTIC_WRITE);
                bloqueadora.complete(((Number) em.createNativeQuery("select connection_id()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(25, TimeUnit.SECONDS)) throw new AssertionError("Contrato não foi liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = bloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> mvc.perform(pedido).andReturn());
            aguardarBloqueioReal(conexao, tentativa);
            executar(c.admin, put("/api/usuarios/" + c.localA.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content(json(mapa("active", false))), 200,
                    "Admin desativa executor e confirma antes de liberar a operação negocial");
            assertThat(emTransacao(() -> em.find(UsuarioAplicacao.class, c.localA.getId()).getActive())).isFalse();
            assertThat(tentativa.isDone()).as("Revogação commitou durante espera negocial real").isFalse();
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resposta = tentativa.get(15, TimeUnit.SECONDS);
            Estado depois = estado(vinculoId);
            Path evidencia = Path.of("target/auditoria-http/candidato4-pessoas-revogacao-" + alocacao + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, json(mapa("hipotese", "C4-PES-H04", "alocacao", alocacao,
                    "status", resposta.getResponse().getStatus(), "estadoAntes", antes, "estadoDepois", depois,
                    "espera_mysql_observada", true, "revogacao_commitou_antes_da_liberacao", true)));
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(resposta.getResponse().getStatus()).isIn(401, 403, 404, 409);
                soft.assertThat(depois).as("Revogação impede Vínculo, reservas, faixas e eventos parciais").isEqualTo(antes);
            });
        } finally {
            liberar.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Estado estado(long vinculoId) {
        return emTransacao(() -> {
            em.clear();
            VinculoContratual v = em.find(VinculoContratual.class, vinculoId);
            List<String> faixas = em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:v order by a.id",
                            AlocacaoVinculo.class).setParameter("v", vinculoId).getResultList().stream()
                    .map(a -> a.getId() + ":" + a.getVersion() + ":" + a.getDataInicio() + ":" + a.getDataFim()
                            + ":" + a.getEstadoNegocial() + ":" + a.getComposicao().getId()).toList();
            return new Estado(v.getVersion(), v.getMatricula(), faixas,
                    contar("EventoVinculoContratual", vinculoId), contar("EventoAlocacaoVinculo", vinculoId),
                    contar("ReservaMatriculaContrato", vinculoId));
        });
    }
    private long contar(String entidade, long vinculoId) {
        return em.createQuery("select count(e) from " + entidade + " e where e.vinculo.id=:v", Long.class)
                .setParameter("v", vinculoId).getSingleResult();
    }
    private void aguardarBloqueioReal(long bloqueadora, Future<?> tentativa) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            Long esperas = jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w "
                    + "join performance_schema.threads t on t.THREAD_ID=w.BLOCKING_THREAD_ID "
                    + "where t.PROCESSLIST_ID=?", Long.class, bloqueadora);
            if (esperas != null && esperas >= 1) return;
            if (tentativa.isDone()) throw new AssertionError("Comando não alcançou a espera real no Contrato");
            Thread.sleep(25);
        }
        throw new AssertionError("Espera não observada no MySQL performance_schema");
    }
    private record Estado(long versaoVinculo, String matricula, List<String> faixas,
                          long eventosVinculo, long eventosAlocacao, long reservas) {}
}
