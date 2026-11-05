package com.example.siggtrat.painel;

import com.example.siggtrat.dto.PainelEfetivoDTO;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import com.example.siggtrat.servico.PainelServico;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PainelPeriodosRequisitosTest {
    @ParameterizedTest
    @CsvSource({"DIARIO,2026-10-28,1", "SEMANAL,2026-10-22,7", "QUINZENAL,2026-10-14,15",
            "MENSAL,2026-10-01,28", "SEMESTRAL,2026-07-01,4", "ANUAL,2026-01-01,10"})
    void periodosEComparacaoAnualPreservamRecortes(String periodo, LocalDate inicio, int pontosEsperados) {
        var repo = mock(PainelEfetivoRepositorio.class);
        var servico = spy(new PainelServico(null,null,null,null,null,repo,null,null,null));
        doReturn(new PainelServico.PainelCalculoContexto("LOCAL","Gestão",10L,20L,true,true,true,true,true))
                .when(servico).contextoAtual();
        when(repo.obterSerie(any(),any(),eq(1L),eq(2L),eq(3L),eq(4L),eq(20L),eq(10L))).thenAnswer(i -> {
            LocalDate desde=i.getArgument(0), ate=i.getArgument(1);
            List<PainelEfetivoDTO.PontoDiario> pontos=new ArrayList<>();
            for(LocalDate d=desde;!d.isAfter(ate);d=d.plusDays(1)) {
                long previsto=d.getYear()==2026?10:8;
                pontos.add(new PainelEfetivoDTO.PontoDiario(d,previsto,6,5,4,1,0,1,previsto-6,0,83.33,40));
            }
            return pontos;
        });
        LocalDate fim=LocalDate.of(2026,10,28);
        var resposta=servico.obterIndicadorEfetivo(fim,periodo,1L,2L,3L,4L,true);
        assertEquals(inicio,resposta.inicio());
        assertEquals(pontosEsperados,resposta.serieAtual().size());
        assertEquals(pontosEsperados,resposta.serieAnoAnterior().size());
        assertEquals(10,resposta.resumoAtual().efetivoPrevisto());
        assertEquals(8,resposta.resumoAnoAnterior().efetivoPrevisto());
        verify(repo).obterSerie(inicio,fim,1L,2L,3L,4L,20L,10L);
        verify(repo).obterSerie(inicio.minusYears(1),fim.minusYears(1),1L,2L,3L,4L,20L,10L);
    }
}
