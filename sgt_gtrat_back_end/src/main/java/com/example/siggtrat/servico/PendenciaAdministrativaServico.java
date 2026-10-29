package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.pendencia.*;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
public class PendenciaAdministrativaServico {
    private static final int TAMANHO_MAXIMO = 500;

    private final PendenciaAdministrativaRepositorio repositorio;
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MinimizadorDadosPessoaisServico minimizador;
    private final RegistroAuditoriaServico auditoria;

    public PendenciaAdministrativaServico(
            PendenciaAdministrativaRepositorio repositorio,
            FrequenciaRepositorio frequenciaRepositorio,
            VinculoContratualRepositorio vinculoRepositorio,
            ContratoRepositorio contratoRepositorio,
            AcessoOrganizacionalServico acesso,
            MinimizadorDadosPessoaisServico minimizador,
            RegistroAuditoriaServico auditoria
    ) {
        this.repositorio = repositorio;
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.acesso = acesso;
        this.minimizador = minimizador;
        this.auditoria = auditoria;
    }

    @Transactional
    public PendenciaAdministrativa registrarRevisaoFrequencia(Frequencia frequencia, String motivo) {
        if (frequencia == null || frequencia.getId() == null) {
            throw new IllegalArgumentException("Frequência persistida é obrigatória para gerar pendência.");
        }
        int versao = frequencia.getVersaoNegocial() == null ? 1 : frequencia.getVersaoNegocial();
        String chave = "FREQUENCIA:" + frequencia.getId() + ":VERSAO:" + versao;
        Optional<PendenciaAdministrativa> existente = repositorio.findByChaveOrigem(chave);
        if (existente.isPresent()) return existente.get();

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
        item.setPessoaFotografia(frequencia.getEmpregadoSnapshotNome() != null
                ? frequencia.getEmpregadoSnapshotNome()
                : frequencia.getEmpregado() == null ? null : frequencia.getEmpregado().getNome());
        item.setVinculoId(frequencia.getVinculoContratual() == null ? null : frequencia.getVinculoContratual().getId());
        item.setFrequenciaId(frequencia.getId());
        item.setLotacaoId(frequencia.getLotacaoSnapshotId());
        item.setJornadaId(frequencia.getJornadaSnapshotId());
        item.setGestaoLocalId(frequencia.getGestaoLocalSnapshotId());
        item.setSuperintendenciaId(frequencia.getSuperintendenciaSnapshotId());
        item.setOrigemEntidade("Frequencia");
        item.setOrigemId(frequencia.getId());
        return salvarSemDuplicar(item);
    }

    @Transactional
    public PendenciaAdministrativa registrarFimNatural(VinculoContratual vinculo) {
        if (vinculo == null || vinculo.getId() == null || vinculo.getContrato() == null
                || vinculo.getContrato().getDataFim() == null) {
            throw new IllegalArgumentException("Vínculo aberto e fim contratual são obrigatórios para gerar pendência.");
        }
        Contrato contrato = vinculo.getContrato();
        String chave = "FIM_VIGENCIA_VINCULO:" + vinculo.getId() + ":" + contrato.getDataFim();
        Optional<PendenciaAdministrativa> existente = repositorio.findByChaveOrigem(chave);
        if (existente.isPresent()) return existente.get();

        GestaoLocal gestao = contrato.getGestaoLocal();
        SuperintendenciaEstadual se = gestao == null ? null : gestao.getSuperintendencia();
        PendenciaAdministrativa item = new PendenciaAdministrativa();
        item.setChaveOrigem(chave);
        item.setTipo(TipoPendencia.FIM_NATURAL_VIGENCIA);
        item.setEstado(EstadoPendencia.ABERTA);
        item.setMotivo("Vínculo permaneceu aberto após o fim natural da vigência contratual.");
        item.setDataEfetiva(contrato.getDataFim());
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

    @Transactional
    public PendenciaAdministrativa registrarConflitoReplay(
            Contrato contrato, ComposicaoContratualTemporalServico.ConflitoReplay conflito,
            String operacaoOrigem) {
        if (contrato == null || contrato.getId() == null || conflito == null || conflito.eventoId() == null)
            throw new IllegalArgumentException("Contrato e conflito temporal persistidos são obrigatórios.");
        String chave = "CONFLITO_REPLAY:" + contrato.getId() + ":EVENTO:" + conflito.eventoId();
        Optional<PendenciaAdministrativa> existente = repositorio.findByChaveOrigem(chave);
        if (existente.isPresent()) return existente.get();
        GestaoLocal gestao = contrato.getGestaoLocal();
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
            String chave = "FREQUENCIA:" + frequencia.getId() + ":VERSAO:"
                    + (frequencia.getVersaoNegocial() == null ? 1 : frequencia.getVersaoNegocial());
            if (repositorio.findByChaveOrigem(chave).isEmpty()) {
                registrarRevisaoFrequencia(frequencia, frequencia.getMotivoRevisao());
                criadas++;
            }
        }
        return criadas;
    }

    @Scheduled(cron = "${app.pendencias.fim-vigencia.cron:0 10 0 * * *}", zone = "America/Sao_Paulo")
    @Transactional
    public int sincronizarFimNatural() {
        int criadas = 0;
        for (VinculoContratual vinculo : vinculoRepositorio.buscarAbertosAposFimNatural(RelogioSistema.hoje())) {
            String chave = "FIM_VIGENCIA_VINCULO:" + vinculo.getId() + ":" + vinculo.getContrato().getDataFim();
            if (repositorio.findByChaveOrigem(chave).isEmpty()) {
                registrarFimNatural(vinculo);
                criadas++;
            }
        }
        return criadas;
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

    @Transactional
    public AvisoRevisaoRelatorioDTO avisoRelatorio(LocalDate inicio, LocalDate fim,
                                                   Collection<Long> contratoIds,
                                                   Long lotacaoId, Long jornadaId) {
        validarPeriodo(inicio, fim);
        sincronizarRevisoesFrequencia(inicio, fim);
        UsuarioAplicacao usuario = usuarioLeitor();
        Specification<PendenciaAdministrativa> base = especificacao(
                usuario, null, TipoPendencia.RETIFICACAO_FREQUENCIA,
                inicio, fim, contratoIds, lotacaoId, jornadaId);
        Specification<PendenciaAdministrativa> ativas = base.and((root, query, cb) ->
                root.get("estado").in(EstadoPendencia.ABERTA, EstadoPendencia.EM_TRATAMENTO));
        return AvisoRevisaoRelatorioDTO.de(repositorio.count(ativas));
    }

    @Transactional
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

    @Transactional
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
            if (v.getDataFim() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O vínculo continua aberto; trate o fim natural antes de resolver a pendência.");
            }
        } else if (item.getEstado() != EstadoPendencia.EM_TRATAMENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O conflito temporal deve ser analisado antes da confirmação da resolução.");
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
        if (usuario.getGestaoLocal() == null
                || !Objects.equals(usuario.getGestaoLocal().getId(), item.getGestaoLocalId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pendência não encontrada no seu escopo.");
        }
        if (item.getContratoId() != null) {
            Contrato contrato = contratoRepositorio.findById(item.getContratoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
            acesso.exigirAcessoContrato(usuario, contrato);
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
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> filtros = new ArrayList<>();
            if (acesso.ehGestorEstadual(usuario)) {
                SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(usuario);
                filtros.add(cb.equal(root.get("superintendenciaId"), se == null ? -1L : se.getId()));
            } else if (acesso.ehGestaoLocal(usuario)) {
                filtros.add(cb.equal(root.get("gestaoLocalId"),
                        usuario.getGestaoLocal() == null ? -1L : usuario.getGestaoLocal().getId()));
            } else if (!acesso.ehAdministrador(usuario) && !acesso.ehGestorNacional(usuario)) {
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
        return new PendenciaDTO(item.getId(), item.getTipo(), item.getEstado(), item.getMotivo(),
                item.getSurgidaEm(), item.getDataEfetiva(), item.getOperacaoOrigem(), item.getPerfilResponsavel(),
                item.getEscopoBloqueio(), item.getContratoId(), item.getContratoFotografia(), item.getPessoaId(),
                minimizador.nome(item.getPessoaFotografia(), nivel), item.getVinculoId(), item.getFrequenciaId(),
                item.getGestaoLocalId(), item.getSuperintendenciaId(), item.getOrigemEntidade(), item.getOrigemId(),
                item.getIniciadaEm(), item.getIniciadaPor(), item.getResolvidaEm(), item.getResolvidaPor(),
                item.getObservacaoTratamento(), acao);
    }

    private PendenciaAdministrativa salvarSemDuplicar(PendenciaAdministrativa item) {
        try {
            PendenciaAdministrativa salva = repositorio.saveAndFlush(item);
            auditoria.registrarAcao("PENDENCIA_GERADA", "PendenciaAdministrativa", salva.getId(),
                    salva.getTipo().name(), RegistroAuditoriaServico.detalhes(
                            "origem", salva.getOrigemEntidade(), "origemId", salva.getOrigemId(),
                            "escopoBloqueio", salva.getEscopoBloqueio(), "contratoId", salva.getContratoId()));
            return salva;
        } catch (DataIntegrityViolationException e) {
            return repositorio.findByChaveOrigem(item.getChaveOrigem()).orElseThrow(() -> e);
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
