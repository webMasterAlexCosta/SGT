package com.example.siggtrat.contrato;

import com.example.siggtrat.configuracao.InvalidaCacheNegocio;
import com.example.siggtrat.entidade.Fornecedor;
import com.example.siggtrat.seguranca.ExigirSenha;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gate sem contexto Spring, sem DataSource e sem mocks. Examina o artefato
 * compilado e os contratos de entrada do produto. A prova HTTP/persistência
 * permanece nas ITs com aplicação, filtros e MySQL reais.
 */
class RemocaoGovernancaTerceirosArquiteturaTest {
    private static final String PACOTE = "com.example.siggtrat.";
    private static final List<String> CLASSES_EXCLUSIVAS = List.of(
            "controlador.GovernancaTerceirosControlador",
            "servico.GovernancaTerceirosServico", "servico.RiscoContinuoServico",
            "entidade.ProcessoTerceiro", "entidade.AvaliacaoRiscoFornecedor",
            "repositorio.ProcessoTerceiroRepositorio", "repositorio.AvaliacaoRiscoFornecedorRepositorio",
            "dto.GovernancaResumoDTO", "dto.FornecedorGovernancaEntradaDTO",
            "dto.FornecedorGovernancaDTO", "dto.ContratoFornecedorGovernancaDTO",
            "dto.ProcessoTerceiroEntradaDTO", "dto.ProcessoTerceiroGovernancaDTO",
            "dto.StatusGovernancaDTO", "dto.RiscoFornecedorEntradaDTO", "dto.RiscoFornecedorGovernancaDTO");

    @Test
    void artefatoNaoContemClassesExecutaveisDoModuloRetirado() {
        assertAll("Classes exclusivas não podem continuar no artefato",
                CLASSES_EXCLUSIVAS.stream().map(nome -> () ->
                        assertThrows(ClassNotFoundException.class, () -> Class.forName(PACOTE + nome),
                                "Classe exclusiva ainda presente: " + nome)));
    }

    @Test
    void nenhumControladorDeclaraPrefixoAntigoOuAvaliacaoManual() {
        List<Rota> rotas = classesAnotadas("controlador", RestController.class).stream()
                .flatMap(tipo -> rotas(tipo).stream()).toList();
        assertFalse(rotas.isEmpty(), "A descoberta precisa encontrar os controladores reais do produto");
        assertTrue(rotas.stream().noneMatch(rota -> rota.caminho().startsWith("/api/governanca-terceiros")),
                () -> "Rotas removidas ainda declaradas: " + rotas.stream()
                        .filter(rota -> rota.caminho().startsWith("/api/governanca-terceiros"))
                        .map(Rota::identidade).toList());
        assertTrue(rotas.stream().noneMatch(rota -> rota.handler().getName().equals("avaliarRisco")),
                "A avaliação manual não pode sobreviver com outro prefixo HTTP");
    }

    @Test
    void entidadesAtivasPreservamFornecedorERetiramProcessoERisco() {
        Set<String> entidades = classesAnotadas("entidade", Entity.class).stream()
                .map(Class::getSimpleName).collect(Collectors.toSet());
        assertTrue(entidades.containsAll(Set.of("Fornecedor", "Contrato", "AditamentoContratual",
                "VersaoContratoNegocial", "Empregado", "Frequencia")),
                "A retirada não pode eliminar identidade ou fatos operacionais compartilhados");
        assertFalse(entidades.contains("ProcessoTerceiro"), "Processo exclusivo ainda mapeado");
        assertFalse(entidades.contains("AvaliacaoRiscoFornecedor"), "Avaliação exclusiva ainda mapeada");
    }

    @Test
    void cadastroOperacionalConservaAsQuatroRotasComGuardaESenha() {
        Class<?> controlador = exigirClasse("controlador.FornecedorControlador");
        Set<String> caminhos = rotas(controlador).stream().map(Rota::identidade)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(Set.of("GET /api/fornecedores", "POST /api/fornecedores",
                "PUT /api/fornecedores/{id}",
                "PUT /api/contratos/{contratoId}/fornecedor/{fornecedorId}"), caminhos);
        PreAuthorize autorizacao = controlador.getAnnotation(PreAuthorize.class);
        assertNotNull(autorizacao, "O catálogo mantém guarda explícita de Gestão Local");
        assertEquals("hasAuthority('PERFIL_GESTOR_LOCAL')", autorizacao.value());
        assertTrue(controlador.isAnnotationPresent(InvalidaCacheNegocio.class));
        for (Rota rota : rotas(controlador)) {
            if (!rota.metodo().equals("GET")) {
                assertTrue(rota.handler().isAnnotationPresent(ExigirSenha.class),
                        "Mutação sem confirmação de senha: " + rota.identidade());
            }
        }
    }

    @Test
    void contratosCadastraisNaoExpoemEscoresOuMonitoramento() {
        assertComponentes("dto.FornecedorEntradaDTO", "razaoSocial", "cnpj", "email", "ativo");
        assertComponentes("dto.FornecedorDTO", "id", "razaoSocial", "cnpj", "email", "ativo");
        assertComponentes("dto.ContratoFornecedorDTO", "contratoId", "fornecedor");
        Class<?> vinculo = exigirClasse("dto.ContratoFornecedorDTO");
        assertEquals(exigirClasse("dto.FornecedorDTO"), vinculo.getRecordComponents()[1].getType());
    }

    @Test
    void fornecedorNaoOfereceRiscoESoConservaColunaLegadaImutavel() {
        Set<String> metodos = Arrays.stream(Fornecedor.class.getMethods()).map(Method::getName)
                .collect(Collectors.toSet());
        for (String sufixo : List.of("RiscoScore", "RiscoNivel", "ProximaAvaliacao", "MonitoramentoStatus")) {
            assertFalse(metodos.contains("get" + sufixo), "Leitura de risco ainda exposta: " + sufixo);
            assertFalse(metodos.contains("set" + sufixo), "Mutação de risco ainda exposta: " + sufixo);
        }
        Set<String> colunas = Arrays.stream(Fornecedor.class.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(Column.class))
                .map(campo -> campo.getAnnotation(Column.class).name()).collect(Collectors.toSet());
        assertFalse(colunas.contains("risco_score"));
        assertFalse(colunas.contains("risco_nivel"));
        assertFalse(colunas.contains("proxima_avaliacao"));
        var legadas = Arrays.stream(Fornecedor.class.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(Column.class))
                .filter(campo -> campo.getAnnotation(Column.class).name().equals("monitoramento_status")).toList();
        assertEquals(1, legadas.size(), "Schema legado exige preenchimento desta coluna no INSERT");
        var campo = legadas.get(0);
        assertTrue(Modifier.isPrivate(campo.getModifiers()));
        assertFalse(campo.getAnnotation(Column.class).updatable(),
                "Edição cadastral não pode regravar o valor histórico de monitoramento");
        String sufixo = Character.toUpperCase(campo.getName().charAt(0)) + campo.getName().substring(1);
        assertFalse(metodos.contains("get" + sufixo), "A exceção de INSERT não é uma API de consulta");
        assertFalse(metodos.contains("set" + sufixo), "A exceção de INSERT não é uma API de edição");
    }

    private static Class<?> exigirClasse(String nome) {
        return assertDoesNotThrow(() -> Class.forName(PACOTE + nome), "Contrato operacional ausente: " + nome);
    }

    private static void assertComponentes(String nome, String... esperados) {
        Class<?> tipo = exigirClasse(nome);
        assertTrue(tipo.isRecord(), "Contrato de dados precisa continuar explícito: " + nome);
        assertArrayEquals(esperados, Arrays.stream(tipo.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName).toArray(String[]::new));
    }

    private static List<Class<?>> classesAnotadas(String subpacote, Class<? extends Annotation> anotacao) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(anotacao));
        return scanner.findCandidateComponents(PACOTE + subpacote).stream()
                .map(bean -> assertDoesNotThrow(() -> Class.forName(bean.getBeanClassName())))
                .collect(Collectors.toList());
    }

    private static List<Rota> rotas(Class<?> controlador) {
        RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(controlador, RequestMapping.class);
        List<Rota> resultado = new ArrayList<>();
        for (Method metodo : controlador.getDeclaredMethods()) {
            RequestMapping recurso = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
            if (recurso == null) continue;
            for (String prefixo : caminhos(base)) for (String sufixo : caminhos(recurso)) {
                String caminho = (prefixo + "/" + sufixo).replaceAll("/{2,}", "/");
                if (recurso.method().length == 0) resultado.add(new Rota("ANY", caminho, metodo));
                else for (var verbo : recurso.method()) resultado.add(new Rota(verbo.name(), caminho, metodo));
            }
        }
        return resultado;
    }

    private static String[] caminhos(RequestMapping mapping) {
        if (mapping == null) return new String[]{""};
        String[] valores = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return valores.length == 0 ? new String[]{""} : valores;
    }

    private record Rota(String metodo, String caminho, Method handler) {
        String identidade() { return metodo + " " + caminho; }
    }
}
