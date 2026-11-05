package com.example.siggtrat.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Gera um PDF da execução JUnit ao final do plano de testes. */
public class JunitPdfReportListener implements TestExecutionListener {

    private final Map<String, Instant> inicios = new ConcurrentHashMap<>();
    private final List<Resultado> resultados = java.util.Collections.synchronizedList(new ArrayList<>());

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        inicios.clear();
        resultados.clear();
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            inicios.put(testIdentifier.getUniqueId(), Instant.now());
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String reason) {
        if (!id.isTest()) return;
        resultados.add(new Resultado(
                origem(id),
                id.getDisplayName(),
                "SKIPPED",
                0,
                reason == null || reason.isBlank() ? "Teste ignorado." : reason
        ));
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult resultado) {
        if (!id.isTest()) return;
        Instant inicio = inicios.remove(id.getUniqueId());
        long duracaoMs = inicio == null ? 0 : Duration.between(inicio, Instant.now()).toMillis();
        String detalhe = resultado.getThrowable()
                .map(this::detalhar)
                .orElse(resultado.getStatus().name());
        resultados.add(new Resultado(
                origem(id),
                id.getDisplayName(),
                resultado.getStatus().name(),
                duracaoMs,
                detalhe
        ));
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        gerarPdf();
    }

    private String origem(TestIdentifier id) {
        return id.getSource()
                .map(Object::toString)
                .orElseGet(() -> id.getParentId().orElse("suite"));
    }

    private void gerarPdf() {
        File diretorio = new File("target/relatorios");
        if (!diretorio.exists() && !diretorio.mkdirs()) {
            System.err.println("Não foi possível criar target/relatorios.");
            return;
        }
        List<Resultado> copia;
        synchronized (resultados) {
            copia = new ArrayList<>(resultados);
        }
        copia.sort(Comparator.comparing(Resultado::origem).thenComparing(Resultado::nome));

        boolean execucaoDeCarga = !copia.isEmpty() && copia.stream().allMatch(r ->
                r.origem().contains("com.example.siggtrat.load") || r.nome().toLowerCase().contains("carga")
        );
        File arquivo = new File(diretorio, execucaoDeCarga
                ? "relatorio-testes-carga-junit.pdf"
                : "relatorio-testes-java.pdf");

        long aprovados = copia.stream().filter(r -> "SUCCESSFUL".equals(r.status())).count();
        long falhos = copia.stream().filter(r -> "FAILED".equals(r.status())).count();
        long abortados = copia.stream().filter(r -> "ABORTED".equals(r.status())).count();
        long ignorados = copia.stream().filter(r -> "SKIPPED".equals(r.status())).count();
        long duracao = copia.stream().mapToLong(Resultado::duracaoMs).sum();

        Document documento = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try (FileOutputStream out = new FileOutputStream(arquivo)) {
            PdfWriter.getInstance(documento, out);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 68, 121));
            Paragraph cabecalho = new Paragraph(execucaoDeCarga
                    ? "RELATÓRIO JUNIT - TESTE DE CARGA HTTP"
                    : "RELATÓRIO DE TESTES - JAVA", titulo);
            cabecalho.setAlignment(Element.ALIGN_CENTER);
            cabecalho.setSpacingAfter(12);
            documento.add(cabecalho);

            PdfPTable resumo = new PdfPTable(2);
            resumo.setWidthPercentage(100);
            adicionarResumo(resumo, "Gerado em", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            adicionarResumo(resumo, "Comando", execucaoDeCarga
                    ? "mvn -Dtest=com.example.siggtrat.load.HttpLoad test"
                    : "mvn test");
            adicionarResumo(resumo, "Java", System.getProperty("java.version"));
            adicionarResumo(resumo, "Total", String.valueOf(copia.size()));
            adicionarResumo(resumo, "Aprovados", String.valueOf(aprovados));
            adicionarResumo(resumo, "Falhos", String.valueOf(falhos));
            adicionarResumo(resumo, "Abortados", String.valueOf(abortados));
            adicionarResumo(resumo, "Ignorados", String.valueOf(ignorados));
            adicionarResumo(resumo, "Duração acumulada", duracao + " ms");
            documento.add(resumo);

            adicionarMatrizEngenharia(documento, copia);
            adicionarResumoPorModulo(documento, copia);

            String textoStatus;
            Color corStatus;
            if (copia.isEmpty()) {
                textoStatus = "NENHUM TESTE EXECUTADO";
                corStatus = new Color(180, 95, 0);
            } else if (falhos == 0) {
                textoStatus = "SUÍTE APROVADA";
                corStatus = new Color(0, 120, 65);
            } else {
                textoStatus = "SUÍTE REPROVADA";
                corStatus = Color.RED;
            }
            Paragraph status = new Paragraph(textoStatus,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, corStatus));
            status.setAlignment(Element.ALIGN_CENTER);
            status.setSpacingBefore(12);
            status.setSpacingAfter(12);
            documento.add(status);

            PdfPTable tabela = new PdfPTable(new float[]{0.55f, 2.5f, 3.3f, 1.05f, 0.85f, 4.4f});
            tabela.setWidthPercentage(100);
            tabela.setSplitLate(false);
            for (String coluna : List.of("#", "Origem", "Cenário", "Status", "Tempo", "Detalhes")) {
                PdfPCell c = new PdfPCell(new Phrase(coluna,
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                c.setBackgroundColor(new Color(0, 68, 121));
                c.setPadding(5);
                tabela.addCell(c);
            }

            int indice = 1;
            for (Resultado r : copia) {
                tabela.addCell(celula(String.valueOf(indice++), false));
                tabela.addCell(celula(limitar(r.origem(), 220), false));
                tabela.addCell(celula(r.nome(), false));
                PdfPCell situacao = celula(r.status(), true);
                situacao.setBackgroundColor(corStatus(r.status()));
                tabela.addCell(situacao);
                tabela.addCell(celula(r.duracaoMs() + " ms", false));
                tabela.addCell(celula(limitar(r.detalhe(), 2000), false));
            }
            documento.add(tabela);
            documento.close();
            System.out.println("Relatório PDF Java gerado em: " + arquivo.getPath());
        } catch (Exception ex) {
            System.err.println("Falha ao gerar relatório PDF dos testes Java: " + ex.getMessage());
            if (documento.isOpen()) documento.close();
        }
    }

    private void adicionarMatrizEngenharia(Document documento, List<Resultado> copia) throws Exception {
        Paragraph tituloSecao = new Paragraph(
                "MATRIZ DE ENGENHARIA DE TESTES",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 68, 121))
        );
        tituloSecao.setSpacingBefore(10);
        tituloSecao.setSpacingAfter(5);
        documento.add(tituloSecao);

        Paragraph explicacao = new Paragraph(
                "O status abaixo distingue a ferramenta que executou nesta suíte das ferramentas opt-in " +
                        "usadas em integração, mutation testing e performance. Assim o relatório não confunde " +
                        "teste unitário com teste de banco ou teste de carga.",
                FontFactory.getFont(FontFactory.HELVETICA, 8)
        );
        explicacao.setSpacingAfter(6);
        documento.add(explicacao);

        boolean temArquitetura = copia.stream().anyMatch(r ->
                r.origem().contains("architecture") || r.origem().contains("ArchitectureTest") ||
                        r.nome().toLowerCase().contains("arquitetura"));
        boolean temIntegracao = copia.stream().anyMatch(r -> r.origem().contains(".integration."));
        boolean temSpring = copia.stream().anyMatch(r ->
                r.origem().contains("MockMvcTest") || r.origem().contains(".integration."));
        boolean temMockito = copia.stream().anyMatch(r ->
                r.origem().contains("ControllerTest") || r.origem().contains("ServiceTest") ||
                        r.origem().contains("InterceptorTest") || r.origem().contains("RepositorioSqlConsistenciaTest"));

        PdfPTable ferramentas = new PdfPTable(new float[]{1.35f, 3.0f, 1.55f, 3.2f});
        ferramentas.setWidthPercentage(100);
        ferramentas.setSplitLate(false);
        for (String coluna : List.of("Ferramenta", "Finalidade", "Nesta execução", "Evidência / saída")) {
            PdfPCell c = new PdfPCell(new Phrase(coluna,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.6f, Color.WHITE)));
            c.setBackgroundColor(new Color(65, 82, 99));
            c.setPadding(4);
            ferramentas.addCell(c);
        }

        adicionarFerramenta(ferramentas, "JUnit 5", "Motor e assertions dos testes Java",
                copia.isEmpty() ? "SEM TESTES" : "EXECUTADO", "Surefire/Failsafe + relatório PDF");
        adicionarFerramenta(ferramentas, "Mockito", "Mocks, stubs e verificação de interações",
                temMockito ? "EVIDENCIADO" : "DISPONÍVEL", "spring-boot-starter-test / MockitoExtension");
        adicionarFerramenta(ferramentas, "Spring Test / MockMvc", "Contexto Spring, controllers e segurança HTTP",
                temSpring ? "EVIDENCIADO" : "DISPONÍVEL", "Spring Boot Test + spring-security-test");
        adicionarFerramenta(ferramentas, "ArchUnit", "Regras estruturais Controller > Service > Repository",
                temArquitetura ? "EXECUTADO" : "OPT-IN NA SUÍTE", "classes *ArchitectureTest");
        adicionarFerramenta(ferramentas, "Testcontainers", "MySQL 8.0.45 real e descartável para integração",
                temIntegracao ? "EXECUTADO" : "OPT-IN", "mvn verify -Pintegration / -Pdb-load");
        adicionarFerramenta(ferramentas, "JaCoCo", "Cobertura de linhas e branches",
                "ATIVO NO BUILD", "target/site/jacoco/index.html");
        adicionarFerramenta(ferramentas, "PIT", "Mutation testing: mede se o teste detecta defeitos",
                "OPT-IN", "mvn -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage");
        adicionarFerramenta(ferramentas, "Gatling", "Carga HTTP, concorrência, P95 e taxa de falha",
                "MÓDULO ISOLADO", "performance-tests/target/gatling/");

        documento.add(ferramentas);
    }

    private void adicionarResumoPorModulo(Document documento, List<Resultado> copia) throws Exception {
        Paragraph tituloSecao = new Paragraph(
                "COBERTURA DA EXECUÇÃO POR MÓDULO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 68, 121))
        );
        tituloSecao.setSpacingBefore(10);
        tituloSecao.setSpacingAfter(5);
        documento.add(tituloSecao);

        Paragraph explicacao = new Paragraph(
                "Esta tabela mostra a distribuição dos testes efetivamente executados por área do backend. " +
                        "Ela não substitui cobertura de código: linhas e branches continuam sendo medidos pelo JaCoCo.",
                FontFactory.getFont(FontFactory.HELVETICA, 8)
        );
        explicacao.setSpacingAfter(6);
        documento.add(explicacao);

        Map<String, ModuloResumo> modulos = new LinkedHashMap<>();
        copia.stream()
                .sorted(Comparator.comparing(r -> modulo(r.origem())))
                .forEach(r -> {
                    String modulo = modulo(r.origem());
                    ModuloResumo atual = modulos.computeIfAbsent(modulo, k -> new ModuloResumo());
                    atual.total++;
                    atual.duracaoMs += r.duracaoMs();
                    if ("SUCCESSFUL".equals(r.status())) atual.aprovados++;
                    else if ("FAILED".equals(r.status())) atual.falhos++;
                    else atual.outros++;
                });

        PdfPTable tabela = new PdfPTable(new float[]{2.4f, 1.0f, 1.0f, 1.0f, 1.0f, 1.4f});
        tabela.setWidthPercentage(100);
        tabela.setSplitLate(false);
        for (String coluna : List.of("Módulo", "Total", "Aprovados", "Falhos", "Outros", "Duração")) {
            PdfPCell c = new PdfPCell(new Phrase(coluna,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.6f, Color.WHITE)));
            c.setBackgroundColor(new Color(65, 82, 99));
            c.setPadding(4);
            tabela.addCell(c);
        }

        if (modulos.isEmpty()) {
            PdfPCell vazio = celula("Nenhum teste executado.", false);
            vazio.setColspan(6);
            tabela.addCell(vazio);
        } else {
            modulos.forEach((nome, resumo) -> {
                tabela.addCell(celula(nome, true));
                tabela.addCell(celula(String.valueOf(resumo.total), false));
                tabela.addCell(celula(String.valueOf(resumo.aprovados), false));
                tabela.addCell(celula(String.valueOf(resumo.falhos), false));
                tabela.addCell(celula(String.valueOf(resumo.outros), false));
                tabela.addCell(celula(resumo.duracaoMs + " ms", false));
            });
        }
        documento.add(tabela);
    }

    private String modulo(String origem) {
        if (origem == null) return "Geral";
        if (origem.contains(".architecture.")) return "Arquitetura";
        if (origem.contains(".cache.")) return "Cache / Redis";
        if (origem.contains(".config.")) return "Configuração";
        if (origem.contains(".controller.")) return "Controllers / API";
        if (origem.contains(".frequencia.")) return "Frequência";
        if (origem.contains(".integration.")) return "Integração / Banco";
        if (origem.contains(".load.")) return "Carga / Performance";
        if (origem.contains(".model.")) return "Entidades / Model";
        if (origem.contains(".painel.")) return "Painel";
        if (origem.contains(".repository.")) return "Repositories / SQL";
        if (origem.contains(".security.")) return "Segurança";
        if (origem.contains(".service.")) return "Services / Regras de negócio";
        if (origem.contains(".util.")) return "Utilitários";
        if (origem.contains(".report.")) return "Infraestrutura de Relatório";
        return "Geral";
    }

    private static final class ModuloResumo {
        long total;
        long aprovados;
        long falhos;
        long outros;
        long duracaoMs;
    }

    private void adicionarFerramenta(PdfPTable tabela, String ferramenta, String finalidade,
                                      String situacao, String evidencia) {
        tabela.addCell(celula(ferramenta, true));
        tabela.addCell(celula(finalidade, false));
        PdfPCell status = celula(situacao, true);
        if ("EXECUTADO".equals(situacao) || "EVIDENCIADO".equals(situacao) || "ATIVO NO BUILD".equals(situacao)) {
            status.setBackgroundColor(new Color(220, 245, 228));
        } else {
            status.setBackgroundColor(new Color(238, 242, 247));
        }
        tabela.addCell(status);
        tabela.addCell(celula(evidencia, false));
    }

    private Color corStatus(String status) {
        return switch (status) {
            case "SUCCESSFUL" -> new Color(220, 245, 228);
            case "SKIPPED", "ABORTED" -> new Color(255, 244, 204);
            default -> new Color(255, 220, 220);
        };
    }

    private void adicionarResumo(PdfPTable tabela, String chave, String valor) {
        PdfPCell c1 = celula(chave, true);
        c1.setBackgroundColor(new Color(238, 242, 247));
        tabela.addCell(c1);
        tabela.addCell(celula(valor, false));
    }

    private PdfPCell celula(String texto, boolean negrito) {
        Font fonte = FontFactory.getFont(negrito ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 7.4f);
        PdfPCell celula = new PdfPCell(new Phrase(texto == null ? "" : texto, fonte));
        celula.setPadding(4);
        celula.setVerticalAlignment(Element.ALIGN_TOP);
        return celula;
    }

    private String detalhar(Throwable erro) {
        StringBuilder sb = new StringBuilder();
        Throwable atual = erro;
        int nivel = 0;
        while (atual != null && nivel++ < 6) {
            if (!sb.isEmpty()) sb.append(" | causado por: ");
            sb.append(atual.getClass().getSimpleName()).append(": ")
                    .append(atual.getMessage() == null ? "sem mensagem" : atual.getMessage());
            atual = atual.getCause();
        }
        return sb.toString();
    }

    private String limitar(String valor, int limite) {
        if (valor == null) return "";
        return valor.length() <= limite ? valor : valor.substring(0, limite) + "...";
    }

    private record Resultado(String origem, String nome, String status, long duracaoMs, String detalhe) {}
}
