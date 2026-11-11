package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.infra.EsperaJdbcPostgres;
import com.example.siggtrat.infra.ObservadorAtorPostgres.Ator;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** DIF-02, PER-010, DOC-009/010, AUD-001/005: prova HTTP e releitura PostgreSQL após commit. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OcorrenciaAutorizacaoTemporalHttpIT extends ContratosHttpBase {
    @org.springframework.beans.factory.annotation.Autowired private javax.sql.DataSource dataSource;
    @Autowired HistoricoIntegridadeServico integridade;
    @Autowired com.example.siggtrat.servico.OcorrenciaServico ocorrencias;
    @Autowired com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo armazenamento;

    private record Contexto(Cenario c, long contrato, long empregado, long vinculo,
                            Jornada turnoDestino, Lotacao lotacaoDestino, UsuarioAplicacao operadorDestino,
                            LocalDate mudanca, LocalDate fato) {}

    @Test
    void consultaEListaUsamLotacaoETurnoEfetivosMesmoComProjecaoDefasada() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "NEGOCIAL", "consulta-temporal-" + UUID.randomUUID());
        long id = o.path("id").asLong();
        executar(x.c.operacionalA, get("/api/ocorrencias/{id}", id), 403,
                "DIF-02 operador da antiga Lotação/Turno não lê fato posterior à mudança");
        JsonNode visto = dados(executar(x.operadorDestino, get("/api/ocorrencias/{id}", id), 200,
                "DIF-02 operador do contexto efetivo encontra o fato apesar da projeção antiga"));
        assertEquals(id, visto.path("ocorrencia").path("id").asLong());
        JsonNode antiga = dados(executar(x.c.operacionalA, get("/api/ocorrencias")
                .param("data", x.fato.toString()).param("contratoId", Long.toString(x.contrato))
                .param("lotacaoId", x.c.lotacaoA.getId().toString()).param("size", "1"), 200,
                "Listagem e total não incluem ocorrências de outro contexto temporal"));
        assertEquals(0, antiga.path("totalElements").asLong());
        JsonNode atual = dados(executar(x.operadorDestino, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado)).param("data", x.fato.toString())
                .param("lotacaoId", x.lotacaoDestino.getId().toString())
                .param("jornadaId", x.turnoDestino.getId().toString()).param("size", "1"), 200,
                "Filtros e paginação usam o contexto da data, não os campos atuais de Empregado"));
        assertEquals(1, atual.path("totalElements").asLong());
        assertEquals(id, atual.path("content").get(0).path("id").asLong());
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, id).getCanceladoEm()));
        executar(x.c.localB, delete("/api/ocorrencias/{id}", id), 403,
                "Cancelamento fora da Gestão falha sem alteração persistida");
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, id).getCanceladoEm()));
    }

    @Test
    void anexoNegocialRevalidaContextoEfetivoAteDepoisDoCancelamento() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "NEGOCIAL", "anexo-temporal-" + UUID.randomUUID());
        long id = o.path("id").asLong(), anexo = o.path("anexos").get(0).path("id").asLong();
        executar(x.c.operacionalA, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Download não contorna a autorização temporal da ocorrência");
        assertArrayEquals(bytes(), executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Documento negocial no contexto autorizado é recuperado íntegro").getResponse().getContentAsByteArray());
        JsonNode metadadosEstaduais = dados(executar(x.c.estadualA, get("/api/ocorrencias/{id}", id), 200,
                "PER-009:001[2] Estadual correto consulta metadados negociais no contexto do fato"));
        assertTrue(java.util.stream.StreamSupport.stream(metadadosEstaduais.path("ocorrencia").path("anexos").spliterator(), false)
                .anyMatch(a -> a.path("id").asLong() == anexo));
        assertArrayEquals(bytes(), executar(x.c.estadualA, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "PER009 Estadual correto também recupera os bytes existentes").getResponse().getContentAsByteArray());
        JsonNode antesEstadualAlheio = fotografiaNegocialB08(x.empregado);
        for (String rota : java.util.List.of("/api/ocorrencias/" + id, "/api/ocorrencias/anexos/" + anexo)) {
            var recusa = executar(x.c.estadualB, get(rota), 403, "PER009 Estadual de outro estado não consulta metadados nem bytes");
            assertNull(recusa.getResponse().getHeader("Content-Disposition"));
            assertEquals(antesEstadualAlheio, fotografiaNegocialB08(x.empregado));
        }
        executar(x.c.localA, delete("/api/ocorrencias/{id}", id), 204, "Cancelamento lógico autorizado");
        executar(x.c.operacionalA, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Cancelamento não devolve a prova ao contexto materializado incorreto");
        assertArrayEquals(bytes(), executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Prova negocial preservada continua acessível no contexto histórico").getResponse().getContentAsByteArray());
    }

    @Test
    void documentoPessoalENarrativaExigemPerfilOuConcessaoAuditadaERevogavel() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "PESSOAL_RESTRITO", "restrito-" + UUID.randomUUID());
        long id = o.path("id").asLong(), anexo = o.path("anexos").get(0).path("id").asLong();
        executar(x.c.nacional, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Nacional não recebe documento pessoal por consulta global");
        executar(x.c.admin, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Administrador técnico precisa de suporte excepcional autorizado");
        executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Operador não acessa documento pessoal mesmo na Lotação/Turno corretos");
        JsonNode lista = dados(executar(x.c.nacional, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado)), 200,
                "Lista nacional é minimizada como o detalhe"));
        assertEquals(1, lista.path("totalElements").asLong());
        assertFalse(lista.toString().contains("Conteúdo pessoal reservado"));
        assertTrue(lista.path("content").get(0).path("anexos").isEmpty());
        JsonNode permissao = enviar(x.c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", x.c.nacional.getId(),
                "contratoId", x.contrato, "finalidade", "Conferência da prova do afastamento",
                "justificativa", "Auditoria documental autorizada", "autorizacaoAdministrativa", "ATO-TESTE",
                "validoAte", Instant.now().plusSeconds(1800)), 201, "Concessão específica de conteúdo pessoal");
        executar(x.c.nacional, delete("/api/ocorrencias/{id}", id), 403,
                "Permissão pessoal ativa não transforma consulta nacional em escrita negocial");
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, id).getCanceladoEm()));
        finalidadeObrigatoriaB08(x, o, permissao.path("id").asLong(), x.c.nacional);
        assertArrayEquals(bytes(), executar(x.c.nacional, get("/api/ocorrencias/anexos/{id}", anexo)
                .param("finalidade", "Conferência da prova do afastamento"), 200,
                "Concessão válida libera somente prova no seu escopo").getResponse().getContentAsByteArray());
        JsonNode usos = consultar(x.c.admin, "/api/permissoes-excepcionais/" + permissao.path("id").asLong() + "/usos",
                "Uso do documento pessoal é auditado");
        assertFalse(usos.isEmpty());
        enviar(x.c.admin, "/api/permissoes-excepcionais/" + permissao.path("id").asLong() + "/revogar",
                mapa("motivo", "Fim da necessidade documental"), 200, "Revogação vigente imediatamente");
        executar(x.c.nacional, get("/api/ocorrencias/anexos/{id}", anexo)
                .param("finalidade", "Conferência da prova do afastamento"), 403,
                "Revogação exige nova autorização e não pode ser contornada por cache");
        assertNotNull(emTransacao(() -> em.find(Ocorrencia.class, id)));
    }

    @Test
    void reenvioComMesmaChaveNaoDuplicaFatoArquivoHistoricoNemFrequencia() throws Exception {
        Contexto x = preparar();
        String chave = "reenvio-" + UUID.randomUUID();
        JsonNode primeiro = criar(x, "PESSOAL_RESTRITO", chave);
        long historias = contarHistoricos(x.empregado);
        JsonNode segundo = criar(x, "PESSOAL_RESTRITO", chave);
        assertEquals(primeiro, segundo, "AUD-005 mesma intenção retorna a ocorrência já confirmada");
        assertEquals(historias, contarHistoricos(x.empregado));
        assertEquals(1L, emTransacao(() -> em.createQuery(
                "select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
        executar(x.c.localA, multipart("/api/ocorrencias").param("empregadoId", Long.toString(x.empregado))
                .param("motivo", "FALTA").param("dataOcorrencia", x.fato.toString()).param("dias", "1")
                .param("chaveIdempotencia", chave), 409,
                "Mesma chave com outra intenção não modifica o fato original");
        assertEquals(historias, contarHistoricos(x.empregado));
    }

    @Test
    void assinaturaDetectaAlteracaoDoEscopoQueControlaAConsultaHistorica() throws Exception {
        Contexto x = preparar(); criar(x, "PESSOAL_RESTRITO", "assinatura-" + UUID.randomUUID());
        assertTrue(integridade.verificarEmpregado(x.empregado).integro());
        emTransacao(() -> {
            HistoricoEmpregado h = em.createQuery("select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id desc",
                    HistoricoEmpregado.class).setParameter("id", x.empregado).setMaxResults(1).getSingleResult();
            h.setGestaoLocalSnapshotId(x.c.gestaoB.getId()); h.setSuperintendenciaSnapshotId(x.c.seB.getId());
            em.flush(); return null;
        });
        assertFalse(integridade.verificarEmpregado(x.empregado).integro(),
                "AUD-001 modificar Gestão/estado de autorização deve invalidar a assinatura");
    }

    @Test
    void duracaoInvalidaNaoCriaOcorrenciaNemHistoricoParcial() throws Exception {
        Contexto x = preparar(); long antes = contarHistoricos(x.empregado);
        executar(x.c.localA, multipart("/api/ocorrencias").param("empregadoId", Long.toString(x.empregado))
                .param("motivo", "ATESTADO").param("dataOcorrencia", x.fato.toString()).param("dias", "0"),
                400, "Duração zero é entrada inválida e não significa implicitamente um dia");
        assertEquals(antes, contarHistoricos(x.empregado));
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLASSE_INEXISTENTE", "NEGOCIAL,PESSOAL_RESTRITO"})
    void classificacaoInvalidaOuQuantidadeIncompativelNaoPublicaArquivoNemFato(String classes) throws Exception {
        Contexto x = preparar(); long antes = contarHistoricos(x.empregado);
        executar(x.c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova.txt", "text/plain", bytes()))
                .param("empregadoId", Long.toString(x.empregado)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", x.fato.toString()).param("dias", "1")
                .param("classificacoesAnexos", classes.split(",")), 400,
                "DOC009 cada arquivo exige classificação válida e a lista não pode divergir dos anexos");
        assertEquals(antes, contarHistoricos(x.empregado));
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(a) from AnexoOcorrencia a where a.ocorrencia.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
    }

    @Test
    void fatoAnteriorConservaEscopoAntigoMesmoDepoisDeAtualizarAProjecao() throws Exception {
        Contexto x = preparar();
        Contexto anterior = new Contexto(x.c, x.contrato, x.empregado, x.vinculo, x.turnoDestino,
                x.lotacaoDestino, x.operadorDestino, x.mudanca, x.mudanca.minusDays(1));
        JsonNode o = criar(anterior, "NEGOCIAL", "fato-anterior-" + UUID.randomUUID());
        emTransacao(() -> {
            Empregado e = em.find(Empregado.class, x.empregado);
            e.setLotacao(em.find(Lotacao.class, x.lotacaoDestino.getId()));
            e.setJornada(em.find(Jornada.class, x.turnoDestino.getId())); return null;
        });
        long id = o.path("id").asLong(), anexo = o.path("anexos").get(0).path("id").asLong();
        executar(x.c.operacionalA, get("/api/ocorrencias/{id}", id), 200,
                "Fato anterior pertence a L1/T1 mesmo com a projeção já em L2/T2");
        executar(x.operadorDestino, get("/api/ocorrencias/{id}", id), 403,
                "Mudança não concede acesso a fatos anteriores ao novo operador");
        assertArrayEquals(bytes(), executar(x.c.operacionalA, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Prova anterior segue o mesmo contexto do fato").getResponse().getContentAsByteArray());
        executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Download anterior não segue a projeção atual");
    }

    @Test
    void ocorrenciaIndivisivelDeDoisDiasExigeEscopoEmTodoOPeriodo() throws Exception {
        Contexto x = preparar();
        JsonNode o = dados(executar(x.c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova-dois-dias.txt", "text/plain", bytes()))
                .param("empregadoId", Long.toString(x.empregado)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", x.mudanca.minusDays(1).toString()).param("dias", "2")
                .param("classificacoesAnexos", "NEGOCIAL"), 201,
                "Gestor da mesma Gestão registra os dois contextos comprovados"));
        long id = o.path("id").asLong();
        executar(x.c.operacionalA, get("/api/ocorrencias/{id}", id), 403,
                "Um único recorte não autoriza narrativa indivisível que inclui outro dia/recorte");
        executar(x.operadorDestino, get("/api/ocorrencias/{id}", id), 403,
                "O operador do segundo dia também não recebe o primeiro recorte");
        executar(x.c.localA, get("/api/ocorrencias/{id}", id), 200, "Gestor possui escopo sobre os dois dias");
        assertEquals(1, o.path("anexos").size());
        long anexoId = o.path("anexos").get(0).path("id").asLong();
        executar(x.c.operacionalA, get("/api/ocorrencias/anexos/{id}", anexoId), 403,
                "DOC010 o arquivo indivisível exige autorização também no segundo dia");
        executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexoId), 403,
                "DOC010 autorização do segundo dia não libera bytes que também provam o primeiro");
        assertArrayEquals(bytes(), executar(x.c.localA, get("/api/ocorrencias/anexos/{id}", anexoId), 200,
                "Gestor responsável pelos dois dias baixa integralmente a prova negocial")
                .getResponse().getContentAsByteArray());
        assertEquals(2L, emTransacao(() -> em.createQuery("select count(c) from ContextoOcorrencia c where c.ocorrencia.id=:id", Long.class)
                .setParameter("id", id).getSingleResult()).longValue());
    }

    @Test
    void reenviosRealmenteConcorrentesConfirmamUmaUnicaFonteNoMysql() throws Exception {
        Contexto x = preparar(); long historicos = contarHistoricos(x.empregado);
        String chave = "concorrencia-" + UUID.randomUUID();
        var dto = new com.example.siggtrat.dto.CriacaoOcorrenciaDTO(x.empregado, "ATESTADO", x.fato,
                "Concorrência documental", null, null, 1, java.util.List.of("PESSOAL_RESTRITO"), chave);
        var pronto = new java.util.concurrent.CountDownLatch(2);
        var iniciar = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        var liberarContrato = new java.util.concurrent.CountDownLatch(1);
        var bloqueadorPronto = new java.util.concurrent.CompletableFuture<Long>();
        var atorA = new Ator("ocorrencia-A"); var atorB = new Ator("ocorrencia-B");
        try {
            var bloqueador = executor.submit(() -> emTransacao(() -> {
                em.find(Contrato.class, x.contrato, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                bloqueadorPronto.complete(((Number) em.createNativeQuery("select pg_backend_pid()").getSingleResult()).longValue());
                try {
                    if (!liberarContrato.await(25, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("AUD005 fixture não liberou o contexto");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                return null;
            }));
            long raiz = bloqueadorPronto.get(10, java.util.concurrent.TimeUnit.SECONDS);
            java.util.concurrent.Callable<Long> chamada = () -> {
                autenticarServico(x.c.localA);
                try {
                    pronto.countDown(); assertTrue(iniciar.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    return ocorrencias.criar(dto, java.util.List.of(new MockMultipartFile("anexos", "concorrente.txt", "text/plain", bytes()))).id();
                } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
            };
            var primeira = executor.submit(() -> atorA.executar(chamada));
            var segunda = executor.submit(() -> atorB.executar(chamada));
            assertTrue(pronto.await(10, java.util.concurrent.TimeUnit.SECONDS)); iniciar.countDown();
            var esperas = EsperaJdbcPostgres.aguardarAmbos(new JdbcTemplate(dataSource), raiz,
                    atorA, sql -> sql.selecaoChave("contrato", "id_contrato", x.contrato),
                    atorB, sql -> sql.selecaoChave("contrato", "id_contrato", x.contrato),
                    "AUD005 criação/reenvio ocorrência no Contrato " + x.contrato, primeira, segunda);
            assertFalse(esperas.isEmpty()); assertFalse(primeira.isDone()); assertFalse(segunda.isDone());
            liberarContrato.countDown(); bloqueador.get(10, java.util.concurrent.TimeUnit.SECONDS);
            long id = primeira.get(40, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(id, segunda.get(40, java.util.concurrent.TimeUnit.SECONDS).longValue());
            assertEquals(historicos + 1, contarHistoricos(x.empregado));
            emTransacao(() -> {
                assertEquals(1L, em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                        .setParameter("id", x.empregado).getSingleResult());
                assertEquals(1L, em.createQuery("select count(a) from AnexoOcorrencia a where a.ocorrencia.id=:id", Long.class)
                        .setParameter("id", id).getSingleResult());
                assertEquals(1L, em.createQuery("select count(c) from ContextoOcorrencia c where c.ocorrencia.id=:id", Long.class)
                        .setParameter("id", id).getSingleResult()); return null;
            });
            assertTrue(integridade.verificarEmpregado(x.empregado).integro());
        } finally { iniciar.countDown(); liberarContrato.countDown(); executor.shutdownNow(); }
    }

    @Test
    void rollbackDaCriacaoReverteFonteContextosArquivoEHistoricoAssinado() throws Exception {
        Contexto x = preparar(); long historicos = contarHistoricos(x.empregado);
        autenticarServico(x.c.localA);
        String nome;
        var ids = new java.util.concurrent.atomic.AtomicReference<java.util.Map<String, java.util.List<Long>>>();
        var arquivosAntes = estadoArquivosOcorrencia();
        try {
            nome = new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
                var dto = new com.example.siggtrat.dto.CriacaoOcorrenciaDTO(x.empregado, "ATESTADO", x.fato,
                        "Rollback documental", null, null, 1, java.util.List.of("PESSOAL_RESTRITO"), "rollback-" + UUID.randomUUID());
                var criada = ocorrencias.criar(dto, java.util.List.of(new MockMultipartFile("anexos", "rollback.txt", "text/plain", bytes())));
                String salvo = em.find(AnexoOcorrencia.class, criada.anexos().get(0).id()).getNomeSalvo();
                em.flush();
                var contextos = em.createQuery("select c.id from ContextoOcorrencia c where c.ocorrencia.id=:id", Long.class)
                        .setParameter("id", criada.id()).getResultList();
                var anexos = em.createQuery("select a.id from AnexoOcorrencia a where a.ocorrencia.id=:id", Long.class)
                        .setParameter("id", criada.id()).getResultList();
                assertEquals(1, contextos.size(), "Controle positivo: a própria criação escreveu o contexto antes do rollback");
                assertEquals(1, anexos.size(), "Controle positivo: a própria criação escreveu o anexo antes do rollback");
                var arquivosDurante = estadoArquivosOcorrencia();
                assertTrue(arquivosDurante.entrySet().containsAll(arquivosAntes.entrySet()), "Arquivos preexistentes mantidos durante a operação");
                var novos = new java.util.HashSet<>(arquivosDurante.keySet()); novos.removeAll(arquivosAntes.keySet());
                assertEquals(1, novos.size(), "Controle positivo: um arquivo sintético temporário foi preparado nesta operação");
                assertTrue(novos.iterator().next().startsWith(".staging/"), "Publicação definitiva aguarda beforeCommit");
                ids.set(java.util.Map.of("ocorrencia", java.util.List.of(criada.id()), "contextos", contextos, "anexos", anexos));
                status.setRollbackOnly(); return salvo;
            });
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        assertNotNull(nome); assertFalse(java.nio.file.Files.exists(armazenamento.uploadDir().resolve(nome)));
        assertNotNull(ids.get());
        assertEquals(arquivosAntes, estadoArquivosOcorrencia(), "Rollback remove staging novo e preserva caminhos/bytes anteriores");
        emTransacao(() -> {
            for (long id : ids.get().get("ocorrencia")) assertNull(em.find(Ocorrencia.class, id));
            for (long id : ids.get().get("contextos")) assertNull(em.find(ContextoOcorrencia.class, id), "Nenhum contexto órfão por PK capturada");
            for (long id : ids.get().get("anexos")) assertNull(em.find(AnexoOcorrencia.class, id), "Nenhum anexo órfão por PK capturada");
            return null;
        });
        assertEquals(historicos, contarHistoricos(x.empregado));
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
        assertEquals(0L, emTransacao(() -> em.createQuery("select count(f) from Frequencia f where f.empregado.id=:id", Long.class)
                .setParameter("id", x.empregado).getSingleResult()).longValue());
    }

    @Test
    void intencaoSemChaveNormalizaBordasEDistingueNomeEBytesDosArquivos() throws Exception {
        Contexto x = preparar();
        long historicosAntes = contarHistoricos(x.empregado);
        byte[] provaA = "ARQUIVO SINTETICO A".getBytes(StandardCharsets.UTF_8);
        byte[] provaB = "ARQUIVO SINTETICO B".getBytes(StandardCharsets.UTF_8);
        var primeira = criarIntencaoSemChave(x, " ATESTADO ", "  Texto sintético  ", "  ", "prova-a.txt", provaA);
        long idA = primeira.path("id").asLong();
        var estadoAntesReplay = estadoIntencoes(x);
        var repetida = criarIntencaoSemChave(x, "ATESTADO", "Texto sintético", null, "prova-a.txt", provaA.clone());
        assertEquals(primeira, repetida, "AUD005 trim e opcional vazio não criam outra intenção sem chave explícita");
        assertEquals(estadoAntesReplay, estadoIntencoes(x), "Mesmos IDs, contextos, arquivos, história e frequência após replay");
        assertEquals(historicosAntes + 1, contarHistoricos(x.empregado));

        var nomeDiferente = criarIntencaoSemChave(x, "ATESTADO", "Texto sintético", null, "prova-b.txt", provaA);
        var bytesDiferentes = criarIntencaoSemChave(x, "ATESTADO", "Texto sintético", null, "prova-a.txt", provaB);
        long idNome = nomeDiferente.path("id").asLong(), idBytes = bytesDiferentes.path("id").asLong();
        assertEquals(3, java.util.Set.of(idA, idNome, idBytes).size(), "Nome e bytes distintos não se fundem pela data/pessoa/narrativa");
        assertEquals(historicosAntes + 3, contarHistoricos(x.empregado));
        for (var intencao : java.util.List.of(primeira, nomeDiferente, bytesDiferentes)) {
            assertEquals(1, intencao.path("anexos").size());
            long id = intencao.path("id").asLong();
            long anexoId = intencao.path("anexos").get(0).path("id").asLong();
            emTransacao(() -> {
                assertEquals(id, em.find(AnexoOcorrencia.class, anexoId).getOcorrencia().getId().longValue());
                assertEquals(1L, em.createQuery("select count(c) from ContextoOcorrencia c where c.ocorrencia.id=:id", Long.class)
                        .setParameter("id", id).getSingleResult());
                return null;
            });
            assertArrayEquals(id == idBytes ? provaB : provaA,
                    executar(x.c.localA, get("/api/ocorrencias/anexos/{id}", anexoId), 200,
                            "AUD005 bytes discriminados do arquivo sintético pelo ID da fonte").getResponse().getContentAsByteArray());
        }
        var estadoAntesRepetirTodas = estadoIntencoes(x);
        assertEquals(nomeDiferente, criarIntencaoSemChave(x, "ATESTADO", "Texto sintético", null, "prova-b.txt", provaA));
        assertEquals(bytesDiferentes, criarIntencaoSemChave(x, "ATESTADO", "Texto sintético", null, "prova-a.txt", provaB));
        assertEquals(estadoAntesRepetirTodas, estadoIntencoes(x), "Reenvio de cada intenção distinta conserva seu próprio fato e cadeia");
        emTransacao(() -> {
            assertEquals(3L, em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                    .setParameter("id", x.empregado).getSingleResult());
            assertEquals(1L, em.createQuery("select count(f) from Frequencia f where f.vinculoContratual.id=:id and f.dataReferencia=:data", Long.class)
                    .setParameter("id", x.vinculo).setParameter("data", x.fato).getSingleResult(), "Três fontes não duplicam a frequência do mesmo Vínculo/dia");
            return null;
        });
        assertTrue(integridade.verificarEmpregado(x.empregado).integro());
    }

    private JsonNode criarIntencaoSemChave(Contexto x, String motivo, String observacao, String tipoRo,
                                          String nome, byte[] conteudo) throws Exception {
        autenticarServico(x.c.localA);
        try {
            var dto = new com.example.siggtrat.dto.CriacaoOcorrenciaDTO(x.empregado, motivo, x.fato,
                    observacao, tipoRo, null, 1, java.util.List.of("NEGOCIAL"), null);
            return mapper.valueToTree(ocorrencias.criar(dto, java.util.List.of(new MockMultipartFile("anexos", nome, "text/plain", conteudo))));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    private java.util.Map<String, java.util.List<String>> estadoIntencoes(Contexto x) {
        // Oráculo de fatos persistidos por ID; não reimplementa o digest da intenção.
        return emTransacao(() -> java.util.Map.of(
                "fontes", em.createQuery("select o from Ocorrencia o where o.empregado.id=:id order by o.id", Ocorrencia.class)
                        .setParameter("id", x.empregado).getResultList().stream().map(o -> o.getId()+"|"+o.getChaveIdempotencia()+"|"+o.getHashIntencao()).toList(),
                "contextos", em.createQuery("select c.id,c.ocorrencia.id,c.vinculoId,c.alocacaoId,c.dataReferencia,c.contratoId,c.gestaoLocalId,c.superintendenciaId,c.lotacaoId,c.jornadaId from ContextoOcorrencia c where c.ocorrencia.empregado.id=:id order by c.id", Object[].class)
                        .setParameter("id", x.empregado).getResultList().stream().map(java.util.Arrays::toString).toList(),
                "anexos", em.createQuery("select a from AnexoOcorrencia a where a.ocorrencia.empregado.id=:id order by a.id", AnexoOcorrencia.class)
                        .setParameter("id", x.empregado).getResultList().stream().map(a -> a.getId()+"|"+a.getOcorrencia().getId()+"|"+a.getNomeOriginal()+"|"+a.getNomeSalvo()+"|"+a.getSha256()).toList(),
                "historico", em.createQuery("select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id", HistoricoEmpregado.class)
                        .setParameter("id", x.empregado).getResultList().stream().map(h -> h.getId()+"|"+h.getHashIntegridade()).toList(),
                "frequencias", em.createQuery("select f from Frequencia f where f.vinculoContratual.id=:id order by f.id", Frequencia.class)
                        .setParameter("id", x.vinculo).getResultList().stream().map(f -> f.getId()+"|"+f.getStatus()+"|"+f.getVersaoNegocial()).toList(),
                "eventosFrequencia", em.createQuery("select e from EventoFrequencia e where e.frequencia.vinculoContratual.id=:id order by e.id", EventoFrequencia.class)
                        .setParameter("id", x.vinculo).getResultList().stream().map(e -> e.getId()+"|"+e.getFrequencia().getId()+"|"+e.getHashEvento()).toList()));
    }

    private java.util.Map<String,String> estadoArquivosOcorrencia() {
        var raiz = armazenamento.uploadDir();
        if (!java.nio.file.Files.exists(raiz)) return java.util.Map.of();
        try (var caminhos = java.nio.file.Files.walk(raiz)) {
            var estado = new java.util.TreeMap<String,String>();
            for (var p : caminhos.filter(java.nio.file.Files::isRegularFile).toList())
                estado.put(raiz.relativize(p).toString(), java.util.HexFormat.of().formatHex(
                        java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(p))));
            return java.util.Map.copyOf(estado);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private void autenticarServico(UsuarioAplicacao u) {
        var auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                u.getUsername(), null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(u.getPerfil().name())));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void transferenciaRetroativaDefineGestaoDaCriacaoCancelamentoEHistoricoDaOcorrencia() throws Exception {
        Cenario c = criarCenarioPersistido();
        NovoContrato contrato = emTransacao(() -> {
            try { return publicado(c, 100, null, false); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa ocorrência transferida",
                "cpf", cpf(Math.toIntExact(940000000L + contrato.id()))), "contratoId", contrato.id(),
                "composicaoId", contrato.composicao1(), "dataInicio", RelogioSistema.hoje().minusDays(15)),
                201, "Vínculo anterior à transferência retroativa").path("id").asLong();
        conceder(c, contrato.id(), "TRANSFERENCIA_GESTAO_LOCAL");
        enviar(c.localA, base(contrato.id()) + "/transferir-gestao", mapa(/* Conferência SINTÉTICA explícita desta fixture; não é padrão do produto. */
                "conferenciaDocumentalConfirmada", true, "versaoConferenciaDocumental", "transferencia-gestao-conferencia-v1", "dataEfetiva", RelogioSistema.hoje().minusDays(5),
                "dataAto", RelogioSistema.hoje(), "gestaoLocalDestinoId", c.gestaoB.getId(),
                "tipoContratacaoDestinoId", c.tipoB.getId(), "motivo", "Reorganização administrativa comprovada",
                "referenciaSei", "SEI-OCORRENCIA-TRANSFERIDA", "autorizacaoAdministrativa", "ATO-HTTP",
                "mapeamentos", java.util.List.of(mapa("composicaoOrigemId", contrato.composicao1(),
                        "lotacaoDestinoId", c.lotacaoB.getId(), "jornadaDestinoId", c.jornadaB.getId(), "cargoDestinoId", c.cargoB.getId()))),
                200, "Gestão B responsável a partir da data documental retroativa");
        long empregado = emTransacao(() -> em.createQuery("select e.id from Empregado e where e.pessoaCanonica.id=:p", Long.class)
                .setParameter("p", em.find(VinculoContratual.class, vinculo).getPessoa().getId()).getSingleResult());
        LocalDate data = RelogioSistema.hoje().minusDays(2);
        executar(c.localA, multipart("/api/ocorrencias").param("empregadoId", Long.toString(empregado))
                .param("motivo", "ATESTADO").param("dataOcorrencia", data.toString()), 403,
                "Gestão anterior não cria fato posterior à transferência");
        String chaveTransferida = "fonte-transferida-" + UUID.randomUUID();
        JsonNode o = dados(executar(c.localB, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "comprovante.txt", "text/plain", bytes()))
                .param("empregadoId", Long.toString(empregado)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", data.toString()).param("chaveIdempotencia", chaveTransferida),
                201, "Gestão efetiva cria ocorrência e reconcilia frequência"));
        long id = o.path("id").asLong();
        executar(c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "comprovante.txt", "text/plain", bytes()))
                .param("empregadoId", Long.toString(empregado)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", data.toString()).param("chaveIdempotencia", chaveTransferida), 403,
                "Reenvio de chave existente não concede acesso a outra Gestão antes da autorização");
        assertEquals(1L, emTransacao(() -> em.createQuery("select count(o) from Ocorrencia o where o.empregado.id=:id", Long.class)
                .setParameter("id", empregado).getSingleResult()).longValue());
        long historico = emTransacao(() -> {
            HistoricoEmpregado h = em.createQuery("select h from HistoricoEmpregado h where h.empregado.id=:id order by h.id desc",
                    HistoricoEmpregado.class).setParameter("id", empregado).setMaxResults(1).getSingleResult();
            assertEquals(c.gestaoB.getId(), h.getGestaoLocalSnapshotId());
            assertEquals(c.seB.getId(), h.getSuperintendenciaSnapshotId()); assertEquals(data, h.getDataEfetiva());
            assertEquals(3, h.getVersaoAssinatura()); assertEquals(id, h.getOcorrencia().getId()); return h.getId();
        });
        executar(c.localA, get("/api/historicos-empregados/{id}", historico), 403,
                "Histórico não usa a Gestão materializada antiga para conceder acesso");
        executar(c.localB, get("/api/historicos-empregados/{id}", historico), 200,
                "Autor da Gestão do fato recupera a prova assinada");
        executar(c.localA, delete("/api/ocorrencias/{id}", id), 403,
                "Gestão anterior não cancela fato posterior");
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, id).getCanceladoEm()));
        executar(c.localB, delete("/api/ocorrencias/{id}", id), 204,
                "Gestão efetiva cancela sem destruir os bytes e o histórico");
        assertArrayEquals(bytes(), executar(c.localB, get("/api/ocorrencias/anexos/{id}", o.path("anexos").get(0).path("id").asLong()),
                200, "Documento pessoal preservado continua no escopo comprovado do fato").getResponse().getContentAsByteArray());
        assertTrue(integridade.verificarEmpregado(empregado).integro());
    }

    private long contarHistoricos(long empregado) {
        return emTransacao(() -> em.createQuery("select count(h) from HistoricoEmpregado h where h.empregado.id=:id", Long.class)
                .setParameter("id", empregado).getSingleResult());
    }

    @Test
    void presidenciaConsultaDocumentoNegocialComMinimizacaoESemEscrita() throws Exception {
        Contexto x = preparar();
        JsonNode pessoal = criar(x, "PESSOAL_RESTRITO", "presidente-pessoal-" + UUID.randomUUID());
        executar(x.c.presidente, get("/api/ocorrencias/anexos/{id}", pessoal.path("anexos").get(0).path("id").asLong()),
                403, "DOC010 Presidência não acessa prova pessoal por padrão");
        JsonNode negocial = criar(x, "NEGOCIAL", "presidente-negocial-" + UUID.randomUUID());
        long id = negocial.path("id").asLong();
        assertArrayEquals(bytes(), executar(x.c.presidente,
                get("/api/ocorrencias/anexos/{id}", negocial.path("anexos").get(0).path("id").asLong()),
                200, "PER009 documento classificado como negocial é consultável nacionalmente pela Presidência")
                .getResponse().getContentAsByteArray());
        JsonNode detalhe = dados(executar(x.c.presidente, get("/api/ocorrencias/{id}", id), 200,
                "PER004 PER011 contexto institucional é minimizado"));
        assertTrue(detalhe.path("ocorrencia").path("empregado").path("nome").isNull());
        assertTrue(detalhe.path("ocorrencia").path("observacao").isNull());
        assertEquals(1, detalhe.path("ocorrencia").path("anexos").size());
        JsonNode lista = dados(executar(x.c.presidente, get("/api/ocorrencias")
                .param("empregadoId", Long.toString(x.empregado)), 200,
                "Lista institucional conserva quantitativos sem narrativa ou identidade pessoal"));
        assertEquals(2, lista.path("totalElements").asInt());
        for (JsonNode item : lista.path("content")) {
            assertTrue(item.path("empregado").path("nome").isNull());
            assertTrue(item.path("observacao").isNull());
        }
        executar(x.c.presidente, delete("/api/ocorrencias/{id}", id), 403,
                "Consulta nacional não concede cancelamento negocial");
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, id).getCanceladoEm()));
    }

    @Test
    void presidenciaAcessaDocumentoPessoalSomenteComConcessaoEspecificaAuditada() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "PESSOAL_RESTRITO", "presidente-concessao-" + UUID.randomUUID());
        long anexo = o.path("anexos").get(0).path("id").asLong();
        String finalidade = "Consulta institucional excepcional documentada";
        executar(x.c.presidente, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", finalidade), 403,
                "Finalidade declarada sem concessão não libera conteúdo pessoal");
        JsonNode permissao = enviar(x.c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", x.c.presidente.getId(),
                "contratoId", x.contrato, "finalidade", finalidade, "justificativa", "Necessidade específica comprovada",
                "autorizacaoAdministrativa", "ATO-PRESIDENCIA-TESTE", "validoAte", Instant.now().plusSeconds(1800)),
                201, "PER011 permissão vigente para consulta institucional excepcional");
        finalidadeObrigatoriaB08(x, o, permissao.path("id").asLong(), x.c.presidente);
        assertArrayEquals(bytes(), executar(x.c.presidente, get("/api/ocorrencias/anexos/{id}", anexo)
                .param("finalidade", finalidade), 200, "Permissão e escopo são revalidados no download pessoal")
                .getResponse().getContentAsByteArray());
        JsonNode usos = consultar(x.c.admin, "/api/permissoes-excepcionais/" + permissao.path("id").asLong() + "/usos",
                "Consulta excepcional possui prova de uso");
        assertFalse(usos.isEmpty());
        executar(x.c.presidente, delete("/api/ocorrencias/{id}", o.path("id").asLong()), 403,
                "Concessão pessoal não concede escrita à Presidência");
        enviar(x.c.admin, "/api/permissoes-excepcionais/" + permissao.path("id").asLong() + "/revogar",
                mapa("motivo", "Necessidade encerrada"), 200, "Revogação da permissão específica");
        executar(x.c.presidente, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", finalidade), 403,
                "Concessão revogada não é reutilizada pelo cache");
    }

    @Test
    void administracaoAcessaDocumentoPessoalApenasDuranteSuporteExcepcionalAuditado() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "PESSOAL_RESTRITO", "suporte-" + UUID.randomUUID());
        long anexo = o.path("anexos").get(0).path("id").asLong();
        String finalidade = "Suporte técnico autorizado para verificar a prova documental";
        executar(x.c.admin, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", finalidade), 403,
                "Administração técnica não possui acesso funcional pessoal por padrão");
        JsonNode concessao = enviar(x.c.admin, "/api/permissoes-excepcionais", mapa(
                "tipo", "INTERVENCAO_NEGOCIAL_TECNICA", "usuarioBeneficiarioId", x.c.admin.getId(),
                "contratoId", x.contrato, "finalidade", finalidade, "justificativa", "Incidente sintético documentado",
                "autorizacaoAdministrativa", "ATO-SUPORTE-TESTE", "validoAte", Instant.now().plusSeconds(1800)),
                201, "Suporte exige ato específico, finalidade, prazo e contrato delimitados");
        finalidadeObrigatoriaB08(x, o, concessao.path("id").asLong(), x.c.admin);
        assertArrayEquals(bytes(), executar(x.c.admin, get("/api/ocorrencias/anexos/{id}", anexo)
                .param("finalidade", finalidade), 200, "Concessão técnica vigente autoriza a prova delimitada")
                .getResponse().getContentAsByteArray());
        JsonNode usos = consultar(x.c.admin, "/api/permissoes-excepcionais/" + concessao.path("id").asLong() + "/usos",
                "Suporte funcional preserva trilha de uso atribuível");
        assertEquals(1, usos.size()); assertEquals(anexo, usos.get(0).path("recursoId").asLong());
        assertEquals(x.c.admin.getId().longValue(), usos.get(0).path("usuarioId").asLong());
        assertEquals("AnexoOcorrencia", usos.get(0).path("tipoRecurso").asText());
        assertEquals(concessao.path("id").asLong(), usos.get(0).path("permissaoId").asLong());
        assertEquals(finalidade, usos.get(0).path("finalidade").asText());
        executar(x.c.admin, delete("/api/ocorrencias/{id}", o.path("id").asLong()), 403,
                "Suporte documental também não altera a competência negocial");
        enviar(x.c.admin, "/api/permissoes-excepcionais/" + concessao.path("id").asLong() + "/revogar",
                mapa("motivo", "Incidente técnico encerrado"), 200, "Fim do suporte revoga a capacidade excepcional");
        executar(x.c.admin, get("/api/ocorrencias/anexos/{id}", anexo).param("finalidade", finalidade), 403,
                "A capacidade técnica expirada por revogação não é reutilizada");
        assertNull(emTransacao(() -> em.find(Ocorrencia.class, o.path("id").asLong()).getCanceladoEm()));
    }

    @Test
    void anexoLegadoSemClasseOuHashMantemRestricaoENaoRecebeCertificacaoRetroativa() throws Exception {
        Contexto x = preparar();
        JsonNode o = criar(x, "NEGOCIAL", "legado-restrito-" + UUID.randomUUID());
        long anexo = o.path("anexos").get(0).path("id").asLong();
        emTransacao(() -> {
            // DML em fixture sintética representa uma linha anterior às novas colunas.
            assertEquals(1, em.createQuery("update AnexoOcorrencia set classificacaoAcesso=null, sha256=null where id=:id")
                    .setParameter("id", anexo).executeUpdate()); return null;
        });
        executar(x.c.nacional, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Ausência de classificação comprovada não concede leitura nacional do conteúdo");
        executar(x.operadorDestino, get("/api/ocorrencias/anexos/{id}", anexo), 403,
                "Legado sem classificação é tratado como pessoal também no recorte operacional correto");
        assertArrayEquals(bytes(), executar(x.c.localA, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "Gestor responsável conserva acesso necessário à prova, sem fabricar hash de origem")
                .getResponse().getContentAsByteArray());
        JsonNode detalhe = consultar(x.c.localA, "/api/ocorrencias/" + o.path("id").asLong(),
                "Resposta distingue tratamento restrito de certificação dos bytes antigos");
        assertEquals("PESSOAL_RESTRITO", detalhe.path("ocorrencia").path("anexos").get(0).path("classificacaoAcesso").asText());
        assertTrue(detalhe.path("ocorrencia").path("anexos").get(0).path("sha256").isNull());
        emTransacao(() -> {
            Object[] persistido = (Object[]) em.createNativeQuery("select classificacao_acesso, sha256 from anexo_ocorrencia where id=:id")
                    .setParameter("id", anexo).getSingleResult();
            assertNull(persistido[0]); assertNull(persistido[1]); return null;
        });
    }

    private byte[] bytes() { return "Prova documental sintética preservada".getBytes(StandardCharsets.UTF_8); }

    private JsonNode criar(Contexto x, String classificacao, String chave) throws Exception {
        return dados(executar(x.c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "prova.txt", "text/plain", bytes()))
                .param("empregadoId", Long.toString(x.empregado)).param("motivo", "ATESTADO")
                .param("dataOcorrencia", x.fato.toString()).param("dias", "1")
                .param("observacao", "Conteúdo pessoal reservado")
                .param("classificacoesAnexos", classificacao).param("chaveIdempotencia", chave), 201,
                "Ocorrência posterior à mudança registrada pelo Gestor Local responsável"));
    }

    private void finalidadeObrigatoriaB08(Contexto x, JsonNode ocorrencia, long permissao, UsuarioAplicacao ator) throws Exception {
        long anexo = ocorrencia.path("anexos").get(0).path("id").asLong();
        String rotaUsos = "/api/permissoes-excepcionais/" + permissao + "/usos";
        JsonNode usosAntes = consultar(x.c.admin, rotaUsos, "B08 concessão válida antes das finalidades inválidas");
        assertTrue(usosAntes.isEmpty());
        JsonNode fonteAntes = fotografiaNegocialB08(x.empregado);
        for (String invalida : java.util.Arrays.asList(null, "", "   ")) {
            var pedido = get("/api/ocorrencias/anexos/{id}", anexo);
            if (invalida != null) pedido.param("finalidade", invalida);
            var recusa = executar(ator, pedido, 403, "PER005004/PER011004 mesma concessão ativa, finalidade ausente/vazia/branca");
            assertNull(recusa.getResponse().getHeader("Content-Disposition"));
            assertFalse(recusa.getResponse().getContentAsString().contains(new String(bytes(), StandardCharsets.UTF_8)));
            assertEquals(usosAntes, consultar(x.c.admin, rotaUsos, "B08 recusa não consome a concessão"));
            assertEquals(fonteAntes, fotografiaNegocialB08(x.empregado));
            assertTrue(emTransacao(() -> Boolean.TRUE.equals(em.find(PermissaoExcepcional.class, permissao).getAtivo())));
        }
        assertArrayEquals(bytes(), executar(x.c.localA, get("/api/ocorrencias/anexos/{id}", anexo), 200,
                "B08 controle dos mesmos bytes preservados após as recusas").getResponse().getContentAsByteArray());
    }

    private JsonNode fotografiaNegocialB08(long empregado) {
        return emTransacao(() -> {
            em.clear();
            return mapper.valueToTree(mapa(
                    "ocorrencias", linhasB08("select o.id,o.empregado.id,o.dataOcorrencia,o.quantidadeDias,o.canceladoEm,o.hashIntencao from Ocorrencia o where o.empregado.id=:e order by o.id", empregado),
                    "contextos", linhasB08("select c.id,c.ocorrencia.id,c.dataReferencia,c.contratoId,c.vinculoId,c.alocacaoId,c.gestaoLocalId,c.superintendenciaId,c.lotacaoId,c.jornadaId from ContextoOcorrencia c where c.ocorrencia.empregado.id=:e order by c.id", empregado),
                    "anexos", linhasB08("select a.id,a.ocorrencia.id,a.sha256,a.nomeSalvo from AnexoOcorrencia a where a.ocorrencia.empregado.id=:e order by a.id", empregado),
                    "frequencias", linhasB08("select f.id,f.dataReferencia,f.vinculoContratual.id,f.alocacaoVinculo.id,f.status,f.versaoNegocial,f.bloqueadaEdicaoOperacional from Frequencia f where f.empregado.id=:e order by f.id", empregado),
                    "eventos", linhasB08("select e.id,e.frequencia.id,e.hashAnterior,e.hashEvento from EventoFrequencia e where e.frequencia.empregado.id=:e order by e.id", empregado),
                    "historicos", linhasB08("select h.id,h.empregado.id,h.hashIntegridade from HistoricoEmpregado h where h.empregado.id=:e order by h.id", empregado)));
        });
    }
    private java.util.List<java.util.List<Object>> linhasB08(String jpql, long empregado) {
        return em.createQuery(jpql, Object[].class).setParameter("e", empregado).getResultList().stream()
                .map(java.util.Arrays::asList).toList();
    }

    private Contexto preparar() throws Exception {
        Cenario c = criarCenarioPersistido();
        Jornada j = emTransacao(() -> {
            Jornada turno = new Jornada(); turno.setDescricao("Turno destino DIF02 " + UUID.randomUUID());
            turno.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            for (java.time.DayOfWeek dia : java.time.DayOfWeek.values()) {
                JornadaDiaSemana regra = new JornadaDiaSemana(); regra.setJornada(turno); regra.setDiaSemana(dia);
                regra.setDiaTrabalho(dia.getValue() <= 5);
                if (dia.getValue() <= 5) {
                    regra.setEntrada1("08:00"); regra.setSaida1("12:00"); regra.setEntrada2("13:00"); regra.setSaida2("17:00");
                }
                turno.getDias().add(regra);
            }
            em.persist(turno); return turno;
        });
        Lotacao l = emTransacao(() -> {
            Lotacao destino = new Lotacao("Lotação destino DIF02 " + UUID.randomUUID());
            destino.setGestaoLocal(em.find(GestaoLocal.class, c.gestaoA.getId()));
            destino.setMunicipio(c.lotacaoA.getMunicipio()); destino.getTurnos().add(em.find(Jornada.class, j.getId()));
            em.persist(destino); return destino;
        });
        UsuarioAplicacao op = emTransacao(() -> {
            UsuarioAplicacao u = usuario(PerfilAcesso.OPERACIONAL, em.find(GestaoLocal.class, c.gestaoA.getId()));
            u.setLotacao(em.find(Lotacao.class, l.getId())); u.setJornada(em.find(Jornada.class, j.getId()));
            return u;
        });
        LocalDate inicio = RelogioSistema.hoje().minusDays(20), mudanca = RelogioSistema.hoje().minusDays(3);
        emTransacao(() -> {
            // A composição retroativa requer fonte explícita da existência
            // de L2/T2 naquela data. A flag atual não é prova do passado.
            for (var item : java.util.Map.of(
                    com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, l.getId(),
                    com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, j.getId()).entrySet()) {
                EventoDisponibilidadeCatalogo origem = new EventoDisponibilidadeCatalogo();
                origem.setTipoCatalogo(item.getKey()); origem.setCatalogoId(item.getValue());
                origem.setDataEfetiva(inicio); origem.setAtivo(true);
                origem.setMotivo("Fonte sintética documentada do contexto L2/T2 desde o início contratual");
                em.persist(origem);
            }
            em.flush();
            return null;
        });
        long contrato = enviar(c.localA, "/api/contratos", rascunho(c, "DIF02-" + UUID.randomUUID().toString().substring(0, 8),
                10, inicio, RelogioSistema.hoje().plusDays(60)), 201, "Contrato completo para os dois contextos").path("id").asLong();
        long comp1 = enviar(c.localA, base(contrato) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "Composição L1/T1").path("id").asLong();
        long comp2 = enviar(c.localA, base(contrato) + "/composicoes", mapa("lotacaoId", l.getId(), "jornadaId", j.getId(),
                "cargoId", c.cargoA.getId(), "efetivoPrevisto", 5), 201, "Composição L2/T2 na mesma Gestão").path("id").asLong();
        var pub = mapa("dataPublicacao", RelogioSistema.hoje(), "referenciaSei", "SEI-DIF02",
                "justificativa", "Publicação da configuração histórica", "confirmarImpacto", true);
        pub.put("tokenPrevia", enviar(c.localA, base(contrato) + "/previas/publicar", pub, 200,
                "Prévia do contrato retroativo").path("tokenPrevia").asText());
        enviar(c.localA, base(contrato) + "/publicar", pub, 200, "Contrato publicado pelas regras reais");
        long vinculo = enviar(c.localA, "/api/vinculos-contratuais", mapa("pessoa", mapa("nome", "Pessoa DIF02",
                "cpf", cpf(Math.toIntExact(960000000L + contrato))), "contratoId", contrato,
                "composicaoId", comp1, "dataInicio", inicio), 201, "Vínculo inicial L1/T1").path("id").asLong();
        var mud = mapa("dataEfetiva", mudanca, "composicaoId", comp2, "justificativa", "Mudança efetiva comprovada",
                "evidencia", "SEI-MUDANCA", "autorizacao", "Ato do Gestor Local", "confirmarImpacto", true);
        mud.put("tokenPrevia", enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/previas/alocacoes", mud, 200,
                "Prévia da mudança L1/T1 para L2/T2").path("token").asText());
        enviar(c.localA, "/api/vinculos-contratuais/" + vinculo + "/alocacoes", mud, 200, "Mudança temporal confirmada");
        long empregado = emTransacao(() -> {
            Long pessoa = em.find(VinculoContratual.class, vinculo).getPessoa().getId();
            Empregado e = em.createQuery("select e from Empregado e where e.pessoaCanonica.id=:id", Empregado.class)
                    .setParameter("id", pessoa).getSingleResult();
            // A projeção materializada pode estar atrasada; a linha canônica permanece correta.
            e.setLotacao(em.find(Lotacao.class, c.lotacaoA.getId())); e.setJornada(em.find(Jornada.class, c.jornadaA.getId()));
            em.flush(); return e.getId();
        });
        return new Contexto(c, contrato, empregado, vinculo, j, l, op, mudanca, mudanca.plusDays(1));
    }
}
