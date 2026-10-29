package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.configuracao.propriedades.PropriedadesEmpregado;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.auditoria.DiferencasAuditoriaEmpregado;
import com.example.siggtrat.auditoria.NarradorAlteracoesEmpregado;
import com.example.siggtrat.repositorio.*;
import org.springframework.data.jpa.domain.Specification;
import com.example.siggtrat.utilitario.UtilitarioCpf;
import com.example.siggtrat.utilitario.UtilitarioFiltroContrato;
import com.example.siggtrat.utilitario.UtilitarioMatriculaEmpregado;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import com.example.siggtrat.utilitario.UtilitarioHorario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class EmpregadoServico {

    private final EmpregadoRepositorio empregadoRepositorio;
    private final EmpregadoListaConsultaRepositorio empregadoListaConsultaRepositorio;
    private final EmpregadoFrequenciaConsultaRepositorio empregadoFrequenciaConsultaRepositorio;
    private final HistoricoEmpregadoRepositorio historicoEmpRepositorio;
    private final HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    private final StatusEmpregadoRepositorio statusRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final ContaRepositorio contaRepositorio;
    private final AgenciaRepositorio agenciaRepositorio;
    private final TipoContaRepositorio tipoContaRepositorio;
    private final EnderecoRepositorio enderecoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;

    private final RegistroAuditoriaServico auditLogServico;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final HistoricoIntegridadeServico historicoIntegridadeServico;
    private final ConsultaCacheRedisServico redisQueryCacheServico;
    private final ServicoChaveCacheRedis redisCacheKeyServico;
    private final MinimizadorDadosPessoaisServico minimizador;

    private final ZoneId zoneBR = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    private final long maxJornadaMinutes;

    public EmpregadoServico(
            EmpregadoRepositorio empregadoRepositorio, EmpregadoListaConsultaRepositorio empregadoListaConsultaRepositorio,
            EmpregadoFrequenciaConsultaRepositorio empregadoFrequenciaConsultaRepositorio,
            HistoricoEmpregadoRepositorio historicoEmpRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoRepositorio,
            StatusEmpregadoRepositorio statusRepositorio, LotacaoRepositorio lotacaoRepositorio,
            CargoRepositorio cargoRepositorio, ContaRepositorio contaRepositorio, AgenciaRepositorio agenciaRepositorio,
            TipoContaRepositorio tipoContaRepositorio, EnderecoRepositorio enderecoRepositorio,
            ContratoRepositorio contratoRepositorio, JornadaRepositorio jornadaRepositorio,
            RegistroAuditoriaServico auditLogServico, EscopoAcessoUsuarioServico userAccessScopeServico,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            HistoricoIntegridadeServico historicoIntegridadeServico,
            ConsultaCacheRedisServico redisQueryCacheServico,
            ServicoChaveCacheRedis redisCacheKeyServico,
            MinimizadorDadosPessoaisServico minimizador,
            PropriedadesEmpregado empregadoProperties) {

        this.empregadoRepositorio = empregadoRepositorio;
        this.empregadoListaConsultaRepositorio = empregadoListaConsultaRepositorio;
        this.empregadoFrequenciaConsultaRepositorio = empregadoFrequenciaConsultaRepositorio;
        this.historicoEmpRepositorio = historicoEmpRepositorio;
        this.historicoVinculoRepositorio = historicoVinculoRepositorio;
        this.statusRepositorio = statusRepositorio; this.lotacaoRepositorio = lotacaoRepositorio;
        this.cargoRepositorio = cargoRepositorio; this.contaRepositorio = contaRepositorio;
        this.agenciaRepositorio = agenciaRepositorio; this.tipoContaRepositorio = tipoContaRepositorio;
        this.enderecoRepositorio = enderecoRepositorio; this.contratoRepositorio = contratoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio; this.auditLogServico = auditLogServico;
        this.userAccessScopeServico = userAccessScopeServico;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.historicoIntegridadeServico = historicoIntegridadeServico;
        this.redisQueryCacheServico = redisQueryCacheServico;
        this.redisCacheKeyServico = redisCacheKeyServico;
        this.minimizador = minimizador;
        this.maxJornadaMinutes = empregadoProperties.maxJornadaMinutes();
    }

    @Transactional(readOnly = true)
    public List<HistoricoVinculoContratoDTO> obterHistoricoContratos(Long id) {
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

        Set<Long> contratoIds = historicos.stream()
                .map(HistoricoVinculoContrato::getContrato)
                .filter(Objects::nonNull)
                .map(Contrato::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Long> efetivoAtualPorContrato = contratoIds.isEmpty()
                ? Map.of()
                : contratoRepositorio.countEmpregadosAtivosByContratoIds(contratoIds)
                        .stream()
                        .collect(Collectors.toMap(
                                linha -> ((Number) linha[0]).longValue(),
                                linha -> ((Number) linha[1]).longValue(),
                                (primeiro, ignorado) -> primeiro
                        ));

        List<HistoricoVinculoContratoDTO> dtos = historicos.stream()
                .map(h -> {
                    Contrato contrato = h.getContrato();
                    GestaoLocal gestaoLocal = contrato != null ? contrato.getGestaoLocal() : null;
                    SuperintendenciaEstadual se = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;
                    String descricao = contrato != null ? contrato.getDescricao() : null;
                    long efetivoAtual = contrato != null && contrato.getId() != null
                            ? efetivoAtualPorContrato.getOrDefault(contrato.getId(), 0L)
                            : 0L;
                    int efetivoPrevisto = contrato != null && contrato.getEfetivoPrevisto() != null
                            ? contrato.getEfetivoPrevisto()
                            : 0;
                    long saldoEfetivo = (long) efetivoPrevisto - efetivoAtual;
                    double percentualOcupacao = efetivoPrevisto > 0
                            ? Math.round((efetivoAtual * 10000.0 / efetivoPrevisto)) / 100.0
                            : 0.0;

                    return new HistoricoVinculoContratoDTO(
                            h.getId(),
                            h.getEmpregado().getId(),
                            h.getEmpregadoSnapshotNome() != null
                                    ? h.getEmpregadoSnapshotNome()
                                    : h.getEmpregado().getNome(),
                            contrato != null ? contrato.getId() : null,
                            contrato != null ? contrato.getNumeroContrato() : null,
                            descricao,
                            descricao,
                            contrato != null ? contrato.getTipo() : null,
                            contrato != null ? contrato.getDataInicio() : null,
                            contrato != null ? contrato.getDataFim() : null,
                            contrato != null ? contrato.getAtivo() : null,
                            contrato != null ? contrato.getEfetivoPrevisto() : null,
                            efetivoAtual,
                            saldoEfetivo,
                            percentualOcupacao,
                            contrato != null ? contrato.getDiaSemanaReduzido() : null,
                            contrato != null ? contrato.getHorasDiaReduzido() : null,
                            h.getGestaoLocalSnapshotId(),
                            gestaoLocal != null ? gestaoLocal.getNome() : null,
                            h.getSuperintendenciaSnapshotId(),
                            se != null ? se.getNome() : null,
                            se != null ? se.getUf() : null,
                            h.getDataInicio(),
                            h.getDataFim(),
                            h.getObservacao(),
                            h.estaAtivo()
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
        return empregadoRepositorio.findAll(spec, pageable).map(e -> paraDTO(e, user));
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

        int tamanhoSeguro = Math.min(Math.max(pageable.getPageSize(), 1), 2000);
        Sort ordenacao = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "id");
        Pageable paginaSegura = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0), tamanhoSeguro, ordenacao);

        Specification<Empregado> especificacaoFinal = spec;
        String chaveTotal = redisCacheKeyServico.consulta(
                "empregados-resumo-total",
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
                "empregados-resumo",
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

        Specification<Empregado> spec = criarEspecificacaoEmpregados(
                user, searchTerm, ConstantesDominio.STATUS_ATIVO, lotacaoId, null, null, false, null);
        spec = spec.and((root, query, cb) -> cb.and(
                cb.equal(root.get("jornada").get("id"), turnoId),
                cb.or(cb.isNull(root.get("dataAdmissao")),
                        cb.lessThanOrEqualTo(root.get("dataAdmissao"), dataRef))));

        ZonedDateTime inicioDia = dataRef.atStartOfDay(zoneBR);
        ZonedDateTime fimDiaExclusivo = dataRef.plusDays(1).atStartOfDay(zoneBR);
        spec = spec.and((root, query, cb) -> {
            var historicoVigente = query.subquery(Long.class);
            var hist = historicoVigente.from(HistoricoVinculoContrato.class);
            List<jakarta.persistence.criteria.Predicate> filtrosHistorico = new ArrayList<>();
            filtrosHistorico.add(cb.equal(hist.get("empregado").get("id"), root.get("id")));
            filtrosHistorico.add(cb.lessThan(hist.get("dataInicio"), fimDiaExclusivo));
            filtrosHistorico.add(cb.or(
                    cb.isNull(hist.get("dataFim")),
                    cb.greaterThanOrEqualTo(hist.get("dataFim"), inicioDia)));
            if (!contratosNormalizados.isEmpty()) {
                filtrosHistorico.add(hist.get("contrato").get("id").in(contratosNormalizados));
            }
            historicoVigente.select(hist.get("id"));
            historicoVigente.where(filtrosHistorico.toArray(new jakarta.persistence.criteria.Predicate[0]));

            return cb.exists(historicoVigente);
        });
        Pageable paginaSegura = com.example.siggtrat.utilitario.UtilitarioPaginacao.limitarComOrdenacaoFixa(
                pageable, org.springframework.data.domain.Sort.by("nome").ascending().and(
                        org.springframework.data.domain.Sort.by("id").ascending()));
        Specification<Empregado> especificacaoFinal = spec;
        String chaveCache = redisCacheKeyServico.consulta(
                "empregados-frequencia",
                dataRef, contratosNormalizados, lotacaoId, turnoId, searchTerm, skipCount,
                paginaSegura.getPageNumber(), paginaSegura.getPageSize(), paginaSegura.getSort());
        PaginaCacheDTO<EmpregadoFrequenciaDTO> paginaCache = redisQueryCacheServico.empregadosFrequencia(
                chaveCache,
                () -> PaginaCacheDTO.dePagina(empregadoFrequenciaConsultaRepositorio.buscar(
                        especificacaoFinal, paginaSegura, skipCount)));
        return paginaCache.paraPagina(paginaSegura).map(dto -> minimizarFrequencia(dto, user));
    }
    public EmpregadoDTO buscarPorId(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Empregado empregado = empregadoRepositorio.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado."));
        acessoOrganizacionalServico.exigirConsultaEmpregado(user, empregado);
        return paraDTO(empregado, user);
    }

    @Transactional
    public EmpregadoDTO criar(EmpregadoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoEmpregado(user);
        Empregado empregado = new Empregado();
        mapearBasicos(dto, empregado);
        mapearRelacionamentos(dto, empregado);
        resolverEValidarGestaoLocal(user, empregado);
        normalizarDados(empregado);
        validarObrigatoriosCadastro(empregado);
        validarCpfObrigatorio(empregado);
        if (empregadoRepositorio.existsByCpf(empregado.getCpf())) throw new IllegalArgumentException("CPF já cadastrado");
        if (empregado.getMatricula() != null && empregadoRepositorio.existsByMatricula(empregado.getMatricula())) {
            throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("Matrícula já cadastrada.");
        }

        Empregado saved = empregadoRepositorio.save(empregado);
        gerenciarHistoricoContratos(saved, saved.getContratos(), "Cadastro Inicial");
        java.util.List<java.util.Map<String, Object>> changes = DiferencasAuditoriaEmpregado.criado(saved);
        registrarHistorico(saved, "Cadastro do colaborador", user);
        for (String frase : NarradorAlteracoesEmpregado.paraFrasesAmigaveis(changes)) registrarHistorico(saved, frase, user);
        auditLogServico.registrarAcao("EMPREGADO_CREATE", "Empregado", saved.getId(), saved.getNome(),
                RegistroAuditoriaServico.detalhes("cpf", saved.getCpf(), "gestaoLocalId", saved.getGestaoLocal() != null ? saved.getGestaoLocal().getId() : null,
                        "lotacaoId", saved.getLotacao() != null ? saved.getLotacao().getId() : null, "changes", changes));
        return paraDTO(saved, user);
    }

    @Transactional
    public EmpregadoDTO atualizar(Long id, EmpregadoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoEmpregado(user);
        Empregado atual = empregadoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado."));
        acessoOrganizacionalServico.exigirConsultaEmpregado(user, atual);
        DiferencasAuditoriaEmpregado.EstadoEmpregado beforeAudit = DiferencasAuditoriaEmpregado.capturarEstado(atual);
        mapearBasicos(dto, atual);
        mapearRelacionamentos(dto, atual);
        resolverEValidarGestaoLocal(user, atual);
        normalizarDados(atual);
        validarObrigatoriosCadastro(atual);
        validarCpfObrigatorio(atual);
        if (empregadoRepositorio.existsByCpfAndIdNot(atual.getCpf(), id)) throw new IllegalArgumentException("CPF duplicado");
        if (atual.getMatricula() != null && empregadoRepositorio.existsByMatriculaAndIdNot(atual.getMatricula(), id)) {
            throw new com.example.siggtrat.excecao.ExcecaoConflitoDados("Matrícula já cadastrada.");
        }

        Empregado saved = empregadoRepositorio.save(atual);
        gerenciarHistoricoContratos(saved, saved.getContratos(), "Atualização Cadastral");
        java.util.List<java.util.Map<String, Object>> changes = DiferencasAuditoriaEmpregado.calcularDiferencas(beforeAudit, saved);
        List<String> frases = NarradorAlteracoesEmpregado.paraFrasesAmigaveis(changes);
        if (frases.isEmpty()) registrarHistorico(saved, "Atualização cadastral", user);
        else for (String frase : frases) registrarHistorico(saved, frase, user);
        auditLogServico.registrarAcao("EMPREGADO_UPDATE", "Empregado", saved.getId(), saved.getNome(),
                RegistroAuditoriaServico.detalhes("cpf", saved.getCpf(), "gestaoLocalId", saved.getGestaoLocal() != null ? saved.getGestaoLocal().getId() : null,
                        "lotacaoId", saved.getLotacao() != null ? saved.getLotacao().getId() : null, "changes", changes));
        return paraDTO(saved, user);
    }

    @Transactional
    public void excluir(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoEmpregado(user);
        Empregado empregado = empregadoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado não encontrado."));
        acessoOrganizacionalServico.exigirConsultaEmpregado(user, empregado);
        StatusEmpregado inativo = statusRepositorio.findByDescricaoIgnoreCase(ConstantesDominio.STATUS_INATIVO)
                .orElseThrow(() -> new IllegalStateException("Status INATIVO não configurado."));
        empregado.setStatus(inativo);
        if (empregado.getContratos() == null) empregado.setContratos(new ArrayList<>());
        else empregado.getContratos().clear();

        Empregado salvo = empregadoRepositorio.save(empregado);
        gerenciarHistoricoContratos(
                salvo,
                List.of(),
                "Desativação do colaborador"
        );
        registrarHistorico(salvo, "Colaborador desativado (Soft Delete)", user);
        auditLogServico.registrarAcao("EMPREGADO_SOFT_DELETE", "Empregado", id, empregado.getNome(),
                RegistroAuditoriaServico.detalhes("cpf", empregado.getCpf(), "action", "Desativado por Soft Delete"));
        return;
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
                contratoJoin = root.join("contratos", jakarta.persistence.criteria.JoinType.INNER);
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

    private void resolverEValidarGestaoLocal(UsuarioAplicacao user, Empregado empregado) {
        GestaoLocal gestaoLocal = empregado.getLotacao() != null ? empregado.getLotacao().getGestaoLocal() : null;
        if (gestaoLocal == null) throw new IllegalArgumentException("A lotação selecionada ainda não está vinculada a uma Gestão Local.");
        if (empregado.getJornada() != null && empregado.getJornada().getGestaoLocal() != null
                && !Objects.equals(empregado.getJornada().getGestaoLocal().getId(), gestaoLocal.getId())) {
            throw new IllegalArgumentException("O turno selecionado pertence a outra Gestão Local.");
        }

        boolean ativo = empregado.getStatus() == null
                || !ConstantesDominio.STATUS_INATIVO.equalsIgnoreCase(empregado.getStatus().getDescricao());
        int quantidadeContratos = empregado.getContratos() == null ? 0 : empregado.getContratos().size();
        if (quantidadeContratos > 1) {
            throw new IllegalArgumentException(
                    "O empregado pode possuir somente um contrato atual. "
                            + "Encerre o vínculo anterior antes de informar outro contrato."
            );
        }
        if (ativo && quantidadeContratos != 1) {
            throw new IllegalArgumentException(
                    "Todo empregado ativo deve estar vinculado a exatamente um contrato da Gestão Local responsável."
            );
        }
        if (empregado.getContratos() != null) {
            for (Contrato contrato : empregado.getContratos()) {
                if (contrato == null || contrato.getGestaoLocal() == null
                        || !Objects.equals(gestaoLocal.getId(), contrato.getGestaoLocal().getId())) {
                    throw new IllegalArgumentException("Todos os contratos do empregado devem pertencer à mesma Gestão Local da lotação.");
                }
                if (ativo && !Boolean.TRUE.equals(contrato.getAtivo())) {
                    throw new IllegalArgumentException("Empregado ativo não pode ser vinculado a contrato inativo.");
                }
            }
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && (user.getGestaoLocal() == null || !Objects.equals(user.getGestaoLocal().getId(), gestaoLocal.getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "A Gestão Local não pode cadastrar empregado em outra unidade.");
        }
        empregado.setGestaoLocal(gestaoLocal);
    }

    private void mapearBasicos(EmpregadoDTO dto, Empregado entity) {
        entity.setNome(dto.nome());
        entity.setMatricula(dto.matricula());
        entity.setCpf(dto.cpf());
        entity.setRg(dto.rg());
        entity.setCtpsSerie(dto.ctpsSerie());
        entity.setNitPisPasep(dto.nitPisPasep());
        entity.setFormacaoEscolar(dto.formacaoEscolar());
        entity.setOptanteFgts(dto.optanteFgts() != null ? dto.optanteFgts() : false);
        entity.setDataOpcaoFgts(dto.dataOpcaoFgts());
        entity.setQtdDependenteIr(dto.qtdDependenteIr());
        entity.setQtdDependenteSalFamilia(dto.qtdDependenteSalFamilia());
        entity.setQtdBeneficiariosPensaoAlim(dto.qtdBeneficiariosPensaoAlim());
        entity.setOptanteVa(dto.optanteVa() != null ? dto.optanteVa() : false);
        entity.setValorVa(dto.valorVa());
        entity.setOutrosBeneficios(dto.outrosBeneficios());
        entity.setObservacao(dto.observacao());
        entity.setEmail(dto.email());
        entity.setDataNascimento(dto.dataNascimento());
        entity.setDataAdmissao(dto.dataAdmissao());
    }

    private void mapearRelacionamentos(EmpregadoDTO dto, Empregado entity) {
        // --- STATUS ---
        if (dto.statusId() != null) {
            entity.setStatus(statusRepositorio.findById(dto.statusId())
                    .orElseThrow(() -> new IllegalArgumentException("Erro de ID: o status informado não existe.")));
        } else if (entity.getStatus() == null) {
            entity.setStatus(statusRepositorio.findByDescricaoIgnoreCase(ConstantesDominio.STATUS_ATIVO)
                    .orElseThrow(() -> new IllegalStateException("Status ATIVO não configurado.")));
        }

        // --- LOTAÇÃO ---
        if (dto.lotacaoId() != null) {
            Lotacao lotacao = lotacaoRepositorio.findById(dto.lotacaoId())
                    .orElseThrow(() -> new IllegalArgumentException("Erro de ID: A Lotação enviada não existe no banco de dados."));
            entity.setLotacao(lotacao);
        }

        // --- CARGO ---
        if (dto.cargoId() != null) {
            Cargo cargo = cargoRepositorio.findById(dto.cargoId())
                    .orElseThrow(() -> new IllegalArgumentException("Erro de ID: O Cargo enviado não existe no banco de dados."));
            entity.setCargo(cargo);
        }

        // --- JORNADA / TURNO ---
        if (dto.jornadaId() != null) {
            Jornada j = jornadaRepositorio.findById(dto.jornadaId())
                    .orElseThrow(() -> new IllegalArgumentException("Erro de ID: O Turno enviado não existe no banco de dados."));
            validarJornada(j);
            if (entity.getLotacao() != null) validarTurnoDaLotacao(entity.getLotacao(), j);
            entity.setJornada(j);
        } else {
            entity.setJornada(null);
        }

        boolean isInativo = entity.getStatus() != null && ConstantesDominio.STATUS_INATIVO.equalsIgnoreCase(entity.getStatus().getDescricao());
        if (isInativo) {
            if (entity.getContratos() == null) entity.setContratos(new ArrayList<>());
            else entity.getContratos().clear();
        } else {
            if (dto.contratos() != null) {
                Map<Long, Contrato> contratosUnicos = new LinkedHashMap<>();
                for (EmpregadoContratoDTO cDto : dto.contratos()) {
                    if (cDto != null && cDto.id() != null) {
                        Contrato contrato = contratoRepositorio.findById(cDto.id())
                                .orElseThrow(() -> new IllegalArgumentException("Contrato informado não existe."));
                        contratosUnicos.putIfAbsent(contrato.getId(), contrato);
                    }
                }
                if (contratosUnicos.size() > 1) {
                    throw new IllegalArgumentException(
                            "O empregado pode possuir somente um contrato atual. "
                                    + "Informe apenas um contrato no cadastro."
                    );
                }
                entity.setContratos(new ArrayList<>(contratosUnicos.values()));
            }
        }

        if (dto.conta() != null) {
            EmpregadoContaDTO cReq = dto.conta();
            boolean hasNum = cReq.numeroConta() != null && !cReq.numeroConta().isBlank();
            boolean hasAgencia = cReq.agenciaId() != null && cReq.agenciaId() > 0;
            boolean hasTipo = cReq.tipoContaId() != null && cReq.tipoContaId() > 0;

            if (hasNum || hasAgencia || hasTipo) {
                if (!hasNum || !hasAgencia || !hasTipo) {
                    throw new IllegalArgumentException(
                            "Número da conta, agência e tipo da conta devem ser informados juntos."
                    );
                }

                Conta cAtu = entity.getConta() == null ? new Conta() : entity.getConta();
                cAtu.setNumeroConta(cReq.numeroConta().trim());
                cAtu.setAgencia(agenciaRepositorio.findById(cReq.agenciaId())
                        .orElseThrow(() -> new IllegalArgumentException("A agência informada não existe.")));
                cAtu.setTipoConta(tipoContaRepositorio.findById(cReq.tipoContaId())
                        .orElseThrow(() -> new IllegalArgumentException("O tipo de conta informado não existe.")));
                entity.setConta(cAtu);
            } else {
                entity.setConta(null);
            }
        } else {
            entity.setConta(null);
        }

        if (dto.endereco() != null) {
            EmpregadoEnderecoDTO eReq = dto.endereco();
            Endereco eAtu = entity.getEndereco() == null ? new Endereco() : entity.getEndereco();
            boolean possuiEndereco = (eReq.logradouro() != null && !eReq.logradouro().isBlank())
                    || (eReq.cep() != null && !eReq.cep().isBlank())
                    || (eReq.numero() != null && !eReq.numero().isBlank())
                    || (eReq.bairro() != null && !eReq.bairro().isBlank())
                    || (eReq.cidade() != null && !eReq.cidade().isBlank())
                    || (eReq.uf() != null && !eReq.uf().isBlank());

            if (possuiEndereco) {
                String logradouro = eReq.logradouro() == null ? null : eReq.logradouro().trim();
                if ((logradouro == null || logradouro.isBlank()) && eAtu.getId() == null) {
                    throw new IllegalArgumentException("O logradouro é obrigatório quando o endereço é informado.");
                }
                if (eReq.cep() != null) eAtu.setCep(eReq.cep().replaceAll("\\D", ""));
                if (logradouro != null && !logradouro.isBlank()) eAtu.setLogradouro(logradouro.toUpperCase());
                if (eReq.numero() != null) eAtu.setNumero(eReq.numero().trim());
                if (eReq.bairro() != null) eAtu.setBairro(eReq.bairro().trim().toUpperCase());
                if (eReq.cidade() != null) eAtu.setCidade(eReq.cidade().trim().toUpperCase());
                if (eReq.uf() != null) {
                    String uf = eReq.uf().trim().toUpperCase(Locale.ROOT);
                    if (!ConstantesDominio.UFS_BRASIL.contains(uf)) {
                        throw new IllegalArgumentException("A UF informada no endereço é inválida.");
                    }
                    eAtu.setUf(uf);
                }
                entity.setEndereco(eAtu);
            } else {
                entity.setEndereco(null);
            }
        } else {
            entity.setEndereco(null);
        }

        if (dto.telefones() != null) {
            if (entity.getTelefones() == null) entity.setTelefones(new ArrayList<>());
            entity.getTelefones().clear();
            for (EmpregadoTelefoneDTO tReq : dto.telefones()) {
                if (tReq == null || tReq.numero() == null || tReq.numero().isBlank()) continue;
                Telefone t = new Telefone();
                t.setNumero(tReq.numero().replaceAll("\\D", ""));
                t.setEmpregado(entity);
                entity.getTelefones().add(t);
            }
        }
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

        return pagina.map(empregado -> paraListaDTO(
                empregado,
                contratosPorEmpregado.getOrDefault(empregado.id(), List.of()), usuario));
    }

    private EmpregadoListaDTO paraListaDTO(
            EmpregadoListaBaseDTO empregado,
            List<EmpregadoContratoDTO> contratos,
            UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
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
        if (e == null) return null;
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        boolean completo = minimizador.completo(nivel);

        EmpregadoContaDTO contaDTO = null;
        if (completo && e.getConta() != null) {
            Conta c = e.getConta();
            Long agId = c.getAgencia() != null ? c.getAgencia().getId() : null;
            String agNum = c.getAgencia() != null ? c.getAgencia().getNumero() : null;
            Long bkId = (c.getAgencia() != null && c.getAgencia().getBanco() != null) ? c.getAgencia().getBanco().getId() : null;
            String bkNome = (c.getAgencia() != null && c.getAgencia().getBanco() != null) ? c.getAgencia().getBanco().getNome() : null;
            Long tcId = c.getTipoConta() != null ? c.getTipoConta().getId() : null;
            String tcDesc = c.getTipoConta() != null ? c.getTipoConta().getDescricao() : null;
            contaDTO = new EmpregadoContaDTO(c.getId(), c.getNumeroConta(), agId, agNum, bkId, bkNome, tcId, tcDesc);
        }

        EmpregadoEnderecoDTO endDTO = null;
        if (completo && e.getEndereco() != null) {
            Endereco end = e.getEndereco();
            endDTO = new EmpregadoEnderecoDTO(end.getId(), end.getLogradouro(), end.getNumero(), end.getBairro(), end.getCidade(), end.getUf(), end.getCep());
        }

        List<EmpregadoTelefoneDTO> telefonesDTO = new ArrayList<>();
        if (completo && e.getTelefones() != null) {
            telefonesDTO = e.getTelefones().stream().map(t -> new EmpregadoTelefoneDTO(t.getId(), t.getNumero())).collect(Collectors.toList());
        }

        List<EmpregadoContratoDTO> contratosDTO = new ArrayList<>();
        if (e.getContratos() != null) {
            contratosDTO = e.getContratos().stream().map(c -> new EmpregadoContratoDTO(c.getId(), c.getNumeroContrato(), c.getDescricao(), c.getAtivo())).collect(Collectors.toList());
        }

        return new EmpregadoDTO(
                e.getId(), minimizador.nome(e.getNome(), nivel),
                minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(e.getMatricula()), nivel),
                minimizador.cpf(e.getCpf(), nivel), minimizador.restrito(e.getRg(), nivel),
                minimizador.restrito(e.getCtpsSerie(), nivel), minimizador.restrito(e.getNitPisPasep(), nivel),
                minimizador.restrito(e.getFormacaoEscolar(), nivel), minimizador.restrito(e.getOptanteFgts(), nivel),
                minimizador.restrito(e.getDataOpcaoFgts(), nivel), minimizador.restrito(e.getQtdDependenteIr(), nivel),
                minimizador.restrito(e.getQtdDependenteSalFamilia(), nivel),
                minimizador.restrito(e.getQtdBeneficiariosPensaoAlim(), nivel),
                minimizador.restrito(e.getOptanteVa(), nivel), minimizador.restrito(e.getValorVa(), nivel),
                minimizador.restrito(e.getOutrosBeneficios(), nivel), minimizador.restrito(e.getObservacao(), nivel),
                minimizador.restrito(e.getEmail(), nivel), minimizador.restrito(e.getDataNascimento(), nivel), e.getDataAdmissao(),
                e.getStatus() != null ? e.getStatus().getId() : null, e.getStatus() != null ? e.getStatus().getDescricao() : null,
                e.getLotacao() != null ? e.getLotacao().getId() : null, e.getLotacao() != null ? e.getLotacao().getNome() : null,
                e.getCargo() != null ? e.getCargo().getId() : null, e.getCargo() != null ? e.getCargo().getNome() : null,
                e.getJornada() != null ? e.getJornada().getId() : null, e.getJornada() != null ? e.getJornada().getDescricao() : null,
                contaDTO, endDTO, telefonesDTO, contratosDTO,
                e.getGestaoLocal() != null ? e.getGestaoLocal().getId() : null,
                e.getGestaoLocal() != null ? e.getGestaoLocal().getNome() : null,
                e.getGestaoLocal() != null && e.getGestaoLocal().getSuperintendencia() != null ? e.getGestaoLocal().getSuperintendencia().getId() : null,
                e.getGestaoLocal() != null && e.getGestaoLocal().getSuperintendencia() != null ? e.getGestaoLocal().getSuperintendencia().getNome() : null,
                e.getGestaoLocal() != null && e.getGestaoLocal().getSuperintendencia() != null ? e.getGestaoLocal().getSuperintendencia().getUf() : null
        );
    }

    private EmpregadoFrequenciaDTO minimizarFrequencia(EmpregadoFrequenciaDTO dto, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new EmpregadoFrequenciaDTO(dto.id(), minimizador.nome(dto.nome(), nivel),
                minimizador.matricula(UtilitarioMatricula.formatarParaExibicao(dto.matricula()), nivel),
                minimizador.cpf(dto.cpf(), nivel), dto.lotacaoNome(), dto.lotacaoId(),
                dto.turnoDescricao(), dto.turnoId(), dto.cargoNome());
    }

    private void gerenciarHistoricoContratos(
            Empregado empregado,
            List<Contrato> contratosNovos,
            String observacao
    ) {
        Map<Long, Contrato> novosPorId = contratosNovos == null
                ? Map.of()
                : contratosNovos.stream()
                        .filter(Objects::nonNull)
                        .filter(contrato -> contrato.getId() != null)
                        .collect(Collectors.toMap(
                                Contrato::getId,
                                contrato -> contrato,
                                (primeiro, ignorado) -> primeiro,
                                LinkedHashMap::new
                        ));

        if (novosPorId.size() > 1) {
            throw new IllegalArgumentException(
                    "O empregado pode possuir somente um contrato atual."
            );
        }

        Contrato contratoNovo = novosPorId.values().stream().findFirst().orElse(null);
        Long contratoNovoId = contratoNovo != null ? contratoNovo.getId() : null;
        ZonedDateTime agora = ZonedDateTime.now(zoneBR);

        List<HistoricoVinculoContrato> historicosAtivos =
                historicoVinculoRepositorio
                        .findByEmpregadoIdAndDataFimIsNullOrderByDataInicioDesc(empregado.getId());

        boolean manteveVinculoAtual = false;
        List<HistoricoVinculoContrato> historicosParaEncerrar = new ArrayList<>();

        for (HistoricoVinculoContrato historico : historicosAtivos) {
            Long contratoHistoricoId = historico.getContrato() != null
                    ? historico.getContrato().getId()
                    : null;

            if (!manteveVinculoAtual && contratoNovoId != null
                    && Objects.equals(contratoNovoId, contratoHistoricoId)) {
                // Lotação, turno e cargo são dados operacionais do empregado e podem
                // mudar sem encerrar o vínculo contratual. O histórico cadastral
                // assinado registra essas alterações; o vínculo de contrato mantém a
                // data de início original enquanto o contrato atual continuar o mesmo.
                preencherContextoAlocacao(historico, empregado, contratoNovo);
                historicoVinculoRepositorio.save(historico);
                manteveVinculoAtual = true;
                continue;
            }

            historico.setDataFim(agora);
            String observacaoAnterior = historico.getObservacao();
            historico.setObservacao(
                    (observacaoAnterior == null || observacaoAnterior.isBlank()
                            ? ""
                            : observacaoAnterior + " | ")
                            + "Encerrado em: " + observacao
            );
            historicosParaEncerrar.add(historico);
        }

        if (!historicosParaEncerrar.isEmpty()) {
            historicoVinculoRepositorio.saveAll(historicosParaEncerrar);
            // Garante que o marcador vinculo_ativo dos vínculos encerrados seja
            // persistido como NULL antes de inserir um novo contrato ativo. Isso
            // evita violação das constraints uk_hist_* pela ordem de flush do Hibernate.
            historicoVinculoRepositorio.flush();
        }

        if (contratoNovo != null && !manteveVinculoAtual) {
            HistoricoVinculoContrato historico = new HistoricoVinculoContrato();
            historico.setEmpregado(empregado);
            historico.setContrato(contratoNovo);
            historico.setDataInicio(agora);
            historico.setObservacao("Iniciado em: " + observacao);
            preencherContextoAlocacao(historico, empregado, contratoNovo);
            historicoVinculoRepositorio.save(historico);
        }
    }

    private boolean mesmoId(Object atual, Object novo) {
        return Objects.equals(idEntidadeAlocacao(atual), idEntidadeAlocacao(novo));
    }

    private Long idEntidadeAlocacao(Object entidade) {
        if (entidade instanceof Lotacao lotacao) return lotacao.getId();
        if (entidade instanceof Jornada jornada) return jornada.getId();
        if (entidade instanceof Cargo cargo) return cargo.getId();
        return null;
    }

    private void preencherContextoAlocacao(HistoricoVinculoContrato historico,
                                            Empregado empregado,
                                            Contrato contrato) {
        historico.setLotacao(empregado.getLotacao());
        historico.setJornada(empregado.getJornada());
        historico.setCargo(empregado.getCargo());
        historico.setEmpregadoSnapshotNome(empregado.getNome());
        historico.setContratoSnapshotNumero(contrato != null ? contrato.getNumeroContrato() : null);
        historico.setLotacaoSnapshotNome(empregado.getLotacao() != null ? empregado.getLotacao().getNome() : null);
        historico.setLotacaoSnapshotMunicipio(empregado.getLotacao() != null ? empregado.getLotacao().getMunicipio() : null);
        historico.setJornadaSnapshotDescricao(empregado.getJornada() != null ? empregado.getJornada().getDescricao() : null);
        historico.setJornadaSnapshotDiasTrabalho(serializarDiasTrabalho(empregado.getJornada()));
        historico.setJornadaSnapshotMinutosDia(serializarMinutosPorDia(empregado.getJornada()));
        historico.setCargoSnapshotNome(empregado.getCargo() != null ? empregado.getCargo().getNome() : null);
    }

    private String serializarDiasTrabalho(Jornada jornada) {
        if (jornada == null || jornada.getDias() == null) return null;
        return jornada.getDias().stream()
                .filter(dia -> Boolean.TRUE.equals(dia.getDiaTrabalho()))
                .map(dia -> dia.getDiaSemana().name())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String serializarMinutosPorDia(Jornada jornada) {
        if (jornada == null || jornada.getDias() == null) return null;
        return jornada.getDias().stream()
                .filter(dia -> Boolean.TRUE.equals(dia.getDiaTrabalho()))
                .sorted(Comparator.comparing(JornadaDiaSemana::getDiaSemana))
                .map(dia -> dia.getDiaSemana().name() + ":" + (
                        UtilitarioHorario.calcularIntervaloMinutos(dia.getEntrada1(), dia.getSaida1(), "entrada 1", "saída 1")
                                + UtilitarioHorario.calcularIntervaloMinutos(dia.getEntrada2(), dia.getSaida2(), "entrada 2", "saída 2")))
                .collect(Collectors.joining(";"));
    }

    // empregado_contrato representa o vínculo atual; HistoricoVinculoContrato
    // é a fonte temporal canônica da alocação.
    private void validarJornada(Jornada jornada) {
        if (jornada == null) return;
        try {
            if (jornada.getDias() != null && !jornada.getDias().isEmpty()) {
                for (JornadaDiaSemana dia : jornada.getDias()) {
                    if (Boolean.TRUE.equals(dia.getDiaTrabalho())) {
                        long minutos = UtilitarioHorario.calcularIntervaloMinutos(
                                dia.getEntrada1(), dia.getSaida1(), "entrada 1 da jornada", "saída 1 da jornada")
                                + UtilitarioHorario.calcularIntervaloMinutos(
                                dia.getEntrada2(), dia.getSaida2(), "entrada 2 da jornada", "saída 2 da jornada");
                        if (minutos > maxJornadaMinutes) {
                            throw new IllegalArgumentException("A jornada selecionada excede o limite legal de " + (maxJornadaMinutes / 60) + " horas no dia: " + dia.getDiaSemana());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }


    private void normalizarDados(Empregado emp) {
        if (emp.getCpf() != null) emp.setCpf(UtilitarioCpf.somenteDigitos(emp.getCpf()));
        if (emp.getNome() != null) emp.setNome(emp.getNome().trim().toUpperCase());
        if (emp.getEmail() != null) emp.setEmail(emp.getEmail().trim().toLowerCase());
        emp.setMatricula(UtilitarioMatriculaEmpregado.normalizar(emp.getMatricula()));
        if (emp.getRg() != null) emp.setRg(emp.getRg().trim().toUpperCase());
        if (emp.getCtpsSerie() != null) emp.setCtpsSerie(emp.getCtpsSerie().trim().toUpperCase());
        if (emp.getNitPisPasep() != null) emp.setNitPisPasep(emp.getNitPisPasep().replaceAll("\\D", ""));
        if (emp.getFormacaoEscolar() != null) emp.setFormacaoEscolar(emp.getFormacaoEscolar().trim().toUpperCase());
        if (emp.getOutrosBeneficios() != null) emp.setOutrosBeneficios(emp.getOutrosBeneficios().trim().toUpperCase());
        if (emp.getObservacao() != null) emp.setObservacao(emp.getObservacao().trim());
    }

    private void validarObrigatoriosCadastro(Empregado emp) {
        if (emp == null) throw new IllegalArgumentException("Empregado inválido");
        if (emp.getNome() == null || emp.getNome().isBlank()) throw new IllegalArgumentException("Nome é obrigatório.");
        if (emp.getDataAdmissao() == null) throw new IllegalArgumentException("Data de admissão é obrigatória.");
        if (emp.getLotacao() == null || emp.getLotacao().getId() == null) throw new IllegalArgumentException("Lotação é obrigatória.");
        if (emp.getCargo() == null || emp.getCargo().getId() == null) throw new IllegalArgumentException("Cargo é obrigatório.");
        if (emp.getJornada() == null || emp.getJornada().getId() == null) throw new IllegalArgumentException("Turno/Jornada é obrigatório.");

        if (emp.getNitPisPasep() != null && !emp.getNitPisPasep().isBlank() && emp.getNitPisPasep().replaceAll("\\D", "").length() != 11) {
            throw new IllegalArgumentException("NIT/PIS/PASEP deve conter 11 dígitos.");
        }

        if (Boolean.TRUE.equals(emp.getOptanteFgts()) && emp.getDataOpcaoFgts() == null) throw new IllegalArgumentException("Data opção FGTS é obrigatória quando optante FGTS = SIM.");
        if (Boolean.TRUE.equals(emp.getOptanteVa())) {
            if (emp.getValorVa() == null) throw new IllegalArgumentException("Valor VA é obrigatório quando optante VA = SIM.");
            if (emp.getValorVa().signum() <= 0) throw new IllegalArgumentException("Valor VA deve ser maior que zero.");
        }
    }

    private void validarCpfObrigatorio(Empregado emp) {
        if (emp == null || emp.getCpf() == null || emp.getCpf().isBlank()) throw new IllegalArgumentException("CPF é obrigatório.");
        if (!UtilitarioCpf.ehValido(emp.getCpf())) throw new IllegalArgumentException("CPF inválido.");
    }

    private void validarTurnoDaLotacao(Lotacao lotacao, Jornada jornada) {
        if (lotacao == null || lotacao.getId() == null || jornada == null || jornada.getId() == null) return;
        Long lotId = lotacao.getId();
        Long jornadaId = jornada.getId();

        if (jornada.getLotacoes() != null && !jornada.getLotacoes().isEmpty()) {
            if (jornada.getLotacoes().stream().filter(java.util.Objects::nonNull).anyMatch(l -> l.getId() != null && l.getId().equals(lotId))) return;
        }

        Lotacao lot = lotacaoRepositorio.findById(lotId).orElse(lotacao);
        if (lot.getTurnos() == null || lot.getTurnos().stream().noneMatch(t -> t != null && t.getId() != null && t.getId().equals(jornadaId))) {
            throw new IllegalArgumentException("O turno selecionado não está habilitado para a lotação informada.");
        }
    }

    private void registrarHistorico(Empregado empregado, String descricao, UsuarioAplicacao usuario) {
        HistoricoEmpregado hist = new HistoricoEmpregado();
        hist.setEmpregado(empregado);
        hist.setDataEvento(LocalDateTime.now(zoneBR));
        hist.setDescricao(descricao);
        hist.setUsuario(usuario);
        historicoIntegridadeServico.salvarAssinado(hist);
    }
}
