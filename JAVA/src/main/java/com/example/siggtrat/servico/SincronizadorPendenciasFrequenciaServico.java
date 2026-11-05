package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.repositorio.ContratoRepositorio;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Repara projeções de pendências legadas fora das consultas HTTP de relatório. */
@Service
public class SincronizadorPendenciasFrequenciaServico {
    private static final int TAMANHO_LOTE = 500;
    private final FrequenciaRepositorio frequencias;
    private final ContratoRepositorio contratos;
    private final PendenciaAdministrativaServico pendencias;
    private final TransactionTemplate transacaoLote;
    private final AtomicBoolean emExecucao = new AtomicBoolean();

    public SincronizadorPendenciasFrequenciaServico(FrequenciaRepositorio frequencias,
                                                    ContratoRepositorio contratos,
                                                    PendenciaAdministrativaServico pendencias,
                                                    PlatformTransactionManager transactionManager) {
        this.frequencias = frequencias;
        this.contratos = contratos;
        this.pendencias = pendencias;
        this.transacaoLote = new TransactionTemplate(transactionManager);
        this.transacaoLote.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelayString = "${app.pendencias.revisao-intervalo-ms:60000}",
            initialDelayString = "${app.pendencias.revisao-atraso-inicial-ms:60000}")
    public void sincronizar() {
        if (!emExecucao.compareAndSet(false, true)) return;
        try {
            long ultimoId = 0;
            while (true) {
                long cursor = ultimoId;
                Long proximo = transacaoLote.execute(status -> sincronizarLote(cursor));
                if (proximo == null) return;
                ultimoId = proximo;
            }
        } finally {
            emExecucao.set(false);
        }
    }

    private Long sincronizarLote(long ultimoId) {
        List<Long> ids = frequencias.buscarIdsSujeitasRevisaoApos(ultimoId, PageRequest.of(0, TAMANHO_LOTE));
        if (ids.isEmpty()) return null;
        Map<Long, Long> contratosPorFrequencia = new HashMap<>();
        for (Long id : ids) {
            contratosPorFrequencia.put(id, frequencias.buscarContratoSnapshotId(id).orElse(null));
        }
        Set<Long> contratosAusentes = new HashSet<>();
        // Lotes de instâncias diferentes podem conter contratos em sequências distintas.
        // Bloquear todos em ordem crescente evita a inversão A→B / B→A entre os lotes.
        contratosPorFrequencia.values().stream().filter(Objects::nonNull).distinct().sorted()
                .forEach(contratoId -> {
                    if (contratos.buscarPorIdParaAtualizacao(contratoId).isEmpty()) contratosAusentes.add(contratoId);
                });
        for (Long id : ids) {
            Long contratoId = contratosPorFrequencia.get(id);
            if (contratoId != null && contratosAusentes.contains(contratoId)) continue;
            var frequencia = frequencias.buscarPorIdParaAtualizacao(id).orElse(null);
            if (frequencia == null || frequencia.getEstadoAuditoria() != EstadoAuditoriaFrequencia.SUJEITA_REVISAO
                    || !Objects.equals(contratoId, frequencia.getContratoSnapshotId())) continue;
            pendencias.registrarRevisaoFrequencia(frequencia, frequencia.getMotivoRevisao());
        }
        return ids.get(ids.size() - 1);
    }
}
