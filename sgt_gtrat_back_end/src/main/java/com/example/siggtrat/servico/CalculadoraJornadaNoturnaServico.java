package com.example.siggtrat.servico;

import com.example.siggtrat.utilitario.UtilitarioHorario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculadora pura da duração física e da redução ficta da hora noturna.
 *
 * <p>A regra é centralizada aqui para que lançamento, relatório e testes não
 * mantenham implementações divergentes. O período noturno urbano é obtido da
 * configuração (por padrão, 22h às 5h) e cada hora é convertida pela duração
 * configurada (por padrão, 52min30s).</p>
 */
@Service
public class CalculadoraJornadaNoturnaServico {

    private static final int MINUTOS_DIA = 24 * 60;

    private final ConfiguracaoServico configuracaoServico;

    public CalculadoraJornadaNoturnaServico(ConfiguracaoServico configuracaoServico) {
        this.configuracaoServico = configuracaoServico;
    }

    public Resultado calcular(String entrada1, String saida1, String entrada2, String saida2) {
        return calcularPeriodos(normalizarPeriodos(entrada1, saida1, entrada2, saida2));
    }

    public Resultado calcularPeriodos(List<Periodo> periodosInformados) {
        List<Periodo> periodos = periodosInformados == null
                ? List.of()
                : periodosInformados.stream()
                .filter(periodo -> periodo != null && periodo.fim() > periodo.inicio())
                .toList();
        if (periodos.isEmpty()) return Resultado.vazio();

        int minutosFisicos = periodos.stream().mapToInt(Periodo::minutosTotais).sum();
        int minutosNoturnosReais = periodos.stream()
                .mapToInt(periodo -> calcularIntersecaoNoturnaLegal(periodo.inicio(), periodo.fim()))
                .sum();

        Integer fimPeriodoIntegral = encontrarFimPeriodoNoturnoIntegral(periodos);
        if (fimPeriodoIntegral != null) {
            int fimJornada = periodos.get(periodos.size() - 1).fim();
            minutosNoturnosReais += periodos.stream()
                    .mapToInt(periodo -> intersecao(
                            periodo.inicio(), periodo.fim(), fimPeriodoIntegral, fimJornada))
                    .sum();
        }

        double minutosPorHoraNoturna = configuracaoServico.obterMinutosPorHoraNoturna();
        if (minutosPorHoraNoturna <= 0) {
            throw new IllegalStateException("A duração da hora noturna deve ser maior que zero.");
        }
        int minutosNoturnosReduzidos = minutosNoturnosReais == 0
                ? 0
                : (int) Math.round(minutosNoturnosReais * 60.0 / minutosPorHoraNoturna);
        int minutosPagaveisBase = minutosFisicos - minutosNoturnosReais + minutosNoturnosReduzidos;

        double horasNoturnasExatas = minutosNoturnosReais == 0
                ? 0.0
                : minutosNoturnosReais / minutosPorHoraNoturna;

        return new Resultado(
                minutosFisicos,
                minutosNoturnosReais,
                minutosNoturnosReduzidos,
                minutosPagaveisBase,
                horasNoturnasExatas,
                fimPeriodoIntegral != null
        );
    }

    public List<Periodo> normalizarPeriodos(
            String entrada1, String saida1, String entrada2, String saida2) {
        List<Periodo> periodos = new ArrayList<>(2);
        adicionarPeriodo(periodos, entrada1, saida1);
        adicionarPeriodo(periodos, entrada2, saida2);
        return List.copyOf(periodos);
    }

    private void adicionarPeriodo(List<Periodo> periodos, String entrada, String saida) {
        if (entrada == null || entrada.isBlank() || saida == null || saida.isBlank()) return;
        UtilitarioHorario.validarPar(entrada, saida, "entrada", "saída");
        int inicio = UtilitarioHorario.horarioParaMinutos(entrada, "entrada");
        int fim = UtilitarioHorario.horarioParaMinutos(saida, "saída");
        if (fim < inicio) fim += MINUTOS_DIA;

        if (!periodos.isEmpty()) {
            int fimAnterior = periodos.get(periodos.size() - 1).fim();
            while (inicio < fimAnterior) {
                inicio += MINUTOS_DIA;
                fim += MINUTOS_DIA;
            }
        }
        if (fim > inicio) periodos.add(new Periodo(inicio, fim));
    }

    private int calcularIntersecaoNoturnaLegal(int inicio, int fim) {
        int inicioNoturno = configuracaoServico.obterHoraInicioNoturno().getHour() * 60
                + configuracaoServico.obterHoraInicioNoturno().getMinute();
        int fimNoturno = configuracaoServico.obterHoraFimNoturno().getHour() * 60
                + configuracaoServico.obterHoraFimNoturno().getMinute();
        int total = 0;
        int primeiroDia = Math.floorDiv(inicio, MINUTOS_DIA) - 1;
        int ultimoDia = Math.floorDiv(fim, MINUTOS_DIA) + 1;

        for (int dia = primeiroDia; dia <= ultimoDia; dia++) {
            int inicioJanela = dia * MINUTOS_DIA + inicioNoturno;
            int fimJanela = dia * MINUTOS_DIA + fimNoturno;
            if (fimJanela <= inicioJanela) fimJanela += MINUTOS_DIA;
            total += intersecao(inicio, fim, inicioJanela, fimJanela);
        }
        return total;
    }

    private Integer encontrarFimPeriodoNoturnoIntegral(List<Periodo> periodos) {
        int inicioNoturno = configuracaoServico.obterHoraInicioNoturno().getHour() * 60
                + configuracaoServico.obterHoraInicioNoturno().getMinute();
        int fimNoturno = configuracaoServico.obterHoraFimNoturno().getHour() * 60
                + configuracaoServico.obterHoraFimNoturno().getMinute();
        Periodo primeiro = periodos.get(0);
        Periodo ultimo = periodos.get(periodos.size() - 1);
        int diaBase = Math.floorDiv(primeiro.inicio(), MINUTOS_DIA) * MINUTOS_DIA;

        for (int deslocamento = -1; deslocamento <= 1; deslocamento++) {
            int inicioJanela = diaBase + deslocamento * MINUTOS_DIA + inicioNoturno;
            int fimJanela = diaBase + deslocamento * MINUTOS_DIA + fimNoturno;
            if (fimJanela <= inicioJanela) fimJanela += MINUTOS_DIA;
            if (primeiro.inicio() <= inicioJanela && ultimo.fim() > fimJanela) {
                return fimJanela;
            }
        }
        return null;
    }

    private int intersecao(int inicio1, int fim1, int inicio2, int fim2) {
        return Math.max(0, Math.min(fim1, fim2) - Math.max(inicio1, inicio2));
    }

    public record Periodo(int inicio, int fim) {
        public Periodo {
            if (fim < inicio) throw new IllegalArgumentException("O fim do período não pode anteceder o início.");
        }

        public int minutosTotais() {
            return Math.max(0, fim - inicio);
        }
    }

    public record Resultado(
            int minutosFisicos,
            int minutosNoturnosReais,
            int minutosNoturnosReduzidos,
            int minutosPagaveisBase,
            double horasNoturnasReduzidas,
            boolean prorrogacaoNoturna
    ) {
        public static Resultado vazio() {
            return new Resultado(0, 0, 0, 0, 0.0, false);
        }
    }
}
