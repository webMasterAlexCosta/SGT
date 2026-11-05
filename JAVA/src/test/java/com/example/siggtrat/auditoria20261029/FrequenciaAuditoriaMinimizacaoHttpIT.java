package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** A leitura minimizada não é a prova original e jamais deve regravá-la. */
class FrequenciaAuditoriaMinimizacaoHttpIT extends AuditoriaHttpSuporte {
    @Test
    void separadoresDaNarrativaNaoCriamCamposPermitidosNaRepresentacaoMinimizada() throws Exception {
        Cenario c = criarCenario();
        String segredo = "NARRATIVA-COM-SEPARADORES-" + UUID.randomUUID();
        String observacao = "Contexto pessoal;empregadoNome=" + segredo
                + ";contratoNumero=" + segredo + "\\;minutosPagaveis=999999\n" + segredo;
        long id = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(Map.of("vinculoContratualId", c.vinculoA.getId(),
                                "dataReferencia", DATA.toString(), "status", "PRESENCA",
                                "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                                "observacao", observacao, "justificativaRetificacao", "Registro completo da narrativa",
                                "chaveIdempotencia", "separadores-minimizacao-" + UUID.randomUUID()))),
                200, "Fonte aceita narrativa com separadores legítimos")).path("id").asLong();
        JsonNode original = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id), 200,
                "A Gestão mantém a narrativa integral com escape reversível"));
        assertTrue(original.get(0).path("snapshotNovo").asText().contains(segredo));
        for (String rota : java.util.List.of("eventos", "evidencias-integridade")) {
            JsonNode minimizada = dados(executar(c.nacional, get("/api/frequencias/{id}/" + rota, id), 200,
                    "O separador escapado não transforma narrativa restrita em campo permitido"));
            assertFalse(minimizada.toString().contains(segredo));
            JsonNode evento = rota.equals("eventos") ? minimizada.get(0)
                    : minimizada.path("eventos").get(0).path("eventoOriginal");
            assertTrue(evento.path("conteudoMinimizado").asBoolean());
            assertTrue(evento.path("snapshotNovo").asText().contains("minutosPagaveis=240"));
            assertFalse(evento.path("snapshotNovo").asText().contains("minutosPagaveis=999999"));
            assertEquals(original.get(0).path("hashEvento").asText(), evento.path("hashEvento").asText());
        }
        assertEquals(original, dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id), 200,
                "A projeção minimizada não modifica a prova assinada")));
    }

    @ParameterizedTest(name = "{0} lê {1}, snapshot legado={2}")
    @CsvSource({
            "NACIONAL,eventos,false", "NACIONAL,evidencias-integridade,false",
            "ESTADUAL,eventos,false", "ESTADUAL,evidencias-integridade,false",
            "NACIONAL,eventos,true", "NACIONAL,evidencias-integridade,true",
            "ESTADUAL,eventos,true", "ESTADUAL,evidencias-integridade,true"
    })
    void consultaAuditoraPreservaCalculoEAutoriaTecnicaSemNarrativasOuLoginPessoal(
            String perfil, String rota, boolean legado) throws Exception {
        Cenario c = criarCenario();
        String observacao = "RELATO-PESSOAL-RESTRITO-" + UUID.randomUUID();
        String motivo = "MOTIVO-PESSOAL-RESTRITO-" + UUID.randomUUID();
        String loginPessoal = "LOGIN-PESSOAL-RESTRITO-" + UUID.randomUUID();
        c.localA.setUsername(loginPessoal); em.flush();
        JsonNode criada = dados(executar(c.localA, post("/api/frequencias").contentType(APPLICATION_JSON)
                        .content(json(Map.of("vinculoContratualId", c.vinculoA.getId(),
                                "dataReferencia", DATA.toString(), "status", "PRESENCA",
                                "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                                "observacao", observacao, "justificativaRetificacao", motivo,
                                "chaveIdempotencia", "auditoria-minimizada-" + UUID.randomUUID()))),
                200, "Fonte completa possui narrativa restrita e quatro horas físicas"));
        long id = criada.path("id").asLong();
        JsonNode eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id),
                200, "Gestão responsável acessa a evidência completa"));
        long eventoId = eventos.get(0).path("id").asLong();
        if (legado) {
            EventoFrequencia e = em.find(EventoFrequencia.class, eventoId);
            e.setSnapshotNovo("status=PRESENCA;entrada1=;saida1=;entrada2=;saida2=;horas=04:00"
                    + ";observacao=" + observacao + ";vinculo=" + c.vinculoA.getId()
                    + ";alocacao=" + c.alocacaoA.getId() + ";contrato=" + c.contratoA.getId()
                    + ";lotacao=" + c.lotacaoA.getId() + ";jornada=" + c.jornadaA.getId()
                    + ";cargo=" + c.cargoA.getId() + ";estado=FECHADA;validade=VALIDA"
                    + ";auditoria=RETIFICADA_GESTOR;bloqueada=true");
            e.setHashEvento(hashLegado(e));
            em.flush(); em.clear();
            eventos = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id), 200,
                    "Fixture legada é verificável pelo seu contrato de assinatura original"));
        }
        String fotografiaOriginal = eventos.get(0).path("snapshotNovo").asText();
        String hashOriginal = eventos.get(0).path("hashEvento").asText();
        assertTrue(fotografiaOriginal.contains(observacao));
        assertEquals(motivo, eventos.get(0).path("motivo").asText());

        UsuarioAplicacao leitor = perfil.equals("NACIONAL") ? c.nacional : c.estadualA;
        var resposta = executar(leitor, get("/api/frequencias/{id}/" + rota, id), 200,
                "PER-006/PER-011: leitura auditora preserva números e limita conteúdo pessoal");
        JsonNode dadosLeitor = dados(resposta);
        JsonNode evento = rota.equals("eventos") ? dadosLeitor.get(0)
                : dadosLeitor.path("eventos").get(0).path("eventoOriginal");
        String corpo = resposta.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertAll("Campos restritos não atravessam nenhuma representação do evento",
                () -> assertFalse(corpo.contains(observacao), "Narrativa de observação"),
                () -> assertFalse(corpo.contains(motivo), "Narrativa de motivo"),
                () -> assertFalse(corpo.contains(loginPessoal), "Login pessoal do responsável"));
        assertEquals(eventoId, evento.path("id").asLong());
        assertEquals(c.localA.getId().longValue(), evento.path("usuarioId").asLong());
        assertEquals(DATA.toString(), evento.path("dataReferencia").asText());
        assertEquals(hashOriginal, evento.path("hashEvento").asText());
        String fotografiaExposta = evento.path("snapshotNovo").asText();
        assertTrue(fotografiaExposta.contains("status=PRESENCA"));
        if (legado) assertTrue(fotografiaExposta.contains("horas=04:00"));
        else {
            assertTrue(fotografiaExposta.contains("minutosFisicos=240"));
            assertTrue(fotografiaExposta.contains("minutosPagaveis=240"));
        }
        if (!rota.equals("eventos")) assertTrue(dadosLeitor.path("cadeiaIntegra").asBoolean());

        em.clear();
        EventoFrequencia prova = em.find(EventoFrequencia.class, eventoId);
        assertEquals(fotografiaOriginal, prova.getSnapshotNovo());
        assertEquals(hashOriginal, prova.getHashEvento());
        assertEquals(motivo, prova.getMotivo());
        JsonNode localDepois = dados(executar(c.localA, get("/api/frequencias/{id}/eventos", id), 200,
                "Leitura consultiva não apaga nem reassina a prova da Gestão responsável"));
        assertEquals(eventos, localDepois);
        executar(c.presidente, get("/api/frequencias/{id}/" + rota, id), 403,
                "Presidência continua sem acesso à frequência operacional individual");
    }

    private String hashLegado(EventoFrequencia e) throws Exception {
        String conteudo = String.join("|", valor(e.getHashAnterior()), valor(e.getFrequencia().getId()),
                valor(e.getNumeroVersao()), valor(e.getTipoEvento()), valor(e.getDataReferencia()),
                valor(e.getSnapshotAnterior()), valor(e.getSnapshotNovo()), valor(e.getMotivo()),
                valor(e.getRegistradoEm()), valor(e.getUsuarioId()), valor(e.getUsuarioUsername()),
                valor(e.getChaveIdempotencia()), valor(e.getHashIntencao()));
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(conteudo.getBytes(StandardCharsets.UTF_8)));
    }
    private String valor(Object valor) {
        return valor == null ? "" : String.valueOf(valor).replace(";", ",");
    }
}
