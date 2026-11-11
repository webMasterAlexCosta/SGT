package com.example.siggtrat.integration;

import com.example.siggtrat.configuracao.propriedades.PropriedadesAuditoria;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.HistoricoEmpregadoRepositorio;
import com.example.siggtrat.servico.HistoricoIntegridadeServico;
import com.example.siggtrat.servico.RelatorioConsultaServico;
import com.example.siggtrat.servico.RelatorioUsuarioServico;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enquanto alguns services ainda possuem JDBC/EntityManager direto, estes
 * testes garantem que essas chamadas também sejam exercitadas contra o PostgreSQL.
 * Se essas consultas forem movidas para repositories no futuro, o teste pode
 * acompanhar a refatoração sem perder cobertura do SQL.
 */
@DisplayName("Integração PostgreSQL: chamadas diretas de banco ainda existentes em services")
class DirectDatabaseServiceSmokeIT extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmpregadoRepositorio empregadoRepositorio;

    @Autowired
    private HistoricoEmpregadoRepositorio historicoRepositorio;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("RelatorioUsuarioServico executa o SELECT de exportação e produz CSV válido com base vazia")
    void userReportSqlExecuta() throws Exception {
        RelatorioUsuarioServico servico = new RelatorioUsuarioServico(jdbc);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RelatorioUsuarioServico.Metadados meta = servico.gravarCsv(out);
        String csv = out.toString(java.nio.charset.StandardCharsets.UTF_8);

        assertNotNull(meta);
        assertTrue(meta.fileName().startsWith("usuarios_"));
        assertTrue(csv.contains("id;username;email;matricula;perfil;active;lotacao_id;lotacao_nome;jornada_id;jornada_desc"));
    }

    @Test
    @DisplayName("RelatorioConsultaServico executa os três SQLs JDBC privados contra o schema real")
    @SuppressWarnings("unchecked")
    void relatorioQuerySqlExecuta() throws Exception {
        RelatorioConsultaServico servico = new RelatorioConsultaServico(
                jdbc, null, null, null, null, null, null, null, null);

        Method carregarEmpregados = RelatorioConsultaServico.class.getDeclaredMethod(
                "carregarEmpregados", Long.class, Long.class, Long.class);
        carregarEmpregados.setAccessible(true);
        List<?> empregados = (List<?>) carregarEmpregados.invoke(servico, -9_999_991L, null, null);
        assertTrue(empregados.isEmpty());

        Class<?> empregadoRelatorioClass = java.util.Arrays.stream(RelatorioConsultaServico.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("EmpregadoRelatorio"))
                .findFirst().orElseThrow();
        Constructor<?> ctor = empregadoRelatorioClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object empregado = ctor.newInstance(
                -9_999_991L, "TESTE", "-", "-", "-",
                null, null, null, null);
        List<?> lista = List.of(empregado);

        Method carregarFrequencias = RelatorioConsultaServico.class.getDeclaredMethod(
                "carregarFrequencias", List.class, LocalDate.class, LocalDate.class, boolean.class);
        carregarFrequencias.setAccessible(true);
        Map<?, ?> frequencias = (Map<?, ?>) carregarFrequencias.invoke(
                servico, lista, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), false);
        assertTrue(frequencias.isEmpty());

        Method carregarHoras = RelatorioConsultaServico.class.getDeclaredMethod(
                "carregarAgregacoesDeHoras", List.class, LocalDate.class, LocalDate.class);
        carregarHoras.setAccessible(true);
        Map<?, ?> horas = (Map<?, ?>) carregarHoras.invoke(
                servico, lista, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertTrue(horas.isEmpty());
    }

    @Test
    @DisplayName("HistoricoIntegridadeServico executa lock pessimista e rejeita empregado inexistente")
    void historicoIntegridadeLockExecuta() {
        PropriedadesAuditoria properties = new PropriedadesAuditoria(
                "test-audit-secret-with-at-least-32-characters",
                Path.of(System.getProperty("java.io.tmpdir"), "audit-it.log"),
                new PropriedadesAuditoria.ConteudoRequisicao(65_536),
                "(?i)^(password|senha|token|secret)$"
        );
        HistoricoIntegridadeServico servico = new HistoricoIntegridadeServico(
                historicoRepositorio, empregadoRepositorio, entityManager, properties);

        HistoricoEmpregado historico = new HistoricoEmpregado();
        Empregado empregado = new Empregado();
        empregado.setId(-9_999_991L);
        historico.setEmpregado(empregado);
        historico.setDataEvento(LocalDateTime.of(2026, 1, 15, 12, 0));
        historico.setDescricao("teste de lock");

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tx.executeWithoutResult(status -> servico.salvarAssinado(historico)));
        assertTrue(ex.getMessage().contains("não encontrado"));
    }
}
