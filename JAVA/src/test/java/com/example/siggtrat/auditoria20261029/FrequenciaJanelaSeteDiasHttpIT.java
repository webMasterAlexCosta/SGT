package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.servico.AuditoriaFrequenciaServico;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * FRE-002/003/013/014: Spring MVC, autenticação, autorização, política e persistência
 * reais, com PostgreSQL 18.6. Somente a leitura do relógio ZonedDateTime.now(zona) é
 * controlada para atingir a fronteira exata sem modificar política ou banco.
 * MockMvc não substitui o gate separado de transporte TCP/navegador do JAR.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaJanelaSeteDiasHttpIT extends ContratosHttpBase {
    @Autowired DataSource dataSource;
    @Autowired Environment ambiente;
    @Autowired PoliticaTemporalFrequenciaServico politica;
    @Autowired AuditoriaFrequenciaServico auditoria;

    @BeforeEach
    void confirmarMySqlESchemaGeridoPeloHibernate() throws Exception {
        try (var conexao = dataSource.getConnection()) {
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(conexao.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
    }

    @ParameterizedTest(name = "Editar em limite{0}ns retorna HTTP {1}")
    @CsvSource({"-1,200", "0,403", "1,403"})
    void apiAplicaLimiteExclusivoSemAlterarFrequenciaRecusada(long deslocamentoNanos, int esperado) throws Exception {
        Cenario c = criarCenarioPersistido();
        // Segunda-feira real coberta pela jornada da fixture, anterior a hoje.
        LocalDate data = segundaAnterior();
        ZonedDateTime limite = data.plusDays(7).atStartOfDay(RelogioSistema.ZONA);
        ZonedDateTime antes = limite.minusSeconds(1);
        ZonedDateTime instanteTeste = limite.plusNanos(deslocamentoNanos);
        assertThat(politica.instanteFechamento(data)).isEqualTo(limite);
        long id;
        Foto original;
        JsonNode resposta;
        Foto posterior;
        try (var relogio = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(antes);
            JsonNode criada = enviar(c.operacionalA, "/api/frequencias", pedido(c, data, "FALTA"), 200,
                    "Controle positivo: registro operacional no sétimo dia antes do fechamento");
            id = criada.path("id").asLong();
            assertThat(criada.path("estadoNegocial").asText()).isEqualTo("ABERTA");
            assertThat(criada.path("bloqueadaEdicaoOperacional").asBoolean()).isFalse();
            original = foto(id);
            assertThat(original.eventos()).hasSize(1);
            assertThat(original.raiz().get("versaoNegocial")).isEqualTo(1);

            relogio.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(instanteTeste);
            resposta = enviar(c.operacionalA, "/api/frequencias", pedido(c, data, "PRESENCA"), esperado,
                    "FRE-002/003: edição direta à API antes, no limite exclusivo ou depois dele");
            posterior = foto(id);
            if (esperado == 200) {
                assertThat(resposta.path("id").asLong()).isEqualTo(id);
                assertThat(resposta.path("status").asText()).isEqualTo("PRESENCA");
                assertThat(resposta.path("estadoNegocial").asText()).isEqualTo("ABERTA");
                assertThat(resposta.path("estadoAuditoria").asText()).isEqualTo("ALTERADA_OPERACIONAL");
                assertThat(posterior.raiz().get("versaoNegocial")).isEqualTo(2);
                assertThat(posterior.raiz().get("autorOriginalId")).isEqualTo(c.operacionalA.getId());
                assertThat(posterior.eventos()).hasSize(2);
                assertThat(posterior.eventos().get(0)).isEqualTo(original.eventos().get(0));
                assertThat(posterior.eventos().get(1).get("hashAnterior"))
                        .isEqualTo(original.eventos().get(0).get("hashEvento"));
                assertThat(posterior.eventos().get(1).get("usuarioId")).isEqualTo(c.operacionalA.getId());
                assertThat(posterior.eventos().get(1).get("snapshotAnterior").toString()).contains("status=FALTA");
                assertThat(posterior.eventos().get(1).get("snapshotNovo").toString()).contains("status=PRESENCA");
            } else {
                assertThat(resposta.path("message").asText()).contains("FECHADA");
                assertThat(posterior).as("A recusa desfaz inclusive qualquer fechamento tentado na transação")
                        .isEqualTo(original);
            }
            JsonNode pagina = dados(executar(c.localA, get("/api/frequencias")
                            .param("data", data.toString()).param("contratoId", c.contratoA.getId().toString()),
                    200, "Leitura projeta o estado temporal sem ampliar a autorização de escrita"));
            assertThat(pagina.path("totalElements").asInt()).isEqualTo(1);
            assertThat(pagina.path("content").get(0).path("id").asLong()).isEqualTo(id);
            assertThat(pagina.path("content").get(0).path("estadoNegocial").asText())
                    .isEqualTo(esperado == 200 ? "ABERTA" : "FECHADA");
            assertThat(foto(id)).isEqualTo(posterior);
            assertThat(auditoria.evidenciasIntegridade(id).cadeiaIntegra()).isTrue();
        }
        registrar("limite-" + deslocamentoNanos, mapa("dataReferencia", data,
                "zona", RelogioSistema.ZONA.getId(), "limiteExclusivo", limite,
                "instantePolitica", instanteTeste, "statusEsperado", esperado,
                "raizAntes", original, "raizDepois", posterior, "resposta", resposta));
    }

    @Test
    void intervencaoGerencialBloqueiaSomentePrimeiroRegistroEPreservaControleEditavel() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate primeiroDia = segundaAnterior(), segundoDia = primeiroDia.plusDays(1);
        ZonedDateTime agora = primeiroDia.plusDays(3).atTime(12, 0).atZone(RelogioSistema.ZONA);
        try (var relogio = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS)) {
            relogio.when(() -> ZonedDateTime.now(RelogioSistema.ZONA)).thenReturn(agora);
            long primeiro = enviar(c.operacionalA, "/api/frequencias", pedido(c, primeiroDia, "FALTA"), 200,
                    "Primeira frequência criada na própria janela").path("id").asLong();
            long segundo = enviar(c.operacionalA, "/api/frequencias", pedido(c, segundoDia, "FALTA"), 200,
                    "Segunda frequência independente criada no mesmo vínculo").path("id").asLong();
            assertThat(primeiro).isNotEqualTo(segundo);
            Foto primeiraAntes = foto(primeiro), controleAntes = foto(segundo);
            Map<String, Object> correcao = pedido(c, primeiroDia, "PRESENCA");
            correcao.put("justificativaRetificacao", "Conferência documental apenas da primeira frequência");
            correcao.put("chaveIdempotencia", "sete-dias-gestor-" + UUID.randomUUID());
            JsonNode retificada = enviar(c.localA, "/api/frequencias", correcao, 200,
                    "FRE-014: gestor intervém somente no primeiro registro, sem fechar a janela");
            assertThat(retificada.path("estadoNegocial").asText()).isEqualTo("ABERTA");
            assertThat(retificada.path("bloqueadaEdicaoOperacional").asBoolean()).isTrue();
            Foto primeiraRetificada = foto(primeiro);
            assertThat(primeiraRetificada.raiz().get("autorOriginalId")).isEqualTo(c.operacionalA.getId());
            assertThat(primeiraRetificada.raiz().get("ultimaRetificacaoPorId")).isEqualTo(c.localA.getId());
            assertThat(primeiraRetificada.eventos()).hasSize(2);
            assertThat(primeiraRetificada.eventos().get(0)).isEqualTo(primeiraAntes.eventos().get(0));
            assertThat(primeiraRetificada.eventos().get(1).get("motivo"))
                    .isEqualTo(correcao.get("justificativaRetificacao"));
            assertThat(foto(segundo)).as("A retificação do primeiro não grava nada no segundo").isEqualTo(controleAntes);

            enviar(c.operacionalA, "/api/frequencias", pedido(c, primeiroDia, "FALTA"), 403,
                    "O registro retificado permanece bloqueado ao Operacional");
            assertThat(foto(primeiro)).isEqualTo(primeiraRetificada);
            assertThat(foto(segundo)).isEqualTo(controleAntes);
            JsonNode controleAlterado = enviar(c.operacionalA, "/api/frequencias", pedido(c, segundoDia, "PRESENCA"), 200,
                    "O segundo registro continua editável na janela apesar da intervenção no primeiro");
            Foto controleDepois = foto(segundo);
            assertThat(controleAlterado.path("bloqueadaEdicaoOperacional").asBoolean()).isFalse();
            assertThat(controleAlterado.path("estadoNegocial").asText()).isEqualTo("ABERTA");
            assertThat(controleAlterado.path("estadoAuditoria").asText()).isEqualTo("ALTERADA_OPERACIONAL");
            assertThat(controleDepois.raiz().get("versaoNegocial")).isEqualTo(2);
            assertThat(controleDepois.eventos()).hasSize(2);
            assertThat(controleDepois.eventos().get(0)).isEqualTo(controleAntes.eventos().get(0));
            assertThat(foto(primeiro)).as("Editar o controle também não altera a frequência retificada")
                    .isEqualTo(primeiraRetificada);
            assertThat(auditoria.evidenciasIntegridade(primeiro).cadeiaIntegra()).isTrue();
            assertThat(auditoria.evidenciasIntegridade(segundo).cadeiaIntegra()).isTrue();
            registrar("dois-registros", mapa("instantePolitica", agora, "primeiraAntes", primeiraAntes,
                    "primeiraRetificada", primeiraRetificada, "controleAntes", controleAntes, "controleDepois", controleDepois));
        }
    }

    private LocalDate segundaAnterior() {
        return RelogioSistema.hoje().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(7);
    }

    private Map<String, Object> pedido(Cenario c, LocalDate data, String status) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status);
    }

    private Foto foto(long id) {
        return emTransacao(() -> {
            em.clear();
            Frequencia f = em.find(Frequencia.class, id);
            Map<String, Object> raiz = mapa("id", f.getId(), "dataReferencia", f.getDataReferencia(),
                    "status", f.getStatus(), "versaoNegocial", f.getVersaoNegocial(),
                    "estadoNegocial", f.getEstadoNegocial(), "estadoAuditoria", f.getEstadoAuditoria(),
                    "validadeOperacional", f.getValidadeOperacional(),
                    "bloqueadaEdicaoOperacional", f.getBloqueadaEdicaoOperacional(),
                    "autorOriginalId", f.getAutorOriginalId(), "ultimaRetificacaoPorId", f.getUltimaRetificacaoPorId(),
                    "registradoEm", f.getRegistradoEm(), "updatedAt", f.getUpdatedAt(),
                    "vinculoId", f.getVinculoContratual().getId(), "alocacaoId", f.getAlocacaoVinculo().getId(),
                    "contratoSnapshotId", f.getContratoSnapshotId());
            List<Map<String, Object>> eventos = em.createQuery(
                    "select e from EventoFrequencia e where e.frequencia.id=:id order by e.numeroVersao,e.id",
                    EventoFrequencia.class).setParameter("id", id).getResultList().stream().map(e -> mapa(
                            "id", e.getId(), "versao", e.getNumeroVersao(), "tipo", e.getTipoEvento(),
                            "usuarioId", e.getUsuarioId(), "registradoEm", e.getRegistradoEm(), "motivo", e.getMotivo(),
                            "snapshotAnterior", e.getSnapshotAnterior(), "snapshotNovo", e.getSnapshotNovo(),
                            "hashAnterior", e.getHashAnterior(), "hashEvento", e.getHashEvento())).toList();
            return new Foto(raiz, eventos);
        });
    }

    private void registrar(String nome, Map<String, Object> evidencia) throws Exception {
        evidencia.put("observadoEmReal", Instant.now());
        evidencia.put("transporte", "Spring MockMvc; cadeia real de aplicação e PostgreSQL, sem TCP");
        evidencia.put("controleRelogio", "Somente ZonedDateTime.now(America/Sao_Paulo), CALLS_REAL_METHODS");
        Path path = Path.of("target/auditoria-http/frequencia-sete-dias-" + nome + ".json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidencia));
    }

    private record Foto(Map<String, Object> raiz, List<Map<String, Object>> eventos) { }
}
