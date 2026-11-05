package com.example.siggtrat.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import com.example.siggtrat.entidade.HabilitacaoAdministrativaContrato;

public record VersaoContratoNegocialDTO(
        Long id,
        Integer numeroVersao,
        Long eventoOrigemId,
        LocalDate dataEfetiva,
        Instant registradoEm,
        String numeroContrato,
        Integer anoContrato,
        String descricao,
        Long tipoContratacaoId,
        String tipoContratacaoDescricao,
        Long fornecedorId,
        String cnpjEmpresa,
        String razaoSocialEmpresa,
        String nomeFantasiaEmpresa,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer efetivoPrevisto,
        Long gestaoLocalId,
        String gestaoLocalNome,
        Boolean matriculaVinculoObrigatoria,
        Boolean matriculaTipoContratacaoObrigatoria,
        Boolean matriculaVinculoObrigatoriaEfetiva,
        DayOfWeek diaSemanaReduzido,
        Integer horasDiaReduzido,
        HabilitacaoAdministrativaContrato habilitacaoAdministrativa,
        Boolean tipoContratacaoComprovado
) {
    /** Altera apenas os campos abrangidos pela correção documental do identificador. */
    public VersaoContratoNegocialDTO comIdentificador(String numero, Integer ano) {
        return new VersaoContratoNegocialDTO(id, numeroVersao, eventoOrigemId, dataEfetiva, registradoEm,
                numero, ano, descricao, tipoContratacaoId, tipoContratacaoDescricao,
                fornecedorId, cnpjEmpresa, razaoSocialEmpresa, nomeFantasiaEmpresa, dataInicio, dataFim,
                efetivoPrevisto, gestaoLocalId, gestaoLocalNome, matriculaVinculoObrigatoria,
                matriculaTipoContratacaoObrigatoria, matriculaVinculoObrigatoriaEfetiva,
                diaSemanaReduzido, horasDiaReduzido, habilitacaoAdministrativa, tipoContratacaoComprovado);
    }

    /** Posição projetada: a cópia não altera a fotografia contratual persistida. */
    public VersaoContratoNegocialDTO comVigencia(LocalDate inicio, LocalDate fim) {
        return new VersaoContratoNegocialDTO(id, numeroVersao, eventoOrigemId, dataEfetiva, registradoEm,
                numeroContrato, anoContrato, descricao, tipoContratacaoId, tipoContratacaoDescricao,
                fornecedorId, cnpjEmpresa, razaoSocialEmpresa, nomeFantasiaEmpresa, inicio, fim,
                efetivoPrevisto, gestaoLocalId, gestaoLocalNome, matriculaVinculoObrigatoria,
                matriculaTipoContratacaoObrigatoria, matriculaVinculoObrigatoriaEfetiva,
                diaSemanaReduzido, horasDiaReduzido, habilitacaoAdministrativa, tipoContratacaoComprovado);
    }

    /** Foto armazenada não certifica por si só o mapeamento de uma transferência legada. */
    public VersaoContratoNegocialDTO(Long id, Integer numeroVersao, Long eventoOrigemId, LocalDate dataEfetiva,
            Instant registradoEm, String numeroContrato, Integer anoContrato, String descricao,
            Long tipoContratacaoId, String tipoContratacaoDescricao, Long fornecedorId, String cnpjEmpresa,
            String razaoSocialEmpresa, String nomeFantasiaEmpresa, LocalDate dataInicio, LocalDate dataFim,
            Integer efetivoPrevisto, Long gestaoLocalId, String gestaoLocalNome, Boolean matriculaVinculoObrigatoria,
            Boolean matriculaTipoContratacaoObrigatoria, Boolean matriculaVinculoObrigatoriaEfetiva,
            DayOfWeek diaSemanaReduzido, Integer horasDiaReduzido, HabilitacaoAdministrativaContrato habilitacaoAdministrativa) {
        this(id, numeroVersao, eventoOrigemId, dataEfetiva, registradoEm, numeroContrato, anoContrato, descricao,
                tipoContratacaoId, tipoContratacaoDescricao, fornecedorId, cnpjEmpresa, razaoSocialEmpresa,
                nomeFantasiaEmpresa, dataInicio, dataFim, efetivoPrevisto, gestaoLocalId, gestaoLocalNome,
                matriculaVinculoObrigatoria, matriculaTipoContratacaoObrigatoria, matriculaVinculoObrigatoriaEfetiva,
                diaSemanaReduzido, horasDiaReduzido, habilitacaoAdministrativa, null);
    }

    /** Compatibilidade com fotografias legadas, cujas configurações adicionais são desconhecidas. */
    public VersaoContratoNegocialDTO(Long id, Integer numeroVersao, Long eventoOrigemId, LocalDate dataEfetiva,
            Instant registradoEm, String numeroContrato, Integer anoContrato, String descricao,
            Long tipoContratacaoId, String tipoContratacaoDescricao, Long fornecedorId, String cnpjEmpresa,
            String razaoSocialEmpresa, String nomeFantasiaEmpresa, LocalDate dataInicio, LocalDate dataFim,
            Integer efetivoPrevisto, Long gestaoLocalId, String gestaoLocalNome) {
        this(id, numeroVersao, eventoOrigemId, dataEfetiva, registradoEm, numeroContrato, anoContrato, descricao,
                tipoContratacaoId, tipoContratacaoDescricao, fornecedorId, cnpjEmpresa, razaoSocialEmpresa,
                nomeFantasiaEmpresa, dataInicio, dataFim, efetivoPrevisto, gestaoLocalId, gestaoLocalNome,
                null, null, null, null, null, null);
    }
}
