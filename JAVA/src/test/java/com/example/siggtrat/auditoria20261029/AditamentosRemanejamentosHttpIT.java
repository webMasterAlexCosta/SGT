package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AditamentosRemanejamentosHttpIT extends ContratosHttpBase {
    @Test
    void aumentoEReducoesUsamBaseOriginalGlobalEPisoArredondado() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        var aumento = aditamento("AUMENTO", n.inicio().plusDays(2));
        aumento.put("novoEfetivoTotal", 140);
        aumento.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 140)));
        var a = registrarAditamento(c, n.id(), aumento);
        assertThat(a.path("alteracoesComposicao").get(0).path("quantidadeAnterior").asInt()).isEqualTo(100);
        assertThat(a.path("alteracoesComposicao").get(0).path("variacao").asInt()).isEqualTo(40);
        var reducao = aditamento("REDUCAO", n.inicio().plusDays(3));
        reducao.put("novoEfetivoTotal", 75);
        reducao.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 75)));
        JsonNode previa = previaAditamento(c, n.id(), reducao);
        assertThat(previa.path("efetivoOriginalPublicado").asInt()).isEqualTo(100);
        assertThat(previa.path("pisoNormalReducao").asInt()).isEqualTo(75);
        enviar(c.localA, base(n.id()) + "/aditamentos", reducao, 201, "RED-001/002/004/006 100→140→75");
        em.flush();
        var abaixo = aditamento("ABAIXO", n.inicio().plusDays(4));
        abaixo.put("novoEfetivoTotal", 74);
        abaixo.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 74)));
        previaAditamento(c, n.id(), abaixo);
        enviar(c.localA, base(n.id()) + "/aditamentos", abaixo, 409, "RED-005 piso normal não pode ser ultrapassado");
        JsonNode pos = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + RelogioSistema.hoje(), "RED-006 posição válida");
        assertThat(pos.path("versaoContratual").path("efetivoPrevisto").asInt()).isEqualTo(75);
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-001 lista preserva dois fatos").size()).isEqualTo(2);
    }

    @Test
    void reducaoOriginal101Exige76MesmoComComposicaoZerada() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 101, 25, false);
        var r = aditamento("ZERAR-25", n.inicio().plusDays(1));
        r.put("novoEfetivoTotal", 76);
        r.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 0)));
        JsonNode p = previaAditamento(c, n.id(), r);
        assertThat(p.path("pisoNormalReducao").asInt()).isEqualTo(76);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 201, "RED-003/007 redução da composição a zero com piso76");
        JsonNode comps = consultar(c.localA, base(n.id()) + "/composicoes?data=" + RelogioSistema.hoje(), "CMP-004 zero indisponível");
        JsonNode primeira = null;
        for (JsonNode x : comps) if (x.path("id").asLong() == n.composicao1()) primeira = x;
        assertThat(primeira).isNotNull();
        assertThat(primeira.path("efetivoPrevisto").asInt()).isZero();
        assertThat(primeira.path("permiteNovaAlocacao").asBoolean()).isFalse();
        var invalida = aditamento("REDUZIR-A75", n.inicio().plusDays(2));
        invalida.put("novoEfetivoTotal", 75);
        invalida.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao2(), "quantidadeResultante", 75)));
        previaAditamento(c, n.id(), invalida);
        enviar(c.localA, base(n.id()) + "/aditamentos", invalida, 409, "RED-007 arredondamento para cima");
    }

    @Test
    void aditamentoFuturoPodeSerRetificadoECanceladoSemApagarVersoes() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, true);
        var r = aditamento("ADI-FUTURO", n.inicio().plusDays(1));
        r.put("novaDescricao", "Primeira versão futura");
        var a = registrarAditamento(c, n.id(), r);
        var corrigida = aditamento("ADI-FUTURO", n.inicio().plusDays(1));
        corrigida.put("novaDescricao", "Descrição corrigida");
        corrigida.put("eventoPrecedenteId", a.path("eventoId").asLong());
        corrigida.put("relacaoPrecedencia", "RETIFICA");
        previaAditamento(c, n.id(), corrigida);
        var nova = enviar(c.localA, base(n.id()) + "/aditamentos/" + a.path("id").asLong() + "/retificar",
                corrigida, 200, "ADI-007 retificação preserva original");
        assertThat(nova.path("versaoInstrumento").asInt()).isEqualTo(2);
        enviar(c.localA, base(n.id()) + "/aditamentos/" + nova.path("id").asLong() + "/cancelar",
                mapa("motivo", "Documento revogado antes do efeito", "referenciaSei", "SEI-CANCEL"), 200, "ADI-006 cancela somente futuro");
        JsonNode lista = consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-001/007 cadeia completa");
        assertThat(lista.size()).isEqualTo(2);
        assertThat(lista.get(0).path("situacao").asText()).isEqualTo("RETIFICADO");
        assertThat(lista.get(1).path("situacao").asText()).isEqualTo("CANCELADO");
        JsonNode pos = consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + n.inicio().plusDays(2), "ADI-006 cancelado não produz efeito");
        assertThat(pos.path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
    }

    @Test
    void remanejamentoPreservaTotalPermiteSeiOpcionalRetificaECancelaFuturo() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, 25, true);
        var r = remanejamento(n, "REM-FUTURO", n.inicio().plusDays(1), 5);
        JsonNode p = previaRemanejamento(c, n.id(), r);
        assertThat(p.path("somaVariacoes").asInt()).isZero();
        assertThat(p.path("totalPreservado").asBoolean()).isTrue();
        var rem = enviar(c.localA, base(n.id()) + "/remanejamentos", r, 201, "REM-001/002/005 sem fonte opcional");
        em.flush();
        var cor = remanejamento(n, "REM-FUTURO", n.inicio().plusDays(1), 10);
        cor.put("eventoPrecedenteId", rem.path("eventoId").asLong());
        cor.put("relacaoPrecedencia", "RETIFICA");
        previaRemanejamento(c, n.id(), cor);
        var nova = enviar(c.localA, base(n.id()) + "/remanejamentos/" + rem.path("id").asLong() + "/retificar",
                cor, 200, "REM-004 preserva remanejamento anterior");
        assertThat(nova.path("versaoInstrumento").asInt()).isEqualTo(2);
        enviar(c.localA, base(n.id()) + "/remanejamentos/" + nova.path("id").asLong() + "/cancelar",
                mapa("motivo", "Redistribuição cancelada antes do efeito"), 200, "REM-007 cancelamento auditado");
        var lista = consultar(c.localA, base(n.id()) + "/remanejamentos", "REM-004 cadeia preservada");
        assertThat(lista.size()).isEqualTo(2);
        assertThat(lista.get(1).path("situacao").asText()).isEqualTo("CANCELADO");
        var comps = consultar(c.localA, base(n.id()) + "/composicoes?data=" + n.inicio().plusDays(2), "REM-007 distribuição cancelada não aplicada");
        int total = 0;
        for (JsonNode comp : comps) total += comp.path("efetivoPrevisto").asInt();
        assertThat(total).isEqualTo(100);
    }

    @Test
    void aditamentoRecusaFonteAusenteTokenAlteradoEDuplicidade() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, null, false);
        var r = aditamento("ADI-DOC", n.inicio().plusDays(1)); r.put("novaDescricao", "Uma nova descrição");
        r.remove("referenciaSei");
        enviar(c.localA, base(n.id()) + "/aditamentos/previa", r, 400, "DOC-002 fonte obrigatória");
        r.put("referenciaSei", "SEI-VALIDO");
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 409, "AUD-004 token ausente");
        previaAditamento(c, n.id(), r);
        r.put("novaDescricao", "Intenção modificada depois da prévia");
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 409, "AUD-004 intenção alterada invalida token");
        registrarAditamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 409, "AUD-005 repetição não cria fato duplicado");
        assertThat(consultar(c.localA, base(n.id()) + "/aditamentos", "ADI-001 somente um registro").size()).isEqualTo(1);
    }

    @Test
    void remanejamentoRecusaNovaCombinacaoOuSomaNaoZero() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, 25, false);
        var r = remanejamento(n, "REM-INVALIDO", n.inicio().plusDays(1), 5);
        r.put("itens", List.of(mapa("composicaoId", n.composicao1(), "variacao", -5),
                mapa("composicaoId", n.composicao2(), "variacao", 4)));
        var p = previaRemanejamento(c, n.id(), r);
        assertThat(p.path("somaVariacoes").asInt()).isEqualTo(-1);
        enviar(c.localA, base(n.id()) + "/remanejamentos", r, 400, "REM-002 total precisa ser preservado");
        r.put("itens", List.of(mapa("composicaoId", n.composicao1(), "variacao", -5),
                mapa("composicaoId", Long.MAX_VALUE, "variacao", 5)));
        enviar(c.localA, base(n.id()) + "/remanejamentos/previa", r, 400, "REM-003 não cria combinação nova");
    }

    @Test
    void zeramentoNaoPodeDeixarProgramacaoFuturaSemComposicao() throws Exception {
        var c = criarCenario();
        var n = publicado(c, 100, 25, false);
        var inicioVinculo = RelogioSistema.hoje().plusDays(20);
        enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa programada HTTP", "cpf", "52998224725"),
                "contratoId", n.id(), "dataInicio", inicioVinculo, "composicaoId", n.composicao1()),
                201, "CMP-006 programação futura anterior ao zeramento");
        var r = aditamento("ZERAR-PROGRAMADO", RelogioSistema.hoje().plusDays(10));
        r.put("novoEfetivoTotal", 75);
        r.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 0)));
        previaAditamento(c, n.id(), r);
        enviar(c.localA, base(n.id()) + "/aditamentos", r, 409, "CMP-006 futura alocação órfã bloqueia conclusão");
    }
}
