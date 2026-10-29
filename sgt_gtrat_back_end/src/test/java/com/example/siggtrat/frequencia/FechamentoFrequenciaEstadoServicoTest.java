package com.example.siggtrat.frequencia;

import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.entidade.FechamentoFrequencia;
import com.example.siggtrat.entidade.FechamentoFrequenciaStatus;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.repositorio.FechamentoFrequenciaRepositorio;
import com.example.siggtrat.servico.FechamentoFrequenciaEstadoServico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FechamentoFrequenciaEstadoServicoTest {

    @Mock
    private FechamentoFrequenciaRepositorio repositorio;
    private FechamentoFrequenciaEstadoServico servico;

    @BeforeEach
    void setUp() {
        servico = new FechamentoFrequenciaEstadoServico(repositorio);
    }

    @Test
    void reivindicaSomenteFechamentoPendente() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PENDENTE);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));

        var descritor = servico.reivindicar(1L).orElseThrow();

        assertEquals(FechamentoFrequenciaStatus.PROCESSANDO, f.getStatus());
        assertNotNull(f.getIniciadoEm());
        assertNotNull(f.getHeartbeatEm());
        assertEquals(10L, descritor.lotacaoId());
        assertEquals(20L, descritor.jornadaId());
        verify(repositorio).save(f);
    }

    @Test
    void naoReivindicaFechamentoJaProcessando() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));
        assertTrue(servico.reivindicar(1L).isEmpty());
        verify(repositorio, never()).save(any());
    }

    @Test
    void cancelamentoDeRegistroInexistenteEhTratadoComoSolicitado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        assertTrue(servico.cancelamentoSolicitado(99L));
    }

    @Test
    void defineTotalPrevistoUmaUnicaVezParaManterProgressoMonotonico() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));
        servico.definirTotalPrevisto(1L, 100_000);
        assertEquals(100_000, f.getTotalPrevisto());

        servico.definirTotalPrevisto(1L, 80_000);
        assertEquals(100_000, f.getTotalPrevisto());
    }

    @Test
    void totalPrevistoZeroEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> servico.definirTotalPrevisto(1L, 0));
    }

    @Test
    void concluirAjustaTotalPrevistoAoTotalReal() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        f.setTotalPrevisto(100);
        f.setTotalProcessado(97);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));

        servico.concluir(1L);

        assertEquals(FechamentoFrequenciaStatus.CONCLUIDO, f.getStatus());
        assertEquals(97, f.getTotalPrevisto());
        assertNotNull(f.getConcluidoEm());
        assertNull(f.getDetalheErro());
    }

    @Test
    void cancelarNaoAlteraFechamentoConcluido() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.CONCLUIDO);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));
        servico.cancelar(1L);
        verify(repositorio, never()).save(any());
    }

    @Test
    void cancelarMarcaEstadoTerminal() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));
        servico.cancelar(1L);
        assertEquals(FechamentoFrequenciaStatus.CANCELADO, f.getStatus());
        assertNotNull(f.getConcluidoEm());
    }

    @Test
    void falhaEhPersistidaComCadeiaDeCausas() {
        FechamentoFrequencia f = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        when(repositorio.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(f));
        RuntimeException erro = new RuntimeException("externo", new IllegalStateException("interno"));

        servico.falhar(1L, erro);

        assertEquals(FechamentoFrequenciaStatus.ERRO, f.getStatus());
        assertTrue(f.getDetalheErro().contains("RuntimeException"));
        assertTrue(f.getDetalheErro().contains("IllegalStateException"));
        assertNotNull(f.getConcluidoEm());
    }

    @Test
    void reenfileiraTrabalhosSemHeartbeat() {
        FechamentoFrequencia a = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        FechamentoFrequencia b = fechamento(FechamentoFrequenciaStatus.PROCESSANDO);
        b.setId(2L);
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        when(repositorio.buscarTravados(FechamentoFrequenciaStatus.PROCESSANDO, limite)).thenReturn(List.of(a, b));

        assertEquals(List.of(1L, 2L), servico.reenfileirarTravados(limite));
        assertEquals(FechamentoFrequenciaStatus.PENDENTE, a.getStatus());
        assertNull(a.getHeartbeatEm());
        verify(repositorio, times(2)).save(any());
    }

    @Test
    void listaSomenteCinquentaPendentesNaOrdemDoRepositorio() {
        FechamentoFrequencia a = fechamento(FechamentoFrequenciaStatus.PENDENTE);
        FechamentoFrequencia b = fechamento(FechamentoFrequenciaStatus.PENDENTE);
        b.setId(2L);
        when(repositorio.findTop50ByStatusOrderByCriadoEmAsc(FechamentoFrequenciaStatus.PENDENTE))
                .thenReturn(List.of(a, b));
        assertEquals(List.of(1L, 2L), servico.pendentes());
    }

    private FechamentoFrequencia fechamento(FechamentoFrequenciaStatus status) {
        Lotacao lotacao = new Lotacao();
        lotacao.setId(10L);
        lotacao.setNome("CTE BENFICA");
        Jornada jornada = new Jornada();
        jornada.setId(20L);
        jornada.setDescricao("TURNO A");
        UsuarioAplicacao user = new UsuarioAplicacao();
        user.setId(30L);

        FechamentoFrequencia f = new FechamentoFrequencia();
        f.setId(1L);
        f.setStatus(status);
        f.setDataReferencia(LocalDate.of(2026, 8, 6));
        f.setLotacao(lotacao);
        f.setJornada(jornada);
        f.setUsuarioCriador(user);
        f.setContratoIds("1,2");
        f.setTentativa(1);
        return f;
    }
}
