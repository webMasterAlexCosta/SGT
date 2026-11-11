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
class OcorrenciaProvaPreservadaHttpIT extends ContratosHttpBase {
    @Test
    void retiradaDaOcorrenciaPreservaFatoEArquivoJaReferenciadosPelaCadeiaEEmissao() throws Exception {
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
                        .file(new MockMultipartFile("anexos", "prova-publicada.txt", "text/plain", conteudo))
                        .param("empregadoId", Long.toString(empregadoId)).param("motivo", "ATESTADO")
                        .param("dataOcorrencia", referencia.toString()).param("dias", "1")
                        .param("observacao", "Documento conferido que já produziu alteração oficial")
                        .param("numeroSei", "00001.000002/2026-01"),
                201, "Ocorrência e bytes do anexo confirmados em transação real"));
        long ocorrenciaId = criada.path("id").asLong();
        assertEquals(1, criada.path("anexos").size());
        long anexoId = criada.path("anexos").get(0).path("id").asLong();
        assertArrayEquals(conteudo, executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId),
                200, "Controle: anexo está acessível antes da retirada").getResponse().getContentAsByteArray());
        JsonNode fatoAtivo = consultar(c.localA, "/api/ocorrencias/" + ocorrenciaId,
                "Consulta por ID recupera o fato original antes do cancelamento");
        assertEquals(criada, fatoAtivo.path("ocorrencia"));
        assertFalse(fatoAtivo.path("cancelada").asBoolean());
        executar(c.localB, get("/api/ocorrencias/{id}", ocorrenciaId), 403,
                "Consulta histórica não amplia o escopo para outra Gestão");
        executar(c.localA, get("/api/ocorrencias/{id}", Long.MAX_VALUE), 404,
                "Consulta por ID inexistente preserva o contrato de recurso ausente");
        JsonNode cadeiaAntes = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "A fonte já é referenciada pela versão formal de frequência");
        assertEquals(2, cadeiaAntes.size());
        assertTrue(cadeiaAntes.get(1).path("motivo").asText().contains("ocorrência " + ocorrenciaId));
        assertTrue(cadeiaAntes.get(1).path("snapshotNovo").asText().contains("status=ATESTADO"));
        JsonNode emissao = enviar(c.localA, "/api/relatorios/administrativos/emissoes", mapa(
                "tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", referencia, "fim", referencia, "contratoIds", List.of(contrato.id()),
                "chaveIdempotencia", "emissao-prova-ocorrencia-" + UUID.randomUUID()),
                201, "A ocorrência integra uma emissão administrativa já publicada");
        long emissaoId = emissao.path("metadados").path("id").asLong();
        assertEquals(1, emissao.path("conteudo").path("totais").path("atestados").asInt());
        // A comparação de rollback usa a fotografia já relida após commit:
        // PostgreSQL armazena microssegundos, enquanto a resposta de criação ainda
        // pode conter os nanossegundos do Instant gerado na memória da JVM.
        JsonNode emissaoPersistidaAntes = consultar(c.localA,
                "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "Fotografia persistida anterior ao ensaio de rollback");
        assertEquals(emissao.path("conteudo"), emissaoPersistidaAntes.path("conteudo"));
        assertEquals(emissao.path("metadados").path("hashConteudo"),
                emissaoPersistidaAntes.path("metadados").path("hashConteudo"));
        long historicosAntesRetirada = emTransacao(() -> em.createQuery(
                        "select count(h) from HistoricoEmpregado h where h.empregado.id = :id", Long.class)
                .setParameter("id", empregadoId).getSingleResult());

        // O comando participa da transação externa: nenhuma retirada pode sobreviver ao rollback.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            try {
                executar(c.localA, delete("/api/ocorrencias/{id}", ocorrenciaId), 204,
                        "Retirada executada dentro da transação que será revertida");
            } catch (Exception e) {
                throw new AssertionError("A retirada de controle deveria ter sido processada", e);
            }
            assertNotNull(em.find(Ocorrencia.class, ocorrenciaId).getCanceladoEm());
            assertEquals("PRESENCA", em.find(Frequencia.class, frequenciaId).getStatus());
            status.setRollbackOnly();
        });
        assertEquals(fatoAtivo, consultar(c.localA, "/api/ocorrencias/" + ocorrenciaId,
                "Rollback conserva o fato ativo sem autor ou data de cancelamento"));
        assertEquals(cadeiaAntes, consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Rollback não publica uma nova versão da frequência"));
        assertEquals(emissaoPersistidaAntes, consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "Rollback conserva a emissão e seus metadados anteriores"));
        assertEquals(historicosAntesRetirada, emTransacao(() -> em.createQuery(
                        "select count(h) from HistoricoEmpregado h where h.empregado.id = :id", Long.class)
                .setParameter("id", empregadoId).getSingleResult()).longValue(),
                "Rollback não publica cancelamento no histórico assinado");
        assertArrayEquals(conteudo, executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId),
                200, "Rollback não elimina os bytes já confirmados").getResponse().getContentAsByteArray());

        executar(c.localA, delete("/api/ocorrencias/{id}", ocorrenciaId), 204,
                "Retirada autorizada do efeito operacional preserva a prova publicada");
        emTransacao(() -> {
            Frequencia restaurada = em.find(Frequencia.class, frequenciaId);
            assertEquals("PRESENCA", restaurada.getStatus());
            assertEquals(240, restaurada.getMinutosFisicos());
            return null;
        });
        JsonNode cadeiaDepois = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "A retirada acrescenta uma versão sem modificar a referência anterior");
        assertEquals(3, cadeiaDepois.size());
        assertEquals(cadeiaAntes.get(0), cadeiaDepois.get(0));
        assertEquals(cadeiaAntes.get(1), cadeiaDepois.get(1));
        JsonNode historica = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "A emissão superada conserva o atestado documental originalmente apurado");
        assertEquals(emissao.path("conteudo"), historica.path("conteudo"));
        assertEquals(emissao.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo"));

        assertAll("Fonte e arquivo citados pela prova histórica permanecem recuperáveis após o commit",
                () -> emTransacao(() -> {
                    Ocorrencia preservada = em.find(Ocorrencia.class, ocorrenciaId);
                    assertNotNull(preservada, "Fato publicado não pode desaparecer fisicamente");
                    assertEquals("ATESTADO", preservada.getMotivo());
                    assertEquals(referencia, preservada.getDataOcorrencia());
                    assertNotNull(preservada.getCanceladoEm());
                    assertEquals(c.localA.getId(), preservada.getCanceladoPorId());
                    assertNotNull(em.find(AnexoOcorrencia.class, anexoId), "Metadados do documento original são preservados");
                    return null;
                }),
                () -> assertArrayEquals(conteudo,
                        executar(c.localA, get("/api/ocorrencias/anexos/{id}", anexoId), 200,
                                "DOC-007 documento de fato publicado continua auditável após retirada do efeito")
                                .getResponse().getContentAsByteArray()));

        JsonNode fatoCancelado = consultar(c.localA, "/api/ocorrencias/" + ocorrenciaId,
                "Consulta histórica expõe a fonte original e o cancelamento confirmado");
        assertEquals(criada, fatoCancelado.path("ocorrencia"));
        assertTrue(fatoCancelado.path("cancelada").asBoolean());
        assertTrue(fatoCancelado.hasNonNull("canceladoEm"));
        assertEquals(c.localA.getId().longValue(), fatoCancelado.path("canceladoPorId").asLong());
        assertEquals(c.localA.getUsername(), fatoCancelado.path("canceladoPorUsername").asText());
        JsonNode operacionais = dados(executar(c.localA, get("/api/ocorrencias")
                        .param("empregadoId", Long.toString(empregadoId)).param("data", referencia.toString()),
                200, "A fonte cancelada deixa de compor a listagem operacional"));
        assertEquals(0, operacionais.path("totalElements").asInt());
        assertTrue(operacionais.path("content").isEmpty());
        executar(c.localB, get("/api/ocorrencias/{id}", ocorrenciaId), 403,
                "Preservar o fato cancelado não concede consulta à outra Gestão");
        executar(c.localB, get("/api/ocorrencias/anexos/{id}", anexoId), 403,
                "Preservar os bytes não concede download à outra Gestão");
        executar(c.localB, delete("/api/ocorrencias/{id}", ocorrenciaId), 403,
                "Repetição continua exigindo autorização antes de reconhecer o cancelamento");

        long historicosAntesRepeticao = emTransacao(() -> em.createQuery(
                        "select count(h) from HistoricoEmpregado h where h.empregado.id = :id", Long.class)
                .setParameter("id", empregadoId).getSingleResult());
        executar(c.localA, delete("/api/ocorrencias/{id}", ocorrenciaId), 204,
                "Reenvio autorizado confirma a retirada sem publicar outro fato");
        assertEquals(fatoCancelado, consultar(c.localA, "/api/ocorrencias/" + ocorrenciaId,
                "Reenvio preserva autor, data e motivo do primeiro cancelamento"));
        assertEquals(cadeiaDepois, consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Reenvio não duplica a versão de frequência"));
        assertEquals(historicosAntesRepeticao, emTransacao(() -> em.createQuery(
                        "select count(h) from HistoricoEmpregado h where h.empregado.id = :id", Long.class)
                .setParameter("id", empregadoId).getSingleResult()).longValue(),
                "Reenvio não duplica o histórico assinado do empregado");
    }
}
