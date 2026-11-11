package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Provas específicas dos critérios que não eram afirmados pelos cenários abrangentes. */
class ContratosCriteriosEspecificosHttpIT extends ContratosHttpBase {
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void identificadorDuplicadoEmPutDeRascunhoNaoAlteraNenhumContrato() throws Exception {
        var c = criarCenarioPersistido(); var inicio = RelogioSistema.hoje().plusDays(5);
        var primeiro = rascunho(c, "PUT-A-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, inicio.plusMonths(2));
        var segundo = rascunho(c, "PUT-B-" + UUID.randomUUID().toString().substring(0, 8), 5, inicio, inicio.plusMonths(2));
        long a = enviar(c.localA, "/api/contratos", primeiro, 201, "CON002 primeiro rascunho").path("id").asLong();
        long b = enviar(c.localA, "/api/contratos", segundo, 201, "CON002 segundo rascunho").path("id").asLong();
        executar(c.localA, put(base(b)).contentType(MediaType.APPLICATION_JSON).content(json(primeiro)), 409,
                "CON002 PUT duplicado não pode trocar a identidade do segundo rascunho");
        emTransacao(() -> {
            em.clear();
            assertThat(em.find(Contrato.class, a).getNumeroContrato()).isEqualToIgnoringCase((String) primeiro.get("numeroContrato"));
            assertThat(em.find(Contrato.class, b).getNumeroContrato()).isEqualToIgnoringCase((String) segundo.get("numeroContrato"));
            assertThat(em.find(Contrato.class, b).getAnoContrato()).isEqualTo(segundo.get("anoContrato"));
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"empregado", "historico", "evento"})
    void rascunhoComDependenciaAuditavelNaoPodeSerDescartado(String dependencia) throws Exception {
        var c = criarCenario(); var inicio = RelogioSistema.hoje().plusDays(5);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "GOV004-" + UUID.randomUUID().toString().substring(0, 8),
                5, inicio, inicio.plusMonths(2)), 201, "GOV004 rascunho isolado para proteção do legado").path("id").asLong();
        long composicao = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "GOV004 composição exclusiva deve permanecer se o descarte for recusado").path("id").asLong();
        Contrato contrato = em.find(Contrato.class, id);
        switch (dependencia) {
            case "empregado" -> {
                c.empregadoA.getContratos().clear();
                c.empregadoA.getContratos().add(contrato);
            }
            case "historico" -> {
                var h = new HistoricoVinculoContrato(); h.setContrato(contrato); h.setEmpregado(c.empregadoA);
                h.setDataInicio(inicio.minusDays(2).atStartOfDay(ZoneOffset.UTC));
                h.setDataFim(inicio.minusDays(1).atStartOfDay(ZoneOffset.UTC)); h.setVinculoAtivo(null); em.persist(h);
            }
            case "evento" -> {
                var e = new EventoCicloVidaContrato(); e.setContrato(contrato); e.setTipoEvento(TipoEventoCicloVidaContrato.ADITAMENTO);
                e.setDataEfetiva(inicio); e.setDataAto(inicio); e.setMotivo("Fato legado já auditável"); e.setChaveNegocial("GOV004:" + id); em.persist(e);
            }
            default -> throw new IllegalArgumentException(dependencia);
        }
        em.flush(); em.clear();
        executar(c.localA, delete(base(id)), 409, "GOV004 dependência " + dependencia + " impede descarte");
        em.flush(); em.clear();
        assertThat(em.find(Contrato.class, id)).isNotNull();
        assertThat(em.find(ComposicaoContratual.class, composicao)).isNotNull();
        if (dependencia.equals("evento")) assertThat(em.createQuery(
                "select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult()).isEqualTo(1L);
        if (dependencia.equals("historico")) assertThat(em.createQuery(
                "select count(h) from HistoricoVinculoContrato h where h.contrato.id=:id", Long.class)
                .setParameter("id", id).getSingleResult()).isEqualTo(1L);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falhaNaTransacaoDeDescarteRestauraContratoEComposicaoEmNovaLeitura() throws Exception {
        var c = criarCenarioPersistido(); var inicio = RelogioSistema.hoje().plusDays(5);
        long id = enviar(c.localA, "/api/contratos", rascunho(c, "ROLLBACK-" + UUID.randomUUID().toString().substring(0, 8),
                5, inicio, inicio.plusMonths(2)), 201, "GOV004 rascunho previamente confirmado").path("id").asLong();
        long composicao = enviar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 5),
                201, "GOV004 composição previamente confirmada").path("id").asLong();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
            try {
                executar(c.localA, delete(base(id)), 204, "GOV004 descarte executado dentro da transação ainda não confirmada");
                em.flush(); em.clear();
                assertThat(em.find(Contrato.class, id)).isNull();
                assertThat(em.find(ComposicaoContratual.class, composicao)).isNull();
            } catch (Exception e) { throw new IllegalStateException(e); }
            throw new FalhaAntesDoCommit();
        })).isInstanceOf(FalhaAntesDoCommit.class);
        emTransacao(() -> {
            assertThat(em.find(Contrato.class, id)).isNotNull();
            assertThat(em.find(ComposicaoContratual.class, composicao)).isNotNull();
            return null;
        });
        executar(c.localA, get(base(id)), 200, "GOV004 nova transação enxerga contrato integral após rollback");
    }

    @Test
    void retificacaoDeRetificacaoDeAditamentoPreservaTresVersoesMetadadosEAssinaturaOpcional() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, null, true); var data = n.inicio().plusDays(1);
        Instant antes = Instant.now().minusSeconds(1); JsonNode anterior = null;
        for (int versao = 1; versao <= 3; versao++) {
            var req = aditamento("ADI-TRES-VERSOES", data); req.put("novaDescricao", "Descrição da versão " + versao);
            req.put("justificativa", "Motivo próprio da versão " + versao);
            if (versao == 1) req.put("dataAssinatura", data.minusDays(2));
            if (anterior == null) anterior = registrarAditamento(c, n.id(), req);
            else {
                req.put("eventoPrecedenteId", anterior.path("eventoId").asLong()); req.put("relacaoPrecedencia", "RETIFICA");
                previaAditamento(c, n.id(), req);
                anterior = enviar(c.localA, base(n.id()) + "/aditamentos/" + anterior.path("id").asLong() + "/retificar", req,
                        200, "ADI007 retificação da versão anterior conserva a cadeia");
            }
        }
        em.flush(); em.clear();
        var lista = consultar(c.localA, base(n.id()) + "/aditamentos", "ADI001/002/007 três instrumentos persistidos");
        assertThat(lista.size()).isEqualTo(3); var ids = new HashSet<Long>();
        for (int i = 0; i < 3; i++) {
            var a = lista.get(i); assertThat(ids.add(a.path("id").asLong())).isTrue();
            assertThat(a.path("versaoInstrumento").asInt()).isEqualTo(i + 1);
            assertThat(a.path("novaDescricao").asText()).isEqualTo("Descrição da versão " + (i + 1));
            assertThat(a.path("justificativa").asText()).isEqualTo("Motivo próprio da versão " + (i + 1));
            assertThat(a.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
            assertThat(a.path("usuarioResponsavelUsername").asText()).isEqualTo(c.localA.getUsername());
            assertThat(Instant.parse(a.path("registradoEm").asText())).isAfter(antes).isBefore(Instant.now().plusSeconds(1));
            assertThat(a.path("dataDocumento").asText()).isEqualTo(data.toString());
            assertThat(a.path("dataEfetiva").asText()).isEqualTo(data.toString());
            assertThat(a.path("situacao").asText()).isEqualTo(i < 2 ? "RETIFICADO" : "VIGENTE");
        }
        assertThat(lista.get(0).path("dataAssinatura").asText()).isEqualTo(data.minusDays(2).toString());
        assertThat(lista.get(1).path("dataAssinatura").isNull()).isTrue();
        assertThat(lista.get(2).path("dataAssinatura").isNull()).isTrue();
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data.minusDays(1),
                "CON017 sem efeito antecipado").path("versaoContratual").path("descricao").asText()).isEqualTo("Contrato de auditoria HTTP");
        assertThat(consultar(c.localA, base(n.id()) + "/posicao-temporal?data=" + data,
                "ADI007 somente versão vigente produz efeito").path("versaoContratual").path("descricao").asText()).isEqualTo("Descrição da versão 3");
        var relacoes = consultar(c.localA, base(n.id()) + "/eventos/precedencias", "ADI007 relações sucessivas persistidas");
        assertThat(relacoes.size()).isEqualTo(2);
    }

    @Test
    void retificacaoDeRetificacaoDeRemanejamentoPreservaItensFonteOpcionalEAutoria() throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, true); var data = n.inicio().plusDays(1);
        Instant antes = Instant.now().minusSeconds(1); JsonNode anterior = null;
        for (int versao = 1; versao <= 3; versao++) {
            var req = remanejamento(n, "REM-TRES-VERSOES", data, versao * 5);
            req.put("justificativa", "Redistribuição da versão " + versao);
            if (versao > 1) req.put("referenciaSei", "SEI-REM-" + versao);
            if (anterior != null) {
                req.put("eventoPrecedenteId", anterior.path("eventoId").asLong()); req.put("relacaoPrecedencia", "RETIFICA");
            }
            previaRemanejamento(c, n.id(), req);
            anterior = enviar(c.localA, base(n.id()) + "/remanejamentos" + (anterior == null ? "" : "/" + anterior.path("id").asLong() + "/retificar"),
                    req, versao == 1 ? 201 : 200, "REM004 cadeia sucessiva sem aplicar as versões substituídas");
        }
        em.flush(); em.clear();
        var lista = consultar(c.localA, base(n.id()) + "/remanejamentos", "REM001/004/005 versões e metadados imutáveis");
        assertThat(lista.size()).isEqualTo(3); var ids = new HashSet<Long>();
        var itensEsperados = List.of(
                Map.of(n.composicao1(), List.of(25, -5, 20), n.composicao2(), List.of(75, 5, 80)),
                Map.of(n.composicao1(), List.of(25, -10, 15), n.composicao2(), List.of(75, 10, 85)),
                Map.of(n.composicao1(), List.of(25, -15, 10), n.composicao2(), List.of(75, 15, 90)));
        for (int i = 0; i < 3; i++) {
            var r = lista.get(i);
            assertThat(r.path("identificacao").asText()).isEqualTo("REM-TRES-VERSOES");
            var itensPorId = java.util.stream.StreamSupport.stream(r.path("itens").spliterator(), false)
                    .collect(java.util.stream.Collectors.toMap(item -> item.path("composicaoId").asLong(),
                            item -> List.of(item.path("quantidadeAnterior").asInt(), item.path("variacao").asInt(),
                                    item.path("quantidadeResultante").asInt())));
            assertThat(itensPorId).as("REM001/003 versão %s: todos os IDs e anterior/variação/resultante", i + 1)
                    .isEqualTo(itensEsperados.get(i));
             assertThat(ids.add(r.path("id").asLong())).isTrue();
            assertThat(r.path("versaoInstrumento").asInt()).isEqualTo(i + 1);
            assertThat(r.path("situacao").asText()).isEqualTo(i < 2 ? "RETIFICADO" : "VIGENTE");
            assertThat(r.path("usuarioResponsavelId").asLong()).isEqualTo(c.localA.getId());
            assertThat(r.path("justificativa").asText()).isEqualTo("Redistribuição da versão " + (i + 1));
            assertThat(r.path("dataEfetiva").asText()).isEqualTo(data.toString());
            assertThat(Instant.parse(r.path("registradoEm").asText())).isAfter(antes).isBefore(Instant.now().plusSeconds(1));
            assertThat(r.path("somaVariacoes").asInt()).isZero();
            assertThat(r.path("itens").size()).isEqualTo(2);
            assertThat(r.path("itens").get(0).path("variacao").asInt()).isEqualTo(-(i + 1) * 5);
        }
        assertThat(lista.get(0).path("referenciaSei").isNull()).isTrue();
        assertThat(lista.get(1).path("referenciaSei").asText()).isEqualTo("SEI-REM-2");
        assertThat(lista.get(2).path("referenciaSei").asText()).isEqualTo("SEI-REM-3");
        assertEfetivo(c, n, data.minusDays(1), 25, 75);
        assertEfetivo(c, n, data, 10, 90);
        var eventoId = lista.get(2).path("eventoId").asLong();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos", "REM001 possui tipo próprio").findValues("tipoEvento")
                .stream().map(JsonNode::asText)).contains("REMANEJAMENTO");
        assertThat(eventoId).isPositive();
        assertThat(consultar(c.localA, base(n.id()) + "/eventos/precedencias", "REM004 duas relações de retificação").size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aditamentos", "remanejamentos"})
    void instrumentoJaEfetivoNaoPodeSerCanceladoComoProgramacao(String recurso) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false); var data = RelogioSistema.hoje().minusDays(1);
        Map<String, Object> req; JsonNode ato;
        if (recurso.equals("aditamentos")) {
            req = aditamento("ADI-EFETIVO", data); req.put("novaDescricao", "Efeito já ocorrido"); ato = registrarAditamento(c, n.id(), req);
        } else {
            req = remanejamento(n, "REM-EFETIVO", data, 5); previaRemanejamento(c, n.id(), req);
            ato = enviar(c.localA, base(n.id()) + "/" + recurso, req, 201, "REM007 ato já efetivo");
        }
        enviar(c.localA, base(n.id()) + "/" + recurso + "/" + ato.path("id").asLong() + "/cancelar",
                mapa("motivo", "Não se pode cancelar como futuro um fato já ocorrido", "referenciaSei", "SEI-CANCEL"), 409,
                "ADI006/REM007 cancelamento de programação não apaga fato já efetivo");
        em.flush(); em.clear();
        var lista = consultar(c.localA, base(n.id()) + "/" + recurso, "Rejeição conserva único fato vigente");
        assertThat(lista.size()).isEqualTo(1); assertThat(lista.get(0).path("id").asLong()).isEqualTo(ato.path("id").asLong());
        assertThat(lista.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aditamentos", "remanejamentos"})
    void instrumentoRejeitaCombinacaoRepetidaSemPersistirFato(String recurso) throws Exception {
        var c = criarCenario(); var n = publicado(c, 100, 25, false); Map<String, Object> req;
        if (recurso.equals("aditamentos")) {
            req = aditamento("ADI-DUPLICADO", RelogioSistema.hoje()); req.put("novoEfetivoTotal", 100);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", n.composicao1(), "quantidadeResultante", 25),
                    mapa("composicaoId", n.composicao1(), "quantidadeResultante", 25)));
        } else {
            req = remanejamento(n, "REM-DUPLICADO", RelogioSistema.hoje(), 5);
            req.put("itens", List.of(mapa("composicaoId", n.composicao1(), "variacao", -5),
                    mapa("composicaoId", n.composicao1(), "variacao", 5)));
        }
        enviar(c.localA, base(n.id()) + "/" + recurso + "/previa", req, 409,
                "CMP002/REM003 a mesma combinação não pode aparecer duas vezes no instrumento");
        em.flush(); em.clear();
        assertThat(consultar(c.localA, base(n.id()) + "/" + recurso, "Nenhum instrumento duplicado persistido").isEmpty()).isTrue();
        assertEfetivo(c, n, RelogioSistema.hoje(), 25, 75);
    }

    private void assertEfetivo(Cenario c, NovoContrato n, java.time.LocalDate data, int primeiro, int segundo) throws Exception {
        var itens = consultar(c.localA, base(n.id()) + "/composicoes?data=" + data, "Distribuição temporal não duplica instrumentos retificados");
        assertThat(itens.size()).isEqualTo(2);
        assertThat(java.util.stream.StreamSupport.stream(itens.spliterator(), false)
                .map(item -> item.path("id").asLong()).toList())
                .containsExactlyInAnyOrder(n.composicao1(), n.composicao2());
        for (var item : itens) assertThat(item.path("efetivoPrevisto").asInt())
                .isEqualTo(item.path("id").asLong() == n.composicao1() ? primeiro : segundo);
    }

    private static final class FalhaAntesDoCommit extends RuntimeException {}
}
