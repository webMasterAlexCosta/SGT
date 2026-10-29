package com.example.siggtrat.configuracao;

import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.HistoricoVinculoContrato;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;
import com.example.siggtrat.repositorio.ConfiguracaoSistemaRepositorio;
import com.example.siggtrat.utilitario.UtilitarioCpf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;

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
    void deveCarregarQuantidadeEsperadaDeRegistrosEstruturais() throws Exception {
        Method method = InicializadorDados.class.getDeclaredMethod("carregarRegistrosTerceirizados");
        method.setAccessible(true);

        List<?> registros = (List<?>) method.invoke(null);

        assertEquals(13_405, registros.size());
    }

    @Test
    void cargaNaoDeveConterMarcadoresDeErroDePlanilha() throws Exception {
        try (var input = getClass().getClassLoader()
                .getResourceAsStream("db/seed/terceirizados.tsv")) {
            assertTrue(input != null, "Arquivo de carga não encontrado.");
            String conteudo = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            for (String marcador : List.of("#VALOR!", "#N/A", "#REF!", "#DIV/0!", "#NOME?")) {
                assertTrue(
                        !conteudo.toUpperCase().contains(marcador),
                        () -> "A carga contém marcador inválido de planilha: " + marcador
                );
            }
            assertTrue(conteudo.contains("\tAC IPIAU\tIPIAU\t"));
        }
    }

    @Test
    void rioDeJaneiroDeveUsarSomenteCteBenficaECtceCidadeNova() throws Exception {
        Method method = InicializadorDados.class.getDeclaredMethod("carregarRegistrosTerceirizados");
        method.setAccessible(true);

        List<?> registros = (List<?>) method.invoke(null);
        Set<String> lotacoesRj = new HashSet<>();
        int quantidadeRj = 0;

        for (Object registro : registros) {
            Method uf = registro.getClass().getDeclaredMethod("uf");
            Method lotacao = registro.getClass().getDeclaredMethod("lotacao");
            Method municipio = registro.getClass().getDeclaredMethod("municipio");
            uf.setAccessible(true);
            lotacao.setAccessible(true);
            municipio.setAccessible(true);

            if (!"RJ".equals(uf.invoke(registro))) {
                continue;
            }

            quantidadeRj++;
            lotacoesRj.add((String) lotacao.invoke(registro));
            assertEquals("RIO DE JANEIRO", municipio.invoke(registro));
        }

        assertTrue(quantidadeRj > 0);
        assertEquals(Set.of("CTE BENFICA", "CTCE CIDADE NOVA"), lotacoesRj);
    }

    @Test
    void deveGerarNomesECpfsFicticiosUnicosEValidosNaEscalaDeCinquentaVezes() throws Exception {
        Method cpfMethod = InicializadorDados.class.getDeclaredMethod(
                "gerarCpfFicticio",
                int.class
        );
        Method nomeMethod = InicializadorDados.class.getDeclaredMethod("gerarNomeFicticio", int.class);
        cpfMethod.setAccessible(true);
        nomeMethod.setAccessible(true);

        int total = 13_405 * 50;
        Set<String> cpfs = new HashSet<>(total * 2);

        for (int indice = 1; indice <= total; indice++) {
            String cpf = (String) cpfMethod.invoke(null, indice);
            String nome = (String) nomeMethod.invoke(null, indice);

            assertTrue(UtilitarioCpf.ehValido(cpf), () -> "CPF inválido: " + cpf);
            assertTrue(cpfs.add(cpf), () -> "CPF repetido: " + cpf);
            assertTrue(nome.contains(" FICTICIO "));
            assertTrue(nome.endsWith(String.format("%05d", indice)));
        }

        assertEquals(total, cpfs.size());
    }

    @Test
    void planoAmpliadoDeveManterContratosEConcentrarColaboradoresEmSpERj() throws Exception {
        Method carregar = InicializadorDados.class.getDeclaredMethod("carregarRegistrosTerceirizados");
        carregar.setAccessible(true);
        List<?> registrosBase = (List<?>) carregar.invoke(null);

        Method criarPlano = InicializadorDados.class.getDeclaredMethod(
                "criarPlanoCargaTerceirizados",
                List.class,
                int.class,
                int.class,
                int.class
        );
        criarPlano.setAccessible(true);
        Object plano = criarPlano.invoke(null, registrosBase, 50, 55, 30);

        Method totalRegistros = plano.getClass().getDeclaredMethod("totalRegistros");
        Method registroParaIndice = plano.getClass().getDeclaredMethod("registroParaIndice", int.class);
        totalRegistros.setAccessible(true);
        registroParaIndice.setAccessible(true);

        int total = (Integer) totalRegistros.invoke(plano);
        assertEquals(670_250, total);

        Set<String> contratosBase = new HashSet<>();
        for (Object registro : registrosBase) {
            contratosBase.add(chaveContrato(registro));
        }

        int quantidadeSp = 0;
        int quantidadeRj = 0;
        Set<String> contratosAmpliados = new HashSet<>();
        for (int indice = 1; indice <= total; indice++) {
            Object registro = registroParaIndice.invoke(plano, indice);
            Method uf = registro.getClass().getDeclaredMethod("uf");
            uf.setAccessible(true);
            String valorUf = (String) uf.invoke(registro);
            if ("SP".equals(valorUf)) quantidadeSp++;
            if ("RJ".equals(valorUf)) quantidadeRj++;
            contratosAmpliados.add(chaveContrato(registro));
        }

        assertTrue(quantidadeSp > 350_000, "SP deve concentrar a maior parte da carga.");
        assertTrue(quantidadeRj > 190_000, "RJ deve receber uma parcela elevada da carga.");
        assertTrue(quantidadeSp + quantidadeRj > total * 0.80);
        assertEquals(contratosBase, contratosAmpliados);
    }


    @Test
    void cargaRjDeveConterCteBenficaComTurno1A() throws Exception {
        Method carregar = InicializadorDados.class.getDeclaredMethod("carregarRegistrosTerceirizados");
        carregar.setAccessible(true);
        List<?> registros = (List<?>) carregar.invoke(null);

        int encontrados = 0;
        for (int indice = 1; indice <= registros.size(); indice++) {
            Object registro = registros.get(indice - 1);
            Method uf = registro.getClass().getDeclaredMethod("uf");
            Method lotacao = registro.getClass().getDeclaredMethod("lotacao");
            uf.setAccessible(true);
            lotacao.setAccessible(true);

            boolean turno1A = Math.floorMod(indice - 1, 9) == 0;
            if ("RJ".equals(uf.invoke(registro))
                    && "CTE BENFICA".equals(lotacao.invoke(registro))
                    && turno1A) {
                encontrados++;
            }
        }

        assertEquals(59, encontrados,
                "A carga de desenvolvimento deve manter dados para CTE BENFICA + TURNO 1A.");
    }

    @Test
    void snapshotDaJornadaDoHistoricoDeveGuardarDiasEMinutos() throws Exception {
        Jornada jornada = new Jornada();
        jornada.setDescricao("TURNO TESTE");

        JornadaDiaSemana segunda = new JornadaDiaSemana();
        segunda.setJornada(jornada);
        segunda.setDiaSemana(DayOfWeek.MONDAY);
        segunda.setDiaTrabalho(true);
        segunda.setEntrada1("06:00");
        segunda.setSaida1("10:00");
        segunda.setEntrada2("11:00");
        segunda.setSaida2("15:00");
        jornada.getDias().add(segunda);

        HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
        Method snapshot = InicializadorDados.class.getDeclaredMethod(
                "preencherSnapshotJornada", HistoricoVinculoContrato.class, Jornada.class);
        snapshot.setAccessible(true);
        snapshot.invoke(null, historico, jornada);

        assertEquals("MONDAY", historico.getJornadaSnapshotDiasTrabalho());
        assertEquals("MONDAY:480", historico.getJornadaSnapshotMinutosDia());
    }

    private static String chaveContrato(Object registro) throws Exception {
        Method uf = registro.getClass().getDeclaredMethod("uf");
        Method empresa = registro.getClass().getDeclaredMethod("empresa");
        uf.setAccessible(true);
        empresa.setAccessible(true);
        return uf.invoke(registro) + "|" + empresa.invoke(registro);
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
    void deveUsarAMaiorDataEntreAdmissaoEInicioDoContratoNoHistorico() throws Exception {
        Method method = InicializadorDados.class.getDeclaredMethod(
                "calcularInicioVinculo",
                Empregado.class,
                Contrato.class,
                ZoneId.class
        );
        method.setAccessible(true);

        Empregado empregado = new Empregado();
        empregado.setDataAdmissao(LocalDate.of(2026, 2, 15));

        Contrato contrato = new Contrato();
        contrato.setDataInicio(LocalDate.of(2026, 1, 1));

        ZoneId zoneId = ZoneId.of("America/Sao_Paulo");
        ZonedDateTime inicio = (ZonedDateTime) method.invoke(null, empregado, contrato, zoneId);

        assertEquals(LocalDate.of(2026, 2, 15), inicio.toLocalDate());
        assertEquals(zoneId, inicio.getZone());
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
