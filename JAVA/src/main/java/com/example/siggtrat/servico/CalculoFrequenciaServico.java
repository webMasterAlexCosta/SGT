package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.JornadaDiaSemana;
import com.example.siggtrat.entidade.RegraAdicional;
import com.example.siggtrat.repositorio.RegraAdicionalRepositorio;
import com.example.siggtrat.utilitario.UtilitarioHorario;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalculoFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(CalculoFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    public static final String CALCULO_VERSAO = "CLT-NOTURNO-3.0";

    private final ConfiguracaoServico configuracaoServico;
    private final RegraAdicionalRepositorio regraAdicionalRepositorio;
    private final CalculadoraJornadaNoturnaServico calculadoraJornadaNoturna;

    public CalculoFrequenciaServico(ConfiguracaoServico configuracaoServico,
                                    RegraAdicionalRepositorio regraAdicionalRepositorio,
                                    CalculadoraJornadaNoturnaServico calculadoraJornadaNoturna) {
        this.configuracaoServico = configuracaoServico;
        this.regraAdicionalRepositorio = regraAdicionalRepositorio;
        this.calculadoraJornadaNoturna = calculadoraJornadaNoturna;
    }

    private int obterMetaDiariaEmMinutos(Frequencia frequencia) {
        if (frequencia.getEmpregado() == null || frequencia.getDataReferencia() == null) {
            return configuracaoServico.obterHorasTrabalhoPorDia() * 60;
        }

        if (frequencia.possuiSnapshotJornada()) {
            if (Boolean.FALSE.equals(frequencia.getJornadaSnapshotDiaTrabalho())) return 0;
            if (Boolean.TRUE.equals(frequencia.getJornadaSnapshotDiaTrabalho())) {
                long minutos = calcularMinutosIntervalo(frequencia.getJornadaSnapshotEntrada1(), frequencia.getJornadaSnapshotSaida1())
                        + calcularMinutosIntervalo(frequencia.getJornadaSnapshotEntrada2(), frequencia.getJornadaSnapshotSaida2());
                return (int) minutos;
            }
        }

        DayOfWeek diaAtual = frequencia.getDataReferencia().getDayOfWeek();
        Jornada jornada = frequencia.getEmpregado().getJornada();

        if (jornada != null && jornada.getDias() != null && !jornada.getDias().isEmpty()) {
            JornadaDiaSemana diaConfig = jornada.getDias().stream()
                    .filter(d -> d.getDiaSemana() == diaAtual)
                    .findFirst()
                    .orElse(null);

            if (diaConfig != null && Boolean.TRUE.equals(diaConfig.getDiaTrabalho())) {
                long minutos = calcularMinutosIntervalo(diaConfig.getEntrada1(), diaConfig.getSaida1())
                        + calcularMinutosIntervalo(diaConfig.getEntrada2(), diaConfig.getSaida2());
                return (int) minutos;
            } else if (diaConfig != null && Boolean.FALSE.equals(diaConfig.getDiaTrabalho())) {
                return 0; // Dia de Descanso (Folga/DSR)
            }
        }

        return configuracaoServico.obterHorasTrabalhoPorDia() * 60;
    }

    private long calcularMinutosIntervalo(String entrada, String saida) {
        return UtilitarioHorario.calcularIntervaloMinutos(entrada, saida, "entrada da jornada", "saída da jornada");
    }

    public void processarCalculos(Frequencia frequencia) {
        String statusOriginal = frequencia.getStatus();

        boolean temTurno1 = frequencia.getEntrada1() != null && !frequencia.getEntrada1().isBlank() &&
                frequencia.getSaida1() != null && !frequencia.getSaida1().isBlank();
        boolean temTurno2 = frequencia.getEntrada2() != null && !frequencia.getEntrada2().isBlank() &&
                frequencia.getSaida2() != null && !frequencia.getSaida2().isBlank();

        if (!temTurno1 && !temTurno2) {
            String horasManuais = frequencia.getHorasTrabalhadas();

            if (horasManuais != null && !horasManuais.isBlank()) {
                int minTrabalhadosManuais = interpretarHorarioEmMinutos(horasManuais);
                int minNoturnosPelaSaida = calcularNoturnoPeloHorarioSaida(minTrabalhadosManuais, frequencia.getHorarioSaida());

                double minutosPorHoraNoturna = configuracaoServico.obterMinutosPorHoraNoturna();
                double horasNoturnasConvertidas = minNoturnosPelaSaida > 0 ? (double) minNoturnosPelaSaida / minutosPorHoraNoturna : 0.0;
                int minutosFaturaveisBase = converterParaMinutosFaturaveis(
                        minTrabalhadosManuais, minNoturnosPelaSaida, horasNoturnasConvertidas);
                List<ResultadoTurno> turnosManuais = obterTurnosManuais(
                        minTrabalhadosManuais, frequencia.getHorarioSaida());
                int minutosAdicionais = calcularMinutosAdicionais(
                        turnosManuais, resolverJornadaId(frequencia));
                int minutosFaturaveis = minutosFaturaveisBase + minutosAdicionais;

                frequencia.setHorasNoturnas(arredondarHorasNoturnas(horasNoturnasConvertidas));
                frequencia.setHorasTrabalhadas(converterMinutosParaString(minutosFaturaveis));
                gravarMemoriaCalculo(frequencia, minTrabalhadosManuais, minNoturnosPelaSaida,
                        minutosAdicionais, minutosFaturaveis);
            } else {
                if ("PRESENCA".equalsIgnoreCase(statusOriginal)) {
                    ResultadoJornada jornadaPrevista = calcularJornada(
                            obterTurnosDaJornada(frequencia), resolverJornadaId(frequencia));
                    if (jornadaPrevista.minutosFisicos() > 0) {
                        frequencia.setHorasTrabalhadas(converterMinutosParaString(jornadaPrevista.minutosPagaveis()));
                        frequencia.setHorasNoturnas(arredondarHorasNoturnas(jornadaPrevista.horasNoturnasConvertidas()));
                        gravarMemoriaCalculo(frequencia, jornadaPrevista);
                    } else {
                        int minutosMeta = obterMetaDiariaEmMinutos(frequencia);
                        frequencia.setHorasTrabalhadas(converterMinutosParaString(minutosMeta));
                        frequencia.setHorasNoturnas(0.0);
                        gravarMemoriaCalculo(frequencia, minutosMeta, 0, 0, minutosMeta);
                    }
                } else {
                    frequencia.setHorasTrabalhadas("00:00");
                    frequencia.setHorasNoturnas(0.0);
                    gravarMemoriaCalculo(frequencia, 0, 0, 0, 0);
                }
            }

            if (statusOriginal == null || statusOriginal.isBlank()) {
                frequencia.setStatus("FALTA");
            } else {
                frequencia.setStatus(statusOriginal);
            }
            return;
        }

        // Modo Automático (Batidas)
        ResultadoJornada resultado = calcularJornada(calcularTurnos(
                frequencia.getEntrada1(), frequencia.getSaida1(),
                frequencia.getEntrada2(), frequencia.getSaida2()), resolverJornadaId(frequencia));

        frequencia.setHorasNoturnas(arredondarHorasNoturnas(resultado.horasNoturnasConvertidas()));
        frequencia.setHorasTrabalhadas(converterMinutosParaString(resultado.minutosPagaveis()));
        gravarMemoriaCalculo(frequencia, resultado);

        // Presença é aferida pelo tempo físico da escala. A redução ficta da hora
        // noturna altera faturamento/remuneração, mas não pode mascarar jornada parcial.
        definirStatus(frequencia, resultado.minutosFisicos(), statusOriginal);
    }

    private void definirStatus(Frequencia frequencia, int minutosTrabalhados, String statusOriginal) {
        if (statusOriginal != null &&
                !statusOriginal.equalsIgnoreCase("PRESENCA") &&
                !statusOriginal.equalsIgnoreCase("PRESENCA_PARCIAL") &&
                !statusOriginal.equalsIgnoreCase("FALTA") &&
                !statusOriginal.isBlank()) {

            frequencia.setStatus(statusOriginal);
            return;
        }

        int minutosMetaDiaria = obterMetaDiariaEmMinutos(frequencia);

        if (minutosMetaDiaria == 0 && minutosTrabalhados > 0) {
            frequencia.setStatus("PRESENCA");
            return;
        }

        if (minutosTrabalhados == 0) {
            frequencia.setStatus("FALTA");
        } else if (minutosTrabalhados >= (minutosMetaDiaria - 10)) {
            frequencia.setStatus("PRESENCA");
        } else {
            frequencia.setStatus("PRESENCA_PARCIAL");
        }
    }

    private int calcularNoturnoPeloHorarioSaida(int minTrabalhados, String horarioSaida) {
        if (horarioSaida == null || horarioSaida.isBlank()) return 0;
        int out = horarioParaMinutos(horarioSaida);
        int in = out - minTrabalhados;
        if (in < 0) in += 24 * 60;
        if (out < in) out += 24 * 60;
        return calcularJornada(List.of(criarTurno(in, out)), null).minutosNoturnosReais();
    }

    private record ResultadoTurno(int inicio, int fim) {
        int minutosTotais() {
            return Math.max(0, fim - inicio);
        }
    }

    private record ResultadoJornada(
            int minutosFisicos,
            int minutosNoturnosReais,
            double horasNoturnasConvertidas,
            int minutosAdicionais,
            int minutosPagaveis
    ) {}

    private Long resolverJornadaId(Frequencia frequencia) {
        if (frequencia.getJornadaSnapshotId() != null) return frequencia.getJornadaSnapshotId();
        return frequencia.getEmpregado() != null && frequencia.getEmpregado().getJornada() != null
                ? frequencia.getEmpregado().getJornada().getId() : null;
    }

    private List<ResultadoTurno> obterTurnosManuais(int minutos, String horarioSaida) {
        if (minutos <= 0 || horarioSaida == null || horarioSaida.isBlank()) return List.of();
        int fim = horarioParaMinutos(horarioSaida);
        int inicio = fim - minutos;
        if (inicio < 0) {
            inicio += 24 * 60;
            fim += 24 * 60;
        }
        return List.of(criarTurno(inicio, fim));
    }

    private void gravarMemoriaCalculo(Frequencia frequencia, ResultadoJornada resultado) {
        gravarMemoriaCalculo(frequencia, resultado.minutosFisicos(), resultado.minutosNoturnosReais(),
                resultado.minutosAdicionais(), resultado.minutosPagaveis());
    }

    private void gravarMemoriaCalculo(Frequencia frequencia, int minutosFisicos,
                                      int minutosNoturnosReais, int minutosAdicionais,
                                      int minutosPagaveis) {
        frequencia.setMinutosFisicos(Math.max(0, minutosFisicos));
        frequencia.setMinutosNoturnosReais(Math.max(0, minutosNoturnosReais));
        frequencia.setMinutosAdicionais(Math.max(0, minutosAdicionais));
        frequencia.setMinutosPagaveis(Math.max(0, minutosPagaveis));
        frequencia.setCalculoVersao(CALCULO_VERSAO);
    }

    private List<ResultadoTurno> obterTurnosDaJornada(Frequencia frequencia) {
        if (frequencia.possuiSnapshotJornada()) {
            if (!Boolean.TRUE.equals(frequencia.getJornadaSnapshotDiaTrabalho())) return List.of();
            return calcularTurnos(
                    frequencia.getJornadaSnapshotEntrada1(), frequencia.getJornadaSnapshotSaida1(),
                    frequencia.getJornadaSnapshotEntrada2(), frequencia.getJornadaSnapshotSaida2());
        }

        if (frequencia.getEmpregado() == null || frequencia.getDataReferencia() == null
                || frequencia.getEmpregado().getJornada() == null
                || frequencia.getEmpregado().getJornada().getDias() == null) return List.of();

        JornadaDiaSemana dia = frequencia.getEmpregado().getJornada().getDias().stream()
                .filter(item -> item.getDiaSemana() == frequencia.getDataReferencia().getDayOfWeek())
                .findFirst()
                .orElse(null);
        if (dia == null || !Boolean.TRUE.equals(dia.getDiaTrabalho())) return List.of();
        return calcularTurnos(dia.getEntrada1(), dia.getSaida1(), dia.getEntrada2(), dia.getSaida2());
    }

    private List<ResultadoTurno> calcularTurnos(String entrada1, String saida1, String entrada2, String saida2) {
        List<ResultadoTurno> turnos = new ArrayList<>(2);
        adicionarTurno(turnos, entrada1, saida1);
        adicionarTurno(turnos, entrada2, saida2);
        return turnos;
    }

    private void adicionarTurno(List<ResultadoTurno> turnos, String entradaStr, String saidaStr) {
        if (entradaStr == null || entradaStr.isBlank() || saidaStr == null || saidaStr.isBlank()) return;
        UtilitarioHorario.validarPar(entradaStr, saidaStr, "entrada", "saída");
        int inicio = horarioParaMinutos(entradaStr);
        int fim = horarioParaMinutos(saidaStr);
        if (fim < inicio) fim += 24 * 60;

        if (!turnos.isEmpty()) {
            int fimAnterior = turnos.get(turnos.size() - 1).fim();
            while (inicio < fimAnterior) {
                inicio += 24 * 60;
                fim += 24 * 60;
            }
        }
        turnos.add(criarTurno(inicio, fim));
    }

    private ResultadoTurno criarTurno(int inicio, int fim) {
        return new ResultadoTurno(inicio, Math.max(inicio, fim));
    }

    private ResultadoJornada calcularJornada(List<ResultadoTurno> turnos, Long jornadaId) {
        if (turnos == null || turnos.isEmpty()) return new ResultadoJornada(0, 0, 0.0, 0, 0);
        CalculadoraJornadaNoturnaServico.Resultado base = calculadoraJornadaNoturna.calcularPeriodos(
                turnos.stream()
                        .map(turno -> new CalculadoraJornadaNoturnaServico.Periodo(
                                turno.inicio(), turno.fim()))
                        .toList()
        );
        int minutosAdicionais = calcularMinutosAdicionais(turnos, jornadaId);
        return new ResultadoJornada(
                base.minutosFisicos(),
                base.minutosNoturnosReais(),
                base.horasNoturnasReduzidas(),
                minutosAdicionais,
                base.minutosPagaveisBase() + minutosAdicionais
        );
    }

    /**
     * Converte cada percentual cadastrado em minutos equivalentes sobre a
     * interseção efetivamente trabalhada. A memória fica separada dos minutos
     * físicos para que o faturamento possa explicar cada parcela.
     */
    private int calcularMinutosAdicionais(List<ResultadoTurno> turnos, Long jornadaId) {
        if (jornadaId == null || turnos == null || turnos.isEmpty()) return 0;
        int total = 0;
        for (RegraAdicional regra : regraAdicionalRepositorio.findByJornada_IdOrderByOrdemAsc(jornadaId)) {
            if (!Boolean.TRUE.equals(regra.getAtivo()) || regra.getPercentual() == null
                    || regra.getPercentual().signum() <= 0) continue;

            int baseMinutos;
            if (regra.getInicio() == null || regra.getInicio().isBlank()
                    || regra.getFim() == null || regra.getFim().isBlank()) {
                baseMinutos = turnos.stream().mapToInt(ResultadoTurno::minutosTotais).sum();
            } else {
                int inicioJanela = horarioParaMinutos(regra.getInicio());
                int fimJanela = horarioParaMinutos(regra.getFim());
                if (fimJanela <= inicioJanela) fimJanela += 24 * 60;
                baseMinutos = 0;
                for (ResultadoTurno turno : turnos) {
                    int primeiroDia = Math.floorDiv(turno.inicio(), 24 * 60) - 1;
                    int ultimoDia = Math.floorDiv(turno.fim(), 24 * 60) + 1;
                    for (int dia = primeiroDia; dia <= ultimoDia; dia++) {
                        baseMinutos += interseccao(turno.inicio(), turno.fim(),
                                dia * 24 * 60 + inicioJanela, dia * 24 * 60 + fimJanela);
                    }
                }
            }
            total += regra.getPercentual()
                    .multiply(java.math.BigDecimal.valueOf(baseMinutos))
                    .divide(java.math.BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .intValue();
        }
        return total;
    }

    private int converterParaMinutosFaturaveis(
            int minutosFisicos, int minutosNoturnosReais, double horasNoturnasConvertidas) {
        double bonusNoturnoMinutos = (horasNoturnasConvertidas * 60.0) - minutosNoturnosReais;
        return minutosFisicos + (int) Math.round(bonusNoturnoMinutos);
    }

    private int interseccao(int in1, int out1, int in2, int out2) {
        int maxIn = Math.max(in1, in2);
        int minOut = Math.min(out1, out2);
        return Math.max(0, minOut - maxIn);
    }

    private int interpretarHorarioEmMinutos(String timeStr) {
        return UtilitarioHorario.duracaoParaMinutos(timeStr, "horas trabalhadas");
    }

    private int horarioParaMinutos(String time) {
        return UtilitarioHorario.horarioParaMinutos(time, "horário");
    }

    private String converterMinutosParaString(int totalMinutos) {
        int horas = totalMinutos / 60;
        int minutos = totalMinutos % 60;
        return String.format("%02d:%02d", horas, minutos);
    }

    private double arredondarHorasNoturnas(double valor) {
        // Evita acumular erro de arredondamento quando o relatório soma muitos
        // dias. A apresentação final continua responsável por exibir 2 casas.
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }
}
