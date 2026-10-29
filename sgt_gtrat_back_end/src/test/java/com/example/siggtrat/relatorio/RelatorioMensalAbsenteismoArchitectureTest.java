package com.example.siggtrat.relatorio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioMensalAbsenteismoArchitectureTest {

    private String ler(String relativo) throws Exception {
        return Files.readString(Path.of("src/main/java", relativo));
    }

    @Test
    void mensalEAbsenteismoAceitamSomentePerfisGerenciaisAutorizados() throws Exception {
        String controller = ler("com/example/siggtrat/controlador/RelatorioControlador.java");
        String seguranca = ler("com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java");
        String acesso = ler("com/example/siggtrat/servico/AcessoOrganizacionalServico.java");

        assertTrue(controller.contains("PERFIL_GESTOR_ESTADUAL"));
        assertTrue(controller.contains("PERFIL_PRESIDENTE"));
        assertFalse(controller.contains("PERFIL_OPERACIONAL"));
        assertTrue(seguranca.contains("/api/relatorios/**"));
        assertTrue(acesso.contains("podeAcessarAlocacaoRelatorio"));
        assertTrue(acesso.contains("podeAcessarJornadaLotacao"));
    }

    @Test
    void filtrosDoRelatorioSaoDedicadosENaoDependemDaPaginacaoDeLotacoes() throws Exception {
        String controller = ler("com/example/siggtrat/controlador/RelatorioControlador.java");
        String consulta = ler("com/example/siggtrat/servico/RelatorioConsultaServico.java");

        assertTrue(controller.contains("/filtros"));
        assertTrue(consulta.contains("RelatorioFiltrosDTO"));
        assertTrue(consulta.contains("buscarTodosComTurnosPorIds"));
        assertTrue(consulta.contains("obterIdsLotacoes"));
        assertTrue(consulta.contains("obterIdsJornadas"));
    }

    @Test
    void processamentoGrandeAgrupaHistoricosEConsultaFrequenciasEmLotes() throws Exception {
        String consolidado = ler("com/example/siggtrat/servico/RelatorioFrequenciaConsolidadoServico.java");

        assertTrue(consolidado.contains("TAMANHO_LOTE_FREQUENCIAS = 1000"));
        assertTrue(consolidado.contains("alocacoesPorEmpregado"));
        assertTrue(consolidado.contains("carregarFrequenciasEmLotes"));
        assertTrue(consolidado.contains("ids.subList"));
        assertFalse(consolidado.contains("alocacoes.stream()\n                    .filter(item -> Objects.equals(item.getEmpregado().getId(), empregadoId))"));
    }

    @Test
    void absenteismoNaoMaterializaOMesInteiroEmMapasDiarios() throws Exception {
        String consulta = ler("com/example/siggtrat/servico/RelatorioConsultaServico.java");
        String consolidado = ler("com/example/siggtrat/servico/RelatorioFrequenciaConsolidadoServico.java");

        assertTrue(consulta.contains("gerarAbsenteismo("));
        assertTrue(consolidado.contains("AbsenteismoAcumulado"));
        assertTrue(consolidado.contains("não materializa o mapa diário"));
    }
    @Test
    void bancoNovoDeveCriarHistoricoComLotacaoTurnoCargoAntesDoPrimeiroRelatorio() throws Exception {
        String inicializador = ler("com/example/siggtrat/configuracao/InicializadorDados.java");

        assertTrue(inicializador.contains("historico.setLotacao(empregado.getLotacao())"));
        assertTrue(inicializador.contains("historico.setJornada(empregado.getJornada())"));
        assertTrue(inicializador.contains("historico.setCargo(empregado.getCargo())"));
        assertTrue(inicializador.contains("historico.setLotacaoSnapshotNome"));
        assertTrue(inicializador.contains("historico.setJornadaSnapshotDescricao"));
        assertTrue(inicializador.contains("preencherSnapshotJornada(historico, empregado.getJornada())"));
        assertTrue(inicializador.contains("historico.setJornadaSnapshotDiasTrabalho"));
        assertTrue(inicializador.contains("historico.setJornadaSnapshotMinutosDia"));
        assertTrue(inicializador.contains("jornada.getDias().size()"));
    }

    @Test
    void gestorEstadualPodeVisualizarCalendarioDeFeriadosEFolgas() throws Exception {
        String seguranca = ler("com/example/siggtrat/configuracao/ConfiguracaoSeguranca.java");
        int inicio = seguranca.indexOf("* CALENDÁRIO DE EVENTOS");
        int fim = seguranca.indexOf("* EXCEÇÕES / ADICIONAIS DE FREQUÊNCIA", inicio);
        assertTrue(inicio >= 0, "Bloco de calendário não encontrado na configuração de segurança.");
        assertTrue(fim > inicio, "Fim do bloco de calendário não encontrado na configuração de segurança.");
        String blocoCalendario = seguranca.substring(inicio, fim);

        assertTrue(blocoCalendario.contains("PERFIL_ADMINISTRADOR"));
        assertTrue(blocoCalendario.contains("PERFIL_GESTOR_LOCAL"));
        assertTrue(blocoCalendario.contains("PERFIL_OPERACIONAL"));
        assertTrue(blocoCalendario.contains("PERFIL_GESTOR_ESTADUAL"));
    }

    @Test
    void consolidacaoRjEhSomenteSeedENaoRemoveLotacaoComHistorico() throws Exception {
        String inicializador = ler("com/example/siggtrat/configuracao/InicializadorDados.java");
        String repositorioHistorico = ler("com/example/siggtrat/repositorio/HistoricoVinculoContratoRepositorio.java");

        int inicio = inicializador.indexOf("CommandLineRunner consolidarLotacoesRioDeJaneiro(");
        int anotacao = inicializador.lastIndexOf("@ConditionalOnProperty", inicio);
        String cabecalho = inicializador.substring(anotacao, inicio);

        assertTrue(cabecalho.contains("app.seed.terceirizados-enabled"));
        assertTrue(inicializador.contains("historicoVinculoContratoRepositorio.existsByLotacao_Id"));
        assertTrue(repositorioHistorico.contains("boolean existsByLotacao_Id(Long idLotacao)"));
    }

}
