package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import com.example.siggtrat.entidade.PermissaoDocumentoRestrito;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Autoria: revisor independente /root/revisao_final_transversais.
 * Base congelada C9: bb2bd19e2015cfdab1ca6bea8d07e7d038b820e5d59f2216eb44cf9144c085b6.
 * Hipóteses, ainda sem execução pelo autor: depois de bloquear o Contrato,
 * concederAcesso pode esperar no INSERT/FK do beneficiário, e revogarAcesso
 * no UPDATE da permissão, sem revalidar o Administrador que autorizou antes.
 * Ator, beneficiário e Administrador revogador são três usuários distintos.
 * Espera em registro e tabela exatos, commit independente e snapshots reais.
 * Sem DDL, mocks de banco/autorização, alterações de produção ou JVM pelo autor.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class Candidato9PermissoesDocumentoExecutorRevogadoRedIT extends ContratosHttpBase {
    private static final String BASE_SHA =
            "bb2bd19e2015cfdab1ca6bea8d07e7d038b820e5d59f2216eb44cf9144c085b6";
    @Autowired private DataSource dataSource;

    @ParameterizedTest(name = "permissaoDocumento={0}")
    @ValueSource(strings = {"CONCEDER", "REVOGAR"})
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void permissaoDocumentalRevalidaExecutorDepoisDaEsperaPosteriorAoContrato(String fluxo) throws Exception {
        Cenario c = criarCenarioPersistido();
        UsuarioAplicacao ator = emTransacao(() -> {
            UsuarioAplicacao novo = usuario(PerfilAcesso.ADMINISTRADOR, null);
            em.flush(); return novo;
        });
        assertThat(List.of(ator.getId(), c.admin.getId(), c.nacional.getId())).doesNotHaveDuplicates();
        DocumentoPreparado d = prepararDocumento(c);

        // O mesmo ator concede e revoga legalmente antes do interleaving.
        long controleId = conceder(ator, c.nacional.getId(), d.rota(), "controle");
        conferirPermissao(controleId, ator.getId(), c.nacional.getId(), true);
        enviar(ator, d.rota() + "/permissoes/" + controleId + "/revogar",
                motivoRevogacao("controle"), 200,
                "Controle positivo: o Administrador revoga o acesso que concedeu ao beneficiário distinto");
        conferirPermissao(controleId, ator.getId(), c.nacional.getId(), false);

        long permissaoAlvo = fluxo.equals("REVOGAR")
                ? conceder(ator, c.nacional.getId(), d.rota(), "alvo-da-espera") : controleId;
        if (fluxo.equals("REVOGAR"))
            conferirPermissao(permissaoAlvo, ator.getId(), c.nacional.getId(), true);
        String rotaTentativa = fluxo.equals("CONCEDER") ? d.rota() + "/permissoes"
                : d.rota() + "/permissoes/" + permissaoAlvo + "/revogar";
        var pedido = autenticar(comando(fluxo, c.nacional.getId(), rotaTentativa), ator);
        JsonNode antes = estado(d.id(), ator.getId(), d.rota());
        assertThat(antes.path("permissoes").size()).isEqualTo(fluxo.equals("CONCEDER") ? 1 : 2);

        String tabelaEsperada = fluxo.equals("CONCEDER") ? "usuario_aplicacao" : "documento_restrito_permissao";
        long registroBloqueado = fluxo.equals("CONCEDER") ? c.nacional.getId() : permissaoAlvo;
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch liberar = new CountDownLatch(1);
        CompletableFuture<Long> conexaoBloqueadora = new CompletableFuture<>();
        var atorJdbc = new Ator("Candidato9PermissoesDocumentoExecutorRevogadoRedIT-" + fluxo);
        try {
            Future<?> bloqueador = pool.submit(() -> emTransacao(() -> {
                Object alvo = fluxo.equals("CONCEDER")
                        ? em.find(UsuarioAplicacao.class, registroBloqueado, LockModeType.PESSIMISTIC_WRITE)
                        : em.find(PermissaoDocumentoRestrito.class, registroBloqueado, LockModeType.PESSIMISTIC_WRITE);
                assertThat(alvo).isNotNull();
                conexaoBloqueadora.complete(((Number) em.createNativeQuery("select pg_backend_pid()")
                        .getSingleResult()).longValue());
                try {
                    if (!liberar.await(50, TimeUnit.SECONDS))
                        throw new AssertionError("Limite da fixture: recurso documental não liberado");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt(); throw new IllegalStateException(ex);
                }
                return null;
            }));
            long conexao = conexaoBloqueadora.get(10, TimeUnit.SECONDS);
            Future<MvcResult> tentativa = pool.submit(() -> atorJdbc.executar(() -> mvc.perform(pedido).andReturn()));
            List<Map<String, Object>> espera = EsperaJdbcPostgres.aguardar(new JdbcTemplate(dataSource),conexao,atorJdbc,
                    sql -> sql.selecaoChave(tabelaEsperada,
                            fluxo.equals("CONCEDER") ? "id" : "id_documento_permissao",registroBloqueado),
                    tabelaEsperada+"/"+registroBloqueado,tentativa);
            Instant esperaObservadaEm = Instant.now();

            executar(c.admin, put("/api/usuarios/" + ator.getId()).contentType(APPLICATION_JSON)
                    .content(json(mapa("perfil", "GESTOR_NACIONAL"))), 200,
                    "Outro Administrador retira a autoridade documental e confirma antes de liberar a espera");
            JsonNode executorDepoisDaRevogacao = emTransacao(() -> {
                em.clear();
                UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, ator.getId());
                assertThat(atual.getPerfil()).isEqualTo(PerfilAcesso.GESTOR_NACIONAL);
                assertThat(atual.getActive()).isTrue();
                assertThat(em.find(UsuarioAplicacao.class, c.nacional.getId()).getId()).isNotEqualTo(atual.getId());
                return mapper.valueToTree(mapa("id", atual.getId(), "perfil", atual.getPerfil(),
                        "active", atual.getActive(), "version", atual.getVersion()));
            });
            Instant revogacaoConferidaEm = Instant.now();
            assertThat(tentativa.isDone()).as("A escrita ainda espera após o commit da retirada de autoridade").isFalse();
            Instant liberacaoEm = Instant.now();
            liberar.countDown();
            bloqueador.get(10, TimeUnit.SECONDS);
            MvcResult resultado = tentativa.get(15, TimeUnit.SECONDS);
            JsonNode depois = estado(d.id(), ator.getId(), d.rota());

            Path evidencia = Path.of("target/auditoria-http/candidato9-permissao-documento-executor-" + fluxo + ".json");
            Files.createDirectories(evidencia.getParent());
            Files.writeString(evidencia, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "audit_round", System.getenv("SGT_AUDITORIA_RODADA"), "recorded_at", Instant.now().toString(),
                    "base_source_manifest_sha256", BASE_SHA,
                    "hipotese", "C9-TR-PERMISSAO-DOCUMENTO-EXECUTOR-" + fluxo,
                    "fluxo", fluxo, "metodo", "POST", "rota", rotaTentativa,
                    "executorId", ator.getId(), "revogadorId", c.admin.getId(), "beneficiarioId", c.nacional.getId(),
                    "tabelaEsperada", tabelaEsperada, "registroBloqueado", registroBloqueado,
                    "esperaPostgresObservada", true, "esperaDataLockWaits", espera,
                    "esperaObservadaEm", esperaObservadaEm, "revogacaoConferidaEm", revogacaoConferidaEm,
                    "liberacaoEm", liberacaoEm, "revogacaoCommitadaAntesDaLiberacao", true,
                    "executorAposRevogacao", executorDepoisDaRevogacao,
                    "status", resultado.getResponse().getStatus(), "antes", antes, "depois", depois)));
            assertAll("A permissão documental exige autoridade corrente depois da espera: " + fluxo,
                    () -> assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404, 409),
                    () -> assertThat(depois).as("Permissões, documento, eventos e trilha permanecem integralmente iguais")
                            .isEqualTo(antes));
        } catch (AssertionError ex) {
            var comando = atorJdbc.comandoAtual();
            Path diagnostico = Path.of("target/auditoria-http/candidato9-permissao-espera-" + fluxo + ".json");
            Files.createDirectories(diagnostico.getParent());
            Files.writeString(diagnostico, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapa(
                    "erro", ex.getMessage(), "tabelaEsperada", tabelaEsperada,
                    "registroBloqueado", registroBloqueado,
                    "sqlPreparado", comando == null ? null : comando.sql(),
                    "bindingsSha256", comando == null ? null : comando.conjuntos(),
                    "atividade", comando == null ? List.of() : new JdbcTemplate(dataSource).queryForList(
                            "select pid,state,wait_event_type,wait_event,pg_blocking_pids(pid)::text as bloqueadores "
                                    + "from pg_stat_activity where datname=current_database() and pid=?", comando.pid()))));
            throw ex;
        } finally {
            liberar.countDown();
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private DocumentoPreparado prepararDocumento(Cenario c) throws Exception {
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 5, null, false); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
        });
        long publicacao = consultar(c.localA, base(contrato.id()) + "/eventos", "Origem publicada do documento sintético")
                .get(0).path("id").asLong();
        byte[] bytes = pdfSintetico("Fonte pessoal sintética para conferir autorização após espera");
        JsonNode documento = dados(executar(c.localA, multipart(base(contrato.id()) + "/documentos")
                .file(metadados(mapa("tipoFato", "CONTRATO_ORIGINAL", "fatoId", publicacao,
                        "fatoChave", "ORIGINAL", "titulo", "Documento pessoal sintético do RED C9",
                        "classificacaoAcesso", "PESSOAL_RESTRITO", "dataEfetiva", contrato.inicio(),
                        "chaveIdempotencia", "permissao-executor-c9-" + UUID.randomUUID())))
                .file(new MockMultipartFile("arquivo", "fonte.pdf", "application/pdf", bytes)),
                201, "Preparação explícita: documento restrito com arquivo PDF real"));
        long id = documento.path("id").asLong();
        assertThat(id).isPositive();
        assertThat(documento.path("sha256Arquivo").asText()).hasSize(64);
        return new DocumentoPreparado(id, base(contrato.id()) + "/documentos/" + id);
    }

    private long conceder(UsuarioAplicacao ator, long beneficiarioId, String rotaDocumento, String marcador) throws Exception {
        JsonNode criada = enviar(ator, rotaDocumento + "/permissoes", concessao(beneficiarioId, marcador), 200,
                "Controle positivo: Administrador concede ao beneficiário distinto, com finalidade e autorização explícitas");
        assertThat(criada.path("id").asLong()).isPositive();
        return criada.path("id").asLong();
    }

    private Map<String, Object> concessao(long beneficiarioId, String marcador) {
        return mapa("usuarioId", beneficiarioId, "finalidade", "Auditoria documental sintética " + marcador,
                "justificativa", "Fonte restrita autorizada para controle independente " + marcador,
                "autorizacaoAdministrativa", "ATO-SINTETICO-C9-" + marcador,
                "validoAte", Instant.now().plusSeconds(3600));
    }

    private Map<String, Object> motivoRevogacao(String marcador) {
        return mapa("motivo", "Retirada documental sintética " + marcador,
                "autorizacaoAdministrativa", "ATO-SINTETICO-REVOGACAO-C9-" + marcador);
    }

    private AbstractMockHttpServletRequestBuilder<?> comando(String fluxo, long beneficiarioId, String rota) throws Exception {
        Object body = fluxo.equals("CONCEDER") ? concessao(beneficiarioId, "tentativa-concorrente")
                : motivoRevogacao("tentativa-concorrente");
        return post(rota).contentType(APPLICATION_JSON).content(json(body));
    }

    private void conferirPermissao(long id, long atorId, long beneficiarioId, boolean ativa) {
        emTransacao(() -> {
            em.clear();
            PermissaoDocumentoRestrito p = em.find(PermissaoDocumentoRestrito.class, id);
            assertThat(p).isNotNull();
            assertThat(p.getUsuarioBeneficiario().getId()).isEqualTo(beneficiarioId);
            assertThat(p.getConcedidoPorId()).isEqualTo(atorId);
            assertThat(p.getConcedidoEm()).isNotNull();
            assertThat(p.getAtivo()).isEqualTo(ativa);
            if (ativa) assertThat(p.getRevogadoEm()).isNull();
            else {
                assertThat(p.getRevogadoEm()).isNotNull();
                assertThat(p.getRevogadoPorId()).isEqualTo(atorId);
            }
            return null;
        });
    }

    private JsonNode estado(long documentoId, long atorId, String rotaDocumento) {
        return emTransacao(() -> {
            em.clear();
            DocumentoComprobatorio d = em.find(DocumentoComprobatorio.class, documentoId);
            assertThat(d).isNotNull();
            Object documento = mapa("id", d.getId(), "grupo", d.getGrupoLogicoId(), "contrato", d.getContrato().getId(),
                    "fatoId", d.getFatoId(), "tipoFato", d.getTipoFato(), "estado", d.getEstado(),
                    "classificacao", d.getClassificacaoAcesso(), "versaoDocumento", d.getVersaoDocumento(),
                    "version", d.getVersion(), "sha256Arquivo", d.getSha256Arquivo(), "bytes", d.getTamanhoBytes(),
                    "nomeArmazenado", d.getNomeArmazenado(), "referenciaSei", d.getReferenciaSei(),
                    "quantidadeEventos", d.getQuantidadeEventosAuditoria(), "ultimoHash", d.getUltimoHashEventoAuditoria());
            var permissoes = em.createQuery("select p.id,p.documentoReferencia.id,p.grupoLogicoId,p.usuarioBeneficiario.id,"
                            + "p.finalidade,p.justificativa,p.autorizacaoAdministrativa,p.validoAte,p.ativo,p.concedidoEm,"
                            + "p.concedidoPorId,p.concedidoPorUsername,p.revogadoEm,p.revogadoPorId,p.revogadoPorUsername,"
                            + "p.motivoRevogacao,p.version from PermissaoDocumentoRestrito p "
                            + "where p.documentoReferencia.id=:d order by p.id", Object[].class)
                    .setParameter("d", documentoId).getResultList();
            var eventos = em.createQuery("select e.id,e.documento.id,e.grupoLogicoId,e.tipoEvento,e.dataEfetiva,"
                            + "e.registradoEm,e.responsavelId,e.responsavelUsername,e.motivo,e.estadoAnterior,e.estadoNovo,"
                            + "e.referenciaSeiAnterior,e.referenciaSeiNova,e.sha256Anterior,e.sha256Novo,e.hashAnterior,e.hashEvento "
                            + "from EventoDocumentoComprobatorio e where e.grupoLogicoId=:g order by e.id", Object[].class)
                    .setParameter("g", d.getGrupoLogicoId()).getResultList();
            var acessos = em.createQuery("select a.id from AcessoDocumentoRestrito a where a.documento.id=:d order by a.id", Long.class)
                    .setParameter("d", documentoId).getResultList();
            var trilha = em.createQuery("select a.id,a.action,a.entityType,a.entityId,a.detailsJson "
                            + "from RegistroAuditoria a where a.userId=:u and a.path like :p order by a.id", Object[].class)
                    .setParameter("u", atorId).setParameter("p", rotaDocumento + "/permissoes%").getResultList();
            return mapper.valueToTree(mapa("documento", documento, "permissoes", permissoes,
                    "eventos", eventos, "acessosIds", acessos, "trilhaDoRecurso", trilha));
        });
    }



    private byte[] pdfSintetico(String texto) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Document documento = new Document();
        PdfWriter.getInstance(documento, buffer);
        documento.open(); documento.add(new Paragraph(texto)); documento.close();
        return buffer.toByteArray();
    }

    private record DocumentoPreparado(long id, String rota) { }
}
