package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Propaga impactos retroativos de Contrato/Vínculo/Alocação para frequências já
 * materializadas. O conteúdo original nunca é apagado nem reassociado em
 * silêncio: a frequência passa a revisão gerencial ou perde validade
 * operacional, mantendo-se consultável e auditável.
 */
@Service
public class RevalidacaoFrequenciaTemporalServico {
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final ResolvedorContextoTemporalFrequenciaServico resolvedor;
    private final AuditoriaFrequenciaServico auditoria;
    private PendenciaAdministrativaServico pendencias;
    private InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios;

    public RevalidacaoFrequenciaTemporalServico(FrequenciaRepositorio frequenciaRepositorio,
                                                 ResolvedorContextoTemporalFrequenciaServico resolvedor,
                                                 AuditoriaFrequenciaServico auditoria) {
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.resolvedor = resolvedor;
        this.auditoria = auditoria;
    }

    @Autowired(required = false)
    public void configurarPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    @Autowired(required = false)
    public void configurarInvalidacaoRelatorios(InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios) {
        this.invalidacaoRelatorios = invalidacaoRelatorios;
    }

    @Transactional
    public int revalidarPorVinculo(Long vinculoId, LocalDate desde, String motivo) {
        if (vinculoId == null || desde == null) return 0;
        return revalidar(frequenciaRepositorio
                .findByVinculoContratualIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(vinculoId, desde),
                motivo);
    }

    @Transactional
    public int revalidarPorContrato(Long contratoId, LocalDate desde, String motivo) {
        if (contratoId == null || desde == null) return 0;
        return revalidar(frequenciaRepositorio
                .findByContratoSnapshotIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(contratoId, desde),
                motivo);
    }

    private int revalidar(List<Frequencia> frequencias, String motivo) {
        int alteradas = 0;
        for (Frequencia frequencia : frequencias) {
            if (frequencia == null || frequencia.getEmpregado() == null) continue;
            String antes = auditoria.snapshot(frequencia);
            ValidadeOperacionalFrequencia novaValidade;
            String detalhe;
            try {
                var atual = resolvedor.resolver(frequencia.getEmpregado(), frequencia.getDataReferencia());
                if (contextoCoincide(frequencia, atual)) {
                    if (frequencia.getValidadeOperacional() == ValidadeOperacionalFrequencia.VALIDA
                            && frequencia.getEstadoAuditoria() != EstadoAuditoriaFrequencia.SUJEITA_REVISAO) {
                        continue;
                    }
                    // Ex.: reativação retroativa restabeleceu a cobertura. A volta à visão
                    // oficial exige confirmação gerencial; nunca é restaurada silenciosamente.
                    novaValidade = ValidadeOperacionalFrequencia.SUJEITA_REVISAO;
                    detalhe = "A cobertura temporal voltou a ser compatível, mas exige confirmação do Gestor Local.";
                } else {
                    novaValidade = ValidadeOperacionalFrequencia.SUJEITA_REVISAO;
                    detalhe = "O contexto histórico atual diverge do Contrato/Vínculo/Alocação gravado na frequência.";
                }
            } catch (ResponseStatusException ex) {
                novaValidade = ValidadeOperacionalFrequencia.INVALIDA_COBERTURA;
                detalhe = "O fato retroativo retirou cobertura operacional válida de Contrato/Vínculo/Alocação.";
            }

            if (frequencia.getEstadoAuditoria() == EstadoAuditoriaFrequencia.SUJEITA_REVISAO
                    && frequencia.getValidadeOperacional() == novaValidade) {
                continue;
            }
            frequencia.setValidadeOperacional(novaValidade);
            frequencia.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
            frequencia.setBloqueadaEdicaoOperacional(true);
            frequencia.setMotivoRevisao(normalizarMotivo(motivo, detalhe));
            frequencia.setVersaoNegocial(frequencia.getVersaoNegocial() == null ? 2 : frequencia.getVersaoNegocial() + 1);
            frequenciaRepositorio.saveAndFlush(frequencia);
            auditoria.registrar(frequencia, TipoEventoFrequencia.IMPACTO_TEMPORAL, antes,
                    frequencia.getMotivoRevisao(), null, null, null);
            if (pendencias != null) {
                pendencias.registrarRevisaoFrequencia(frequencia, frequencia.getMotivoRevisao());
            }
            if (invalidacaoRelatorios != null) {
                invalidacaoRelatorios.invalidarPorFrequencia(frequencia, frequencia.getMotivoRevisao(), null);
            }
            alteradas++;
        }
        return alteradas;
    }

    private boolean contextoCoincide(Frequencia frequencia,
                                     ResolvedorContextoTemporalFrequenciaServico.Contexto atual) {
        String numeroHistorico = atual.posicaoContrato().versaoContratual() == null
                ? null : atual.posicaoContrato().versaoContratual().numeroContrato();
        return Objects.equals(id(frequencia.getVinculoContratual()), atual.vinculo().getId())
                && Objects.equals(id(frequencia.getAlocacaoVinculo()), atual.alocacao().getId())
                && Objects.equals(frequencia.getContratoSnapshotId(), atual.vinculo().getContrato().getId())
                && Objects.equals(frequencia.getLotacaoSnapshotId(), atual.posicaoAlocacao().lotacaoId())
                && Objects.equals(frequencia.getJornadaSnapshotId(), atual.posicaoAlocacao().jornadaId())
                && Objects.equals(frequencia.getCargoSnapshotId(), atual.posicaoAlocacao().cargoId())
                && Objects.equals(frequencia.getGestaoLocalSnapshotId(),
                                  atual.gestaoHistorica() == null ? null : atual.gestaoHistorica().getId())
                && Objects.equals(normalizar(frequencia.getContratoSnapshotNumero()), normalizar(numeroHistorico));
    }

    private static String normalizarMotivo(String motivo, String detalhe) {
        String base = motivo == null || motivo.isBlank() ? "Retificação temporal" : motivo.trim();
        return base + ". " + detalhe;
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim();
        return normalizado.isBlank() ? null : normalizado;
    }

    private static Long id(VinculoContratual v) { return v == null ? null : v.getId(); }
    private static Long id(AlocacaoVinculo a) { return a == null ? null : a.getId(); }
}
