package com.example.siggtrat.dto;

import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;
import com.example.siggtrat.entidade.SituacaoTemporalContrato;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

public record ContratoDTO(
        Long id,
        String numeroContrato,
        String descricao,
        String tipo,
        LocalDate dataInicio,
        LocalDate dataFim,
        Boolean ativo,
        DayOfWeek diaSemanaReduzido,
        Integer horasDiaReduzido,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf,
        Integer efetivoPrevisto,
        Long efetivoAtual,
        Long saldoEfetivo,
        Double percentualOcupacao,

        // Identificação e relações contratuais canônicas.
        Integer anoContrato,
        Long tipoContratacaoId,
        String tipoContratacaoDescricao,
        Long fornecedorId,
        String cnpjEmpresa,
        String razaoSocialEmpresa,
        String nomeFantasiaEmpresa,
        EstadoNegocialContrato estadoNegocial,
        SituacaoTemporalContrato situacaoTemporal,
        HabilitacaoAdministrativaContrato habilitacaoAdministrativa,
        Integer efetivoOriginalPublicado,
        Boolean matriculaVinculoObrigatoria,
        Instant publicadoEm,
        Long publicadoPorId,
        String publicadoPorUsername,
        String referenciaSeiPublicacao,
        long version
) {
}
