package com.example.siggtrat;

import com.example.siggtrat.entidade.Empregado;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class EmpregadoMatriculaOptionalConfigurationTest {

    @Test
    void matriculaDoEmpregadoDeveSerOpcionalEUnicaQuandoInformada() throws Exception {
        Field campo = Empregado.class.getDeclaredField("matricula");
        Column coluna = campo.getAnnotation(Column.class);

        assertNotNull(coluna);
        assertTrue(coluna.nullable(), "A matrícula deve aceitar null no banco.");
        assertTrue(coluna.unique(), "A matrícula informada deve continuar única.");
        assertEquals(30, coluna.length(), "A coluna deve aceitar até 30 caracteres.");
        assertNull(campo.getAnnotation(NotBlank.class), "A matrícula não pode ter @NotBlank.");

        Size limite = campo.getAnnotation(Size.class);
        assertNotNull(limite, "A entidade deve validar o limite antes da persistência.");
        assertEquals(30, limite.max(), "O limite da matrícula do empregado deve ser 30.");
    }
}
