package com.example.siggtrat.infra;

import com.example.siggtrat.configuracao.VerificadorAmbientePostgres186;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostgresAdministrativeMembershipGuardTest {
    private Connection conexao(boolean associacaoAdministrativa) throws Exception {
        var c = mock(Connection.class); var s = mock(Statement.class); var r = mock(ResultSet.class); var md = mock(DatabaseMetaData.class);
        when(c.createStatement()).thenReturn(s); when(s.executeQuery(anyString())).thenReturn(r); when(c.getMetaData()).thenReturn(md);
        when(r.next()).thenReturn(true, false); when(md.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(md.getDatabaseProductVersion()).thenReturn("18.6"); when(md.getUserName()).thenReturn("aplicacao_limitada");
        when(r.getString(1)).thenReturn("PostgreSQL 18.6 on linux"); when(r.getInt(2)).thenReturn(180006);
        when(r.getString(4)).thenReturn("aplicacao_limitada"); when(r.getString(5)).thenReturn("aplicacao_limitada");
        when(r.getBoolean(15)).thenReturn(associacaoAdministrativa);
        return c;
    }
    @Test void atributosPropriosFalsosNaoBastamQuandoHaAssociacaoAdministrativa() throws Exception {
        var c = conexao(true);
        assertThrows(IllegalStateException.class, () -> VerificadorAmbientePostgres186.verificar(c, "fixture"));
    }
    @Test void identidadeLimitadaSemAssociacaoAdministrativaContinuaAceita() throws Exception {
        var c = conexao(false);
        assertDoesNotThrow(() -> VerificadorAmbientePostgres186.verificar(c, "fixture"));
    }
}
