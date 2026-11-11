package com.example.siggtrat.servico;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.OcorrenciaRepositorio;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Política comum de classificação: fontes explícitas do recorte, sem adquirir autoridade de Gestor. */
@Service
public class ClassificacaoOcorrenciaFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ClassificacaoOcorrenciaFrequenciaServico.class);
    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    public record Resultado(String status, String observacao, boolean sobreposta, String statusBase,
                            String statusDerivado, List<Long> fontes) {}
    private final OcorrenciaRepositorio ocorrencias;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final ResolvedorEscopoProvaFrequenciaServico provas;
    private final ResolvedorContextoTemporalFrequenciaServico contextos;

    public ClassificacaoOcorrenciaFrequenciaServico(OcorrenciaRepositorio ocorrencias,
            AlocacaoVinculoRepositorio alocacoes, ResolvedorEscopoProvaFrequenciaServico provas,
            ResolvedorContextoTemporalFrequenciaServico contextos) {
        this.ocorrencias = ocorrencias; this.alocacoes = alocacoes; this.provas = provas; this.contextos = contextos;
    }

    public Resultado paraNovaFrequencia(Frequencia frequencia, ResolvedorContextoTemporalFrequenciaServico.Contexto contexto) {
        Instant conhecimento = Objects.requireNonNull(contexto.posicaoVinculo().conhecidoAte(), "Contexto sem corte de conhecimento");
        List<Ocorrencia> fontes = ocorrencias.buscarFontesRegistradasDoDia(
                frequencia.getEmpregado().getId(), frequencia.getDataReferencia(), conhecimento);
        return calcular(frequencia, contexto, fontes);
    }

    public Resultado paraComandoOcorrencia(Frequencia frequencia, Empregado empregado, LocalDate dia,
                                           List<Ocorrencia> candidatas) {
        return calcular(frequencia, contextos.resolver(empregado, dia), candidatas);
    }

    private Resultado calcular(Frequencia frequencia, ResolvedorContextoTemporalFrequenciaServico.Contexto contexto,
                                List<Ocorrencia> candidatas) {
        List<Ocorrencia> fontes = candidatas.stream().filter(o -> {
            boolean compativel = compativel(o, frequencia.getDataReferencia(), contexto);
            if (!compativel) LOG_DIAGNOSTICO.debug("Ocorrência {} não classifica Frequência em {} no Vínculo {}: fonte fora do recorte comprovado",
                    o == null ? null : o.getId(), frequencia.getDataReferencia(), contexto.vinculo().getId());
            return compativel;
        }).sorted(Comparator.comparingInt(this::prioridade)
                .thenComparing(Ocorrencia::getDataOcorrencia, Comparator.reverseOrder())
                .thenComparing(Ocorrencia::getId, Comparator.reverseOrder())).toList();
        String novoStatus = fontes.stream().map(o -> status(o.getMotivo())).filter(Objects::nonNull).findFirst().orElse(null);
        String observacaoManual = removerNotas(frequencia.getObservacao());
        List<String> marcadores = fontes.stream().map(o -> rotulo(o.getMotivo()) + " (Ocorrência ID: " + o.getId() + ")").toList();
        StringJoiner observacao = new StringJoiner(" | ");
        if (observacaoManual != null) observacao.add(observacaoManual);
        marcadores.forEach(observacao::add);
        String statusFinal = frequencia.getStatus(), base = frequencia.getStatusBaseAntesOcorrencia();
        boolean sobreposta = Boolean.TRUE.equals(frequencia.getOcorrenciaOverrideAtivo());
        if (novoStatus != null) {
            if (!sobreposta) { base = statusFinal; sobreposta = true; }
            statusFinal = novoStatus;
        } else if (sobreposta) { statusFinal = base; base = null; sobreposta = false; }
        return new Resultado(statusFinal, observacao.length() == 0 ? null : observacao.toString(), sobreposta, base,
                novoStatus, fontes.stream().map(Ocorrencia::getId).toList());
    }

    private boolean compativel(Ocorrencia o, LocalDate dia, ResolvedorContextoTemporalFrequenciaServico.Contexto contexto) {
        Instant conhecimento = contexto.posicaoVinculo().conhecidoAte();
        if (o == null || o.getId() == null || o.getCanceladoEm() != null || !abrange(o, dia)
                || o.getRegistradoEm() == null || o.getRegistradoPorId() == null
                || o.getRegistradoEm().isAfter(conhecimento) || o.getEmpregado() == null
                || o.getEmpregado().getPessoaCanonica() == null
                || !Objects.equals(o.getEmpregado().getPessoaCanonica().getId(), contexto.pessoa().getId())) return false;
        List<ContextoOcorrencia> fotos = o.getContextos().stream().filter(c -> Objects.equals(c.getDataReferencia(), dia)).toList();
        if (fotos.size() != 1) return false;
        ContextoOcorrencia foto = fotos.get(0);
        if (!Objects.equals(foto.getContratoId(), contexto.vinculo().getContrato().getId())
                || !Objects.equals(foto.getVinculoId(), contexto.vinculo().getId()) || foto.getAlocacaoId() == null) return false;
        AlocacaoVinculo origem = alocacoes.findById(foto.getAlocacaoId()).orElse(null);
        if (origem == null || !Objects.equals(origem.getVinculo().getId(), foto.getVinculoId())
                || !Objects.equals(origem.getVinculo().getPessoa().getId(), contexto.pessoa().getId())
                || !Objects.equals(origem.getVinculo().getContrato().getId(), foto.getContratoId())
                || !Objects.equals(origem.getLotacao().getId(), foto.getLotacaoId())
                || !Objects.equals(origem.getJornada().getId(), foto.getJornadaId())) return false;
        var prova = provas.realinhar(new ContextoComplementoFrequenciaDTO(dia, foto.getContratoId(), foto.getVinculoId(),
                foto.getAlocacaoId(), foto.getGestaoLocalId(), foto.getSuperintendenciaId(), foto.getLotacaoId(), foto.getJornadaId()), conhecimento);
        return prova != null && Objects.equals(prova.contratoId(), contexto.vinculo().getContrato().getId())
                && Objects.equals(prova.vinculoId(), contexto.vinculo().getId())
                && Objects.equals(prova.alocacaoId(), contexto.alocacao().getId())
                && Objects.equals(prova.gestaoLocalId(), contexto.gestaoHistorica().getId())
                && Objects.equals(prova.superintendenciaId(), contexto.gestaoHistorica().getSuperintendencia().getId())
                && Objects.equals(prova.lotacaoId(), contexto.posicaoAlocacao().lotacaoId())
                && Objects.equals(prova.jornadaId(), contexto.posicaoAlocacao().jornadaId());
    }

    public boolean mudou(Frequencia f, Resultado r) {
        return !Objects.equals(f.getStatus(), r.status()) || !Objects.equals(f.getObservacao(), r.observacao())
                || Boolean.TRUE.equals(f.getOcorrenciaOverrideAtivo()) != r.sobreposta()
                || !Objects.equals(f.getStatusBaseAntesOcorrencia(), r.statusBase());
    }
    public void aplicar(Frequencia f, Resultado r) {
        // Batidas, horas e memória do cálculo físico permanecem exatamente como estavam.
        f.setStatus(r.status()); f.setObservacao(r.observacao());
        f.setOcorrenciaOverrideAtivo(r.sobreposta()); f.setStatusBaseAntesOcorrencia(r.statusBase());
    }
    private boolean abrange(Ocorrencia o, LocalDate dia) {
        if (o.getDataOcorrencia() == null || dia == null) return false;
        int dias = o.getQuantidadeDias() != null && o.getQuantidadeDias() > 0 ? o.getQuantidadeDias() : 1;
        return !dia.isBefore(o.getDataOcorrencia()) && !dia.isAfter(o.getDataOcorrencia().plusDays(dias - 1L));
    }
    private int prioridade(Ocorrencia o) {
        String valor = status(o.getMotivo());
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(valor)) return 0;
        if (ConstantesDominio.FREQ_FALTA.equals(valor)) return 1;
        if (ConstantesDominio.FREQ_ATESTADO.equals(valor)) return 2;
        if (ConstantesDominio.FREQ_ATRASO.equals(valor)) return 3;
        return 10;
    }
    public String status(String motivo) {
        String valor = normalizarMotivo(motivo);
        if (ConstantesDominio.FREQ_FALTA.equals(valor)) return ConstantesDominio.FREQ_FALTA;
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(valor)) return ConstantesDominio.FREQ_SUSPENSAO;
        if (ConstantesDominio.FREQ_ATESTADO.equals(valor) || ConstantesDominio.OCORRENCIA_LICENCA_MEDICA.equals(valor)) return ConstantesDominio.FREQ_ATESTADO;
        if (ConstantesDominio.FREQ_ATRASO.equals(valor)) return ConstantesDominio.FREQ_ATRASO;
        return null;
    }
    private String rotulo(String motivo) {
        String valor = normalizarMotivo(motivo);
        if (ConstantesDominio.FREQ_FALTA.equals(valor)) return "Falta";
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(valor)) return "Suspensão";
        if (ConstantesDominio.FREQ_ATESTADO.equals(valor) || ConstantesDominio.OCORRENCIA_LICENCA_MEDICA.equals(valor)) return "Atestado/Abono";
        if (ConstantesDominio.FREQ_ATRASO.equals(valor)) return "Atraso registrado";
        return "Evento";
    }
    private String normalizarMotivo(String motivo) { return motivo == null ? null : motivo.trim().toUpperCase(Locale.ROOT); }
    private String removerNotas(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String regex = "(?i)(^|\\s*\\|\\s*)(" + rotulo(ConstantesDominio.FREQ_FALTA) + "|"
                + rotulo(ConstantesDominio.FREQ_SUSPENSAO) + "|" + rotulo(ConstantesDominio.FREQ_ATESTADO) + "|"
                + rotulo(ConstantesDominio.FREQ_ATRASO) + ") \\(Ocorrência ID: \\d+\\)";
        String limpa = valor.replaceAll(regex, "").replaceAll("\\s*\\|\\s*\\|\\s*", " | ")
                .replaceAll("^\\s*\\|\\s*", "").replaceAll("\\s*\\|\\s*$", "").trim();
        return limpa.isBlank() ? null : limpa;
    }
}
