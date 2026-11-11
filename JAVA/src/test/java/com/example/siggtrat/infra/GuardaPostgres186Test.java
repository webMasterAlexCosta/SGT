package com.example.siggtrat.infra;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
class GuardaPostgres186Test {
 @Test void aceitaIdentidadeExataComOuSemSufixoDaDistribuicao() {
  assertThatCode(() -> GuardaPostgres186.exigirVersao("PostgreSQL", "18.6", "PostgreSQL 18.6 on x86_64-pc-linux-gnu, compiled by gcc",180006)).doesNotThrowAnyException();
  assertThatCode(() -> GuardaPostgres186.exigirVersao("PostgreSQL", "18.6 (Debian 18.6-1.pgdg12+2)", "PostgreSQL 18.6 (Debian 18.6-1.pgdg12+2) on x86_64-pc-linux-gnu, compiled by gcc",180006)).doesNotThrowAnyException();
 }
 @ParameterizedTest @CsvSource({"MySQL,18.6,180006", "H2,18.6,180006", "PostgreSQL,18.5,180006", "PostgreSQL,18.6,180005", "PostgreSQL,18.6rc1,180006", "PostgreSQL,18.60,180006", "PostgreSQL,19.6,190006", "PostgreSQL,18.6 (unsupported spoof),180006"})
 void recusaSubstituicaoPreReleaseOuDivergencia(String produto,String metadata,int numero) {
  assertThatThrownBy(() -> GuardaPostgres186.exigirVersao(produto,metadata,"PostgreSQL "+metadata+" on linux",numero)).isInstanceOf(IllegalStateException.class).hasMessageContaining("18.6").hasMessageContaining("recusada");
 }
 @Test void naoAceitaVersaoNumericaComoProvaIsolada() {
  assertThatThrownBy(() -> GuardaPostgres186.exigirVersao("PostgreSQL","18.6","PostgreSQL 18.7 on linux",180006)).isInstanceOf(IllegalStateException.class);
 }
}
