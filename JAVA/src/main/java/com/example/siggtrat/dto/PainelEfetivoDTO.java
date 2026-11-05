package com.example.siggtrat.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Indicador contratual e operacional de efetivo.
 *
 * <p>A cadeia de leitura é deliberadamente explícita:</p>
 * <ol>
 *   <li>efetivo previsto no contrato;</li>
 *   <li>empregados efetivamente alocados;</li>
 *   <li>frequências efetivamente lançadas;</li>
 *   <li>presenças e ausências registradas.</li>
 * </ol>
 *
 * <p>Dessa forma, falta de pessoal contratado e esquecimento de lançamento não
 * são tratados como o mesmo problema.</p>
 */
public record PainelEfetivoDTO(
        LocalDate inicio,
        LocalDate fim,
        String periodo,
        Long contratoId,
        Long lotacaoId,
        Long jornadaId,
        Long cargoId,
        Resumo resumoAtual,
        Resumo resumoAnoAnterior,
        List<PontoDiario> serieAtual,
        List<PontoDiario> serieAnoAnterior,
        List<DetalheContrato> detalhamentoContratos,
        List<DetalheDimensao> detalhamentoLotacoes,
        List<DetalheDimensao> detalhamentoTurnos,
        List<DetalheDimensao> detalhamentoCargos,
        List<LinhaDimensionada> quadroDimensionado
) {
    public record PontoDiario(
            LocalDate data,
            long efetivoPrevisto,
            long efetivoAlocado,
            long registrosLancados,
            long presentes,
            long ausentes,
            long outrosRegistros,
            long pendentesLancamento,
            long deficitContratual,
            long excedenteContratual,
            double percentualCobertura,
            double percentualAtendimento
    ) {}

    /** Fotografia da data final selecionada; não é média do período. */
    public record Resumo(
            long efetivoPrevisto,
            long efetivoAlocado,
            long registrosLancados,
            long presentes,
            long ausentes,
            long outrosRegistros,
            long pendentesLancamento,
            long deficitContratual,
            long excedenteContratual,
            double percentualCobertura,
            double percentualAtendimento
    ) {}

    /** Contrato: previsão reconstruída da composição válida na data e no recorte. */
    public record DetalheContrato(
            Long id,
            String nome,
            long efetivoPrevisto,
            long efetivoAlocado,
            long registrosLancados,
            long presentes,
            long ausentes,
            long pendentesLancamento,
            long deficitContratual,
            long excedenteContratual,
            double percentualCobertura,
            double percentualAtendimento
    ) {}

    /**
     * Lotação, turno e cargo expõem a previsão contratual temporal junto da
     * alocação efetiva, sem confundir vagas com frequências não lançadas.
     */
    public record DetalheDimensao(
            Long id, String nome, long efetivoAlocado, long registrosLancados,
            long presentes, long ausentes, long pendentesLancamento,
            double percentualCobertura, double percentualAtendimento,
            long efetivoPrevisto, long deficitContratual, long excedenteContratual
    ) {
        public DetalheDimensao(Long id, String nome, long efetivoAlocado, long registrosLancados,
                long presentes, long ausentes, long pendentesLancamento,
                double percentualCobertura, double percentualAtendimento) {
            this(id, nome, efetivoAlocado, registrosLancados, presentes, ausentes, pendentesLancamento,
                    percentualCobertura, percentualAtendimento, 0, 0, 0);
        }
    }

    /** Combinação operacional vigente, com seu dimensionamento contratual. */
    public record LinhaDimensionada(
            Long contratoId, String contrato, Long lotacaoId, String lotacao,
            Long jornadaId, String turno, Long cargoId, String cargo,
            long efetivoAlocado, long registrosLancados, long presentes, long ausentes,
            long pendentesLancamento, double percentualCobertura, double percentualAtendimento,
            long efetivoPrevisto, long deficitContratual, long excedenteContratual
    ) {
        public LinhaDimensionada(Long contratoId, String contrato, Long lotacaoId, String lotacao,
                Long jornadaId, String turno, Long cargoId, String cargo,
                long efetivoAlocado, long registrosLancados, long presentes, long ausentes,
                long pendentesLancamento, double percentualCobertura, double percentualAtendimento) {
            this(contratoId, contrato, lotacaoId, lotacao, jornadaId, turno, cargoId, cargo,
                    efetivoAlocado, registrosLancados, presentes, ausentes, pendentesLancamento,
                    percentualCobertura, percentualAtendimento, 0, 0, 0);
        }
    }

    public record Opcao(Long id, String nome) {}

    public record Filtros(
            List<Opcao> contratos,
            List<Opcao> lotacoes,
            List<Opcao> turnos,
            List<Opcao> cargos,
            boolean permiteDetalhamentoOperacional
    ) {}
}
