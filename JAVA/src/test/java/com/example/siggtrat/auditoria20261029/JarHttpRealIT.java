package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in pelo perfil integration e suas credenciais PostgreSQL explícitas.
 * O contexto herdado prepara a fixture JPA confirmada; nenhuma operação de
 * negócio abaixo usa MockMvc, JWT fabricado ou scheduler substituído no JAR.
 * Não comprova navegador/LAN, virada física do dia nem homologação integral.
 * SGT_PROFILE_HTTP=1 acrescenta observabilidade JMX local/SQL do laboratório;
 * sua ausência não exige configuração administrativa de logs para testar o fluxo funcional.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JarHttpRealIT extends ContratosHttpBase {
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Path PREPARO = diretorioPreparador();
    private static final String TEXTO_PDF = "Documento sintetico legivel do teste TCP/JAR SGT";
    private static final byte[] PDF = pdfSinteticoLegivel();
    private final List<Map<String, Object>> requisicoes = new ArrayList<>();
    @Autowired private JdbcTemplate jdbc;
    private Path evidencias;
    private URI origem;
    private final String applicationNameJar = "sgt-http-it-" + UUID.randomUUID();

    @DynamicPropertySource
    static void armazenamentoPreparador(DynamicPropertyRegistry propriedades) {
        propriedades.add("file.upload-dir", () -> PREPARO.resolve("uploads").toString());
        propriedades.add("audit.fallback-log", () -> PREPARO.resolve("auditoria-fallback.jsonl").toString());
        propriedades.add("app.relatorios.temp.diretorio", () -> PREPARO.resolve("relatorios").toString());
    }

    private static Path diretorioPreparador() {
        try {
            Path raiz = Path.of("target/real-jar-http").toAbsolutePath();
            Files.createDirectories(raiz);
            return Files.createTempDirectory(raiz, "preparo-",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } catch (java.io.IOException falha) {
            throw new IllegalStateException("Não foi possível isolar o armazenamento da fixture JPA", falha);
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void tomcatRealPreservaSessaoPrazosVigentesDocumentosEAuditoriaDepoisDoReinicio() throws Exception {
        LocalDate hoje = LocalDate.now(ZONA);
        assertFalse(hoje.isBefore(INICIO.plusDays(45)), "Fixture possui cobertura temporal explícita");
        assertFalse(hoje.plusDays(120).isAfter(FIM), "Atualizar a fixture quando terminar sua cobertura");
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "JAR-TCP-" + UUID.randomUUID());
        Path jar = Path.of("target/SGT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "Empacotar o candidato corrente antes do IT");
        conferirClassesEmpacotadas(jar);
        String hashJar = sha256(jar);
        Path raiz = Path.of("target/real-jar-http").toAbsolutePath();
        Files.createDirectories(raiz);
        evidencias = Files.createTempDirectory(raiz, "cenario-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Files.createFile(evidencias.resolve("http-status.jsonl"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        int porta;
        try (ServerSocket reserva = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            porta = reserva.getLocalPort();
        }
        origem = URI.create("http://127.0.0.1:" + porta);
        List<Long> processos = new ArrayList<>();
        Process filho = null;
        try {
            filho = iniciarJar(jar, hashJar, porta, "primeiro");
            processos.add(filho.pid());
            try (Sessao anonima = new Sessao(); Sessao gestor = login(c.localA);
                 Sessao operacional = login(c.operacionalA); Sessao outroEscopo = login(c.localB);
                 Sessao administrador = login(c.admin); Sessao leitor = login(c.nacional)) {
                anonima.get("/api/contratos", 401);
                gestor.json("POST", "/api/contratos", rascunho(c, "CSRF-NEGADO", 3,
                        hoje.minusDays(40), hoje.plusDays(120)), 403, false);
                JsonNode sessao = gestor.get("/api/autenticacao/sessao", 200);
                assertTrue(sessao.path("authenticated").asBoolean());
                assertEquals(c.localA.getId().longValue(), sessao.path("user").path("id").asLong());
                assertTrue(sessao.path("session").path("boundToDevice").asBoolean());
                gestor.json("POST", "/api/autenticacao/renovar-sessao", mapa("password", SENHA), 200);
                gestor.renovarCsrf();

                long contrato = publicarContrato(gestor, c, hoje);
                long documentoSei = gestor.get(base(contrato) + "/documentos", 200).get(0).path("id").asLong();
                JsonNode documento = gestor.pdf(base(contrato) + "/documentos/" + documentoSei + "/substituir",
                        mapa("motivo", "Anexo sintético recebido após a publicação", "chaveIdempotencia", "pdf-" + UUID.randomUUID()));
                long documentoId = documento.path("id").asLong();
                String caminhoDocumento = base(contrato) + "/documentos/" + documentoId;
                assertEquals("PUBLICADO", documento.path("estado").asText());
                assertEquals(documentoSei, documento.path("documentoAnteriorId").asLong());
                assertEquals(sha256(PDF), documento.path("sha256Arquivo").asText());
                conferirDownload(gestor, caminhoDocumento);
                outroEscopo.get(caminhoDocumento, 404);
                outroEscopo.get(caminhoDocumento + "/arquivo", 404);
                operacional.get(caminhoDocumento, 403);
                operacional.get(caminhoDocumento + "/arquivo", 403);
                gestor.json("DELETE", caminhoDocumento, null, 409);
                conferirDownload(gestor, caminhoDocumento);

                LocalDate dia = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
                assertFalse(dia.isBefore(hoje.minusDays(6)), "Lançamento dentro das sete datas civis vigentes");
                Instant antesDoPrimeiroLancamento = Instant.now();
                JsonNode frequencia = operacional.json("POST", "/api/frequencias", frequencia(c, dia, "FALTA"), 200);
                Instant depoisDoPrimeiroLancamento = Instant.now();
                JsonNode editada = operacional.json("POST", "/api/frequencias", frequencia(c, dia, "PRESENCA"), 200);
                long frequenciaId = frequencia.path("id").asLong();
                assertEquals(frequenciaId, editada.path("id").asLong());
                assertEquals("ABERTA", editada.path("estadoNegocial").asText());
                assertEquals("ALTERADA_OPERACIONAL", editada.path("estadoAuditoria").asText());
                assertEquals(frequencia.path("versaoNegocial").asInt() + 1, editada.path("versaoNegocial").asInt());
                Instant prazoEdicao = LocalDateTime.parse(frequencia.path("prazoEdicao").asText()).atZone(ZONA).toInstant();
                assertFalse(prazoEdicao.isBefore(antesDoPrimeiroLancamento.plus(Duration.ofHours(72))));
                assertFalse(prazoEdicao.isAfter(depoisDoPrimeiroLancamento.plus(Duration.ofHours(72))));
                assertEquals(frequencia.path("prazoEdicao"), editada.path("prazoEdicao"), "Editar não renova as 72 horas");
                operacional.json("POST", "/api/frequencias", frequencia(c, hoje.minusDays(7), "FALTA"), 403);
                operacional.json("POST", "/api/frequencias", frequencia(c, hoje.plusDays(1), "FALTA"), 400);
                gestor.json("POST", "/api/frequencias", frequencia(c, hoje.plusDays(1), "FALTA"), 400);
                Map<String, Object> retificacao = frequencia(c, dia, "FALTA");
                retificacao.put("justificativaRetificacao", "Conferência sintética do registro pelo Gestor Local");
                retificacao.put("chaveIdempotencia", "retificar-" + UUID.randomUUID());
                outroEscopo.json("POST", "/api/frequencias", retificacao, 403);
                JsonNode retificada = gestor.json("POST", "/api/frequencias", retificacao, 200);
                assertEquals(editada.path("versaoNegocial").asInt() + 1, retificada.path("versaoNegocial").asInt());
                assertEquals("RETIFICADA_GESTOR", retificada.path("estadoAuditoria").asText());
                assertTrue(retificada.path("bloqueadaEdicaoOperacional").asBoolean());
                assertEquals(editada.path("prazoEdicao"), retificada.path("prazoEdicao"));
                Map<String, Object> antesDoReenvio = fotoPersistidaFrequencia(frequenciaId);
                JsonNode reenviada = gestor.json("POST", "/api/frequencias", retificacao, 200);
                assertEquals(antesDoReenvio, fotoPersistidaFrequencia(frequenciaId),
                        "Reenvio preserva exatamente versão, updatedAt persistido, estado e autores");
                // A primeira resposta ainda contém nanos; PostgreSQL armazena updatedAt em micros.
                // Esse arredondamento não pode ocultar uma nova escrita: a fotografia acima é exata.
                com.fasterxml.jackson.databind.node.ObjectNode original = retificada.deepCopy();
                com.fasterxml.jackson.databind.node.ObjectNode replay = reenviada.deepCopy();
                original.remove("updatedAt"); replay.remove("updatedAt");
                assertEquals(original, replay, "Reenvio preserva todos os demais campos do DTO");
                operacional.json("POST", "/api/frequencias", frequencia(c, dia, "PRESENCA"), 403);
                LocalDate diaControle = dia.equals(hoje) ? dia.minusDays(1) : dia.plusDays(1);
                JsonNode controle = operacional.json("POST", "/api/frequencias", frequencia(c, diaControle, "PRESENCA"), 200);
                assertFalse(controle.path("bloqueadaEdicaoOperacional").asBoolean());
                assertEquals("ABERTA", controle.path("estadoNegocial").asText());
                JsonNode eventos = gestor.get("/api/frequencias/" + frequenciaId + "/eventos", 200);
                assertEquals(3, eventos.size());
                assertEquals(c.operacionalA.getId().longValue(), eventos.get(0).path("usuarioId").asLong());
                assertEquals(c.operacionalA.getId().longValue(), eventos.get(1).path("usuarioId").asLong());
                assertEquals(c.localA.getId().longValue(), eventos.get(2).path("usuarioId").asLong());
                assertEquals("Conferência sintética do registro pelo Gestor Local", eventos.get(2).path("motivo").asText());
                long auditorias = aguardarAuditoriaConfirmada(c);
                JsonNode eventosDocumento = gestor.get(caminhoDocumento + "/eventos", 200);
                assertFalse(eventosDocumento.isEmpty());

                conferirIdentidadeExplicita(c, hoje, contrato, gestor, operacional);

                ConcessaoTcp concessao = concederLeituraTcp(c, hoje, gestor, administrador, leitor);
                conferirLeituraTcp(concessao, administrador, leitor, 2);
                RelatorioReinicio relatorioAntes = conferirExportadoresReinicio(gestor, c, dia, "antes");
                CatalogoPostgresAssercoes.Fotografia catalogoAntes = CatalogoPostgresAssercoes.capturar(jdbc);
                List<String> garantias = CatalogoPostgresAssercoes.exigirRelacoesCriticas(catalogoAntes);
                escreverPrivado("catalogo-antes-reinicio.json", mapa("catalogo", catalogoAntes, "garantias", garantias));

                encerrar(filho);
                filho = iniciarJar(jar, hashJar, porta, "reinicio");
                processos.add(filho.pid());
                assertNotEquals(processos.get(0), processos.get(1));
                CatalogoPostgresAssercoes.Fotografia catalogoDepois = CatalogoPostgresAssercoes.capturar(jdbc);
                assertEquals(catalogoAntes, catalogoDepois, "Mesmo catálogo lógico: colunas, FKs/restrições e índices após update/reinício");
                assertEquals(garantias, CatalogoPostgresAssercoes.exigirRelacoesCriticas(catalogoDepois));
                escreverPrivado("catalogo-depois-reinicio.json", mapa("catalogo", catalogoDepois, "garantias", garantias));
                assertTrue(gestor.get("/api/autenticacao/sessao", 200).path("authenticated").asBoolean(),
                        "Sessão real confirmada sobrevive ao reinício");
                assertEquals("PUBLICADO", gestor.get(base(contrato), 200).path("estadoNegocial").asText());
                assertEquals(documento, gestor.get(caminhoDocumento, 200));
                conferirDownload(gestor, caminhoDocumento);
                assertEquals(eventosDocumento, gestor.get(caminhoDocumento + "/eventos", 200));
                assertEquals(eventos, gestor.get("/api/frequencias/" + frequenciaId + "/eventos", 200));
                JsonNode pagina = gestor.get("/api/frequencias?data=" + dia + "&contratoId=" + c.contratoA.getId(), 200);
                JsonNode relida = null;
                for (JsonNode item : pagina.path("content")) if (item.path("id").asLong() == frequenciaId) relida = item;
                assertNotNull(relida, "Registro confirmado é relido por HTTP depois do reinício");
                assertEquals(retificada.path("status"), relida.path("status"));
                assertEquals(retificada.path("versaoNegocial"), relida.path("versaoNegocial"));
                assertTrue(relida.path("bloqueadaEdicaoOperacional").asBoolean());
                emTransacao(() -> {
                    em.clear();
                    var persistida = em.find(com.example.siggtrat.entidade.Frequencia.class, frequenciaId);
                    assertEquals(c.vinculoA.getId(), persistida.getVinculoContratual().getId());
                    assertEquals(c.operacionalA.getId(), persistida.getAutorOriginalId());
                    assertEquals(c.operacionalA.getUsername(), persistida.getAutorOriginalUsername());
                    assertEquals(c.localA.getId(), persistida.getUltimaRetificacaoPorId());
                    assertEquals(retificada.path("versaoNegocial").asInt(), persistida.getVersaoNegocial());
                    assertEquals("FALTA", persistida.getStatus());
                    assertTrue(persistida.getBloqueadaEdicaoOperacional());
                    return null;
                });
                try (Sessao novaOperacional = login(c.operacionalA)) {
                    novaOperacional.json("POST", "/api/frequencias", frequencia(c, dia, "PRESENCA"), 403);
                    novaOperacional.get(caminhoDocumento + "/arquivo", 403);
                }
                assertTrue(leitor.get("/api/autenticacao/sessao", 200).path("authenticated").asBoolean());
                conferirLeituraTcp(concessao, administrador, leitor, 4);
                JsonNode usos = administrador.get("/api/permissoes-excepcionais/" + concessao.permissaoId() + "/usos", 200);
                administrador.json("POST", "/api/permissoes-excepcionais/" + concessao.permissaoId() + "/revogar",
                        mapa("motivo", "Conclusão da conferência sintética TCP após reinício"), 200);
                leitor.get("/api/ocorrencias/" + concessao.ocorrenciaId() + concessao.consulta(), 403);
                leitor.enviar("GET", "/api/ocorrencias/anexos/" + concessao.anexoId() + concessao.consulta(),
                        null, null, 403, false);
                assertEquals(usos, administrador.get("/api/permissoes-excepcionais/" + concessao.permissaoId() + "/usos", 200));
                assertEquals(concessao.versao(), versaoOcorrencia(concessao.ocorrenciaId()));
                RelatorioReinicio relatorioDepois = conferirExportadoresReinicio(gestor, c, dia, "depois");
                assertEquals(relatorioAntes.dados(), relatorioDepois.dados(), "DTO integral preservado após reinício");
                assertEquals(relatorioAntes.celulasXlsx(), relatorioDepois.celulasXlsx(), "Valores reais do XLSX preservados após reinício");
                caracterizarConsultasHttp(filho, gestor, c, dia, contrato, relatorioDepois);
                assertEquals(hashJar, sha256(jar));
                escreverPrivado("resultado.json", mapa("concluidoEm", Instant.now().toString(), "jar", jar.toString(),
                        "jarSha256", hashJar, "pids", processos, "origem", origem.toString(),
                        "arquivoSha256", sha256(PDF), "auditoriasConfirmadasAntesDoShutdown", auditorias,
                        "janelaPadraoDias", 7, "horasEdicao", 72, "concessaoContextual", mapa("ocorrenciaId", concessao.ocorrenciaId(),
                                "outraOcorrenciaId", concessao.outraId(), "anexoId", concessao.anexoId(),
                                "permissaoId", concessao.permissaoId(), "usosAntesReinicio", 2,
                                "usosDepoisReinicio", 4, "revogadaSemNovoConsumo", true), "requisicoes", requisicoes));
                // Fixture privada reutilizável; não contém senha, cookies, JWT, CSRF nem credenciais de infraestrutura.
                escreverPrivado("fixture.json", mapa("sintetica", true, "jarSha256", hashJar,
                        "documentosDiretorio", evidencias.resolve("uploads").toString(), "contratoPublicadoId", contrato,
                        "documentoId", documentoId, "frequenciaId", frequenciaId, "dataFrequencia", dia.toString(),
                        "gestaoAId", c.gestaoA.getId(), "gestaoBId", c.gestaoB.getId(),
                        "usuarios", List.of(c.localA, c.localB, c.estadualA, c.estadualB, c.nacional,
                                c.presidente, c.admin, c.operacionalA, c.operacionalB).stream()
                                .map(u -> mapa("id", u.getId(), "matricula", u.getMatricula(), "perfil", u.getPerfil().name())).toList()));
            }
        } finally {
            encerrar(filho);
            escreverPrivado("http-status.json", requisicoes);
        }
    }

    private void conferirIdentidadeExplicita(Cenario c, LocalDate hoje, long contrato,
                                             Sessao gestor, Sessao operacional) throws Exception {
        JsonNode composicoes = gestor.get(base(contrato) + "/composicoes?data=" + hoje, 200);
        assertEquals(1, composicoes.size());
        long composicao = composicoes.get(0).path("id").asLong();
        String nome = "Pessoa homônima sintética TCP " + UUID.randomUUID();
        List<Long> vinculos = new ArrayList<>(), pessoas = new ArrayList<>(), empregados = new ArrayList<>();
        List<JsonNode> eventos = new ArrayList<>();
        for (int ordem = 0; ordem < 2; ordem++) {
            JsonNode vinculo = gestor.json("POST", "/api/vinculos-contratuais", mapa(
                    "pessoa", mapa("nome", nome, "cpf", cpf(Math.toIntExact(861_000_000L + contrato * 2 + ordem))),
                    "contratoId", contrato, "composicaoId", composicao, "dataInicio", hoje.minusDays(7),
                    "dataFim", hoje.plusDays(20)), 201);
            long vinculoId = vinculo.path("id").asLong();
            vinculos.add(vinculoId); pessoas.add(vinculo.path("pessoaId").asLong());
            JsonNode frequencia = operacional.json("POST", "/api/frequencias", mapa(
                    "vinculoContratualId", vinculoId, "dataReferencia", hoje, "status", "FALTA"), 200);
            assertEquals(vinculoId, frequencia.path("vinculoContratualId").asLong());
            assertEquals(pessoas.get(ordem).longValue(), frequencia.path("pessoaId").asLong());
            assertEquals(nome, frequencia.path("empregado").path("nome").asText());
            long empregado = frequencia.path("empregado").path("id").asLong();
            assertTrue(empregado > 0); empregados.add(empregado);
            eventos.add(gestor.get("/api/frequencias/" + frequencia.path("id").asLong() + "/eventos", 200));
        }
        assertNotEquals(vinculos.get(0), vinculos.get(1)); assertNotEquals(pessoas.get(0), pessoas.get(1));
        assertNotEquals(empregados.get(0), empregados.get(1), "Homônimos têm projeções distintas, sem identidade por nome");
        Map<String, Object> antes = fotografiaIdentidade(vinculos, pessoas, empregados);
        LocalDate diaSemFrequencia = hoje.with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
        assertEquals(0L, jdbc.queryForObject("select count(*) from frequencia where id_vinculo_contratual in (?,?) and data_referencia=?",
                Long.class, vinculos.get(0), vinculos.get(1), diaSemFrequencia));
        Map<String, Object> divergente = mapa("vinculoContratualId", vinculos.get(0),
                "empregado", mapa("id", empregados.get(1)), "dataReferencia", diaSemFrequencia, "status", "FALTA");
        operacional.json("POST", "/api/frequencias", divergente, 409);
        assertEquals(antes, fotografiaIdentidade(vinculos, pessoas, empregados),
                "409 preserva integralmente frequências, projeções e fontes canônicas, incluindo timestamps PostgreSQL");
        for (int i = 0; i < 2; i++) {
            long id = jdbc.queryForObject("select id from frequencia where id_vinculo_contratual=? and data_referencia=?",
                    Long.class, vinculos.get(i), hoje);
            assertEquals(eventos.get(i), gestor.get("/api/frequencias/" + id + "/eventos", 200));
        }
        Map<String, Object> depois = fotografiaIdentidade(vinculos, pessoas, empregados);
        List<Long> controlesCoerentesNaMesmaData = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            JsonNode controle = operacional.json("POST", "/api/frequencias", mapa("vinculoContratualId", vinculos.get(i),
                    "empregado", mapa("id", empregados.get(i)), "dataReferencia", diaSemFrequencia, "status", "FALTA"), 200);
            assertEquals(vinculos.get(i).longValue(), controle.path("vinculoContratualId").asLong());
            assertEquals(empregados.get(i).longValue(), controle.path("empregado").path("id").asLong());
            controlesCoerentesNaMesmaData.add(controle.path("id").asLong());
        }
        assertNotEquals(controlesCoerentesNaMesmaData.get(0), controlesCoerentesNaMesmaData.get(1));
        escreverPrivado("identidade-explicita-409.json", mapa("homonimos", true, "vinculos", vinculos,
                "pessoas", pessoas, "empregados", empregados, "antes", antes,
                "depois", depois, "eventosPreservados", eventos, "dataNegativaSemFrequenciaPrevia", diaSemFrequencia.toString(),
                "controlesCoerentesNaMesmaData", controlesCoerentesNaMesmaData));
    }

    private Map<String, Object> fotografiaIdentidade(List<Long> vinculos, List<Long> pessoas, List<Long> empregados) {
        // row_to_json::text conserva a precisão temporal nativa, sem arredondamento Jackson de Timestamp.
        return mapa("frequencias", jdbc.queryForList("select row_to_json(f)::text from frequencia f "
                        + "where id_vinculo_contratual in (?,?) order by id", String.class, vinculos.get(0), vinculos.get(1)),
                "projecoes", jdbc.queryForList("select row_to_json(e)::text from empregado e "
                        + "where id_empregado in (?,?) order by id_empregado", String.class, empregados.get(0), empregados.get(1)),
                "pessoas", jdbc.queryForList("select row_to_json(p)::text from pessoa p "
                        + "where id_pessoa in (?,?) order by id_pessoa", String.class, pessoas.get(0), pessoas.get(1)),
                "vinculos", jdbc.queryForList("select row_to_json(v)::text from vinculo_contratual v "
                        + "where id_vinculo_contratual in (?,?) order by id_vinculo_contratual", String.class, vinculos.get(0), vinculos.get(1)),
                "alocacoes", jdbc.queryForList("select row_to_json(a)::text from alocacao_vinculo a "
                        + "where id_vinculo_contratual in (?,?) order by id_alocacao_vinculo", String.class, vinculos.get(0), vinculos.get(1)));
    }

    private record RelatorioReinicio(JsonNode dados, List<List<String>> celulasXlsx) {}

    private void caracterizarConsultasHttp(Process filho, Sessao gestor, Cenario c, LocalDate dia,
                                          long contratoComHistorico, RelatorioReinicio relatorio) throws Exception {
        String dimensoes = "&contratoId=" + c.contratoA.getId() + "&lotacaoId=" + c.lotacaoA.getId()
                + "&jornadaId=" + c.jornadaA.getId() + "&cargoId=" + c.cargoA.getId();
        String filtro = "?inicio=" + dia + "&fim=" + dia + "&contratoIds=" + c.contratoA.getId()
                + "&lotacaoId=" + c.lotacaoA.getId() + "&jornadaId=" + c.jornadaA.getId();
        var rotas = List.of(new CaracterizacaoHttpJar.Rota("listagem", "/api/contratos", true),
                new CaracterizacaoHttpJar.Rota("painel", "/api/painel/efetivo?referencia=" + dia
                        + "&periodo=DIARIO&compararAnoAnterior=false" + dimensoes, true),
                new CaracterizacaoHttpJar.Rota("historico", base(contratoComHistorico) + "/eventos", true),
                new CaracterizacaoHttpJar.Rota("frequencia", "/api/frequencias?data=" + dia
                        + "&contratoId=" + c.contratoA.getId(), false),
                new CaracterizacaoHttpJar.Rota("exportacao", "/api/relatorios/frequencia-consolidada/exportacao"
                        + filtro + "&formato=PDF", false));
        JsonNode historico = gestor.get(base(contratoComHistorico) + "/eventos", 200);
        assertFalse(historico.isEmpty());
        boolean observarRecursos = "1".equals(System.getenv("SGT_PROFILE_HTTP"));
        try (RecursosJarObservador recursos = observarRecursos
                ? new RecursosJarObservador(filho, jdbc, applicationNameJar) : null) {
        Map<String, Object> prova = CaracterizacaoHttpJar.executar(filho, rotas,
                caminho -> gestor.enviar("GET", caminho, null, null, 200, false, false), (nome, resposta) -> {
                    if (nome.equals("exportacao")) {
                        conferirCabecalhosExportacao(resposta, "application/pdf");
                        PdfReader leitor = new PdfReader(resposta.body());
                        try {
                            assertTrue(new PdfTextExtractor(leitor).getTextFromPage(1)
                                    .contains(relatorio.dados().path("registros").get(0).path("nome").asText()));
                        } finally { leitor.close(); }
                        return;
                    }
                    JsonNode dados = gestor.dadosHttp(resposta);
                    switch (nome) {
                        case "listagem" -> {
                            assertTrue(dados.isArray());
                            assertTrue(java.util.stream.StreamSupport.stream(dados.spliterator(), false)
                                    .anyMatch(item -> item.path("id").asLong() == c.contratoA.getId()));
                        }
                        case "painel" -> {
                            assertEquals(c.contratoA.getId().longValue(), dados.path("contratoId").asLong());
                            assertEquals(1, dados.path("serieAtual").size());
                            assertEquals(dia.toString(), dados.path("serieAtual").get(0).path("data").asText());
                            assertEquals(1, dados.path("resumoAtual").path("ausentes").asLong());
                        }
                        case "historico" -> assertEquals(historico, dados);
                        case "frequencia" -> {
                            assertEquals(1, dados.path("content").size());
                            assertEquals(c.vinculoA.getId().longValue(), dados.path("content").get(0).path("vinculoContratualId").asLong());
                            assertEquals("FALTA", dados.path("content").get(0).path("status").asText());
                        }
                        default -> fail("Rota sem oráculo: " + nome);
                    }
                }, recursos);
        escreverPrivado("caracterizacao-http-local.json", prova);
        }
    }

    private RelatorioReinicio conferirExportadoresReinicio(Sessao gestor, Cenario c, LocalDate data, String fase) throws Exception {
        String filtro = "?inicio=" + data + "&fim=" + data + "&contratoIds=" + c.contratoA.getId()
                + "&lotacaoId=" + c.lotacaoA.getId() + "&jornadaId=" + c.jornadaA.getId();
        String api = "/api/relatorios/frequencia-consolidada";
        JsonNode dados = gestor.get(api + filtro + "&completo=true", 200);
        assertTrue(dados.path("completo").asBoolean()); assertEquals(1, dados.path("registros").size());
        JsonNode linha = dados.path("registros").get(0);
        assertEquals(c.empregadoA.getId().longValue(), linha.path("empregadoId").asLong());
        assertEquals("FALTA", linha.path("diario").path(data.toString()).path("status").asText());
        assertEquals(1, linha.path("totais").path("faltas").asInt());
        String nome = linha.path("nome").asText(); assertFalse(nome.isBlank());
        HttpResponse<byte[]> pdf = gestor.enviar("GET", api + "/exportacao" + filtro + "&formato=PDF", null, null, 200, false);
        conferirCabecalhosExportacao(pdf, "application/pdf");
        PdfReader leitor = new PdfReader(pdf.body());
        try {
            assertTrue(leitor.getNumberOfPages() > 0);
            assertTrue(new PdfTextExtractor(leitor).getTextFromPage(1).contains(nome));
        } finally { leitor.close(); }
        LocalDateTime geracaoMinima = LocalDateTime.now(ZONA).withSecond(0).withNano(0);
        HttpResponse<byte[]> xlsx = gestor.enviar("GET", api + "/exportacao" + filtro + "&formato=XLSX", null, null, 200, false);
        LocalDateTime geracaoMaxima = LocalDateTime.now(ZONA).withSecond(0).withNano(0);
        conferirCabecalhosExportacao(xlsx, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        List<List<String>> celulas = PlanilhaXlsxAssercoes.linhasXlsx(xlsx.body());
        assertEquals(1, celulas.stream().flatMap(List::stream).filter(nome::equals).count());
        List<List<String>> celulasEstaveis = new ArrayList<>(celulas);
        List<String> metadados = new ArrayList<>(celulas.get(1));
        String linhaGeracao = metadados.get(0), separador = " | Gerado em ";
        int posicao = linhaGeracao.lastIndexOf(separador); assertTrue(posicao > 0);
        LocalDateTime geradoEm = LocalDateTime.parse(linhaGeracao.substring(posicao + separador.length()),
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
                        .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        assertFalse(geradoEm.isBefore(geracaoMinima)); assertFalse(geradoEm.isAfter(geracaoMaxima));
        // Somente o instante de geração é volátil; todos os demais bytes de células entram na comparação.
        metadados.set(0, linhaGeracao.substring(0, posicao) + separador + "<instante validado>");
        celulasEstaveis.set(1, List.copyOf(metadados));
        escreverPrivado("exportadores-" + fase + "-reinicio.json", mapa("dados", dados, "celulasXlsx", celulas,
                "celulasXlsxEstaveis", celulasEstaveis, "geradoEmValidado", geradoEm.toString(),
                "pdfSha256", sha256(pdf.body()), "xlsxSha256", sha256(xlsx.body()),
                "pdfBytes", pdf.body().length, "xlsxBytes", xlsx.body().length));
        return new RelatorioReinicio(dados, List.copyOf(celulasEstaveis));
    }

    private void conferirCabecalhosExportacao(HttpResponse<byte[]> resposta, String mime) {
        assertEquals(mime, resposta.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resposta.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        assertTrue(resposta.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertEquals("nosniff", resposta.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertTrue(resposta.body().length > 0);
    }

    private record ConcessaoTcp(long ocorrenciaId, long outraId, long anexoId, long permissaoId,
                                 long versao, String observacao, String finalidade) {
        String consulta() { return "?finalidade=" + URLEncoder.encode(finalidade, StandardCharsets.UTF_8); }
    }

    private ConcessaoTcp concederLeituraTcp(Cenario c, LocalDate hoje, Sessao gestor,
                                            Sessao administrador, Sessao leitor) throws Exception {
        String observacao = "Narrativa sintética restrita ao alvo TCP/JAR";
        JsonNode alvo = gestor.ocorrencia(c, hoje.minusDays(4), 2, observacao);
        JsonNode outra = gestor.ocorrencia(c, hoje.minusDays(2), 1, "Outro fato sem concessão TCP");
        long id = alvo.path("id").asLong();
        leitor.get("/api/ocorrencias/" + id + "?finalidade=sem-concessao", 403);
        String finalidade = "Conferência sintética específica TCP";
        JsonNode grant = administrador.json("POST", "/api/permissoes-excepcionais", mapa(
                "tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "usuarioBeneficiarioId", c.nacional.getId(),
                "ocorrenciaId", id, "finalidade", finalidade,
                "justificativa", "Verificação sintética por Tomcat real e reinício",
                "autorizacaoAdministrativa", "ATO-TCP-SINTETICO", "validoAte", Instant.now().plusSeconds(1800)), 201);
        assertEquals(id, grant.path("ocorrenciaId").asLong());
        assertTrue(grant.path("contratoId").isNull());
        assertTrue(grant.path("gestaoLocalId").isNull());
        long permissao = grant.path("id").asLong();
        assertTrue(administrador.get("/api/permissoes-excepcionais/" + permissao + "/usos", 200).isEmpty());
        return new ConcessaoTcp(id, outra.path("id").asLong(), alvo.path("anexos").get(0).path("id").asLong(),
                permissao, versaoOcorrencia(id), observacao, finalidade);
    }

    private long versaoOcorrencia(long id) {
        return emTransacao(() -> {
            em.clear();
            return em.find(com.example.siggtrat.entidade.Ocorrencia.class, id).getVersion();
        });
    }

    private void conferirLeituraTcp(ConcessaoTcp c, Sessao administrador, Sessao leitor, int totalUsos) throws Exception {
        assertEquals(c.observacao(), leitor.get("/api/ocorrencias/" + c.ocorrenciaId() + c.consulta(), 200)
                .path("ocorrencia").path("observacao").asText());
        HttpResponse<byte[]> anexo = leitor.enviar("GET", "/api/ocorrencias/anexos/" + c.anexoId() + c.consulta(),
                null, null, 200, false);
        assertArrayEquals(PDF, anexo.body());
        conferirPdfLegivel(anexo.body());
        var recusada = leitor.enviar("GET", "/api/ocorrencias/" + c.ocorrenciaId() + "?finalidade=outra-finalidade",
                null, null, 403, false);
        assertFalse(new String(recusada.body(), StandardCharsets.UTF_8).contains(c.observacao()));
        leitor.get("/api/ocorrencias/" + c.outraId() + c.consulta(), 403);
        leitor.json("DELETE", "/api/ocorrencias/" + c.ocorrenciaId(), null, 403);
        JsonNode usos = administrador.get("/api/permissoes-excepcionais/" + c.permissaoId() + "/usos", 200);
        assertEquals(totalUsos, usos.size());
        for (JsonNode uso : usos) assertEquals(c.finalidade(), uso.path("finalidade").asText());
        assertEquals(c.versao(), versaoOcorrencia(c.ocorrenciaId()));
    }

    private long publicarContrato(Sessao gestor, Cenario c, LocalDate hoje) throws Exception {
        long id = gestor.json("POST", "/api/contratos", rascunho(c, "JAR-" + UUID.randomUUID().toString().substring(0, 8),
                3, hoje.minusDays(40), hoje.plusDays(120)), 201).path("id").asLong();
        gestor.json("POST", base(id) + "/composicoes", composicao(c, c.cargoA.getId(), 3), 201);
        var publicacao = mapa("dataPublicacao", hoje, "referenciaSei", "SEI-JAR-SINTETICO",
                "justificativa", "Publicação retroativa sintética conferida por HTTP real");
        gestor.json("POST", base(id) + "/publicar", publicacao, 409);
        assertEquals("RASCUNHO", gestor.get(base(id), 200).path("estadoNegocial").asText());
        JsonNode previa = gestor.json("POST", base(id) + "/previas/publicar", publicacao, 200);
        assertFalse(previa.path("tokenPrevia").asText().isBlank());
        publicacao.put("tokenPrevia", previa.path("tokenPrevia").asText());
        publicacao.put("confirmarImpacto", true);
        assertEquals("PUBLICADO", gestor.json("POST", base(id) + "/publicar", publicacao, 200).path("estadoNegocial").asText());
        return id;
    }

    private Map<String, Object> frequencia(Cenario c, LocalDate data, String status) {
        return mapa("vinculoContratualId", c.vinculoA.getId(), "dataReferencia", data, "status", status);
    }

    private Map<String, Object> fotoPersistidaFrequencia(long id) {
        return emTransacao(() -> {
            em.clear();
            var f = em.find(com.example.siggtrat.entidade.Frequencia.class, id);
            return mapa("id", f.getId(), "versaoNegocial", f.getVersaoNegocial(), "updatedAt", f.getUpdatedAt(),
                    "status", f.getStatus(), "autorId", f.getAutorOriginalId(), "autor", f.getAutorOriginalUsername(),
                    "retificadorId", f.getUltimaRetificacaoPorId(), "retificador", f.getUltimaRetificacaoPorUsername(),
                    "bloqueadaEdicaoOperacional", f.getBloqueadaEdicaoOperacional());
        });
    }

    private void conferirDownload(Sessao sessao, String caminho) throws Exception {
        HttpResponse<byte[]> resposta = sessao.enviar("GET", caminho + "/arquivo", null, null, 200, false);
        assertArrayEquals(PDF, resposta.body());
        assertEquals(sha256(PDF), sha256(resposta.body()));
        assertEquals("application/pdf", resposta.headers().firstValue("Content-Type").orElse(""));
        assertTrue(resposta.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        assertEquals("nosniff", resposta.headers().firstValue("X-Content-Type-Options").orElse(""));
        conferirPdfLegivel(resposta.body());
    }

    private static byte[] pdfSinteticoLegivel() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, bytes);
            try {
                documento.open();
                documento.add(new Paragraph(TEXTO_PDF));
            } finally { documento.close(); }
            byte[] pdf = bytes.toByteArray();
            conferirPdfLegivel(pdf);
            return pdf;
        } catch (Exception falha) { throw new IllegalStateException("Fixture PDF sintetica indisponivel", falha); }
    }

    private static void conferirPdfLegivel(byte[] pdf) throws Exception {
        PdfReader leitor = new PdfReader(pdf);
        try {
            assertEquals(1, leitor.getNumberOfPages());
            assertTrue(new PdfTextExtractor(leitor).getTextFromPage(1).contains(TEXTO_PDF),
                    "A mesma pagina legivel deve sobreviver ao armazenamento e ao reinicio");
        } finally { leitor.close(); }
    }

    private long aguardarAuditoriaConfirmada(Cenario c) throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        do {
            long quantidade = emTransacao(() -> em.createQuery("select count(a) from RegistroAuditoria a "
                            + "where a.userId=:u and a.path='/api/frequencias'", Long.class)
                    .setParameter("u", c.operacionalA.getId()).getSingleResult());
            // O contexto preparador não faz HTTP nem drena a fila do processo filho.
            // Com poucas requisições, a persistência antes do shutdown exige o flush agendado real.
            if (quantidade >= 2) return quantidade;
            Thread.sleep(100);
        } while (System.nanoTime() < limite);
        fail("Auditoria HTTP não foi confirmada pelo scheduler real com o JAR ativo");
        return 0;
    }

    private Process iniciarJar(Path jar, String hashJar, int porta, String fase) throws Exception {
        assertEquals(hashJar, sha256(jar), "Reinício usa exatamente o mesmo JAR");
        Path log = Files.createFile(evidencias.resolve(fase + ".log"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        List<String> comando = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC",
                "-Duser.timezone=America/Sao_Paulo", "-jar", jar.toString(),
                "--env-file=./src/test/resources/auditoria-endpoints.env", "--server.address=127.0.0.1", "--server.port=" + porta,
                "--server.ssl.enabled=false", "--spring.main.banner-mode=off", "--spring.jpa.hibernate.ddl-auto=update",
                "--spring.jpa.open-in-view=false", "--spring.sql.init.mode=never", "--spring.jpa.show-sql=false",
                "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true", "--spring.datasource.hikari.maximum-pool-size=4",
                "--spring.datasource.hikari.minimum-idle=1", "--logging.level.root=WARN", "--logging.level.com.example.siggtrat=WARN",
                "--logging.level.org.hibernate.SQL=OFF", "--logging.level.org.hibernate.orm.jdbc.bind=OFF",
                "--app.email.enabled=false", "--app.email.gmail.enabled=false", "--spring.cache.type=redis",
                "--app.seed.unidades-operacionais-enabled=false", "--app.seed.jornadas-operacionais-enabled=false",
                "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + evidencias.resolve("uploads"),
                "--audit.fallback-log=" + evidencias.resolve("auditoria-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + evidencias.resolve("relatorios")));
        if ("1".equals(System.getenv("SGT_PROFILE_HTTP"))) comando.addAll(List.of(
                "--spring.datasource.hikari.register-mbeans=true", "--server.tomcat.mbeanregistry.enabled=true"));
        ProcessBuilder builder = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile());
        Map<String, String> ambiente = builder.environment();
        // Não herdar overrides do contexto preparador, dotenv operacional ou agentes JVM.
        ambiente.clear();
        ambiente.put("TZ", "America/Sao_Paulo");
        ConfiguracaoPostgresIntegracao banco = ConfiguracaoPostgresIntegracao.carregar();
        String urlObservada = banco.url() + (banco.url().contains("?") ? "&" : "?") + "ApplicationName=" + applicationNameJar;
        ambiente.put("DB_URL", urlObservada);
        ambiente.put("DB_USER", banco.usuario());
        ambiente.put("DB_PASS", banco.senha());
        ambiente.put("SPRING_DATASOURCE_URL", urlObservada);
        ambiente.put("SPRING_DATASOURCE_USERNAME", banco.usuario());
        ambiente.put("SPRING_DATASOURCE_PASSWORD", banco.senha());
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) {
            String valor = System.getenv(chave);
            assertNotNull(valor, "Configurar explicitamente o Redis real isolado: " + chave);
            assertFalse(valor.isBlank(), "Configurar explicitamente o Redis real isolado: " + chave);
            ambiente.put(chave, valor);
        }
        ambiente.put("REDIS_HTTP_GET_ENABLED", "true");
        Process processo = builder.start();
        try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            long limite = System.nanoTime() + Duration.ofSeconds(55).toNanos();
            while (System.nanoTime() < limite) {
                assertTrue(processo.isAlive(), "JAR encerrou antes da prontidão; diagnóstico no log privado " + log);
                try {
                    var resposta = cliente.send(HttpRequest.newBuilder(origem.resolve("/api/autenticacao/csrf"))
                            .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                    if (resposta.statusCode() == 200) return processo;
                } catch (java.io.IOException indisponivelDuranteInicializacao) {
                    // Socket indisponível até o Tomcat inicializar; falhas não são aprovação.
                }
                Thread.sleep(100);
            }
            fail("Tomcat não ficou pronto no prazo; diagnóstico no log privado " + log);
            return processo;
        } catch (Exception | AssertionError falha) {
            encerrar(processo);
            throw falha;
        }
    }

    private void encerrar(Process processo) throws InterruptedException {
        if (processo == null || !processo.isAlive()) return;
        processo.destroy();
        if (!processo.waitFor(12, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            assertTrue(processo.waitFor(5, TimeUnit.SECONDS), "Encerrar somente o processo filho deste teste");
        }
    }

    private void conferirClassesEmpacotadas(Path jar) throws Exception {
        Path classes = Path.of("target/classes");
        int conferidas = 0;
        try (ZipFile zip = new ZipFile(jar.toFile()); var caminhos = Files.walk(classes)) {
            for (Path classe : caminhos.filter(p -> p.toString().endsWith(".class")).toList()) {
                String relativa = classes.relativize(classe).toString().replace('\\', '/');
                var entrada = zip.getEntry("BOOT-INF/classes/" + relativa);
                assertNotNull(entrada, "Classe corrente ausente do JAR: " + relativa);
                try (var bytes = zip.getInputStream(entrada)) {
                    assertArrayEquals(Files.readAllBytes(classe), bytes.readAllBytes(),
                            "Reempacotar após alterar main: " + relativa);
                }
                conferidas++;
            }
        }
        assertTrue(conferidas > 0, "Comparação inclui as classes de produção compiladas nesta execução");
    }

    private void escreverPrivado(String nome, Object conteudo) throws Exception {
        Path arquivo = Files.createFile(evidencias.resolve(nome),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(arquivo, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conteudo) + "\n",
                StandardOpenOption.WRITE);
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private String sha256(Path arquivo) throws Exception {
        MessageDigest resumo = MessageDigest.getInstance("SHA-256");
        try (var entrada = Files.newInputStream(arquivo)) {
            byte[] bloco = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(bloco)) != -1) resumo.update(bloco, 0, lidos);
        }
        return HexFormat.of().formatHex(resumo.digest());
    }

    private Sessao login(UsuarioAplicacao usuario) throws Exception {
        Sessao sessao = new Sessao();
        try {
            sessao.renovarCsrf();
            JsonNode resposta = sessao.json("POST", "/api/autenticacao/autenticar",
                    mapa("matricula", usuario.getMatricula(), "password", SENHA), 200);
            assertEquals(usuario.getUsername(), resposta.path("username").asText());
            assertEquals(usuario.getPerfil().name(), resposta.path("perfil").asText());
            assertFalse(resposta.has("token"));
            assertFalse(resposta.has("password"));
            assertTrue(sessao.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals("autenticacao")));
            assertTrue(sessao.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals("id_dispositivo")));
            sessao.renovarCsrf();
            return sessao;
        } catch (Exception | AssertionError falha) {
            sessao.close();
            throw falha;
        }
    }

    private final class Sessao implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        private final HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
        private String csrf;

        private void renovarCsrf() throws Exception {
            JsonNode token = get("/api/autenticacao/csrf", 200);
            assertEquals("X-XSRF-TOKEN", token.path("headerName").asText());
            csrf = token.path("token").asText();
            assertFalse(csrf.isBlank());
        }

        private JsonNode get(String caminho, int status) throws Exception {
            return dadosHttp(enviar("GET", caminho, null, null, status, false));
        }

        private JsonNode json(String metodo, String caminho, Object corpo, int status) throws Exception {
            return json(metodo, caminho, corpo, status, true);
        }

        private JsonNode json(String metodo, String caminho, Object corpo, int status, boolean comCsrf) throws Exception {
            return dadosHttp(enviar(metodo, caminho, corpo == null ? null : JarHttpRealIT.this.json(corpo).getBytes(StandardCharsets.UTF_8),
                    "application/json", status, comCsrf));
        }

        private JsonNode pdf(String caminho, Object metadados) throws Exception {
            String limite = "sgt-" + UUID.randomUUID();
            ByteArrayOutputStream corpo = new ByteArrayOutputStream();
            corpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"metadados\"; filename=\"metadados.json\"\r\n"
                    + "Content-Type: application/json\r\n\r\n" + JarHttpRealIT.this.json(metadados)
                    + "\r\n--" + limite + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"contrato-sintetico.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            corpo.write(PDF);
            corpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return dadosHttp(enviar("POST", caminho, corpo.toByteArray(), "multipart/form-data; boundary=" + limite, 200, true));
        }

        private JsonNode ocorrencia(Cenario c, LocalDate inicio, int dias, String observacao) throws Exception {
            String limite = "ocorrencia-" + UUID.randomUUID();
            ByteArrayOutputStream corpo = new ByteArrayOutputStream();
            Map<String, Object> campos = mapa("empregadoId", c.empregadoA.getId(), "motivo", "ATESTADO",
                    "dataOcorrencia", inicio, "dias", dias, "observacao", observacao,
                    "chaveIdempotencia", "tcp-" + UUID.randomUUID());
            for (var campo : campos.entrySet()) corpo.write(("--" + limite
                    + "\r\nContent-Disposition: form-data; name=\"" + campo.getKey() + "\"\r\n\r\n"
                    + campo.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
            corpo.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"anexos\"; filename=\"prova-tcp.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            corpo.write(PDF);
            corpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return dadosHttp(enviar("POST", "/api/ocorrencias", corpo.toByteArray(),
                    "multipart/form-data; boundary=" + limite, 201, true));
        }

        private HttpResponse<byte[]> enviar(String metodo, String caminho, byte[] corpo, String tipo,
                                             int esperado, boolean comCsrf) throws Exception {
            return enviar(metodo, caminho, corpo, tipo, esperado, comCsrf, true);
        }

        private HttpResponse<byte[]> enviar(String metodo, String caminho, byte[] corpo, String tipo,
                                             int esperado, boolean comCsrf, boolean revalidarCache) throws Exception {
            var builder = HttpRequest.newBuilder(origem.resolve(caminho)).timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json").header("User-Agent", "SGT-JarHttpRealIT");
            if (metodo.equals("GET") && revalidarCache) builder.header("Cache-Control", "no-cache");
            if (tipo != null) builder.header("Content-Type", tipo);
            if (comCsrf) {
                assertNotNull(csrf, "Bootstrap/refresh CSRF antes da mutação");
                builder.header("X-XSRF-TOKEN", csrf).header("X-User-Password", SENHA);
            }
            HttpResponse<byte[]> resposta = cliente.send(builder.method(metodo,
                    corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(corpo)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            Map<String, Object> registro = mapa("metodo", metodo, "caminho", caminho, "esperado", esperado,
                    "recebido", resposta.statusCode(), "bytes", resposta.body().length);
            requisicoes.add(registro);
            Files.writeString(evidencias.resolve("http-status.jsonl"), JarHttpRealIT.this.json(registro) + "\n",
                    StandardOpenOption.APPEND);
            // Nunca anexar corpos/headers à asserção: autenticação/CSRF podem conter segredos.
            assertEquals(esperado, resposta.statusCode(), metodo + " " + caminho);
            return resposta;
        }

        private JsonNode dadosHttp(HttpResponse<byte[]> resposta) throws Exception {
            if (resposta.body().length == 0) return mapper.nullNode();
            JsonNode raiz = mapper.readTree(resposta.body());
            return raiz.hasNonNull("data") ? raiz.get("data") : raiz;
        }

        @Override public void close() { cliente.close(); }
    }
}
