package com.example.siggtrat.servico;

import com.example.siggtrat.dto.PainelGestaoDTO;
import com.example.siggtrat.evento.EventoPainelCalculoSolicitado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PainelCalculoProcessador {

    private static final Logger log = LoggerFactory.getLogger(PainelCalculoProcessador.class);
    private final Set<Long> emExecucao = ConcurrentHashMap.newKeySet();
    private final PainelCalculoEstadoServico stateServico;
    private final PainelServico painelServico;
    private final ObjectMapper objectMapper;

    public PainelCalculoProcessador(PainelCalculoEstadoServico stateServico,
                                  PainelServico painelServico,
                                  ObjectMapper objectMapper) {
        this.stateServico = stateServico;
        this.painelServico = painelServico;
        this.objectMapper = objectMapper;
    }

    @Async("painelExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoSolicitar(EventoPainelCalculoSolicitado event) {
        processar(event.calculoId());
    }

    public void processar(Long calculoId) {
        if (calculoId == null || !emExecucao.add(calculoId)) return;
        try {
            var descriptorOpt = stateServico.reivindicar(calculoId);
            if (descriptorOpt.isEmpty()) return;
            var descritor = descriptorOpt.get();
            log.info("Iniciando consolidação assíncrona do painel: id={} escopo={} referência={} período={}",
                    calculoId, descritor.contexto().nivel(), descritor.referencia(), descritor.periodoDias());
            PainelGestaoDTO dados = painelServico.calcularGestaoCacheado(
                    descritor.contexto(), descritor.referencia(), descritor.periodoDias());
            if (dados == null) {
                throw new IllegalStateException("A consolidação do painel terminou sem produzir indicadores.");
            }
            String payload = serializar(dados);
            if (payload == null || payload.isBlank() || "null".equalsIgnoreCase(payload.trim())) {
                throw new IllegalStateException("A consolidação do painel produziu um payload inválido.");
            }
            stateServico.concluir(calculoId, payload);
        } catch (Throwable erro) {
            log.error("Falha na consolidação assíncrona do painel id={}", calculoId, erro);
            stateServico.falhar(calculoId, erro);
        } finally {
            emExecucao.remove(calculoId);
        }
    }

    private String serializar(PainelGestaoDTO dados) throws JsonProcessingException {
        return objectMapper.writeValueAsString(dados);
    }
}
