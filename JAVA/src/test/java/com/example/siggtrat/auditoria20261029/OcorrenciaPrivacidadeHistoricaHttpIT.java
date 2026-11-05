package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AnexoOcorrencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Ocorrencia;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** GOV-003/DOC-007: retirar o efeito operacional não elimina a fonte já publicada. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaPrivacidadeHistoricaHttpIT extends ContratosHttpBase {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"NACIONAL,false", "ESTADUAL,false", "ADMIN,false", "NACIONAL,true", "ESTADUAL,true", "ADMIN,true"})
    void leituraAuditoraNaoExpoeNarrativaOuMetadadosPessoais(String perfil, boolean cancelar) throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception e) { throw new IllegalStateException("Falha na publicação HTTP da fixture", e); }
        });
        LocalDate referencia = RelogioSistema.hoje().with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
        long vinculoId = enviar(c.localA, "/api/vinculos-contratuais", mapa(
                "pessoa", mapa("nome", "Pessoa da prova de ocorrência", "cpf", cpf(Math.toIntExact(970000000L + contrato.id()))),
                "contratoId", contrato.id(), "composicaoId", contrato.composicao1(), "dataInicio", referencia.minusDays(1)),
                201, "Vínculo real em contrato efetivamente publicado").path("id").asLong();
        JsonNode frequencia = enviar(c.localA, "/api/frequencias", mapa(
                "vinculoContratualId", vinculoId, "dataReferencia", referencia,
                "status", "PRESENCA", "horasTrabalhadas", "04:00", "horarioSaida", "12:00",
                "justificativaRetificacao", "Registro anterior à fonte documental de ocorrência",
                "chaveIdempotencia", "frequencia-prova-" + UUID.randomUUID()),
                200, "Presença confirmada antes da classificação administrativa");
        long frequenciaId = frequencia.path("id").asLong();
        long empregadoId = frequencia.path("empregado").path("id").asLong();
        byte[] conteudo = ("Evidência íntegra que fundamentou o fato publicado " + UUID.randomUUID())
                .getBytes(StandardCharsets.UTF_8);
        JsonNode criada = dados(executar(c.localA, multipart("/api/ocorrencias")
                        .file(new MockMultipartFile("anexos", "ANEXO-PESSOAL-RESTRITO.txt", "text/plain", conteudo))
                        .param("empregadoId", Long.toString(empregadoId)).param("motivo", "ATESTADO")
                        .param("dataOcorrencia", referencia.toString()).param("dias", "1")
                        .param("observacao", "NARRATIVA-PESSOAL-RESTRITA")
                        .param("numeroSei", "00001.000002/2026-01"),
                201, "Ocorrência e bytes do anexo confirmados em transação real"));
        long ocorrenciaId = criada.path("id").asLong();
        assertEquals(1, criada.path("anexos").size());
        long anexoId = criada.path("anexos").get(0).path("id").asLong();
        assertArrayEquals(conteudo, executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId),
                200, "Controle: anexo está acessível antes da retirada").getResponse().getContentAsByteArray());
        if (cancelar) executar(c.localA, delete("/api/ocorrencias/{id}", ocorrenciaId), 204, "Cancelar preservando fonte");
        var leitor = switch (perfil) { case "NACIONAL" -> c.nacional; case "ESTADUAL" -> c.estadualA; default -> c.admin; };
        JsonNode restrita = consultar(leitor, "/api/ocorrencias/" + ocorrenciaId, "Leitura auditora sem concessão excepcional");
        String resposta = restrita.toString();
        assertAll(
                () -> assertFalse(resposta.contains("NARRATIVA-PESSOAL-RESTRITA"), "Narrativa livre exige nível pessoal completo"),
                () -> assertFalse(resposta.contains("ANEXO-PESSOAL-RESTRITO"), "Nome original do anexo pode conter dado pessoal"),
                () -> assertFalse(resposta.contains("00001.000002/2026-01"), "Referência documental pessoal não é campo gerencial"),
                () -> assertFalse(resposta.contains(c.localA.getUsername()), "Login do autor não deve ampliar leitura auditora"));
        assertEquals(ocorrenciaId, restrita.path("ocorrencia").path("id").asLong());
        assertEquals(empregadoId, restrita.path("ocorrencia").path("empregado").path("id").asLong());
        assertEquals(referencia.toString(), restrita.path("ocorrencia").path("dataOcorrencia").asText());
        assertEquals("ATESTADO", restrita.path("ocorrencia").path("motivo").asText());
        assertEquals(1, restrita.path("ocorrencia").path("quantidadeDias").asInt());
        assertEquals(cancelar, restrita.path("cancelada").asBoolean());
        if (cancelar) {
            assertTrue(restrita.hasNonNull("canceladoEm"));
            assertEquals(c.localA.getId().longValue(), restrita.path("canceladoPorId").asLong());
        }
        JsonNode completa = consultar(c.localA, "/api/ocorrencias/" + ocorrenciaId, "Controle: Local autorizado preserva prova integral");
        assertEquals(criada, completa.path("ocorrencia"), "Minimização do auditor não altera a prova integral do Gestor Local");
    }
}
