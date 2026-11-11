package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.pendencia.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Projeção operacional auditável das incompatibilidades que exigem ação humana.
 * A origem negocial continua nas entidades temporais; a Central não inventa nem
 * modifica fatos negociais e limita cada bloqueio ao recorte explicitado.
 */
@Service
public class PendenciaAdministrativaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(PendenciaAdministrativaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private static final int TAMANHO_MAXIMO = 500;

    private final PendenciaAdministrativaRepositorio repositorio;
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final RegistroAuditoriaServico auditoria;
    private final GestaoContratoTemporalServico gestaoTemporal;
    private final ResolvedorEscopoProvaFrequenciaServico escopoProvaFrequencia;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final EventoCicloVidaContratoRepositorio eventoContratoRepositorio;
    private final EntityManager entityManager;
    private final MotorTemporalContratoServico motorContrato;

    public PendenciaAdministrativaServico(
            PendenciaAdministrativaRepositorio repositorio,
            FrequenciaRepositorio frequenciaRepositorio,
            VinculoContratualRepositorio vinculoRepositorio,
            ContratoRepositorio contratoRepositorio,
            AcessoOrganizacionalServico acesso,
            MinimizadorDadosPessoaisServico minimizador,
            RegistroAuditoriaServico auditoria,
            GestaoContratoTemporalServico gestaoTemporal,
            ResolvedorEscopoProvaFrequenciaServico escopoProvaFrequencia,
            ComposicaoContratualTemporalServico composicaoTemporal,
            EventoCicloVidaContratoRepositorio eventoContratoRepositorio,
            EntityManager entityManager,
            MotorTemporalContratoServico motorContrato
    ) {
        this.repositorio = repositorio;
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.acesso = acesso;
        this.minimizador = minimizador;
        this.auditoria = auditoria;
        this.gestaoTemporal = gestaoTemporal;
        this.escopoProvaFrequencia = escopoProvaFrequencia;
        this.composicaoTemporal = composicaoTemporal;
        this.eventoContratoRepositorio = eventoContratoRepositorio;
        this.entityManager = entityManager;
        this.motorContrato = motorContrato;
    }

    @Transactional
    public PendenciaAdministrativa registrarRevisaoFrequencia(Frequencia frequencia, String motivo) {
        if (frequencia == null || frequencia.getId() == null) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Frequência persistida é obrigatória para gerar pendência.");
        }
        bloquearOrigemFrequencia(frequencia);
        ContextoComplementoFrequenciaDTO responsabilidade = resolverResponsabilidadeFrequencia(frequencia);
        // Cada versão conserva sua tarefa e história. Enquanto ainda estiver
        // ativa, a tarefa acompanha quem pode tratar a mesma fonte hoje.
        for (PendenciaAdministrativa anterior : repositorio
                .findByOrigemEntidadeAndOrigemIdAndEstadoNot("Frequencia", frequencia.getId(), EstadoPendencia.RESOLVIDA)) {
            if (anterior.getTipo() == TipoPendencia.RETIFICACAO_FREQUENCIA)
                atualizarResponsabilidadeFrequencia(anterior, responsabilidade);
        }
        int versao = frequencia.getVersaoNegocial() == null ? 1 : frequencia.getVersaoNegocial();
        String chave = "FREQUENCIA:" + frequencia.getId() + ":VERSAO:" + versao;
        Optional<PendenciaAdministrativa> existente = repositorio.findByChaveOrigem(chave);
        if (existente.isPresent()) {
            PendenciaAdministrativa item = existente.get();
            if (item.getEstado() != EstadoPendencia.RESOLVIDA) atualizarResponsabilidadeFrequencia(item, responsabilidade);
            return item;
        }

        PendenciaAdministrativa item = new PendenciaAdministrativa();
        item.setChaveOrigem(chave);
        item.setTipo(TipoPendencia.RETIFICACAO_FREQUENCIA);
        item.setEstado(EstadoPendencia.ABERTA);
        item.setMotivo(texto(motivo, "Frequência atingida por alteração temporal e sujeita à revisão gerencial."));
        item.setDataEfetiva(frequencia.getDataReferencia());
        item.setOperacaoOrigem("IMPACTO_TEMPORAL_FREQUENCIA");
        item.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL);
        item.setEscopoBloqueio(EscopoBloqueioPendencia.FREQUENCIA_PERIODO);
        item.setContratoId(frequencia.getContratoSnapshotId());
        item.setContratoFotografia(frequencia.getContratoSnapshotNumero());
        item.setPessoaId(frequencia.getPessoa() == null ? null : frequencia.getPessoa().getId());
        item.setPessoaFotografia(frequencia.getEmpregadoSnapshotNome() == null
                || frequencia.getEmpregadoSnapshotNome().isBlank() ? null : frequencia.getEmpregadoSnapshotNome());
        item.setVinculoId(frequencia.getVinculoContratual() == null ? null : frequencia.getVinculoContratual().getId());
        item.setFrequenciaId(frequencia.getId());
        atualizarResponsabilidadeFrequencia(item, responsabilidade);
        item.setOrigemEntidade("Frequencia");
        item.setOrigemId(frequencia.getId());
        return salvarSemDuplicar(item);
    }

    private ContextoComplementoFrequenciaDTO resolverResponsabilidadeFrequencia(Frequencia frequencia) {
        // A pendência é uma projeção da tarefa atual. A fotografia da Frequencia e
        // seus eventos continuam íntegros, mesmo quando a decisão retroativa muda
        // quem pode conferir e corrigir o fato.
        var fotografia = new ContextoComplementoFrequenciaDTO(frequencia.getDataReferencia(),
                frequencia.getContratoSnapshotId(),
                frequencia.getVinculoContratual() == null ? null : frequencia.getVinculoContratual().getId(),
                frequencia.getAlocacaoVinculo() == null ? null : frequencia.getAlocacaoVinculo().getId(),
                frequencia.getGestaoLocalSnapshotId(), frequencia.getSuperintendenciaSnapshotId(),
                frequencia.getLotacaoSnapshotId(), frequencia.getJornadaSnapshotId());
        return escopoProvaFrequencia.realinhar(fotografia, Instant.now());
    }

    private void atualizarResponsabilidadeFrequencia(PendenciaAdministrativa item,
                                                     ContextoComplementoFrequenciaDTO efetivo) {
        // Prova insuficiente não atribui silenciosamente a tarefa à organização
        // materializada do empregado ou a um recorte histórico já superado.
        item.setLotacaoId(efetivo == null ? null : efetivo.lotacaoId());
        item.setJornadaId(efetivo == null ? null : efetivo.jornadaId());
        item.setGestaoLocalId(efetivo == null ? null : efetivo.gestaoLocalId());
        item.setSuperintendenciaId(efetivo == null ? null : efetivo.superintendenciaId());
    }

    /** Retorna null quando não existe fim natural comprovado e pendente na data atual. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PendenciaAdministrativa registrarFimNatural(VinculoContratual vinculo) {
        if (vinculo == null || vinculo.getId() == null || vinculo.getContrato() == null) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Vínculo persistido e contrato são obrigatórios para gerar pendência.");
        }
        bloquearFonteFimNatural(vinculo);
        LocalDate fim = fimNaturalPendente(vinculo, RelogioSistema.hoje());
        return fim == null ? null : registrarFimNatural(vinculo, fim);
    }

    private PendenciaAdministrativa registrarFimNatural(VinculoContratual vinculo, LocalDate fim) {
        Contrato contrato = vinculo.getContrato();
        Optional<PendenciaAdministrativa> ativa = pendenciaFimNaturalAtiva(vinculo, fim);
        if (ativa.isPresent()) return ativa.get();
        String chave = "FIM_VIGENCIA_VINCULO:" + vinculo.getId() + ":" + fim;
        if (repositorio.findByChaveOrigem(chave).isPresent()) {
            // A causa pode voltar após outros atos documentados. Sua resolução
            // anterior é imutável; uma nova ocorrência tem identidade própria.
            // Todos os produtores mantêm o lock do contrato durante a busca e
            // a inserção, inclusive quando duas sincronizações concorrem.
            chave += ":RECORRENCIA:" + UUID.randomUUID();
        }

        GestaoLocal gestao = gestaoTemporal.gestaoEm(contrato, fim, Instant.now());
        SuperintendenciaEstadual se = gestao == null ? null : gestao.getSuperintendencia();
        PendenciaAdministrativa item = new PendenciaAdministrativa();
        item.setChaveOrigem(chave);
        item.setTipo(TipoPendencia.FIM_NATURAL_VIGENCIA);
        item.setEstado(EstadoPendencia.ABERTA);
        item.setMotivo("Vínculo permaneceu aberto após o fim natural da vigência contratual.");
        item.setDataEfetiva(fim);
        item.setOperacaoOrigem("FIM_NATURAL_CONTRATO");
        item.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL);
        item.setEscopoBloqueio(EscopoBloqueioPendencia.OPERACAO_VINCULO);
        item.setContratoId(contrato.getId());
        item.setContratoFotografia(identificacaoContrato(contrato));
        item.setPessoaId(vinculo.getPessoa() == null ? null : vinculo.getPessoa().getId());
        item.setPessoaFotografia(vinculo.getPessoa() == null ? null : vinculo.getPessoa().getNome());
        item.setVinculoId(vinculo.getId());
        item.setGestaoLocalId(gestao == null ? null : gestao.getId());
        item.setSuperintendenciaId(se == null ? null : se.getId());
        item.setOrigemEntidade("VinculoContratual");
        item.setOrigemId(vinculo.getId());
        return salvarSemDuplicar(item);
    }

    private Optional<PendenciaAdministrativa> pendenciaFimNaturalAtiva(VinculoContratual vinculo, LocalDate fim) {
        return repositorio.findByOrigemEntidadeAndOrigemIdAndEstadoNot(
                        "VinculoContratual", vinculo.getId(), EstadoPendencia.RESOLVIDA).stream()
                .filter(p -> p.getTipo() == TipoPendencia.FIM_NATURAL_VIGENCIA
                        && Objects.equals(p.getContratoId(), vinculo.getContrato().getId())
                        && Objects.equals(p.getDataEfetiva(), fim))
                .findFirst();
    }

    @Transactional
    public PendenciaAdministrativa registrarConflitoReplay(
            Contrato contrato, ComposicaoContratualTemporalServico.ConflitoReplay conflito,
            String operacaoOrigem) {
        if (contrato == null || contrato.getId() == null || conflito == null || conflito.eventoId() == null)
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("Contrato e conflito temporal persistidos são obrigatórios.");
        bloquearContrato(contrato.getId());
        String chave = "CONFLITO_REPLAY:" + contrato.getId() + ":EVENTO:" + conflito.eventoId();
        Optional<PendenciaAdministrativa> existente = repositorio.findByChaveOrigem(chave);
        if (existente.isPresent()) {
            if (existente.get().getEstado() != EstadoPendencia.RESOLVIDA) return existente.get();
            Optional<PendenciaAdministrativa> ativa = repositorio
                    .findByOrigemEntidadeAndOrigemIdAndEstadoNot(
                            "EventoCicloVidaContrato", conflito.eventoId(), EstadoPendencia.RESOLVIDA).stream()
                    .filter(p -> p.getTipo() == TipoPendencia.CONFLITO_REPLAY_TEMPORAL
                            && Objects.equals(p.getContratoId(), contrato.getId()))
                    .findFirst();
            if (ativa.isPresent()) return ativa.get();
            // A recorrência é outra tarefa. A resolução anterior conserva sua
            // autoria, data e observação, e o lock do contrato impede duplicatas.
            chave += ":RECORRENCIA:" + UUID.randomUUID();
        }
        GestaoLocal gestao = gestaoTemporal.gestaoEm(contrato, conflito.dataEfetiva(), Instant.now());
        SuperintendenciaEstadual se = gestao == null ? null : gestao.getSuperintendencia();
        PendenciaAdministrativa item = new PendenciaAdministrativa();
        item.setChaveOrigem(chave); item.setTipo(TipoPendencia.CONFLITO_REPLAY_TEMPORAL);
        item.setEstado(EstadoPendencia.ABERTA); item.setMotivo(conflito.motivo());
        item.setDataEfetiva(conflito.dataEfetiva()); item.setOperacaoOrigem(operacaoOrigem);
        item.setPerfilResponsavel(PerfilAcesso.GESTOR_LOCAL);
        item.setEscopoBloqueio(EscopoBloqueioPendencia.CONTRATO_TEMPORAL);
        item.setContratoId(contrato.getId()); item.setContratoFotografia(identificacaoContrato(contrato));
        item.setGestaoLocalId(gestao == null ? null : gestao.getId());
        item.setSuperintendenciaId(se == null ? null : se.getId());
        item.setOrigemEntidade("EventoCicloVidaContrato"); item.setOrigemId(conflito.eventoId());
        return salvarSemDuplicar(item);
    }

    @Transactional
    public int sincronizarRevisoesFrequencia(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        int criadas = 0;
        for (Frequencia frequencia : frequenciaRepositorio
                .findByEstadoAuditoriaAndDataReferenciaBetweenOrderByDataReferenciaAscIdAsc(
                        EstadoAuditoriaFrequencia.SUJEITA_REVISAO, inicio, fim)) {
            bloquearOrigemFrequencia(frequencia);
            String chave = "FREQUENCIA:" + frequencia.getId() + ":VERSAO:"
                    + (frequencia.getVersaoNegocial() == null ? 1 : frequencia.getVersaoNegocial());
            boolean nova = repositorio.findByChaveOrigem(chave).isEmpty();
            registrarRevisaoFrequencia(frequencia, frequencia.getMotivoRevisao());
            if (nova) criadas++;
        }
        return criadas;
    }

    @Scheduled(cron = "${app.pendencias.fim-vigencia.cron:0 10 0 * * *}", zone = "America/Sao_Paulo")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int sincronizarFimNatural() {
        return sincronizarFimNaturalNoRecorte(List.of());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int sincronizarFimNatural(Collection<Long> contratoIds) {
        return sincronizarFimNaturalNoRecorte(idsNormalizados(contratoIds));
    }

    private int sincronizarFimNaturalNoRecorte(List<Long> contratoIds) {
        int criadas = 0;
        LocalDate hoje = RelogioSistema.hoje();
        long ultimoContrato = 0, ultimoVinculo = 0;
        while (true) {
            // A seleção é apenas conservadora. Datas de versões/instrumentos
            // também tornam o vínculo candidato; somente o replay sob lock
            // comprova o fim. Keyset não perde candidatos se outra transação
            // fechar vínculos enquanto o lote aguarda o contrato.
            // O recorte precisa participar da seleção antes da paginação.
            // Consultas filtradas não devem bloquear contratos alheios; a
            // execução agendada e consultas sem filtro continuam globais.
            List<VinculoContratual> lote = contratoIds.isEmpty()
                    ? vinculoRepositorio.buscarCandidatosFimNatural(
                            hoje, ultimoContrato, ultimoVinculo, PageRequest.of(0, 200))
                    : vinculoRepositorio.buscarCandidatosFimNaturalNoRecorte(
                            contratoIds, hoje, ultimoContrato, ultimoVinculo, PageRequest.of(0, 200));
            if (lote.isEmpty()) break;
            for (VinculoContratual vinculo : lote) {
                ultimoContrato = vinculo.getContrato().getId();
                ultimoVinculo = vinculo.getId();
                bloquearFonteFimNatural(vinculo);
                LocalDate fim = fimNaturalPendente(vinculo, hoje);
                if (fim == null) continue;
                if (pendenciaFimNaturalAtiva(vinculo, fim).isEmpty()) {
                    registrarFimNatural(vinculo, fim);
                    criadas++;
                }
            }
            if (lote.size() < 200) break;
        }
        return criadas;
    }

    private void bloquearFonteFimNatural(VinculoContratual vinculo) {
        Contrato contrato = vinculo.getContrato();
        bloquearContrato(contrato.getId());
        // A seleção anterior ao lock não autoriza usar estado já ultrapassado
        // por uma prorrogação ou por um desligamento concorrente confirmado.
        if (entityManager.contains(contrato)) entityManager.refresh(contrato);
        if (entityManager.contains(vinculo)) entityManager.refresh(vinculo);
    }

    private LocalDate fimNaturalPendente(VinculoContratual vinculo, LocalDate hoje) {
        if (vinculo.getDataFim() != null || vinculo.getEstadoNegocial()
                != com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO) return null;
        var posicao = motorContrato.reconstruirPosicao(vinculo.getContrato(), hoje, Instant.now());
        if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || posicao.vigenciaFim() == null || !posicao.vigenciaFim().isBefore(hoje)
                || posicao.eventosAplicados().stream().anyMatch(e ->
                        e.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO)) return null;
        return posicao.vigenciaFim();
    }

    @Transactional(readOnly = true)
    public PaginaPendenciasDTO listar(EstadoPendencia estado, TipoPendencia tipo,
                                      LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
                                      Integer pagina, Integer tamanho) {
        UsuarioAplicacao usuario = usuarioLeitor();
        Pageable pageable = PageRequest.of(normalizarPagina(pagina), normalizarTamanho(tamanho),
                Sort.by(Sort.Direction.DESC, "surgidaEm").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<PendenciaAdministrativa> resultado = repositorio.findAll(
                especificacao(usuario, estado, tipo, inicio, fim, contratoIds, null, null), pageable);
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new PaginaPendenciasDTO(
                resultado.getContent().stream().map(item -> paraDto(item, nivel)).toList(),
                resultado.getTotalElements(), resultado.getTotalPages(), resultado.getNumber(), resultado.getSize(),
                resultado.isFirst(), resultado.isLast());
    }

    @Transactional(readOnly = true)
    public ResumoPendenciasDTO resumo(LocalDate inicio, LocalDate fim, Collection<Long> contratoIds) {
        UsuarioAplicacao usuario = usuarioLeitor();
        return new ResumoPendenciasDTO(
                contar(usuario, EstadoPendencia.ABERTA, null, inicio, fim, contratoIds, null, null),
                contar(usuario, EstadoPendencia.EM_TRATAMENTO, null, inicio, fim, contratoIds, null, null),
                contar(usuario, EstadoPendencia.RESOLVIDA, null, inicio, fim, contratoIds, null, null),
                contarAtivas(usuario, TipoPendencia.RETIFICACAO_FREQUENCIA, inicio, fim, contratoIds),
                contarAtivas(usuario, TipoPendencia.FIM_NATURAL_VIGENCIA, inicio, fim, contratoIds));
    }

    @Transactional(readOnly = true)
    public AvisoRevisaoRelatorioDTO avisoRelatorio(LocalDate inicio, LocalDate fim,
                                                   Collection<Long> contratoIds,
                                                   Long lotacaoId, Long jornadaId) {
        return avisoRelatorioNoEscopo(inicio, fim, contratoIds, lotacaoId, jornadaId, null, null);
    }

    @Transactional(readOnly = true)
    public AvisoRevisaoRelatorioDTO avisoRelatorioNoEscopo(LocalDate inicio, LocalDate fim,
            Collection<Long> contratoIds, Long lotacaoId, Long jornadaId,
            Long gestaoLocalId, Long superintendenciaId) {
        validarPeriodo(inicio, fim);
        // Comandos e sincronizador materializam a projeção. Consultar relatório
        // não deve escrever nem tentar elevar uma transação readOnly para escrita.
        UsuarioAplicacao usuario = usuarioLeitor();
        Specification<PendenciaAdministrativa> base = especificacao(
                usuario, null, TipoPendencia.RETIFICACAO_FREQUENCIA,
                inicio, fim, contratoIds, lotacaoId, jornadaId);
        // Mantém os predicados da autorização atual e acrescenta o recorte da
        // emissão. Restringir a contagem não concede novos direitos ao leitor.
        if (gestaoLocalId != null) base = base.and((root, query, cb) ->
                cb.equal(root.get("gestaoLocalId"), gestaoLocalId));
        if (superintendenciaId != null) base = base.and((root, query, cb) ->
                cb.equal(root.get("superintendenciaId"), superintendenciaId));
        Specification<PendenciaAdministrativa> ativas = base.and((root, query, cb) ->
                root.get("estado").in(EstadoPendencia.ABERTA, EstadoPendencia.EM_TRATAMENTO));
        return AvisoRevisaoRelatorioDTO.de(repositorio.count(ativas));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PendenciaDTO iniciarTratamento(Long id, String observacao) {
        UsuarioAplicacao usuario = gestorResponsavel(id);
        PendenciaAdministrativa item = obter(id);
        if (item.getEstado() == EstadoPendencia.RESOLVIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pendência já resolvida.");
        }
        item.setEstado(EstadoPendencia.EM_TRATAMENTO);
        if (item.getIniciadaEm() == null) {
            item.setIniciadaEm(Instant.now());
            item.setIniciadaPorId(usuario.getId());
            item.setIniciadaPor(usuario.getUsername());
        }
        item.setObservacaoTratamento(normalizar(observacao));
        item = repositorio.save(item);
        auditar("PENDENCIA_INICIAR_TRATAMENTO", item, usuario);
        return paraDto(item, minimizador.nivelPadrao(usuario));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PendenciaDTO confirmarResolucao(Long id, String observacao) {
        UsuarioAplicacao usuario = gestorResponsavel(id);
        PendenciaAdministrativa item = obter(id);
        validarOrigemResolvida(item);
        resolver(item, usuario, observacao);
        return paraDto(item, minimizador.nivelPadrao(usuario));
    }

    @Transactional
    public int resolverRevisoesFrequencia(Long frequenciaId, UsuarioAplicacao usuario, String observacao) {
        return resolverPorOrigem("Frequencia", frequenciaId, usuario, observacao);
    }

    @Transactional
    public int resolverFimNaturalContrato(Long contratoId, UsuarioAplicacao usuario, String observacao) {
        if (contratoId == null) return 0;
        int total = 0;
        for (PendenciaAdministrativa item : repositorio.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tipo"), TipoPendencia.FIM_NATURAL_VIGENCIA),
                cb.equal(root.get("contratoId"), contratoId),
                root.get("estado").in(EstadoPendencia.ABERTA, EstadoPendencia.EM_TRATAMENTO)))) {
            resolver(item, usuario, observacao);
            total++;
        }
        return total;
    }

    private int resolverPorOrigem(String entidade, Long origemId, UsuarioAplicacao usuario, String observacao) {
        if (origemId == null) return 0;
        int total = 0;
        for (PendenciaAdministrativa item : repositorio
                .findByOrigemEntidadeAndOrigemIdAndEstadoNot(entidade, origemId, EstadoPendencia.RESOLVIDA)) {
            resolver(item, usuario, observacao);
            total++;
        }
        return total;
    }

    private void validarOrigemResolvida(PendenciaAdministrativa item) {
        if (item.getEstado() == EstadoPendencia.RESOLVIDA) return;
        if (item.getTipo() == TipoPendencia.RETIFICACAO_FREQUENCIA) {
            Frequencia f = frequenciaRepositorio.findById(item.getFrequenciaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "A frequência de origem não está disponível para validação."));
            if (f.getEstadoAuditoria() == EstadoAuditoriaFrequencia.SUJEITA_REVISAO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A retificação da frequência ainda não foi concluída.");
            }
        } else if (item.getTipo() == TipoPendencia.FIM_NATURAL_VIGENCIA) {
            VinculoContratual v = vinculoRepositorio.findById(item.getVinculoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "O vínculo de origem não está disponível para validação."));
            if (entityManager.contains(v)) entityManager.refresh(v);
            if (v.getDataFim() == null && !fimDaPendenciaSuperado(item, v)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O vínculo continua aberto; trate o fim natural antes de resolver a pendência.");
            }
        } else if (item.getTipo() == TipoPendencia.CONFLITO_REPLAY_TEMPORAL) {
            if (item.getEstado() != EstadoPendencia.EM_TRATAMENTO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O conflito temporal deve ser analisado antes da confirmação da resolução.");
            }
            validarConflitoReplayResolvido(item);
        }
    }

    private boolean fimDaPendenciaSuperado(PendenciaAdministrativa item, VinculoContratual vinculo) {
        if (item.getDataEfetiva() == null || vinculo.getContrato() == null
                || !Objects.equals(item.getContratoId(), vinculo.getContrato().getId())) return false;
        // confirmarResolucao já bloqueou o contrato e releu a autorização. Uma
        // tarefa legada pode ter sido gerada pelo master anterior à prorrogação.
        // A confirmação humana resolve apenas essa projeção: nunca encerra o
        // vínculo, altera a origem da tarefa ou dispensa outra causa ainda ativa.
        LocalDate hoje = RelogioSistema.hoje();
        var posicao = motorContrato.reconstruirPosicao(vinculo.getContrato(), hoje, Instant.now());
        if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || posicao.vigenciaFim() == null
                || posicao.eventosAplicados().stream().anyMatch(e ->
                        e.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO)) return false;
        // Ausência de prova não é causa resolvida. O fim conhecido deve provar
        // que a tarefa aponta para outra data, ou que esse fim ainda não ocorreu.
        return !Objects.equals(item.getDataEfetiva(), posicao.vigenciaFim())
                || !posicao.vigenciaFim().isBefore(hoje);
    }

    private void validarConflitoReplayResolvido(PendenciaAdministrativa item) {
        if (!"EventoCicloVidaContrato".equals(item.getOrigemEntidade())
                || item.getContratoId() == null || item.getOrigemId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A origem do conflito temporal não está disponível para validação.");
        }
        EventoCicloVidaContrato origem = eventoContratoRepositorio.findById(item.getOrigemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "O evento de origem do conflito temporal não está disponível para validação."));
        Instant conhecimento = Instant.now();
        if (origem.getContrato() == null || !Objects.equals(origem.getContrato().getId(), item.getContratoId())
                || origem.getTipoEvento() != TipoEventoCicloVidaContrato.ADITAMENTO
                && origem.getTipoEvento() != TipoEventoCicloVidaContrato.REMANEJAMENTO
                && origem.getTipoEvento() != TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL
                || origem.getRegistradoEm() != null && origem.getRegistradoEm().isAfter(conhecimento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A origem informada não comprova o conflito temporal deste contrato.");
        }
        // Inclui fatos já programados: limitar o diagnóstico a hoje permitiria
        // encerrar administrativamente uma incompatibilidade que continua futura.
        boolean causaPermanece = composicaoTemporal.diagnosticarConflitos(
                        origem.getContrato(), LocalDate.MAX, conhecimento).stream()
                .anyMatch(conflito -> Objects.equals(conflito.eventoId(), origem.getId()));
        if (causaPermanece) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A incompatibilidade temporal da origem continua presente; retifique ou cancele o fato antes de resolver a pendência.");
        }
    }

    private UsuarioAplicacao usuarioLeitor() {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        if (!acesso.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu perfil não possui acesso à Central de Pendências.");
        }
        return usuario;
    }

    private UsuarioAplicacao gestorResponsavel(Long pendenciaId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        if (!acesso.ehGestaoLocal(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente o Gestor Local responsável pode tratar pendências negociais.");
        }
        PendenciaAdministrativa item = obter(pendenciaId);
        Long contratoId = item.getContratoId();
        Contrato contrato = bloquearContrato(contratoId);
        // A mesma ordem de lock dos atos temporais serializa a decisão com
        // transferência/retificação. Releitura após a espera evita autorizar
        // pela fotografia do primeiro nível JPA anterior ao commit concorrente.
        entityManager.refresh(contrato, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(usuario);
        if (!Boolean.TRUE.equals(usuario.getActive()) || !acesso.ehGestaoLocal(usuario)
                || !acesso.podeAcessarRelatoriosGestao(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A autorização atual não permite tratar pendências negociais.");
        }
        if (!Objects.equals(contratoId, item.getContratoId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A origem da pendência mudou; consulte novamente antes de tratar.");
        }

        Long gestaoResponsavel;
        if (item.getTipo() == TipoPendencia.RETIFICACAO_FREQUENCIA) {
            Frequencia frequencia = item.getFrequenciaId() == null ? null
                    : frequenciaRepositorio.findById(item.getFrequenciaId()).orElse(null);
            if (frequencia != null) entityManager.refresh(frequencia);
            var responsabilidade = frequencia == null ? null : resolverResponsabilidadeFrequencia(frequencia);
            gestaoResponsavel = responsabilidade != null
                    && Objects.equals(contratoId, responsabilidade.contratoId())
                    && Objects.equals(item.getDataEfetiva(), responsabilidade.dataReferencia())
                    ? responsabilidade.gestaoLocalId() : null;
        } else {
            GestaoLocal gestao = item.getDataEfetiva() == null ? null
                    : gestaoTemporal.gestaoEm(contrato, item.getDataEfetiva(), Instant.now());
            gestaoResponsavel = gestao == null ? null : gestao.getId();
        }
        // O responsável pelo dia da prova conserva a tarefa histórica mesmo
        // que uma transferência posterior tenha mudado a Gestão atual. Uma
        // projeção desatualizada da pendência nunca concede permissão sozinha.
        if (gestaoResponsavel == null || usuario.getGestaoLocal() == null
                || !Objects.equals(usuario.getGestaoLocal().getId(), gestaoResponsavel)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pendência não encontrada no seu escopo.");
        }
        return usuario;
    }

    private PendenciaAdministrativa obter(Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pendência não encontrada."));
    }

    private Specification<PendenciaAdministrativa> especificacao(
            UsuarioAplicacao usuario, EstadoPendencia estado, TipoPendencia tipo,
            LocalDate inicio, LocalDate fim, Collection<Long> contratoIds,
            Long lotacaoId, Long jornadaId) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período de consulta inválido.");
        }
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> filtros = new ArrayList<>();
            if (acesso.ehGestorEstadual(usuario)) {
                SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(usuario);
                filtros.add(cb.equal(root.get("superintendenciaId"), se == null ? -1L : se.getId()));
            } else if (acesso.ehGestaoLocal(usuario)) {
                filtros.add(cb.equal(root.get("gestaoLocalId"),
                        usuario.getGestaoLocal() == null ? -1L : usuario.getGestaoLocal().getId()));
            } else if (!acesso.ehAdministrador(usuario) && !acesso.ehGestorNacional(usuario)
                    && !acesso.ehPresidente(usuario)) {
                filtros.add(cb.disjunction());
            }
            if (estado != null) filtros.add(cb.equal(root.get("estado"), estado));
            if (tipo != null) filtros.add(cb.equal(root.get("tipo"), tipo));
            if (inicio != null) filtros.add(cb.greaterThanOrEqualTo(root.get("dataEfetiva"), inicio));
            if (fim != null) filtros.add(cb.lessThanOrEqualTo(root.get("dataEfetiva"), fim));
            List<Long> ids = idsNormalizados(contratoIds);
            if (!ids.isEmpty()) filtros.add(root.get("contratoId").in(ids));
            if (lotacaoId != null) filtros.add(cb.equal(root.get("lotacaoId"), lotacaoId));
            if (jornadaId != null) filtros.add(cb.equal(root.get("jornadaId"), jornadaId));
            return cb.and(filtros.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private long contar(UsuarioAplicacao usuario, EstadoPendencia estado, TipoPendencia tipo,
                        LocalDate inicio, LocalDate fim, Collection<Long> contratos,
                        Long lotacaoId, Long jornadaId) {
        return repositorio.count(especificacao(usuario, estado, tipo, inicio, fim, contratos, lotacaoId, jornadaId));
    }

    private long contarAtivas(UsuarioAplicacao usuario, TipoPendencia tipo,
                              LocalDate inicio, LocalDate fim, Collection<Long> contratos) {
        Specification<PendenciaAdministrativa> spec = especificacao(
                usuario, null, tipo, inicio, fim, contratos, null, null)
                .and((root, query, cb) -> root.get("estado").in(
                        EstadoPendencia.ABERTA, EstadoPendencia.EM_TRATAMENTO));
        return repositorio.count(spec);
    }

    private PendenciaDTO paraDto(PendenciaAdministrativa item, NivelExposicaoDadosPessoais nivel) {
        String acao = switch (item.getTipo()) {
            case FIM_NATURAL_VIGENCIA -> "Tratar fim natural do contrato e confirmar a data final do vínculo.";
            case CONFLITO_REPLAY_TEMPORAL -> "Revisar e retificar/cancelar o fato posterior incompatível.";
            default -> "Concluir a retificação da frequência pelo Gestor Local.";
        };
        return new PendenciaDTO(item.getId(), item.getTipo(), item.getEstado(), minimizador.restrito(item.getMotivo(), nivel),
                item.getSurgidaEm(), item.getDataEfetiva(), item.getOperacaoOrigem(), item.getPerfilResponsavel(),
                item.getEscopoBloqueio(), item.getContratoId(), item.getContratoFotografia(), item.getPessoaId(),
                minimizador.nome(item.getPessoaFotografia(), nivel), item.getVinculoId(), item.getFrequenciaId(),
                item.getGestaoLocalId(), item.getSuperintendenciaId(), item.getOrigemEntidade(), item.getOrigemId(),
                item.getIniciadaEm(), item.getIniciadaPor(), item.getResolvidaEm(), item.getResolvidaPor(),
                minimizador.restrito(item.getObservacaoTratamento(), nivel), acao);
    }

    private PendenciaAdministrativa salvarSemDuplicar(PendenciaAdministrativa item) {
        // O lock da origem precede a consulta pela chave. Uma falha inesperada
        // deve abortar também o comando negocial; nunca consultar em tx inválida.
        PendenciaAdministrativa salva = repositorio.saveAndFlush(item);
        auditoria.registrarAcao("PENDENCIA_GERADA", "PendenciaAdministrativa", salva.getId(),
                salva.getTipo().name(), RegistroAuditoriaServico.detalhes(
                        "origem", salva.getOrigemEntidade(), "origemId", salva.getOrigemId(),
                        "escopoBloqueio", salva.getEscopoBloqueio(), "contratoId", salva.getContratoId()));
        return salva;
    }

    private Contrato bloquearContrato(Long contratoId) {
        if (contratoId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contrato de origem da pendência indisponível.");
        }
        return contratoRepositorio.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Contrato de origem da pendência indisponível."));
    }

    private void bloquearOrigemFrequencia(Frequencia frequencia) {
        if (frequencia.getContratoSnapshotId() != null) {
            bloquearContrato(frequencia.getContratoSnapshotId());
        } else if (frequenciaRepositorio.buscarPorIdParaAtualizacao(frequencia.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Frequência de origem da pendência indisponível.");
        }
    }

    private void resolver(PendenciaAdministrativa item, UsuarioAplicacao usuario, String observacao) {
        if (item.getEstado() == EstadoPendencia.RESOLVIDA) return;
        item.setEstado(EstadoPendencia.RESOLVIDA);
        item.setResolvidaEm(Instant.now());
        item.setResolvidaPorId(usuario == null ? null : usuario.getId());
        item.setResolvidaPor(usuario == null ? "sistema" : usuario.getUsername());
        String texto = normalizar(observacao);
        if (texto != null) item.setObservacaoTratamento(texto);
        repositorio.save(item);
        auditar("PENDENCIA_RESOLVIDA", item, usuario);
    }

    private void auditar(String acao, PendenciaAdministrativa item, UsuarioAplicacao usuario) {
        auditoria.registrarAcaoComoUsuario(acao, "PendenciaAdministrativa", item.getId(), item.getTipo().name(),
                RegistroAuditoriaServico.detalhes("estado", item.getEstado(), "origem", item.getOrigemEntidade(),
                        "origemId", item.getOrigemId(), "contratoId", item.getContratoId()), usuario);
    }

    private static List<Long> idsNormalizados(Collection<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static int normalizarPagina(Integer pagina) { return pagina == null ? 0 : Math.max(0, pagina); }
    private static int normalizarTamanho(Integer tamanho) {
        return Math.min(TAMANHO_MAXIMO, tamanho == null ? 50 : Math.max(1, tamanho));
    }
    private static String identificacaoContrato(Contrato contrato) {
        if (contrato == null) return null;
        String numero = contrato.getNumeroContrato() == null ? "Contrato " + contrato.getId() : contrato.getNumeroContrato();
        return contrato.getAnoContrato() == null ? numero : numero + "/" + contrato.getAnoContrato();
    }
    private static String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }
    private static String normalizar(String valor) {
        if (valor == null) return null;
        String n = valor.trim().replaceAll("\\s+", " ");
        return n.isBlank() ? null : n;
    }
    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período de pendências inválido.");
        }
        if (inicio.plusYears(2).isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período de pendências não pode ultrapassar dois anos.");
        }
    }
}
