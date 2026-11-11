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
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
            // pgJDBC pode retornar "18.6" sem sufixo de distribuição; exigir a versão real exata.
            assertEquals(180006, jdbc.queryForObject(
                    "SELECT current_setting('server_version_num')::integer", Integer.class));
        }
        Map<String,Object> coluna=jdbc.queryForMap("""
                SELECT c.data_type AS tipo,c.character_maximum_length AS largura,
                       current_setting('server_encoding') AS charset,
                       cn.nspname AS collation_schema,co.collname AS collation,
                       co.collprovider::text AS provider,co.collisdeterministic AS deterministica
                  FROM information_schema.columns c
                  JOIN pg_namespace n ON n.nspname=c.table_schema
                  JOIN pg_class t ON t.relnamespace=n.oid AND t.relname=c.table_name
                  JOIN pg_attribute a ON a.attrelid=t.oid AND a.attname=c.column_name AND NOT a.attisdropped
                  JOIN pg_collation co ON co.oid=a.attcollation
                  JOIN pg_namespace cn ON cn.oid=co.collnamespace
                 WHERE c.table_schema=current_schema() AND c.table_name='superintendencia_estadual'
                   AND c.column_name='uf'
                """);
        assertEquals("character varying",coluna.get("tipo"));
        assertEquals(2,((Number)coluna.get("largura")).intValue());
        assertEquals("UTF8",coluna.get("charset"));
        assertTrue(com.example.siggtrat.infra.EsperaJdbcPostgres.indiceUnico(jdbc,
                "superintendencia_estadual","uk_superintendencia_estadual_uf","uf"));
        String collation=identificador(coluna.get("collation_schema").toString())+"."
                +identificador(coluna.get("collation").toString());
        assertTrue(Boolean.TRUE.equals(jdbc.queryForObject(
                "select (cast(? as varchar(2)) collate "+collation+") <> (cast(? as varchar(2)) collate "+collation+")",
                Boolean.class,"A","A ")),"VARCHAR mantém distinção de espaço final nesta collation real");
        List<String> namespace = new ArrayList<>();
        for (char primeiro : ALFABETO_PROPOSTO.toCharArray())
            for (char segundo : ALFABETO_PROPOSTO.toCharArray())
                namespace.add("" + primeiro + segundo);
        assertEquals(3249, new HashSet<>(namespace).size());
        // Verifica TODOS os pares sob a collation efetiva, sem inserir a expansão inteira.
        Long distintos=jdbc.queryForObject(
                "SELECT count(DISTINCT cast(j.uf AS varchar(2)) COLLATE "+collation+") "
                        +"FROM jsonb_array_elements_text(cast(? AS jsonb)) AS j(uf)",Long.class,json(namespace));
        assertEquals(3249L,distintos);
        evidencia.put("colunaPostgres",coluna);
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

    private String identificador(String nome) {
        return "\""+nome.replace("\"","\"\"")+"\"";
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
