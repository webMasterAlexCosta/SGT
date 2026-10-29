package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.evento.EventoPainelCalculoSolicitado;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import com.example.siggtrat.repositorio.PainelCalculoRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PainelCalculoRecuperacaoServico {

    private final PainelCalculoRepositorio repositorio;
    private final PainelCalculoEstadoServico stateServico;
    private final ApplicationEventPublisher eventPublisher;
    private final long timeoutMinutos;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public PainelCalculoRecuperacaoServico(PainelCalculoRepositorio repositorio,
                                        PainelCalculoEstadoServico stateServico,
                                        ApplicationEventPublisher eventPublisher,
                                        @Value("${app.painel.timeout-minutos:30}") long timeoutMinutos) {
        this.repositorio = repositorio;
        this.stateServico = stateServico;
        this.eventPublisher = eventPublisher;
        this.timeoutMinutos = Math.max(5, timeoutMinutos);
    }

    @Scheduled(fixedDelayString = "${app.painel.recuperacao-intervalo-ms:30000}", initialDelay = 15000)
    public void recuperar() {
        repositorio.buscarProcessamentosTravados(PainelCalculoStatus.PROCESSANDO, LocalDateTime.now(zoneId).minusMinutes(timeoutMinutos))
                .forEach(item -> stateServico.reenfileirar(item.getId(), "Processamento retomado após interrupção."));
        repositorio.findTop20ByStatusOrderByAtualizadoEmAsc(PainelCalculoStatus.PENDENTE)
                .forEach(item -> eventPublisher.publishEvent(new EventoPainelCalculoSolicitado(item.getId())));
    }
}
