package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.data.jpa.domain.Specification;
import com.example.siggtrat.utilitario.UtilitarioFiltroContrato;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class EmpregadoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(EmpregadoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final EmpregadoRepositorio empregadoRepositorio;
    private final EmpregadoListaConsultaRepositorio empregadoListaConsultaRepositorio;
    private final EmpregadoFrequenciaConsultaRepositorio empregadoFrequenciaConsultaRepositorio;
    private final HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final ConsultaCacheRedisServico redisQueryCacheServico;
    private final ServicoChaveCacheRedis redisCacheKeyServico;
    private final MinimizadorDadosPessoaisServico minimizador;
    private GradeFrequenciaCanonicaServico gradeCanonica;
    private ContextoEmpregadoCanonicoServico contextoCanonico;
    private IdentidadeHistoricaServico identidadeHistorica;
    private VinculoContratualRepositorio vinculosHistoricos;
    private EscopoLeituraPessoaVinculoServico escopoIdentidadeHistorica;
    private MotorTemporalVinculoServico motorIdentidadeHistorica;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarIdentidadeHistorica(IdentidadeHistoricaServico servico,
            VinculoContratualRepositorio vinculos, EscopoLeituraPessoaVinculoServico escopo, MotorTemporalVinculoServico motor) {
        this.identidadeHistorica = servico; this.vinculosHistoricos = vinculos; this.escopoIdentidadeHistorica = escopo;
        this.motorIdentidadeHistorica = motor;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGradeCanonica(GradeFrequenciaCanonicaServico servico) { this.gradeCanonica = servico; }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarContextoCanonico(ContextoEmpregadoCanonicoServico servico) { this.contextoCanonico = servico; }

    private final ZoneId zoneBR = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public EmpregadoServico(
            EmpregadoRepositorio empregadoRepositorio, EmpregadoListaConsultaRepositorio empregadoListaConsultaRepositorio,
            EmpregadoFrequenciaConsultaRepositorio empregadoFrequenciaConsultaRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoRepositorio,
            ContratoRepositorio contratoRepositorio, EscopoAcessoUsuarioServico userAccessScopeServico,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            ConsultaCacheRedisServico redisQueryCacheServico,
            ServicoChaveCacheRedis redisCacheKeyServico,
            MinimizadorDadosPessoaisServico minimizador) {

        this.empregadoRepositorio = empregadoRepositorio;
        this.empregadoListaConsultaRepositorio = empregadoListaConsultaRepositorio;
        this.empregadoFrequenciaConsultaRepositorio = empregadoFrequenciaConsultaRepositorio;
        this.historicoVinculoRepositorio = historicoVinculoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.userAccessScopeServico = userAccessScopeServico;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.redisQueryCacheServico = redisQueryCacheServico;
        this.redisCacheKeyServico = redisCacheKeyServico;
        this.minimizador = minimizador;
    }



    @Transactional(readOnly = true)
    public List<HistoricoVinculoContratoDTO> obterHistoricoContratos(Long id) {
        return obterHistoricoContratos(id, null, null);
    }

    public List<HistoricoVinculoContratoDTO> obterHistoricoContratos(Long id, LocalDate data, java.time.Instant conhecidoAte) {
        java.time.Instant conhecimento = conhecidoAte == null ? java.time.Instant.now() : conhecidoAte;
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (!empregadoRepositorio.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Empregado não encontrado.");
        }

        List<HistoricoVinculoContrato> historicos = historicoVinculoRepositorio
                .findByEmpregadoIdOrderByDataInicioDesc(id);
        if (!acessoOrganizacionalServico.ehGlobal(user)) {
            if (acessoOrganizacionalServico.ehGestorEstadual(user)
                    && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
                Long superintendenciaId = acessoOrganizacionalServico.superintendenciaEfetiva(user).getId();
                historicos = historicos.stream()
                        .filter(item -> Objects.equals(
                                item.getSuperintendenciaSnapshotId(), superintendenciaId))
                        .toList();
            } else if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
                Long gestaoLocalId = user.getGestaoLocal().getId();
                historicos = historicos.stream()
                        .filter(item -> Objects.equals(item.getGestaoLocalSnapshotId(), gestaoLocalId))
                        .toList();
            } else {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Seu perfil não possui acesso ao histórico contratual.");
            }
        }

        List<HistoricoVinculoContratoDTO> dtos = historicos.stream()
                .map(h -> {
                    Contrato contrato = h.getContrato();
                    String descricao = h.getContratoSnapshotDescricao();
                    Long efetivoAtual = h.getEfetivoAtualFotografia();
                    Integer efetivoPrevisto = h.getContratoSnapshotEfetivoPrevisto();
                    Long saldoEfetivo = efetivoAtual == null || efetivoPrevisto == null ? null : (long) efetivoPrevisto - efetivoAtual;
                    Double percentualOcupacao = efetivoAtual == null || efetivoPrevisto == null ? null
                            : efetivoPrevisto > 0 ? Math.round((efetivoAtual * 10000.0 / efetivoPrevisto)) / 100.0 : 0.0;

                    LocalDate referencia = data == null && h.getDataInicio() != null
                            ? h.getDataInicio().withZoneSameInstant(zoneBR).toLocalDate() : data;
                    Pessoa pessoa = h.getEmpregado().getPessoaCanonica();
                    boolean dentroDaFaixa = referencia != null && h.getDataInicio() != null
                            && !referencia.isBefore(h.getDataInicio().withZoneSameInstant(zoneBR).toLocalDate())
                            && (h.getDataFim() == null || !referencia.isAfter(h.getDataFim().withZoneSameInstant(zoneBR).toLocalDate()));
                    List<VinculoContratual> candidatos = pessoa == null || contrato == null || !dentroDaFaixa
                            || vinculosHistoricos == null || motorIdentidadeHistorica == null ? List.of()
                            : vinculosHistoricos.findByPessoaIdOrderByDataInicioAscIdAsc(pessoa.getId()).stream()
                            .filter(v -> Objects.equals(v.getContrato().getId(), contrato.getId()))
                            .filter(v -> { var pos = motorIdentidadeHistorica.reconstruir(v, referencia, conhecimento);
                                return pos.dataInicio() != null && !referencia.isBefore(pos.dataInicio())
                                        && (pos.dataFim() == null || !referencia.isAfter(pos.dataFim())); })
                            .filter(v -> escopoIdentidadeHistorica != null && escopoIdentidadeHistorica.permiteVinculo(user, v, referencia, conhecimento)).toList();
                    VinculoContratual ancora = candidatos.size() == 1 ? candidatos.get(0) : null;
                    IdentidadeHistoricaResolvidaDTO identidade = ancora == null || identidadeHistorica == null
                            ? IdentidadeHistoricaResolvidaDTO.ausente()
                            : identidadeHistorica.resolver(pessoa, ancora, referencia, conhecimento);
                    var nivel = minimizador.nivelPadrao(user);
                    identidade = new IdentidadeHistoricaResolvidaDTO(minimizador.nome(identidade.nome(), nivel), identidade.comprovado(),
                            identidade.tipoFonte(), identidade.fonteId(), identidade.fonteRegistradaEm(), identidade.inicioComprovado(), identidade.fimComprovado());
                    return new HistoricoVinculoContratoDTO(
                            h.getId(),
                            h.getEmpregado().getId(),
                            h.getEmpregadoSnapshotNome() == null || h.getEmpregadoSnapshotNome().isBlank()
                                    ? null : h.getEmpregadoSnapshotNome(),
                            contrato != null ? contrato.getId() : null,
                            h.getContratoSnapshotNumero(),
                            descricao,
                            descricao,
                            h.getContratoSnapshotTipo(),
                            h.getContratoSnapshotDataInicio(),
                            h.getContratoSnapshotDataFim(),
                            h.getContratoSnapshotAtivo(),
                            h.getContratoSnapshotEfetivoPrevisto(),
                            efetivoAtual,
                            saldoEfetivo,
                            percentualOcupacao,
                            h.getContratoSnapshotDiaReduzido(),
                            h.getContratoSnapshotHorasDiaReduzido(),
                            h.getGestaoLocalSnapshotId(),
                            h.getGestaoLocalSnapshotNome(),
                            h.getSuperintendenciaSnapshotId(),
                            h.getSuperintendenciaSnapshotNome(),
                            h.getSuperintendenciaSnapshotUf(),
                            h.getDataInicio(),
                            h.getDataFim(),
                            h.getObservacao(),
                            h.estaAtivo(), identidade, ancora == null ? null : ancora.getId(), referencia, conhecimento
                    );
                })
                .toList();
        return dtos;
    }
    public Page<EmpregadoDTO> listarTodos(
            String search,
            String status,
            Long lotacaoId,
            Long cargoId,
            Long contratoId,
            Boolean semContrato,
            Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeConsultarEmpregados(user)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não possui acesso aos empregados.");
        }
        if (acessoOrganizacionalServico.ehOperacional(user)) status = ConstantesDominio.STATUS_ATIVO;
        Specification<Empregado> spec = criarEspecificacaoEmpregados(user, search, status, lotacaoId, cargoId, contratoId, semContrato, null);
        boolean apenasSemContrato = Boolean.TRUE.equals(semContrato);
        spec = combinarConsultaCanonica(spec, user, new ContextoEmpregadoCanonicoServico.Filtros(search, status,
                lotacaoId, cargoId, null, apenasSemContrato || contratoId == null ? Set.of() : Set.of(contratoId),
                apenasSemContrato || contratoId != null));
        Page<Empregado> pagina = empregadoRepositorio.findAll(spec, pageable);
        Map<Long, ContextoEmpregadoCanonicoServico.Contexto> contextos = contextoCanonico == null ? Map.of()
                : contextoCanonico.resolverEmpregados(pagina.getContent().stream().map(Empregado::getId).toList());
        return pagina.map(e -> paraDTO(e, user, contextos.get(e.getId())));
    }

    /**
     * Consulta resumida usada apenas pela tela de listagem.
     * Os endpoints de cadastro, edição, frequência e histórico permanecem inalterados.
     */
    @Transactional(readOnly = true)
    public Page<EmpregadoListaDTO> obterResumo(
            String search,
            String status,
            Long lotacaoId,
            Long cargoId,
            Long turnoId,
            List<String> contratoIds,
            Boolean incluirSemContrato,
            Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        if (!acessoOrganizacionalServico.podeConsultarEmpregados(user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Seu perfil não possui acesso aos empregados.");
        }
        if (acessoOrganizacionalServico.ehOperacional(user)) status = ConstantesDominio.STATUS_ATIVO;

        Specification<Empregado> spec = criarEspecificacaoEmpregados(
                user, search, status, lotacaoId, cargoId, null, false, null);

        if (turnoId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("jornada").get("id"), turnoId));
        }

        final List<Long> idsContratos;
        try {
            idsContratos = UtilitarioFiltroContrato.normalizar(contratoIds);
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }

        if (!idsContratos.isEmpty() || Boolean.TRUE.equals(incluirSemContrato)) {
            spec = spec.and(criarEspecificacaoFiltroContratos(
                    idsContratos,
                    Boolean.TRUE.equals(incluirSemContrato)));
        }

        spec = combinarConsultaCanonica(spec, user, new ContextoEmpregadoCanonicoServico.Filtros(search, status,
                lotacaoId, cargoId, turnoId, Set.copyOf(idsContratos),
                !idsContratos.isEmpty() || Boolean.TRUE.equals(incluirSemContrato)));

        int tamanhoSeguro = Math.min(Math.max(pageable.getPageSize(), 1), 2000);
        Sort ordenacao = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");
        Pageable paginaSegura = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0), tamanhoSeguro, ordenacao);

        Specification<Empregado> especificacaoFinal = spec;
        String chaveTotal = redisCacheKeyServico.consulta(
                "empregados-resumo-total-canonico-v2", RelogioSistema.hoje(),
                search, status, lotacaoId, cargoId, turnoId,
                idsContratos, incluirSemContrato);
        long total = redisQueryCacheServico.empregadosResumoTotal(
                chaveTotal,
                () -> empregadoListaConsultaRepositorio.contar(especificacaoFinal));

        boolean cachear = paginaSegura.getPageNumber() <= 4
                && paginaSegura.getPageSize() <= 500;
        if (!cachear) {
            return carregarResumo(especificacaoFinal, paginaSegura, total, user);
        }

        String chaveCache = redisCacheKeyServico.consulta(
                "empregados-resumo-canonico-v2", RelogioSistema.hoje(),
                search, status, lotacaoId, cargoId, turnoId,
                idsContratos, incluirSemContrato,
                paginaSegura.getPageNumber(), paginaSegura.getPageSize(), paginaSegura.getSort());
        PaginaCacheDTO<EmpregadoListaDTO> paginaCache = redisQueryCacheServico.empregadosResumo(
                chaveCache,
                () -> PaginaCacheDTO.dePagina(carregarResumo(especificacaoFinal, paginaSegura, total, user)));
        return paginaCache.paraPagina(paginaSegura);
    }

    @Transactional(readOnly = true)
    public Page<EmpregadoFrequenciaDTO> obterEmpregadosParaFrequencia(
            LocalDate dataRef,
            Long contratoId,
            List<Long> contratoIds,
            Long lotacaoId,
            Long turnoId,
            String searchTerm,
            boolean skipCount,
            Pageable pageable) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaFrequencia(user);
        if (lotacaoId == null || turnoId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação e turno são obrigatórios para carregar a grade de frequência.");
        }
        if (acessoOrganizacionalServico.ehOperacional(user)) {
            Long gestaoLocalId = user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
            boolean escopoValido = true;
            if (lotacaoId != null && turnoId != null) {
                escopoValido = userAccessScopeServico.podeAcessarJornadaLotacao(user, gestaoLocalId, lotacaoId, turnoId);
            } else if (lotacaoId != null) {
                escopoValido = userAccessScopeServico.podeAcessarLotacao(user, gestaoLocalId, lotacaoId);
            } else if (turnoId != null) {
                escopoValido = userAccessScopeServico.podeAcessarJornada(user, gestaoLocalId, turnoId);
            }
            if (!escopoValido) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A lotação/turno não pertence ao seu escopo operacional.");
            }
        }
        LinkedHashSet<Long> contratosNormalizados = new LinkedHashSet<>();
        if (contratoId != null) contratosNormalizados.add(contratoId);
        if (contratoIds != null) contratosNormalizados.addAll(contratoIds.stream().filter(Objects::nonNull).toList());

        GradeFrequenciaCanonicaServico.Consulta consultaCanonica = gradeCanonica.preparar(
                user, dataRef, lotacaoId, turnoId, contratosNormalizados, searchTerm);
        Pageable paginaSegura = com.example.siggtrat.utilitario.UtilitarioPaginacao.limitarComOrdenacaoFixa(
                pageable, org.springframework.data.domain.Sort.by("nome").ascending().and(
                        org.springframework.data.domain.Sort.by("id").ascending()));
        Specification<Empregado> especificacaoFinal = consultaCanonica.especificacao();
        String chaveCache = redisCacheKeyServico.consulta(
                "empregados-frequencia-elegivel-v3",
                dataRef, contratosNormalizados, lotacaoId, turnoId, searchTerm, skipCount,
                paginaSegura.getPageNumber(), paginaSegura.getPageSize(), paginaSegura.getSort());
        PaginaCacheDTO<EmpregadoFrequenciaDTO> paginaCache = redisQueryCacheServico.empregadosFrequencia(
                chaveCache,
                () -> {
                    Page<EmpregadoFrequenciaDTO> pagina = empregadoFrequenciaConsultaRepositorio.buscar(
                            especificacaoFinal, paginaSegura, skipCount);
                    return PaginaCacheDTO.dePagina(gradeCanonica.projetar(pagina, consultaCanonica));
                });
        return paginaCache.paraPagina(paginaSegura).map(dto -> minimizarFrequencia(dto, user));
    }
    public EmpregadoDTO buscarPorId(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Empregado empregado = empregadoRepositorio.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado."));
        acessoOrganizacionalServico.exigirConsultaEmpregado(user, empregado);
        return paraDTO(empregado, user);
    }



    /**
     * Filtra o vínculo atual por EXISTS em vez de JOIN na consulta principal.
     * Isso mantém a paginação e a contagem corretas nas projeções DTO e evita
     * duplicidade de linhas quando o Hibernate monta a consulta resumida.
     */
    private Specification<Empregado> criarEspecificacaoFiltroContratos(
            List<Long> contratoIds,
            boolean incluirSemContrato) {
        List<Long> idsSeguros = contratoIds == null ? List.of() : List.copyOf(contratoIds);

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> opcoes = new ArrayList<>();

            if (!idsSeguros.isEmpty()) {
                jakarta.persistence.criteria.Subquery<Long> subconsulta = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<Empregado> empregadoVinculado =
                        subconsulta.from(Empregado.class);
                jakarta.persistence.criteria.Join<Empregado, Contrato> contrato =
                        empregadoVinculado.join("contratos", jakarta.persistence.criteria.JoinType.INNER);

                subconsulta.select(empregadoVinculado.get("id"));
                subconsulta.where(
                        cb.equal(empregadoVinculado.get("id"), root.get("id")),
                        contrato.get("id").in(idsSeguros)
                );
                opcoes.add(cb.exists(subconsulta));
            }

            if (incluirSemContrato) {
                opcoes.add(cb.isEmpty(root.get("contratos")));
            }

            return opcoes.isEmpty()
                    ? cb.conjunction()
                    : cb.or(opcoes.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ==========================================
    // MÉTODOS DE MAPEMENTO E REGRAS DE NEGÓCIO
    // ==========================================

    private Specification<Empregado> combinarConsultaCanonica(Specification<Empregado> legada,
            UsuarioAplicacao usuario, ContextoEmpregadoCanonicoServico.Filtros filtros) {
        Specification<Empregado> semPessoa = (root, query, cb) -> cb.isNull(root.get("pessoaCanonica"));
        Specification<Empregado> apenasLegados = legada.and(semPessoa);
        // Configuração incompleta falha fechada para os registros canônicos.
        if (contextoCanonico == null) return apenasLegados;
        boolean operacional = acessoOrganizacionalServico.ehOperacional(usuario);
        return apenasLegados.or(contextoCanonico.especificacao(filtros, contratosCanonicosDoEscopo(usuario, filtros),
                operacional, operacional ? userAccessScopeServico.obterEscopos(usuario) : List.of(), RelogioSistema.hoje()));
    }

    private Set<Long> contratosCanonicosDoEscopo(UsuarioAplicacao usuario, ContextoEmpregadoCanonicoServico.Filtros filtros) {
        if (acessoOrganizacionalServico.ehGlobal(usuario)) return null;
        List<Contrato> candidatos;
        if (filtros.filtrarContratos()) {
            if (filtros.contratosSolicitados().isEmpty()) return Set.of();
            // O ID limita o trabalho de reconstrução; a autorização temporal
            // continua sendo exigida abaixo para cada contrato encontrado.
            candidatos = contratoRepositorio.findAllById(filtros.contratosSolicitados());
        } else if (acessoOrganizacionalServico.ehGestorEstadual(usuario)
                && acessoOrganizacionalServico.superintendenciaEfetiva(usuario) != null) {
            candidatos = contratoRepositorio.buscarCandidatosSuperintendenciaTemporal(
                    acessoOrganizacionalServico.superintendenciaEfetiva(usuario).getId(), RelogioSistema.hoje());
        } else if ((acessoOrganizacionalServico.ehGestaoLocal(usuario) || acessoOrganizacionalServico.ehOperacional(usuario))
                && acessoOrganizacionalServico.gestaoLocalEfetiva(usuario) != null) {
            candidatos = contratoRepositorio.buscarCandidatosGestaoTemporal(
                    acessoOrganizacionalServico.gestaoLocalEfetiva(usuario).getId(), RelogioSistema.hoje());
        } else return Set.of();
        return candidatos.stream().filter(c -> acessoOrganizacionalServico.podeAcessarContrato(usuario, c))
                .map(Contrato::getId).collect(Collectors.toSet());
    }

    private Specification<Empregado> criarEspecificacaoEmpregados(
            UsuarioAplicacao user, String termo, String status, Long lotacaoId, Long cargoId,
            Long contratoId, Boolean semContrato, LocalDate dataReferencia) {
        String termoNormalizado = termo == null || termo.isBlank()
                ? null
                : termo.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (termoNormalizado != null && termoNormalizado.length() > 80) {
            termoNormalizado = termoNormalizado.substring(0, 80);
        }
        final String termoBusca = termoNormalizado;
        String statusNormalizado = status == null || status.isBlank() ? null : status.trim();

        return (root, query, cb) -> {
            // DISTINCT só é necessário quando esta especificação realmente cria
            // join com a coleção de contratos. Na frequência são usados EXISTS,
            // evitando temporary table e filesort desnecessários no MySQL.
            if (query != null && (contratoId != null || dataReferencia != null)) {
                query.distinct(true);
            }
            List<jakarta.persistence.criteria.Predicate> filtros = new ArrayList<>();

            if (termoBusca != null) {
                List<jakarta.persistence.criteria.Predicate> buscas = new ArrayList<>();

                // Busca por nome com múltiplas palavras, sem recriar LOWER em CPF
                // e matrícula. "alex barbosa" exige as duas partes no nome.
                List<jakarta.persistence.criteria.Predicate> partesNome = new ArrayList<>();
                for (String parte : termoBusca.split(" ")) {
                    if (!parte.isBlank()) {
                        partesNome.add(cb.like(cb.lower(root.get("nome")), "%" + parte + "%"));
                    }
                }
                if (!partesNome.isEmpty()) {
                    buscas.add(cb.and(partesNome.toArray(new jakarta.persistence.criteria.Predicate[0])));
                }

                buscas.add(cb.like(root.get("cpf"), "%" + termoBusca + "%"));
                buscas.add(cb.like(root.get("matricula"), "%" + termoBusca + "%"));

                String digitosBusca = UtilitarioMatricula.apenasDigitos(termoBusca);
                // A remoção de máscara no banco é mais cara; só é usada quando
                // há uma quantidade útil de dígitos para reduzir o resultado.
                if (digitosBusca.length() >= 4) {
                    jakarta.persistence.criteria.Expression<String> cpfSemPontos = cb.function(
                            "replace", String.class, root.get("cpf"), cb.literal("."), cb.literal(""));
                    jakarta.persistence.criteria.Expression<String> cpfSemMascara = cb.function(
                            "replace", String.class, cpfSemPontos, cb.literal("-"), cb.literal(""));
                    jakarta.persistence.criteria.Expression<String> matriculaSemPontos = cb.function(
                            "replace", String.class, root.get("matricula"), cb.literal("."), cb.literal(""));
                    jakarta.persistence.criteria.Expression<String> matriculaSemMascara = cb.function(
                            "replace", String.class, matriculaSemPontos, cb.literal("-"), cb.literal(""));
                    buscas.add(cb.like(cpfSemMascara, "%" + digitosBusca + "%"));
                    buscas.add(cb.like(matriculaSemMascara, "%" + digitosBusca + "%"));
                }

                filtros.add(cb.or(buscas.toArray(new jakarta.persistence.criteria.Predicate[0])));
            }
            if (statusNormalizado != null) filtros.add(cb.equal(root.get("status").get("descricao"), statusNormalizado));
            if (lotacaoId != null) filtros.add(cb.equal(root.get("lotacao").get("id"), lotacaoId));
            if (cargoId != null) filtros.add(cb.equal(root.get("cargo").get("id"), cargoId));

            jakarta.persistence.criteria.Join<Empregado, Contrato> contratoJoin = null;
            if (Boolean.TRUE.equals(semContrato)) {
                filtros.add(cb.isEmpty(root.get("contratos")));
            } else if (contratoId != null || dataReferencia != null) {
                contratoJoin = root.join("contratos", jakarta.persistence.criteria.JoinType.LEFT);
                if (contratoId != null) filtros.add(cb.equal(contratoJoin.get("id"), contratoId));
                if (dataReferencia != null) {
                    filtros.add(cb.isTrue(contratoJoin.get("ativo")));
                    filtros.add(cb.or(cb.isNull(contratoJoin.get("dataInicio")), cb.lessThanOrEqualTo(contratoJoin.get("dataInicio"), dataReferencia)));
                    filtros.add(cb.or(cb.isNull(contratoJoin.get("dataFim")), cb.greaterThanOrEqualTo(contratoJoin.get("dataFim"), dataReferencia)));
                    filtros.add(cb.or(cb.isNull(root.get("dataAdmissao")), cb.lessThanOrEqualTo(root.get("dataAdmissao"), dataReferencia)));
                }
            }

            if (!acessoOrganizacionalServico.ehGlobal(user)) {
                if (acessoOrganizacionalServico.ehGestorEstadual(user) && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
                    filtros.add(cb.equal(root.get("gestaoLocal").get("superintendencia").get("id"), acessoOrganizacionalServico.superintendenciaEfetiva(user).getId()));
                } else if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
                    filtros.add(cb.equal(root.get("gestaoLocal").get("id"), user.getGestaoLocal().getId()));
                } else if (acessoOrganizacionalServico.ehOperacional(user)) {
                    List<jakarta.persistence.criteria.Predicate> escopos = new ArrayList<>();
                    for (EscopoAcessoUsuarioServico.Escopo escopo : userAccessScopeServico.obterEscopos(user)) {
                        jakarta.persistence.criteria.Predicate p = cb.equal(root.get("gestaoLocal").get("id"), escopo.gestaoLocalId());
                        if (escopo.lotacaoId() != null) p = cb.and(p, cb.equal(root.get("lotacao").get("id"), escopo.lotacaoId()));
                        if (escopo.jornadaId() != null) p = cb.and(p, cb.equal(root.get("jornada").get("id"), escopo.jornadaId()));
                        escopos.add(p);
                    }
                    filtros.add(escopos.isEmpty() ? cb.disjunction() : cb.or(escopos.toArray(new jakarta.persistence.criteria.Predicate[0])));
                } else filtros.add(cb.disjunction());
            }
            return cb.and(filtros.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }




    private Page<EmpregadoListaDTO> carregarResumo(
            Specification<Empregado> specification,
            Pageable pageable,
            long total,
            UsuarioAplicacao usuario) {
        List<EmpregadoListaBaseDTO> conteudo =
                empregadoListaConsultaRepositorio.buscarConteudo(specification, pageable);
        Page<EmpregadoListaBaseDTO> pagina = new org.springframework.data.domain.PageImpl<>(
                conteudo, pageable, total);

        List<Long> empregadoIds = pagina.getContent().stream()
                .map(EmpregadoListaBaseDTO::id)
                .toList();

        Map<Long, List<EmpregadoContratoDTO>> contratosPorEmpregado = new HashMap<>();
        if (!empregadoIds.isEmpty()) {
            for (EmpregadoContratoListaDTO vinculo :
                    empregadoRepositorio.buscarContratosDaListaPorIdsEmpregados(empregadoIds)) {
                contratosPorEmpregado
                        .computeIfAbsent(vinculo.empregadoId(), ignorado -> new ArrayList<>())
                        .add(new EmpregadoContratoDTO(
                                vinculo.contratoId(),
                                vinculo.numeroContrato(),
                                vinculo.descricao(),
                                vinculo.ativo()));
            }
        }

        Map<Long, ContextoEmpregadoCanonicoServico.Contexto> contextos = contextoCanonico == null ? Map.of()
                : contextoCanonico.resolverEmpregados(empregadoIds);
        return pagina.map(empregado -> paraListaDTO(
                empregado,
                contratosPorEmpregado.getOrDefault(empregado.id(), List.of()), usuario, contextos.get(empregado.id())));
    }

    private EmpregadoListaDTO paraListaDTO(
            EmpregadoListaBaseDTO empregado,
            List<EmpregadoContratoDTO> contratos,
            UsuarioAplicacao usuario,
            ContextoEmpregadoCanonicoServico.Contexto contexto) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        if (contexto != null) {
            Contrato contrato = contexto.vinculo().getContrato();
            return new EmpregadoListaDTO(empregado.id(), minimizador.nome(contexto.pessoa().getNome(), nivel),
                    minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(contexto.matricula()), nivel),
                    minimizador.cpf(contexto.pessoa().getCpf(), nivel), empregado.statusId(), empregado.statusDescricao(),
                    contexto.lotacao() == null ? null : contexto.lotacao().getId(), contexto.lotacaoNome(),
                    contexto.cargo() == null ? null : contexto.cargo().getId(), contexto.cargoNome(),
                    contexto.jornada() == null ? null : contexto.jornada().getId(), contexto.jornadaDescricao(),
                    List.of(new EmpregadoContratoDTO(contrato.getId(), contrato.getNumeroContrato(), contrato.getDescricao(), contrato.getAtivo())));
        }
        return new EmpregadoListaDTO(
                empregado.id(),
                minimizador.nome(empregado.nome(), nivel),
                minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(empregado.matricula()), nivel),
                minimizador.cpf(empregado.cpf(), nivel),
                empregado.statusId(),
                empregado.statusDescricao(),
                empregado.lotacaoId(),
                empregado.lotacaoNome(),
                empregado.cargoId(),
                empregado.cargoNome(),
                empregado.jornadaId(),
                empregado.jornadaDescricao(),
                List.copyOf(contratos));
    }

    private EmpregadoDTO paraDTO(Empregado e, UsuarioAplicacao usuario) {
        return paraDTO(e, usuario, contextoCanonico == null ? null : contextoCanonico.resolver(e));
    }

    private EmpregadoDTO paraDTO(Empregado e, UsuarioAplicacao usuario, ContextoEmpregadoCanonicoServico.Contexto contexto) {
        if (e == null) return null;
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        boolean completo = minimizador.completo(nivel);

        Pessoa pessoa = e.getPessoaCanonica();
        Lotacao lotacao = contexto != null ? contexto.lotacao() : pessoa == null ? e.getLotacao() : null;
        Jornada jornada = contexto != null ? contexto.jornada() : pessoa == null ? e.getJornada() : null;
        Cargo cargo = contexto != null ? contexto.cargo() : pessoa == null ? e.getCargo() : null;
        GestaoLocal gestao = contexto != null ? contexto.gestao() : pessoa == null ? e.getGestaoLocal() : null;
        Endereco enderecoAtual = pessoa == null ? e.getEndereco() : pessoa.getEndereco();
        EnderecoDTO endDTO = null;
        if (completo && enderecoAtual != null) {
            Endereco end = enderecoAtual;
            endDTO = new EnderecoDTO(end.getId(), end.getLogradouro(), end.getNumero(), end.getBairro(), end.getCidade(), end.getUf(), end.getCep(), end.getVersion());
        }

        List<EmpregadoContratoDTO> contratosDTO = new ArrayList<>();
        if (contexto != null) {
            Contrato contrato = contexto.vinculo().getContrato();
            contratosDTO.add(new EmpregadoContratoDTO(contrato.getId(), contrato.getNumeroContrato(), contrato.getDescricao(), contrato.getAtivo()));
        } else if (pessoa == null && e.getContratos() != null) {
            contratosDTO = e.getContratos().stream().map(c -> new EmpregadoContratoDTO(c.getId(), c.getNumeroContrato(), c.getDescricao(), c.getAtivo())).collect(Collectors.toList());
        }

        return new EmpregadoDTO(
                e.getId(), minimizador.nome(pessoa == null ? e.getNome() : pessoa.getNome(), nivel),
                minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(contexto != null ? contexto.matricula() : pessoa == null ? e.getMatricula() : null), nivel),
                minimizador.cpf(pessoa == null ? e.getCpf() : pessoa.getCpf(), nivel), minimizador.restrito(pessoa == null ? e.getRg() : pessoa.getRg(), nivel),
                minimizador.restrito(pessoa == null ? e.getCtpsSerie() : pessoa.getCtpsSerie(), nivel),
                minimizador.restrito(pessoa == null ? e.getNitPisPasep() : pessoa.getNitPisPasep(), nivel),
                minimizador.restrito(pessoa == null ? e.getEmail() : pessoa.getEmail(), nivel),
                minimizador.restrito(pessoa == null ? e.getDataNascimento() : pessoa.getDataNascimento(), nivel),
                contexto != null ? contexto.vinculo().getDataInicio() : pessoa == null ? e.getDataAdmissao() : null,
                e.getStatus() != null ? e.getStatus().getId() : null, e.getStatus() != null ? e.getStatus().getDescricao() : null,
                lotacao != null ? lotacao.getId() : null, contexto != null ? contexto.lotacaoNome() : lotacao == null ? null : lotacao.getNome(),
                cargo != null ? cargo.getId() : null, contexto != null ? contexto.cargoNome() : cargo == null ? null : cargo.getNome(),
                jornada != null ? jornada.getId() : null, contexto != null ? contexto.jornadaDescricao() : jornada == null ? null : jornada.getDescricao(),
                endDTO, contratosDTO,
                gestao != null ? gestao.getId() : null,
                gestao != null ? gestao.getNome() : null,
                gestao != null && gestao.getSuperintendencia() != null ? gestao.getSuperintendencia().getId() : null,
                gestao != null && gestao.getSuperintendencia() != null ? gestao.getSuperintendencia().getNome() : null,
                gestao != null && gestao.getSuperintendencia() != null ? gestao.getSuperintendencia().getUf() : null
        );
    }

    private EmpregadoFrequenciaDTO minimizarFrequencia(EmpregadoFrequenciaDTO dto, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new EmpregadoFrequenciaDTO(dto.id(), minimizador.nome(dto.nome(), nivel),
                minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(dto.matricula()), nivel),
                minimizador.cpf(dto.cpf(), nivel), dto.lotacaoNome(), dto.lotacaoId(),
                dto.turnoDescricao(), dto.turnoId(), dto.cargoNome());
    }



    // empregado_contrato representa o vínculo atual; HistoricoVinculoContrato
    // é a fonte temporal canônica da alocação.


}
