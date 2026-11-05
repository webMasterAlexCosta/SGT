package com.example.siggtrat.dto;

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
        EnderecoDTO endereco,
        List<EmpregadoContratoDTO> contratos,

        Long gestaoLocalId,
        String gestaoLocalNome,
        Long superintendenciaId,
        String superintendenciaNome,
        String uf
) {}
