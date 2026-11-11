package com.example.siggtrat.auditoria20261029;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
/** Prova da conexão efetiva e política Hibernate do contexto completo. */
class Postgres186AmbienteHttpIT extends AuditoriaHttpSuporte {
 @Autowired DataSource dataSource;
 @Autowired Environment environment;
 @Autowired JdbcTemplate jdbc;
 @Autowired org.springframework.context.ApplicationContext contexto;
 @Test void conexaoEfetivaUsaServidorExatoESchemaSomenteHibernate() throws Exception {
  assertThat(contexto.getBeansOfType(com.example.siggtrat.configuracao.VerificadorAmbientePostgres186.class)).hasSize(1);
  Map<String,Object> prova=new TreeMap<>();
  try(var c=dataSource.getConnection()) {
   com.example.siggtrat.configuracao.VerificadorAmbientePostgres186.verificar(c,"http-it");
   assertThat(c.getMetaData().getDriverVersion()).isEqualTo("42.7.14");
   String usuario=environment.getRequiredProperty("spring.datasource.username");
   assertThat(c.getMetaData().getUserName()).isEqualTo(usuario);
   assertThat(jdbc.queryForObject("select current_user",String.class)).isEqualTo(usuario);
   prova.put("driver",c.getMetaData().getDriverVersion()); prova.put("metadataVersion",c.getMetaData().getDatabaseProductVersion());
  }
  assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
  assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
  assertThat(jdbc.queryForObject("select current_setting('session_replication_role')",String.class)).isEqualTo("origin");
  assertThat(jdbc.queryForObject("select current_setting('server_encoding')",String.class)).isEqualTo("UTF8");
  assertThat(jdbc.queryForObject("select (extract(microseconds from timestamp '2026-11-01 23:59:59.123456')::bigint % 1000000)",Integer.class)).isEqualTo(123456);
  assertThat(jdbc.queryForObject("select '{\"presenca\":true}'::jsonb ->> 'presenca'",String.class)).isEqualTo("true");
  prova.putAll(jdbc.queryForMap("select version() as server_version,current_setting('server_version_num') as server_version_num,current_database() as database,current_user as role,current_schema() as schema,inet_server_addr()::text as address,inet_server_port() as port,pg_backend_pid() as pid,current_setting('TimeZone') as timezone,current_setting('transaction_isolation') as isolation"));
  prova.put("ddlAuto",environment.getProperty("spring.jpa.hibernate.ddl-auto")); prova.put("sqlInit",environment.getProperty("spring.sql.init.mode"));
  Path p=Path.of("target/auditoria-http/postgres186-conexao-aplicacao.json");Files.createDirectories(p.getParent());Files.writeString(p,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prova));
 }
}
