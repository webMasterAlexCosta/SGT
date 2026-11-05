package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Regressões de preservação histórica e coerência dos indicadores. */
class FrequenciaHistoricoIndicadoresAuditoriaHttpIT extends AuditoriaHttpSuporte {
    @Test
    void retificacaoNovaMantemVerificavelEventoAssinadoNoFormatoLegado() throws Exception {
        Cenario c = criarCenario();
        Frequencia f = frequencia(c, false, DATA, "PRESENCA");
        f.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
        f.setHorasTrabalhadas("08:00"); f.setHorarioSaida("05:00");
        f.setMinutosFisicos(420); f.setMinutosNoturnosReais(420);
        f.setMinutosAdicionais(0); f.setMinutosPagaveis(480);
        f.setCalculoVersao("CLT-NOTURNO-3.0");
        em.flush();
        String fotografiaLegada = "status=PRESENCA;entrada1=;saida1=;entrada2=;saida2=;horas=08:00";
        Instant registro = Instant.parse("2026-06-10T13:00:00Z");
        String motivo = "Registro legado documental";
        // Fixture assinada pelo contrato legado, sem chamar o gerador novo.
        String conteudoLegado = String.join("|", "", f.getId().toString(), "1", "REGISTRO",
                DATA.toString(), "", fotografiaLegada.replace(";", ","), motivo, registro.toString(),
                c.localA.getId().toString(), c.localA.getUsername(), "", "");
        String hashLegado = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(conteudoLegado.getBytes(StandardCharsets.UTF_8)));
        EventoFrequencia evento = new EventoFrequencia();
        evento.setFrequencia(f); evento.setNumeroVersao(1); evento.setTipoEvento(TipoEventoFrequencia.REGISTRO);
        evento.setDataReferencia(DATA); evento.setSnapshotNovo(fotografiaLegada); evento.setMotivo(motivo);
        evento.setRegistradoEm(registro); evento.setUsuarioId(c.localA.getId()); evento.setUsuarioUsername(c.localA.getUsername());
        evento.setHashEvento(hashLegado); em.persist(evento); em.flush(); em.clear();
        executar(c.localA, get("/api/frequencias/{id}/eventos", f.getId()), 200,
                "O leitor continua verificando a assinatura legada original");
        salvarManual(c, "08:00", "17:00");
        em.flush(); em.clear();
        JsonNode cadeia = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", f.getId()), 200,
                "Cadeia mista conserva a assinatura anterior e valida o evento com memória completa"));
        assertEquals(2, cadeia.size());
        assertEquals(fotografiaLegada, cadeia.get(0).path("snapshotNovo").asText());
        assertEquals(hashLegado, cadeia.get(0).path("hashEvento").asText());
        assertEquals(hashLegado, cadeia.get(1).path("hashAnterior").asText());
        assertTrue(cadeia.get(1).path("snapshotNovo").asText().contains("480"));
    }

    @Test
    void retificacaoManualDeNoiteParaDiaPreservaAMemoriaAnteriorNaCadeia() throws Exception {
        Cenario c = criarCenario();
        JsonNode original = salvarManual(c, "07:00", "05:00");
        long id = original.path("id").asLong();
        Frequencia primeira = em.find(Frequencia.class, id);
        assertEquals(420, primeira.getMinutosFisicos());
        assertEquals(420, primeira.getMinutosNoturnosReais());
        assertEquals(480, primeira.getMinutosPagaveis());
        JsonNode eventosAntes = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "Evidência anterior à correção da jornada manual"));
        assertEquals(1, eventosAntes.size());
        String fotografiaOriginal = eventosAntes.get(0).path("snapshotNovo").asText();
        String hashOriginal = eventosAntes.get(0).path("hashEvento").asText();

        JsonNode corrigida = salvarManual(c, "08:00", "17:00");
        assertEquals(id, corrigida.path("id").asLong());
        Frequencia segunda = em.find(Frequencia.class, id);
        assertEquals(480, segunda.getMinutosFisicos());
        assertEquals(0, segunda.getMinutosNoturnosReais());
        assertEquals(480, segunda.getMinutosPagaveis());
        assertEquals("08:00", original.path("horasTrabalhadas").asText());
        assertEquals("08:00", corrigida.path("horasTrabalhadas").asText());
        em.flush(); em.clear();

        JsonNode cadeia = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "AUD-001/002: cadeia íntegra deve explicar a mudança física e noturna"));
        assertEquals(2, cadeia.size());
        assertEquals(fotografiaOriginal, cadeia.get(0).path("snapshotNovo").asText(),
                "A versão original não deve ser reescrita nem reassinada.");
        assertEquals(hashOriginal, cadeia.get(0).path("hashEvento").asText());
        String antes = cadeia.get(1).path("snapshotAnterior").asText();
        String depois = cadeia.get(1).path("snapshotNovo").asText();
        assertAll("Mudança real da memória não pode resultar em fotografias idênticas",
                () -> assertNotEquals(antes, depois),
                () -> assertTrue(antes.contains("05:00"), "Fotografia anterior deve manter a saída manual 05:00"),
                () -> assertTrue(antes.contains("420"), "Fotografia anterior deve manter os 420 minutos físicos/noturnos"),
                () -> assertTrue(depois.contains("17:00"), "Fotografia nova deve manter a saída manual 17:00"),
                () -> assertTrue(depois.contains("480"), "Fotografia nova deve manter os 480 minutos físicos"));
    }

    @Test
    void consolidadoSemFaixaOperacionalMantemAvisoDaFrequenciaInvalidada() throws Exception {
        Cenario c = criarCenario();
        salvar(c, DATA, "FALTA");
        String base = "/api/contratos/" + c.contratoA.getId();
        Map<String,Object> acao = new LinkedHashMap<>(Map.of("dataEfetiva", DATA.toString(),
                "motivo", "Inativação retroativa documentada sobre a frequência do dia"));
        JsonNode previa = dados(executar(c.localA, post(base + "/previas/inativar")
                        .contentType(APPLICATION_JSON).content(json(acao)),
                200, "Prévia real do impacto retroativo"));
        assertFalse(previa.path("tokenPrevia").asText().isBlank());
        acao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        acao.put("confirmarImpacto", true);
        executar(c.localA, post(base + "/inativar").contentType(APPLICATION_JSON).content(json(acao)),
                200, "Inativação confirmada gera revisão da frequência sem apagar fonte");
        JsonNode pendencias = dados(executar(c.localA, get("/api/pendencias/resumo")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "Controle: existe pendência real após a retirada de cobertura"));
        long quantidade = pendencias.path("retificacoesFrequencia").asLong();
        assertTrue(quantidade > 0);
        JsonNode consolidado = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "FRE-011/REL-004: relatório vazio continua sujeito a revisão"));
        JsonNode pagina = dados(executar(c.localA, get("/api/relatorios/frequencia-consolidada/paginada")
                        .param("inicio", DATA.toString()).param("fim", DATA.toString())
                        .param("contratoIds", c.contratoA.getId().toString()),
                200, "Controle: a página consulta o mesmo recorte e pendências"));
        assertEquals(0, consolidado.path("registros").size());
        assertTrue(pagina.path("revisao").path("sujeitoARevisao").asBoolean());
        assertEquals(quantidade, pagina.path("revisao").path("quantidadePendencias").asLong());
        assertAll(() -> assertTrue(consolidado.path("revisao").path("sujeitoARevisao").asBoolean()),
                () -> assertEquals(quantidade, consolidado.path("revisao").path("quantidadePendencias").asLong()));
    }

    @Test
    void duasAlocacoesDoMesmoColaboradorNoMesNaoDuplicamPopulacaoDoResumo() throws Exception {
        Cenario c = criarCenario();
        c.composicaoA.setEfetivoPrevisto(3);
        Lotacao segundaLotacao = new Lotacao("Segunda lotação histórica " + UUID.randomUUID());
        segundaLotacao.setGestaoLocal(c.gestaoA);
        segundaLotacao.setMunicipio(c.lotacaoA.getMunicipio());
        segundaLotacao.getTurnos().add(c.jornadaA);
        em.persist(segundaLotacao);
        ComposicaoContratual segundaComposicao = composicao(c.contratoA, segundaLotacao, c.jornadaA, c.cargoA, 2);
        c.alocacaoA.setDataFim(DATA);
        AlocacaoVinculo segundaAlocacao = new AlocacaoVinculo();
        segundaAlocacao.setVinculo(c.vinculoA); segundaAlocacao.setComposicao(segundaComposicao);
        segundaAlocacao.setLotacao(segundaLotacao); segundaAlocacao.setJornada(c.jornadaA);
        segundaAlocacao.setCargo(c.cargoA); segundaAlocacao.setDataInicio(DATA.plusDays(1));
        segundaAlocacao.setDataFim(FIM);
        em.persist(segundaAlocacao); em.flush();
        salvar(c, DATA, "FALTA");
        salvar(c, DATA.plusDays(1), "FALTA");

        JsonNode pagina = dados(executar(c.localA, get("/api/relatorios/absenteismo/paginado")
                        .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString()),
                200, "HIS-002: mês mostra as duas faixas de uma única Pessoa"));
        JsonNode resumo = dados(executar(c.localA, get("/api/relatorios/absenteismo/resumo")
                        .param("mes", "6").param("ano", "2026").param("contratoId", c.contratoA.getId().toString()),
                200, "REL-001: indicador de população conta pessoas, mesmo com mudança interna"));
        assertEquals(1, pagina.path("totalColaboradores").asLong());
        assertEquals(2, pagina.path("registros").size(), "As duas faixas precisam existir para exercitar o defeito.");
        assertEquals(2, resumo.path("faltas").asLong());
        assertEquals(1, resumo.path("totalColaboradores").asLong(),
                "Duas alocações temporais não transformam uma Pessoa em dois colaboradores.");
    }

    private JsonNode salvar(Cenario c, LocalDate data, String status) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(Map.of("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data.toString(),
                                "status", status, "justificativaRetificacao", "Conferência documentada da frequência histórica",
                                "chaveIdempotencia", "fonte-indicadores-" + UUID.randomUUID()))),
                200, "Fonte canônica para testar indicador histórico"));
    }

    private JsonNode salvarManual(Cenario c, String duracao, String saida) throws Exception {
        return dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(Map.of("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA.toString(),
                                "status", "PRESENCA", "horasTrabalhadas", duracao, "horarioSaida", saida,
                                "observacao", "Mesma observação: mudou somente o horário e a memória de cálculo",
                                "justificativaRetificacao", "Correção documental da jornada efetivamente realizada",
                                "chaveIdempotencia", "fotografia-calculo-" + UUID.randomUUID()))),
                200, "Retificação manual com conteúdo físico diferente e mesmo total pagável"));
    }
}
