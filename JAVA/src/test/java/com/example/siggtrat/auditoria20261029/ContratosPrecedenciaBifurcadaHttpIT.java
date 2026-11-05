package com.example.siggtrat.auditoria20261029;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RED proposto pela revisão independente; ainda não executado nesta etapa. */
class ContratosPrecedenciaBifurcadaHttpIT extends ContratosHttpBase {
    @Test
    void mesmaDataNaoAdmiteDoisSucessoresConflitantesSemOrdemEntreEles() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var data = n.inicio().plusDays(1);

        var primeiro = aditamento("CADEIA-A", data);
        primeiro.put("novaDescricao", "Descrição do primeiro instrumento");
        var a = registrarAditamento(c, n.id(), primeiro);

        var segundo = aditamento("CADEIA-B", data);
        segundo.put("novaDescricao", "Descrição do segundo instrumento");
        segundo.put("eventoPrecedenteId", a.path("eventoId").asLong());
        segundo.put("relacaoPrecedencia", "SUCEDE");
        var b = registrarAditamento(c, n.id(), segundo);
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "TMP-010 cadeia antes da bifurcação");

        var terceiro = aditamento("CADEIA-C", data);
        terceiro.put("novaDescricao", "Descrição do terceiro instrumento");
        terceiro.put("eventoPrecedenteId", a.path("eventoId").asLong());
        terceiro.put("relacaoPrecedencia", "SUCEDE");
        previaAditamento(c, n.id(), terceiro);
        enviar(c.localA, base(n.id()) + "/aditamentos", terceiro, 409,
                "TMP-010/TMP-016/ADI-009 A→B e A→C não definem precedência entre B e C no mesmo campo e dia");

        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "TMP-010 recusa não grava fato ambíguo"))
                .isEqualTo(eventosAntes);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-001 preserva somente os dois instrumentos válidos"))
                .hasSize(2);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP-004 último valor explicitamente ordenado permanece vigente")
                .path("versaoContratual").path("descricao").asText())
                .isEqualTo("Descrição do segundo instrumento");

        terceiro.put("eventoPrecedenteId", b.path("eventoId").asLong());
        registrarAditamento(c, n.id(), terceiro);
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP-016 A→B→C fornece sequência negocial completa")
                .path("versaoContratual").path("descricao").asText())
                .isEqualTo("Descrição do terceiro instrumento");
    }

    @Test
    void precedenciaTransitivaPodePassarPorInstrumentoQueAlteraOutroCampo() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var data = n.inicio().plusDays(1);
        var inicial = aditamento("TRANSITIVA-A", data);
        inicial.put("novaDescricao", "Descrição inicial transitiva");
        var a = registrarAditamento(c, n.id(), inicial);
        var independente = aditamento("TRANSITIVA-B", data);
        independente.put("novaRazaoSocialEmpresa", "Empresa independente na mesma data");
        independente.put("eventoPrecedenteId", a.path("eventoId").asLong());
        independente.put("relacaoPrecedencia", "SUCEDE");
        var b = registrarAditamento(c, n.id(), independente);
        var finalReq = aditamento("TRANSITIVA-C", data);
        finalReq.put("novaDescricao", "Descrição final transitiva");
        finalReq.put("eventoPrecedenteId", b.path("eventoId").asLong());
        finalReq.put("relacaoPrecedencia", "SUCEDE");
        registrarAditamento(c, n.id(), finalReq);

        var versao = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP-011/TMP-016 A→B→C define a ordem dos campos conflitantes mesmo com campo intermediário distinto")
                .path("versaoContratual");
        assertThat(versao.path("descricao").asText()).isEqualTo("Descrição final transitiva");
        assertThat(versao.path("razaoSocialEmpresa").asText()).isEqualTo("Empresa independente na mesma data");
    }

    @Test
    void sequenciaFormalCompletaDesambiguaSucessoresDoMesmoAto() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var data = n.inicio().plusDays(1);
        var inicial = aditamento("FORMAL-A", data);
        inicial.put("novaDescricao", "Ordem formal 1");
        inicial.put("ordemDocumental", 1);
        var a = registrarAditamento(c, n.id(), inicial);
        var segundo = aditamento("FORMAL-B", data);
        segundo.put("novaDescricao", "Ordem formal 2");
        segundo.put("ordemDocumental", 2);
        segundo.put("eventoPrecedenteId", a.path("eventoId").asLong());
        segundo.put("relacaoPrecedencia", "SUCEDE");
        registrarAditamento(c, n.id(), segundo);
        var terceiro = aditamento("FORMAL-C", data);
        terceiro.put("novaDescricao", "Ordem formal 3");
        terceiro.put("ordemDocumental", 3);
        terceiro.put("eventoPrecedenteId", a.path("eventoId").asLong());
        terceiro.put("relacaoPrecedencia", "SUCEDE");
        registrarAditamento(c, n.id(), terceiro);

        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "TMP-016 sequência formal distinta define B antes de C")
                .path("versaoContratual").path("descricao").asText()).isEqualTo("Ordem formal 3");
    }

    @Test
    void retificarAntecessorPreservaSequenciaDoSucessorValidoNaMesmaData() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var data = n.inicio().plusDays(1);
        var inicial = aditamento("RETIFICA-CADEIA-A", data);
        inicial.put("novaDescricao", "Texto original do antecessor");
        var a = registrarAditamento(c, n.id(), inicial);
        var sucessor = aditamento("RETIFICA-CADEIA-B", data);
        sucessor.put("novaDescricao", "Texto válido do sucessor");
        sucessor.put("eventoPrecedenteId", a.path("eventoId").asLong());
        sucessor.put("relacaoPrecedencia", "SUCEDE");
        registrarAditamento(c, n.id(), sucessor);
        var retificacao = aditamento("RETIFICA-CADEIA-A", data);
        retificacao.put("novaDescricao", "Correção do texto anterior");
        retificacao.put("eventoPrecedenteId", a.path("eventoId").asLong());
        retificacao.put("relacaoPrecedencia", "RETIFICA");
        previaAditamento(c, n.id(), retificacao);
        enviar(c.localA, base(n.id()) + "/aditamentos/" + a.path("id").asLong() + "/retificar", retificacao,
                200, "ADI-007/TMP-008 nova versão herda a posição causal do instrumento corrigido");

        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "ADI-008 sucessor válido mantém a sua precedência após retificação do antecessor")
                .path("versaoContratual").path("descricao").asText()).isEqualTo("Texto válido do sucessor");
    }

    @Test
    void remanejamentoTambemExigeOrdemEntreTodosOsSucessoresDoMesmoRecorte() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, 50, true);
        var data = n.inicio().plusDays(1);
        var primeiro = remanejamento(n, "RAMO-REM-A", data, 5);
        previaRemanejamento(c, n.id(), primeiro);
        var a = enviar(c.localA, base(n.id()) + "/remanejamentos", primeiro, 201, "REM-001 primeiro remanejamento");
        var segundo = remanejamento(n, "RAMO-REM-B", data, 5);
        segundo.put("eventoPrecedenteId", a.path("eventoId").asLong());
        segundo.put("relacaoPrecedencia", "SUCEDE");
        previaRemanejamento(c, n.id(), segundo);
        var b = enviar(c.localA, base(n.id()) + "/remanejamentos", segundo, 201, "TMP-016 cadeia explícita A→B");
        var antes = consultar(c.localA, base(n.id()) + "/eventos", "GOV-003 eventos antes da recusa");
        var terceiro = remanejamento(n, "RAMO-REM-C", data, 5);
        terceiro.put("eventoPrecedenteId", a.path("eventoId").asLong());
        terceiro.put("relacaoPrecedencia", "SUCEDE");
        previaRemanejamento(c, n.id(), terceiro);
        enviar(c.localA, base(n.id()) + "/remanejamentos", terceiro, 409,
                "TMP-010 dois remanejamentos no mesmo recorte não podem ser ramos sem ordem definida");
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "GOV-003 recusa sem persistência parcial")).isEqualTo(antes);
        terceiro.put("eventoPrecedenteId", b.path("eventoId").asLong());
        previaRemanejamento(c, n.id(), terceiro);
        enviar(c.localA, base(n.id()) + "/remanejamentos", terceiro, 201, "REM-002 cadeia A→B→C preserva total");
        var composicoes = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data,
                "REM-002 três variações válidas preservam o total cem");
        for (var composicao : composicoes) {
            int esperado = composicao.path("id").asLong() == n.composicao1() ? 35 : 65;
            assertThat(composicao.path("efetivoPrevisto").asInt()).isEqualTo(esperado);
        }
    }
}
