package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** REM-003:001: o comando final também recusa criação implícita, após commits reais. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RemanejamentoComposicaoInexistenteHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void postFinalRecusaComposicaoInexistenteSemGravarEControleValidoPersiste() throws Exception {
        var c = criarCenarioPersistido();
        var inicio = RelogioSistema.hoje().plusDays(2);
        long contrato = dados(postar(c.localA, "/api/contratos", rascunho(c,
                "REM-SEM-COMPOSICAO-" + UUID.randomUUID().toString().substring(0, 8),
                100, inicio, inicio.plusMonths(4)), 201)).path("id").asLong();
        long cargoB = dados(postar(c.localA, "/api/cargos",
                mapa("nome", "Cargo REM controle " + UUID.randomUUID(), "ativo", true), 200)).path("id").asLong();
        long a = dados(postar(c.localA, base(contrato) + "/composicoes",
                composicao(c, c.cargoA.getId(), 25), 201)).path("id").asLong();
        long b = dados(postar(c.localA, base(contrato) + "/composicoes",
                composicao(c, cargoB, 75), 201)).path("id").asLong();
        postar(c.localA, base(contrato) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-SINTETICO-REM-BASE", "justificativa", "Publicação sintética explícita"), 200);

        var antes = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        var consultaAntes = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(contrato) + "/composicoes?data=" + inicio, "REM003 duas composições publicadas"));
        var invalida = requisicao("REM-INEXISTENTE", inicio.plusDays(1), a, Long.MAX_VALUE);
        String endpoint = base(contrato) + "/remanejamentos";
        var previa = http(c.localA, endpoint + "/previa", invalida);
        var depoisPrevia = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        var comando = http(c.localA, endpoint, invalida);
        var depoisComando = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        var consultaDepois = AssercoesComposicoesHttp.capturar(() -> consultar(c.localA,
                base(contrato) + "/composicoes?data=" + inicio, "REM003 nenhum item criado implicitamente"));
        var instrumentos = consultar(c.localA, endpoint, "REM003 nenhuma gravação parcial");
        var resumoPrevia = SnapshotQuantitativos.http(mapper, previa);
        var resumoComando = SnapshotQuantitativos.http(mapper, comando);
        SnapshotQuantitativos.registrar(mapper, "rem-inexistente", "negativas", mapa(
                "antes", antes, "depoisPrevia", depoisPrevia, "depoisComando", depoisComando,
                "previa", resumoPrevia, "comandoFinal", resumoComando,
                "composicoesAntesSha", SnapshotQuantitativos.hashJson(mapper, consultaAntes.dados()),
                "composicoesDepoisSha", SnapshotQuantitativos.hashJson(mapper, consultaDepois.dados()),
                "instrumentosSha", SnapshotQuantitativos.hashJson(mapper, instrumentos)));

        String motivo = mapper.readTree(comando.getResponse().getContentAsString()).path("message").asText()
                .toLowerCase(Locale.ROOT);
        assertAll("A recusa final é da composição ausente e não de um token de prévia",
                () -> assertThat(previa.getResponse().getStatus()).isEqualTo(400),
                () -> assertThat(comando.getResponse().getStatus()).isEqualTo(400),
                () -> assertThat(motivo).contains("composi").doesNotContain("token", "prévia", "previa"),
                () -> assertThat(antes.tabelas().get("composicao_contratual").linhas()).hasSize(2),
                () -> assertThat(depoisPrevia).isEqualTo(antes),
                () -> assertThat(depoisComando).isEqualTo(antes),
                () -> AssercoesComposicoesHttp.exigirPreservacaoComConsultaPosterior(consultaAntes, consultaDepois),
                () -> assertThat(instrumentos).isEmpty());

        // A mesma sessão e as duas composições existentes permitem uma redistribuição real.
        var valida = requisicao("REM-CONTROLE-VALIDO", inicio.plusDays(1), a, b);
        var previaValida = postar(c.localA, endpoint + "/previa", valida, 200);
        valida.put("tokenPrevia", dados(previaValida).path("tokenPrevia").asText());
        var aceito = postar(c.localA, endpoint, valida, 201);
        var persistido = SnapshotQuantitativos.capturar(dataSource, contrato, null);
        var projetadas = consultar(c.localA, base(contrato) + "/composicoes?data=" + inicio.plusDays(1),
                "REM003 controle válido relido após commit");
        var basePersistida = persistido.tabelas().get("composicao_contratual");
        SnapshotQuantitativos.registrar(mapper, "rem-inexistente", "controle-positivo", mapa(
                "comando", SnapshotQuantitativos.http(mapper, aceito), "persistido", persistido,
                "projetadasSha", SnapshotQuantitativos.hashJson(mapper, projetadas)));
        assertAll("Controle positivo distingue autorização/CSRF de recusa negocial",
                () -> assertThat(dados(aceito).path("id").asLong()).isPositive(),
                () -> assertThat(persistido.tabelas().get("remanejamento_contratual").linhas()).hasSize(1),
                () -> assertThat(persistido.tabelas().get("remanejamento_composicao").linhas()).hasSize(2),
                () -> assertThat(basePersistida).isEqualTo(antes.tabelas().get("composicao_contratual")),
                () -> assertThat(projetadas).hasSize(2),
                () -> assertThat(quantidade(projetadas, a)).isEqualTo(20),
                () -> assertThat(quantidade(projetadas, b)).isEqualTo(80));
    }

    private Map<String, Object> requisicao(String identificacao, java.time.LocalDate data, long origem, long destino) {
        return mapa("identificacao", identificacao, "dataEfetiva", data,
                "justificativa", "Redistribuição sintética sem criar composição", "confirmarImpacto", true,
                "itens", List.of(mapa("composicaoId", origem, "variacao", -5),
                        mapa("composicaoId", destino, "variacao", 5)));
    }

    private MvcResult http(UsuarioAplicacao usuario, String endpoint, Object corpo) throws Exception {
        return mvc.perform(autenticar(post(endpoint).contentType(APPLICATION_JSON).content(json(corpo)), usuario)).andReturn();
    }

    private MvcResult postar(UsuarioAplicacao usuario, String endpoint, Object corpo, int esperado) throws Exception {
        var resultado = http(usuario, endpoint, corpo);
        SnapshotQuantitativos.registrar(mapper, "rem-inexistente", "http-setup-controle",
                SnapshotQuantitativos.http(mapper, resultado));
        assertThat(resultado.getResponse().getStatus()).as("Pré-condição HTTP %s", endpoint).isEqualTo(esperado);
        return resultado;
    }

    private int quantidade(com.fasterxml.jackson.databind.JsonNode composicoes, long id) {
        for (var item : composicoes) if (item.path("id").asLong() == id) return item.path("efetivoPrevisto").asInt();
        throw new AssertionError("Composição esperada ausente da consulta");
    }
}
