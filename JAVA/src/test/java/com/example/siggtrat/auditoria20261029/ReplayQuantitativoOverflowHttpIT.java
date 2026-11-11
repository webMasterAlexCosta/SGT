package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.AditamentoContratual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CMP-003:003/005. Dois itens, nenhum colaborador: uma inserção entre atos
 * válidos não pode fazer o delta posterior ultrapassar o total representável.
 * A soma de referência é long e os valores esperados são literais independentes.
 * RED/GREEN pendentes: este draft não foi compilado ou executado pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReplayQuantitativoOverflowHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    private static final String FUNDAMENTO = "Ato sintético específico de redução excepcional quantitativa";

    @ParameterizedTest(name = "{3}: intermediário={0}, comando={1}, posterior={2}")
    @CsvSource({"2147483642,409,30,overflow", "2147483637,201,2147483647,limite-exato"})
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void replayValidaSomaRealAntesDeRepresentarTotal(int quantidadeIntermediaria, int statusEsperado,
                                                    long totalPosteriorEsperado, String caso) throws Exception {
        Preparado p = preparar();
        var concessao = postar(p.c().admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "REDUCAO_CONTRATUAL_EXCEPCIONAL", "usuarioBeneficiarioId", p.c().localA.getId(),
                "contratoId", p.id(), "finalidade", "Auditoria quantitativa",
                "justificativa", "Ato sintético conferido", "autorizacaoAdministrativa", "ATO-QUANTITATIVO",
                "validoAte", Instant.now().plusSeconds(7200)), 201, "conceder");
        assertThat(dados(concessao).path("id").asLong()).isPositive();
        registrar(p, quantitativo(p, "EXCECAO-20-0", p.excecao(), 20, 0, 20, true));
        JsonNode posterior = dados(registrar(p, quantitativo(p, "POSTERIOR-20-10", p.posterior(), 20, 10, 30, true)));
        Corte original = corte(p, p.posterior());
        MetaPosterior metadadosPosterior = emTransacao(() -> {
            var a = em.find(AditamentoContratual.class, posterior.path("id").asLong());
            return new MetaPosterior(a.getNovoEfetivoTotal(), a.getReducaoExcepcional(), a.getImpactoConfirmado(),
                    a.getFundamentoExcecao() == null ? null : SnapshotQuantitativos.hash(a.getFundamentoExcecao()));
        });
        SnapshotQuantitativos.registrar(mapper, "overflow", "setup-posterior", mapa("contratoId", p.id(),
                "corteOriginal", original, "metadadosPosteriorPersistidos", metadadosPosterior));
        assertThat(original.quantidades()).containsEntry(p.a(), 20L).containsEntry(p.b(), 10L);
        assertThat(original.totalContrato()).isEqualTo(30);
        assertThat(posterior.path("novoEfetivoTotal").asInt()).isEqualTo(30);
        assertThat(posterior.path("reducaoExcepcional").asBoolean()).isTrue();
        assertThat(posterior.path("fundamentoExcecao").asText()).isEqualTo(FUNDAMENTO);
        assertThat(metadadosPosterior).isEqualTo(new MetaPosterior(30, true, true, SnapshotQuantitativos.hash(FUNDAMENTO)));
        var antes = SnapshotQuantitativos.capturar(dataSource, p.id(), null);
        assertThat(antes.tabelas().get("permissao_excepcional_uso").linhas()).hasSize(1);

        var req = quantitativo(p, "INTERMEDIARIO-" + caso, p.intermediario(), quantidadeIntermediaria, 0,
                quantidadeIntermediaria, false);
        // A prévia pode explicar a impossibilidade; o comando também deve
        // rejeitar pelo motivo quantitativo, nunca somente por token ausente.
        MvcResult previa = http(p.c().localA, base(p.id()) + "/aditamentos/previa", req);
        if (previa.getResponse().getStatus() == 200) req.put("tokenPrevia", dados(previa).path("tokenPrevia").asText());
        MvcResult resultado = http(p.c().localA, base(p.id()) + "/aditamentos", req);
        var depois = SnapshotQuantitativos.capturar(dataSource, p.id(), null);
        Corte noIntermediario = corte(p, p.intermediario());
        Corte noPosterior = corte(p, p.posterior());
        String mensagem = mapper.readTree(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("message").asText("");
        String minuscula = mensagem.toLowerCase(Locale.ROOT);
        boolean motivoQuantitativo = (minuscula.contains("efetivo") || minuscula.contains("soma")
                || minuscula.contains("represent") || minuscula.contains("limite num"))
                && !minuscula.contains("token") && !minuscula.contains("prévia") && !minuscula.contains("previa");
        SnapshotQuantitativos.registrar(mapper, "overflow", caso, mapa("contratoId", p.id(), "composicaoA", p.a(),
                "composicaoB", p.b(), "quantidadeIntermediaria", quantidadeIntermediaria,
                "somaMatematicaSeAplicadoPosterior", ((long) quantidadeIntermediaria) + 10L,
                "posteriorId", posterior.path("id").asLong(), "metadadosPosteriorPersistidos", metadadosPosterior,
                "antes", antes, "depois", depois,
                "previa", SnapshotQuantitativos.http(mapper, previa), "comando", SnapshotQuantitativos.http(mapper, resultado),
                "mensagemDominioNaoVazia", !mensagem.isBlank(), "motivoQuantitativo", motivoQuantitativo,
                "corteOriginal", original, "corteIntermediarioDepois", noIntermediario, "cortePosteriorDepois", noPosterior));

        assertAll("Replay não pode publicar total negativo por overflow nem confirmar escrita parcial",
                () -> assertThat(previa.getResponse().getStatus()).isIn(200, 409),
                () -> assertThat(resultado.getResponse().getStatus()).isEqualTo(statusEsperado),
                () -> assertThat(noIntermediario.statusComposicoes()).isEqualTo(200),
                () -> assertThat(noIntermediario.statusPosicao()).isEqualTo(200),
                () -> assertThat(noPosterior.statusComposicoes()).isEqualTo(200),
                () -> assertThat(noPosterior.statusPosicao()).isEqualTo(200),
                () -> assertThat(noPosterior.quantidades()).hasSize(2),
                () -> assertThat(noIntermediario.quantidades()).hasSize(2)
                        .containsEntry(p.a(), statusEsperado == 409 ? 20L : 2147483637L).containsEntry(p.b(), 0L),
                () -> assertThat(noPosterior.somaReal()).isEqualTo(totalPosteriorEsperado),
                () -> assertThat(noPosterior.totalContrato()).isEqualTo(totalPosteriorEsperado),
                () -> assertThat(noPosterior.quantidades()).containsEntry(p.a(), statusEsperado == 409 ? 20L : 2147483637L)
                        .containsEntry(p.b(), 10L),
                () -> assertThat(noIntermediario.somaReal()).isEqualTo(statusEsperado == 409 ? 20L : 2147483637L),
                () -> assertThat(noIntermediario.totalContrato()).isEqualTo(noIntermediario.somaReal()),
                () -> assertThat(depois.tabelas().get("permissao_excepcional_uso"))
                        .as("Aumento não consome de novo a autorização da redução anterior")
                        .isEqualTo(antes.tabelas().get("permissao_excepcional_uso")),
                () -> {
                    if (statusEsperado == 409) {
                        assertThat(mensagem).isNotBlank();
                        assertThat(motivoQuantitativo).as("409 precisa explicar o impedimento matemático, não falha de prévia").isTrue();
                        assertThat(depois).as("Instrumentos, itens, eventos, versões, projeção, permissões e dependências intactos").isEqualTo(antes);
                    } else {
                        assertThat(depois.tabelas().get("aditamento_contratual").linhas()).hasSize(3);
                        assertThat(depois.tabelas().get("composicao_contratual"))
                                .as("Aditamento mantém as bases publicadas imutáveis").isEqualTo(antes.tabelas().get("composicao_contratual"));
                    }
                });
    }

    private Preparado preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate inicio = RelogioSistema.hoje().plusDays(2);
        long cargoB = dados(postar(c.localA, "/api/cargos", mapa("nome", "Cargo replay numérico " + UUID.randomUUID(), "ativo", true),
                200, "cargoB")).path("id").asLong();
        long id = dados(postar(c.localA, "/api/contratos", rascunho(c, "OVER-" + UUID.randomUUID().toString().substring(0, 8),
                100, inicio, inicio.plusDays(60)), 201, "rascunho")).path("id").asLong();
        long a = dados(postar(c.localA, base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 50), 201, "baseA50")).path("id").asLong();
        long b = dados(postar(c.localA, base(id) + "/composicoes", composicao(c, cargoB, 50), 201, "baseB50")).path("id").asLong();
        postar(c.localA, base(id) + "/publicar", mapa("dataPublicacao", RelogioSistema.hoje(),
                "referenciaSei", "SEI-OVERFLOW-BASE", "justificativa", "Base sintética 50/50 publicada"), 200, "publicar100");
        return new Preparado(c, id, a, b, inicio.plusDays(3), inicio.plusDays(6), inicio.plusDays(9));
    }

    private Map<String, Object> quantitativo(Preparado p, String identificacao, LocalDate data, int a, int b, int total,
                                            boolean excepcional) {
        var req = aditamento(identificacao, data);
        req.put("novoEfetivoTotal", total);
        req.put("alteracoesComposicao", List.of(mapa("composicaoId", p.a(), "quantidadeResultante", a),
                mapa("composicaoId", p.b(), "quantidadeResultante", b)));
        if (excepcional) { req.put("reducaoExcepcional", true); req.put("fundamentoExcecao", FUNDAMENTO); }
        return req;
    }

    private MvcResult registrar(Preparado p, Map<String, Object> req) throws Exception {
        var previa = postar(p.c().localA, base(p.id()) + "/aditamentos/previa", req, 200, "previa-setup");
        req.put("tokenPrevia", dados(previa).path("tokenPrevia").asText());
        return postar(p.c().localA, base(p.id()) + "/aditamentos", req, 201, "aditamento-setup");
    }

    private MvcResult http(UsuarioAplicacao usuario, String path, Object body) throws Exception {
        return mvc.perform(autenticar(post(path).contentType(APPLICATION_JSON).content(json(body)), usuario)).andReturn();
    }

    private MvcResult postar(UsuarioAplicacao usuario, String path, Object body, int esperado, String etapa) throws Exception {
        var resultado = http(usuario, path, body);
        SnapshotQuantitativos.registrar(mapper, "overflow", "setup-" + etapa, SnapshotQuantitativos.http(mapper, resultado));
        assertThat(resultado.getResponse().getStatus()).as("Pré-condição HTTP: %s", etapa).isEqualTo(esperado);
        return resultado;
    }

    private Corte corte(Preparado p, LocalDate data) throws Exception {
        var comps = mvc.perform(autenticar(get(base(p.id()) + "/composicoes").param("data", data.toString()), p.c().localA)).andReturn();
        var posicao = mvc.perform(autenticar(get(base(p.id()) + "/posicao-temporal").param("data", data.toString()), p.c().localA)).andReturn();
        Map<Long, Long> quantidades = new LinkedHashMap<>();
        JsonNode distribuicao = dados(comps);
        if (distribuicao.isArray()) for (var item : distribuicao)
            quantidades.put(item.path("id").asLong(), item.path("efetivoPrevisto").asLong());
        long soma = quantidades.values().stream().mapToLong(Long::longValue).sum();
        return new Corte(data, comps.getResponse().getStatus(), posicao.getResponse().getStatus(), Map.copyOf(quantidades), soma,
                dados(posicao).path("versaoContratual").path("efetivoPrevisto").asLong(Long.MIN_VALUE));
    }

    private record Preparado(Cenario c, long id, long a, long b, LocalDate excecao, LocalDate intermediario, LocalDate posterior) { }
    private record MetaPosterior(Integer totalOriginal, Boolean excepcional, Boolean impactoConfirmado, String fundamentoSha256) { }
    private record Corte(LocalDate data, int statusComposicoes, int statusPosicao, Map<Long, Long> quantidades, long somaReal, long totalContrato) { }
}
