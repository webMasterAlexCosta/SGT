package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Persiste FECHADA; não é apenas um estado calculado na resposta HTTP. */
@Service
public class FechamentoAutomaticoFrequenciaNegocialServico {
    private final FrequenciaRepositorio repositorio;
    private final PoliticaTemporalFrequenciaServico politica;
    private final AuditoriaFrequenciaServico auditoria;

    public FechamentoAutomaticoFrequenciaNegocialServico(FrequenciaRepositorio repositorio,
                                                          PoliticaTemporalFrequenciaServico politica,
                                                          AuditoriaFrequenciaServico auditoria) {
        this.repositorio = repositorio;
        this.politica = politica;
        this.auditoria = auditoria;
    }

    @Scheduled(cron = "${frequencia.fechamento-negocial.cron:0 * * * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public int fecharVencidas() {
        LocalDate limite = LocalDate.now(politica.zona()).minusDays(7);
        int total = 0;
        for (Frequencia frequencia : repositorio.findByEstadoNegocialAndDataReferenciaLessThanEqual(
                EstadoNegocialFrequencia.ABERTA, limite)) {
            if (!politica.fechada(frequencia.getDataReferencia(), null)) continue;
            String antes = auditoria.snapshot(frequencia);
            frequencia.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            frequencia.setVersaoNegocial(frequencia.getVersaoNegocial() == null ? 2 : frequencia.getVersaoNegocial() + 1);
            repositorio.saveAndFlush(frequencia);
            auditoria.registrar(frequencia, TipoEventoFrequencia.FECHAMENTO_AUTOMATICO, antes,
                    "Fechamento negocial automático no início do 8º dia em America/Sao_Paulo", null, null, null);
            total++;
        }
        return total;
    }
}
