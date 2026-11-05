package com.example.siggtrat.configuracao;

import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataInitializerSeedTest {

    @Test
    void turnoTresDeveEncerrarAsSeisParaGerarOitoHorasNoturnasReduzidas() throws Exception {
        Field field = InicializadorDados.class.getDeclaredField("DEFINICOES_JORNADAS");
        field.setAccessible(true);
        List<?> definicoes = (List<?>) field.get(null);
        int turnosNoturnosValidados = 0;

        for (Object definicao : definicoes) {
            Method descricao = definicao.getClass().getDeclaredMethod("descricao");
            Method entrada1 = definicao.getClass().getDeclaredMethod("entrada1");
            Method saida1 = definicao.getClass().getDeclaredMethod("saida1");
            Method entrada2 = definicao.getClass().getDeclaredMethod("entrada2");
            Method saida2 = definicao.getClass().getDeclaredMethod("saida2");
            descricao.setAccessible(true);
            entrada1.setAccessible(true);
            saida1.setAccessible(true);
            entrada2.setAccessible(true);
            saida2.setAccessible(true);

            if (!String.valueOf(descricao.invoke(definicao)).startsWith("TURNO 3")) {
                continue;
            }

            turnosNoturnosValidados++;
            assertEquals("22:00", entrada1.invoke(definicao));
            assertEquals("02:00", saida1.invoke(definicao));
            assertEquals("03:00", entrada2.invoke(definicao));
            assertEquals("06:00", saida2.invoke(definicao));
        }

        assertEquals(3, turnosNoturnosValidados);
    }

    @Test
    void deveDeduplicarChavesIgnorandoAcentosComoOMySql() throws Exception {
        Method method = InicializadorDados.class.getDeclaredMethod("normalizarChave", String.class);
        method.setAccessible(true);

        String semAcento = (String) method.invoke(null, "AUXILIAR OPERACIONAL DE DISTRIBUICAO");
        String comAcento = (String) method.invoke(null, "AUXILIAR OPERACIONAL DE DISTRIBUIÇÃO");
        String liderSemAcento = (String) method.invoke(null, "VIGILANTE LIDER");
        String liderComAcento = (String) method.invoke(null, "VIGILANTE LÍDER");

        assertEquals(semAcento, comAcento);
        assertEquals(liderSemAcento, liderComAcento);
    }

    @Test
    void deveUsarNovosNomesDosGestoresEstadualELocal() throws Exception {
        Method gestorEstadual = InicializadorDados.class.getDeclaredMethod(
                "nomeUsuarioGestorEstadual", String.class
        );
        Method gestaoLocal = InicializadorDados.class.getDeclaredMethod(
                "nomeUsuarioGestaoLocal", String.class, String.class
        );
        gestorEstadual.setAccessible(true);
        gestaoLocal.setAccessible(true);

        assertEquals("gestorEstadual", gestorEstadual.invoke(null, "RJ"));
        assertEquals("gestorLocalGtratRJ", gestaoLocal.invoke(null, "GTRAT", "RJ"));
        assertEquals("gestorLocalGedisRJ", gestaoLocal.invoke(null, "GEDIS", "RJ"));
        assertEquals("gestorEstadualSP", gestorEstadual.invoke(null, "sp"));
        assertEquals("gestorLocalGetraSP", gestaoLocal.invoke(null, "GETRA", "sp"));
    }

    @Test
    void marcadorImpedeReexecucaoDoSeedSemExigirAlteracaoNoEnv() throws Exception {
        ConfiguracaoSistemaRepositorio repositorio = mock(ConfiguracaoSistemaRepositorio.class);
        when(repositorio.existsById("SEED_TESTE")).thenReturn(true);

        Method metodo = InicializadorDados.class.getDeclaredMethod(
                "deveIgnorarCargaConcluida",
                ConfiguracaoSistemaRepositorio.class,
                String.class,
                String.class,
                boolean.class
        );
        metodo.setAccessible(true);

        assertTrue((Boolean) metodo.invoke(
                null, repositorio, "SEED_TESTE", "Carga de teste", true));
        assertFalse((Boolean) metodo.invoke(
                null, repositorio, "SEED_TESTE", "Carga de teste", false));
    }

}
