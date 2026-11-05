package com.example.siggtrat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

/** Teste unitário da guarda de versão; não simula nem substitui banco funcional. */
class GuardaMysql8045Test {
    @Test
    void aceitaSomenteIdentidadeExataEConcordante() {
        assertThatCode(() -> GuardaMysql8045.exigirVersao("MySQL", "8.0.45", "8.0.45")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"MySQL,8.0.46,8.0.46", "MySQL,8.4.0,8.4.0", "MySQL,8.0.45,8.0.46",
            "MySQL,8.0.46,8.0.45", "MySQL,8.0.45-community,8.0.45-community",
            "MariaDB,8.0.45,8.0.45", "H2,8.0.45,8.0.45", "MySQL,5.7.44,5.7.44"})
    void recusaSubstituicoesEDivergenciaEntreMetadataEServidor(String produto, String metadata, String servidor) {
        assertThatThrownBy(() -> GuardaMysql8045.exigirVersao(produto, metadata, servidor))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("8.0.45").hasMessageContaining("recusada");
    }
}
