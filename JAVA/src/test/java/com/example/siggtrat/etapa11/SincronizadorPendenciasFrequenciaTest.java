package com.example.siggtrat.etapa11;

import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.servico.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SincronizadorPendenciasFrequenciaTest {
    @Test
    void materializaPendenciaLegadaEmLoteEConfereEstadoAtualSobLock() {
        var frequencias = mock(FrequenciaRepositorio.class);
        var contratos = mock(ContratoRepositorio.class);
        var pendencias = mock(PendenciaAdministrativaServico.class);
        var transacoes = mock(PlatformTransactionManager.class);
        when(transacoes.getTransaction(any())).thenAnswer(inv -> {
            TransactionDefinition definicao = inv.getArgument(0);
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definicao.getPropagationBehavior());
            return new SimpleTransactionStatus();
        });
        when(frequencias.buscarIdsSujeitasRevisaoApos(eq(0L), any())).thenAnswer(inv -> {
            Pageable pagina = inv.getArgument(1);
            assertEquals(500, pagina.getPageSize());
            return List.of(1L, 2L, 3L);
        });
        when(frequencias.buscarContratoSnapshotId(anyLong())).thenReturn(Optional.of(10L));
        var contrato = new Contrato();
        contrato.setId(10L);
        when(contratos.buscarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(contrato));
        var pendente = frequencia(1L, 10L, EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        var resolvida = frequencia(2L, 10L, EstadoAuditoriaFrequencia.RETIFICADA_GESTOR);
        var alteradaConcorrentemente = frequencia(3L, 20L, EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        when(frequencias.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(pendente));
        when(frequencias.buscarPorIdParaAtualizacao(2L)).thenReturn(Optional.of(resolvida));
        when(frequencias.buscarPorIdParaAtualizacao(3L)).thenReturn(Optional.of(alteradaConcorrentemente));

        new SincronizadorPendenciasFrequenciaServico(frequencias, contratos, pendencias, transacoes).sincronizar();

        verify(pendencias).registrarRevisaoFrequencia(pendente, "Revisão histórica");
        verify(pendencias, never()).registrarRevisaoFrequencia(eq(resolvida), any());
        verify(pendencias, never()).registrarRevisaoFrequencia(eq(alteradaConcorrentemente), any());
        verify(frequencias).buscarIdsSujeitasRevisaoApos(eq(3L), any());
        verify(transacoes, times(2)).commit(any());
        var ordem = inOrder(contratos, frequencias);
        ordem.verify(contratos).buscarPorIdParaAtualizacao(10L);
        ordem.verify(frequencias).buscarPorIdParaAtualizacao(1L);
    }

    @Test
    void bloqueiaContratosEmOrdemCrescenteAntesDasFrequencias() {
        var frequencias = mock(FrequenciaRepositorio.class);
        var contratos = mock(ContratoRepositorio.class);
        var pendencias = mock(PendenciaAdministrativaServico.class);
        var transacoes = mock(PlatformTransactionManager.class);
        when(transacoes.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(frequencias.buscarIdsSujeitasRevisaoApos(eq(0L), any())).thenReturn(List.of(1L, 2L, 3L));
        when(frequencias.buscarContratoSnapshotId(1L)).thenReturn(Optional.of(20L));
        when(frequencias.buscarContratoSnapshotId(2L)).thenReturn(Optional.of(10L));
        when(frequencias.buscarContratoSnapshotId(3L)).thenReturn(Optional.of(20L));
        when(contratos.buscarPorIdParaAtualizacao(anyLong())).thenAnswer(inv -> {
            var contrato = new Contrato();
            contrato.setId(inv.getArgument(0));
            return Optional.of(contrato);
        });
        for (long id = 1; id <= 3; id++) {
            var pendente = frequencia(id, id == 2 ? 10L : 20L, EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
            when(frequencias.buscarPorIdParaAtualizacao(id)).thenReturn(Optional.of(pendente));
        }

        new SincronizadorPendenciasFrequenciaServico(frequencias, contratos, pendencias, transacoes).sincronizar();

        var ordem = inOrder(contratos, frequencias);
        ordem.verify(contratos).buscarPorIdParaAtualizacao(10L);
        ordem.verify(contratos).buscarPorIdParaAtualizacao(20L);
        ordem.verify(frequencias).buscarPorIdParaAtualizacao(1L);
        ordem.verify(frequencias).buscarPorIdParaAtualizacao(2L);
        ordem.verify(frequencias).buscarPorIdParaAtualizacao(3L);
        verify(contratos, times(2)).buscarPorIdParaAtualizacao(anyLong());
        verify(pendencias, times(3)).registrarRevisaoFrequencia(any(), eq("Revisão histórica"));
    }

    private Frequencia frequencia(Long id, Long contratoId, EstadoAuditoriaFrequencia estado) {
        var frequencia = new Frequencia();
        frequencia.setId(id);
        frequencia.setContratoSnapshotId(contratoId);
        frequencia.setEstadoAuditoria(estado);
        frequencia.setMotivoRevisao("Revisão histórica");
        return frequencia;
    }
}
