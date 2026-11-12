package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.infra.ConfiguracaoPostgresIntegracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ARQ-002/M19/ROP-041/042: JAR/Tomcat reais, 41 pessoas A e uma pessoa B.
 * A preparação JPA confirma identidades e fontes temporais; toda frequência,
 * consulta e exportação passa pelo HTTP autenticado do mesmo JAR.
 * Não comprova Chromium, paginação visual, PDF/UA ou homologação integral.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RelatorioJarMultipaginaIT extends ContratosHttpBase {
    private static final int QUANTIDADE = 41;
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String API = "/api/relatorios/frequencia-consolidada";
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final Path PREPARO = diretorioPrivado("preparo-");
    private Path evidencias;
    private URI origem;
    private final Map<String, Boolean> checks = new LinkedHashMap<>();
    private final Map<String, Object> arquivos = new LinkedHashMap<>();

    @DynamicPropertySource
    static void armazenamentoPreparador(DynamicPropertyRegistry propriedades) {
        propriedades.add("file.upload-dir", () -> PREPARO.resolve("uploads").toString());
        propriedades.add("audit.fallback-log", () -> PREPARO.resolve("auditoria-fallback.jsonl").toString());
        propriedades.add("app.relatorios.temp.diretorio", () -> PREPARO.resolve("relatorios").toString());
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.MINUTES)
    void mesmoJarExportaQuarentaEUmEmDuasPaginasPdfEPaginacaoApiVinteECincoMaisDezesseis() throws Exception {
        evidencias = diretorioPrivado("cenario-");
        Files.createFile(evidencias.resolve("http-status.jsonl"), permissaoArquivo());
        Path jar = Path.of("target/SGT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "Empacotar o candidato corrente antes do IT");
        String hashJar = hashArquivo(jar);
        Process filho = null;
        boolean concluido = false;
        try {
            conferirClasses(jar);
            Massa massa = preparar();
            filho = iniciar(jar, hashJar);
            try (Sessao gestorA = login(massa.c().localA); Sessao gestorB = login(massa.c().localB);
                 Sessao operacional = login(massa.c().operacionalA)) {
                Set<Long> frequencias = new HashSet<>();
                for (PessoaEsperada pessoa : massa.pessoas())
                    registrar(gestorA, pessoa, massa.data(), frequencias);
                registrar(gestorB, massa.controleB(), massa.data(), frequencias);
                assertEquals(QUANTIDADE + 1, frequencias.size());
                checks.put("quarentaEDuasFontesHttpConfirmadas", true);
                gravar("fontes-http-confirmadas.json", mapa("frequencias", frequencias.stream().sorted().toList()));

                String filtroA = filtro(massa, false);
                JsonNode completo = gestorA.get(API + filtroA, 200);
                conferirUniverso(completo, massa.pessoas(), massa, false);
                assertEquals(QUANTIDADE, completo.path("totais").path("presencasParciais").asInt());
                assertEquals(somaMinutos(massa.pessoas()), completo.path("totais").path("minutosPagaveis").asInt());
                assertEquals(somaMinutos(massa.pessoas()), completo.path("totais").path("minutosFisicos").asInt());
                assertEquals(480 * QUANTIDADE, completo.path("totais").path("minutosPrevistos").asInt());
                JsonNode primeira = gestorA.get(API + "/paginada" + filtroA + "&page=0&size=25", 200);
                JsonNode segunda = gestorA.get(API + "/paginada" + filtroA + "&page=1&size=25", 200);
                conferirPagina(primeira, 0, 25, true, false);
                conferirPagina(segunda, 1, 16, false, true);
                List<JsonNode> reunidas = new ArrayList<>();
                primeira.path("registros").forEach(reunidas::add);
                segunda.path("registros").forEach(reunidas::add);
                assertEquals(QUANTIDADE, reunidas.size());
                assertEquals(QUANTIDADE, reunidas.stream().map(l -> l.path("empregadoId").asLong()).distinct().count());
                List<JsonNode> linhas = new ArrayList<>();
                completo.path("registros").forEach(linhas::add);
                assertEquals(linhas, reunidas, "25+16 preserva conteúdo, ordem, valores e todos os IDs do consolidado");
                for (JsonNode pagina : List.of(primeira, segunda)) {
                    for (String campo : List.of("inicio", "fim", "contratos", "calculoVersao", "revisao"))
                        assertEquals(completo.path(campo), pagina.path(campo), "Mesmo recorte e cálculo: " + campo);
                }
                gravar("consulta-a-completa.json", completo);
                gravar("consulta-a-pagina-0.json", primeira);
                gravar("consulta-a-pagina-1.json", segunda);
                checks.put("apiQuarentaEUmSemPerdaOuDuplicacaoEmVinteECincoMaisDezesseis", true);

                JsonNode controle = gestorB.get(API + filtro(massa, true), 200);
                conferirUniverso(controle, List.of(massa.controleB()), massa, true);
                gravar("consulta-b-uma-pessoa.json", controle);
                checks.put("controleOutroEscopoComUmaPessoa", true);

                // Gravar ambos os arquivos antes dos oráculos de conteúdo: falhas preservam seus bytes.
                byte[] pdfA = baixar(gestorA, filtroA, "PDF", "relatorio-a-41.pdf");
                byte[] xlsxA = baixar(gestorA, filtroA, "XLSX", "relatorio-a-41.xlsx");
                byte[] pdfB = baixar(gestorB, filtro(massa, true), "PDF", "relatorio-b-1.pdf");
                byte[] xlsxB = baixar(gestorB, filtro(massa, true), "XLSX", "relatorio-b-1.xlsx");
                conferirPdf(pdfA, completo, massa.pessoas(), List.of(massa.controleB()), 2, "pdf-a");
                checks.put("pdfExatamenteDuasPaginasQuarentaEUmNomesEValores", true);
                conferirXlsx(xlsxA, completo, massa.pessoas(), List.of(massa.controleB()), massa.data(), "xlsx-a");
                checks.put("xlsxRecorteCompletoQuarentaEUmLinhasEValores", true);
                conferirPdf(pdfB, controle, List.of(massa.controleB()), massa.pessoas(), 1, "pdf-b");
                conferirXlsx(xlsxB, controle, List.of(massa.controleB()), massa.pessoas(), massa.data(), "xlsx-b");
                assertNotEquals(hash(pdfA), hash(pdfB), "Filtro de uma pessoa não reutiliza bytes do recorte41");
                assertNotEquals(hash(xlsxA), hash(xlsxB), "XLSX corresponde ao filtro da resposta atual");
                checks.put("arquivosControleUmaPessoaSemVazamentoEntreFiltros", true);

                gestorB.get(API + filtroA, 404);
                gestorB.get(API + "/paginada" + filtroA + "&page=0&size=25", 404);
                operacional.get(API + filtroA, 403);
                operacional.get(API + "/paginada" + filtroA + "&page=0&size=25", 403);
                for (String formato : List.of("PDF", "XLSX")) {
                    gestorB.enviar("GET", API + "/exportacao" + filtroA + "&formato=" + formato, null, 404);
                    operacional.enviar("GET", API + "/exportacao" + filtroA + "&formato=" + formato, null, 403);
                }
                checks.put("negativasDeEscopoEPerfilNaConsultaEEmAmbosExportadores", true);
                assertTrue(filho.isAlive(), "O mesmo JAR permanece saudável após as exportações e negativas");
                assertEquals(hashJar, hashArquivo(jar));
                checks.put("mesmoJarClassesConferidasEFilhoSaudavel", true);
                concluido = true;
            }
        } finally {
            boolean interrompido = Thread.interrupted();
            try { encerrar(filho); }
            finally {
                interrompido |= Thread.interrupted();
                try {
                    boolean encerrado = filho == null || !filho.isAlive();
                    checks.put("filhoProprioEncerrado", encerrado);
                    gravar("resultado.json", mapa("status", concluido && encerrado ? "PASS" : "INCOMPLETE_OR_FAILED",
                            "jar", jar.toString(), "jarSha256", hashJar, "quantidadeA", QUANTIDADE, "quantidadeB", 1,
                            "checks", checks, "arquivos", arquivos, "pid", filho == null ? null : filho.pid(),
                            "filhoVivo", !encerrado, "instante", Instant.now().toString(),
                            "limites", "HTTP/Tomcat/backend; Chromium, UI/LAN, PDF/UA e aceite global não avaliados"));
                } finally { if (interrompido) Thread.currentThread().interrupt(); }
            }
        }
    }

    private record PessoaEsperada(long empregado, long vinculo, String nome, int minutos) {}
    private record Massa(Cenario c, LocalDate data, List<PessoaEsperada> pessoas, PessoaEsperada controleB) {}

    private void validarBancoIsolado() {
        ConfiguracaoPostgresIntegracao banco = ConfiguracaoPostgresIntegracao.carregar();
        assertEquals("127.0.0.1", URI.create(banco.url().substring("jdbc:".length())).getHost(),
                "Somente PostgreSQL do laboratório local explícito");
        emTransacao(() -> {
            Object[] identidade = (Object[]) em.createNativeQuery("""
                SELECT current_setting('server_version_num')::integer,current_database(),
                  d.datdba=r.oid,
                  NOT(r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls),
                  NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid),
                  session_user=current_user,
                  (n.nspowner=r.oid OR (pg_get_userbyid(n.nspowner)='pg_database_owner' AND d.datdba=r.oid))
                    AND has_schema_privilege(current_user,'public','USAGE')
                    AND has_schema_privilege(current_user,'public','CREATE')
                FROM pg_roles r JOIN pg_database d ON d.datname=current_database()
                  JOIN pg_namespace n ON n.nspname='public' WHERE r.rolname=current_user
                """).getSingleResult();
            assertEquals(180006, ((Number) identidade[0]).intValue());
            String bancoEsperado = System.getenv("SGT_INTEGRATION_DATABASE_EXPECTED");
            if (bancoEsperado == null) {
                assertTrue(identidade[1].toString().startsWith("sgt_20261109_"),
                        "Laboratório diferente exige SGT_INTEGRATION_DATABASE_EXPECTED explícito");
            } else {
                assertFalse(bancoEsperado.isBlank(), "Nome explícito de banco não pode ser vazio");
                assertEquals(bancoEsperado, identidade[1].toString(), "Banco real deve corresponder ao laboratório autorizado");
            }
            for (int i = 2; i < identidade.length; i++) assertEquals(Boolean.TRUE, identidade[i], "Role limitada/ownership explícitos");
            return null;
        });
    }

    private Massa preparar() throws Exception {
        validarBancoIsolado(); // Provar identidade e privilégios antes de criar a massa.
        Cenario c = criarCenarioPersistido();
        Massa massa = emTransacao(() -> {
            Contrato contrato = em.find(Contrato.class, c.contratoA.getId());
            ComposicaoContratual composicao = em.find(ComposicaoContratual.class, c.composicaoA.getId());
            contrato.setEfetivoPrevisto(QUANTIDADE); contrato.setEfetivoOriginalPublicado(QUANTIDADE);
            composicao.setEfetivoPrevisto(QUANTIDADE);
            em.createQuery("select v from VersaoContratoNegocial v where v.contrato.id=:id", VersaoContratoNegocial.class)
                    .setParameter("id", contrato.getId()).getResultList().forEach(v -> v.setEfetivoPrevisto(QUANTIDADE));
            Pessoa primeira = em.find(Pessoa.class, c.pessoaA.getId());
            primeira.setNome(nome(1)); em.find(Empregado.class, c.empregadoA.getId()).setNome(primeira.getNome());
            List<PessoaEsperada> pessoas = new ArrayList<>();
            pessoas.add(new PessoaEsperada(c.empregadoA.getId(), c.vinculoA.getId(), primeira.getNome(), 241));
            origemNominal(em.find(VinculoContratual.class, c.vinculoA.getId()), c.gestaoA, c.localA);
            Set<String> cpfs = new HashSet<>(em.createQuery("select p.cpf from Pessoa p", String.class).getResultList());
            int semente = 92000000;
            for (int n = 2; n <= QUANTIDADE; n++) {
                String documento;
                do { documento = cpf(semente++); } while (!cpfs.add(documento));
                Pessoa pessoa = pessoa(nome(n), documento);
                VinculoContratual vinculo = vinculo(pessoa, contrato);
                AlocacaoVinculo alocacao = alocacao(vinculo, composicao);
                Empregado empregado = projecao.projetar(vinculo, alocacao);
                origemNominal(vinculo, c.gestaoA, c.localA);
                pessoas.add(new PessoaEsperada(empregado.getId(), vinculo.getId(), pessoa.getNome(), 240 + n));
            }
            Pessoa controle = em.find(Pessoa.class, c.pessoaB.getId());
            controle.setNome("Pessoa Controle Escopo B");
            em.find(Empregado.class, c.empregadoB.getId()).setNome(controle.getNome());
            origemNominal(em.find(VinculoContratual.class, c.vinculoB.getId()), c.gestaoB, c.localB);
            em.flush(); em.clear();
            LocalDate data = null;
            LocalDate hoje = LocalDate.now(ZONA);
            assertFalse(hoje.minusDays(20).isBefore(INICIO));
            assertFalse(hoje.isAfter(FIM));
            for (int d = 1; d <= 20; d++) {
                LocalDate candidata = hoje.minusDays(d);
                if (candidata.getDayOfWeek().getValue() > 5) continue;
                long eventos = em.createQuery("select count(e) from CalendarioEvento e where e.data=:data and e.ativo=true "
                                + "and e.cancelado=false and upper(e.tipo) in ('FERIADO','DIA_DESCANSO')", Long.class)
                        .setParameter("data", candidata).getSingleResult();
                if (eventos == 0) { data = candidata; break; }
            }
            assertNotNull(data, "Dia útil sintético dentro da cobertura temporal e da janela padrão30");
            assertEquals(QUANTIDADE, pessoas.stream().map(PessoaEsperada::empregado).distinct().count());
            assertEquals(QUANTIDADE, pessoas.stream().map(PessoaEsperada::vinculo).distinct().count());
            assertEquals(QUANTIDADE, pessoas.stream().map(PessoaEsperada::nome).distinct().count());
            return new Massa(c, data, List.copyOf(pessoas),
                    new PessoaEsperada(c.empregadoB.getId(), c.vinculoB.getId(), controle.getNome(), 329));
        });
        gravar("fixture-sintetica-sem-credenciais.json", mapa("data", massa.data().toString(), "pessoasA", massa.pessoas(),
                "controleB", massa.controleB(), "contratoA", c.contratoA.getId(), "contratoB", c.contratoB.getId(),
                "lotacaoA", c.lotacaoA.getId(), "jornadaA", c.jornadaA.getId(), "fontesNominaisDocumentadas", 42,
                "duracoesFisicasUnicasEmMinutos", "241..281 em A; 329 em B"));
        return massa;
    }

    /** Fonte nominal sintética explícita, equivalente ao helper do suporte para cada vínculo dos dois escopos. */
    private void origemNominal(VinculoContratual vinculo, GestaoLocal gestao, UsuarioAplicacao autor) {
        assertEquals(0L, em.createQuery("select count(e) from EventoVinculoContratual e where e.vinculo.id=:id", Long.class)
                .setParameter("id", vinculo.getId()).getSingleResult());
        EventoVinculoContratual evento = new EventoVinculoContratual();
        evento.setVinculo(vinculo); evento.setTipoEvento(TipoEventoVinculoContratual.CRIACAO);
        evento.setRelacaoPrecedencia(TipoRelacaoEventoVinculo.ORIGEM);
        evento.setDataEfetiva(INICIO); evento.setRegistradoEm(INICIO.atStartOfDay(ZoneOffset.UTC).toInstant());
        evento.setInicioNovo(vinculo.getDataInicio()); evento.setFimNovo(vinculo.getDataFim());
        evento.setMatriculaNova(vinculo.getMatricula());
        evento.setPessoaNomeFotografia(vinculo.getPessoa().getNome()); evento.setPessoaCpfFotografia(vinculo.getPessoa().getCpf());
        evento.setContratoNumeroFotografia(vinculo.getContrato().getNumeroContrato());
        evento.setContratoAnoFotografia(vinculo.getContrato().getAnoContrato());
        evento.setGestaoLocalIdFotografia(gestao.getId()); evento.setGestaoLocalNomeFotografia(gestao.getNome());
        evento.setResponsavelId(autor.getId()); evento.setResponsavelUsername(autor.getUsername());
        String referencia = "RELATORIO-JAR-41-" + vinculo.getId();
        evento.setJustificativa("Fonte sintética explícita da identidade e admissão para " + referencia);
        evento.setEvidencia("SEI-FIXTURE-" + referencia); evento.setAutorizacao("ATO-FIXTURE-" + referencia);
        evento.setHashEvento(com.example.siggtrat.utilitario.UtilitarioHashSeguranca.sha256Hexadecimal(
                "FIXTURE-ORIGEM-NOMINAL|" + referencia + "|" + vinculo.getId() + "|" + INICIO + "|"
                        + vinculo.getDataInicio() + "|" + vinculo.getDataFim() + "|" + vinculo.getMatricula() + "|"
                        + vinculo.getPessoa().getNome() + "|" + vinculo.getPessoa().getCpf()));
        em.persist(evento);
    }

    private void registrar(Sessao sessao, PessoaEsperada pessoa, LocalDate data, Set<Long> frequencias) throws Exception {
        JsonNode criada = sessao.json("/api/frequencias", mapa("vinculoContratualId", pessoa.vinculo(),
                "dataReferencia", data.toString(), "status", "PRESENCA_PARCIAL", "horasTrabalhadas", horas(pessoa.minutos()),
                "horarioSaida", "12:00", "justificativaRetificacao", "Duração manual sintética individual conferida para relatório",
                "chaveIdempotencia", "relatorio-41-" + UUID.randomUUID()), 200);
        assertTrue(criada.path("id").asLong() > 0 && frequencias.add(criada.path("id").asLong()));
        assertEquals(pessoa.empregado(), criada.path("empregado").path("id").asLong());
        assertEquals(pessoa.vinculo(), criada.path("vinculoContratualId").asLong());
        emTransacao(() -> {
            em.clear();
            Frequencia f = em.find(Frequencia.class, criada.path("id").asLong());
            assertNotNull(f);
            assertEquals(pessoa.minutos(), f.getMinutosFisicos().intValue());
            assertEquals(pessoa.minutos(), f.getMinutosPagaveis().intValue());
            assertEquals(0, f.getMinutosNoturnosReais().intValue());
            assertEquals(data, f.getDataReferencia());
            assertEquals(pessoa.vinculo(), f.getVinculoContratual().getId().longValue());
            return null;
        });
    }

    private String filtro(Massa massa, boolean ladoB) {
        Cenario c = massa.c();
        return "?inicio=" + massa.data() + "&fim=" + massa.data()
                + "&contratoIds=" + (ladoB ? c.contratoB : c.contratoA).getId()
                + "&lotacaoId=" + (ladoB ? c.lotacaoB : c.lotacaoA).getId()
                + "&jornadaId=" + (ladoB ? c.jornadaB : c.jornadaA).getId();
    }

    private void conferirUniverso(JsonNode resposta, List<PessoaEsperada> esperado, Massa massa, boolean ladoB) {
        assertEquals(massa.data().toString(), resposta.path("inicio").asText());
        assertEquals(massa.data().toString(), resposta.path("fim").asText());
        assertEquals(esperado.size(), resposta.path("registros").size());
        Map<Long, PessoaEsperada> porId = new HashMap<>();
        esperado.forEach(p -> assertNull(porId.put(p.empregado(), p)));
        Set<Long> vistos = new HashSet<>();
        long contrato = (ladoB ? massa.c().contratoB : massa.c().contratoA).getId();
        assertEquals(1, resposta.path("contratos").size());
        assertEquals(contrato, resposta.path("contratos").get(0).asLong());
        for (JsonNode linha : resposta.path("registros")) {
            long id = linha.path("empregadoId").asLong();
            assertTrue(vistos.add(id)); PessoaEsperada pessoa = porId.get(id); assertNotNull(pessoa);
            assertEquals(pessoa.nome(), linha.path("nome").asText());
            assertEquals(contrato, linha.path("contratoId").asLong());
            assertEquals((ladoB ? massa.c().lotacaoB : massa.c().lotacaoA).getId().longValue(), linha.path("lotacaoId").asLong());
            assertEquals((ladoB ? massa.c().jornadaB : massa.c().jornadaA).getId().longValue(), linha.path("jornadaId").asLong());
            JsonNode dia = linha.path("diario").path(massa.data().toString());
            assertEquals(1, linha.path("diario").size());
            assertTrue(dia.path("lancado").asBoolean()); assertTrue(dia.path("diaTrabalho").asBoolean());
            assertEquals("PRESENCA_PARCIAL", dia.path("status").asText());
            assertEquals(480, dia.path("minutosPrevistos").asInt());
            for (String campo : List.of("minutosFisicos", "minutosPagaveis")) {
                assertEquals(pessoa.minutos(), dia.path(campo).asInt());
                assertEquals(pessoa.minutos(), linha.path("totais").path(campo).asInt());
            }
            assertEquals(0, dia.path("minutosNoturnosReais").asInt());
            assertEquals(1, linha.path("totais").path("presencasParciais").asInt());
            assertEquals(0, linha.path("totais").path("faltas").asInt());
        }
        assertEquals(porId.keySet(), vistos);
    }

    private void conferirPagina(JsonNode pagina, int numero, int linhas, boolean primeira, boolean ultima) {
        assertEquals(QUANTIDADE, pagina.path("totalColaboradores").asLong());
        assertEquals(2, pagina.path("totalPaginas").asInt()); assertEquals(25, pagina.path("tamanho").asInt());
        assertEquals(numero, pagina.path("pagina").asInt()); assertEquals(linhas, pagina.path("registros").size());
        assertEquals(primeira, pagina.path("primeira").asBoolean()); assertEquals(ultima, pagina.path("ultima").asBoolean());
    }

    private byte[] baixar(Sessao sessao, String filtro, String formato, String nome) throws Exception {
        HttpResponse<byte[]> r = sessao.enviar("GET", API + "/exportacao" + filtro + "&formato=" + formato, null, 200);
        byte[] bytes = r.body();
        gravarBytes(nome, bytes);
        Map<String, Object> metadados = mapa("sha256", hash(bytes), "bytes", bytes.length,
                "contentType", r.headers().firstValue("Content-Type").orElse(""),
                "contentDisposition", r.headers().firstValue("Content-Disposition").orElse(""),
                "cacheControl", r.headers().firstValue("Cache-Control").orElse(""),
                "nosniff", r.headers().firstValue("X-Content-Type-Options").orElse(""));
        arquivos.put(nome, metadados); gravar("arquivos.json", arquivos);
        assertEquals(formato.equals("PDF") ? "application/pdf" : MIME_XLSX, metadados.get("contentType"));
        assertTrue(metadados.get("contentDisposition").toString().contains("attachment"));
        assertTrue(metadados.get("contentDisposition").toString().contains("Relatorio_Frequencia_"));
        assertTrue(metadados.get("contentDisposition").toString().contains(formato.equals("PDF") ? ".pdf" : ".xlsx"));
        assertEquals("no-store", metadados.get("cacheControl")); assertEquals("nosniff", metadados.get("nosniff"));
        assertTrue(bytes.length > 1000, "Arquivo deve conter estrutura e conteúdo significativo");
        byte[] assinatura = formato.equals("PDF") ? "%PDF-".getBytes(StandardCharsets.US_ASCII) : new byte[]{'P', 'K', 3, 4};
        assertArrayEquals(assinatura, Arrays.copyOf(bytes, assinatura.length));
        assertEquals(hash(bytes), hashArquivo(evidencias.resolve(nome)));
        if (r.headers().firstValueAsLong("Content-Length").isPresent())
            assertEquals(bytes.length, r.headers().firstValueAsLong("Content-Length").getAsLong());
        return bytes;
    }

    private void conferirPdf(byte[] bytes, JsonNode consulta, List<PessoaEsperada> pessoas,
                             List<PessoaEsperada> alheias, int paginas, String fase) throws Exception {
        PdfReader leitor = new PdfReader(bytes);
        List<String> textos = new ArrayList<>();
        try {
            assertFalse(leitor.isEncrypted());
            PdfTextExtractor extrator = new PdfTextExtractor(leitor);
            for (int p = 1; p <= leitor.getNumberOfPages(); p++) textos.add(extrator.getTextFromPage(p));
            gravar(fase + "-texto-e-contagem.json", mapa("paginas", leitor.getNumberOfPages(), "textos", textos,
                    "nomesPorPagina", textos.stream().map(t -> pessoas.stream().filter(p -> compacto(t).contains(compacto(p.nome()))).count()).toList()));
            assertEquals(paginas, leitor.getNumberOfPages(), "ARQ-002 conta as páginas reais, sem alterar layout ou massa para obter verde");
            assertEquals("SGT-GTRAT", leitor.getInfo().get("Author"));
        } finally { leitor.close(); }
        String texto = compacto(String.join("\n", textos));
        assertTrue(texto.contains(DATA_BR.format(LocalDate.parse(consulta.path("inicio").asText()))));
        List<JsonNode> linhas = new ArrayList<>(); consulta.path("registros").forEach(linhas::add);
        Set<String> encontrados = new HashSet<>();
        int posicaoAnterior = -1;
        for (int i = 0; i < linhas.size(); i++) {
            JsonNode linha = linhas.get(i);
            String nome = compacto(linha.path("nome").asText());
            int posicao = texto.indexOf(nome);
            assertTrue(posicao >= 0 && encontrados.add(nome));
            assertEquals(posicao, texto.lastIndexOf(nome), "Um único registro nominal em todo o PDF");
            assertTrue(posicao > posicaoAnterior, "Ordem do PDF corresponde ao mesmo consolidado");
            posicaoAnterior = posicao;
            int fim;
            if (i + 1 < linhas.size()) {
                fim = texto.indexOf(compacto(linhas.get(i + 1).path("nome").asText()), posicao + nome.length());
            } else {
                // O extrator pode devolver o resumo antes da tabela numa página.
                // A legenda delimita a última linha sem incluir valores do rodapé.
                fim = List.of("Totalexportado:", "PPresençaIntegral").stream()
                        .mapToInt(marcador -> texto.indexOf(marcador, posicao + nome.length()))
                        .filter(indice -> indice > posicao).min().orElse(-1);
            }
            assertTrue(fim > posicao, "Limite textual da linha identificada");
            String registro = texto.substring(posicao, fim);
            for (String campo : List.of("cargo", "lotacao", "turno"))
                assertTrue(registro.contains(compacto(linha.path(campo).asText())), "Dimensão do mesmo registro: " + campo);
            int minutos = linha.path("totais").path("minutosPagaveis").asInt();
            String valores = "PP010000" + horas(minutos) + "00:00" + horas(minutos)
                    + String.format(Locale.ROOT, "%.2f", minutos / 60.0);
            assertTrue(registro.contains(valores), "Status, seis contadores e horas da pessoa devem permanecer associados");
        }
        assertEquals(pessoas.size(), encontrados.size());
        assertTrue(texto.contains("Totalexportado:" + pessoas.size() + "alocação(ões)."));
        for (PessoaEsperada alheia : alheias) assertFalse(texto.contains(compacto(alheia.nome())), "PDF não inclui outro escopo");
        assertTrue(texto.contains(compacto(linhas.get(0).path("nome").asText())), "Primeiro registro presente");
        assertTrue(texto.contains(compacto(linhas.get(linhas.size() - 1).path("nome").asText())), "Último registro presente");
    }

    private void conferirXlsx(byte[] bytes, JsonNode consulta, List<PessoaEsperada> pessoas,
                              List<PessoaEsperada> alheias, LocalDate data, String fase) throws Exception {
        List<List<String>> linhas = linhasXlsx(bytes);
        gravar(fase + "-celulas.json", linhas);
        List<String> cabecalho = List.of("COLABORADOR", "CARGO", "LOTAÇÃO", "TURNO", data.format(DateTimeFormatter.ofPattern("dd/MM")),
                "P", "PP", "F", "AT", "D", "RT", "H.REAL", "H.NOT", "H.PAG", "H.PAG DEC");
        int indice = linhas.indexOf(cabecalho);
        assertTrue(indice >= 0); assertEquals(1, linhas.stream().filter(cabecalho::equals).count());
        assertTrue(linhas.get(0).get(0).contains(DATA_BR.format(data) + " a " + DATA_BR.format(data)));
        List<List<String>> registros = new ArrayList<>();
        int cursor = indice + 1;
        while (cursor < linhas.size() && !linhas.get(cursor).get(0).startsWith("TOTAL GERAL")) registros.add(linhas.get(cursor++));
        assertEquals(pessoas.size(), registros.size(), "Exportação inclui todo o recorte, além das primeiras25 pessoas");
        assertTrue(cursor < linhas.size(), "Total geral após a última linha de dados");
        Set<String> nomes = new HashSet<>();
        for (int i = 0; i < registros.size(); i++) {
            List<String> registro = registros.get(i); JsonNode fonte = consulta.path("registros").get(i);
            assertEquals(cabecalho.size(), registro.size());
            assertTrue(nomes.add(registro.get(0))); assertEquals(fonte.path("nome").asText(), registro.get(0));
            assertEquals(fonte.path("cargo").asText(), registro.get(1)); assertEquals(fonte.path("lotacao").asText(), registro.get(2));
            assertEquals(fonte.path("turno").asText(), registro.get(3)); assertEquals("PP", registro.get(4));
            assertEquals(List.of("0", "1", "0", "0", "0", "0"), registro.subList(5, 11));
            int minutos = fonte.path("totais").path("minutosPagaveis").asInt();
            assertEquals(horas(minutos), registro.get(11)); assertEquals("00:00", registro.get(12));
            assertEquals(horas(minutos), registro.get(13));
            assertEquals(minutos / 60.0, Double.parseDouble(registro.get(14)), 0.000000001);
        }
        assertEquals(pessoas.stream().map(PessoaEsperada::nome).collect(java.util.stream.Collectors.toSet()), nomes);
        List<String> total = linhas.get(cursor);
        assertEquals("TOTAL GERAL - " + pessoas.size() + " alocação(ões)", total.get(0));
        assertEquals(String.valueOf(pessoas.size()), total.get(6));
        assertEquals(horas(somaMinutos(pessoas)), total.get(11)); assertEquals("00:00", total.get(12));
        assertEquals(horas(somaMinutos(pessoas)), total.get(13));
        assertEquals(somaMinutos(pessoas) / 60.0, Double.parseDouble(total.get(14)), 0.000000001);
        String todos = compacto(linhas.toString());
        for (PessoaEsperada alheia : alheias) assertFalse(todos.contains(compacto(alheia.nome())));
    }

    private List<List<String>> linhasXlsx(byte[] bytes) throws Exception {
        return PlanilhaXlsxAssercoes.linhasXlsx(bytes);
    }

    private Process iniciar(Path jar, String hashJar) throws Exception {
        assertEquals(hashJar, hashArquivo(jar));
        int porta;
        try (ServerSocket reserva = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) { porta = reserva.getLocalPort(); }
        origem = URI.create("http://127.0.0.1:" + porta);
        Path log = Files.createFile(evidencias.resolve("jar.log"), permissaoArquivo());
        List<String> comando = List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx192m", "-XX:MaxMetaspaceSize=192m", "-XX:ActiveProcessorCount=2", "-Duser.timezone=America/Sao_Paulo",
                "-jar", jar.toString(), "--env-file=./src/test/resources/auditoria-endpoints.env", "--server.address=127.0.0.1",
                "--server.port=" + porta, "--server.ssl.enabled=false", "--spring.main.banner-mode=off",
                "--spring.jpa.hibernate.ddl-auto=update", "--spring.sql.init.mode=never", "--spring.jpa.open-in-view=false",
                "--spring.jpa.show-sql=false", "--spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
                "--spring.datasource.hikari.maximum-pool-size=2", "--spring.datasource.hikari.minimum-idle=1",
                "--logging.level.root=WARN", "--logging.level.com.example.siggtrat=WARN", "--logging.level.org.hibernate.SQL=OFF",
                "--logging.level.org.hibernate.orm.jdbc.bind=OFF", "--app.email.enabled=false", "--app.email.gmail.enabled=false",
                "--spring.cache.type=redis", "--app.seed.unidades-operacionais-enabled=false", "--app.seed.jornadas-operacionais-enabled=false",
                "--app.seed.usuarios-globais-enabled=false", "--app.seed.gestores-se-gestao-local-enabled=false",
                "--file.upload-dir=" + evidencias.resolve("uploads"), "--audit.fallback-log=" + evidencias.resolve("audit-fallback.jsonl"),
                "--app.relatorios.temp.diretorio=" + evidencias.resolve("relatorios"));
        ProcessBuilder builder = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile());
        Map<String, String> ambiente = builder.environment(); ambiente.clear(); ambiente.put("TZ", "America/Sao_Paulo");
        var banco = ConfiguracaoPostgresIntegracao.carregar();
        ambiente.put("DB_URL", banco.url()); ambiente.put("DB_USER", banco.usuario()); ambiente.put("DB_PASS", banco.senha());
        ambiente.put("SPRING_DATASOURCE_URL", banco.url()); ambiente.put("SPRING_DATASOURCE_USERNAME", banco.usuario());
        ambiente.put("SPRING_DATASOURCE_PASSWORD", banco.senha());
        for (String chave : List.of("REDIS_HOST", "REDIS_PORT", "REDIS_PASSWORD", "REDIS_DATABASE")) {
            String valor = System.getenv(chave); assertNotNull(valor, "Infraestrutura isolada explícita: " + chave);
            assertFalse(valor.isBlank()); ambiente.put(chave, valor);
        }
        ambiente.put("REDIS_HTTP_GET_ENABLED", "true");
        Process filho = builder.start();
        try {
            gravar("processo.json", mapa("pid", filho.pid(), "origem", origem.toString(), "jarSha256", hashJar,
                    "heapMiB", 192, "metaspaceMiB", 192, "cpu", 2, "poolMax", 2));
            try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
                long limite = System.nanoTime() + Duration.ofSeconds(55).toNanos();
                do {
                    assertTrue(filho.isAlive(), "JAR encerrou antes da prontidão; consultar somente o log privado");
                    try {
                        var resposta = cliente.send(HttpRequest.newBuilder(origem.resolve("/api/autenticacao/csrf"))
                                .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                        if (resposta.statusCode() == 200) return filho;
                    } catch (java.io.IOException iniciando) { /* Socket ainda indisponível durante o boot real. */ }
                    Thread.sleep(100);
                } while (System.nanoTime() < limite);
                fail("Tomcat não ficou pronto em55s; diagnóstico privado preservado");
                return filho;
            }
        } catch (Exception | AssertionError falha) { encerrar(filho); throw falha; }
    }

    private Sessao login(UsuarioAplicacao ator) throws Exception {
        Sessao sessao = new Sessao();
        try {
            sessao.csrf();
            JsonNode autenticado = sessao.json("/api/autenticacao/autenticar", mapa("matricula", ator.getMatricula(), "password", SENHA), 200);
            assertEquals(ator.getUsername(), autenticado.path("username").asText());
            assertEquals(ator.getPerfil().name(), autenticado.path("perfil").asText());
            assertFalse(autenticado.has("token")); assertFalse(autenticado.has("password"));
            for (String cookie : List.of("autenticacao", "id_dispositivo"))
                assertTrue(sessao.cookies.getCookieStore().getCookies().stream().anyMatch(c -> c.getName().equals(cookie)));
            sessao.csrf(); return sessao;
        } catch (Exception | AssertionError falha) { sessao.close(); throw falha; }
    }

    private final class Sessao implements AutoCloseable {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        private final HttpClient cliente = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        private String tokenCsrf;
        private void csrf() throws Exception {
            JsonNode token = get("/api/autenticacao/csrf", 200);
            assertEquals("X-XSRF-TOKEN", token.path("headerName").asText());
            tokenCsrf = token.path("token").asText(); assertFalse(tokenCsrf.isBlank());
        }
        private JsonNode get(String caminho, int esperado) throws Exception { return dados(enviar("GET", caminho, null, esperado)); }
        private JsonNode json(String caminho, Object corpo, int esperado) throws Exception {
            return dados(enviar("POST", caminho, RelatorioJarMultipaginaIT.this.json(corpo).getBytes(StandardCharsets.UTF_8), esperado));
        }
        private HttpResponse<byte[]> enviar(String metodo, String caminho, byte[] corpo, int esperado) throws Exception {
            long inicio = System.nanoTime();
            var request = HttpRequest.newBuilder(origem.resolve(caminho)).timeout(Duration.ofSeconds(25))
                    .header("Accept", "*/*").header("Cache-Control", "no-cache").header("User-Agent", "SGT-RelatorioJarMultipaginaIT");
            if (!metodo.equals("GET")) {
                assertNotNull(tokenCsrf); request.header("X-XSRF-TOKEN", tokenCsrf).header("X-User-Password", SENHA);
                request.header("Content-Type", "application/json");
            }
            var resposta = cliente.send(request.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(corpo)).build(), HttpResponse.BodyHandlers.ofByteArray());
            append("http-status.jsonl", mapa("instante", Instant.now().toString(), "metodo", metodo, "caminho", caminho,
                    "esperado", esperado, "recebido", resposta.statusCode(), "bytes", resposta.body().length,
                    "duracaoMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio)));
            assertEquals(esperado, resposta.statusCode(), metodo + " " + caminho);
            return resposta;
        }
        private JsonNode dados(HttpResponse<byte[]> resposta) throws Exception {
            JsonNode valor = mapper.readTree(resposta.body());
            return valor.hasNonNull("data") ? valor.get("data") : valor;
        }
        @Override public void close() { cliente.close(); }
    }

    private void conferirClasses(Path jar) throws Exception {
        Path raiz = Path.of("target/classes"); Map<String, String> conferidas = new TreeMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile()); var arquivos = Files.walk(raiz)) {
            for (Path classe : arquivos.filter(p -> p.toString().endsWith(".class")).toList()) {
                String relativa = raiz.relativize(classe).toString().replace('\\', '/');
                var entrada = zip.getEntry("BOOT-INF/classes/" + relativa); assertNotNull(entrada, relativa);
                try (InputStream bytes = zip.getInputStream(entrada)) {
                    byte[] corrente = Files.readAllBytes(classe);
                    assertArrayEquals(corrente, bytes.readAllBytes(), "Reempacotar o main corrente antes do IT: " + relativa);
                    conferidas.put(relativa, hash(corrente));
                }
            }
        }
        assertFalse(conferidas.isEmpty()); gravar("classes-producao-conferidas.json", conferidas);
    }

    private void encerrar(Process filho) throws InterruptedException {
        if (filho == null || !filho.isAlive()) return;
        boolean interrompido = Thread.interrupted();
        try {
            filho.destroy();
            try {
                if (!filho.waitFor(10, TimeUnit.SECONDS)) {
                    filho.destroyForcibly(); assertTrue(filho.waitFor(8, TimeUnit.SECONDS), "Encerrar somente o filho conhecido");
                }
            } catch (InterruptedException falha) {
                interrompido = true; filho.destroyForcibly();
                assertTrue(filho.waitFor(8, TimeUnit.SECONDS), "Filho conhecido não encerrou após interrupção");
            }
            assertFalse(filho.isAlive());
        } finally { if (interrompido) Thread.currentThread().interrupt(); }
    }

    private void append(String nome, Object valor) throws Exception {
        byte[] bytes = (json(valor) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(evidencias.resolve(nome), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) canal.write(buffer); canal.force(true);
        }
    }
    private void gravar(String nome, Object valor) throws Exception {
        gravarBytes(nome, (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(valor) + "\n").getBytes(StandardCharsets.UTF_8));
    }
    private void gravarBytes(String nome, byte[] bytes) throws Exception {
        Path temporario = Files.createTempFile(evidencias, nome + "-", ".tmp", permissaoArquivo());
        try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) canal.write(buffer); canal.force(true);
        }
        Files.move(temporario, evidencias.resolve(nome), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        try (FileChannel diretorio = FileChannel.open(evidencias, StandardOpenOption.READ)) { diretorio.force(true); }
    }
    private static Path diretorioPrivado(String prefixo) {
        try {
            Path raiz = Path.of("target/relatorio-jar-multipagina").toAbsolutePath(); Files.createDirectories(raiz);
            return Files.createTempDirectory(raiz, prefixo, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        } catch (java.io.IOException falha) { throw new IllegalStateException("Armazenamento exclusivo de teste indisponível", falha); }
    }
    private static java.nio.file.attribute.FileAttribute<Set<java.nio.file.attribute.PosixFilePermission>> permissaoArquivo() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
    }
    private static String nome(int n) { return String.format(Locale.ROOT, "Pessoa Relatorio %02d", n); }
    private static String horas(int minutos) { return String.format(Locale.ROOT, "%02d:%02d", minutos / 60, minutos % 60); }
    private static int somaMinutos(List<PessoaEsperada> pessoas) { return pessoas.stream().mapToInt(PessoaEsperada::minutos).sum(); }
    private static String compacto(String texto) { return texto.replaceAll("\\s+", ""); }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String hashArquivo(Path arquivo) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            byte[] buffer = new byte[65536];
            for (int n; (n = entrada.read(buffer)) >= 0;) if (n > 0) hash.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(hash.digest());
    }
}
