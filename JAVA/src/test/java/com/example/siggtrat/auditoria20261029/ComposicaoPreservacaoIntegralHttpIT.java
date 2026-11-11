package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CMP-006:003. Mantém os oráculos de ReplayCapacidadeDependenteHttpIT e de
 * CancelamentoCapacidadeContratualHttpIT, acrescentando SELECT* tipado/hasheado
 * das linhas integrais do Contrato e da Pessoa obtida do vínculo afetado.
 * Quatro variantes reais, sem transação de teste que esconda o commit HTTP.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ComposicaoPreservacaoIntegralHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "retroativo aditamento={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void retroativoIncompativelPreservaContratoPessoaEVerdadePosteriorInteiros(boolean aditamento) throws Exception {
        Cenario c = criarCenarioPersistido();
        Base n = publicar(c, List.of(90, 10, 0));
        LocalDate d1 = n.inicio().plusDays(3), d2 = n.inicio().plusDays(8);
        long a = n.composicoes().get(0), b = n.composicoes().get(1), destino = n.composicoes().get(2);
        var posterior = mapa("identificacao", "REM-POSTERIOR-SUSTENTA-C", "dataEfetiva", d2,
                "justificativa", "Transferir dez vagas para C", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", b, "variacao", -10), mapa("composicaoId", destino, "variacao", 10)));
        JsonNode rem = registrar(c, n.id(), "remanejamentos", posterior);
        String nomePessoa = "Pessoa sintética dependente do replay " + n.id();
        Alvo alvo = vincular(c, n.id(), destino, d2, nomePessoa);
        var leituraAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + d2, "CMP006 capacidade C10 antes da tentativa"));
        assertThat(quantidade(leituraAntes.dados(), destino)).isEqualTo(10);
        var remAntes = consultar(c.localA, base(n.id()) + "/remanejamentos", "Instrumento posterior vigente");
        assertThat(remAntes).hasSize(1);
        assertThat(remAntes.get(0).path("situacao").asText()).isEqualTo("VIGENTE");
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "História antes da tentativa");
        var antes = SnapshotQuantitativos.capturar(dataSource, n.id(), alvo.pessoaId());
        String familia = aditamento ? "aditamentos" : "remanejamentos";
        Map<String, Object> anterior;
        if (aditamento) {
            anterior = aditamento("ADI-ANTERIOR-RETIRA-B", d1);
            anterior.put("novoEfetivoTotal", 100);
            anterior.put("alteracoesComposicao", List.of(mapa("composicaoId", b, "quantidadeResultante", 0),
                    mapa("composicaoId", a, "quantidadeResultante", 100)));
        } else {
            anterior = mapa("identificacao", "REM-ANTERIOR-RETIRA-B", "dataEfetiva", d1,
                    "justificativa", "Transferir B para A", "confirmarImpacto", true,
                    "itens", List.of(mapa("composicaoId", b, "variacao", -10), mapa("composicaoId", a, "variacao", 10)));
        }
        JsonNode previa = prever(c, n.id(), familia, anterior);
        MvcResult recusada = http(c.localA, base(n.id()) + "/" + familia, anterior);
        var depois = SnapshotQuantitativos.capturar(dataSource, n.id(), alvo.pessoaId());
        String caso = "retroativo-" + (aditamento ? "adi" : "rem");
        boolean pessoaNoAlerta = previa.path("alertas").toString().contains(nomePessoa);
        SnapshotQuantitativos.registrar(mapper, "preservacao", caso + "-persistencia", mapa("contratoId", n.id(),
                "vinculoId", alvo.vinculoId(), "pessoaId", alvo.pessoaId(), "antes", antes, "depois", depois,
                "resposta", SnapshotQuantitativos.http(mapper, recusada), "pessoaRealNoAlerta", pessoaNoAlerta,
                "previaSha256", SnapshotQuantitativos.hashJson(mapper, previa)));

        var eventosDepois = consultar(c.localA, base(n.id()) + "/eventos", "Recusa não grava evento parcial");
        var leituraDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + d2, "Capacidade posterior preservada"));
        var remDepois = consultar(c.localA, base(n.id()) + "/remanejamentos", "Instrumento posterior preservado");
        var vinculoDepois = consultar(c.localA, "/api/vinculos-contratuais/" + alvo.vinculoId(), "Vínculo real não foi movido");
        SnapshotQuantitativos.registrar(mapper, "preservacao", caso + "-oraculos", mapa("persistenciaAntes", antes,
                "persistenciaDepois", depois, "eventosAntesSha", SnapshotQuantitativos.hashJson(mapper, eventosAntes),
                "eventosDepoisSha", SnapshotQuantitativos.hashJson(mapper, eventosDepois),
                "remAntesSha", SnapshotQuantitativos.hashJson(mapper, remAntes), "remDepoisSha", SnapshotQuantitativos.hashJson(mapper, remDepois),
                "composicoesAntesSha", SnapshotQuantitativos.hashJson(mapper, leituraAntes.dados()),
                "composicoesDepoisSha", SnapshotQuantitativos.hashJson(mapper, leituraDepois.dados()),
                "vinculoDepoisSha", SnapshotQuantitativos.hashJson(mapper, vinculoDepois)));
        assertAll("Oráculos anteriores e preservação integral das linhas realmente afetadas",
                () -> assertThat(recusada.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(previa.path("possuiImpactoOcupacao").asBoolean()).isTrue(),
                () -> assertThat(pessoaNoAlerta).isTrue(),
                () -> assertThat(eventosDepois).isEqualTo(eventosAntes),
                () -> AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(leituraAntes, leituraDepois),
                () -> assertThat(remDepois).hasSize(1),
                () -> assertThat(remDepois.get(0).path("id").asLong()).isEqualTo(rem.path("id").asLong()),
                () -> assertThat(remDepois.get(0).path("situacao").asText()).isEqualTo("VIGENTE"),
                () -> assertThat(vinculoDepois.path("composicaoInicialId").asLong()).isEqualTo(destino),
                () -> exigirLinhasIntegrais(antes, depois, n.id(), alvo.pessoaId()));
    }

    @ParameterizedTest(name = "cancelamento remanejamento={0}")
    @ValueSource(booleans = {false, true})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void cancelamentoIncompativelPreservaContratoPessoaInstrumentoECapacidadeInteiros(boolean remanejamento) throws Exception {
        Cenario c = criarCenarioPersistido();
        Base n = publicar(c, List.of(100, 0));
        long a = n.composicoes().get(0), b = n.composicoes().get(1);
        LocalDate efeito = n.inicio().plusDays(3), inicioPessoa = efeito.plusDays(1);
        String familia = remanejamento ? "remanejamentos" : "aditamentos";
        Map<String, Object> req;
        if (remanejamento) {
            req = mapa("identificacao", "CAPACIDADE-FUTURA-REM", "dataEfetiva", efeito,
                    "justificativa", "Redistribuição explícita", "confirmarImpacto", true,
                    "itens", List.of(mapa("composicaoId", a, "variacao", -1), mapa("composicaoId", b, "variacao", 1)));
        } else {
            req = aditamento("CAPACIDADE-FUTURA-ADI", efeito); req.put("novoEfetivoTotal", 101);
            req.put("alteracoesComposicao", List.of(mapa("composicaoId", b, "quantidadeResultante", 1)));
        }
        JsonNode instrumento = registrar(c, n.id(), familia, req);
        Alvo alvo = vincular(c, n.id(), b, inicioPessoa, "Pessoa sintética dependente da capacidade " + n.id());
        var eventosAntes = consultar(c.localA, base(n.id()) + "/eventos", "História antes do cancelamento");
        var instrumentosAntes = consultar(c.localA, base(n.id()) + "/" + familia, "Fotografia integral do original");
        assertThat(instrumentosAntes).hasSize(1);
        assertThat(instrumentosAntes.get(0)).isEqualTo(instrumento);
        var leituraAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + inicioPessoa, "Composição disponível no início futuro"));
        var antes = SnapshotQuantitativos.capturar(dataSource, n.id(), alvo.pessoaId());
        MvcResult recusada = http(c.localA, base(n.id()) + "/" + familia + "/" + instrumento.path("id").asLong() + "/cancelar",
                mapa("motivo", "Cancelamento retiraria a capacidade da pessoa", "referenciaSei", "SEI-CANCELAMENTO-CAPACIDADE"));
        var depois = SnapshotQuantitativos.capturar(dataSource, n.id(), alvo.pessoaId());
        String caso = "cancelamento-" + (remanejamento ? "rem" : "adi");
        SnapshotQuantitativos.registrar(mapper, "preservacao", caso + "-persistencia", mapa("contratoId", n.id(),
                "pessoaId", alvo.pessoaId(), "vinculoId", alvo.vinculoId(), "antes", antes, "depois", depois,
                "resposta", SnapshotQuantitativos.http(mapper, recusada)));
        var eventosDepois = consultar(c.localA, base(n.id()) + "/eventos", "Cancelamento recusado não produz evento");
        var leituraDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(n.id()) + "/composicoes?data=" + inicioPessoa, "Capacidade permanece publicada"));
        var instrumentosDepois = consultar(c.localA, base(n.id()) + "/" + familia, "Original continua vigente");
        var vinculoDepois = consultar(c.localA, "/api/vinculos-contratuais/" + alvo.vinculoId(), "Vínculo não é apagado ou movido");
        SnapshotQuantitativos.registrar(mapper, "preservacao", caso + "-oraculos", mapa("antes", antes, "depois", depois,
                "eventosAntesSha", SnapshotQuantitativos.hashJson(mapper, eventosAntes), "eventosDepoisSha", SnapshotQuantitativos.hashJson(mapper, eventosDepois),
                "instrumentosAntesSha", SnapshotQuantitativos.hashJson(mapper, instrumentosAntes),
                "instrumentosDepoisSha", SnapshotQuantitativos.hashJson(mapper, instrumentosDepois),
                "composicoesAntesSha", SnapshotQuantitativos.hashJson(mapper, leituraAntes.dados()),
                "composicoesDepoisSha", SnapshotQuantitativos.hashJson(mapper, leituraDepois.dados()),
                "vinculoDepoisSha", SnapshotQuantitativos.hashJson(mapper, vinculoDepois)));
        assertAll("Cancelamento preserva os oráculos anteriores e as linhas completas",
                () -> assertThat(recusada.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(eventosDepois).isEqualTo(eventosAntes),
                () -> AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(leituraAntes, leituraDepois),
                () -> assertThat(instrumentosDepois).hasSize(1),
                () -> assertThat(instrumentosDepois).isEqualTo(instrumentosAntes),
                () -> assertThat(instrumentosDepois.get(0).path("situacao").asText()).isEqualTo("VIGENTE"),
                () -> assertThat(vinculoDepois.path("composicaoInicialId").asLong()).isEqualTo(b),
                () -> assertThat(vinculoDepois.path("dataInicio").asText()).isEqualTo(inicioPessoa.toString()),
                () -> assertThat(vinculoDepois.path("dataFim").isNull()).isTrue(),
                () -> assertThat(vinculoDepois.path("estadoNegocial").asText()).isEqualTo("REGISTRADO"),
                () -> exigirLinhasIntegrais(antes, depois, n.id(), alvo.pessoaId()));
    }

    private Base publicar(Cenario c, List<Integer> quantidades) throws Exception {
        LocalDate inicio = RelogioSistema.hoje().plusDays(2), fim = inicio.plusMonths(4);
        long id = dados(postar(c.localA, "/api/contratos", rascunho(c, "INTEGRAL-" + UUID.randomUUID().toString().substring(0, 8),
                100, inicio, fim), 201, "rascunho")).path("id").asLong();
        List<Long> ids = new ArrayList<>();
        // O tamanho é fixo pelo cenário (dois/três itens), não pelo efetivo.
        for (int i = 0; i < quantidades.size(); i++) {
            long cargo = i == 0 ? c.cargoA.getId() : dados(postar(c.localA, "/api/cargos",
                    mapa("nome", "Cargo preservação " + UUID.randomUUID(), "ativo", true), 200, "cargo")).path("id").asLong();
            ids.add(dados(postar(c.localA, base(id) + "/composicoes", composicao(c, cargo, quantidades.get(i)), 201, "composicao")).path("id").asLong());
        }
        postar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-INTEGRAL-BASE", "justificativa", "Base integral sintética"), 200, "publicar");
        return new Base(id, List.copyOf(ids), inicio, fim);
    }

    private Alvo vincular(Cenario c, long contrato, long composicao, LocalDate inicio, String nome) throws Exception {
        var vinculo = dados(postar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", contrato, "composicaoId", composicao,
                "dataInicio", inicio, "pessoa", mapa("nome", nome, "cpf", cpf(Math.toIntExact(650_000_000L + contrato)))),
                201, "vinculo")).path("id").asLong();
        long pessoa = emTransacao(() -> em.find(VinculoContratual.class, vinculo).getPessoa().getId());
        assertThat(pessoa).as("ID da Pessoa vem do vínculo realmente persistido neste cenário").isPositive();
        return new Alvo(vinculo, pessoa);
    }

    private JsonNode prever(Cenario c, long id, String familia, Map<String, Object> req) throws Exception {
        JsonNode previa = dados(postar(c.localA, base(id) + "/" + familia + "/previa", req, 200, "previa"));
        req.put("tokenPrevia", previa.path("tokenPrevia").asText());
        return previa;
    }

    private JsonNode registrar(Cenario c, long id, String familia, Map<String, Object> req) throws Exception {
        prever(c, id, familia, req);
        return dados(postar(c.localA, base(id) + "/" + familia, req, 201, "instrumento"));
    }

    private MvcResult http(UsuarioAplicacao usuario, String path, Object body) throws Exception {
        return mvc.perform(autenticar(post(path).contentType(APPLICATION_JSON).content(json(body)), usuario)).andReturn();
    }

    private MvcResult postar(UsuarioAplicacao usuario, String path, Object body, int esperado, String etapa) throws Exception {
        var resultado = http(usuario, path, body);
        SnapshotQuantitativos.registrar(mapper, "preservacao", "setup-" + etapa, SnapshotQuantitativos.http(mapper, resultado));
        assertThat(resultado.getResponse().getStatus()).as("Pré-condição HTTP: %s", etapa).isEqualTo(esperado);
        return resultado;
    }

    private void exigirLinhasIntegrais(SnapshotQuantitativos.Estado antes, SnapshotQuantitativos.Estado depois,
                                       long contrato, long pessoa) {
        assertAll(() -> assertThat(antes.tabelas().get("contrato").linhas()).extracting(SnapshotQuantitativos.Linha::id).containsExactly(contrato),
                () -> assertThat(antes.tabelas().get("pessoa").linhas()).extracting(SnapshotQuantitativos.Linha::id).containsExactly(pessoa),
                () -> assertThat(antes.tabelas().get("contrato").colunas()).isNotEmpty(),
                () -> assertThat(antes.tabelas().get("pessoa").colunas()).isNotEmpty(),
                () -> assertThat(depois).as("Igualdade cobre todas as colunas, inclusive novas, versões e valores não expostos em DTO").isEqualTo(antes));
    }

    private int quantidade(JsonNode composicoes, long id) {
        for (var item : composicoes) if (item.path("id").asLong() == id) return item.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição esperada ausente da consulta");
    }

    private record Base(long id, List<Long> composicoes, LocalDate inicio, LocalDate fim) { }
    private record Alvo(long vinculoId, long pessoaId) { }
}
