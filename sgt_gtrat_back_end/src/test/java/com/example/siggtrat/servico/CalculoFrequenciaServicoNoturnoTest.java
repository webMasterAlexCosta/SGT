package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.RegraAdicional;
import com.example.siggtrat.repositorio.RegraAdicionalRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculoFrequenciaServicoNoturnoTest {

    @Mock
    private ConfiguracaoServico configuracaoServico;
    @Mock
    private RegraAdicionalRepositorio regraAdicionalRepositorio;

    private CalculoFrequenciaServico servico;

    @BeforeEach
    void setUp() {
        when(configuracaoServico.obterHoraInicioNoturno()).thenReturn(LocalTime.of(22, 0));
        when(configuracaoServico.obterHoraFimNoturno()).thenReturn(LocalTime.of(5, 0));
        when(configuracaoServico.obterMinutosPorHoraNoturna()).thenReturn(52.5);
        when(regraAdicionalRepositorio.findByJornada_IdOrderByOrdemAsc(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(java.util.List.of());
        servico = new CalculoFrequenciaServico(
                configuracaoServico,
                regraAdicionalRepositorio,
                new CalculadoraJornadaNoturnaServico(configuracaoServico)
        );
    }

    @Test
    void presencaAutomaticaDas22As06ComIntervaloGeraOitoHorasFaturaveis() {
        Frequencia frequencia = frequenciaNoturnaPrevista();
        frequencia.setStatus("PRESENCA");

        servico.processarCalculos(frequencia);

        assertEquals("PRESENCA", frequencia.getStatus());
        assertEquals("08:00", frequencia.getHorasTrabalhadas());
        assertEquals(8.0, frequencia.getHorasNoturnas());
    }

    @Test
    void batidasDas22As06ConsideramProrrogacaoApos05ComoHoraNoturnaReduzida() {
        Frequencia frequencia = frequenciaNoturnaPrevista();
        frequencia.setStatus("PRESENCA");
        frequencia.setEntrada1("22:00");
        frequencia.setSaida1("01:00");
        frequencia.setEntrada2("02:00");
        frequencia.setSaida2("06:00");

        servico.processarCalculos(frequencia);

        assertEquals("PRESENCA", frequencia.getStatus());
        assertEquals("08:00", frequencia.getHorasTrabalhadas());
        assertEquals(8.0, frequencia.getHorasNoturnas());
    }

    @Test
    void batidasDas22As05GeramSeteHorasFisicasEOitoHorasNoturnasReduzidas() {
        Frequencia frequencia = frequenciaNoturnaPrevista();
        frequencia.setJornadaSnapshotEntrada1("22:00");
        frequencia.setJornadaSnapshotSaida1("05:00");
        frequencia.setJornadaSnapshotEntrada2(null);
        frequencia.setJornadaSnapshotSaida2(null);
        frequencia.setStatus("PRESENCA");
        frequencia.setEntrada1("22:00");
        frequencia.setSaida1("05:00");

        servico.processarCalculos(frequencia);

        assertEquals(420, frequencia.getMinutosFisicos());
        assertEquals(420, frequencia.getMinutosNoturnosReais());
        assertEquals(480, frequencia.getMinutosPagaveis());
        assertEquals("08:00", frequencia.getHorasTrabalhadas());
        assertEquals(8.0, frequencia.getHorasNoturnas());
    }

    @Test
    void horaFictaNaoTransformaJornadaFisicamenteParcialEmPresencaIntegral() {
        Frequencia frequencia = frequenciaNoturnaPrevista();
        frequencia.setStatus("PRESENCA");
        frequencia.setEntrada1("22:00");
        frequencia.setSaida1("04:00");

        servico.processarCalculos(frequencia);

        assertEquals("PRESENCA_PARCIAL", frequencia.getStatus());
        assertEquals("06:51", frequencia.getHorasTrabalhadas());
        assertEquals(6.857143, frequencia.getHorasNoturnas());
    }

    @Test
    void regraAdicionalAtivaCompõeMemoriaPagavelSemAlterarTempoFisico() {
        RegraAdicional regra = new RegraAdicional();
        regra.setAtivo(true);
        regra.setTipo("ADICIONAL_CONTRATUAL");
        regra.setPercentual(new BigDecimal("20"));
        when(regraAdicionalRepositorio.findByJornada_IdOrderByOrdemAsc(10L))
                .thenReturn(java.util.List.of(regra));
        Frequencia frequencia = frequenciaNoturnaPrevista();
        frequencia.setEntrada1("22:00");
        frequencia.setSaida1("01:00");
        frequencia.setEntrada2("02:00");
        frequencia.setSaida2("06:00");

        servico.processarCalculos(frequencia);

        assertEquals(420, frequencia.getMinutosFisicos());
        assertEquals(84, frequencia.getMinutosAdicionais());
        assertEquals(564, frequencia.getMinutosPagaveis());
        assertEquals("09:24", frequencia.getHorasTrabalhadas());
    }

    private Frequencia frequenciaNoturnaPrevista() {
        Frequencia frequencia = new Frequencia();
        frequencia.setJornadaSnapshotId(10L);
        frequencia.setJornadaSnapshotDiaTrabalho(true);
        frequencia.setJornadaSnapshotEntrada1("22:00");
        frequencia.setJornadaSnapshotSaida1("01:00");
        frequencia.setJornadaSnapshotEntrada2("02:00");
        frequencia.setJornadaSnapshotSaida2("06:00");
        frequencia.setEmpregado(new Empregado());
        frequencia.setDataReferencia(LocalDate.of(2026, 8, 10));
        return frequencia;
    }
}
