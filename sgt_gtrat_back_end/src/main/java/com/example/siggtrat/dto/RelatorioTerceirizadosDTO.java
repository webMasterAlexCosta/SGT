package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Visão consolidada e paginada do cadastro de terceirizados.
 *
 * Os totais e agrupamentos são calculados diretamente no MySQL sobre todo o
 * escopo autenticado. A página de empregados é apenas uma janela do resultado
 * e nunca é utilizada para calcular os indicadores.
 */
public record RelatorioTerceirizadosDTO(
        LocalDate referencia,
        LocalDateTime geradoEm,
        String escopo,
        Indicadores indicadores,
        List<Distribuicao> distribuicaoStatus,
        List<DistribuicaoContrato> distribuicaoContratos,
        List<OpcaoTexto> statusDisponiveis,
        List<Opcao> lotacoes,
        List<Opcao> contratos,
        PaginaEmpregados empregados
) {
    public record Indicadores(
            long empregados,
            long ativos,
            long semContratoVigente,
            long admitidosNoMes,
            long contratosVigentes,
            long efetivoPrevisto,
            long efetivoVinculado,
            long saldoContratual
    ) {}

    public record Distribuicao(String nome, long quantidade) {}

    public record DistribuicaoContrato(Long id, String nome, long quantidade, boolean semContrato) {}

    public record Opcao(Long id, String nome) {}

    public record OpcaoTexto(String valor, String nome) {}

    public record ContratoResumo(
            Long id,
            String numeroContrato,
            String descricao,
            boolean vigente
    ) {}

    public record EmpregadoResumo(
            Long id,
            String nome,
            String matricula,
            String cpf,
            String statusDescricao,
            Long lotacaoId,
            String lotacaoNome,
            String cargoNome,
            String jornadaDescricao,
            LocalDate dataAdmissao,
            List<ContratoResumo> contratos
    ) {}

    public record PaginaEmpregados(
            List<EmpregadoResumo> content,
            long totalElements,
            int totalPages,
            int number,
            int size,
            boolean first,
            boolean last
    ) {}
}
