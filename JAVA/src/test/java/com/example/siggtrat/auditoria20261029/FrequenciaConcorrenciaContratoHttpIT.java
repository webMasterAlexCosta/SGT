package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.CalculoFrequenciaServico;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** O spy apenas sincroniza o ponto pré-INSERT; todo cálculo, HTTP e commit são reais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaConcorrenciaContratoHttpIT extends AuditoriaHttpSuporte {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSource;
    @MockitoSpyBean CalculoFrequenciaServico calculo;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(value = 45, unit = TimeUnit.SECONDS)
    void inativacaoNaoPodeConfirmarEDeixarFrequenciaConcorrenteValidaPorContextoAntigo(boolean lote) throws Exception {
        Cenario c = criarCenarioPersistido();
        String base = "/api/contratos/" + c.contratoA.getId();
        Map<String,Object> acao = new LinkedHashMap<>(Map.of("dataEfetiva", DATA,
                "motivo", "Inativação concorrente com frequência em processamento"));
        var previa = dados(executar(c.localA, post(base + "/previas/inativar")
                .contentType(APPLICATION_JSON).content(json(acao)), 200, "AUD-004 prévia sem frequência ainda persistida"));
        assertThat(previa.path("quantidadeFrequenciasAfetadas").asLong()).isZero();
        acao.put("tokenPrevia", previa.path("tokenPrevia").asText()); acao.put("confirmarImpacto", true);
        String comando = json(acao);
        Map<String,Object> item = new LinkedHashMap<>(Map.of("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA,
                "status", "FALTA", "justificativaRetificacao", "Conferência documental concorrente",
                "chaveIdempotencia", "corrida-" + UUID.randomUUID()));
        Map<String,Object> segundo = new LinkedHashMap<>(item);
        segundo.put("dataReferencia", DATA.plusDays(1)); segundo.put("chaveIdempotencia", "corrida-" + UUID.randomUUID());
        String lancamento = json(lote ? List.of(item, segundo) : item);
        var requisicaoFrequencia = autenticar(post(lote ? "/api/frequencias/lote" : "/api/frequencias")
                .contentType(APPLICATION_JSON).content(lancamento), c.localA);
        var requisicaoContrato = autenticar(post(base + "/inativar").contentType(APPLICATION_JSON).content(comando), c.localA);
        CompletableFuture<Long> contextoPronto = new CompletableFuture<>();
        CountDownLatch liberarInsercao = new CountDownLatch(1);
        AtomicBoolean sincronizar = new AtomicBoolean(true);
        doAnswer(invocacao -> {
            Frequencia f = invocacao.getArgument(0);
            if (f.getVinculoContratual() != null && c.vinculoA.getId().equals(f.getVinculoContratual().getId())
                    && sincronizar.compareAndSet(true, false)) {
                contextoPronto.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                if (!liberarInsercao.await(25, TimeUnit.SECONDS)) throw new AssertionError("Concorrência não chegou ao ponto observável.");
            }
            return invocacao.callRealMethod();
        }).when(org.springframework.test.util.AopTestUtils.<CalculoFrequenciaServico>getUltimateTargetObject(calculo)).processarCalculos(any(Frequencia.class));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> frequencia = pool.submit(() -> mvc.perform(requisicaoFrequencia).andReturn());
            long conexao = contextoPronto.get(10, TimeUnit.SECONDS);
            var atorContrato = new Ator("inativacao-FRE017");
            Future<MvcResult> contrato = pool.submit(() -> atorContrato.executar(() -> mvc.perform(requisicaoContrato).andReturn()));
            var espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource), conexao, atorContrato,
                    sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()),
                    "FRE017 inativação/Contrato " + c.contratoA.getId(), contrato);
            assertThat(espera).as("PID do ator, SQL e PK do Contrato vinculados à espera física").isNotEmpty();
            assertThat(contrato.isDone()).isFalse();
            liberarInsercao.countDown();
            MvcResult rf = frequencia.get(20, TimeUnit.SECONDS), rc = contrato.get(20, TimeUnit.SECONDS);
            assertThat(List.of(rf.getResponse().getStatus(), rc.getResponse().getStatus()))
                    .as("Frequência=%s; Contrato=%s", rf.getResponse().getContentAsString(), rc.getResponse().getContentAsString())
                    .containsExactlyInAnyOrder(200, 409);
            emTransacao(() -> {
                Contrato persistido = em.find(Contrato.class, c.contratoA.getId());
                List<Frequencia> fs = em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:id", Frequencia.class)
                        .setParameter("id", c.vinculoA.getId()).getResultList();
                if (rc.getResponse().getStatus() == 409) {
                    assertThat(persistido.getAtivo()).isTrue(); assertThat(fs).hasSize(lote ? 2 : 1);
                    assertThat(fs.get(0).getValidadeOperacional().name()).isEqualTo("VALIDA");
                } else {
                    assertThat(persistido.getAtivo()).isFalse(); assertThat(fs).isEmpty();
                }
                return null;
            });
        } finally { liberarInsercao.countDown(); pool.shutdownNow(); }
    }
}
