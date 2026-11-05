package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import com.example.siggtrat.evento.EventoFechamentoFrequenciaSolicitado;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class FechamentoFrequenciaRecuperacaoServico {

    private static final ZoneId ZONE_ID = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    private final FechamentoFrequenciaEstadoServico stateServico;
    private final PropriedadesFechamentoFrequencia properties;
    private final ApplicationEventPublisher eventPublisher;

    public FechamentoFrequenciaRecuperacaoServico(FechamentoFrequenciaEstadoServico stateServico,
                                                PropriedadesFechamentoFrequencia properties,
                                                ApplicationEventPublisher eventPublisher) {
        this.stateServico = stateServico;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarAoIniciar() {
        // PENDENTE pode ser retomado imediatamente. PROCESSANDO só é retomado
        // quando o lease/heartbeat expira, evitando interromper outra instância.
        recuperarPeriodicamente();
    }

    @Scheduled(fixedDelayString = "${frequencia.fechamento.recovery-delay-ms:30000}")
    public void recuperarPeriodicamente() {
        Set<Long> ids = new LinkedHashSet<>();
        ids.addAll(stateServico.reenfileirarTravados(
                LocalDateTime.now(ZONE_ID).minusSeconds(properties.staleAfterSeconds())));
        ids.addAll(stateServico.pendentes());
        ids.forEach(this::publicar);
    }

    private void publicar(Long id) {
        eventPublisher.publishEvent(new EventoFechamentoFrequenciaSolicitado(id));
    }
}
