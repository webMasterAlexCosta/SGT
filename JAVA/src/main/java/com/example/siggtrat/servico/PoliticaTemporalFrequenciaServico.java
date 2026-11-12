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
    private final int horasEdicao;

    public PoliticaTemporalFrequenciaServico() { this(7, 72); }

    public PoliticaTemporalFrequenciaServico(int diasJanela) { this(diasJanela, 72); }

    @org.springframework.beans.factory.annotation.Autowired
    public PoliticaTemporalFrequenciaServico(
            @org.springframework.beans.factory.annotation.Value("${frequencia.janela-operacional-dias:7}") int diasJanela,
            @org.springframework.beans.factory.annotation.Value("${frequencia.edicao-horas:72}") int horasEdicao) {
        if (diasJanela < 1) throw new IllegalArgumentException("frequencia.janela-operacional-dias deve ser positivo.");
        if (horasEdicao < 1) throw new IllegalArgumentException("frequencia.edicao-horas deve ser positivo.");
        this.diasJanela = diasJanela;
        this.horasEdicao = horasEdicao;
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

    /** Não recalcula os prazos depois do primeiro lançamento. */
    public void capturarPrazos(com.example.siggtrat.entidade.Frequencia frequencia, Instant primeiro) {
        if (frequencia.getPrazoLancamentoEm() != null) return;
        frequencia.setRegistradoEm(primeiro);
        frequencia.setPrazoLancamentoEm(instanteFechamento(frequencia.getDataReferencia()).toInstant());
        frequencia.setPrazoEdicaoEm(primeiro.plus(Duration.ofHours(horasEdicao)));
    }

    public Instant prazoEdicao(com.example.siggtrat.entidade.Frequencia frequencia) {
        // Acervo: aplica a nova janela de edição sem fabricar classificação de tempestividade.
        return frequencia.getPrazoEdicaoEm() != null ? frequencia.getPrazoEdicaoEm()
                : frequencia.getRegistradoEm().plus(Duration.ofHours(horasEdicao));
    }

    public boolean edicaoEncerrada(com.example.siggtrat.entidade.Frequencia frequencia, Instant agora) {
        return !(agora == null ? Instant.now() : agora).isBefore(prazoEdicao(frequencia));
    }

    public int horasEdicao() { return horasEdicao; }
    public String motivoFechamentoEdicao() { return "Fechamento automático após o prazo de edição do primeiro lançamento."; }

    public ZoneId zona() { return zona; }
}
