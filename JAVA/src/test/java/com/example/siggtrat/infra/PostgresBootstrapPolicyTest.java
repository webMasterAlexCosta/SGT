package com.example.siggtrat.infra;
import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PostgresBootstrapPolicyTest {
 @Test void perfilProdValidaSemMutarESemExporSql() throws Exception {
  var p=new Properties();try(var in=Files.newInputStream(Path.of("src/main/resources/application-prod.properties"))){p.load(in);}
  assertThat(p.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
  assertThat(p.getProperty("spring.jpa.generate-ddl")).isEqualTo("false");
  assertThat(p.getProperty("spring.sql.init.mode")).isEqualTo("never");
  assertThat(p.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("OFF");
  assertThat(p.getProperty("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("OFF");
 }
 @Test void identidadeInvalidaInterrompeAntesDoConsumidorJpa() throws Exception {
  var ds=mock(DataSource.class);var c=mock(Connection.class);var st=mock(Statement.class);var rs=mock(ResultSet.class);var md=mock(DatabaseMetaData.class);
  when(ds.getConnection()).thenReturn(c);when(c.createStatement()).thenReturn(st);when(c.getMetaData()).thenReturn(md);when(st.executeQuery(anyString())).thenReturn(rs);when(rs.next()).thenReturn(true);
  when(md.getDatabaseProductName()).thenReturn("H2");when(md.getDatabaseProductVersion()).thenReturn("18.6");when(rs.getString(1)).thenReturn("PostgreSQL 18.6 on linux");when(rs.getInt(2)).thenReturn(180006);
  var jpaIniciado=new AtomicBoolean();
  try(var context=new GenericApplicationContext()) {
   context.getBeanFactory().addBeanPostProcessor(new VerificadorAmbientePostgres186());
   context.registerBean("dataSource",DataSource.class,()->ds);
   context.registerBean("entityManagerFactory",Object.class,()->{context.getBean(DataSource.class);jpaIniciado.set(true);return new Object();});
   assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class);
   assertThat(jpaIniciado).isFalse();
  }
  verify(c).close();
 }
}
