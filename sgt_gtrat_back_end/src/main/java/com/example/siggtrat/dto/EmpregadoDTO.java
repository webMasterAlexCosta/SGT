package com.example.siggtrat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmpregadoDTO(
        Long id,
        String nome,
        String matricula,
        String cpf,
        String rg,
        String ctpsSerie,
        String nitPisPasep,
        String formacaoEscolar,
        Boolean optanteFgts,
        LocalDate dataOpcaoFgts,
        Integer qtdDependenteIr,
        Integer qtdDependenteSalFamilia,
        Integer qtdBeneficiariosPensaoAlim,
        Boolean optanteVa,
        BigDecimal valorVa,
        String outrosBeneficios,
        String observacao,
        String email,
        LocalDate dataNascimento,
        LocalDate dataAdmissao,

        // Referências
        Long statusId,
        String statusDescricao,

        Long lotacaoId,
        String lotacaoNome,

        Long cargoId,
        String cargoNome,

        Long jornadaId,
        String jornadaDescricao,

        // Objetos Aninhados
        EmpregadoContaDTO conta,
        EmpregadoEnderecoDTO endereco,
        List<EmpregadoTelefoneDTO> telefones,
        List<EmpregadoContratoDTO> contratos,

        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf
) {}