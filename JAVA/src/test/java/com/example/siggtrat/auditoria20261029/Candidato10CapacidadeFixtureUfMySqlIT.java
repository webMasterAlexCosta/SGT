package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Autoria independente: /root/revisao_final_pessoas.
 * Regressão da capacidade da fixture, reproduzida na integral04 C9.
 * Ocupa apenas nesta transação o namespace antigo e chama o helper real.
 * Não há commit, DELETE, UPDATE, alteração de schema ou reaproveitamento de SE.
 * Compila também contra C9 original: não depende de membro novo do helper.
 * Execução central pendente; este arquivo não é prova de GREEN.
 */
@Rollback
class Candidato10CapacidadeFixtureUfMySqlIT extends AuditoriaHttpSuporte {
    private static final String ALFABETO_PROPOSTO =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ!#$%&()+,-.;=@[]^_{}~";
    private static final List<String> TABELAS = List.of(
            "superintendencia_estadual", "gestao_local", "jornada", "jornada_dia_semana",
            "lotacao", "cargo", "tipo_contratacao", "contrato", "contrato_versao_negocial",
            "composicao_contratual", "pessoa", "vinculo_contratual", "vinculo_contratual_evento",
            "alocacao_vinculo", "empregado", "usuario_aplicacao", "evento_disponibilidade_catalogo");
    @Autowired private DataSource dataSource;
    private JdbcTemplate jdbc;
    private List<Map<String, Object>> superintendenciasAntes;
    private Map<String, Long> contagensAntes;
    private final Map<String, Object> evidencia = new LinkedHashMap<>();

    @BeforeTransaction
    void fotografarAntesDaTransacaoDeTeste() {
        jdbc = new JdbcTemplate(dataSource);
        superintendenciasAntes = fotografarSuperintendencias();
        contagensAntes = contarLinhasDaFixture();
        evidencia.put("classe", getClass().getSimpleName());
        evidencia.put("doisCenariosConcluidos", false);
        evidencia.put("contagensAntes", contagensAntes);
    }

    @Test
    void criaDuasSEsNovasPorCenarioAposEsgotarTodosOsParesBase36() throws Exception {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        try (var connection = dataSource.getConnection()) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertTrue(connection.getMetaData().getDatabaseProductVersion().equals("8.0.45"));
        }
        Map<String, Object> coluna = jdbc.queryForMap("""
                SELECT c.DATA_TYPE AS tipo, c.CHARACTER_MAXIMUM_LENGTH AS largura,
                       c.COLLATION_NAME AS collation, c.CHARACTER_SET_NAME AS charset,
                       x.PAD_ATTRIBUTE AS padding
                  FROM information_schema.COLUMNS c
                  JOIN information_schema.COLLATIONS x ON x.COLLATION_NAME=c.COLLATION_NAME
                 WHERE c.TABLE_SCHEMA=DATABASE() AND c.TABLE_NAME='superintendencia_estadual'
                   AND c.COLUMN_NAME='uf'
                """);
        assertEquals("varchar", coluna.get("tipo"));
        assertEquals(2, ((Number) coluna.get("largura")).intValue());
        assertEquals("utf8mb4", coluna.get("charset"));
        assertEquals("utf8mb4_0900_ai_ci", coluna.get("collation"));
        assertEquals("NO PAD", coluna.get("padding"));
        assertEquals(1L, jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='superintendencia_estadual'
                   AND INDEX_NAME='uk_superintendencia_estadual_uf' AND NON_UNIQUE=0 AND COLUMN_NAME='uf'
                """, Long.class));
        List<String> namespace = new ArrayList<>();
        for (char primeiro : ALFABETO_PROPOSTO.toCharArray())
            for (char segundo : ALFABETO_PROPOSTO.toCharArray())
                namespace.add("" + primeiro + segundo);
        assertEquals(3249, new HashSet<>(namespace).size());
        // Verifica TODOS os pares sob a collation efetiva, sem inserir a expansão inteira.
        Long distintos = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT CONVERT(j.uf USING utf8mb4) COLLATE utf8mb4_0900_ai_ci)
                  FROM JSON_TABLE(?, '$[*]' COLUMNS(uf VARCHAR(2) PATH '$')) AS j
                """, Long.class, json(namespace));
        assertEquals(3249L, distintos);
        evidencia.put("colunaMySql", coluna);
        evidencia.put("paresDistintosNaCollationReal", distintos);

        Set<String> reservadas = new HashSet<>(ConstantesDominio.UFS_BRASIL);
        assertEquals(27, reservadas.size());
        reservadas.add("YY");
        Set<String> antigas = new HashSet<>();
        for (int n=0; n<36*36; n++) {
            String uf = String.format(Locale.ROOT, "%2s", Integer.toString(n,36))
                    .replace(' ', '0').toUpperCase(Locale.ROOT);
            if (!reservadas.contains(uf)) antigas.add(uf);
        }
        assertEquals(1268, antigas.size());
        Set<String> existentes = new HashSet<>(em.createQuery(
                "select s.uf from SuperintendenciaEstadual s", String.class).getResultList());
        int preenchidas = 0;
        for (String uf : antigas) {
            if (existentes.contains(uf)) continue;
            em.persist(new SuperintendenciaEstadual("Ocupação sintética rollback " + uf, uf));
            preenchidas++;
        }
        em.flush();
        Set<String> ocupadas = new HashSet<>(em.createQuery(
                "select s.uf from SuperintendenciaEstadual s", String.class).getResultList());
        assertTrue(ocupadas.containsAll(antigas), "Toda UF que o helper antigo podia emitir está ocupada");
        evidencia.put("paresAntigosOcupados", antigas.size());
        evidencia.put("seInseridasParaEsgotarNamespaceAntigo", preenchidas);
        gravarEvidencia();

        Cenario primeiro = criarCenario();
        Cenario segundo = criarCenario();
        List<SuperintendenciaEstadual> novas = List.of(primeiro.seA, primeiro.seB, segundo.seA, segundo.seB);
        List<GestaoLocal> gestoes = List.of(primeiro.gestaoA, primeiro.gestaoB, segundo.gestaoA, segundo.gestaoB);
        assertEquals(4, novas.stream().map(SuperintendenciaEstadual::getId).distinct().count());
        assertEquals(4, novas.stream().map(SuperintendenciaEstadual::getUf).distinct().count());
        List<Map<String, Object>> identidades = new ArrayList<>();
        for (int i=0; i<novas.size(); i++) {
            var se = novas.get(i);
            assertEquals(2, se.getUf().length());
            assertTrue(namespace.contains(se.getUf()));
            assertFalse(se.getUf().matches("[0-9A-Z]{2}"), "Novo código precisa superar a base36 saturada");
            assertFalse(ocupadas.contains(se.getUf()));
            assertFalse(reservadas.contains(se.getUf()));
            assertEquals(se.getId(), gestoes.get(i).getSuperintendencia().getId());
            identidades.add(Map.of("idSE", se.getId(), "uf", se.getUf(), "idGestao", gestoes.get(i).getId()));
        }
        em.flush();
        em.clear();
        for (var item : identidades) {
            var se = em.find(SuperintendenciaEstadual.class, ((Number) item.get("idSE")).longValue());
            var gestao = em.find(GestaoLocal.class, ((Number) item.get("idGestao")).longValue());
            assertEquals(item.get("uf"), se.getUf());
            assertEquals("SE/" + item.get("uf"), se.getNome());
            assertEquals(se.getId(), gestao.getSuperintendencia().getId());
        }
        assertEquals(contagensAntes.get("superintendencia_estadual") + preenchidas + 4L,
                jdbc.queryForObject("SELECT COUNT(*) FROM superintendencia_estadual", Long.class));
        evidencia.put("doisCenariosConcluidos", true);
        evidencia.put("identidadesNovasRelidasAposFlushEClear", identidades);
        gravarEvidencia();
    }

    @AfterTransaction
    void conferirRollbackEmLeituraIndependente() throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        var depois = fotografarSuperintendencias();
        var contagensDepois = contarLinhasDaFixture();
        evidencia.put("contagensDepoisDoRollback", contagensDepois);
        evidencia.put("rollbackConservouLinhasSE", superintendenciasAntes.equals(depois));
        evidencia.put("rollbackConservouContagensDaFixture", contagensAntes.equals(contagensDepois));
        gravarEvidencia();
        assertEquals(superintendenciasAntes, depois, "Nenhuma SE temporária pode vazar para os demais casos");
        assertEquals(contagensAntes, contagensDepois, "Os dois cenários também precisam ser revertidos");
    }

    private List<Map<String, Object>> fotografarSuperintendencias() {
        return jdbc.queryForList("SELECT id_superintendencia, uf, nome, ativo, version "
                + "FROM superintendencia_estadual ORDER BY id_superintendencia");
    }
    private Map<String, Long> contarLinhasDaFixture() {
        Map<String, Long> contagens = new LinkedHashMap<>();
        for (String tabela : TABELAS)
            contagens.put(tabela, jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class));
        return contagens;
    }
    private void gravarEvidencia() throws Exception {
        Path path = Path.of("target/auditoria-http/candidato10-capacidade-fixture-uf.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, json(evidencia));
    }
}
