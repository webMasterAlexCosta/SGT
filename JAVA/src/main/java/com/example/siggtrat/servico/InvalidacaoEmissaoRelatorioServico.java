package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.relatorio.*;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio;
import com.example.siggtrat.repositorio.EmissaoRelatorioAdministrativoRepositorio.CandidatoInvalidacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Invalida somente consolidações cujo período e escopo são atingidos. */
@Service
public class InvalidacaoEmissaoRelatorioServico {
    private static final int TAMANHO_LOTE = 200;
    private final EmissaoRelatorioAdministrativoRepositorio repositorio;
    private final RegistroAuditoriaServico auditoria;
    private GestaoContratoTemporalServico gestaoTemporal;
    private MotorTemporalAlocacaoServico motorAlocacao;
    private ComposicaoContratualTemporalServico composicaoTemporal;
    private EntityManager entityManager;
    private RelatorioFrequenciaConsolidadoServico apuracao;

    @Autowired
    public void configurarApuracaoCalendario(RelatorioFrequenciaConsolidadoServico apuracao) {
        this.apuracao = apuracao;
    }

    public InvalidacaoEmissaoRelatorioServico(EmissaoRelatorioAdministrativoRepositorio repositorio,
                                               RegistroAuditoriaServico auditoria) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
    }

    @Autowired
    public void configurarContextoTemporal(GestaoContratoTemporalServico gestaoTemporal,
                                            MotorTemporalAlocacaoServico motorAlocacao,
                                            ComposicaoContratualTemporalServico composicaoTemporal,
                                            EntityManager entityManager) {
        this.gestaoTemporal = gestaoTemporal;
        this.motorAlocacao = motorAlocacao;
        this.composicaoTemporal = composicaoTemporal;
        this.entityManager = entityManager;
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
        return invalidarPorVinculo(vinculo, desde, null, motivo, ator);
    }

    @Transactional
    public int invalidarPorVinculo(VinculoContratual vinculo, LocalDate desde, LocalDate ate,
                                   String motivo, UsuarioAplicacao ator) {
        if (vinculo == null) return 0;
        return invalidarFonteTemporal(vinculo.getContrato(), vinculo, desde, ate,
                texto(motivo, "Retificação de vínculo alterou a verdade operacional."), ator);
    }

    @Transactional
    public int invalidarPorContrato(Contrato contrato, LocalDate desde, LocalDate ate,
                                    String motivo, UsuarioAplicacao ator) {
        return invalidarFonteTemporal(contrato, null, desde, ate,
                texto(motivo, "Retificação contratual alterou a verdade operacional."), ator);
    }

    /** Calendário já adquiriu todas as raízes Contrato do escopo, antes da alteração. */
    @Transactional
    public int invalidarPorCalendario(CalendarioEvento anterior, CalendarioEvento novo,
            String motivo, UsuarioAplicacao ator) {
        if (anterior == null && novo == null || mesmoEfeitoCalendario(anterior, novo)) return 0;
        LocalDate desde = anterior == null ? novo.getData() : novo == null ? anterior.getData()
                : anterior.getData().isBefore(novo.getData()) ? anterior.getData() : novo.getData();
        LocalDate ate = anterior == null ? novo.getData() : novo == null ? anterior.getData()
                : anterior.getData().isAfter(novo.getData()) ? anterior.getData() : novo.getData();
        int total = 0;
        long ultimoId = 0L;
        while (true) {
            List<CandidatoInvalidacao> lote = repositorio.buscarCandidatasInvalidacaoEmLote(
                    EstadoEmissaoRelatorio.VIGENTE, desde, ate, ultimoId, PageRequest.of(0, TAMANHO_LOTE));
            if (lote.isEmpty()) return total;
            List<EmissaoRelatorioAdministrativo> alteradas = new ArrayList<>();
            for (CandidatoInvalidacao candidata : lote) {
                if (!atingeCalendario(candidata, anterior) && !atingeCalendario(candidata, novo)) continue;
                if (!apuracao.calendarioAlteraResultado(candidata, anterior, novo)) continue;
                EmissaoRelatorioAdministrativo emissao = repositorio.findById(candidata.getId()).orElse(null);
                if (emissao == null) continue;
                entityManager.refresh(emissao, LockModeType.PESSIMISTIC_WRITE);
                if (emissao.getEstado() != EstadoEmissaoRelatorio.VIGENTE) {
                    entityManager.detach(emissao); continue;
                }
                superar(emissao, null, desde, texto(motivo, "Retificação do calendário alterou o resultado consolidado."), ator);
                alteradas.add(emissao); total++;
            }
            ultimoId = lote.get(lote.size() - 1).getId();
            if (!alteradas.isEmpty()) { entityManager.flush(); alteradas.forEach(entityManager::detach); }
        }
    }

    private boolean atingeCalendario(CandidatoInvalidacao e, CalendarioEvento fonte) {
        if (fonte == null || !Boolean.TRUE.equals(fonte.getAtivo()) || fonte.isCancelado()
                || fonte.getData().isBefore(e.getInicio()) || fonte.getData().isAfter(e.getFim())) return false;
        if (!Set.of("FERIADO", "DIA_DESCANSO").contains(fonte.getTipo())) return false;
        Long gestao = fonte.getGestaoLocal() == null ? null : fonte.getGestaoLocal().getId();
        Long se = fonte.getSuperintendencia() == null ? null : fonte.getSuperintendencia().getId();
        if (e.getGestaoLocalEscopoId() != null && gestao != null && !Objects.equals(e.getGestaoLocalEscopoId(), gestao)) return false;
        if (e.getSuperintendenciaEscopoId() != null && se != null && !Objects.equals(e.getSuperintendenciaEscopoId(), se)) return false;
        return e.getLotacaoId() == null || fonte.getLotacaoId() == null || Objects.equals(e.getLotacaoId(), fonte.getLotacaoId());
    }

    private boolean mesmoEfeitoCalendario(CalendarioEvento a, CalendarioEvento b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.getData(), b.getData()) && Objects.equals(a.getTipo(), b.getTipo())
                && Objects.equals(a.getAbrangencia(), b.getAbrangencia()) && Objects.equals(a.getUf(), b.getUf())
                && Objects.equals(a.getMunicipio(), b.getMunicipio()) && Objects.equals(a.getLotacaoId(), b.getLotacaoId())
                && Objects.equals(a.getGestaoLocal() == null ? null : a.getGestaoLocal().getId(),
                        b.getGestaoLocal() == null ? null : b.getGestaoLocal().getId())
                && Objects.equals(a.getSuperintendencia() == null ? null : a.getSuperintendencia().getId(),
                        b.getSuperintendencia() == null ? null : b.getSuperintendencia().getId())
                && Objects.equals(a.getAtivo(), b.getAtivo()) && a.isCancelado() == b.isCancelado();
    }

    /** O chamador já detém o lock da raiz Contrato; aqui não se adquire outra raiz. */
    private int invalidarFonteTemporal(Contrato contrato, VinculoContratual vinculo,
                                       LocalDate desde, LocalDate ate, String motivo,
                                       UsuarioAplicacao ator) {
        if (contrato == null || contrato.getId() == null || desde == null
                || ate != null && ate.isBefore(desde)) return 0;
        Instant conhecimentoAtual = Instant.now();
        List<MotorTemporalAlocacaoServico.FaixaProjetada> faixasAtuais = vinculo == null ? List.of()
                : motorAlocacao.reconstruirFaixas(vinculo, conhecimentoAtual);
        int total = 0;
        long ultimoId = 0;
        while (true) {
            List<CandidatoInvalidacao> lote = repositorio.buscarCandidatasInvalidacaoEmLote(
                    EstadoEmissaoRelatorio.VIGENTE, desde, ate, ultimoId, PageRequest.of(0, TAMANHO_LOTE));
            if (lote.isEmpty()) return total;
            List<EmissaoRelatorioAdministrativo> alteradas = new ArrayList<>();
            for (CandidatoInvalidacao candidata : lote) {
                if (!atingeFonteTemporal(candidata, contrato, vinculo, desde, ate,
                        conhecimentoAtual, faixasAtuais)) continue;
                EmissaoRelatorioAdministrativo emissao = repositorio.findById(candidata.getId()).orElse(null);
                if (emissao == null) continue;
                // Somente candidatas realmente atingidas recebem lock, em ordem
                // de ID. Uma superação concorrente não é reescrita com outra causa.
                entityManager.refresh(emissao, LockModeType.PESSIMISTIC_WRITE);
                if (emissao.getEstado() != EstadoEmissaoRelatorio.VIGENTE) {
                    entityManager.detach(emissao);
                    continue;
                }
                superar(emissao, contrato.getId(), desde, motivo, ator);
                alteradas.add(emissao);
                total++;
            }
            ultimoId = lote.get(lote.size() - 1).getId();
            if (!alteradas.isEmpty()) {
                entityManager.flush();
                // Libera somente os arquivos das emissões processadas. Contrato,
                // Vínculo e demais alterações do comando continuam gerenciados.
                alteradas.forEach(entityManager::detach);
            }
        }
    }

    private boolean atingeFonteTemporal(CandidatoInvalidacao emissao, Contrato contrato,
                                         VinculoContratual vinculo, LocalDate desde, LocalDate ate,
                                         Instant conhecimentoAtual,
                                         List<MotorTemporalAlocacaoServico.FaixaProjetada> faixasAtuais) {
        Set<Long> contratos = ids(emissao.getContratoIdsCsv());
        if (!contratos.isEmpty() && !contratos.contains(contrato.getId())) return false;
        LocalDate inicio = maior(desde, emissao.getInicio());
        LocalDate fim = menor(ate, emissao.getFim());
        if (inicio.isAfter(fim)) return false;
        Instant conhecimentoOriginal = emissao.getFonteConhecidaAte() == null
                ? conhecimentoAtual : emissao.getFonteConhecidaAte();
        if (vinculo == null) {
            return atingeContratoNoIntervalo(emissao, contrato, inicio, fim, conhecimentoOriginal)
                    || atingeContratoNoIntervalo(emissao, contrato, inicio, fim, conhecimentoAtual);
        }
        // A alocação anterior pode estar cancelada/encerrada na projeção atual.
        // A visão conhecida na emissão preserva o recorte que foi retirado.
        return atingeFaixas(emissao, contrato, inicio, fim, conhecimentoOriginal,
                    motorAlocacao.reconstruirFaixas(vinculo, conhecimentoOriginal))
                || atingeFaixas(emissao, contrato, inicio, fim, conhecimentoAtual, faixasAtuais);
    }

    private boolean atingeContratoNoIntervalo(CandidatoInvalidacao emissao, Contrato contrato,
                                              LocalDate inicio, LocalDate fim, Instant conhecimento) {
        if (emissao.getLotacaoId() == null && emissao.getJornadaId() == null)
            return atingeGestaoNoIntervalo(emissao, contrato, inicio, fim, conhecimento);
        for (LocalDate data : gestaoTemporal.datasDeMudancaNoIntervalo(contrato, inicio, fim, conhecimento)) {
            GestaoLocal gestao = gestaoTemporal.gestaoEm(contrato, data, conhecimento);
            if (!atingeGestao(emissao, gestao)) continue;
            for (var composicao : composicaoTemporal.projetarEntidades(contrato, data, conhecimento)) {
                if (composicao.lotacao() == null || composicao.jornada() == null) continue;
                if (emissao.getLotacaoId() != null
                        && !Objects.equals(emissao.getLotacaoId(), composicao.lotacao().getId())) continue;
                if (emissao.getJornadaId() != null
                        && !Objects.equals(emissao.getJornadaId(), composicao.jornada().getId())) continue;
                if (gestao != null && composicao.lotacao().getGestaoLocal() != null
                        && Objects.equals(gestao.getId(), composicao.lotacao().getGestaoLocal().getId())) return true;
            }
        }
        return false;
    }

    private boolean atingeFaixas(CandidatoInvalidacao emissao, Contrato contrato,
                                 LocalDate inicio, LocalDate fim, Instant conhecimento,
                                 List<MotorTemporalAlocacaoServico.FaixaProjetada> faixas) {
        for (var faixa : faixas) {
            if (faixa.estado() != EstadoNegocialAlocacao.REGISTRADA || faixa.dataInicio() == null) continue;
            if (emissao.getLotacaoId() != null && !Objects.equals(emissao.getLotacaoId(), faixa.lotacaoId())) continue;
            if (emissao.getJornadaId() != null && !Objects.equals(emissao.getJornadaId(), faixa.jornadaId())) continue;
            LocalDate inicioComum = maior(inicio, faixa.dataInicio());
            LocalDate fimComum = menor(fim, faixa.dataFim());
            if (!inicioComum.isAfter(fimComum)
                    && atingeGestaoNoIntervalo(emissao, contrato, inicioComum, fimComum, conhecimento)) return true;
        }
        return false;
    }

    private boolean atingeGestaoNoIntervalo(CandidatoInvalidacao emissao, Contrato contrato,
                                            LocalDate inicio, LocalDate fim, Instant conhecimento) {
        if (emissao.getGestaoLocalEscopoId() == null && emissao.getSuperintendenciaEscopoId() == null) return true;
        return gestaoTemporal.gestoesNoIntervalo(contrato, inicio, fim, conhecimento).stream()
                .anyMatch(gestao -> atingeGestao(emissao, gestao));
    }

    private boolean atingeGestao(CandidatoInvalidacao emissao, GestaoLocal gestao) {
        return gestao != null && (emissao.getGestaoLocalEscopoId() == null
                        || Objects.equals(emissao.getGestaoLocalEscopoId(), gestao.getId()))
                && (emissao.getSuperintendenciaEscopoId() == null
                        || gestao.getSuperintendencia() != null && Objects.equals(emissao.getSuperintendenciaEscopoId(),
                        gestao.getSuperintendencia().getId()));
    }

    private int invalidar(LocalDate desde, Long contratoId, Long lotacaoId, Long jornadaId,
                          Long gestaoLocalId, Long superintendenciaId, String motivo,
                          UsuarioAplicacao ator, boolean dataExata) {
        int total = 0;
        for (EmissaoRelatorioAdministrativo emissao : repositorio.buscarVigentesAfetaveis(
                EstadoEmissaoRelatorio.VIGENTE, desde)) {
            if (dataExata && emissao.getInicio().isAfter(desde)) continue;
            if (!atingeEscopo(emissao, contratoId, lotacaoId, jornadaId, gestaoLocalId, superintendenciaId)) continue;
            superar(emissao, contratoId, desde, motivo, ator);
            total++;
        }
        return total;
    }

    private void superar(EmissaoRelatorioAdministrativo emissao, Long contratoId, LocalDate desde,
                          String motivo, UsuarioAplicacao ator) {
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
    private static LocalDate maior(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate menor(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }
}
