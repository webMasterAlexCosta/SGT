package com.example.siggtrat.integration;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** SQL real, grafo sintético de catálogos. NÃO concede roles reais nem executa programas. */
class PostgresMembershipGraphIT extends PostgresIntegrationTestSupport {
    @Autowired DataSource dataSource;
    record Caso(String nome, String membros, String capacidade, boolean createdb, boolean recusada) {
        @Override public String toString() { return nome; }
    }
    static Stream<Caso> casos() {
        return Stream.of(
            new Caso("sem_associacao", "select 2::oid roleid, 9::oid member where false", "pg_execute_server_program", false, false),
            new Caso("execucao_direta", "values (3::oid,1::oid)", "pg_execute_server_program", false, true),
            new Caso("arquivo_transitivo_set_ou_inherit", "values (2::oid,1::oid),(3::oid,2::oid)", "pg_write_server_files", false, true),
            new Caso("leitura_arquivo_transitiva", "values (2::oid,1::oid),(3::oid,2::oid)", "pg_read_server_files", false, true),
            new Caso("createdb_transitivo", "values (2::oid,1::oid),(3::oid,2::oid)", "grupo_limitado", true, true),
            new Caso("grupo_sem_capacidade_administrativa", "values (2::oid,1::oid),(3::oid,2::oid)", "grupo_limitado", false, false),
            new Caso("role_perigosa_fora_da_cadeia", "values (2::oid,1::oid),(3::oid,9::oid)", "pg_execute_server_program", false, false)
        );
    }
    @ParameterizedTest(name="{0}") @MethodSource("casos")
    void classificaAssociacaoDiretaETransitivaSemGrant(Caso caso) throws Exception {
        // Somente valores estáticos dos fixtures acima; nenhum dado recebido de usuário.
        String prefixo = "WITH RECURSIVE roles_fixture(oid,rolname,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls) AS (VALUES "
            + "(1::oid,current_user::text,false,false,false,false,false),"
            + "(2::oid,'ponte',false,false,false,false,false),"
            + "(3::oid,'" + caso.capacidade() + "',false," + caso.createdb() + ",false,false,false)),"
            + "membros_fixture(roleid,member) AS (" + caso.membros() + "),";
        String sql = VerificadorAmbientePostgres186.CONSULTA_IDENTIDADE
            .replace("WITH RECURSIVE", prefixo)
            .replace("pg_catalog.pg_roles", "roles_fixture")
            .replace("pg_catalog.pg_auth_members", "membros_fixture");
        try (var c=dataSource.getConnection(); var s=c.createStatement(); var r=s.executeQuery(sql)) {
            assertTrue(r.next());
            assertEquals(180006,r.getInt(2));
            for(int i=10;i<=14;i++) assertFalse(r.getBoolean(i),"Atributos próprios permanecem falsos");
            assertEquals(caso.recusada(),r.getBoolean(15));
        }
    }
}
