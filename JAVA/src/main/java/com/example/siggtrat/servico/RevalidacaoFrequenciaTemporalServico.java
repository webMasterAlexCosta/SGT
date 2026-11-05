package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.FrequenciaExcecaoDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.FrequenciaRepositorio;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private static final int TAMANHO_LOTE = 500;
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final ResolvedorContextoTemporalFrequenciaServico resolvedor;
    private final AuditoriaFrequenciaServico auditoria;
    private final EntityManager entityManager;
    private PendenciaAdministrativaServico pendencias;
    private InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios;
    private FrequenciaServico frequenciaServico;

    public RevalidacaoFrequenciaTemporalServico(FrequenciaRepositorio frequenciaRepositorio,
                                                 ResolvedorContextoTemporalFrequenciaServico resolvedor,
                                                 AuditoriaFrequenciaServico auditoria,
                                                 EntityManager entityManager) {
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.resolvedor = resolvedor;
        this.auditoria = auditoria;
        this.entityManager = entityManager;
    }

    @Autowired(required = false)
    public void configurarPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    @Autowired(required = false)
    public void configurarInvalidacaoRelatorios(InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios) {
        this.invalidacaoRelatorios = invalidacaoRelatorios;
    }

    @Autowired
    public void configurarPoliticaFontes(FrequenciaServico frequenciaServico) {
        this.frequenciaServico = frequenciaServico;
    }

    @Transactional
    public int revalidarPorCalendario(CalendarioEvento anterior, CalendarioEvento atual,
            String motivo, UsuarioAplicacao autor) {
        Set<LocalDate> datas = new HashSet<>();
        if (anterior != null && anterior.getData() != null) datas.add(anterior.getData());
        if (atual != null && atual.getData() != null) datas.add(atual.getData());
        if (datas.isEmpty()) return 0;
        Specification<Frequencia> recorte = (root, query, cb) -> root.get("dataReferencia").in(datas);
        return revalidarFontes(recorte, f -> frequenciaServico.fonteCalendarioAlcancaFrequencia(anterior, f)
                || frequenciaServico.fonteCalendarioAlcancaFrequencia(atual, f), motivo, autor);
    }

    @Transactional
    public int revalidarPorExcecao(FrequenciaExcecaoDTO anterior, FrequenciaExcecaoDTO atual,
            String motivo, UsuarioAplicacao autor) {
        List<FrequenciaExcecaoDTO> fontes = new ArrayList<>();
        if (excecaoAtivaDeFolga(anterior)) fontes.add(anterior);
        if (excecaoAtivaDeFolga(atual)) fontes.add(atual);
        if (fontes.isEmpty()) return 0;
        Specification<Frequencia> recorte = (root, query, cb) -> cb.or(fontes.stream().map(f -> cb.and(
                cb.equal(root.get("empregado").get("id"), f.empregadoId()),
                cb.between(root.get("dataReferencia"), f.dataInicio(), f.dataFim())))
                .toArray(jakarta.persistence.criteria.Predicate[]::new));
        return revalidarFontes(recorte, f -> true, motivo, autor);
    }

    private boolean excecaoAtivaDeFolga(FrequenciaExcecaoDTO fonte) {
        return fonte != null && fonte.empregadoId() != null && fonte.dataInicio() != null && fonte.dataFim() != null
                && ConstantesDominio.EXCECAO_FOLGA_REMUNERADA.equalsIgnoreCase(fonte.tipo())
                && !ConstantesDominio.STATUS_CANCELADO.equalsIgnoreCase(fonte.status());
    }

    private int revalidarFontes(Specification<Frequencia> recorte,
            java.util.function.Predicate<Frequencia> atingida, String motivo, UsuarioAplicacao autor) {
        int total = 0;
        long ultimoId = 0;
        while (true) {
            long cursor = ultimoId;
            Specification<Frequencia> apos = (root, query, cb) -> cb.greaterThan(root.get("id"), cursor);
            List<Frequencia> lote = frequenciaRepositorio.findAll(recorte.and(apos),
                    PageRequest.of(0, TAMANHO_LOTE, Sort.by("id"))).getContent();
            if (lote.isEmpty()) return total;
            List<Object> produzidas = new ArrayList<>();
            for (Frequencia frequencia : lote) {
                if (frequencia.getEmpregado() == null || !atingida.test(frequencia)
                        || frequenciaServico.statusSustentadoPorFontes(frequencia)) continue;
                // A recuperação de outra fonte também não regulariza silenciosamente
                // uma pendência anterior. O Gestor confirma a verdade oficial.
                if (frequencia.getEstadoAuditoria() == EstadoAuditoriaFrequencia.SUJEITA_REVISAO
                        && frequencia.getValidadeOperacional() != ValidadeOperacionalFrequencia.VALIDA) continue;
                String antes = auditoria.snapshot(frequencia);
                ValidadeOperacionalFrequencia validade = frequencia.getValidadeOperacional() == ValidadeOperacionalFrequencia.INVALIDA_COBERTURA
                        ? ValidadeOperacionalFrequencia.INVALIDA_COBERTURA : ValidadeOperacionalFrequencia.SUJEITA_REVISAO;
                registrarImpacto(frequencia, validade, antes, normalizarMotivo(motivo,
                        "O status publicado perdeu sustentação nas fontes vigentes do dia e exige retificação do Gestor Local."),
                        autor, produzidas);
                total++;
            }
            ultimoId = lote.get(lote.size() - 1).getId();
            entityManager.flush();
            produzidas.forEach(entityManager::detach);
            lote.forEach(entityManager::detach);
        }
    }

    @Transactional
    public int revalidarPorVinculo(Long vinculoId, LocalDate desde, String motivo) {
        return revalidarPorVinculo(vinculoId, desde, null, motivo);
    }

    @Transactional
    public int revalidarPorVinculo(Long vinculoId, LocalDate desde, LocalDate ate, String motivo) {
        if (vinculoId == null || desde == null) return 0;
        int total = revalidarEmLotes(ultimoId -> frequenciaRepositorio.buscarPorVinculoApos(
                vinculoId, desde, ultimoId, PageRequest.of(0, TAMANHO_LOTE)), motivo);
        if (invalidacaoRelatorios != null) {
            invalidacaoRelatorios.invalidarPorVinculo(entityManager.find(VinculoContratual.class, vinculoId),
                    desde, ate, motivo, null);
        }
        return total;
    }

    @Transactional
    public int revalidarPorContrato(Long contratoId, LocalDate desde, String motivo) {
        if (contratoId == null || desde == null) return 0;
        int total = revalidarEmLotes(ultimoId -> frequenciaRepositorio.buscarPorContratoApos(
                contratoId, desde, ultimoId, PageRequest.of(0, TAMANHO_LOTE)), motivo);
        // SEM_LANCAMENTO e a população/previsão também são fontes de emissão.
        // A invalidação não depende de existir frequência no lote acima.
        if (invalidacaoRelatorios != null) {
            invalidacaoRelatorios.invalidarPorContrato(entityManager.find(Contrato.class, contratoId),
                    desde, null, motivo, null);
        }
        return total;
    }

    private int revalidarEmLotes(java.util.function.LongFunction<List<Frequencia>> consulta, String motivo) {
        int total = 0;
        long ultimoId = 0;
        while (true) {
            List<Frequencia> lote = consulta.apply(ultimoId);
            if (lote.isEmpty()) return total;
            total += revalidar(lote, motivo);
            ultimoId = lote.get(lote.size() - 1).getId();
        }
    }

    private int revalidar(List<Frequencia> frequencias, String motivo) {
        int alteradas = 0;
        List<Object> produzidas = new ArrayList<>();
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
                // Outro ato pode mudar a responsabilidade de uma revisão ainda
                // aberta sem mudar sua classificação nem exigir nova versão da fonte.
                if (pendencias != null) {
                    PendenciaAdministrativa pendencia = pendencias.registrarRevisaoFrequencia(frequencia, motivo);
                    if (pendencia != null) produzidas.add(pendencia);
                }
                continue;
            }
            registrarImpacto(frequencia, novaValidade, antes, normalizarMotivo(motivo, detalhe), null, produzidas);
            alteradas++;
        }
        entityManager.flush();
        // Mantém a transação funcional inteira: flush não é commit. Não usa clear,
        // pois Contrato/Vínculo do comando chamador devem continuar gerenciados.
        produzidas.forEach(entityManager::detach);
        frequencias.forEach(entityManager::detach);
        return alteradas;
    }

    private void registrarImpacto(Frequencia frequencia, ValidadeOperacionalFrequencia validade,
            String antes, String motivo, UsuarioAplicacao autor, List<Object> produzidas) {
        frequencia.setValidadeOperacional(validade);
        frequencia.setEstadoAuditoria(EstadoAuditoriaFrequencia.SUJEITA_REVISAO);
        frequencia.setBloqueadaEdicaoOperacional(true);
        frequencia.setMotivoRevisao(motivo);
        frequencia.setVersaoNegocial(frequencia.getVersaoNegocial() == null ? 2 : frequencia.getVersaoNegocial() + 1);
        frequenciaRepositorio.save(frequencia);
        EventoFrequencia evento = auditoria.registrar(frequencia, TipoEventoFrequencia.IMPACTO_TEMPORAL, antes,
                motivo, autor, null, null);
        if (evento != null) produzidas.add(evento);
        if (pendencias != null) {
            PendenciaAdministrativa pendencia = pendencias.registrarRevisaoFrequencia(frequencia, motivo);
            if (pendencia != null) produzidas.add(pendencia);
        }
        if (invalidacaoRelatorios != null) invalidacaoRelatorios.invalidarPorFrequencia(frequencia, motivo, autor);
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
                && Objects.equals(normalizar(frequencia.getContratoSnapshotNumero()), normalizar(numeroHistorico))
                && configuracaoComprovadaCoincide(frequencia, atual.configuracaoAlocacao());
    }

    private boolean configuracaoComprovadaCoincide(Frequencia frequencia,
            ConsultaProvaHistoricaContratoServico.Configuracao configuracao) {
        if (configuracao == null) return true;
        // A falta de fonte nova não contradiz uma fotografia própria existente.
        // Quando a fonte do mesmo recorte é comprovada, a comparação é do dia
        // atingido: corrigir outro dia da semana não altera esta frequência.
        if (configuracao.municipioComprovado()
                && !Objects.equals(municipioComparavel(frequencia.getLotacaoSnapshotMunicipio()),
                                   municipioComparavel(configuracao.lotacaoMunicipio()))) return false;
        if (normalizar(configuracao.jornadaSemanal()) == null || frequencia.getDataReferencia() == null) return true;
        var dia = com.example.siggtrat.utilitario.FotografiaJornada.dias(configuracao.jornadaSemanal()).stream()
                .filter(d -> d.diaSemana() == frequencia.getDataReferencia().getDayOfWeek()).findFirst().orElse(null);
        if (dia == null) return true;
        if (!Objects.equals(frequencia.getJornadaSnapshotDiaTrabalho(), dia.diaTrabalho())) return false;
        return !dia.diaTrabalho()
                || Objects.equals(horarioComparavel(frequencia.getJornadaSnapshotEntrada1()), horarioComparavel(dia.entrada1()))
                && Objects.equals(horarioComparavel(frequencia.getJornadaSnapshotSaida1()), horarioComparavel(dia.saida1()))
                && Objects.equals(horarioComparavel(frequencia.getJornadaSnapshotEntrada2()), horarioComparavel(dia.entrada2()))
                && Objects.equals(horarioComparavel(frequencia.getJornadaSnapshotSaida2()), horarioComparavel(dia.saida2()));
    }

    private static String municipioComparavel(String valor) {
        String texto = normalizar(valor);
        return texto == null ? null : texto.toUpperCase(Locale.ROOT);
    }

    private static String horarioComparavel(String valor) {
        String texto = normalizar(valor);
        if (texto == null) return null;
        try { return java.time.LocalTime.parse(texto).toString(); }
        catch (java.time.format.DateTimeParseException ex) { return texto; }
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
