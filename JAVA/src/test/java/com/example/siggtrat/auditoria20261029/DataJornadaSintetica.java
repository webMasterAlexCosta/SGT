package com.example.siggtrat.auditoria20261029;

import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.servico.PoliticaTemporalFrequenciaServico;
import com.example.siggtrat.utilitario.RelogioSistema;

import java.time.LocalDate;
import java.util.Objects;

/** Seleção de data somente para a jornada recém-criada da fixture sintética. */
final class DataJornadaSintetica {
    private static final PoliticaTemporalFrequenciaServico POLITICA = new PoliticaTemporalFrequenciaServico();

    private DataJornadaSintetica() {}

    static LocalDate ultimoDiaDeTrabalhoAnterior(Jornada jornada) {
        return ultimoDiaDeTrabalhoAnterior(jornada, RelogioSistema.hoje());
    }

    static LocalDate ultimoDiaDeTrabalhoAnterior(Jornada jornada, LocalDate hoje) {
        Objects.requireNonNull(jornada, "Jornada da fixture obrigatória");
        Objects.requireNonNull(hoje, "Data de referência da fixture obrigatória");
        var agora = hoje.atStartOfDay(POLITICA.zona());
        // A política existente fecha no início de data+7. Não se inventa uma
        // contagem alternativa nem se usa hoje, futuro ou data já fechada.
        for (LocalDate data = hoje.minusDays(1); POLITICA.abertaParaOperacional(data, agora);
                data = data.minusDays(1)) {
            var dia = data.getDayOfWeek();
            if (jornada.getDias().stream().anyMatch(item -> dia == item.getDiaSemana()
                    && Boolean.TRUE.equals(item.getDiaTrabalho()))) {
                return data;
            }
        }
        throw new IllegalStateException("Jornada sintética sem dia de trabalho anterior dentro da janela operacional aberta");
    }
}
