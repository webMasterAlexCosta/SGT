package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Invalida somente consolidações cujo período e escopo são atingidos. */
@Service
public class InvalidacaoEmissaoRelatorioServico {
    private final EmissaoRelatorioAdministrativoRepositorio repositorio;
    private final RegistroAuditoriaServico auditoria;

    public InvalidacaoEmissaoRelatorioServico(EmissaoRelatorioAdministrativoRepositorio repositorio,
                                               RegistroAuditoriaServico auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Transactional
    public int invalidarPorFrequencia(Frequencia frequencia, String motivo, UsuarioAplicacao ator) {
        if (frequencia == null || frequencia.getDataReferencia() == null) return 0;
        return invalidar(frequencia.getDataReferencia(), frequencia.getContratoSnapshotId(),
                frequencia.getLotacaoSnapshotId(), frequencia.getJornadaSnapshotId(),
                frequencia.getGestaoLocalSnapshotId(), frequencia.getSuperintendenciaSnapshotId(),
                texto(motivo, "Retificação de frequência alterou a verdade operacional."), ator, true);
    }

    @Transactional
    public int invalidarPorVinculo(VinculoContratual vinculo, LocalDate desde, String motivo,
                                   UsuarioAplicacao ator) {
        if (vinculo == null || vinculo.getContrato() == null || desde == null) return 0;
        Contrato contrato = vinculo.getContrato();
        GestaoLocal gestao = contrato.getGestaoLocal();
        Long seId = gestao == null || gestao.getSuperintendencia() == null
                ? null : gestao.getSuperintendencia().getId();
        return invalidar(desde, contrato.getId(), null, null,
                gestao == null ? null : gestao.getId(), seId,
                texto(motivo, "Retificação de vínculo alterou a verdade operacional."), ator, false);
    }

    private int invalidar(LocalDate desde, Long contratoId, Long lotacaoId, Long jornadaId,
                          Long gestaoLocalId, Long superintendenciaId, String motivo,
                          UsuarioAplicacao ator, boolean dataExata) {
        int total = 0;
        for (EmissaoRelatorioAdministrativo emissao : repositorio.buscarVigentesAfetaveis(
                EstadoEmissaoRelatorio.VIGENTE, desde)) {
            if (dataExata && emissao.getInicio().isAfter(desde)) continue;
            if (!atingeEscopo(emissao, contratoId, lotacaoId, jornadaId, gestaoLocalId, superintendenciaId)) continue;
            emissao.setEstado(EstadoEmissaoRelatorio.SUPERADA);
            emissao.setSuperadaEm(Instant.now());
            emissao.setCausaSuperacao(CausaSuperacaoRelatorio.RETIFICACAO_FONTE);
            emissao.setMotivoSuperacao(motivo);
            repositorio.save(emissao);
            auditoria.registrarAcaoComoUsuario("RELATORIO_SUPERADO_POR_RETIFICACAO",
                    "EmissaoRelatorioAdministrativo", emissao.getId(),
                    emissao.getTipo() + " v" + emissao.getNumeroVersao(),
                    RegistroAuditoriaServico.detalhes("motivo", motivo, "contratoId", contratoId,
                            "dataEfetiva", desde, "hashPreservado", emissao.getHashConteudo()), ator);
            total++;
        }
        return total;
    }

    private boolean atingeEscopo(EmissaoRelatorioAdministrativo e, Long contratoId,
                                 Long lotacaoId, Long jornadaId, Long gestaoLocalId,
                                 Long superintendenciaId) {
        Set<Long> contratos = ids(e.getContratoIdsCsv());
        if (!contratos.isEmpty() && !contratos.contains(contratoId)) return false;
        if (e.getGestaoLocalEscopoId() != null
                && !Objects.equals(e.getGestaoLocalEscopoId(), gestaoLocalId)) return false;
        if (e.getSuperintendenciaEscopoId() != null
                && !Objects.equals(e.getSuperintendenciaEscopoId(), superintendenciaId)) return false;
        if (lotacaoId != null && e.getLotacaoId() != null && !Objects.equals(e.getLotacaoId(), lotacaoId)) return false;
        return jornadaId == null || e.getJornadaId() == null || Objects.equals(e.getJornadaId(), jornadaId);
    }

    private static Set<Long> ids(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        Set<Long> ids = new HashSet<>();
        for (String parte : csv.split(",")) {
            try { ids.add(Long.valueOf(parte.trim())); } catch (NumberFormatException ignored) { }
        }
        return ids;
    }
    private static String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
}
