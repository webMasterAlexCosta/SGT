package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.ObservadorAtorPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Pessoa;
import com.example.siggtrat.entidade.VinculoContratual;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * RED de contrato da incorporação documental nominal: ausência, prova válida,
 * prova tardia, conhecimento anterior, escopo, idempotência e concorrência.
 * A prova não modifica Pessoa.nome nem os eventos/fotografias originais.
 * PostgreSQL 18.6 e Redis reais; nenhuma transação externa envolve os comandos HTTP.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {"spring.cache.type=redis", "spring.sql.init.mode=never",
        "app.cache.redis.http-get.enabled=true"})
class PessoasProvaNomeHistoricoHttpIT extends ContratosHttpBase {
    @Autowired private DataSource dataSource;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private Environment ambiente;

    @BeforeEach
    void infraestruturaReal() throws Exception {
        try (var c = dataSource.getConnection()) {
            assertThat(c.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(c.getMetaData().getDatabaseProductVersion()).matches("18\\.6(?: \\(Debian [^()]+\\))?");
        }
        assertThat(ambiente.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(ambiente.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(ambiente.getProperty("spring.cache.type")).isEqualTo("redis");
        try (var c = redis.getConnection()) { assertThat(c.ping()).isEqualTo("PONG"); }
    }

    @Test
    void fonteAusentePermaneceDesconhecidaSemEmprestarNomeAtual() throws Exception {
        Cenario c = criarCenarioPersistido();
        JsonNode desconhecido = resolver(c, DATA, null);
        assertThat(desconhecido.path("comprovado").asBoolean()).isFalse();
        assertThat(desconhecido.path("nome").isNull()).isTrue();
        assertThat(desconhecido.path("fonteId").isNull()).isTrue();
        assertThat(consultar(c.localA, rota(c), "Ausência não cria prova no primeiro GET")).isEmpty();
        assertThat(contarEventosOriginais(c)).isZero();
    }

    @Test
    void fonteTardiaEnriqueceVerdadeVigenteSemReescreverOrigemOuConhecimentoAnterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        assertThat(resolver(c, DATA, null).path("nome").isNull()).isTrue();
        Origem antes = origem(c);
        Instant conhecidoAntes = Instant.now();
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = requisicao(c, "Nome documental antigo", DATA.minusDays(1), DATA.plusDays(1), null);
        JsonNode recibo = registrar(c, req);
        assertThat(recibo.path("registradoEm").asText()).isNotBlank();
        assertThat(Instant.parse(recibo.path("registradoEm").asText())).isAfter(conhecidoAntes);
        JsonNode comprovado = resolver(c, DATA, null);
        assertThat(comprovado.path("nome").asText()).isEqualTo("Nome documental antigo");
        assertThat(comprovado.path("comprovado").asBoolean()).isTrue();
        assertThat(comprovado.path("fonteId").asLong()).isEqualTo(recibo.path("id").asLong());
        assertThat(comprovado.path("inicioComprovado").asText()).isEqualTo(DATA.minusDays(1).toString());
        assertThat(comprovado.path("fimComprovado").asText()).isEqualTo(DATA.plusDays(1).toString());
        assertThat(resolver(c, DATA, conhecidoAntes).path("nome").isNull()).isTrue();
        assertThat(resolver(c, DATA.minusDays(2), null).path("nome").isNull()).isTrue();
        assertThat(resolver(c, DATA.plusDays(2), null).path("nome").isNull()).isTrue();
        assertThat(origem(c)).as("Cadastro atual, limites e evento original não são reescritos pela prova tardia")
                .isEqualTo(antes);
    }

    @Test
    void reenvioDaMesmaIntencaoNaoDuplicaProvaNemConsomeNovaVersao() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null); // A ausência desta operação é o RED inicial na baseline.
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = requisicao(c, "Nome documental único", DATA, DATA.plusDays(3), null);
        confirmar(c, req);
        JsonNode primeira = enviar(c.localA, rota(c), req, 201, "Primeira incorporação nominal");
        var versoes = versoes(c);
        JsonNode repetida = enviar(c.localA, rota(c), req, 201, "Reenvio não depende de versão já consumida");
        assertThat(repetida.path("id").asLong()).isEqualTo(primeira.path("id").asLong());
        assertThat(repetida.path("reenvio").asBoolean()).isTrue();
        assertThat(versoes(c)).isEqualTo(versoes);
        assertThat(consultar(c.localA, rota(c), "Uma única prova após reenvio")).hasSize(1);
        req.put("nomeComprovado", "Outra intenção com a mesma chave");
        enviar(c.localA, rota(c), req, 409, "Chave idempotente não pode representar outra intenção");
        assertThat(versoes(c)).isEqualTo(versoes);
    }

    @Test
    void provaConflitanteExigeRelacaoExplicitaERetificacaoPreservaConsultaAnterior() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null);
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        JsonNode original = registrar(c, requisicao(c, "Nome da primeira fonte", DATA, DATA.plusDays(2), null));
        Instant conhecimentoPrimeira = Instant.now();
        Map<String, Object> conflito = requisicao(c, "Nome de fonte conflitante", DATA, DATA.plusDays(2), null);
        enviar(c.localA, rota(c) + "/previa", conflito, 409,
                "Duas provas conflitantes no mesmo intervalo não se resolvem por ID ou timestamp");
        JsonNode nova = registrar(c, requisicao(c, "Nome da retificação documental", DATA, DATA.plusDays(2),
                original.path("id").asLong()));
        assertThat(nova.path("provaAnteriorId").asLong()).isEqualTo(original.path("id").asLong());
        assertThat(resolver(c, DATA, null).path("nome").asText()).isEqualTo("Nome da retificação documental");
        assertThat(resolver(c, DATA, conhecimentoPrimeira).path("nome").asText()).isEqualTo("Nome da primeira fonte");
        JsonNode cadeia = consultar(c.localA, rota(c), "Prova original e retificação continuam consultáveis");
        assertThat(cadeia).hasSize(2);
        assertThat(cadeia.get(0).path("nomeComprovado").asText()).isEqualTo("Nome da primeira fonte");
        assertThat(contarEventosOriginais(c)).isZero();
    }

    @Test
    void escopoConcessaoEValidacoesImpedemIncorporacaoParcial() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null);
        Map<String, Object> req = requisicao(c, "Nome documental", DATA, DATA.plusDays(2), null);
        enviar(c.localA, rota(c) + "/previa", req, 403, "Gestor Local sem concessão específica não incorpora prova");
        enviar(c.nacional, rota(c) + "/previa", req, 403, "Consulta nacional não concede escrita documental");
        enviar(c.localB, rota(c) + "/previa", req, 404, "Outra Gestão não prepara prova da Pessoa/Vínculo alheios");
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        req.put("referenciaSei", null);
        enviar(c.localA, rota(c) + "/previa", req, 400, "Fonte é obrigatória; texto de motivo não substitui SEI/documento");
        req.put("referenciaSei", "SEI-SINTETICO-PROVA"); req.put("dataFimEfetiva", DATA.minusDays(1));
        enviar(c.localA, rota(c) + "/previa", req, 400, "Intervalo invertido não é corrigido silenciosamente");
        req.put("dataFimEfetiva", DATA.plusDays(2)); req.put("nomeComprovado", " ");
        enviar(c.localA, rota(c) + "/previa", req, 400, "Uma fonte vazia não certifica um nome");
        assertThat(consultar(c.localA, rota(c), "Nenhuma prova após tentativas negadas")).isEmpty();
        assertThat(contarEventosOriginais(c)).isZero();
    }

    @Test
    void previaNominalObsoletaNaoGravaProvaDepoisDeOutraAlteracaoDaPessoa() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null);
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> req = requisicao(c, "Nome documental validado", DATA, DATA.plusDays(2), null);
        confirmar(c, req);
        executar(c.localA, put("/api/pessoas/" + c.pessoaA.getId() + "/dados-opcionais")
                .contentType(MediaType.APPLICATION_JSON).content(json(mapa("rg", "RG-SINTETICO-ALTERADO"))),
                200, "Outro escritor confirmou mudança relevante da Pessoa depois da prévia");
        enviar(c.localA, rota(c), req, 409, "Versão/assinatura obsoleta exige nova análise e confirmação");
        assertThat(consultar(c.localA, rota(c), "Negação não deixa prova")).isEmpty();
        assertThat(resolver(c, DATA, null).path("nome").isNull()).isTrue();
    }

    @Test
    void documentoRestritoExigeFinalidadeAuditaAcessoENaoPermiteFonteDeOutroContrato() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null);
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        long documentoA = documentoRestrito(c, false);
        long documentoB = documentoRestrito(c, true);
        Map<String, Object> req = requisicao(c, "Nome conferido no documento pessoal", DATA, DATA.plusDays(2), null);
        req.put("referenciaSei", null); req.put("documentoComprobatorioId", documentoA);
        enviar(c.localA, rota(c) + "/previa", req, 400, "Fonte pessoal exige finalidade de acesso documental explícita");
        req.put("finalidadeAcessoDocumento", "Conferir identidade nominal no período documentado para a incorporação autorizada");
        req.put("documentoComprobatorioId", documentoB);
        enviar(c.localA, rota(c) + "/previa", req, 404, "Concessão do Contrato A não torna o documento do Contrato B acessível");
        assertThat(consultar(c.localA, rota(c), "Falhas documentais não gravaram prova")).isEmpty();
        req.put("documentoComprobatorioId", documentoA);
        registrar(c, req);
        assertThat(resolver(c, DATA, null).path("nome").asText()).isEqualTo("Nome conferido no documento pessoal");
        assertThat(emTransacao(() -> em.createQuery("select count(a) from AcessoDocumentoRestrito a "
                        + "where a.documento.id=:d and a.usuarioId=:u and a.finalidade=:f", Long.class)
                .setParameter("d", documentoA).setParameter("u", c.localA.getId())
                .setParameter("f", req.get("finalidadeAcessoDocumento")).getSingleResult()))
                .as("Prévia/confirmação não contornam a trilha de acesso do documento restrito").isGreaterThanOrEqualTo(1L);
        JsonNode minimizada = consultar(c.nacional, rota(c), "Consulta de prova não concede acesso ao conteúdo documental restrito").get(0);
        assertThat(minimizada.path("motivo").isNull()).isTrue();
        assertThat(minimizada.path("autorizacao").isNull()).isTrue();
        assertThat(minimizada.path("referenciaSei").isNull()).isTrue();
        assertThat(minimizada.path("documentoComprobatorioId").isNull()).isTrue();
    }

    @Test
    void primeirasProvasConcorrentesAguardamMesmoContratoEConservamUmaUnicaVerdade() throws Exception {
        Cenario c = criarCenarioPersistido();
        resolver(c, DATA, null);
        conceder(c, c.contratoA.getId(), "INCORPORACAO_PROVA_HISTORICA");
        Map<String, Object> primeira = requisicao(c, "Nome concorrente um", DATA, DATA.plusDays(2), null);
        Map<String, Object> segunda = requisicao(c, "Nome concorrente dois", DATA, DATA.plusDays(2), null);
        confirmar(c, primeira); confirmar(c, segunda);
        var request1 = autenticar(post(rota(c)).contentType(MediaType.APPLICATION_JSON).content(json(primeira)), c.localA);
        var request2 = autenticar(post(rota(c)).contentType(MediaType.APPLICATION_JSON).content(json(segunda)), c.localA);
        var pool = Executors.newFixedThreadPool(3);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, c.contratoA.getId(), LockModeType.PESSIMISTIC_WRITE);
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try { if (!liberar.await(15, TimeUnit.SECONDS)) throw new AssertionError("Limite de espera da fixture"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                return null;
            }));
            long idConexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            var ator1 = new Ator("confirmacao-nome-1"); var ator2 = new Ator("confirmacao-nome-2");
            Future<MvcResult> tentativa1 = pool.submit(() -> ator1.executar(() -> mvc.perform(request1).andReturn()));
            Future<MvcResult> tentativa2 = pool.submit(() -> ator2.executar(() -> mvc.perform(request2).andReturn()));
            aguardarDuasEsperasReais(idConexao, c.contratoA.getId(), ator1, ator2, tentativa1, tentativa2);
            liberar.countDown(); bloqueador.get(10, TimeUnit.SECONDS);
            assertThat(List.of(tentativa1.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    tentativa2.get(10, TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(201, 409);
        } finally {
            liberar.countDown(); pool.shutdownNow();
        }
        assertThat(consultar(c.localA, rota(c), "Somente uma primeira prova foi confirmada")).hasSize(1);
        assertThat(resolver(c, DATA, null).path("nome").asText()).isIn("Nome concorrente um", "Nome concorrente dois");
        assertThat(contarEventosOriginais(c)).isZero();
    }

    private void aguardarDuasEsperasReais(long bloqueador,long contratoId,Ator ator1,Ator ator2,
                                        Future<?> a,Future<?> b) throws Exception {
        com.example.siggtrat.infra.EsperaJdbcPostgres.aguardarAmbos(new JdbcTemplate(dataSource),bloqueador,
                ator1,sql->sql.selecaoChave("contrato","id_contrato",contratoId),
                ator2,sql->sql.selecaoChave("contrato","id_contrato",contratoId),
                "contrato/"+contratoId,a,b);
    }

    private String rota(Cenario c) { return "/api/pessoas/" + c.pessoaA.getId() + "/provas-nome"; }

    private long documentoRestrito(Cenario c, boolean outroContrato) throws Exception {
        long contratoId = outroContrato ? c.contratoB.getId() : c.contratoA.getId();
        long vinculoId = outroContrato ? c.vinculoB.getId() : c.vinculoA.getId();
        var metadados = new MockMultipartFile("metadados", "metadados.json", MediaType.APPLICATION_JSON_VALUE,
                json(mapa("tipoFato", "VINCULO", "fatoId", vinculoId, "fatoChave", "VINCULO-" + vinculoId,
                        "titulo", "Fonte pessoal nominal sintética", "descricao", "Documento delimita nome e período",
                        "referenciaSei", "SEI-PESSOAL-SINTETICO-" + UUID.randomUUID(),
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", DATA,
                        "chaveIdempotencia", UUID.randomUUID().toString())).getBytes(StandardCharsets.UTF_8));
        return dados(executar(outroContrato ? c.localB : c.localA,
                multipart("/api/contratos/" + contratoId + "/documentos").file(metadados), 201,
                "Documento restrito publicado no vínculo canônico explícito")).path("id").asLong();
    }

    private JsonNode resolver(Cenario c, LocalDate data, Instant conhecimento) throws Exception {
        var req = get("/api/pessoas/" + c.pessoaA.getId() + "/nome-historico")
                .param("vinculoId", c.vinculoA.getId().toString()).param("data", data.toString());
        if (conhecimento != null) req.param("conhecidoAte", conhecimento.toString());
        return dados(executar(c.localA, req, 200, "Nome histórico usa data efetiva e corte de conhecimento explícitos"));
    }

    private Map<String, Object> requisicao(Cenario c, String nome, LocalDate inicio, LocalDate fim, Long anterior) throws Exception {
        var vs = versoes(c);
        return mapa("vinculoId", c.vinculoA.getId(), "dataInicioEfetiva", inicio, "dataFimEfetiva", fim,
                "nomeComprovado", nome, "referenciaSei", "SEI-SINTETICO-PROVA-NOMINAL", "documentoComprobatorioId", null,
                "motivo", "Fonte tardia expressamente delimita o nome e o período",
                "autorizacao", "ATO-SINTETICO-PROVA-NOMINAL", "provaAnteriorId", anterior,
                "versaoPessoaEsperada", vs.pessoa(), "versaoVinculoEsperada", vs.vinculo(),
                "chaveIdempotencia", UUID.randomUUID().toString());
    }

    private void confirmar(Cenario c, Map<String, Object> req) throws Exception {
        JsonNode previa = enviar(c.localA, rota(c) + "/previa", req, 200, "Prévia documental escopada");
        req.put("confirmarImpacto", true); req.put("tokenPrevia", previa.path("tokenPrevia").asText());
    }

    private JsonNode registrar(Cenario c, Map<String, Object> req) throws Exception {
        confirmar(c, req);
        return enviar(c.localA, rota(c), req, 201, "Prova nominal publicada preserva fatos anteriores");
    }

    private Versoes versoes(Cenario c) throws Exception {
        return new Versoes(consultar(c.localA, "/api/pessoas/" + c.pessoaA.getId(), "Versão da Pessoa").path("version").asLong(),
                consultar(c.localA, "/api/vinculos-contratuais/" + c.vinculoA.getId(), "Versão do Vínculo").path("version").asLong());
    }

    private long contarEventosOriginais(Cenario c) {
        return emTransacao(() -> em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                .setParameter("v", c.vinculoA.getId()).getSingleResult());
    }

    private Origem origem(Cenario c) {
        return emTransacao(() -> {
            Pessoa pessoa = em.find(Pessoa.class, c.pessoaA.getId());
            VinculoContratual vinculo = em.find(VinculoContratual.class, c.vinculoA.getId());
            return new Origem(pessoa.getNome(), pessoa.getCpf(), vinculo.getDataInicio(), vinculo.getDataFim(),
                    em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:v", Long.class)
                            .setParameter("v", c.vinculoA.getId()).getSingleResult());
        });
    }

    private record Versoes(long pessoa, long vinculo) {}
    private record Origem(String nomeAtual, String cpfAtual, LocalDate inicio, LocalDate fim, long eventos) {}
}
