package com.example.sgtoperacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreparadorSchemaNovoTest {
    private MockEnvironment autorizado() {
        return new MockEnvironment().withProperty("sgt.schema.autorizado", "true")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                .withProperty("spring.jpa.generate-ddl", "true")
                .withProperty("spring.sql.init.mode", "never")
                .withProperty("spring.main.web-application-type", "none");
    }
    @Test void exigeConsentimentoExplicitoSemConectar() {
        assertThrows(IllegalStateException.class, () -> PreparadorSchemaNovo.verificarOpcoes(new MockEnvironment()));
        assertDoesNotThrow(() -> PreparadorSchemaNovo.verificarOpcoes(autorizado()));
    }
    @ParameterizedTest
    @CsvSource({"spring.jpa.hibernate.ddl-auto,create-drop", "spring.jpa.hibernate.ddl-auto,create", "spring.jpa.generate-ddl,false", "spring.sql.init.mode,always", "spring.main.web-application-type,servlet"})
    void rejeitaModoDestrutivoSqlOuServidorHttp(String key, String value) {
        var error=assertThrows(IllegalStateException.class, () -> PreparadorSchemaNovo.verificarOpcoes(autorizado().withProperty(key,value)));
        assertTrue(error.getMessage().contains("SGT_SCHEMA_NEW_CONFIG_INVALID"));
    }
    @Test void recusaBancoQueJaTemUmaTabelaOuObjetoDeUsuario() throws Exception {
        Connection c=mock(Connection.class);Statement s=mock(Statement.class);ResultSet r=mock(ResultSet.class);
        when(c.createStatement()).thenReturn(s);when(s.executeQuery(anyString())).thenReturn(r);when(r.next()).thenReturn(true);
        when(r.getBoolean(1)).thenReturn(true);when(r.getBoolean(2)).thenReturn(true);when(r.getLong(3)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> PreparadorSchemaNovo.verificarDestinoVazio(c));
        verify(s,times(1)).executeQuery(anyString());
        verify(c,never()).prepareStatement(anyString());
    }
    @Test void recusaBancoSemOwnershipOuSchemaSemCreate() throws Exception {
        for (int denied : new int[]{1,2}) {
            Connection c=mock(Connection.class);Statement s=mock(Statement.class);ResultSet r=mock(ResultSet.class);
            when(c.createStatement()).thenReturn(s);when(s.executeQuery(anyString())).thenReturn(r);when(r.next()).thenReturn(true);
            when(r.getBoolean(1)).thenReturn(denied!=1);when(r.getBoolean(2)).thenReturn(denied!=2);when(r.getLong(3)).thenReturn(0L);
            assertThrows(IllegalStateException.class, () -> PreparadorSchemaNovo.verificarDestinoVazio(c));
        }
    }
    @Test void aceitaSomenteDestinoNovoProprioSemEmitirDdlNoPreflight() throws Exception {
        Connection c=mock(Connection.class);Statement s=mock(Statement.class);ResultSet r=mock(ResultSet.class);
        when(c.createStatement()).thenReturn(s);when(s.executeQuery(anyString())).thenReturn(r);when(r.next()).thenReturn(true);
        when(r.getBoolean(1)).thenReturn(true);when(r.getBoolean(2)).thenReturn(true);when(r.getLong(3)).thenReturn(0L);
        assertDoesNotThrow(() -> PreparadorSchemaNovo.verificarDestinoVazio(c));
        verify(s,never()).execute(anyString());verify(s,never()).executeUpdate(anyString());
    }
}
