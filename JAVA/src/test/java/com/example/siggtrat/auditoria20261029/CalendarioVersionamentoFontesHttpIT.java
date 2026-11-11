package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** GOV-003, TMP-003/005 e REL-002/003/006: calendário é fonte temporal auditável da apuração. */
class CalendarioVersionamentoFontesHttpIT extends AuditoriaHttpSuporte {
    @Test
    void inclusaoRetroativaSuperaEmissaoSemLancamentoEPreservaOutrosRecortes() throws Exception {
        Cenario c = criarCenario();
        JsonNode original = emitir(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId());
        JsonNode outroDia = emitir(c.localA, DATA.plusDays(1), c.contratoA.getId(), c.lotacaoA.getId());
        JsonNode outraGestao = emitir(c.localB, DATA, c.contratoB.getId(), c.lotacaoB.getId());
        assertEquals(480, minutos(original));
        assertEquals(0, original.path("conteudo").path("totais").path("diasLancados").asInt());
        long id = idEmissao(original);
        String hash = original.path("metadados").path("hashConteudo").asText();
        byte[] bytes = em.find(EmissaoRelatorioAdministrativo.class, id).getConteudoSnapshotGzip().clone();

        criar(c, evento(DATA, c.lotacaoA.getId(), "Feriado publicado retroativamente"));
        JsonNode vigente = previa(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId());
        assertEquals(0, minutos(vigente), "A inclusão altera a previsão mesmo sem frequência materializada.");
        JsonNode historica = obter(c.localA, id);
        assertAll(
                () -> assertEquals("SUPERADA", historica.path("metadados").path("estado").asText(),
                        "A emissão que calculava 480 minutos não pode permanecer vigente após o feriado."),
                () -> assertEquals("RETIFICACAO_FONTE", historica.path("metadados").path("causaSuperacao").asText()),
                () -> assertEquals(original.path("conteudo"), historica.path("conteudo")),
                () -> assertEquals("VIGENTE", obter(c.localA, idEmissao(outroDia)).path("metadados").path("estado").asText()),
                () -> assertEquals("VIGENTE", obter(c.localB, idEmissao(outraGestao)).path("metadados").path("estado").asText()));
        JsonNode retificada = emitir(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId());
        assertEquals("RETIFICADA", retificada.path("metadados").path("natureza").asText());
        assertEquals(id, retificada.path("metadados").path("emissaoAnteriorId").asLong());
        assertEquals(0, minutos(retificada));
        em.flush(); em.clear();
        EmissaoRelatorioAdministrativo persistida = em.find(EmissaoRelatorioAdministrativo.class, id);
        assertEquals(hash, persistida.getHashConteudo());
        assertArrayEquals(bytes, persistida.getConteudoSnapshotGzip());
        assertEquals(0L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id = :v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    @Test
    void retificarDataELotacaoInvalidaOsDoisRecortesSemAtingirTerceiroDia() throws Exception {
        Cenario c = criarCenario();
        Lotacao destino = lotacaoComPessoa(c);
        JsonNode calendario = criar(c, evento(DATA, c.lotacaoA.getId(), "Feriado no recorte anterior"));
        JsonNode anterior = emitir(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId());
        JsonNode novo = emitir(c.localA, DATA.plusDays(1), c.contratoA.getId(), destino.getId());
        JsonNode externo = emitir(c.localA, DATA.plusDays(2), c.contratoA.getId(), destino.getId());
        assertEquals(0, minutos(anterior));
        assertEquals(480, minutos(novo));
        Map<String, Object> req = evento(DATA.plusDays(1), destino.getId(), "Feriado no recorte corrigido");
        req.put("justificativa", "Ato publicado corrigiu a data e a unidade beneficiada");
        executar(c.localA, put("/api/calendario-eventos/{id}", calendario.path("id").asLong())
                .contentType(APPLICATION_JSON).content(json(req)), 200, "Retificação com data e lotação diferentes");
        assertAll(
                () -> assertEquals("SUPERADA", obter(c.localA, idEmissao(anterior)).path("metadados").path("estado").asText(),
                        "O recorte anterior perdeu o feriado e deve ser invalidado."),
                () -> assertEquals("SUPERADA", obter(c.localA, idEmissao(novo)).path("metadados").path("estado").asText(),
                        "O novo recorte recebeu o feriado e deve ser invalidado."),
                () -> assertEquals("VIGENTE", obter(c.localA, idEmissao(externo)).path("metadados").path("estado").asText(),
                        "O terceiro dia não sofreu alteração de fonte."),
                () -> assertEquals(480, minutos(previa(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId()))),
                () -> assertEquals(0, minutos(previa(c.localA, DATA.plusDays(1), c.contratoA.getId(), destino.getId()))));
    }

    @Test
    void cancelamentoPreservaFatoCadeiaEEmissaoMasRetiraEfeitoAtual() throws Exception {
        Cenario c = criarCenario();
        JsonNode calendario = criar(c, evento(DATA, c.lotacaoA.getId(), "Feriado posteriormente cancelado"));
        long id = calendario.path("id").asLong();
        Instant antesDoCancelamento = Instant.now();
        JsonNode emissao = emitir(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId());
        executar(c.localA, delete("/api/calendario-eventos/{id}", id)
                .param("justificativa", "Ato posterior revogou o feriado publicado"),
                204, "O contrato DELETE representa cancelamento auditável");
        em.flush(); em.clear();
        assertNotNull(em.find(CalendarioEvento.class, id), "GOV-003 proíbe apagar fisicamente o fato publicado.");
        JsonNode atual = calendarioNaData(c, DATA, null);
        assertEquals(0, atual.size(), "O evento cancelado deixa de compor a lista vigente.");
        JsonNode anterior = calendarioNaData(c, DATA, antesDoCancelamento);
        assertEquals(1, anterior.size());
        assertEquals(id, anterior.get(0).path("id").asLong());
        assertTrue(anterior.get(0).path("ativo").asBoolean());
        JsonNode cadeia = historico(c.localA, id);
        assertEquals(2, cadeia.size());
        assertEquals("CRIACAO", cadeia.get(0).path("tipoOperacao").asText());
        assertEquals("CANCELAMENTO", cadeia.get(1).path("tipoOperacao").asText());
        assertEquals(cadeia.get(0).path("id").asLong(), cadeia.get(1).path("versaoAnteriorId").asLong());
        assertEquals("Ato posterior revogou o feriado publicado", cadeia.get(1).path("justificativa").asText());
        assertFalse(cadeia.get(1).path("fotografia").path("ativo").asBoolean());
        assertTrue(cadeia.get(1).path("fotografia").path("cancelado").asBoolean());
        assertEquals(480, minutos(previa(c.localA, DATA, c.contratoA.getId(), c.lotacaoA.getId())));
        JsonNode historica = obter(c.localA, idEmissao(emissao));
        assertEquals("SUPERADA", historica.path("metadados").path("estado").asText());
        assertEquals(emissao.path("conteudo"), historica.path("conteudo"));
        executar(c.localB, get("/api/calendario-eventos/{id}/eventos", id),
                404, "O histórico do calendário permanece subordinado ao escopo");
        executar(c.localA, put("/api/calendario-eventos/{id}", id).contentType(APPLICATION_JSON)
                .content(json(evento(DATA, c.lotacaoA.getId(), "Tentativa de reescrita"))),
                404, "Cancelado não pode ser reescrito como se continuasse vigente");
    }

    @Test
    void retificacaoDeRetificacaoPreservaConhecimentoAutoriaEFotografiaCompleta() throws Exception {
        Cenario c = criarCenario();
        Instant antesDaCriacao = Instant.now();
        JsonNode criada = criar(c, evento(DATA, c.lotacaoA.getId(), "Publicação original"));
        long id = criada.path("id").asLong();
        Instant primeiroCorte = Instant.now();
        Map<String, Object> primeira = evento(DATA.plusDays(1), c.lotacaoA.getId(), "Primeira correção");
        primeira.put("justificativa", "Data correta segundo o primeiro ato retificador");
        JsonNode retificacao1=dados(executar(c.localA, put("/api/calendario-eventos/{id}", id).contentType(APPLICATION_JSON)
                .content(json(primeira)), 200, "Primeira retificação do calendário"));
        Instant segundoCorte = Instant.now();
        Map<String, Object> segunda = evento(DATA.plusDays(2), c.lotacaoA.getId(), "Segunda correção");
        segunda.put("tipo", "DIA_DESCANSO");
        segunda.put("justificativa", "Segundo ato corrigiu novamente a data e a natureza");
        JsonNode retificacao2=dados(executar(c.localA, put("/api/calendario-eventos/{id}", id).contentType(APPLICATION_JSON)
                .content(json(segunda)), 200, "Retificação da retificação preserva ambas as anteriores"));
        em.flush(); em.clear();
        JsonNode cadeia = historico(c.localA, id);
        assertEquals(3, cadeia.size());
        assertEquals(criada,cadeia.get(0).path("fotografia"),"TMP-003:002 fotografia integral da publicação");
        assertEquals(retificacao1,cadeia.get(1).path("fotografia"),"Fotografia integral da primeira correção");
        assertEquals(retificacao2,cadeia.get(2).path("fotografia"),"Fotografia integral da segunda correção");
        for (int i = 0; i < cadeia.size(); i++) {
            JsonNode versao = cadeia.get(i);
            assertEquals(i + 1, versao.path("numeroVersao").asInt());
            assertEquals(c.localA.getId().longValue(), versao.path("usuarioId").asLong());
            assertEquals(c.localA.getUsername(), versao.path("usuarioUsername").asText());
            assertEquals(DATA.plusDays(i).toString(), versao.path("dataEfetiva").asText());
            assertNotNull(Instant.parse(versao.path("registradoEm").asText()));
            assertEquals(c.lotacaoA.getId().longValue(), versao.path("fotografia").path("lotacaoId").asLong());
            assertEquals(c.gestaoA.getId().longValue(), versao.path("fotografia").path("gestaoLocal").path("id").asLong());
            if (i > 0) assertEquals(cadeia.get(i - 1).path("id").asLong(), versao.path("versaoAnteriorId").asLong());
        }
        assertEquals("Publicação original", cadeia.get(0).path("fotografia").path("descricao").asText());
        assertEquals("FERIADO", cadeia.get(0).path("fotografia").path("tipo").asText());
        assertEquals("DIA_DESCANSO", cadeia.get(2).path("fotografia").path("tipo").asText());
        assertTrue(cadeia.get(0).path("justificativa").isNull(), "Motivo humano ausente não deve ser inventado.");
        assertFalse(cadeia.get(0).path("motivoTecnico").asText().isBlank(), "A operação automática possui identificação distinta.");
        assertEquals(0, calendarioNaData(c, DATA, antesDaCriacao).size());
        assertEquals("Publicação original", calendarioNaData(c, DATA, primeiroCorte).get(0).path("descricao").asText());
        assertEquals("Primeira correção", calendarioNaData(c, DATA.plusDays(1), segundoCorte).get(0).path("descricao").asText());
        assertEquals(0, calendarioNaData(c, DATA, null).size());
        assertEquals("Segunda correção", calendarioNaData(c, DATA.plusDays(2), null).get(0).path("descricao").asText());
        assertEquals(3, historico(c.presidente, id).size(), "Perfil institucional pode auditar sem modificar a fonte.");
    }

    private Lotacao lotacaoComPessoa(Cenario c) {
        Lotacao l = new Lotacao("Outro recorte " + UUID.randomUUID());
        l.setGestaoLocal(c.gestaoA); l.setMunicipio(c.lotacaoA.getMunicipio()); l.getTurnos().add(c.jornadaA); em.persist(l);
        ComposicaoContratual comp = composicao(c.contratoA, l, c.jornadaA, c.cargoA, 5);
        Pessoa p = pessoa("Pessoa do recorte novo " + UUID.randomUUID(), cpf(810000000 + c.pessoaA.getId().intValue()));
        VinculoContratual v = vinculo(p, c.contratoA);
        AlocacaoVinculo a = alocacao(v, comp); projecao.projetar(v, a); em.flush();
        return l;
    }
    private Map<String, Object> evento(LocalDate data, Long lotacao, String descricao) {
        return new LinkedHashMap<>(Map.of("data", data.toString(), "tipo", "FERIADO", "descricao", descricao,
                "abrangencia", "LOTACAO", "lotacaoId", lotacao, "ativo", true));
    }
    private Map<String, Object> filtro(LocalDate data, Long contrato, Long lotacao) {
        return Map.of("tipo", "FREQUENCIA_CONSOLIDADA", "formato", "VISUALIZACAO",
                "inicio", data.toString(), "fim", data.toString(), "contratoIds", List.of(contrato),
                "lotacaoId", lotacao, "chaveIdempotencia", "calendario-emissao-" + UUID.randomUUID());
    }
    private JsonNode criar(Cenario c, Map<String, Object> req) throws Exception {
        return dados(executar(c.localA, post("/api/calendario-eventos").contentType(APPLICATION_JSON)
                .content(json(req)), 201, "Publicação de fato do calendário operacional"));
    }
    private JsonNode emitir(UsuarioAplicacao usuario, LocalDate data, Long contrato, Long lotacao) throws Exception {
        return dados(executar(usuario, post("/api/relatorios/administrativos/emissoes")
                .contentType(APPLICATION_JSON).content(json(filtro(data, contrato, lotacao))), 201, "Emissão da fonte SEM_LANCAMENTO"));
    }
    private JsonNode previa(UsuarioAplicacao usuario, LocalDate data, Long contrato, Long lotacao) throws Exception {
        return dados(executar(usuario, post("/api/relatorios/administrativos/previa")
                .contentType(APPLICATION_JSON).content(json(filtro(data, contrato, lotacao))), 200, "Apuração atual da fonte do calendário"));
    }
    private JsonNode obter(UsuarioAplicacao usuario, long id) throws Exception {
        return dados(executar(usuario, get("/api/relatorios/administrativos/emissoes/{id}", id), 200, "Prova de emissão preservada"));
    }
    private JsonNode historico(UsuarioAplicacao usuario, long id) throws Exception {
        return dados(executar(usuario, get("/api/calendario-eventos/{id}/eventos", id), 200, "Cadeia de versões publicada do calendário"));
    }
    private JsonNode calendarioNaData(Cenario c, LocalDate data, Instant conhecimento) throws Exception {
        var req = get("/api/calendario-eventos").param("data", data.toString());
        if (conhecimento != null) req.param("conhecidoAte", conhecimento.toString());
        return dados(executar(c.localA, req, 200, "Calendário pela data efetiva e conhecimento separado"));
    }
    private int minutos(JsonNode r) { return r.path("conteudo").path("totais").path("minutosPrevistos").asInt(); }
    private long idEmissao(JsonNode r) { return r.path("metadados").path("id").asLong(); }
}
