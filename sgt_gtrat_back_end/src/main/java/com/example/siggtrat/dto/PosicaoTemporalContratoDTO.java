package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;
import com.example.siggtrat.entidade.SituacaoTemporalContrato;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Posição histórica do eixo de ciclo de vida do contrato.
 *
 * A reconstrução considera a publicação e todos os fatos contratuais válidos
 * conhecidos no corte temporal solicitado.
 */
public record PosicaoTemporalContratoDTO(
        Long contratoId,
        LocalDate dataReferencia,
        Instant conhecidoAte,
        EstadoNegocialContrato estadoNegocial,
        SituacaoTemporalContrato situacaoTemporal,
        HabilitacaoAdministrativaContrato habilitacaoAdministrativa,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        VersaoContratoNegocialDTO versaoContratual,
        List<EventoCicloVidaContratoDTO> eventosAplicados,
        List<RelacaoPrecedenciaEventoContratoDTO> relacoesConsideradas
) {}
