package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.example.siggtrat.servico.AutorizacaoTemporalOcorrenciaServico;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * PREPARATION_ONLY_NOT_ACCEPTANCE. Dados comprometidos para o mesmo PostgreSQL/JAR/HMR.
 * Não inicia JAR, navegador, Redis ou outro ApplicationContext. Nenhum @Test é herdado
 * dos dois produtores reutilizados por composição/reflexão, somente seus helpers de massa.
 */
@Tag("preparation-only-not-acceptance")
@TestPropertySource(properties = "frequencia.janela-operacional-dias=30")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PreparacaoFrontendRealIT extends ContratosHttpBase {
    private static final String ESTADO = "PREPARATION_ONLY_NOT_ACCEPTANCE";
    private static final String JAR_SHA = "dcb3a38443c0f941d70f95f10a3a9f80bb44ee1a6aa57f1c29006fc3bf726f7d";
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Path RUNTIME = diretorioRuntime();
    @Autowired private AutowireCapableBeanFactory factory;
    @Autowired private DataSource dataSource;
    @Autowired private AutorizacaoTemporalOcorrenciaServico autorizacaoOcorrencia;

    @DynamicPropertySource
    static void armazenamentoComumAoJar(DynamicPropertyRegistry p) {
        p.add("file.upload-dir", () -> RUNTIME.resolve("storage/uploads").toString());
        p.add("audit.fallback-log", () -> RUNTIME.resolve("storage/audit-fallback.jsonl").toString());
        p.add("app.relatorios.temp.diretorio", () -> RUNTIME.resolve("storage/relatorios").toString());
    }

    @Test
    @Timeout(value = 240, unit = TimeUnit.SECONDS)
    void prepararNavegador() throws Exception {
        assertTrue(Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024, "Preparador deve ter heap máximo de 192 MiB");
        assertEquals("PREPARATION_ONLY", System.getProperty("sgt.frontend.fixture.mode", System.getenv("SGT_FRONTEND_FIXTURE_MODE")),
                "Selecione somente #prepararNavegador e declare PREPARATION_ONLY; não é um gate funcional");
        Path privado = subdiretorio(RUNTIME.resolve("private"));
        assertFalse(Files.exists(privado.resolve("e2e-fixture.json")), "Uma fixture anterior nunca é sobrescrita");
        Path jar = Path.of("target/sig-gtrat-backend-0.0.4-operacional-20261101.jar").toAbsolutePath();
        assertEquals(JAR_SHA, hashArquivo(jar), "Candidato backend permanece congelado");
        Map<String, Object> banco = conferirBanco();
        gravar(privado.resolve("preparacao-iniciada.json"), mapa("status", "PREPARING_NOT_ACCEPTANCE", "jarSha256", JAR_SHA,
                "banco", banco, "instante", Instant.now(), "javaPreparador", System.getProperty("java.version")));

        LocalDate hoje = LocalDate.now(ZONA), futuro = hoje.plusDays(10);
        assertFalse(hoje.isBefore(INICIO)); assertFalse(futuro.isAfter(FIM), "Fixture base tem vigência explícita até 2027");
        Cenario c = criarCenarioPersistido();
        declararFonteNominalSintetica(c, "FRONTEND-NATIVO-BASE-A");
        // Alias do mesmo cenário só para reutilizar o helper no vínculo B; não cria uma nova origem A.
        Cenario ladoB = new Cenario();
        ladoB.vinculoA = c.vinculoB; ladoB.gestaoA = c.gestaoB; ladoB.localA = c.localB;
        declararFonteNominalSintetica(ladoB, "FRONTEND-NATIVO-BASE-B");
        String etiqueta = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> contas = new LinkedHashMap<>();
        conta(contas, "gestor_local", c.localA); conta(contas, "gestor_destino", c.localB);
        conta(contas, "fixture_admin", c.admin); conta(contas, "audit_operacional", c.operacionalA);
        conta(contas, "nacional", c.nacional); conta(contas, "presidente", c.presidente);

        List<JsonNode> fornecedores = new ArrayList<>();
        for (int n = 1; n <= 6; n++) fornecedores.add(enviar(c.localA, "/api/fornecedores", mapa(
                "razaoSocial", "SINTETICA-NAVEGADOR-" + etiqueta + "-" + String.format(Locale.ROOT, "%02d", n),
                "cnpj", novoCnpj(), "email", "fornecedor-" + n + "-" + etiqueta + "@example.test", "ativo", true), 200,
                "Fixture nova: fornecedor criado pela API canônica"));
        JsonNode fornecedor = fornecedores.get(0);
        Map<String, Object> req = rascunho(c, "UI-" + etiqueta, 5, hoje, hoje.plusDays(120));
        req.put("fornecedorId", fornecedor.path("id").asLong()); req.put("cnpjEmpresa", fornecedor.path("cnpj").asText());
        req.put("razaoSocialEmpresa", fornecedor.path("razaoSocial").asText()); req.put("nomeFantasiaEmpresa", "Sintética Navegador " + etiqueta);
        long rascunho = enviar(c.localA, "/api/contratos", req, 201, "Rascunho que a UI deve publicar, ainda sem evento de publicação").path("id").asLong();
        long composicao = enviar(c.localA, base(rascunho) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201,
                "Composição exata cinco, com catálogos e fontes vigentes").path("id").asLong();
        Map<String, Object> reqFuturo = new LinkedHashMap<>(req);
        reqFuturo.put("numeroContrato", "UI-FUTURO-" + etiqueta); reqFuturo.put("dataInicio", futuro); reqFuturo.put("dataFim", futuro.plusDays(120));
        long contratoFuturo = enviar(c.localA, "/api/contratos", reqFuturo, 201, "Rascunho futuro sem publicação inventada").path("id").asLong();
        enviar(c.localA, base(contratoFuturo) + "/composicoes", composicao(c, c.cargoA.getId(), 5), 201, "Composição do rascunho futuro");
        JsonNode motivo = enviar(c.localA, "/api/motivos-desligamento", mapa("descricao", "Motivo sintético navegador " + etiqueta,
                "categoria", "DESLIGAMENTO_INDIVIDUAL", "ativo", true, "gestaoLocalId", c.gestaoA.getId()), 201,
                "Motivo vigente criado com origem temporal pelo serviço real");

        Map<String, Object> ids = mapa("contrato_rascunho", rascunho, "composicao_contrato_rascunho", composicao,
                "contrato_futuro", contratoFuturo, "fornecedor", fornecedor.path("id").asLong(),
                "gestao_origem", c.gestaoA.getId(), "gestao_destino", c.gestaoB.getId(),
                "superintendencia_origem", c.seA.getId(), "superintendencia_destino", c.seB.getId(),
                "lotacao", c.lotacaoA.getId(), "jornada", c.jornadaA.getId(), "cargo", c.cargoA.getId(), "tipo", c.tipoA.getId(),
                "destino_lotacao", c.lotacaoB.getId(), "destino_jornada", c.jornadaB.getId(), "destino_cargo", c.cargoB.getId(), "destino_tipo", c.tipoB.getId(),
                "motivo_desligamento", motivo.path("id").asLong(), "contrato_a", c.contratoA.getId(), "contrato_b", c.contratoB.getId(),
                "composicao_a", c.composicaoA.getId(), "composicao_b", c.composicaoB.getId(), "pessoa_base", c.pessoaA.getId(),
                "vinculo_base", c.vinculoA.getId(), "alocacao_base", c.alocacaoA.getId(), "empregado_base", c.empregadoA.getId());
        JsonNode draft = consultar(c.localA, base(rascunho), "DTO observado do rascunho");
        JsonNode futuroDto = consultar(c.localA, base(contratoFuturo), "DTO observado do rascunho futuro");
        assertEquals("RASCUNHO", draft.path("estadoNegocial").asText()); assertEquals("RASCUNHO", futuroDto.path("estadoNegocial").asText());
        long eventosRascunho = emTransacao(() -> em.createQuery("select count(e) from EventoCicloVidaContrato e where e.contrato.id=:id", Long.class)
                .setParameter("id", rascunho).getSingleResult());
        assertEquals(0L, eventosRascunho);
        Map<String, Object> pendente = mapa("pessoa", mapa("nome", c.pessoaA.getNome(), "cpf", novoCpf()),
                "contratoId", rascunho, "composicaoId", composicao, "dataInicio", hoje, "matricula", "NAVEGADOR-" + etiqueta);
        // Esta pessoa NÃO é persistida: pessoa-vinculo.cjs deve observar os novos IDs no POST da UI.
        Map<String, Object> objetos = mapa("contrato_rascunho", draft, "contrato_futuro", futuroDto,
                "contrato_a", consultar(c.localA, base(c.contratoA.getId()), "Contrato base A confirmado"),
                "contrato_b", consultar(c.localB, base(c.contratoB.getId()), "Contrato base B confirmado"), "fornecedores", fornecedores);
        Map<String, Object> ocorrencia = prepararOcorrencia(c, hoje, privado);
        ids.put("ocorrencia_restrita", ocorrencia.get("id")); ids.put("anexo_restrito", ocorrencia.get("anexoId"));
        Map<String, Object> painel = prepararPainel(contas, privado);
        Map<String, Object> relatorio = prepararRelatorio(contas, privado);
        Map<String, Object> fixture = mapa("synthetic", true, "status", ESTADO, "acceptance", false, "preparedAt", Instant.now(),
                "accounts", contas, "ids", ids, "dates", mapa("today", hoje, "future", futuro), "objects", objetos,
                "pending_vinculo_request", pendente, "homonym", mapa("existingPessoaId", c.pessoaA.getId(), "pendingHasSameName", true),
                "ocorrencia_restrita", ocorrencia, "painel_bissexto", painel, "relatorio41", relatorio,
                "runtime", mapa("jarSha256", JAR_SHA, "database", banco.get("database"), "postgresVersionNum", 180006,
                        "uploads", RUNTIME.resolve("storage/uploads").toString(), "relatorioTemp", RUNTIME.resolve("storage/relatorios").toString(),
                        "auditFallback", RUNTIME.resolve("storage/audit-fallback.jsonl").toString(), "redisFlushRequired", false),
                "limits", List.of("Somente preparação: MockMvc/filtros/Spring/JPA/PostgreSQL, sem TCP ou navegador executado",
                        "Sem concessão contextual pré-criada: deve ser concedida pela UI", "Sem novos poderes de escrita",
                        "Job200, RI03, 2FA e corpus histórico sem origem não são preparados aqui", "Executar os drivers não equivale a aprovar as 24 jornadas"));
        Path arquivo = privado.resolve("e2e-fixture.json");
        gravar(arquivo, fixture); // ÚNICO artefato que recebe a senha sintética, com 0600.
        gravar(privado.resolve("e2e-business.json"), mapa()); // IDs de negócios serão escritos só pelos drivers.
        Map<String, Object> recibo = mapa("status", ESTADO, "acceptance", false, "runtime", RUNTIME.toString(),
                "fixtureSha256", hashArquivo(arquivo), "jarSha256", JAR_SHA, "banco", banco,
                "contas", contas.size(), "fornecedoresCriados", 6, "ids", ids, "today", hoje, "future", futuro,
                "painel", mapa("pontosAtuais", 15, "pontosComparativos", 14, "atualFinal", 229, "comparativoFinal", 128, "alocados", 0, "lancamentos", 0),
                "relatorio", mapa("registrosA", 41, "registrosB", 1, "pdfPaginas", 2),
                "metodo", "PreparacaoFrontendRealIT#prepararNavegador", "camada", "MOCKMVC-SPRING-JPA-POSTGRESQL-PREPARATION",
                "nenhumServidorIniciado", true, "nenhumTesteAprovadoReexecutado", true);
        gravar(RUNTIME.resolve("preparacao-sem-credenciais.json"), recibo);
        SnapshotQuantitativos.registrar(mapper, "frontend-native", "preparation-only", recibo);
        assertEquals(hoje, LocalDate.now(ZONA), "Preparação não atravessa uma virada física de data");
    }

    private Map<String, Object> prepararPainel(Map<String, Object> contas, Path privado) throws Exception {
        Cenario suporte = criarCenarioPersistido();
        conta(contas, "painel_gestor", suporte.localA);
        PainelBissextoDtoRealHttpIT produtor = new PainelBissextoDtoRealHttpIT();
        factory.autowireBean(produtor); // Injeta apenas dependências do contexto já existente; não registra bean ou teste.
        dependencias(produtor, "em", "transactionManager");
        Object massa = Objects.requireNonNull(ReflectionTestUtils.invokeMethod(produtor, "preparar", suporte));
        long lotacao = numero(massa, "lotacao"), jornada = numero(massa, "jornada"), cargo = numero(massa, "cargo");
        Object contratos = campo(massa, "contratos"), esperado = campo(massa, "esperado");
        JsonNode sem = painel(suporte, lotacao, jornada, cargo, false), com = painel(suporte, lotacao, jornada, cargo, true);
        assertEquals("2024-02-15", com.path("inicio").asText()); assertEquals("2024-02-29", com.path("fim").asText());
        assertEquals(sem.path("serieAtual"), com.path("serieAtual")); assertTrue(sem.has("serieAnoAnterior")); assertTrue(sem.path("serieAnoAnterior").isEmpty());
        assertTrue(sem.path("resumoAnoAnterior").isNull());
        for (int ano : List.of(2023, 2024)) {
            JsonNode serie = com.path(ano == 2024 ? "serieAtual" : "serieAnoAnterior");
            int ultimo = ano == 2024 ? 29 : 28; assertEquals(ultimo - 14, serie.size());
            Set<String> datas = new HashSet<>();
            for (int d = 15; d <= ultimo; d++) {
                JsonNode ponto = serie.get(d - 15);
                assertEquals(LocalDate.of(ano, 2, d).toString(), ponto.path("data").asText()); assertTrue(datas.add(ponto.path("data").asText()));
                assertEquals((ano == 2024 ? 200 : 100) + d, ponto.path("efetivoPrevisto").asInt());
                for (String zero : List.of("efetivoAlocado", "registrosLancados")) {
                    assertTrue(ponto.hasNonNull(zero), "Zero deve vir do campo real do DTO: " + zero); assertEquals(0, ponto.path(zero).asInt());
                }
            }
        }
        assertEquals(229, com.path("resumoAtual").path("efetivoPrevisto").asInt());
        assertEquals(128, com.path("resumoAnoAnterior").path("efetivoPrevisto").asInt());
        Path dir = subdiretorio(privado.resolve("painel-bissexto"));
        gravar(dir.resolve("dto-sem-comparacao.json"), sem); gravar(dir.resolve("dto-comparado.json"), com);
        gravar(dir.resolve("esperado-independente.json"), esperado);
        return mapa("accountName", "painel_gestor", "referencia", "2024-02-29", "periodo", "QUINZENAL", "lotacaoId", lotacao,
                "jornadaId", jornada, "cargoId", cargo, "tipoId", numero(massa, "tipo"), "contratoIds", contratos,
                "contratosPorDia", campo(massa, "contratosPorDia"), "esperado", esperado,
                "dtoComparado", com, "dtoSemComparacao", sem, "dtoComparadoSha256", hashArquivo(dir.resolve("dto-comparado.json")),
                "expected", mapa("pontosAtuais", 15, "pontosComparativos", 14, "atualFinal", 229, "comparativoFinal", 128,
                        "alocados", 0, "lancamentos", 0, "comparativo29Inventado", false),
                "origem", "Helper privado preparar(Cenario) do PainelBissextoDtoRealHttpIT, sem invocar seu @Test; fatos novos 2023/2024 em escopo próprio");
    }
    private JsonNode painel(Cenario c, long l, long j, long cargo, boolean comparar) throws Exception {
        return dados(executar(c.localA, get("/api/painel/efetivo").param("referencia", "2024-02-29").param("periodo", "QUINZENAL")
                .param("lotacaoId", Long.toString(l)).param("jornadaId", Long.toString(j)).param("cargoId", Long.toString(cargo))
                .param("compararAnoAnterior", Boolean.toString(comparar)), 200, "DTO real do produtor bissexto; somente preparação"));
    }

    private Map<String, Object> prepararRelatorio(Map<String, Object> contas, Path privado) throws Exception {
        Path dir = subdiretorio(privado.resolve("relatorio41"));
        RelatorioJarMultipaginaIT produtor = new RelatorioJarMultipaginaIT();
        factory.autowireBean(produtor);
        dependencias(produtor, "em", "transactionManager", "mapper", "projecao", "passwordEncoder");
        ReflectionTestUtils.setField(produtor, "evidencias", dir);
        Object massa = Objects.requireNonNull(ReflectionTestUtils.invokeMethod(produtor, "preparar"));
        Cenario suporte = (Cenario) campo(massa, "c"); LocalDate data = (LocalDate) campo(massa, "data");
        List<?> pessoas = (List<?>) campo(massa, "pessoas"); Object controle = campo(massa, "controleB");
        assertEquals(41, pessoas.size());
        conta(contas, "relatorios_gestor", suporte.localA); conta(contas, "relatorios_destino", suporte.localB);
        conta(contas, "relatorios_operacional", suporte.operacionalA);
        List<Map<String, Object>> esperadas = new ArrayList<>();
        for (Object pessoa : pessoas) esperadas.add(lancarRelatorio(suporte.localA, pessoa, data));
        Map<String, Object> esperadoB = lancarRelatorio(suporte.localB, controle, data);
        String filtroA = filtro(suporte, data, false), filtroB = filtro(suporte, data, true);
        String api = "/api/relatorios/frequencia-consolidada";
        JsonNode completo = consultar(suporte.localA, api + filtroA, "Consolidado real de 41 no recorte próprio");
        JsonNode pagina0 = consultar(suporte.localA, api + "/paginada" + filtroA + "&page=0&size=25", "Página real 25");
        JsonNode pagina1 = consultar(suporte.localA, api + "/paginada" + filtroA + "&page=1&size=25", "Página real 16");
        assertEquals(41, completo.path("registros").size()); assertEquals(25, pagina0.path("registros").size()); assertEquals(16, pagina1.path("registros").size());
        assertEquals(41, pagina0.path("totalColaboradores").asInt()); assertEquals(41, pagina1.path("totalColaboradores").asInt());
        ReflectionTestUtils.invokeMethod(produtor, "conferirUniverso", completo, pessoas, massa, false);
        JsonNode controleDto = consultar(suporte.localB, api + filtroB, "Controle real do recorte B com uma pessoa");
        ReflectionTestUtils.invokeMethod(produtor, "conferirUniverso", controleDto, List.of(controle), massa, true);
        byte[] pdf = executar(suporte.localA, get(api + "/exportacao" + filtroA + "&formato=PDF"), 200, "PDF backend real da massa41").getResponse().getContentAsByteArray();
        byte[] xlsx = executar(suporte.localA, get(api + "/exportacao" + filtroA + "&formato=XLSX"), 200, "XLSX backend real da massa41").getResponse().getContentAsByteArray();
        gravarBytes(dir.resolve("relatorio-a-41.pdf"), pdf); gravarBytes(dir.resolve("relatorio-a-41.xlsx"), xlsx);
        ReflectionTestUtils.invokeMethod(produtor, "conferirPdf", pdf, completo, pessoas, List.of(controle), 2, "preparacao-pdf-a");
        ReflectionTestUtils.invokeMethod(produtor, "conferirXlsx", xlsx, completo, pessoas, List.of(controle), data, "preparacao-xlsx-a");
        List<Long> nomesPorPagina = new ArrayList<>();
        PdfReader leitor = new PdfReader(pdf);
        try {
            assertEquals(2, leitor.getNumberOfPages()); PdfTextExtractor extrator = new PdfTextExtractor(leitor);
            for (int p = 1; p <= leitor.getNumberOfPages(); p++) {
                String texto = extrator.getTextFromPage(p).replaceAll("\\s+", "");
                nomesPorPagina.add(esperadas.stream().filter(e -> texto.contains(e.get("nome").toString().replaceAll("\\s+", ""))).count());
            }
        } finally { leitor.close(); }
        assertEquals(41L, nomesPorPagina.stream().mapToLong(Long::longValue).sum());
        gravar(dir.resolve("consulta-a-completa.json"), completo); gravar(dir.resolve("pagina-0.json"), pagina0);
        gravar(dir.resolve("pagina-1.json"), pagina1); gravar(dir.resolve("controle-b.json"), controleDto);
        return mapa("accountName", "relatorios_gestor", "controlAccountName", "relatorios_destino", "operationalAccountName", "relatorios_operacional",
                "data", data, "contratoId", suporte.contratoA.getId(), "lotacaoId", suporte.lotacaoA.getId(), "jornadaId", suporte.jornadaA.getId(),
                "controleContratoId", suporte.contratoB.getId(), "controleLotacaoId", suporte.lotacaoB.getId(), "controleJornadaId", suporte.jornadaB.getId(),
                "api", api, "filtroA", filtroA, "filtroB", filtroB, "pessoasEsperadas", esperadas, "controleEsperado", esperadoB,
                "dtoConsolidado", completo, "expected", mapa("registros", 41, "pagina0", 25, "pagina1", 16, "pdfPaginas", 2,
                        "nomesPorPaginaObservados", nomesPorPagina, "minutosFisicosPagaveisPorPessoa", "241..281", "controleMinutos", 329),
                "pdfArquivo", dir.resolve("relatorio-a-41.pdf").toString(), "pdfSha256", hashArquivo(dir.resolve("relatorio-a-41.pdf")),
                "xlsxArquivo", dir.resolve("relatorio-a-41.xlsx").toString(), "xlsxSha256", hashArquivo(dir.resolve("relatorio-a-41.xlsx")),
                "origem", "Helper privado preparar() e oráculos de conteúdo do RelatorioJarMultipaginaIT; frequências/exportações reais via MockMvc, sem executar seu @Test/JAR");
    }

    private Map<String, Object> lancarRelatorio(UsuarioAplicacao autor, Object pessoa, LocalDate data) throws Exception {
        long vinculo = numero(pessoa, "vinculo"), empregado = numero(pessoa, "empregado"); int minutos = (int) numero(pessoa, "minutos");
        JsonNode f = enviar(autor, "/api/frequencias", mapa("vinculoContratualId", vinculo, "dataReferencia", data, "status", "PRESENCA_PARCIAL",
                "horasTrabalhadas", String.format(Locale.ROOT, "%02d:%02d", minutos / 60, minutos % 60), "horarioSaida", "12:00",
                "justificativaRetificacao", "Duração sintética discriminante para o navegador, sem alterar jornadas",
                "chaveIdempotencia", "frontend-rel41-" + UUID.randomUUID()), 200, "Fonte HTTP real da massa de relatório");
        assertEquals(empregado, f.path("empregado").path("id").asLong()); assertEquals(vinculo, f.path("vinculoContratualId").asLong());
        long id = f.path("id").asLong(); assertTrue(id > 0);
        emTransacao(() -> { em.clear(); Frequencia relida = em.find(Frequencia.class, id);
            assertEquals(minutos, relida.getMinutosFisicos().intValue()); assertEquals(minutos, relida.getMinutosPagaveis().intValue()); return null; });
        return mapa("empregadoId", empregado, "vinculoId", vinculo, "frequenciaId", id, "nome", campo(pessoa, "nome"), "minutos", minutos);
    }
    private String filtro(Cenario c, LocalDate d, boolean b) {
        return "?inicio=" + d + "&fim=" + d + "&contratoIds=" + (b ? c.contratoB : c.contratoA).getId()
                + "&lotacaoId=" + (b ? c.lotacaoB : c.lotacaoA).getId() + "&jornadaId=" + (b ? c.jornadaB : c.jornadaA).getId();
    }

    private Map<String, Object> prepararOcorrencia(Cenario c, LocalDate hoje, Path privado) throws Exception {
        LocalDate inicio = hoje.minusDays(3); int dias = 3;
        byte[] arquivo = "ANEXO SINTETICO DE OCORRENCIA RESTRITA PARA NAVEGADOR. SEM DADOS PESSOAIS REAIS.\n".getBytes(StandardCharsets.UTF_8);
        JsonNode alvo = dados(executar(c.localA, multipart("/api/ocorrencias")
                .file(new MockMultipartFile("anexos", "anexo-sintetico.txt", "text/plain", arquivo))
                .param("empregadoId", c.empregadoA.getId().toString()).param("motivo", "ATESTADO")
                .param("dataOcorrencia", inicio.toString()).param("dias", Integer.toString(dias))
                .param("observacao", "Narrativa sintética restrita ao alvo, sem PII real")
                .param("classificacoesAnexos", "PESSOAL_RESTRITO").param("chaveIdempotencia", "frontend-ocorrencia-" + UUID.randomUUID()),
                201, "Ocorrência multidia e anexo real, sem concessão pré-criada"));
        long id = alvo.path("id").asLong(), anexo = alvo.path("anexos").get(0).path("id").asLong();
        List<AutorizacaoTemporalOcorrenciaServico.Contexto> prova = emTransacao(() -> {
            em.clear(); Ocorrencia o = em.find(Ocorrencia.class, id); assertNotNull(o); assertEquals(dias, o.getContextos().size());
            return autorizacaoOcorrencia.resolver(o);
        });
        assertEquals(dias, prova.size());
        for (int n = 0; n < dias; n++) {
            var p = prova.get(n); assertEquals(inicio.plusDays(n), p.data()); assertEquals(c.contratoA.getId(), p.contratoId());
            assertEquals(c.vinculoA.getId(), p.vinculoId()); assertEquals(c.alocacaoA.getId(), p.alocacaoId());
            assertEquals(c.gestaoA.getId(), p.gestaoId()); assertEquals(c.seA.getId(), p.estadoId());
        }
        byte[] baixa = executar(c.localA, get("/api/ocorrencias/anexos/" + anexo), 200, "Controle local de integridade do anexo").getResponse().getContentAsByteArray();
        assertArrayEquals(arquivo, baixa);
        executar(c.nacional, get("/api/ocorrencias/" + id).param("finalidade", "Conferencia sintetica UI da ocorrencia alvo"), 403,
                "Beneficiário nacional ainda não recebeu concessão contextual");
        long concessoes = emTransacao(() -> em.createQuery("select count(p) from PermissaoExcepcional p where p.ocorrencia.id=:id", Long.class)
                .setParameter("id", id).getSingleResult()); assertEquals(0L, concessoes);
        gravar(privado.resolve("ocorrencia-prova-todos-os-dias.json"), prova);
        return mapa("id", id, "anexoId", anexo, "empregadoId", c.empregadoA.getId(), "pessoaId", c.pessoaA.getId(),
                "vinculoId", c.vinculoA.getId(), "contratoId", c.contratoA.getId(), "inicio", inicio, "dias", dias,
                "fim", inicio.plusDays(dias - 1), "classificacaoAnexo", "PESSOAL_RESTRITO", "anexoSha256", hash(arquivo), "anexoBytes", arquivo.length,
                "beneficiaryAccountName", "nacional", "adminAccountName", "fixture_admin", "grantAlreadyCreated", false,
                "grantRequest", mapa("tipo", "ACESSO_DADOS_PESSOAIS_RESTRITOS", "ocorrenciaId", id, "usuarioBeneficiarioId", c.nacional.getId(),
                        "finalidade", "Conferencia sintetica UI da ocorrencia alvo", "justificativa", "Leitura institucional sintética somente do fato selecionado",
                        "autorizacaoAdministrativa", "ATO-SINTETICO-NAVEGADOR", "validoAte", Instant.now().plus(Duration.ofDays(1))),
                "genericScopesMustBeOmitted", List.of("contratoId", "gestaoLocalId"), "provaTodosOsDias", prova);
    }

    private void conta(Map<String, Object> contas, String alias, UsuarioAplicacao u) {
        Map<String, Object> conta = emTransacao(() -> { em.clear(); UsuarioAplicacao atual = em.find(UsuarioAplicacao.class, u.getId());
            assertNotNull(atual); assertEquals(Boolean.TRUE, atual.getActive()); assertEquals(Boolean.FALSE, atual.getForceChangePassword());
            assertFalse(Boolean.TRUE.equals(atual.getTwoFactorEnabled()), "2FA efetivo deve estar desabilitado na conta sintética");
            assertTrue(passwordEncoder.matches(SENHA, atual.getPassword()));
            return mapa("id", atual.getId(), "username", atual.getUsername(), "matricula", atual.getMatricula(), "perfil", atual.getPerfil().name(),
                    "gestaoLocalId", atual.getGestaoLocal() == null ? null : atual.getGestaoLocal().getId(),
                    "superintendenciaId", atual.getSuperintendencia() == null ? null : atual.getSuperintendencia().getId(),
                    "password", SENHA, "active", true, "forceChangePassword", false, "twoFactorEnabled", false);
        });
        assertNull(contas.put(alias, conta));
    }
    private Map<String, Object> conferirBanco() throws Exception {
        ConfiguracaoPostgresIntegracao.carregar();
        try (Connection c = dataSource.getConnection()) {
            VerificadorAmbientePostgres186.verificar(c, "fixture-frontend-native");
            try (var s = c.createStatement(); var r = s.executeQuery("""
                SELECT current_database(),current_setting('server_version_num')::integer,
                  d.datdba=r.oid,session_user=current_user,
                  (n.nspowner=r.oid OR (pg_get_userbyid(n.nspowner)='pg_database_owner' AND d.datdba=r.oid)),
                  NOT EXISTS(SELECT 1 FROM pg_class t JOIN pg_namespace p ON p.oid=t.relnamespace
                    WHERE p.nspname=current_schema() AND t.relkind IN ('r','p','S') AND t.relowner<>r.oid)
                FROM pg_roles r JOIN pg_database d ON d.datname=current_database()
                  JOIN pg_namespace n ON n.nspname=current_schema() WHERE r.rolname=current_user
                """)) {
                assertTrue(r.next()); assertEquals(180006, r.getInt(2)); for (int i = 3; i <= 6; i++) assertTrue(r.getBoolean(i));
                return mapa("database", r.getString(1), "postgresVersionNum", r.getInt(2), "limitedRoleVerified", true, "ownershipVerified", true);
            }
        }
    }
    private String novoCpf() {
        return emTransacao(() -> {
            for (;;) { String cpf = cpf(ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999));
                if (em.createQuery("select count(p) from Pessoa p where p.cpf=:cpf", Long.class).setParameter("cpf", cpf).getSingleResult() == 0) return cpf; }
        });
    }
    private String novoCnpj() {
        return emTransacao(() -> {
            for (;;) {
                String valor = "73" + String.format(Locale.ROOT, "%06d", ThreadLocalRandom.current().nextInt(1_000_000)) + "0001";
                for (int[] pesos : List.of(new int[]{5,4,3,2,9,8,7,6,5,4,3,2}, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2})) {
                    int soma = 0; for (int i = 0; i < pesos.length; i++) soma += (valor.charAt(i) - '0') * pesos[i];
                    int resto = soma % 11; valor += resto < 2 ? "0" : Integer.toString(11 - resto);
                }
                if (em.createQuery("select count(f) from Fornecedor f where f.cnpj=:cnpj", Long.class).setParameter("cnpj", valor).getSingleResult() == 0) return valor;
            }
        });
    }
    private Object campo(Object record, String nome) { return Objects.requireNonNull(ReflectionTestUtils.invokeMethod(record, nome), nome); }
    private long numero(Object record, String nome) { return ((Number) campo(record, nome)).longValue(); }
    private void dependencias(Object produtor, String... campos) { for (String campo : campos) assertNotNull(ReflectionTestUtils.getField(produtor, campo), campo); }

    private static Path diretorioRuntime() {
        try {
            String definido = System.getProperty("sgt.frontend.fixture.runtime", System.getenv("SGT_TEST_RUNTIME"));
            if (definido != null && !definido.isBlank()) {
                Path p = Path.of(definido); if (!p.isAbsolute()) throw new IllegalArgumentException("Runtime explícito deve ser absoluto");
                return subdiretorio(p.normalize());
            }
            Path raiz = subdiretorio(Path.of("target/frontend-native").toAbsolutePath());
            return Files.createTempDirectory(raiz, "runtime-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } catch (Exception e) { throw new IllegalStateException("Não foi possível criar o runtime privado novo", e); }
    }
    private static Path subdiretorio(Path p) throws Exception {
        assertFalse(Files.isSymbolicLink(p)); Files.createDirectories(p, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwx------")); return p;
    }
    private void gravar(Path p, Object valor) throws Exception { gravarBytes(p, (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(valor) + "\n").getBytes(StandardCharsets.UTF_8)); }
    private void gravarBytes(Path p, byte[] bytes) throws Exception {
        Files.createFile(p, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try (FileChannel c = FileChannel.open(p, StandardOpenOption.WRITE)) { ByteBuffer b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) c.write(b); c.force(true); }
        try (FileChannel d = FileChannel.open(p.getParent(), StandardOpenOption.READ)) { d.force(true); }
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String hashArquivo(Path p) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(p)) { byte[] buffer = new byte[65_536]; for (int n; (n = in.read(buffer)) >= 0;) if (n > 0) d.update(buffer, 0, n); }
        return HexFormat.of().formatHex(d.digest());
    }
}
