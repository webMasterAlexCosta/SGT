package com.example.siggtrat.infra;
import com.example.siggtrat.entidade.*;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.nio.file.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
/** Impede regressão para OID e SQL exclusivo do servidor legado. */
class PostgresPersistenceContractTest {
 @Test void runtimeUsaSomenteDriverPostgresqlFixado() throws Exception {
  String pom = Files.readString(Path.of("pom.xml"));
  assertThat(pom).contains("<postgresql.version>42.7.14</postgresql.version>").doesNotContain("mysql-connector-j", "<artifactId>mysql</artifactId>");
 }
 @Test void fotografiasSaoTextoEBytesSemOid() throws Exception {
  for (Class<?> type : new Class<?>[]{EmissaoRelatorioAdministrativo.class, EventoFrequencia.class, VersaoCalendarioEvento.class, VersaoComplementoFrequencia.class, PainelCalculo.class}) {
   for (var field : type.getDeclaredFields()) {
    Column column = field.getAnnotation(Column.class);
    if (column == null) continue;
    assertThat(column.columnDefinition()).doesNotContain("LONGTEXT", "LONGBLOB");
    assertThat(field.isAnnotationPresent(Lob.class)).as(type.getSimpleName()+"."+field.getName()).isFalse();
    if (column.columnDefinition().equals("text") || field.getType() == byte[].class) {
     var jdbc = field.getAnnotation(JdbcTypeCode.class);
     assertThat(jdbc).as(type.getSimpleName()+"."+field.getName()).isNotNull();
     assertThat(jdbc.value()).isEqualTo(field.getType() == byte[].class ? SqlTypes.VARBINARY : SqlTypes.LONGVARCHAR);
    }
   }
  }
 }
 @Test void consultasNativasNaoDependemDeUnsignedOuDeleteLimit() throws Exception {
  assertThat(Files.readString(Path.of("src/main/java/com/example/siggtrat/repositorio/EmpregadoRepositorio.java"))).doesNotContain("AS UNSIGNED");
  String tokens = Files.readString(Path.of("src/main/java/com/example/siggtrat/repositorio/TokenAtivoRepositorio.java"));
  assertThat(tokens).doesNotContain("DELETE FROM token_ativo WHERE expire_at < :now LIMIT").contains("ORDER BY expire_at, id", "FOR UPDATE SKIP LOCKED");
 }
}
