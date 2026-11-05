package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.GestaoContratoTemporalServico;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Synthetic algorithm proof only. Never substitutes authentic HIS-001/HIS-006 documentary gates. */
class CriteriosCHistoricoHttpIT extends CriteriosCHttpBase {
    @Autowired GestaoContratoTemporalServico gestaoTemporalC;
    @Autowired HistoricoIntegridadeServico integridadeC;

    private JsonNode pesquisa(PessoaPreparada x, String campo, LocalDate de, LocalDate ate) throws Exception {
        return dados(executar(x.c().localA, get("/api/vinculos-contratuais")
                .param("pessoaId", Long.toString(x.pessoa())).param(campo + "De", de.toString())
                .param(campo + "Ate", ate.toString()).param("size", "5"), 200,
                "HIS-003 filtro " + campo + " com limites efetivos " + de + ".." + ate));
    }

    private void contemSomente(JsonNode page, long id) {
        assertEquals(1, page.path("totalElements").asLong()); assertEquals(1, page.path("content").size());
        assertEquals(id, page.path("content").get(0).path("id").asLong());
    }

    private void vazio(JsonNode page) {
        assertEquals(0, page.path("totalElements").asLong()); assertTrue(page.path("content").isEmpty());
    }

    private long motivo(PessoaPreparada x, LocalDate inicio, LocalDate fim) {
        return emTransacao(() -> {
            MotivoDesligamento m = new MotivoDesligamento();
            m.setGestaoLocal(em.find(GestaoLocal.class, x.c().gestaoA.getId()));
            m.setDescricao("Causa sintética explícita " + x.vinculo()); m.setAtivo(true);
            m.setCategoria(CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL); em.persist(m); em.flush();
            EventoDisponibilidadeCatalogo d = new EventoDisponibilidadeCatalogo();
            d.setTipoCatalogo(TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO); d.setCatalogoId(m.getId());
            d.setDataEfetiva(inicio); d.setAtivo(true); d.setMotivo("Disponibilidade sintética exclusiva do ensaio C");
            em.persist(d); em.flush();
            FonteMotivoSintetica.declarar(em, gestaoTemporalC, x.contrato().id(), m.getId(), x.c().gestaoA.getId(),
                    x.c().localA, m.getDescricao(), CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL,
                    inicio, fim, Instant.now(), "SEI-SINTETICO-C-HIS003-" + x.vinculo());
            return m.getId();
        });
    }

    private Map<String, Object> terminar(LocalDate fim, long motivo) {
        return mapa("dataFim", fim, "motivoDesligamentoId", motivo,
                "justificativa", "Desligamento individual sintético com data efetiva documentada",
                "evidencia", "SEI-SINTETICO-C-HIS003", "autorizacao", "ATO-SINTETICO-C");
    }

    @Test
    void filtrosFimUsamTerminoEfetivoInclusivoERetificacaoSemConfundirRegistroTardio() throws Exception {
        PessoaPreparada x = pessoaPreparada(); LocalDate fim = RelogioSistema.hoje().minusDays(8);
        long causa = motivo(x, fim, fim.plusDays(1)); String rota = "/api/vinculos-contratuais/" + x.vinculo();
        vazio(pesquisa(x, "fim", fim.minusDays(1), fim.plusDays(1)));
        JsonNode desligado = enviar(x.c().localA, rota + "/desligar", terminar(fim, causa), 200,
                "HIS-003 controle de vínculo realmente desligado por comando final");
        assertEquals(fim.toString(), desligado.path("dataFim").asText());
        assertFalse(desligado.path("desligamentoRegistradoEm").asText().isBlank());
        contemSomente(pesquisa(x, "fim", fim, fim), x.vinculo());
        vazio(pesquisa(x, "fim", fim.minusDays(1), fim.minusDays(1)));
        vazio(pesquisa(x, "fim", fim.plusDays(1), fim.plusDays(1)));
        vazio(pesquisa(x, "fim", RelogioSistema.hoje(), RelogioSistema.hoje()));
        var req = terminar(fim.plusDays(1), causa);
        JsonNode previa = enviar(x.c().localA, rota + "/previas/retificacao-desligamento", req, 200,
                "Prévia legítima para corrigir o último dia");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        JsonNode corrigido = enviar(x.c().localA, rota + "/retificar-desligamento", req, 200,
                "Retificação confirmada por fonte explícita");
        assertEquals(fim.plusDays(1).toString(), corrigido.path("dataFim").asText());
        vazio(pesquisa(x, "fim", fim, fim));
        JsonNode incluido = pesquisa(x, "fim", fim.plusDays(1), fim.plusDays(1)); contemSomente(incluido, x.vinculo());
        contemSomente(pesquisa(x, "inicio", x.inicio(), x.inicio()), x.vinculo());
        vazio(pesquisa(x, "inicio", fim, fim.plusDays(1)));
        registrar("his003-fim-" + x.vinculo(), mapa("vinculo", x.vinculo(), "inicioReal", x.inicio(),
                "fimOriginal", fim, "fimRetificado", fim.plusDays(1), "controlePagina", incluido,
                "eventos", consultar(x.c().localA, rota + "/eventos", "Eventos originais/retificação preservados")));
    }

    @Test
    void alocacaoInternaNaoCriaAdmissaoOuDesligamentoNosFiltrosDeVinculo() throws Exception {
        PessoaPreparada x = pessoaPreparada(true); assertNotNull(x.contrato().composicao2());
        LocalDate mudanca = RelogioSistema.hoje().minusDays(10);
        String rota = "/api/vinculos-contratuais/" + x.vinculo();
        var req = mapa("dataEfetiva", mudanca, "composicaoId", x.contrato().composicao2(),
                "justificativa", "Mudança interna documentada sem novo vínculo",
                "evidencia", "SEI-SINTETICO-C-MUDANCA", "autorizacao", "ATO-SINTETICO-C");
        JsonNode previa = enviar(x.c().localA, rota + "/previas/alocacoes", req, 200, "Prévia de mudança interna");
        assertTrue(previa.path("incompatibilidades").isEmpty());
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("token").asText());
        JsonNode alocacao = enviar(x.c().localA, rota + "/alocacoes", req, 200, "Comando real de mudança interna");
        assertEquals(mudanca.toString(), alocacao.path("dataInicio").asText());
        JsonNode vinculo = consultar(x.c().localA, rota, "Vínculo relido depois da alocação interna");
        assertEquals(x.inicio().toString(), vinculo.path("dataInicio").asText()); assertTrue(vinculo.path("dataFim").isNull());
        contemSomente(pesquisa(x, "inicio", x.inicio(), x.inicio()), x.vinculo());
        vazio(pesquisa(x, "inicio", mudanca, mudanca));
        vazio(pesquisa(x, "fim", mudanca.minusDays(1), mudanca));
        vazio(pesquisa(x, "fim", x.inicio(), RelogioSistema.hoje()));
        // A genuine subsequent termination makes the end-date positive control discriminatory.
        LocalDate fim = mudanca.plusDays(3); long causa = motivo(x, fim, fim);
        enviar(x.c().localA, rota + "/desligar", terminar(fim, causa), 200, "Controle posterior de término real");
        contemSomente(pesquisa(x, "fim", fim, fim), x.vinculo());
        vazio(pesquisa(x, "fim", mudanca.minusDays(1), mudanca));
        vazio(pesquisa(x, "inicio", mudanca, mudanca));
        contemSomente(pesquisa(x, "inicio", x.inicio(), x.inicio()), x.vinculo());
        registrar("his003-alocacao-" + x.vinculo(), mapa("vinculo", x.vinculo(), "alocacao", alocacao.path("id"),
                "inicioReal", x.inicio(), "mudancaInterna", mudanca, "fimRealPosterior", fim,
                "alocacoes", consultar(x.c().localA, rota + "/alocacoes", "Faixas comprovam mudança sem novo vínculo")));
    }

    @ParameterizedTest(name = "assinatura v2: adulteração isolada de {0}")
    @ValueSource(strings = {"gestao", "estado", "data"})
    void assinaturaV2DetectaCadaMetadadoIsoladamente(String campo) throws Exception {
        Cenario c = criarCenarioPersistido();
        long historico = emTransacao(() -> {
            HistoricoEmpregado h = new HistoricoEmpregado();
            h.setEmpregado(em.find(Empregado.class, c.empregadoA.getId()));
            h.setUsuario(em.find(UsuarioAplicacao.class, c.localA.getId()));
            h.setDataEvento(LocalDateTime.of(2026, 6, 10, 10, 30)); h.setDataEfetiva(DATA);
            h.setGestaoLocalSnapshotId(c.gestaoA.getId()); h.setSuperintendenciaSnapshotId(c.seA.getId());
            h.setDescricao("Ensaio sintético v2 de metadado " + campo);
            return integridadeC.salvarAssinado(h).getId();
        });
        Map<String, Object> antes = emTransacao(() -> linha(em.find(HistoricoEmpregado.class, historico), HistoricoEmpregado.class));
        assertEquals(2, ((Number) antes.get("versaoAssinatura")).intValue()); assertNull(antes.get("ocorrencia"));
        assertTrue(integridadeC.verificarEmpregado(c.empregadoA.getId()).integro());
        emTransacao(() -> {
            HistoricoEmpregado h = em.find(HistoricoEmpregado.class, historico);
            switch (campo) {
                case "gestao" -> h.setGestaoLocalSnapshotId(c.gestaoB.getId());
                case "estado" -> h.setSuperintendenciaSnapshotId(c.seB.getId());
                case "data" -> h.setDataEfetiva(DATA.plusDays(1));
                default -> throw new IllegalArgumentException(campo);
            }
            em.flush(); return null;
        });
        Map<String, Object> adulterado = emTransacao(() -> linha(em.find(HistoricoEmpregado.class, historico), HistoricoEmpregado.class));
        assertEquals(antes.get("hashIntegridade"), adulterado.get("hashIntegridade"));
        var check = integridadeC.verificarEmpregado(c.empregadoA.getId()); assertFalse(check.integro());
        var esperado = new TreeMap<>(antes); var medido = new TreeMap<>(adulterado);
        esperado.remove("version"); medido.remove("version");
        String atributo = switch (campo) { case "gestao" -> "gestaoLocalSnapshotId"; case "estado" -> "superintendenciaSnapshotId"; default -> "dataEfetiva"; };
        esperado.remove(atributo); medido.remove(atributo); assertEquals(esperado, medido, "A adulteração isola um único metadado");
        registrar("aud001-v2-" + campo + "-" + historico, mapa("historico", historico, "campo", campo,
                "antes", antes, "adulterado", adulterado, "integroAntes", true, "integroDepois", check.integro()));
        // The deliberately tampered synthetic record is retained in this isolated schema as the negative proof.
    }
}
