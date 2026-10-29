package com.example.siggtrat.frequencia;

import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.FechamentoFrequencia;
import com.example.siggtrat.entidade.FechamentoFrequenciaStatus;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.servico.FechamentoFrequenciaServico;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FechamentoFrequenciaServicoStaticTest {

    @Test
    void normalizaIdsRemoveNulosInvalidosDuplicadosEOrdena() {
        assertEquals(List.of(1L, 2L, 3L),
                FechamentoFrequenciaServico.normalizarIds(List.of(3L, 2L, 3L, -1L, 0L, 1L)));
        assertEquals(List.of(), FechamentoFrequenciaServico.normalizarIds(null));
    }

    @Test
    void serializaEDesserializaIdsDeFormaCanonica() {
        assertEquals("1,2,3", FechamentoFrequenciaServico.serializarIds(List.of(3L, 1L, 2L, 2L)));
        assertEquals(List.of(1L, 2L, 3L), FechamentoFrequenciaServico.desserializarIds("3, 1,2,2"));
        assertEquals(List.of(), FechamentoFrequenciaServico.desserializarIds("  "));
    }

    @Test
    void idsParaQueryUsaSentinelaQuandoFiltroEstaVazio() {
        assertEquals(List.of(-1L), FechamentoFrequenciaServico.idsParaConsulta(List.of()));
        assertEquals(List.of(7L), FechamentoFrequenciaServico.idsParaConsulta(List.of(7L)));
    }

    @Test
    void chaveDoEscopoEhDeterministicaEIndependenteDaOrdemDosContratos() {
        LocalDate data = LocalDate.of(2026, 8, 6);
        String a = FechamentoFrequenciaServico.montarChaveEscopo(data, 10L, 20L, List.of(3L, 1L, 2L));
        String b = FechamentoFrequenciaServico.montarChaveEscopo(data, 10L, 20L, List.of(2L, 3L, 1L));
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void chaveMudaQuandoEscopoMuda() {
        LocalDate data = LocalDate.of(2026, 8, 6);
        String base = FechamentoFrequenciaServico.montarChaveEscopo(data, 10L, 20L, List.of());
        assertNotEquals(base, FechamentoFrequenciaServico.montarChaveEscopo(data.plusDays(1), 10L, 20L, List.of()));
        assertNotEquals(base, FechamentoFrequenciaServico.montarChaveEscopo(data, 11L, 20L, List.of()));
        assertNotEquals(base, FechamentoFrequenciaServico.montarChaveEscopo(data, 10L, 21L, List.of()));
    }

    @Test
    void chaveRejeitaCamposObrigatoriosAusentes() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> FechamentoFrequenciaServico.montarChaveEscopo(null, 1L, 2L, List.of()));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void dtoCalculaPercentualParcialECompleto() {
        FechamentoFrequencia f = fechamentoBase();
        f.setTotalPrevisto(100);
        f.setTotalProcessado(37);
        FechamentoFrequenciaDTO parcial = FechamentoFrequenciaServico.paraDTO(f);
        assertEquals(37, parcial.percentual());

        f.setTotalProcessado(150);
        assertEquals(100, FechamentoFrequenciaServico.paraDTO(f).percentual());
    }

    @Test
    void dtoSemTotalPrevistoFicaEmZeroExcetoQuandoConcluido() {
        FechamentoFrequencia f = fechamentoBase();
        f.setTotalPrevisto(0);
        f.setTotalProcessado(0);
        assertEquals(0, FechamentoFrequenciaServico.paraDTO(f).percentual());
        f.setStatus(FechamentoFrequenciaStatus.CONCLUIDO);
        assertEquals(100, FechamentoFrequenciaServico.paraDTO(f).percentual());
    }

    private FechamentoFrequencia fechamentoBase() {
        Lotacao lotacao = new Lotacao();
        lotacao.setId(10L);
        lotacao.setNome("CTE BENFICA");
        Jornada jornada = new Jornada();
        jornada.setId(20L);
        jornada.setDescricao("TURNO A");
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(99L);

        FechamentoFrequencia f = new FechamentoFrequencia();
        f.setId(1L);
        f.setStatus(FechamentoFrequenciaStatus.PROCESSANDO);
        f.setDataReferencia(LocalDate.of(2026, 8, 6));
        f.setLotacao(lotacao);
        f.setJornada(jornada);
        f.setUsuarioCriador(user);
        f.setContratoIds("1,2");
        f.setCriadoEm(LocalDateTime.of(2026, 8, 6, 10, 0));
        f.setAtualizadoEm(LocalDateTime.of(2026, 8, 6, 10, 1));
        return f;
    }
}
