package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;
import com.example.siggtrat.entidade.SituacaoTemporalContrato;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A posição e as evidências pertencem ao mesmo D/K; nenhum ID projetado é inventado. */
public record PosicaoTemporalContratoLeituraDTO(Long contratoId, LocalDate dataReferencia, Instant conhecidoAte,
        EstadoNegocialContrato estadoNegocial, SituacaoTemporalContrato situacaoTemporal,
        HabilitacaoAdministrativaContrato habilitacaoAdministrativa, LocalDate vigenciaInicio, LocalDate vigenciaFim,
        VersaoContratoLeituraDTO versaoContratual, List<EventoCicloVidaContratoDTO> eventosAplicados,
        List<RelacaoPrecedenciaEventoContratoDTO> relacoesConsideradas,
        Map<String, ProvaCampoHistoricoDTO> provaCampos) { }
