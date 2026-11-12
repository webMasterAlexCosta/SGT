package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/** Persiste FECHADA; não é apenas um estado calculado na resposta HTTP. */
@Service
public class FechamentoAutomaticoFrequenciaNegocialServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(FechamentoAutomaticoFrequenciaNegocialServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final FrequenciaRepositorio repositorio;
    private final PoliticaTemporalFrequenciaServico politica;
    private final AuditoriaFrequenciaServico auditoria;
    private final TransactionTemplate transacaoLote;
    private final int tamanhoLote;
    private final InvalidacaoCacheServico invalidacaoCache;

    public FechamentoAutomaticoFrequenciaNegocialServico(FrequenciaRepositorio repositorio,
                                                          PoliticaTemporalFrequenciaServico politica,
                                                          AuditoriaFrequenciaServico auditoria,
                                                          PlatformTransactionManager transactionManager,
                                                          InvalidacaoCacheServico invalidacaoCache,
                                                          @Value("${frequencia.fechamento-negocial.tamanho-lote:500}") int tamanhoLote) {
        this.repositorio = repositorio;
        this.politica = politica;
        this.auditoria = auditoria;
        this.invalidacaoCache = invalidacaoCache;
        this.tamanhoLote = Math.min(500, Math.max(1, tamanhoLote));
        this.transacaoLote = new TransactionTemplate(transactionManager);
        this.transacaoLote.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "${frequencia.fechamento-negocial.cron:0 * * * * *}", zone = "America/Sao_Paulo")
    public int fecharVencidas() {
        java.time.Instant agora = java.time.Instant.now();
        int total = 0;
        long ultimoId = 0;
        while (true) {
            long cursor = ultimoId;
            ResultadoLote lote = transacaoLote.execute(status -> fecharLote(agora, cursor));
            if (lote == null || lote.consultadas() == 0) return total;
            total += lote.fechadas();
            ultimoId = lote.ultimoId();
        }
    }

    private ResultadoLote fecharLote(java.time.Instant agora, long ultimoId) {
        List<Frequencia> lote = repositorio.buscarEdicoesVencidasApos(
                EstadoNegocialFrequencia.ABERTA, agora, agora.minus(java.time.Duration.ofHours(politica.horasEdicao())), ultimoId, PageRequest.of(0, tamanhoLote));
        if (lote.isEmpty()) return new ResultadoLote(0, 0, ultimoId);
        int fechadas = 0;
        for (Frequencia frequencia : lote) {
            if (!politica.edicaoEncerrada(frequencia, agora)) continue;
            String antes = auditoria.snapshot(frequencia);
            frequencia.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            frequencia.setVersaoNegocial(frequencia.getVersaoNegocial() == null ? 2 : frequencia.getVersaoNegocial() + 1);
            repositorio.save(frequencia);
            auditoria.registrar(frequencia, TipoEventoFrequencia.FECHAMENTO_AUTOMATICO, antes,
                    politica.motivoFechamentoEdicao(), null, null, null);
            fechadas++;
        }
        // O estado e seus eventos são confirmados juntos. O contexto JPA é
        // liberado no commit deste lote antes de consultar o próximo.
        repositorio.flush();
        if (fechadas > 0) invalidacaoCache.invalidarCachesDeNegocioAposCommit();
        return new ResultadoLote(lote.size(), fechadas, lote.get(lote.size() - 1).getId());
    }

    private record ResultadoLote(int consultadas, int fechadas, long ultimoId) {}
}
