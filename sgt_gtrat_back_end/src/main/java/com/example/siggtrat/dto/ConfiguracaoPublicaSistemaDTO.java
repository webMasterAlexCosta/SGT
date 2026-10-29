package com.example.siggtrat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConfiguracaoPublicaSistemaDTO(
        @JsonProperty("EM_MANUTENCAO") boolean emManutencao,
        @JsonProperty("PREVISAO_RETORNO") String previsaoRetorno,
        @JsonProperty("CLIENT_IP") String clientIp,
        @JsonProperty("HORAS_DIA_TRABALHO") int horasDiaTrabalho,
        @JsonProperty("HORA_INICIO_NOTURNO") String horaInicioNoturno,
        @JsonProperty("MINUTOS_HORA_NOTURNA") double minutosHoraNoturna
) {}
