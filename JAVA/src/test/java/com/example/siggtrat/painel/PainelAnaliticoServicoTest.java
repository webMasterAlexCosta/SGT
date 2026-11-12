package com.example.siggtrat.painel;

import com.example.siggtrat.dto.PainelAnaliticoDTO;
import com.example.siggtrat.repositorio.PainelAnaliticoRepositorio;
import com.example.siggtrat.repositorio.PainelEfetivoRepositorio;
import com.example.siggtrat.servico.PainelAnaliticoServico;
import com.example.siggtrat.servico.PainelServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PainelAnaliticoServicoTest {
    final PainelAnaliticoRepositorio repo = mock(PainelAnaliticoRepositorio.class);
    final PainelEfetivoRepositorio efetivo = mock(PainelEfetivoRepositorio.class);
    final PainelServico painel = mock(PainelServico.class);
    final PainelAnaliticoServico servico = new PainelAnaliticoServico(repo, efetivo, painel);
    final LocalDate ref = LocalDate.of(2026,11,10);

    @BeforeEach void contexto() {
        when(painel.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("Gestão Local","Unidade",10L,20L,true,true,true,true,true));
        when(repo.unidades(any(), any(), any())).thenReturn(List.of(new PainelAnaliticoDTO.Opcao(31L,"Lotação")));
        when(repo.totais(any(),any(),any())).thenReturn(new PainelAnaliticoDTO.Totais(8,6,2));
        when(repo.tempestividade(any(),any(),any())).thenReturn(new PainelAnaliticoDTO.Tempestividade(6,1,1,2));
    }
    @Test void cincoPeriodosPreservamClassificacaoEBasesSemInventarDados() {
        for (String tipo : List.of("DIARIO","SEMANAL","MENSAL","TRIMESTRAL","ANUAL")) {
            var d = servico.obter(tipo,ref,null,null,null,false,0);
            assertEquals(8,d.resumoPeriodo().registros());
            assertEquals(tipo,d.intervalo().granularidade());
            assertNull(d.comparativo());
            assertNull(d.referenciaDiaria().coberturaAlocados());
            assertEquals(8,d.indicadores().tempestividade().dentro()+d.indicadores().tempestividade().fora()+d.indicadores().tempestividade().indeterminado());
            assertTrue(d.metas().stream().allMatch(x->Double.valueOf(100).equals(x.meta())));
            assertNull(d.metas().get(0).valor());
        }
    }
    @Test void filtroForaDoEscopoFalhaAntesDeConsultarTotais() {
        var erro=assertThrows(ResponseStatusException.class,()->servico.obter("MENSAL",ref,null,"LOTACAO",999L,true,0));
        assertEquals(403,erro.getStatusCode().value());
        verify(repo,never()).totais(any(),any(),any());
    }
    @Test void presidenteNaoPodePedirLotacao() {
        when(painel.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("NACIONAL","Brasil",null,null,false,true,false,false,true));
        var erro=assertThrows(ResponseStatusException.class,()->servico.obter("MENSAL",ref,null,"LOTACAO",31L,true,0));
        assertEquals(403,erro.getStatusCode().value());
    }
    @Test void contratoForaDoEscopoNaoViraListaVazia() {
        var erro=assertThrows(ResponseStatusException.class,()->servico.obter("ANUAL",ref,999L,null,null,true,0));
        assertEquals(403,erro.getStatusCode().value());
    }
    @Test void futuroEPeriodoInvalidoRetornam400() {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->servico.obter("TRIMESTRAL",LocalDate.now().plusYears(1),null,null,null,true,0)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->servico.obter("90",ref,null,null,null,true,0)).getStatusCode().value());
    }

    @Test void comparacaoSemBaseAnteriorNaoInventaPercentual() {
        when(repo.totais(any(),any(),any())).thenReturn(new PainelAnaliticoDTO.Totais(8,6,2),PainelAnaliticoDTO.Totais.vazio());
        var d=servico.obter("MENSAL",ref,null,null,null,true,0);
        assertEquals(8,d.variacoes().registros().diferenca());
        assertNull(d.variacoes().registros().variacaoPercentual());
        assertEquals(LocalDate.of(2026,10,10),d.intervalo().anteriorFim());
    }

    @Test void registrosDeOrigemRecusamPresidenteEIntervaloForaDaConsulta() {
        when(painel.contextoAtual()).thenReturn(new PainelServico.PainelCalculoContexto("NACIONAL","Brasil",null,null,false,true,false,false,true));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->servico.registros(ref,ref,ref,null,null,null,"registros",0)).getStatusCode().value());
        contexto();
        assertEquals(400,assertThrows(ResponseStatusException.class,()->servico.registros(ref,ref,ref.plusDays(1),null,null,null,"registros",0)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->servico.registros(ref,ref.minusYears(2),ref,null,null,null,"registros",0)).getStatusCode().value());
    }
}
