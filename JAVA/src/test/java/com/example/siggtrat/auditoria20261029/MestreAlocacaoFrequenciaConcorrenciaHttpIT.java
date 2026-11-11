package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dto.AlteracaoAlocacaoVinculoRequisicao;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.entidade.EventoFrequencia;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.VinculoContratual;
import com.example.siggtrat.servico.AlocacaoVinculoServico;
import com.example.siggtrat.servico.FrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * MASTER CON005/CON007. Movimentos com prévias do mesmo estado conflitam;
 * intenções distintas de frequência podem produzir legitimamente v1 -> v2.
 * Não há precondição de versão no DTO de escrita de frequência; não inventar409.
 * O 409 adicional usa colisão real: chave de A/FALTA reapresentada como PRESENCA.
 * Dois workers, barreira após método real/flush, observação da PK Contrato.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
class MestreAlocacaoFrequenciaConcorrenciaHttpIT extends MestreConcorrenciaSuporte {
    @MockitoSpyBean private AlocacaoVinculoServico alocacoes;
    @MockitoSpyBean private FrequenciaServico frequencias;

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void con005DoisMovimentosNaMesmaDataComPreviasPropriasConfirmamSoAVencedora() throws Exception {
        Cenario c = criarCenarioPersistido(); UsuarioAplicacao segundo = segundoGestor(c);
        // Uma origem e dois destinos diferentes, todos publicados e com vagas.
        BaseContrato n = rascunhoPublicado(c, List.of(5, 5, 5), RelogioSistema.hoje(), null, true);
        long origem = n.composicoes().get(0), destinoA = n.composicoes().get(1), destinoB = n.composicoes().get(2);
        var vinculo = dados(postar(c.localA, "/api/vinculos-contratuais", mapa("contratoId", n.id(), "composicaoId", origem,
                "dataInicio", n.inicio(), "pessoa", mapa("nome", "Pessoa sintética do gate CON005",
                        "cpf", cpf(Math.toIntExact(690_000_000L + n.id())))), 201, "con005-vinculo"));
        long idVinculo = vinculo.path("id").asLong();
        long pessoa = emTransacao(() -> em.find(VinculoContratual.class, idVinculo).getPessoa().getId());
        LocalDate efeito = n.inicio().plusDays(1);
        Map<String, Object> reqA = movimento(efeito, destinoA, "A"), reqB = movimento(efeito, destinoB, "B");
        String rota = "/api/vinculos-contratuais/" + idVinculo;
        var previaA = dados(postar(c.localA, rota + "/previas/alocacoes", reqA, 200, "con005-previa-A"));
        var previaB = dados(postar(segundo, rota + "/previas/alocacoes", reqB, 200, "con005-previa-B"));
        assertThat(previaA.path("incompatibilidades")).isEmpty();
        assertThat(previaB.path("incompatibilidades")).isEmpty();
        assertThat(previaA.path("token").asText()).isNotBlank();
        assertThat(previaB.path("token").asText()).isNotBlank();
        reqA.put("tokenPrevia", previaA.path("token").asText()); reqA.put("confirmarImpacto", true);
        reqB.put("tokenPrevia", previaB.path("token").asText()); reqB.put("confirmarImpacto", true);
        String ra = UUID.randomUUID().toString(), rb = UUID.randomUUID().toString();
        var a = autenticar(post(rota + "/alocacoes").header("X-Request-Id", ra).contentType(APPLICATION_JSON).content(json(reqA)), c.localA);
        var b = autenticar(post(rota + "/alocacoes").header("X-Request-Id", rb).contentType(APPLICATION_JSON).content(json(reqB)), segundo);
        var antes = SnapshotQuantitativos.capturar(dataSource, n.id(), pessoa);
        List<Faixa> faixasAntes = faixas(idVinculo);
        List<EventoAlocacao> eventosAntes = eventosAlocacao(idVinculo);
        assertThat(faixasAntes).hasSize(1);
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<AlocacaoVinculoServico>getUltimateTargetObject(alocacoes))
                .alterar(eq(idVinculo), any(AlteracaoAlocacaoVinculoRequisicao.class));
        Corrida r = disputar(gate, a, b, sql -> sql.selecaoChave("contrato", "id_contrato", n.id()), "CON005 Contrato das duas mudanças");
        var depois = SnapshotQuantitativos.capturar(dataSource, n.id(), pessoa);
        List<Faixa> faixasDepois = faixas(idVinculo);
        List<EventoAlocacao> eventosDepois = eventosAlocacao(idVinculo);
        List<EventoAlocacao> novos = eventosDepois.stream().filter(e -> !eventosAntes.contains(e)).toList();
        var relidas = consultar(c.localA, rota + "/alocacoes", "Faixas reais após os dois commits/rollbacks");
        registrar("con005", mapa("antes", antes, "depois", depois, "corrida", resumo(r), "data", efeito,
                "origemId", origem, "destinoA", destinoA, "destinoB", destinoB, "atorA", c.localA.getId(), "atorB", segundo.getId(),
                "previaASha", hJson(previaA), "previaBSha", hJson(previaB), "faixasAntes", faixasAntes, "faixasDepois", faixasDepois,
                "eventosAntes", eventosAntes, "eventosDepois", eventosDepois, "releituraHttpSha", hJson(relidas)));
        long vencedora = id(r.a());
        Faixa anterior = faixasAntes.get(0);
        assertAll(() -> conferirCorrida(r, 200, 409),
                () -> assertThat(faixasDepois).hasSize(2),
                () -> assertThat(faixasDepois.stream().filter(f -> f.id() == vencedora).toList()).singleElement().satisfies(f -> {
                    assertThat(f.composicaoId()).isEqualTo(destinoA); assertThat(f.inicio()).isEqualTo(efeito);
                    assertThat(f.fim()).isEqualTo(anterior.fim()); assertThat(f.autor()).isEqualTo(c.localA.getId());
                    assertThat(f.estado()).isEqualTo("REGISTRADA");
                }),
                () -> assertThat(faixasDepois.stream().filter(f -> f.id() == anterior.id()).toList()).singleElement()
                        .satisfies(f -> { assertThat(f.fim()).isEqualTo(efeito.minusDays(1)); assertThat(f.composicaoId()).isEqualTo(origem); }),
                () -> assertThat(faixasDepois.stream().filter(f -> Long.valueOf(destinoB).equals(f.composicaoId())).toList()).isEmpty(),
                () -> assertThat(eventosDepois).containsAll(eventosAntes),
                () -> assertThat(novos).hasSize(2),
                () -> assertThat(novos).extracting(EventoAlocacao::tipo).containsExactly("ALTERACAO", "AJUSTE_LIMITE_VINCULO"),
                () -> assertThat(novos).extracting(EventoAlocacao::autor).containsExactly(c.localA.getId(), c.localA.getId()),
                () -> assertThat(novos.stream().filter(e -> e.tipo().equals("ALTERACAO")).toList()).singleElement().satisfies(e -> {
                    assertThat(e.autor()).isEqualTo(c.localA.getId()); assertThat(e.tipo()).isEqualTo("ALTERACAO");
                    assertThat(e.composicaoNova()).isEqualTo(destinoA); assertThat(e.alocacaoNova()).isEqualTo(vencedora);
                }),
                () -> assertThat(novos.stream().filter(e -> e.tipo().equals("AJUSTE_LIMITE_VINCULO")).toList()).singleElement()
                        .satisfies(e -> { assertThat(e.alocacaoNova()).isEqualTo(anterior.id()); assertThat(e.composicaoNova()).isEqualTo(origem); }),
                () -> assertThat(novos.get(1).anterior()).isEqualTo(novos.get(0).hash()),
                () -> assertThat(depois.tabelas().get("contrato")).isEqualTo(antes.tabelas().get("contrato")),
                () -> assertThat(depois.tabelas().get("pessoa")).isEqualTo(antes.tabelas().get("pessoa")),
                () -> assertThat(depois.tabelas().get("permissao_excepcional_uso")).isEqualTo(antes.tabelas().get("permissao_excepcional_uso")));
    }

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    void con007ChavesDistintasCriamDuasVersoesLegitimasEColisaoRealRecusaSemEfeitos() throws Exception {
        Cenario c = criarCenarioPersistido();
        LocalDate data = DataJornadaSintetica.ultimoDiaDeTrabalhoAnterior(c.jornadaA);
        String chaveA = "MESTRE-FREQ-A-" + UUID.randomUUID(), chaveB = "MESTRE-FREQ-B-" + UUID.randomUUID();
        String ra = UUID.randomUUID().toString(), rb = UUID.randomUUID().toString(), rn = UUID.randomUUID().toString();
        var reqA = frequencia(c, data, "FALTA", chaveA);
        var reqB = frequencia(c, data, "PRESENCA", chaveB);
        // Nenhum id/versão precondicional é inventado. São duas intenções novas.
        var a = autenticar(post("/api/frequencias").header("X-Request-Id", ra).contentType(APPLICATION_JSON).content(json(reqA)), c.operacionalA);
        var b = autenticar(post("/api/frequencias").header("X-Request-Id", rb).contentType(APPLICATION_JSON).content(json(reqB)), c.operacionalA);
        var negativa = autenticar(post("/api/frequencias").header("X-Request-Id", rn).contentType(APPLICATION_JSON)
                .content(json(frequencia(c, data, "PRESENCA", chaveA))), c.operacionalA);
        assertThat(emTransacao(() -> em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:id and f.dataReferencia=:data", Long.class)
                .setParameter("id", c.vinculoA.getId()).setParameter("data", data).getSingleResult())).isZero();
        Barreira gate = new Barreira();
        doAnswer(i -> coordenar(i, gate)).when(AopTestUtils.<FrequenciaServico>getUltimateTargetObject(frequencias)).salvarOuAtualizar(any(FrequenciaDTO.class));
        Corrida r = disputar(gate, a, b, sql -> sql.selecaoChave("contrato", "id_contrato", c.contratoA.getId()), "CON007 Contrato do mesmo Vínculo/data");
        List<String> requests = List.of(ra, rb, rn);
        registrar("con007-corrida", mapa("corrida", resumo(r), "estadoPersistido", snapshotFrequencia(c, requests)));
        conferirCorrida(r, 200, 200);
        long idA = id(r.a()), idB = id(r.b());
        FatoFrequencia fato = fatoFrequencia(c, data);
        List<EventoFreq> eventos = eventosFrequencia(idA);
        var integridade = consultar(c.localA, "/api/frequencias/" + idA + "/evidencias-integridade", "Cadeia v1/v2 calculada pelo servidor");
        var antesNegativa = snapshotFrequencia(c, requests);
        var recusa = mvc.perform(negativa).andReturn();
        var depoisNegativa = snapshotFrequencia(c, requests);
        FatoFrequencia fatoDepois = fatoFrequencia(c, data);
        List<EventoFreq> eventosDepois = eventosFrequencia(idA);
        registrar("con007", mapa("corrida", resumo(r), "chaveASha", h(chaveA), "chaveBSha", h(chaveB),
                "respostaNegativa", SnapshotQuantitativos.http(mapper, recusa), "fatoAposDuasIntencoes", fato,
                "fatoAposColisao", fatoDepois, "eventosAntesColisao", eventos, "eventosDepoisColisao", eventosDepois,
                "integridadeServidor", integridade.path("cadeiaIntegra").asBoolean(), "integridadeSha", hJson(integridade),
                "antesColisao", antesNegativa, "depoisColisao", depoisNegativa));
        assertAll(() -> assertThat(idA).isPositive(), () -> assertThat(idB).isEqualTo(idA),
                () -> assertThat(dados(r.a().http()).path("versaoNegocial").asInt()).isEqualTo(1),
                () -> assertThat(dados(r.b().http()).path("versaoNegocial").asInt()).isEqualTo(2),
                () -> assertThat(fato.id()).isEqualTo(idA), () -> assertThat(fato.versaoNegocial()).isEqualTo(2),
                () -> assertThat(fato.status()).isEqualTo("PRESENCA"),
                () -> assertThat(fato.autorOriginal()).isEqualTo(c.operacionalA.getId()),
                () -> assertThat(eventos).hasSize(2),
                () -> assertThat(eventos).extracting(EventoFreq::versao).containsExactly(1, 2),
                () -> assertThat(eventos).extracting(EventoFreq::autor).containsExactly(c.operacionalA.getId(), c.operacionalA.getId()),
                () -> assertThat(eventos.get(0).chaveSha()).isEqualTo(h(chaveA)),
                () -> assertThat(eventos.get(1).chaveSha()).isEqualTo(h(chaveB)),
                () -> assertThat(eventos.get(1).anterior()).isEqualTo(eventos.get(0).hash()),
                () -> assertThat(integridade.path("cadeiaIntegra").asBoolean()).isTrue(),
                () -> assertThat(recusa.getResponse().getStatus()).isEqualTo(409),
                () -> assertThat(depoisNegativa).as("A mesma chave com outra intenção não altera nenhum conjunto observado").isEqualTo(antesNegativa),
                () -> assertThat(fatoDepois).isEqualTo(fato), () -> assertThat(eventosDepois).isEqualTo(eventos));
    }

    private Map<String, Object> movimento(LocalDate data, long composicao, String nome) {
        return mapa("dataEfetiva", data, "composicaoId", composicao, "justificativa", "Movimento sintético mestre " + nome,
                "evidencia", "SEI-MESTRE-ALOCACAO-" + nome, "autorizacao", "ATO-MESTRE-" + nome);
    }

    private List<Faixa> faixas(long id) {
        return emTransacao(() -> em.createQuery("select a from AlocacaoVinculo a where a.vinculo.id=:id order by a.id", AlocacaoVinculo.class)
                .setParameter("id", id).getResultList().stream().map(a -> new Faixa(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                        a.getDataInicio(), a.getDataFim(), a.getCriadoPorId(), a.getEstadoNegocial().name(), a.getVersion())).toList());
    }

    private List<EventoAlocacao> eventosAlocacao(long id) {
        return emTransacao(() -> em.createQuery("select e from EventoAlocacaoVinculo e where e.vinculo.id=:id order by e.id", EventoAlocacaoVinculo.class)
                .setParameter("id", id).getResultList().stream().map(e -> new EventoAlocacao(e.getId(), e.getTipoEvento().name(), e.getResponsavelId(),
                        e.getAlocacaoNova() == null ? null : e.getAlocacaoNova().getId(), e.getComposicaoNovaId(), e.getHashAnterior(), e.getHashEvento())).toList());
    }

    private Map<String, Object> frequencia(Cenario c, LocalDate data, String status, String chave) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status, "chaveIdempotencia", chave);
    }

    private FatoFrequencia fatoFrequencia(Cenario c, LocalDate data) {
        return emTransacao(() -> {
            var fatos = em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:id and f.dataReferencia=:data", Frequencia.class)
                    .setParameter("id", c.vinculoA.getId()).setParameter("data", data).getResultList();
            if (fatos.size() != 1) return new FatoFrequencia(-1, fatos.size(), null, null, null);
            Frequencia f = fatos.get(0);
            return new FatoFrequencia(f.getId(), fatos.size(), f.getStatus(), f.getVersaoNegocial(), f.getAutorOriginalId());
        });
    }

    private List<EventoFreq> eventosFrequencia(long id) {
        return emTransacao(() -> em.createQuery("select e from EventoFrequencia e where e.frequencia.id=:id order by e.numeroVersao,e.id", EventoFrequencia.class)
                .setParameter("id", id).getResultList().stream().map(e -> new EventoFreq(e.getId(), e.getNumeroVersao(), e.getUsuarioId(),
                        h(e.getChaveIdempotencia()), e.getHashIntencao(), e.getHashAnterior(), e.getHashEvento())).toList());
    }

    private Map<String, Object> snapshotFrequencia(Cenario c, List<String> requests) {
        Map<String, Object> x = new LinkedHashMap<>();
        try { x.put("contratoPessoaVinculoAlocacoes", SnapshotQuantitativos.capturar(dataSource, c.contratoA.getId(), c.pessoaA.getId())); }
        catch (java.sql.SQLException e) { throw new IllegalStateException("Snapshot PostgreSQL não coletado", e); }
        x.put("frequencias", linhas("select to_jsonb(f)::text from frequencia f where f.id_vinculo_contratual=:id order by f.id", "id", c.vinculoA.getId()));
        x.put("eventos", linhas("select to_jsonb(e)::text from evento_frequencia e where e.id_frequencia in "
                + "(select f.id from frequencia f where f.id_vinculo_contratual=:id) order by e.id_evento_frequencia", "id", c.vinculoA.getId()));
        x.put("historico", linhas("select to_jsonb(h)::text from historico_empregado h where h.id_empregado=:id order by h.id_historico", "id", c.empregadoA.getId()));
        descarregarAuditoria();
        x.put("auditoriaNegocio", emTransacao(() -> em.createNativeQuery("select to_jsonb(a)::text from registro_auditoria a "
                        + "where a.request_id in :requests and a.entity_type in ('Frequencia','EventoFrequencia','HistoricoEmpregado') order by a.id", String.class)
                .setParameter("requests", requests).getResultList().stream().map(v -> h(v.toString())).toList()));
        return Map.copyOf(x);
    }

    private record Faixa(long id, Long composicaoId, LocalDate inicio, LocalDate fim, Long autor, String estado, long versao) { }
    private record EventoAlocacao(long id, String tipo, Long autor, Long alocacaoNova, Long composicaoNova, String anterior, String hash) { }
    private record FatoFrequencia(long id, long quantidadeDeLinhas, String status, Integer versaoNegocial, Long autorOriginal) { }
    private record EventoFreq(long id, Integer versao, Long autor, String chaveSha, String intencaoSha, String anterior, String hash) { }
}
