package com.example.siggtrat.contrato;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executar na fase failsafe de Maven verify, depois de package/repackage.
 * Compara o JAR real aos bytes compilados, sem carregar classes do produto,
 * sem Spring, banco, mocks ou acesso ao projeto React. Build ausente não é skip.
 * O gate externo confere separadamente dist React e a cópia estática do backend.
 */
class ArtefatoEmpacotadoIT {
    private static final String PREFIXO = "BOOT-INF/classes/";
    private static final String PACOTE = "com/example/siggtrat/";
    // Nomes efetivos dos inventários RemocaoGovernancaTerceirosArquiteturaTest
    // e RemocaoFinanceiraHttpIT. Não se proíbe genericamente o texto GOV/GOV-003.
    private static final List<String> RETIRADAS = List.of(
            "controlador/GovernancaTerceirosControlador", "servico/GovernancaTerceirosServico",
            "servico/RiscoContinuoServico", "entidade/ProcessoTerceiro", "entidade/AvaliacaoRiscoFornecedor",
            "repositorio/ProcessoTerceiroRepositorio", "repositorio/AvaliacaoRiscoFornecedorRepositorio",
            "dto/GovernancaResumoDTO", "dto/FornecedorGovernancaEntradaDTO", "dto/FornecedorGovernancaDTO",
            "dto/ContratoFornecedorGovernancaDTO", "dto/ProcessoTerceiroEntradaDTO",
            "dto/ProcessoTerceiroGovernancaDTO", "dto/StatusGovernancaDTO", "dto/RiscoFornecedorEntradaDTO",
            "dto/RiscoFornecedorGovernancaDTO", "entidade/TabelaTarifaria", "entidade/FolhaHoras",
            "entidade/Fatura", "entidade/PreviaVigenciaTarifa", "entidade/ReciboVigenciaTarifa");
    private static final List<String> PRESERVADAS = List.of(
            "entidade/Fornecedor", "repositorio/FornecedorRepositorio", "servico/FornecedorServico",
            "entidade/Contrato", "servico/ContratoServico", "servico/AditamentoContratualServico",
            "servico/MotorTemporalContratoServico", "servico/ProvaHistoricaContratoServico",
            "entidade/VersaoContratoNegocial", "entidade/Empregado", "entidade/Frequencia",
            "controlador/RegistroAuditoriaControlador", "servico/RegistroAuditoriaServico");

    @Test
    void jarExecutavelContemExatamenteOsBytesDeTodasAsClassesERecursosCompilados() throws Exception {
        Projeto projeto = projeto();
        Map<String, String> compilados = inventariarDiretorio(projeto.classes());
        assertFalse(compilados.isEmpty(), "target/classes vazio não comprova correspondência");
        assertTrue(compilados.keySet().stream().anyMatch(n -> n.endsWith(".class")),
                "O inventário deve conter classes reais, além de recursos");
        String hashAntes = hash(projeto.jar());
        Entradas entradas = inventariarJar(projeto.jar());
        Set<String> faltantes = new TreeSet<>(compilados.keySet());
        faltantes.removeAll(entradas.conteudo().keySet());
        Set<String> extras = new TreeSet<>(entradas.conteudo().keySet());
        extras.removeAll(compilados.keySet());
        List<Map<String, String>> divergentes = new ArrayList<>();
        compilados.forEach((nome, sha) -> {
            String noJar = entradas.conteudo().get(nome);
            if (noJar != null && !sha.equals(noJar)) {
                divergentes.add(Map.of("arquivo", nome, "sha256Compilado", sha, "sha256Jar", noJar));
            }
        });
        Map<String, String> compiladosDepois = inventariarDiretorio(projeto.classes());
        String hashDepois = hash(projeto.jar());
        boolean estavel = compilados.equals(compiladosDepois) && hashAntes.equals(hashDepois);
        Map<String, Object> prova = new LinkedHashMap<>();
        prova.put("registradoEm", Instant.now().toString());
        prova.put("jar", projeto.jar().getFileName().toString());
        prova.put("jar_sha256", hashAntes);
        prova.put("versaoEsperada", projeto.version());
        prova.put("arquivosCompilados", compilados.size());
        prova.put("arquivosEmpacotados", entradas.conteudo().size());
        prova.put("faltantes", faltantes);
        prova.put("extras", extras);
        prova.put("divergentes", divergentes);
        prova.put("entradasDuplicadas", entradas.duplicadas());
        prova.put("leituraEstavel", estavel);
        prova.put("correspondenciaIntegral", estavel && faltantes.isEmpty() && extras.isEmpty()
                && divergentes.isEmpty() && entradas.duplicadas().isEmpty());
        prova.put("inventarioCompilado", compilados);
        prova.put("inventarioEmpacotado", entradas.conteudo());
        Path saida = projeto.build().resolve("relatorios/artefato-empacotado-correspondencia.json");
        Files.createDirectories(saida.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(saida.toFile(), prova);

        assertAll("JAR entregue precisa conter os mesmos bytes usados pelos testes",
                () -> assertTrue(estavel, "Inputs mudaram durante a comparação"),
                () -> assertTrue(entradas.duplicadas().isEmpty(), "Entradas ZIP duplicadas: " + entradas.duplicadas()),
                () -> assertTrue(faltantes.isEmpty(), "Compilados ausentes do JAR: " + faltantes),
                () -> assertTrue(extras.isEmpty(), "Conteúdo de aplicação extra no JAR: " + extras),
                () -> assertTrue(divergentes.isEmpty(), "Bytes divergentes entre classes/resources e JAR: " + divergentes));
    }

    @Test
    void manifestoECoordenadasDoJarCorrespondemAoPomDaCandidata() throws Exception {
        Projeto projeto = projeto();
        try (ZipFile jar = new ZipFile(projeto.jar().toFile())) {
            var entradaManifesto = jar.getEntry("META-INF/MANIFEST.MF");
            assertNotNull(entradaManifesto, "JAR sem manifesto não é o executável esperado");
            Manifest manifest;
            try (InputStream stream = jar.getInputStream(entradaManifesto)) {
                manifest = new Manifest(stream);
            }
            var attrs = manifest.getMainAttributes();
            assertEquals(projeto.version(), attrs.getValue("Implementation-Version"));
            assertEquals("org.springframework.boot.loader.launch.JarLauncher", attrs.getValue("Main-Class"));
            assertEquals("com.example.siggtrat.AplicacaoBackendSigGtrat", attrs.getValue("Start-Class"));
            String pomPath = "META-INF/maven/" + projeto.groupId() + "/" + projeto.artifactId() + "/pom.properties";
            var entradaPom = jar.getEntry(pomPath);
            assertNotNull(entradaPom, "Coordenadas Maven ausentes do JAR: " + pomPath);
            Properties props = new Properties();
            try (InputStream stream = jar.getInputStream(entradaPom)) {
                props.load(stream);
            }
            assertAll("Identidade do artefato executável",
                    () -> assertEquals(projeto.groupId(), props.getProperty("groupId")),
                    () -> assertEquals(projeto.artifactId(), props.getProperty("artifactId")),
                    () -> assertEquals(projeto.version(), props.getProperty("version")));
        }
    }

    @Test
    void jarRetiraClassesExclusivasEConservaNucleoContratualEAuditoria() throws Exception {
        var nomes = inventariarJar(projeto().jar()).conteudo().keySet();
        List<String> residuos = nomes.stream().filter(nome -> RETIRADAS.stream().anyMatch(tipo ->
                nome.equals(PACOTE + tipo + ".class") || nome.startsWith(PACOTE + tipo + "$"))).toList();
        List<String> preservadasAusentes = PRESERVADAS.stream().map(tipo -> PACOTE + tipo + ".class")
                .filter(nome -> !nomes.contains(nome)).toList();
        assertAll("Remoção e preservação também precisam valer no arquivo entregue",
                () -> assertTrue(residuos.isEmpty(), "Classes exclusivas no JAR: " + residuos),
                () -> assertTrue(preservadasAusentes.isEmpty(), "Classes compartilhadas ausentes: " + preservadasAusentes));
    }

    private Projeto projeto() throws Exception {
        Path base = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Element project;
        try (InputStream stream = Files.newInputStream(base.resolve("pom.xml"))) {
            project = factory.newDocumentBuilder().parse(stream).getDocumentElement();
        }
        String group = filho(project, "groupId"), artifact = filho(project, "artifactId"), version = filho(project, "version");
        assertNotNull(group, "groupId explícito da candidata ausente");
        assertNotNull(artifact, "artifactId explícito da candidata ausente");
        assertNotNull(version, "versão explícita da candidata ausente");
        Element configuracaoBuild = elementoFilho(project, "build");
        String nomeFinal = configuracaoBuild == null ? null : filho(configuracaoBuild, "finalName");
        if (nomeFinal == null || nomeFinal.isBlank()) nomeFinal = artifact + "-" + version;
        assertFalse(nomeFinal.contains("${"), "finalName precisa estar resolvido para identificar o JAR real");
        Path build = base.resolve(System.getProperty("sgt.build.directory", "target")).normalize();
        Path jar = build.resolve(nomeFinal + ".jar");
        assertTrue(Files.isRegularFile(jar), "Execute Maven verify com package/repackage; JAR ausente: " + jar);
        assertTrue(Files.isDirectory(build.resolve("classes")), "Diretório classes ausente: " + build);
        return new Projeto(group, artifact, version, build, build.resolve("classes"), jar);
    }

    private String filho(Element parent, String name) {
        Element element = elementoFilho(parent, name);
        return element == null ? null : element.getTextContent().trim();
    }

    private Element elementoFilho(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && name.equals(element.getLocalName())) return element;
        }
        return null;
    }

    private Map<String, String> inventariarDiretorio(Path dir) throws Exception {
        Map<String, String> files = new TreeMap<>();
        try (var stream = Files.walk(dir)) {
            for (Path path : stream.filter(Files::isRegularFile).sorted().toList()) {
                files.put(dir.relativize(path).toString().replace('\\', '/'), hash(path));
            }
        }
        return files;
    }

    private Entradas inventariarJar(Path path) throws Exception {
        Map<String, String> files = new TreeMap<>();
        List<String> duplicadas = new ArrayList<>();
        Set<String> vistos = new TreeSet<>();
        try (ZipFile jar = new ZipFile(path.toFile())) {
            for (var entry : Collections.list(jar.entries())) {
                if (!vistos.add(entry.getName())) duplicadas.add(entry.getName());
                if (entry.isDirectory() || !entry.getName().startsWith(PREFIXO)) continue;
                try (InputStream stream = jar.getInputStream(entry)) {
                    files.put(entry.getName().substring(PREFIXO.length()), hash(stream));
                }
            }
        }
        return new Entradas(files, duplicadas);
    }

    private String hash(Path path) throws Exception {
        try (InputStream stream = Files.newInputStream(path)) {
            return hash(stream);
        }
    }

    private String hash(InputStream stream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        for (int count; (count = stream.read(buffer)) >= 0;) if (count > 0) digest.update(buffer, 0, count);
        return HexFormat.of().formatHex(digest.digest());
    }

    private record Projeto(String groupId, String artifactId, String version, Path build, Path classes, Path jar) { }
    private record Entradas(Map<String, String> conteudo, List<String> duplicadas) { }
}
