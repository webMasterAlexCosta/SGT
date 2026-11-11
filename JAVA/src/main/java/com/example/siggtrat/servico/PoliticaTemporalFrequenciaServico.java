package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import org.springframework.stereotype.Service;

import java.time.*;

/** Janela operacional configurável: N datas civis incluindo D, fechamento exclusivo em D+N no fuso negocial. */
@Service
public class PoliticaTemporalFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PoliticaTemporalFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final ZoneId zona = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);
    private final int diasJanela;

    public PoliticaTemporalFrequenciaServico() { this(30); }

    @org.springframework.beans.factory.annotation.Autowired
    public PoliticaTemporalFrequenciaServico(
            @org.springframework.beans.factory.annotation.Value("${frequencia.janela-operacional-dias:30}") int diasJanela) {
        if (diasJanela < 1) throw new IllegalArgumentException("frequencia.janela-operacional-dias deve ser positivo.");
        this.diasJanela = diasJanela;
    }

    public int diasJanela() { return diasJanela; }

    public LocalDate limiteFechamento(ZonedDateTime agora) {
        return referencia(agora).toLocalDate().minusDays(diasJanela);
    }

    public String motivoFechamento() {
        return "Fechamento negocial automático no início do " + ((long) diasJanela + 1)
                + "º dia em " + zona;
    }

    private ZonedDateTime referencia(ZonedDateTime agora) {
        return agora == null ? ZonedDateTime.now(zona) : agora.withZoneSameInstant(zona);
    }

    public ZonedDateTime instanteFechamento(LocalDate dataFrequencia) {
        if (dataFrequencia == null) throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Data da frequência obrigatória.");
        return dataFrequencia.plusDays(diasJanela).atStartOfDay(zona);
    }

    public boolean fechada(LocalDate dataFrequencia, ZonedDateTime agora) {
        ZonedDateTime referencia = referencia(agora);
        return !referencia.isBefore(instanteFechamento(dataFrequencia));
    }

    public boolean abertaParaOperacional(LocalDate dataFrequencia, ZonedDateTime agora) {
        ZonedDateTime referencia = referencia(agora);
        return dataFrequencia != null && !dataFrequencia.isAfter(referencia.toLocalDate())
                && !fechada(dataFrequencia, referencia);
    }

    public LocalDateTime prazoLocal(LocalDate dataFrequencia) {
        return instanteFechamento(dataFrequencia).toLocalDateTime();
    }

    public ZoneId zona() { return zona; }
}
