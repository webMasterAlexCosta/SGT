package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Proposta TDD fase36. Não executada. Trinta datas civis, incluindo D como dia1. */
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrequenciaJanelaConfiguravelHttpIT extends ContratosHttpBase {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    @Autowired PoliticaTemporalFrequenciaServico politica;

    @Test
    void configuracaoTrintaMantemLimiteCivilExclusivoEFusoNegocial() {
        LocalDate dia = LocalDate.of(2026, 10, 1);
        ZonedDateTime limite = dia.plusDays(30).atStartOfDay(ZONA);
        assertEquals(limite, politica.instanteFechamento(dia));
        assertEquals(limite.toLocalDateTime(), politica.prazoLocal(dia));
        assertTrue(politica.abertaParaOperacional(dia, limite.minusNanos(1)));
        assertFalse(politica.abertaParaOperacional(dia, limite));
        assertTrue(politica.fechada(dia, limite.plusNanos(1)));
        assertTrue(politica.fechada(dia, limite.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))));
    }

    @Test
    void operacionalCriaEEditaAlemDeSeteEGestorBloqueiaSomenteRegistroRetificado() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FASE36-JANELA-CONFIGURAVEL");
        LocalDate data = LocalDate.now(ZONA).minusDays(8)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        assertTrue(data.isBefore(LocalDate.now(ZONA).minusDays(6)), "Preparo excede a regra antiga");
        assertFalse(data.isBefore(LocalDate.now(ZONA).minusDays(29)), "Preparo dentro das30datas");
        JsonNode inicial = gravar(c.operacionalA, corpo(c, data, "FALTA", null), 200);
        JsonNode editada = gravar(c.operacionalA, corpo(c, data, "PRESENCA", null), 200);
        assertEquals(inicial.path("id"), editada.path("id"));
        assertEquals("ABERTA", editada.path("estadoNegocial").asText());
        assertEquals("ALTERADA_OPERACIONAL", editada.path("estadoAuditoria").asText());
        assertEquals(inicial.path("versaoNegocial").asLong() + 1, editada.path("versaoNegocial").asLong());
        assertEquals(data.plusDays(30).atStartOfDay(), LocalDateTime.parse(editada.path("prazoEdicao").asText()));
        JsonNode retificada = gravar(c.localA, corpo(c, data, "FALTA", "gestor-" + UUID.randomUUID()), 200);
        assertEquals("ABERTA", retificada.path("estadoNegocial").asText());
        assertTrue(retificada.path("bloqueadaEdicaoOperacional").asBoolean());
        assertEquals("RETIFICADA_GESTOR", retificada.path("estadoAuditoria").asText());
        gravar(c.operacionalA, corpo(c, data, "PRESENCA", null), 403);
        JsonNode controle = gravar(c.operacionalA, corpo(c, data.minusDays(1), "PRESENCA", null), 200);
        assertFalse(controle.path("bloqueadaEdicaoOperacional").asBoolean());
        assertEquals("ABERTA", controle.path("estadoNegocial").asText());
        JsonNode eventos = consultar(c.localA, "/api/frequencias/" + inicial.path("id").asLong() + "/eventos", "Trilha íntegra das três intenções");
        assertEquals(3, eventos.size());
        assertEquals(c.operacionalA.getId().longValue(), eventos.get(0).path("usuarioId").asLong());
        assertEquals(c.localA.getId().longValue(), eventos.get(2).path("usuarioId").asLong());
    }

    @Test
    void foraDaJanelaOperacionalGestorRetificaSemReabrirEdicaoOperacional() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FASE36-RETIFICACAO-EXCEPCIONAL");
        LocalDate data = LocalDate.now(ZONA).minusDays(31)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        gravar(c.operacionalA, corpo(c, data, "PRESENCA", null), 403);
        JsonNode retificada = gravar(c.localA, corpo(c, data, "PRESENCA", "tardia-" + UUID.randomUUID()), 200);
        assertEquals("FECHADA", retificada.path("estadoNegocial").asText());
        assertEquals("RETIFICADA_GESTOR", retificada.path("estadoAuditoria").asText());
        assertTrue(retificada.path("bloqueadaEdicaoOperacional").asBoolean());
        gravar(c.operacionalA, corpo(c, data, "FALTA", null), 403);
        gravar(c.localB, corpo(c, data, "FALTA", "alheia-" + UUID.randomUUID()), 403);
        gravar(c.admin, corpo(c, data, "FALTA", "admin-" + UUID.randomUUID()), 403);
        java.util.Set<LocalDate> datasAntigas = new java.util.LinkedHashSet<>(List.of(
                data.minusDays(politica.diasJanela()),
                INICIO.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))));
        for (LocalDate antiga : datasAntigas) {
            assertFalse(antiga.isBefore(INICIO), "Fixture só prova datas dentro da cobertura contratual explícita");
            assertTrue(politica.fechada(antiga, null));
            gravar(c.operacionalA, corpo(c, antiga, "FALTA", null), 403);
            JsonNode primeiraAntiga = gravar(c.localA, corpo(c, antiga, "FALTA", "gestor-antiga-" + UUID.randomUUID()), 200);
            JsonNode segundaAntiga = gravar(c.localA, corpo(c, antiga, "PRESENCA", "gestor-antiga-revisao-" + UUID.randomUUID()), 200);
            assertEquals(primeiraAntiga.path("id"), segundaAntiga.path("id"));
            assertEquals(primeiraAntiga.path("versaoNegocial").asLong() + 1, segundaAntiga.path("versaoNegocial").asLong());
            assertEquals("FECHADA", segundaAntiga.path("estadoNegocial").asText());
            assertEquals("RETIFICADA_GESTOR", segundaAntiga.path("estadoAuditoria").asText());
            long idAntiga = segundaAntiga.path("id").asLong();
            emTransacao(() -> {
                em.clear();
                var fonte = em.find(com.example.siggtrat.entidade.Frequencia.class, idAntiga);
                assertEquals(c.vinculoA.getId(), fonte.getVinculoContratual().getId());
                assertEquals(antiga, fonte.getDataReferencia());
                assertEquals(c.localA.getId(), fonte.getUltimaRetificacaoPorId());
                assertTrue(fonte.getBloqueadaEdicaoOperacional());
                return null;
            });
        }
    }

    @Test
    void ocorrenciaMultidiasPreservaFatoEAdiaFrequenciaFuturaComReexecucaoIdempotente() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FASE36-OCORRENCIA-MULTIDIAS");
        LocalDate hoje = LocalDate.now(ZONA);
        String chave = "ocorrencia-fase36-" + UUID.randomUUID();
        JsonNode criada = ocorrencia(c, hoje, chave);
        long id = criada.path("id").asLong();
        assertEquals(2, criada.path("quantidadeDias").asInt(), "Duração original não é truncada");
        assertEquals(1, frequencias(c, hoje));
        assertEquals(0, frequencias(c, hoje.plusDays(1)), "Registro de atestado não antecipa frequência");
        List<String> historia = historiaOcorrencia(id);
        List<String> eventosHoje = hashesFrequencia(c, hoje);
        assertFalse(historia.isEmpty());
        assertEquals(1, eventosHoje.size());
        assertEquals(id, ocorrencia(c, hoje, chave).path("id").asLong());
        assertEquals(historia, historiaOcorrencia(id));
        assertEquals(eventosHoje, hashesFrequencia(c, hoje));
        assertEquals(0, frequencias(c, hoje.plusDays(1)));

        // Relógio simulado para a requisição síncrona MockMvc: não é virada física de dia.
        try (MockedStatic<LocalDate> calendario = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            calendario.when(() -> LocalDate.now(ZONA)).thenReturn(hoje.plusDays(1));
            assertEquals(id, ocorrencia(c, hoje, chave).path("id").asLong());
            assertEquals(1, frequencias(c, hoje.plusDays(1)), "Dia chegou: projeção pode materializar, fato permanece o mesmo");
            List<String> eventosAmanha = hashesFrequencia(c, hoje.plusDays(1));
            assertEquals(1, eventosAmanha.size());
            assertEquals(id, ocorrencia(c, hoje, chave).path("id").asLong());
            assertEquals(eventosAmanha, hashesFrequencia(c, hoje.plusDays(1)), "Novo reenvio não duplica eventos");
        }
        assertEquals(historia, historiaOcorrencia(id));
        assertEquals(eventosHoje, hashesFrequencia(c, hoje));
        assertEquals(1L, emTransacao(() -> em.createQuery(
                "select count(o) from Ocorrencia o where o.id=:id", Long.class).setParameter("id", id).getSingleResult()).longValue());
    }

    @Test
    void reenvioDaOcorrenciaPreservaRetificacaoManualPosteriorENaoReexecutaDiaProjetado() throws Exception {
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FASE36-REPLAY-PRESERVA-RETIFICACAO");
        LocalDate hoje = LocalDate.now(ZONA);
        String chave = "ocorrencia-replay-posterior-" + UUID.randomUUID();
        JsonNode original = ocorrencia(c, hoje, chave);
        // A jornada desta fixture trabalha de segunda a sexta. A retificação manual
        // deve ser válida também quando a suíte executa no fim de semana; a regra
        // de descanso do produto permanece intacta e não é o alvo deste teste de replay.
        String statusManual = hoje.getDayOfWeek().getValue() <= 5 ? "FALTA" : "DESCANSO";
        Map<String,Object> correcao = corpo(c, hoje, statusManual, "correcao-posterior-" + UUID.randomUUID());
        correcao.put("observacao", "Conferência manual posterior que não pode ser refeita por um reenvio antigo");
        JsonNode corrigida = gravar(c.localA, correcao, 200);
        assertEquals(statusManual, corrigida.path("status").asText());
        List<String> eventos = hashesFrequencia(c, hoje);
        assertEquals(2, eventos.size());
        List<Object> antes = fotoFrequencia(corrigida.path("id").asLong());
        List<String> historico = historiaOcorrencia(original.path("id").asLong());
        assertEquals(original.path("id").asLong(), ocorrencia(c, hoje, chave).path("id").asLong());
        assertEquals(antes, fotoFrequencia(corrigida.path("id").asLong()));
        assertEquals(eventos, hashesFrequencia(c, hoje));
        assertEquals(historico, historiaOcorrencia(original.path("id").asLong()));
        assertEquals(0, frequencias(c, hoje.plusDays(1)));
    }

    private List<Object> fotoFrequencia(long id) {
        return emTransacao(() -> {
            var f = em.find(com.example.siggtrat.entidade.Frequencia.class, id);
            return List.of(f.getStatus(), f.getObservacao(), f.getVersaoNegocial(), f.getVersion());
        });
    }

    @Test
    void frequenciaFuturaERecusadaParaOperacionalEGestorSemPersistencia() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate amanha = LocalDate.now(ZONA).plusDays(1);
        for (var usuario : List.of(c.operacionalA, c.localA)) {
            gravar(usuario, corpo(c, amanha, "PRESENCA", "futuro-" + UUID.randomUUID()), 400);
            assertEquals(0, frequencias(c, amanha));
            assertEquals(List.of(), hashesFrequencia(c, amanha));
        }
    }

    private JsonNode gravar(com.example.siggtrat.entidade.UsuarioAplicacao user, Map<String,Object> corpo, int status) throws Exception {
        return dados(executar(user, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(corpo)), status,
                "Janela configurável preserva escopo e retificação"));
    }
    private Map<String,Object> corpo(Cenario c, LocalDate data, String status, String chave) {
        Map<String,Object> r = mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status);
        if (chave != null) { r.put("justificativaRetificacao", "Conferência operacional explícita da fase36"); r.put("chaveIdempotencia", chave); }
        return r;
    }
    private JsonNode ocorrencia(Cenario c, LocalDate data, String chave) throws Exception {
        return dados(executar(c.localA, multipart("/api/ocorrencias")
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", data.toString()).param("dias", "2")
                .param("observacao", "Atestado sintético abrangendo dois dias").param("chaveIdempotencia", chave),
                201, "Fato multidias preservado sem frequência futura"));
    }
    private long frequencias(Cenario c, LocalDate data) {
        return emTransacao(() -> em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:v and f.dataReferencia=:d", Long.class)
                .setParameter("v", c.vinculoA.getId()).setParameter("d", data).getSingleResult());
    }
    private List<String> hashesFrequencia(Cenario c, LocalDate data) {
        return emTransacao(() -> em.createQuery("select e.hashEvento from EventoFrequencia e where e.frequencia.vinculoContratual.id=:v and e.frequencia.dataReferencia=:d order by e.id", String.class)
                .setParameter("v", c.vinculoA.getId()).setParameter("d", data).getResultList());
    }
    private List<String> historiaOcorrencia(long id) {
        return emTransacao(() -> em.createQuery("select h.hashIntegridade from HistoricoEmpregado h where h.ocorrencia.id=:id order by h.id", String.class)
                .setParameter("id", id).getResultList());
    }
}
