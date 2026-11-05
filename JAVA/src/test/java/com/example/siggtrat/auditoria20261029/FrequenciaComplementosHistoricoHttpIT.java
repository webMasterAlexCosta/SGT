package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.FrequenciaExcecao;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** GOV-003/TMP-003/004/005: fonte manual publicada e derivação técnica têm preservações distintas. */
class FrequenciaComplementosHistoricoHttpIT extends ContratosHttpBase {

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void exclusaoDoComplementoManualPreservaRaizEFotografiaDoFato(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso);
        Instant corteOriginal = Instant.now();
        executar(c.localA, delete(rota(recurso) + "/{id}", id).param("justificativa", "Cancelamento documental conferido"),
                204, "DELETE mantém o contrato HTTP e cancela a fonte manual sem apagar a prova");
        em.flush(); em.clear();
        if (recurso.equals("excecoes")) {
            FrequenciaExcecao original = em.find(FrequenciaExcecao.class, id);
            assertNotNull(original, "Uma exceção APROVADA é fato histórico e não pode ser removida fisicamente");
            assertEquals("CANCELADO", original.getStatus());
        } else {
            FrequenciaAdicional original = em.find(FrequenciaAdicional.class, id);
            assertNotNull(original, "Um adicional registrado manualmente é fonte histórica preservada");
            assertEquals("CANCELADO", original.getStatus());
        }
        JsonNode atual = dados(executar(c.localA, get(rota(recurso)).param("data", DATA.toString()), 200,
                "Fonte cancelada não aparece na relação corrente de complementos"));
        assertEquals(0, atual.path("content").size());
        JsonNode eventos = consultar(c.localA, rota(recurso) + "/" + id + "/eventos",
                "Criação e cancelamento são auditáveis depois do DELETE");
        assertEquals(2, eventos.size());
        assertEquals("CRIACAO", eventos.get(0).path("tipoOperacao").asText());
        assertEquals("CANCELAMENTO", eventos.get(1).path("tipoOperacao").asText());
        assertEquals("Cancelamento documental conferido", eventos.get(1).path("justificativa").asText());
        assertEquals(eventos.get(0).path("hashVersao").asText(), eventos.get(1).path("hashAnterior").asText());
        assertEquals(eventos.get(0).path("id").asLong(), eventos.get(1).path("versaoAnteriorId").asLong());
        JsonNode conhecido = consultar(c.localA, rota(recurso) + "/" + id + "/eventos?conhecidoAte=" + corteOriginal,
                "Corte de conhecimento anterior ao cancelamento mantém somente o fato original");
        assertEquals(1, conhecido.size());
        executar(c.localB, get(rota(recurso) + "/{id}/eventos", id), 404,
                "História preservada não é disponibilizada à Gestão alheia");
    }

    @ParameterizedTest
    @ValueSource(strings = {"excecoes", "adicionais"})
    void retificacaoDeRetificacaoPreservaTresFotografiasAutoriaEConhecimento(String recurso) throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, recurso);
        Instant corteOriginal = Instant.now();
        Map<String,Object> segunda = recurso.equals("excecoes")
                ? mapa("observacao", "Conferência documental segunda versão", "justificativa", "Segundo documento corrigiu a observação original")
                : mapa("minutos", 90, "justificativa", "Conferência documental segunda versão");
        executar(c.localA, put(rota(recurso) + "/{id}", id).contentType(APPLICATION_JSON).content(json(segunda)),
                200, "Primeira correção formal do complemento");
        Instant corteIntermediario = Instant.now();
        Map<String,Object> terceira = recurso.equals("excecoes")
                ? mapa("observacao", "Conferência documental terceira versão", "justificativa", "Terceiro documento corrigiu novamente a observação")
                : mapa("minutos", 120, "justificativa", "Conferência documental terceira versão");
        executar(c.localA, put(rota(recurso) + "/{id}", id).contentType(APPLICATION_JSON).content(json(terceira)),
                200, "Retificação de retificação mantém as duas provas anteriores");
        em.flush(); em.clear();
        JsonNode eventos = consultar(c.localA, rota(recurso) + "/" + id + "/eventos",
                "Histórico completo não substitui a versão anterior");
        assertEquals(3, eventos.size());
        for (int i = 0; i < 3; i++) {
            JsonNode e = eventos.get(i);
            assertEquals(i + 1, e.path("numeroVersao").asInt());
            assertEquals(i == 0 ? "CRIACAO" : "RETIFICACAO", e.path("tipoOperacao").asText());
            assertEquals(c.localA.getId().longValue(), e.path("usuarioId").asLong());
            assertFalse(e.path("registradoEm").asText().isBlank());
            assertEquals(DATA.toString(), e.path("dataEfetiva").asText());
            assertEquals(64, e.path("hashVersao").asText().length());
            assertFalse(e.path("motivoTecnico").asText().isBlank());
            assertEquals(c.empregadoA.getId().longValue(), e.path("fotografia").path("empregadoId").asLong());
            assertEquals(c.gestaoA.getId().longValue(), e.path("contextos").path(0).path("gestaoLocalId").asLong());
            assertEquals(c.vinculoA.getId().longValue(), e.path("contextos").path(0).path("vinculoId").asLong());
            if (i > 0) {
                assertEquals(eventos.get(i - 1).path("hashVersao").asText(), e.path("hashAnterior").asText());
                assertEquals(eventos.get(i - 1).path("id").asLong(), e.path("versaoAnteriorId").asLong());
            }
        }
        if (recurso.equals("excecoes")) {
            assertEquals("Compensação original documentada", eventos.get(0).path("fotografia").path("observacao").asText());
            assertEquals("Conferência documental segunda versão", eventos.get(1).path("fotografia").path("observacao").asText());
            assertEquals("Conferência documental terceira versão", eventos.get(2).path("fotografia").path("observacao").asText());
        } else {
            assertEquals(60, eventos.get(0).path("fotografia").path("minutos").asInt());
            assertEquals(90, eventos.get(1).path("fotografia").path("minutos").asInt());
            assertEquals(120, eventos.get(2).path("fotografia").path("minutos").asInt());
        }
        assertEquals(1, consultar(c.localA, rota(recurso) + "/" + id + "/eventos?conhecidoAte=" + corteOriginal,
                "Conhecimento original não inclui retificações posteriores").size());
        assertEquals(2, consultar(c.localA, rota(recurso) + "/" + id + "/eventos?conhecidoAte=" + corteIntermediario,
                "Conhecimento intermediário não inclui a terceira fotografia").size());
    }

    @Test
    void recalculoDaFrequenciaNaoRemoveNemSobrescreveAdicionalManualPublicado() throws Exception {
        Cenario c = criarCenario();
        long id = criar(c, "adicionais");
        registrarFrequencia(c, "PRESENCA");
        em.flush(); em.clear();
        FrequenciaAdicional manual = em.find(FrequenciaAdicional.class, id);
        assertNotNull(manual, "Reconciliação automática não pode apagar a fonte manual CALCULADO");
        assertEquals(60, manual.getMinutos());
        assertNull(manual.getFrequencia(), "A origem manual não é convertida silenciosamente em derivação");
        assertEquals(1, consultar(c.localA, rota("adicionais") + "/" + id + "/eventos",
                "Recálculo sem alteração da fonte manual não cria retificação fictícia").size());
    }

    @Test
    void adicionalDerivadoContinuaReconciliavelComProvaNaFrequenciaDeOrigem() throws Exception {
        Cenario c = criarCenario();
        enviar(c.localA, "/api/calendario-eventos", mapa("data", DATA, "tipo", "FERIADO",
                "abrangencia", "LOTACAO", "lotacaoId", c.lotacaoA.getId(), "ativo", true), 201,
                "Feriado gera condição efetiva para o adicional derivado");
        long frequenciaId = registrarFrequencia(c, "RT");
        FrequenciaAdicional derivado = em.createQuery("select a from FrequenciaAdicional a where a.empregado.id=:e "
                        + "and a.dataReferencia=:d and a.tipo='RT'", FrequenciaAdicional.class)
                .setParameter("e", c.empregadoA.getId()).setParameter("d", DATA).getSingleResult();
        long adicionalId = derivado.getId();
        assertEquals(frequenciaId, derivado.getFrequencia().getId());
        assertEquals(480, derivado.getMinutos());
        registrarFrequencia(c, "DESCANSO");
        em.flush(); em.clear();
        assertNull(em.find(FrequenciaAdicional.class, adicionalId),
                "Projeção puramente derivada pode desaparecer quando a frequência deixa de gerar RT");
        assertEquals(2, consultar(c.localA, "/api/frequencias/" + frequenciaId + "/eventos",
                "As duas versões da frequência continuam provando a mudança que recalculou RT").size());
    }

    private long criar(Cenario c, String recurso) throws Exception {
        Map<String,Object> corpo = recurso.equals("excecoes")
                ? mapa("empregadoId", c.empregadoA.getId(), "dataInicio", DATA, "dataFim", DATA,
                        "tipo", "FOLGA_REMUNERADA", "observacao", "Compensação original documentada")
                : mapa("empregadoId", c.empregadoA.getId(), "dataReferencia", DATA, "tipo", "RT", "minutos", 60,
                        "quantidade", 1, "justificativa", "Serviço original documentado");
        return enviar(c.localA, rota(recurso), corpo, 201, "Publicação manual da fonte do complemento").path("id").asLong();
    }

    private long registrarFrequencia(Cenario c, String status) throws Exception {
        Map<String,Object> corpo = mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", DATA,
                "status", status, "justificativaRetificacao", "Registro gerencial documentado para reconciliação",
                "chaveIdempotencia", "complemento-frequencia-" + UUID.randomUUID());
        if (status.equals("PRESENCA") || status.equals("RT")) {
            corpo.put("horasTrabalhadas", "08:00"); corpo.put("horarioSaida", "17:00");
        }
        return enviar(c.localA, "/api/frequencias", corpo, 200, "Lançamento real recalcula adicional derivado").path("id").asLong();
    }

    private String rota(String recurso) { return "/api/frequencias-" + recurso; }
}
