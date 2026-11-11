package com.example.siggtrat.integration;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import jakarta.persistence.EntityManager;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** PostgreSQL real: introspecção de schema, bytea/text, transações e precisão. */
class PostgresMappingRoundTripIT extends PostgresIntegrationTestSupport {
 @Autowired DataSource dataSource;
 @Autowired JdbcTemplate jdbc;
 @Autowired EntityManager em;
 @Autowired PlatformTransactionManager manager;
 @Autowired TokenAtivoRepositorio tokens;

 @Test void identidadeCompletaRoleLimitadaETiposSemOid() throws Exception {
  try(var c=dataSource.getConnection()) { VerificadorAmbientePostgres186.verificar(c,"roundtrip-it"); }
  Map<String,String> campos=Map.of(
    "emissao_relatorio_administrativo.conteudo_snapshot_gzip","bytea",
    "emissao_relatorio_administrativo.filtros_json","text",
    "evento_frequencia.snapshot_anterior","text", "evento_frequencia.snapshot_novo","text",
    "calendario_evento_versao.fotografia_json","text", "frequencia_complemento_versao.fotografia_json","text",
    "frequencia_complemento_versao.contextos_json","text", "painel_calculo.payload_json","text");
  for(var campo:campos.entrySet()) {
   String[] partes=campo.getKey().split("\\.");
   assertThat(jdbc.queryForObject("select udt_name from information_schema.columns where table_schema=current_schema() and table_name=? and column_name=?",String.class,partes[0],partes[1])).as(campo.getKey()).isEqualTo(campo.getValue());
  }
  assertThat(jdbc.queryForObject("select count(*) from pg_largeobject_metadata",Long.class)).isZero();
  var identidade=jdbc.queryForMap("select version() as server_version, current_setting('server_version_num') as version_num,current_database() as database,current_user as role,current_schema() as schema,inet_server_addr()::text as address,inet_server_port() as port,pg_backend_pid() as pid");
  Path p=Path.of("target/postgres-persistence/identity-and-types.txt"); Files.createDirectories(p.getParent());
  Files.writeString(p,identidade+"\n"+new TreeMap<>(campos)+"\n");
 }
 @Test void fotografiaGrandePreservaBytesHashTextoDatasENulosEmNovaTransacao() throws Exception {
  byte[] bytes=new byte[131089]; new Random(186).nextBytes(bytes);
  String texto="{\"ação\":\"João 日本語 😀 "+"x".repeat(70000)+"\"}";
  Instant instante=Instant.parse("2026-11-06T02:59:59.123456Z");
  var tx=new TransactionTemplate(manager);
  Long id=tx.execute(s->{ var e=emissao(bytes,texto,instante); em.persist(e); em.flush(); return e.getId(); });
  try {
   tx.executeWithoutResult(s->{ var e=em.find(EmissaoRelatorioAdministrativo.class,id); assertThat(e.getConteudoSnapshotGzip()).containsExactly(bytes); assertThat(e.getFiltrosJson()).isEqualTo(texto); assertThat(e.getGeradoEm()).isEqualTo(instante); assertThat(e.getFonteConhecidaAte()).isEqualTo(instante); assertThat(e.getInicio()).isEqualTo(LocalDate.of(2026,11,5)); assertThat(e.getContratoIdsCsv()).isNull(); assertThat(e.getVersion()).isZero(); });
   byte[] persistidos=jdbc.queryForObject("select conteudo_snapshot_gzip from emissao_relatorio_administrativo where id=?",byte[].class,id);
   assertThat(MessageDigest.getInstance("SHA-256").digest(persistidos)).containsExactly(MessageDigest.getInstance("SHA-256").digest(bytes));
   assertThat(jdbc.queryForObject("select octet_length(conteudo_snapshot_gzip) from emissao_relatorio_administrativo where id=?",Integer.class,id)).isEqualTo(bytes.length);
  } finally {tx.executeWithoutResult(s->em.remove(em.find(EmissaoRelatorioAdministrativo.class,id)));}
 }
 @Test void datasCivisConservamOrdemSemanalEIntervaloInclusivo() {
  assertThat(jdbc.queryForList("select extract(isodow from d)::integer-1 from (values (date '2026-11-02'),(date '2026-11-08')) x(d)",Integer.class)).containsExactly(0,6);
  assertThat(jdbc.queryForList("with recursive datas as (select date '2026-11-05' d union all select d+1 from datas where d < date '2026-11-07') select d from datas order by d", java.sql.Date.class)).containsExactly(java.sql.Date.valueOf("2026-11-05"),java.sql.Date.valueOf("2026-11-06"),java.sql.Date.valueOf("2026-11-07"));
 }
 @Test void rollbackNaoPublicaFotografia() {
  String marcador=UUID.randomUUID().toString(); var tx=new TransactionTemplate(manager);
  tx.executeWithoutResult(s->{var e=emissao(new byte[]{0,1,-1},"{}",Instant.parse("2026-11-06T00:00:00.000001Z")); e.setChaveIdempotencia(marcador); em.persist(e); em.flush(); s.setRollbackOnly();});
  assertThat(jdbc.queryForObject("select count(*) from emissao_relatorio_administrativo where chave_idempotencia=?",Long.class,marcador)).isZero();
 }
 private EmissaoRelatorioAdministrativo emissao(byte[] bytes,String texto,Instant instante) {
  var e=new EmissaoRelatorioAdministrativo(); e.setChaveSerie(UUID.randomUUID().toString().replace("-","").repeat(2));e.setNumeroVersao(1);e.setTipo(TipoRelatorioAdministrativo.FREQUENCIA_CONSOLIDADA);e.setFormato(FormatoEmissaoRelatorio.VISUALIZACAO);e.setNatureza(NaturezaEmissaoRelatorio.ORIGINAL);e.setInicio(LocalDate.of(2026,11,5));e.setFim(LocalDate.of(2026,11,6));e.setGeradoEm(instante);e.setFonteConhecidaAte(instante);e.setUsuarioId(186L);e.setUsuarioUsername("postgres-it");e.setPerfilUsuario("ADMINISTRADOR");e.setAvisoRevisao("");e.setConteudoSnapshotGzip(bytes);e.setHashConteudo("a".repeat(64));e.setTamanhoConteudoBytes(bytes.length);e.setFiltrosJson(texto);e.setChaveIdempotencia(UUID.randomUUID().toString());e.setHashIntencao("b".repeat(64));return e;
 }
}
