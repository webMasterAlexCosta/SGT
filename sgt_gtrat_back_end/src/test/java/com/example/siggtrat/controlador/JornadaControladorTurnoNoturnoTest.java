package com.example.siggtrat.controlador;

import com.example.siggtrat.configuracao.propriedades.PropriedadesEmpregado;
import com.example.siggtrat.dto.JornadaDiaSemanaDTO;
import com.example.siggtrat.servico.JornadaServico;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JornadaControladorTurnoNoturnoTest {

    private final JornadaServico servico = new JornadaServico(
            null, null, null, null, null, null, null, new PropriedadesEmpregado(480), null, null);

    @Test
    void aceitaTurnoComDoisPeriodosQueAtravessamAMeiaNoite() {
        List<JornadaDiaSemanaDTO> dias = diasComTurnoNoturno("22:00", "01:00", "02:00", "06:00");
        assertDoesNotThrow(() -> invocarValidacao(dias));
    }

    @Test
    void aceitaTurnoNoturnoEmUmUnicoPeriodo() {
        List<JornadaDiaSemanaDTO> dias = diasComTurnoNoturno("22:00", "06:00", null, null);
        assertDoesNotThrow(() -> invocarValidacao(dias));
    }

    @Test
    void continuaRejeitandoIntervaloComDuracaoZero() {
        List<JornadaDiaSemanaDTO> dias = diasComTurnoNoturno("22:00", "22:00", null, null);

        InvocationTargetException erro = assertThrows(
                InvocationTargetException.class, () -> invocarValidacao(dias));
        IllegalArgumentException causa = assertInstanceOf(IllegalArgumentException.class, erro.getCause());
        assertEquals("O intervalo 1 deve possuir duração maior que zero em MONDAY.", causa.getMessage());
    }

    private void invocarValidacao(List<JornadaDiaSemanaDTO> dias) throws Exception {
        Method metodo = JornadaServico.class.getDeclaredMethod("validarSeteDiasEIntervalos", List.class);
        metodo.setAccessible(true);
        metodo.invoke(servico, dias);
    }

    private List<JornadaDiaSemanaDTO> diasComTurnoNoturno(
            String entrada1, String saida1, String entrada2, String saida2) {
        return Arrays.stream(DayOfWeek.values())
                .map(dia -> dia == DayOfWeek.MONDAY
                        ? new JornadaDiaSemanaDTO(null, dia, true, entrada1, saida1, entrada2, saida2)
                        : new JornadaDiaSemanaDTO(null, dia, false, null, null, null, null))
                .toList();
    }
}
