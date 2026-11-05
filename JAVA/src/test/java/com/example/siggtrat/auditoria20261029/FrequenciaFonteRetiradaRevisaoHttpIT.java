package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Frequencia;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** FRE-009/011/REL-006: retirar fonte de FOLGA fechada exige revisao, sem reescrever a jornada. */
class FrequenciaFonteRetiradaRevisaoHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @CsvSource({"EXCECAO,DELETE", "EXCECAO,MOVER", "CALENDARIO,DELETE", "CALENDARIO,MOVER"})
    void retirarFonteDeFolgaFechadaPreservaFrequenciaESinalizaSomenteRecorteAfetado(String fonte, String acao) throws Exception {
        Cenario c = criarCenario();
        String rota = fonte.equals("EXCECAO") ? "/api/frequencias-excecoes" : "/api/calendario-eventos";
        Map<String, Object> pedidoFonte = fonte.equals("EXCECAO")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                        "tipo", "FOLGA_REMUNERADA", "observacao", "Documento sustenta folga em dia util")
                : mapa("data", DATA, "tipo", "FERIADO", "abrangencia", "LOTACAO",
                        "lotacaoId", c.lotacaoA.getId(), "ativo", true);
        long fonteId = enviar(c.localA, rota, pedidoFonte, 201, "Fonte valida permite FOLGA na quarta-feira util")
                .path("id").asLong();
        long frequenciaId = registrar(c, DATA, "FOLGA");
        long controleId = registrar(c, DATA.plusDays(2), "FALTA");
        em.flush(); em.clear();
        Frequencia original = em.find(Frequencia.class, frequenciaId);
        assertEquals("FECHADA", original.getEstadoNegocial().name());
        assertEquals("VALIDA", original.getValidadeOperacional().name());
        Long autorOriginal = original.getAutorOriginalId();
        JsonNode cadeiaOriginal = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Prova original da folga fechada antes da retificacao da fonte");
        JsonNode emitida = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro(c, DATA), 201,
                "Emissao original sem pendencias inclui a folga sustentada pela fonte");
        JsonNode controle = enviar(c.localA, "/api/relatorios/administrativos/emissoes", filtro(c, DATA.plusDays(2)), 201,
                "Emissao de controle fora das datas da fonte antiga e da fonte movida");
        assertFalse(emitida.path("metadados").path("sujeitoARevisao").asBoolean());
        long emissaoId = emitida.path("metadados").path("id").asLong();
        long emissaoControleId = controle.path("metadados").path("id").asLong();

        if (acao.equals("DELETE")) {
            executar(c.localA, delete(rota + "/{id}", fonteId).param("justificativa", "Fonte revogada apos conferencia documental"),
                    204, "Cancelamento da fonte preserva a prova e identifica suas dependencias");
        } else {
            Map<String, Object> mudanca = fonte.equals("EXCECAO")
                    ? mapa("dataInicio", DATA.plusDays(1), "dataFim", DATA.plusDays(1), "observacao", "Correcao da data real da folga",
                            "justificativa", "Documento corrigiu a data efetiva da folga")
                    : mapa("data", DATA.plusDays(1), "tipo", "FERIADO", "abrangencia", "LOTACAO",
                            "lotacaoId", c.lotacaoA.getId(), "ativo", true, "justificativa", "Correcao da data real do feriado");
            executar(c.localA, put(rota + "/{id}", fonteId).contentType(APPLICATION_JSON).content(json(mudanca)),
                    200, "Mover a fonte revalida o dia que perdeu a sustentacao sem alterar a jornada fechada");
        }
        em.flush(); em.clear();
        Frequencia afetada = em.find(Frequencia.class, frequenciaId);
        Frequencia naoAfetada = em.find(Frequencia.class, controleId);
        JsonNode historica = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + emissaoId,
                "Emissao anterior segue disponivel como prova");
        JsonNode historicaControle = consultar(c.localA, "/api/relatorios/administrativos/emissoes/" + emissaoControleId,
                "Recorte alheio nao deve ser invalidado");
        JsonNode atual = enviar(c.localA, "/api/relatorios/administrativos/previa", filtro(c, DATA), 200,
                "Visao oficial atual informa que a folga perdeu sustentacao e depende de retificacao");
        JsonNode cadeiaDepois = consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "Impacto da fonte gera prova sem substituir o lancamento original");

        assertAll(
                () -> assertEquals("FOLGA", afetada.getStatus(), "A fonte nao transforma silenciosamente folga em falta"),
                () -> assertEquals("FECHADA", afetada.getEstadoNegocial().name()),
                () -> assertEquals(autorOriginal, afetada.getAutorOriginalId()),
                () -> assertNotEquals("VALIDA", afetada.getValidadeOperacional().name()),
                () -> assertEquals("SUJEITA_REVISAO", afetada.getEstadoAuditoria().name()),
                () -> assertTrue(Boolean.TRUE.equals(afetada.getBloqueadaEdicaoOperacional())),
                () -> assertTrue(afetada.getMotivoRevisao() != null && !afetada.getMotivoRevisao().isBlank()),
                () -> assertEquals("VALIDA", naoAfetada.getValidadeOperacional().name()),
                () -> assertEquals("SUPERADA", historica.path("metadados").path("estado").asText()),
                () -> assertEquals(emitida.path("conteudo"), historica.path("conteudo")),
                () -> assertEquals(emitida.path("metadados").path("hashConteudo"), historica.path("metadados").path("hashConteudo")),
                () -> assertEquals("VIGENTE", historicaControle.path("metadados").path("estado").asText()),
                () -> assertTrue(atual.path("metadados").path("sujeitoARevisao").asBoolean()),
                () -> assertEquals(1, atual.path("metadados").path("quantidadePendencias").asLong()),
                () -> assertTrue(cadeiaDepois.size() > cadeiaOriginal.size()),
                () -> assertEquals(cadeiaOriginal.get(0), cadeiaDepois.get(0)));
        assertTrue(cadeiaDepois.toString().contains("IMPACTO_TEMPORAL"));
        executar(c.operacionalA, post("/api/frequencias").contentType(APPLICATION_JSON).content(json(mapa(
                        "vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA, "status", "FALTA"))),
                403, "Operacional nao regulariza silenciosamente frequencia fechada afetada por fonte retroativa");
    }

    private long registrar(Cenario c, LocalDate data, String status) throws Exception {
        return enviar(c.localA, "/api/frequencias", mapa("vinculoContratualId", c.vinculoA.getId(),
                "dataReferencia", data, "status", status, "justificativaRetificacao", "Registro gerencial documental da jornada",
                "chaveIdempotencia", "fonte-folga-" + UUID.randomUUID()), 200, "Lancamento real de frequencia fechada")
                .path("id").asLong();
    }

    private Map<String, Object> filtro(Cenario c, LocalDate data) {
        return mapa("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO", "inicio", data, "fim", data,
                "contratoIds", List.of(c.contratoA.getId()), "chaveIdempotencia", "emissao-fonte-folga-" + UUID.randomUUID());
    }
}
