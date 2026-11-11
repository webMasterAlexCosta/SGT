package com.example.siggtrat.infra;
import com.example.siggtrat.configuracao.TratadorGlobalExcecoes;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import static org.assertj.core.api.Assertions.*;
class TratadorGlobalPostgresTest {
 @Test void notNullPostgresqlPreservaContrato400SemVazarLinha() {
  var sql=new SQLException("ERROR: null value in column \"nome\" of relation \"pessoa\" violates not-null constraint\nDetail: Failing row contains (payload-privado)","23502");
  var r=new TratadorGlobalExcecoes().tratarIntegridade(new DataIntegrityViolationException("write",sql));
  assertThat(r.getStatusCode().value()).isEqualTo(400);
  assertThat(r.getBody().get("message")).isEqualTo("Campo obrigatório não informado: nome");
  assertThat(r.getBody().toString()).doesNotContain("payload-privado","pessoa","Detail:");
 }
 @Test void uniqueEFkPreservamConflito409() {
  for(String estado:new String[]{"23505","23503"}) {
   var r=new TratadorGlobalExcecoes().tratarIntegridade(new DataIntegrityViolationException("write",new SQLException("causa interna",estado)));
   assertThat(r.getStatusCode().value()).isEqualTo(409);
   assertThat(r.getBody().toString()).doesNotContain("causa interna");
  }
 }
 @Test void serializacaoDeadlockELockIndisponivelPreservamConflitoConcorrente() {
  for(String estado:new String[]{"40001","40P01","55P03"}) {
   var r=new TratadorGlobalExcecoes().tratarDisputaConcorrente(new CannotAcquireLockException("conflito",new SQLException("causa interna",estado)));
   assertThat(r.getStatusCode().value()).isEqualTo(409);assertThat(r.getBody().get("code")).isEqualTo("CONCURRENT_OPERATION");
  }
 }
}
