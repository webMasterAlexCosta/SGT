package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.ServicoChaveCacheRedis;
import com.example.siggtrat.configuracao.propriedades.PropriedadesFrequencia;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.frequencia.*;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.dto.PaginaCacheDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.UtilitarioHorario;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Transactional
public class FrequenciaServico {

    private final FrequenciaRepositorio repositorio;
    private final FrequenciaConsultaRepositorio frequenciaConsultaRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final CalendarioEventoRepositorio calendarioEventoRepositorio;
    private final FrequenciaExcecaoRepositorio frequenciaExcecaoRepositorio;
    private final FrequenciaAdicionalRepositorio frequenciaAdicionalRepositorio;
    private final RegistroAuditoriaServico auditLogServico;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final CalculoFrequenciaServico calculoFrequenciaServico;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final HistoricoIntegridadeServico historicoIntegridadeServico;
    private final FechamentoFrequenciaRepositorio fechamentoFrequenciaRepositorio;
    private final ConsultaCacheRedisServico redisQueryCacheServico;
    private final ServicoChaveCacheRedis redisCacheKeyServico;
    private final ResolvedorContextoTemporalFrequenciaServico resolvedorContextoTemporal;
    private final PoliticaTemporalFrequenciaServico politicaTemporal;
    private final AuditoriaFrequenciaServico auditoriaFrequencia;
    private PendenciaAdministrativaServico pendencias;
    private InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios;
    private ProjecaoOperacionalPessoaServico projecaoOperacionalPessoa;

    private final long maxRangeDays;
    private final int maxBatchSize;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public FrequenciaServico(FrequenciaRepositorio repositorio,
                             FrequenciaConsultaRepositorio frequenciaConsultaRepositorio,
                             EmpregadoRepositorio empregadoRepositorio,
                             UsuarioAplicacaoRepositorio appUserRepositorio,
                             CalendarioEventoRepositorio calendarioEventoRepositorio,
                             FrequenciaExcecaoRepositorio frequenciaExcecaoRepositorio,
                             FrequenciaAdicionalRepositorio frequenciaAdicionalRepositorio,
                             RegistroAuditoriaServico auditLogServico,
                             EscopoAcessoUsuarioServico userAccessScopeServico,
                             CalculoFrequenciaServico calculoFrequenciaServico,
                             AcessoOrganizacionalServico acessoOrganizacionalServico,
                             HistoricoIntegridadeServico historicoIntegridadeServico,
                             FechamentoFrequenciaRepositorio fechamentoFrequenciaRepositorio,
                             ConsultaCacheRedisServico redisQueryCacheServico,
                             ServicoChaveCacheRedis redisCacheKeyServico,
                             ResolvedorContextoTemporalFrequenciaServico resolvedorContextoTemporal,
                             PoliticaTemporalFrequenciaServico politicaTemporal,
                             AuditoriaFrequenciaServico auditoriaFrequencia,
                             PropriedadesFrequencia properties) {
        this.repositorio = repositorio;
        this.frequenciaConsultaRepositorio = frequenciaConsultaRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.appUserRepositorio = appUserRepositorio;
        this.calendarioEventoRepositorio = calendarioEventoRepositorio;
        this.frequenciaExcecaoRepositorio = frequenciaExcecaoRepositorio;
        this.frequenciaAdicionalRepositorio = frequenciaAdicionalRepositorio;
        this.auditLogServico = auditLogServico;
        this.userAccessScopeServico = userAccessScopeServico;
        this.calculoFrequenciaServico = calculoFrequenciaServico;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.historicoIntegridadeServico = historicoIntegridadeServico;
        this.fechamentoFrequenciaRepositorio = fechamentoFrequenciaRepositorio;
        this.redisQueryCacheServico = redisQueryCacheServico;
        this.redisCacheKeyServico = redisCacheKeyServico;
        this.resolvedorContextoTemporal = resolvedorContextoTemporal;
        this.politicaTemporal = politicaTemporal;
        this.auditoriaFrequencia = auditoriaFrequencia;
        this.maxRangeDays = properties.maxRangeDays();
        this.maxBatchSize = properties.maxBatchSize();
    }

    @Autowired(required = false)
    public void configurarPendenciasRelatorios(PendenciaAdministrativaServico pendencias,
                                                InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios) {
        this.pendencias = pendencias;
        this.invalidacaoRelatorios = invalidacaoRelatorios;
    }

    @Autowired(required = false)
    public void configurarProjecaoOperacional(ProjecaoOperacionalPessoaServico servico) {
        this.projecaoOperacionalPessoa = servico;
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> buscarPorData(LocalDate data,
                                         Long lotacaoId,
                                         Long jornadaId,
                                         Long contratoId,
                                         Long gestaoLocalId,
                                         Long superintendenciaId,
                                         Pageable pageable) {
        return buscarPorData(data, lotacaoId, jornadaId, contratoId, gestaoLocalId,
                superintendenciaId, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> buscarPorData(LocalDate data,
                                         Long lotacaoId,
                                         Long jornadaId,
                                         Long contratoId,
                                         Long gestaoLocalId,
                                         Long superintendenciaId,
                                         boolean skipCount,
                                         Pageable pageable) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        UsuarioAplicacao user = usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaFrequencia(user);
        exigirFiltroOrganizacionalParaConsultaGlobal(user, lotacaoId, jornadaId, contratoId, gestaoLocalId, superintendenciaId);

        Specification<Frequencia> spec = porPeriodo(data, data)
                .and(porFiltrosOrganizacionais(lotacaoId, jornadaId, contratoId, gestaoLocalId, superintendenciaId))
                .and(porEscopo(user));
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by("empregadoSnapshotNome").ascending().and(Sort.by("id").ascending()));
        String chaveCache = redisCacheKeyServico.consulta(
                "frequencias-dia", data, lotacaoId, jornadaId, contratoId, gestaoLocalId,
                superintendenciaId, skipCount,
                limitado.getPageNumber(), limitado.getPageSize(), limitado.getSort());
        PaginaCacheDTO<FrequenciaDTO> paginaCache = redisQueryCacheServico.frequencias(
                chaveCache,
                () -> PaginaCacheDTO.dePagina(frequenciaConsultaRepositorio
                        .buscar(spec, limitado, skipCount).map(this::paraDTO)));
        return paginaCache.paraPagina(limitado);
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> obterIntervalo(LocalDate inicio,
                                        LocalDate fim,
                                        Long lotacaoId,
                                        Long jornadaId,
                                        Long contratoId,
                                        Long gestaoLocalId,
                                        Long superintendenciaId,
                                        String status,
                                        Pageable pageable) {
        return obterIntervalo(inicio, fim, lotacaoId, jornadaId, contratoId, gestaoLocalId,
                superintendenciaId, status, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> obterIntervalo(LocalDate inicio,
                                        LocalDate fim,
                                        Long lotacaoId,
                                        Long jornadaId,
                                        Long contratoId,
                                        Long gestaoLocalId,
                                        Long superintendenciaId,
                                        String status,
                                        boolean skipCount,
                                        Pageable pageable) {
        validarPeriodo(inicio, fim);
        UsuarioAplicacao user = usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaFrequencia(user);
        exigirFiltroOrganizacionalParaConsultaGlobal(user, lotacaoId, jornadaId, contratoId, gestaoLocalId, superintendenciaId);

        Specification<Frequencia> spec = porPeriodo(inicio, fim)
                .and(porFiltrosOrganizacionais(lotacaoId, jornadaId, contratoId, gestaoLocalId, superintendenciaId))
                .and(porStatus(status))
                .and(porEscopo(user));
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by("dataReferencia").ascending()
                        .and(Sort.by("empregado.nome").ascending())
                        .and(Sort.by("id").ascending()));
        String chaveCache = redisCacheKeyServico.consulta(
                "frequencias-range", inicio, fim, lotacaoId, jornadaId, contratoId, gestaoLocalId,
                superintendenciaId, status, skipCount,
                limitado.getPageNumber(), limitado.getPageSize(), limitado.getSort());
        PaginaCacheDTO<FrequenciaDTO> paginaCache = redisQueryCacheServico.frequencias(
                chaveCache,
                () -> PaginaCacheDTO.dePagina(frequenciaConsultaRepositorio
                        .buscar(spec, limitado, skipCount).map(this::paraDTO)));
        return paginaCache.paraPagina(limitado);
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> obterMensal(int mes,
                                         int ano,
                                         Long lotacaoId,
                                         Long jornadaId,
                                         Long contratoId,
                                         Long gestaoLocalId,
                                         Long superintendenciaId,
                                         Pageable pageable) {
        return obterMensal(mes, ano, lotacaoId, jornadaId, contratoId, gestaoLocalId,
                superintendenciaId, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FrequenciaDTO> obterMensal(int mes,
                                         int ano,
                                         Long lotacaoId,
                                         Long jornadaId,
                                         Long contratoId,
                                         Long gestaoLocalId,
                                         Long superintendenciaId,
                                         boolean skipCount,
                                         Pageable pageable) {
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês inválido.");
        }
        LocalDate inicio;
        try {
            inicio = LocalDate.of(ano, mes, 1);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês/ano inválidos.");
        }
        return obterIntervalo(inicio, inicio.with(TemporalAdjusters.lastDayOfMonth()), lotacaoId, jornadaId,
                contratoId, gestaoLocalId, superintendenciaId, null, skipCount, pageable);
    }

    public FrequenciaDTO salvarOuAtualizar(FrequenciaDTO dto) {
        exigirIdentidadeCanonicaPublica(dto);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAplicacao user = usuarioAtual(auth);
        return salvarOuAtualizarInterno(dto, user, new HashMap<>(), null, true, false);
    }

    @Transactional(readOnly = true)
    public List<com.example.siggtrat.dto.EventoFrequenciaDTO> listarEventos(Long id) {
        Frequencia frequencia = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Frequência não encontrada."));
        UsuarioAplicacao user = usuarioAtual();
        if (!acessoOrganizacionalServico.podeConsultarFrequencia(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso à frequência.");
        }
        if (!acessoOrganizacionalServico.ehGlobal(user)) {
            boolean noEscopo;
            if (acessoOrganizacionalServico.ehGestorEstadual(user)) {
                SuperintendenciaEstadual se = acessoOrganizacionalServico.superintendenciaEfetiva(user);
                noEscopo = se != null && Objects.equals(se.getId(), frequencia.getSuperintendenciaSnapshotId());
            } else {
                noEscopo = userAccessScopeServico.podeGerenciar(user,
                        frequencia.getGestaoLocalSnapshotId(), frequencia.getLotacaoSnapshotId(),
                        frequencia.getJornadaSnapshotId());
            }
            if (!noEscopo) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Frequência não encontrada no seu escopo.");
            }
        }
        return auditoriaFrequencia.listar(id);
    }

    /**
     * Lote transacional e limitado. Qualquer erro reverte o lote inteiro, evitando
     * o estado parcialmente salvo que existia quando o navegador fazia uma chamada
     * por colaborador.
     */
    public List<FrequenciaDTO> salvarOuAtualizarLote(List<FrequenciaDTO> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O lote deve possuir ao menos uma frequência.");
        }
        if (itens.size() > maxBatchSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "O lote excede o limite de " + maxBatchSize + " frequências.");
        }

        long canonicos = itens.stream().filter(i -> i != null && i.getVinculoContratualId() != null).count();
        if (canonicos != itens.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A API de frequência exige vinculoContratualId em todos os itens; empregado.id é apenas projeção interna.");
        }
        if (canonicos == itens.size()) {
            Set<String> chavesCanonicas = new HashSet<>();
            for (FrequenciaDTO item : itens) {
                validarEntrada(item);
                String chave = item.getVinculoContratualId() + "|" + item.getDataReferencia();
                if (!chavesCanonicas.add(chave)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "O lote contém frequência repetida para o mesmo vínculo e data.");
                }
            }
            UsuarioAplicacao user = usuarioAtual(SecurityContextHolder.getContext().getAuthentication());
            List<FrequenciaDTO> resposta = new ArrayList<>(itens.size());
            for (FrequenciaDTO item : itens) {
                resposta.add(salvarOuAtualizarInterno(item, user, new HashMap<>(), null, true, false));
            }
            return resposta;
        }

        Set<ChaveFrequencia> chaves = new HashSet<>();
        SortedSet<Long> empregadoIds = new TreeSet<>();
        LocalDate inicio = null;
        LocalDate fim = null;
        for (FrequenciaDTO item : itens) {
            validarEntrada(item);
            Long empregadoId = item.getEmpregado().getId();
            LocalDate data = item.getDataReferencia();
            ChaveFrequencia chave = new ChaveFrequencia(empregadoId, data);
            if (!chaves.add(chave)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O lote contém frequência repetida para o mesmo colaborador e data.");
            }
            empregadoIds.add(empregadoId);
            inicio = inicio == null || data.isBefore(inicio) ? data : inicio;
            fim = fim == null || data.isAfter(fim) ? data : fim;
        }

        ContextoLote contexto = prepararContextoLote(empregadoIds, inicio, fim);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioAplicacao user = usuarioAtual(auth);
        List<FrequenciaDTO> resposta = new ArrayList<>(itens.size());
        for (FrequenciaDTO item : itens) {
            resposta.add(salvarOuAtualizarInterno(item, user, contexto.calendario(), contexto, true, false));
        }
        return resposta;
    }

    /**
     * Processa um bloco de um fechamento massivo. A transação termina ao final
     * deste bloco, portanto um quadro de 100 mil colaboradores não mantém locks,
     * memória e conexão durante todo o trabalho.
     *
     * <p>Registros já existentes são preservados quando o usuário não os alterou.
     * Para empregados sem frequência é calculado o status padrão no servidor.
     * Apenas as alterações explícitas geram histórico individual; as presenças
     * implícitas são auditadas pelo fechamento agregado.</p>
     */
    @Transactional
    public ResultadoBlocoFechamento processarBlocoFechamento(Long fechamentoId,
                                                               List<Long> empregadoIds,
                                                               LocalDate dataReferencia,
                                                               Map<Long, FrequenciaDTO> alteracoes,
                                                               Long usuarioCriadorId) {
        if (empregadoIds == null || empregadoIds.isEmpty()) {
            return new ResultadoBlocoFechamento(0, 0, 0);
        }
        if (fechamentoId == null || dataReferencia == null || usuarioCriadorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contexto do fechamento de frequência inválido.");
        }

        UsuarioAplicacao user = appUserRepositorio.findById(usuarioCriadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuário criador do fechamento não foi encontrado."));
        acessoOrganizacionalServico.exigirFrequenciaDiaria(user);

        ContextoLote contexto = prepararContextoLote(empregadoIds, dataReferencia, dataReferencia);
        Set<Long> empregadosComFaltaRecente = new HashSet<>(
                repositorio.buscarIdsEmpregadosComFaltaNoPeriodo(
                        empregadoIds, dataReferencia.minusDays(6), dataReferencia.minusDays(1)));

        long inseridos = 0;
        long atualizados = 0;
        long preservados = 0;
        Map<Long, FrequenciaDTO> alteracoesSeguras = alteracoes == null ? Map.of() : alteracoes;

        for (Long empregadoId : empregadoIds) {
            Empregado empregado = contexto.empregados().get(empregadoId);
            if (empregado == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Colaborador do fechamento não foi encontrado: " + empregadoId + ".");
            }

            ChaveFrequencia chave = new ChaveFrequencia(empregadoId, dataReferencia);
            Frequencia existente = contexto.frequencias().get(chave);
            FrequenciaDTO alteracao = alteracoesSeguras.get(empregadoId);

            if (existente != null && alteracao == null) {
                preservados++;
                continue;
            }

            boolean alteracaoExplicita = alteracao != null;
            FrequenciaDTO dto = alteracaoExplicita
                    ? copiarAlteracaoFechamento(alteracao, empregadoId, dataReferencia)
                    : criarFrequenciaPadraoFechamento(empregado, dataReferencia, contexto,
                    empregadosComFaltaRecente.contains(empregadoId));

            salvarOuAtualizarInterno(dto, user, contexto.calendario(), contexto,
                    alteracaoExplicita, true);
            if (existente == null) inseridos++;
            else atualizados++;
        }

        atualizarProgressoDoFechamentoNaMesmaTransacao(
                fechamentoId,
                empregadoIds.get(empregadoIds.size() - 1),
                empregadoIds.size(),
                inseridos,
                atualizados,
                preservados
        );
        return new ResultadoBlocoFechamento(inseridos, atualizados, preservados);
    }

    /**
     * O cursor e os contadores são confirmados na mesma transação das frequências.
     * Assim, uma queda entre o INSERT e a atualização do job não perde progresso
     * nem obriga a reprocessar milhares de presenças já materializadas.
     */
    private void atualizarProgressoDoFechamentoNaMesmaTransacao(Long fechamentoId,
                                                                 Long ultimoEmpregadoId,
                                                                 long processadosNoBloco,
                                                                 long inseridosNoBloco,
                                                                 long atualizadosNoBloco,
                                                                 long preservadosNoBloco) {
        FechamentoFrequencia fechamento = fechamentoFrequenciaRepositorio.buscarPorIdParaAtualizacao(fechamentoId)
                .orElseThrow(() -> new IllegalStateException(
                        "Fechamento de frequência não encontrado durante a confirmação do bloco."));
        if (fechamento.getStatus() != FechamentoFrequenciaStatus.PROCESSANDO) {
            throw new IllegalStateException(
                    "O fechamento deixou de estar em processamento antes da confirmação do bloco.");
        }
        fechamento.setUltimoEmpregadoIdProcessado(ultimoEmpregadoId);
        fechamento.setTotalProcessado(fechamento.getTotalProcessado() + processadosNoBloco);
        fechamento.setTotalInserido(fechamento.getTotalInserido() + inseridosNoBloco);
        fechamento.setTotalAtualizado(fechamento.getTotalAtualizado() + atualizadosNoBloco);
        fechamento.setTotalPreservado(fechamento.getTotalPreservado() + preservadosNoBloco);
        fechamento.setHeartbeatEm(LocalDateTime.now(zoneId));
        fechamento.setMensagem("Processados " + fechamento.getTotalProcessado()
                + " de " + fechamento.getTotalPrevisto() + " colaboradores.");
        fechamentoFrequenciaRepositorio.save(fechamento);
    }

    private FrequenciaDTO copiarAlteracaoFechamento(FrequenciaDTO origem,
                                                     Long empregadoId,
                                                     LocalDate dataReferencia) {
        FrequenciaDTO dto = new FrequenciaDTO();
        dto.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(empregadoId, null));
        dto.setDataReferencia(dataReferencia);
        dto.setStatus(origem.getStatus());
        dto.setEntrada1(origem.getEntrada1());
        dto.setSaida1(origem.getSaida1());
        dto.setEntrada2(origem.getEntrada2());
        dto.setSaida2(origem.getSaida2());
        dto.setHorasTrabalhadas(origem.getHorasTrabalhadas());
        dto.setHorarioSaida(origem.getHorarioSaida());
        dto.setHorasNoturnas(origem.getHorasNoturnas());
        dto.setObservacao(origem.getObservacao());
        return dto;
    }

    private FrequenciaDTO criarFrequenciaPadraoFechamento(Empregado empregado,
                                                           LocalDate dataReferencia,
                                                           ContextoLote contexto,
                                                           boolean perdeuDsr) {
        ResolvedorContextoTemporalFrequenciaServico.Contexto temporal =
                resolvedorContextoTemporal.resolver(empregado, dataReferencia);

        Frequencia referencia = new Frequencia();
        referencia.setEmpregado(empregado);
        referencia.setDataReferencia(dataReferencia);
        capturarRetratoCanonico(referencia, temporal, dataReferencia, true);

        boolean feriado = temEventoCalendarioAplicavel(referencia, empregado, dataReferencia,
                ConstantesDominio.EVENTO_FERIADO, contexto.calendario());
        boolean descansoCalendario = temEventoCalendarioAplicavel(referencia, empregado, dataReferencia,
                ConstantesDominio.EVENTO_DIA_DESCANSO, contexto.calendario());
        boolean folgaRemunerada = existeExcecaoAtiva(empregado.getId(), dataReferencia,
                ConstantesDominio.EXCECAO_FOLGA_REMUNERADA, contexto);
        boolean folgaJornada = ehFolgaPelaJornada(referencia, empregado, dataReferencia);

        String status;
        if ((folgaJornada || descansoCalendario) && !folgaRemunerada && !feriado) {
            if (perdeuDsr) status = ConstantesDominio.FREQ_DSR_PERDIDO;
            else status = descansoCalendario ? ConstantesDominio.FREQ_DESCANSO : ConstantesDominio.FREQ_FOLGA;
        } else if (feriado || descansoCalendario || folgaRemunerada || folgaJornada) {
            status = ConstantesDominio.FREQ_FOLGA;
        } else {
            status = ConstantesDominio.FREQ_PRESENCA;
        }

        FrequenciaDTO dto = new FrequenciaDTO();
        dto.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(empregado.getId(), empregado.getNome()));
        dto.setDataReferencia(dataReferencia);
        dto.setStatus(status);
        return dto;
    }

    private ContextoLote prepararContextoLote(Collection<Long> empregadoIds, LocalDate inicio, LocalDate fim) {
        List<Empregado> bloqueados = empregadoRepositorio.buscarTodosPorIdParaAtualizacao(empregadoIds);
        if (bloqueados.size() != empregadoIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais colaboradores do lote não foram encontrados.");
        }

        // A chave de entrada identifica o registro operacional; a decisão de
        // Contrato/Lotação/Turno/Cargo usa Pessoa/Vínculo/Alocação na data negocial.
        Map<Long, Empregado> empregados = bloqueados.stream()
                .collect(java.util.stream.Collectors.toMap(Empregado::getId, e -> e));
        Map<ChaveFrequencia, Frequencia> frequencias = repositorio
                .findByEmpregadoIdInAndDataReferenciaBetween(empregadoIds, inicio, fim).stream()
                .collect(java.util.stream.Collectors.toMap(
                        f -> new ChaveFrequencia(f.getEmpregado().getId(), f.getDataReferencia()),
                        f -> f));

        Map<LocalDate, List<CalendarioEvento>> calendario = calendarioEventoRepositorio
                .findByDataBetween(inicio, fim).stream()
                .collect(java.util.stream.Collectors.groupingBy(CalendarioEvento::getData));
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            calendario.computeIfAbsent(data, ignorado -> new ArrayList<>());
        }

        Map<Long, List<FrequenciaExcecao>> excecoes = frequenciaExcecaoRepositorio
                .buscarAtivasPorEmpregadosEPeriodo(empregadoIds, inicio, fim).stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getEmpregado().getId()));
        Map<ChaveFrequencia, FrequenciaAdicional> adicionaisRt = frequenciaAdicionalRepositorio
                .findByEmpregadoIdInAndDataReferenciaBetweenAndTipoIgnoreCase(
                        empregadoIds, inicio, fim, ConstantesDominio.FREQ_RT).stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> new ChaveFrequencia(a.getEmpregado().getId(), a.getDataReferencia()),
                        a -> a));

        return new ContextoLote(empregados, frequencias, calendario, excecoes, adicionaisRt);
    }

    private FrequenciaDTO salvarOuAtualizarInterno(FrequenciaDTO dto,
                                                UsuarioAplicacao usuarioAtual,
                                                Map<LocalDate, List<CalendarioEvento>> calendarioCache,
                                                ContextoLote contexto,
                                                boolean registrarTrilhaIndividual,
                                                boolean escopoJaValidado) {
        validarEntrada(dto);
        acessoOrganizacionalServico.exigirFrequenciaDiaria(usuarioAtual);

        LocalDate dataRef = dto.getDataReferencia();
        LocalDate hoje = LocalDate.now(zoneId);
        if (dataRef.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é permitido registrar frequência em datas futuras.");
        }

        Empregado empregado;
        ResolvedorContextoTemporalFrequenciaServico.Contexto temporal;
        if (dto.getVinculoContratualId() != null) {
            if (projecaoOperacionalPessoa == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Projeção operacional da Pessoa indisponível.");
            }
            temporal = resolvedorContextoTemporal.resolverPorVinculo(dto.getVinculoContratualId(), dataRef);
            empregado = projecaoOperacionalPessoa.projetar(temporal.vinculo(), temporal.alocacao());
            if (dto.getEmpregado() != null && dto.getEmpregado().getId() != null
                    && !Objects.equals(dto.getEmpregado().getId(), empregado.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Empregado informado diverge da projeção da Pessoa/Vínculo.");
            }
        } else {
            empregado = contexto != null
                    ? contexto.empregados().get(dto.getEmpregado().getId())
                    : empregadoRepositorio.buscarPorIdParaAtualizacao(dto.getEmpregado().getId()).orElse(null);
            if (empregado == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empregado não encontrado.");
            }
            temporal = resolvedorContextoTemporal.resolver(empregado, dataRef);
        }
        exigirAutorizacaoNoContexto(usuarioAtual, temporal);

        ChaveFrequencia chave = new ChaveFrequencia(empregado.getId(), dataRef);
        Frequencia preexistente = contexto != null ? contexto.frequencias().get(chave)
                : dto.getVinculoContratualId() != null
                ? repositorio.findByVinculoContratualIdAndDataReferencia(dto.getVinculoContratualId(), dataRef).orElse(null)
                : repositorio.findByEmpregadoIdAndDataReferencia(empregado.getId(), dataRef).orElse(null);
        Frequencia entity = preexistente != null ? preexistente : new Frequencia();
        boolean isUpdate = preexistente != null;
        boolean gestor = acessoOrganizacionalServico.ehGestaoLocal(usuarioAtual);
        boolean fechadaPeloTempo = politicaTemporal.fechada(dataRef, null);

        if (isUpdate) {
            fecharNegocialmenteSeNecessario(entity);
            if (!gestor) {
                validarEdicaoOperacional(entity, dataRef);
            }
        } else if (!gestor && fechadaPeloTempo) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A frequência está FECHADA desde o início do 8º dia; somente o Gestor Local pode retificar.");
        }

        boolean intervencaoGerencial = gestor;
        String hashIntencaoRetificacao = null;
        if (intervencaoGerencial) {
            exigirRetificacaoGerencial(dto);
            hashIntencaoRetificacao = hashIntencaoRetificacao(dto, temporal, usuarioAtual);
            if (auditoriaFrequencia.chaveJaAplicada(isUpdate ? entity.getId() : null,
                    dto.getChaveIdempotencia(), hashIntencaoRetificacao)) {
                return paraDTO(entity);
            }
        }

        String snapshotAnterior = isUpdate ? auditoriaFrequencia.snapshot(entity) : null;
        if (!isUpdate) {
            entity.setEmpregado(empregado);
            entity.setDataReferencia(dataRef);
            entity.setAutorOriginalId(usuarioAtual.getId());
            entity.setAutorOriginalUsername(usuarioAtual.getUsername());
            entity.setEstadoNegocial(fechadaPeloTempo ? EstadoNegocialFrequencia.FECHADA : EstadoNegocialFrequencia.ABERTA);
            entity.setValidadeOperacional(ValidadeOperacionalFrequencia.VALIDA);
            entity.setEstadoAuditoria(EstadoAuditoriaFrequencia.ORIGINAL);
            entity.setBloqueadaEdicaoOperacional(false);
            entity.setVersaoNegocial(1);
            capturarRetratoCanonico(entity, temporal, dataRef, true);
        } else {
            entity.setVersaoNegocial(entity.getVersaoNegocial() == null ? 2 : entity.getVersaoNegocial() + 1);
            if (intervencaoGerencial) {
                entity.setEstadoAuditoria(EstadoAuditoriaFrequencia.RETIFICADA_GESTOR);
                entity.setValidadeOperacional(ValidadeOperacionalFrequencia.VALIDA);
                entity.setBloqueadaEdicaoOperacional(true);
                entity.setUltimaRetificacaoPorId(usuarioAtual.getId());
                entity.setUltimaRetificacaoPorUsername(usuarioAtual.getUsername());
                entity.setMotivoRevisao(null);
                // Retificação gerencial pode atualizar a verdade oficial para o novo contexto histórico.
                capturarRetratoCanonico(entity, temporal, dataRef, true);
            } else {
                entity.setEstadoAuditoria(EstadoAuditoriaFrequencia.ALTERADA_OPERACIONAL);
                // Operacional nunca reassocia silenciosamente um registro a outra Alocação.
                exigirMesmoContextoHistorico(entity, temporal);
            }
        }

        if (intervencaoGerencial && !isUpdate) {
            entity.setEstadoAuditoria(EstadoAuditoriaFrequencia.RETIFICADA_GESTOR);
            entity.setBloqueadaEdicaoOperacional(true);
            entity.setUltimaRetificacaoPorId(usuarioAtual.getId());
            entity.setUltimaRetificacaoPorUsername(usuarioAtual.getUsername());
        }

        String status = normalizarEValidarStatus(dto.getStatus());
        validaDiasDescanso(entity, empregado, dataRef, status, calendarioCache, contexto);

        entity.setStatus(status);
        entity.setObservacao(dto.getObservacao());
        entity.setEntrada1(UtilitarioHorario.normalizarHorario(dto.getEntrada1(), "entrada 1"));
        entity.setSaida1(UtilitarioHorario.normalizarHorario(dto.getSaida1(), "saída 1"));
        entity.setEntrada2(UtilitarioHorario.normalizarHorario(dto.getEntrada2(), "entrada 2"));
        entity.setSaida2(UtilitarioHorario.normalizarHorario(dto.getSaida2(), "saída 2"));
        entity.setHorasTrabalhadas(dto.getHorasTrabalhadas());
        entity.setHorarioSaida(UtilitarioHorario.normalizarHorario(dto.getHorarioSaida(), "horário de saída"));
        entity.setHorasNoturnas(dto.getHorasNoturnas());

        calculoFrequenciaServico.processarCalculos(entity);
        Frequencia saved = repositorio.saveAndFlush(entity);
        if (contexto != null) contexto.frequencias().put(chave, saved);
        reconciliarRt(saved, empregado, calendarioCache, contexto);

        TipoEventoFrequencia tipoEvento = !isUpdate
                ? (intervencaoGerencial ? TipoEventoFrequencia.RETIFICACAO_GESTOR : TipoEventoFrequencia.REGISTRO)
                : (intervencaoGerencial ? TipoEventoFrequencia.RETIFICACAO_GESTOR : TipoEventoFrequencia.ALTERACAO_OPERACIONAL);
        auditoriaFrequencia.registrar(saved, tipoEvento, snapshotAnterior,
                intervencaoGerencial ? dto.getJustificativaRetificacao() : null, usuarioAtual,
                intervencaoGerencial ? dto.getChaveIdempotencia() : null,
                intervencaoGerencial ? hashIntencaoRetificacao : null);

        if (intervencaoGerencial) {
            if (invalidacaoRelatorios != null) {
                invalidacaoRelatorios.invalidarPorFrequencia(saved,
                        dto.getJustificativaRetificacao(), usuarioAtual);
            }
            if (pendencias != null) {
                pendencias.resolverRevisoesFrequencia(saved.getId(), usuarioAtual,
                        "Retificação gerencial concluída na versão " + saved.getVersaoNegocial() + ".");
            }
        }

        if (registrarTrilhaIndividual) {
            registrarHistorico(saved, empregado, usuarioAtual, gestor, isUpdate);
            auditLogServico.registrarAcaoComoUsuario("FREQUENCIA_SAVE", "Frequencia", saved.getId(), empregado.getNome(),
                    RegistroAuditoriaServico.detalhes("empId", empregado.getId(), "data", saved.getDataReferencia(),
                            "status", saved.getStatus(), "vinculoId", id(saved.getVinculoContratual()),
                            "alocacaoId", id(saved.getAlocacaoVinculo()), "versaoNegocial", saved.getVersaoNegocial()),
                    usuarioAtual);
        }
        return paraDTO(saved);
    }

    /** Integrações internas também usam exclusivamente Pessoa/Vínculo/Alocação. */
    public void garantirSnapshotHistorico(Frequencia frequencia, Empregado empregado, LocalDate dataReferencia) {
        if (frequencia == null || empregado == null || dataReferencia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto da frequência inválido.");
        }
        capturarRetratoCanonico(frequencia, resolvedorContextoTemporal.resolver(empregado, dataReferencia),
                dataReferencia, false);
    }

    /** Mantido apenas como assinatura de integração; o Contrato informado não é fonte canônica. */
    public void garantirSnapshotHistorico(Frequencia frequencia,
                                           Empregado empregado,
                                           LocalDate dataReferencia,
                                           Contrato contratoInformado) {
        ResolvedorContextoTemporalFrequenciaServico.Contexto temporal =
                resolvedorContextoTemporal.resolver(empregado, dataReferencia);
        if (contratoInformado != null && !Objects.equals(contratoInformado.getId(), temporal.vinculo().getContrato().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato informado diverge do Vínculo Contratual válido na data.");
        }
        capturarRetratoCanonico(frequencia, temporal, dataReferencia, false);
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, Contrato> resolverContratosVigentesNoPeriodo(Empregado empregado,
                                                                       LocalDate inicio,
                                                                       LocalDate fim) {
        if (empregado == null || inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período contratual inválido.");
        }
        Map<LocalDate, Contrato> contratos = new LinkedHashMap<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            contratos.put(data, resolvedorContextoTemporal.resolver(empregado, data).vinculo().getContrato());
        }
        return contratos;
    }

    private void validarEntrada(FrequenciaDTO dto) {
        if (dto == null || dto.getVinculoContratualId() == null
                && (dto.getEmpregado() == null || dto.getEmpregado().getId() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "vinculoContratualId é obrigatório; empregado.id é aceito apenas para compatibilidade.");
        }
        if (dto.getDataReferencia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de referência obrigatória.");
        }
        validarHorarios(dto);
    }

    private void exigirIdentidadeCanonicaPublica(FrequenciaDTO dto) {
        if (dto == null || dto.getVinculoContratualId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "vinculoContratualId é obrigatório no contrato público de Frequência.");
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datas inválidas.");
        }
        long daysBetween = ChronoUnit.DAYS.between(inicio, fim);
        if (daysBetween > maxRangeDays || daysBetween < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período máximo de busca é de " + maxRangeDays + " dias.");
        }
    }

    private void exigirAutorizacaoNoContexto(UsuarioAplicacao usuario,
                                               ResolvedorContextoTemporalFrequenciaServico.Contexto temporal) {
        if (acessoOrganizacionalServico.ehAdministrador(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrador técnico não executa retificação negocial de frequência por padrão.");
        }
        Long gestaoId = temporal.gestaoHistorica() == null ? null : temporal.gestaoHistorica().getId();
        if (acessoOrganizacionalServico.ehGestaoLocal(usuario)) {
            if (usuario.getGestaoLocal() == null || !Objects.equals(usuario.getGestaoLocal().getId(), gestaoId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O Gestor Local atual não possui autorização sobre a Gestão histórica da frequência.");
            }
            return;
        }
        if (!acessoOrganizacionalServico.ehOperacional(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissão para editar frequência.");
        }
        Long lotacaoId = temporal.posicaoAlocacao().lotacaoId();
        Long jornadaId = temporal.posicaoAlocacao().jornadaId();
        if (!userAccessScopeServico.podeGerenciar(usuario, gestaoId, lotacaoId, jornadaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O usuário não possui autorização atual sobre o escopo histórico da frequência.");
        }
    }

    private void validarEdicaoOperacional(Frequencia frequencia, LocalDate dataRef) {
        if (!politicaTemporal.abertaParaOperacional(dataRef, null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A frequência está FECHADA; alteração posterior exige retificação do Gestor Local.");
        }
        if (Boolean.TRUE.equals(frequencia.getBloqueadaEdicaoOperacional())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este registro recebeu intervenção do Gestor Local e não retorna à edição do Operacional.");
        }
        if (frequencia.getValidadeOperacional() != ValidadeOperacionalFrequencia.VALIDA
                || frequencia.getEstadoAuditoria() == EstadoAuditoriaFrequencia.SUJEITA_REVISAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A frequência está sujeita a revisão histórica e deve ser tratada pelo Gestor Local.");
        }
    }

    private String hashIntencaoRetificacao(FrequenciaDTO dto,
                                           ResolvedorContextoTemporalFrequenciaServico.Contexto temporal,
                                           UsuarioAplicacao usuario) {
        String material = String.join("|",
                valorHash(usuario == null ? null : usuario.getId()),
                valorHash(dto.getVinculoContratualId() != null ? dto.getVinculoContratualId()
                        : dto.getEmpregado() == null ? null : dto.getEmpregado().getId()),
                valorHash(dto.getDataReferencia()),
                valorHash(temporal == null || temporal.pessoa() == null ? null : temporal.pessoa().getId()),
                valorHash(temporal == null || temporal.vinculo() == null ? null : temporal.vinculo().getId()),
                valorHash(temporal == null || temporal.alocacao() == null ? null : temporal.alocacao().getId()),
                valorHash(dto.getStatus() == null ? null : dto.getStatus().trim().toUpperCase(Locale.ROOT)),
                valorHash(normalizarTexto(dto.getEntrada1())), valorHash(normalizarTexto(dto.getSaida1())),
                valorHash(normalizarTexto(dto.getEntrada2())), valorHash(normalizarTexto(dto.getSaida2())),
                valorHash(normalizarTexto(dto.getHorasTrabalhadas())), valorHash(normalizarTexto(dto.getHorarioSaida())),
                valorHash(dto.getHorasNoturnas()), valorHash(normalizarTexto(dto.getObservacao())),
                valorHash(normalizarTexto(dto.getJustificativaRetificacao())));
        return UtilitarioHashSeguranca.sha256Hexadecimal(material);
    }

    private static String valorHash(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static String normalizarTexto(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim().replaceAll("\\s+", " ");
        return normalizado.isBlank() ? null : normalizado;
    }

    private void exigirRetificacaoGerencial(FrequenciaDTO dto) {
        if (dto.getJustificativaRetificacao() == null || dto.getJustificativaRetificacao().trim().length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Retificação pelo Gestor Local exige justificativa com ao menos 5 caracteres.");
        }
        if (dto.getChaveIdempotencia() == null || dto.getChaveIdempotencia().trim().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Retificação pelo Gestor Local exige chave de idempotência com ao menos 8 caracteres.");
        }
    }

    private void fecharNegocialmenteSeNecessario(Frequencia frequencia) {
        if (frequencia.getEstadoNegocial() == EstadoNegocialFrequencia.ABERTA
                && politicaTemporal.fechada(frequencia.getDataReferencia(), null)) {
            String antes = auditoriaFrequencia.snapshot(frequencia);
            frequencia.setEstadoNegocial(EstadoNegocialFrequencia.FECHADA);
            frequencia.setVersaoNegocial(frequencia.getVersaoNegocial() == null ? 2 : frequencia.getVersaoNegocial() + 1);
            repositorio.saveAndFlush(frequencia);
            auditoriaFrequencia.registrar(frequencia, TipoEventoFrequencia.FECHAMENTO_AUTOMATICO, antes,
                    "Fechamento negocial automático no início do 8º dia em America/Sao_Paulo", null, null, null);
        }
    }

    private void exigirMesmoContextoHistorico(Frequencia frequencia,
                                                ResolvedorContextoTemporalFrequenciaServico.Contexto temporal) {
        if (!Objects.equals(id(frequencia.getVinculoContratual()), temporal.vinculo().getId())
                || !Objects.equals(id(frequencia.getAlocacaoVinculo()), temporal.alocacao().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O contexto histórico foi alterado retroativamente; a correção deve ser feita pelo Gestor Local.");
        }
    }

    private void capturarRetratoCanonico(Frequencia freq,
                                          ResolvedorContextoTemporalFrequenciaServico.Contexto temporal,
                                          LocalDate dataRef,
                                          boolean substituir) {
        if (freq == null || temporal == null || dataRef == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto canônico da frequência inválido.");
        }
        if (!substituir && freq.getVinculoContratual() != null && freq.getAlocacaoVinculo() != null) return;

        VinculoContratual vinculo = temporal.vinculo();
        AlocacaoVinculo alocacao = temporal.alocacao();
        Contrato contrato = vinculo.getContrato();
        Jornada jornada = alocacao.getJornada();
        Lotacao lotacao = alocacao.getLotacao();
        Cargo cargo = alocacao.getCargo();
        if (jornada == null || jornada.getDias() == null || jornada.getDias().size() != 7) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O turno da Alocação histórica não possui os sete dias configurados.");
        }
        JornadaDiaSemana dia = jornada.getDias().stream()
                .filter(d -> d.getDiaSemana() == dataRef.getDayOfWeek())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "O turno da Alocação histórica não possui configuração para " + dataRef.getDayOfWeek() + "."));

        freq.setPessoa(temporal.pessoa());
        freq.setVinculoContratual(vinculo);
        freq.setAlocacaoVinculo(alocacao);
        freq.setJornadaSnapshotId(jornada.getId());
        freq.setJornadaSnapshotDescricao(temporal.posicaoAlocacao().jornadaDescricao() != null
                ? temporal.posicaoAlocacao().jornadaDescricao() : jornada.getDescricao());
        freq.setJornadaSnapshotDiaTrabalho(dia.getDiaTrabalho());
        freq.setJornadaSnapshotEntrada1(dia.getEntrada1());
        freq.setJornadaSnapshotSaida1(dia.getSaida1());
        freq.setJornadaSnapshotEntrada2(dia.getEntrada2());
        freq.setJornadaSnapshotSaida2(dia.getSaida2());
        freq.setLotacaoSnapshotId(lotacao.getId());
        freq.setLotacaoSnapshotNome(temporal.posicaoAlocacao().lotacaoNome() != null
                ? temporal.posicaoAlocacao().lotacaoNome() : lotacao.getNome());
        freq.setLotacaoSnapshotMunicipio(lotacao.getMunicipio());
        freq.setCargoSnapshotId(cargo.getId());
        freq.setCargoSnapshotNome(temporal.posicaoAlocacao().cargoNome() != null
                ? temporal.posicaoAlocacao().cargoNome() : cargo.getNome());
        freq.setContratoSnapshotId(contrato.getId());
        String numeroContratoHistorico = temporal.posicaoContrato().versaoContratual() == null
                ? null : temporal.posicaoContrato().versaoContratual().numeroContrato();
        freq.setContratoSnapshotNumero(numeroContratoHistorico);
        freq.setEmpregadoSnapshotNome(temporal.pessoa().getNome());
        GestaoLocal gestaoHistorica = temporal.gestaoHistorica();
        freq.setGestaoLocalSnapshotId(gestaoHistorica == null ? null : gestaoHistorica.getId());
        freq.setSuperintendenciaSnapshotId(gestaoHistorica == null || gestaoHistorica.getSuperintendencia() == null
                ? null : gestaoHistorica.getSuperintendencia().getId());
    }

    private static Long id(VinculoContratual v) { return v == null ? null : v.getId(); }
    private static Long id(AlocacaoVinculo a) { return a == null ? null : a.getId(); }

    private String normalizarEValidarStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status obrigatório.");
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        if (!ConstantesDominio.FREQUENCIA_STATUS_PERMITIDOS.contains(normalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status de frequência inválido: " + normalizado + ".");
        }
        return normalizado;
    }

    private boolean ehFolgaPelaJornada(Frequencia freq, Empregado empregado, LocalDate dataRef) {
        if (!freq.possuiSnapshotJornada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Frequência sem snapshot de Turno canônico; revalide o Vínculo/Alocação antes da operação.");
        }
        return Boolean.FALSE.equals(freq.getJornadaSnapshotDiaTrabalho());
    }

    private void validaDiasDescanso(Frequencia freq,
                                     Empregado empregado,
                                     LocalDate dataRef,
                                     String status,
                                     Map<LocalDate, List<CalendarioEvento>> calendarioCache,
                                     ContextoLote contexto) {
        boolean feriado = temEventoCalendarioAplicavel(freq, empregado, dataRef, ConstantesDominio.EVENTO_FERIADO, calendarioCache);
        boolean descansoCalendario = temEventoCalendarioAplicavel(freq, empregado, dataRef, ConstantesDominio.EVENTO_DIA_DESCANSO, calendarioCache);
        boolean folgaRemunerada = existeExcecaoAtiva(empregado.getId(), dataRef,
                ConstantesDominio.EXCECAO_FOLGA_REMUNERADA, contexto);
        boolean folgaJornada = ehFolgaPelaJornada(freq, empregado, dataRef);
        boolean especial = feriado || descansoCalendario || folgaRemunerada || folgaJornada;

        if (especial && (ConstantesDominio.FREQ_PRESENCA.equals(status) || ConstantesDominio.FREQ_FALTA.equals(status))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dia de descanso/feriado/escala: use DESCANSO, DSR_PERDIDO, FOLGA, RT ou TR.");
        }
        if (!especial && Set.of(ConstantesDominio.FREQ_TR, ConstantesDominio.FREQ_FOLGA,
                ConstantesDominio.FREQ_DESCANSO, ConstantesDominio.FREQ_DSR_PERDIDO).contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status de DSR/FOLGA/TR só é permitido em dia especial configurado.");
        }
    }

    private boolean temEventoCalendarioAplicavel(Frequencia freq,
                                                   Empregado empregado,
                                                   LocalDate data,
                                                   String tipo,
                                                   Map<LocalDate, List<CalendarioEvento>> calendarioCache) {
        Long gestaoLocalId = freq.getGestaoLocalSnapshotId();
        Long seId = freq.getSuperintendenciaSnapshotId();
        Long lotacaoId = freq.getLotacaoSnapshotId();
        String municipio = freq.getLotacaoSnapshotMunicipio();

        List<CalendarioEvento> eventos = calendarioCache.computeIfAbsent(data, calendarioEventoRepositorio::findByData);
        return eventos.stream().anyMatch(evento -> {
            if (evento == null || Boolean.FALSE.equals(evento.getAtivo()) || evento.getTipo() == null
                    || !tipo.equalsIgnoreCase(evento.getTipo())) return false;
            if (evento.getGestaoLocal() == null && evento.getSuperintendencia() == null) return true;
            if (evento.getGestaoLocal() != null && !Objects.equals(gestaoLocalId, evento.getGestaoLocal().getId())) return false;
            if (evento.getSuperintendencia() != null && !Objects.equals(seId, evento.getSuperintendencia().getId())) return false;
            if (evento.getLotacaoId() != null && !Objects.equals(lotacaoId, evento.getLotacaoId())) return false;
            return evento.getMunicipio() == null || evento.getMunicipio().isBlank()
                    || (municipio != null && evento.getMunicipio().equalsIgnoreCase(municipio));
        });
    }

    private void reconciliarRt(Frequencia freq,
                               Empregado empregado,
                               Map<LocalDate, List<CalendarioEvento>> calendarioCache,
                               ContextoLote contexto) {
        LocalDate data = freq.getDataReferencia();
        boolean feriado = temEventoCalendarioAplicavel(freq, empregado, data, ConstantesDominio.EVENTO_FERIADO, calendarioCache);
        boolean descanso = temEventoCalendarioAplicavel(freq, empregado, data, ConstantesDominio.EVENTO_DIA_DESCANSO, calendarioCache);
        boolean folgaRemunerada = existeExcecaoAtiva(empregado.getId(), data,
                ConstantesDominio.EXCECAO_FOLGA_REMUNERADA, contexto);
        boolean folgaJornada = ehFolgaPelaJornada(freq, empregado, data);
        boolean trabalhou = Set.of(ConstantesDominio.FREQ_PRESENCA, ConstantesDominio.FREQ_PRESENCA_PARCIAL,
                ConstantesDominio.FREQ_ATRASO, ConstantesDominio.FREQ_TR, ConstantesDominio.FREQ_RT)
                .contains(freq.getStatus());
        ChaveFrequencia chave = new ChaveFrequencia(empregado.getId(), data);
        Optional<FrequenciaAdicional> rtOpt = contexto != null
                ? Optional.ofNullable(contexto.adicionaisRt().get(chave))
                : frequenciaAdicionalRepositorio.findByEmpregadoIdAndDataReferenciaAndTipoIgnoreCase(
                        empregado.getId(), data, ConstantesDominio.FREQ_RT);

        if ((feriado || descanso || folgaRemunerada || folgaJornada) && trabalhou) {
            FrequenciaAdicional rt = rtOpt.orElseGet(FrequenciaAdicional::new);
            rt.setEmpregado(empregado);
            rt.setDataReferencia(data);
            rt.setTipo(ConstantesDominio.FREQ_RT);
            rt.setStatus(ConstantesDominio.STATUS_CALCULADO);
            rt.setFrequencia(freq);
            rt.setMinutos(calcularMinutosTrabalhados(freq));
            rt.setJustificativa("RT - " + (folgaRemunerada ? "Folga Remunerada" :
                    feriado ? "Feriado" : descanso ? "Dia de Descanso" : "Folga de Escala"));
            FrequenciaAdicional salvo = frequenciaAdicionalRepositorio.save(rt);
            if (contexto != null) contexto.adicionaisRt().put(chave, salvo);
        } else {
            rtOpt.ifPresent(rt -> {
                if (ConstantesDominio.STATUS_CALCULADO.equals(rt.getStatus())) {
                    frequenciaAdicionalRepositorio.delete(rt);
                    if (contexto != null) contexto.adicionaisRt().remove(chave);
                }
            });
        }
    }

    private boolean existeExcecaoAtiva(Long empregadoId,
                                        LocalDate data,
                                        String tipo,
                                        ContextoLote contexto) {
        if (contexto == null) {
            return frequenciaExcecaoRepositorio.existsAtivaTipoByEmpregadoAndData(empregadoId, data, tipo);
        }
        return contexto.excecoes().getOrDefault(empregadoId, List.of()).stream()
                .anyMatch(e -> !data.isBefore(e.getDataInicio()) && !data.isAfter(e.getDataFim())
                        && tipo.equalsIgnoreCase(e.getTipo()));
    }

    private Integer calcularMinutosTrabalhados(Frequencia freq) {
        if (freq.getHorasTrabalhadas() != null && !freq.getHorasTrabalhadas().isBlank()) {
            return UtilitarioHorario.duracaoParaMinutos(freq.getHorasTrabalhadas(), "horas trabalhadas");
        }
        if (Boolean.TRUE.equals(freq.getJornadaSnapshotDiaTrabalho())) {
            long total = UtilitarioHorario.calcularIntervaloMinutos(freq.getJornadaSnapshotEntrada1(), freq.getJornadaSnapshotSaida1(),
                    "entrada da jornada", "saída da jornada")
                    + UtilitarioHorario.calcularIntervaloMinutos(freq.getJornadaSnapshotEntrada2(), freq.getJornadaSnapshotSaida2(),
                    "entrada da jornada", "saída da jornada");
            return total > 0 ? (int) total : null;
        }
        return null;
    }

    private void registrarHistorico(Frequencia saved,
                                     Empregado empregado,
                                     UsuarioAplicacao usuarioAtual,
                                     boolean gestorOperacional,
                                     boolean isUpdate) {
        HistoricoEmpregado hist = new HistoricoEmpregado();
        hist.setEmpregado(empregado);
        hist.setDataEvento(LocalDateTime.now(zoneId));
        String origemGestao = gestorOperacional && isUpdate ? " (GESTÃO LOCAL)" : "";
        hist.setDescricao((isUpdate ? "Atualização" : "Registro") + origemGestao
                + ": " + saved.getStatus() + " em " + saved.getDataReferencia());
        hist.setUsuario(usuarioAtual);
        historicoIntegridadeServico.salvarAssinado(hist);
    }

    private void validarHorarios(FrequenciaDTO dto) {
        UtilitarioHorario.validarPar(dto.getEntrada1(), dto.getSaida1(), "entrada 1", "saída 1");
        UtilitarioHorario.validarPar(dto.getEntrada2(), dto.getSaida2(), "entrada 2", "saída 2");
        UtilitarioHorario.normalizarHorario(dto.getHorarioSaida(), "horário de saída");
        if (dto.getHorasTrabalhadas() != null && !dto.getHorasTrabalhadas().isBlank()) {
            UtilitarioHorario.duracaoParaMinutos(dto.getHorasTrabalhadas(), "horas trabalhadas");
        }
    }

    private Specification<Frequencia> porPeriodo(LocalDate inicio, LocalDate fim) {
        return (root, query, cb) -> cb.between(root.get("dataReferencia"), inicio, fim);
    }

    private Specification<Frequencia> porStatus(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String normalizado = status.trim().toUpperCase(Locale.ROOT);
        if (!ConstantesDominio.FREQUENCIA_STATUS_PERMITIDOS.contains(normalizado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de frequência inválido.");
        }
        return (root, query, cb) -> cb.equal(root.get("status"), normalizado);
    }

    private Specification<Frequencia> porFiltrosOrganizacionais(Long lotacaoId,
                                                                 Long jornadaId,
                                                                 Long contratoId,
                                                                 Long gestaoLocalId,
                                                                 Long superintendenciaId) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> filtros = new ArrayList<>();
            if (lotacaoId != null) filtros.add(cb.equal(root.get("lotacaoSnapshotId"), lotacaoId));
            if (jornadaId != null) filtros.add(cb.equal(root.get("jornadaSnapshotId"), jornadaId));
            if (contratoId != null) filtros.add(cb.equal(root.get("contratoSnapshotId"), contratoId));
            if (gestaoLocalId != null) filtros.add(cb.equal(root.get("gestaoLocalSnapshotId"), gestaoLocalId));
            if (superintendenciaId != null) filtros.add(cb.equal(root.get("superintendenciaSnapshotId"), superintendenciaId));
            return filtros.isEmpty() ? cb.conjunction() : cb.and(filtros.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Specification<Frequencia> porEscopo(UsuarioAplicacao user) {
        if (acessoOrganizacionalServico.ehGlobal(user)) return (root, query, cb) -> cb.conjunction();
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            Long id = acessoOrganizacionalServico.superintendenciaEfetiva(user).getId();
            return (root, query, cb) -> cb.equal(root.get("superintendenciaSnapshotId"), id);
        }
        if (acessoOrganizacionalServico.ehGestaoLocal(user) && user.getGestaoLocal() != null) {
            Long id = user.getGestaoLocal().getId();
            return (root, query, cb) -> cb.equal(root.get("gestaoLocalSnapshotId"), id);
        }

        return (root, query, cb) -> {
            List<EscopoAcessoUsuarioServico.Escopo> scopes = userAccessScopeServico.obterEscopos(user);
            if (scopes == null || scopes.isEmpty()) return cb.disjunction();
            List<jakarta.persistence.criteria.Predicate> permitidos = new ArrayList<>();
            for (EscopoAcessoUsuarioServico.Escopo scope : scopes) {
                if (scope.gestaoLocalId() == null) return cb.conjunction();
                jakarta.persistence.criteria.Predicate predicado =
                        cb.equal(root.get("gestaoLocalSnapshotId"), scope.gestaoLocalId());
                if (scope.lotacaoId() != null) {
                    predicado = cb.and(predicado, cb.equal(root.get("lotacaoSnapshotId"), scope.lotacaoId()));
                }
                if (scope.jornadaId() != null) {
                    predicado = cb.and(predicado, cb.equal(root.get("jornadaSnapshotId"), scope.jornadaId()));
                }
                permitidos.add(predicado);
            }
            return cb.or(permitidos.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void exigirFiltroOrganizacionalParaConsultaGlobal(UsuarioAplicacao user,
                                                                Long lotacaoId,
                                                                Long jornadaId,
                                                                Long contratoId,
                                                                Long gestaoLocalId,
                                                                Long superintendenciaId) {
        if (acessoOrganizacionalServico.ehGlobal(user) && lotacaoId == null && jornadaId == null && contratoId == null
                && gestaoLocalId == null && superintendenciaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos um filtro organizacional: lotação, turno, contrato, Gestão Local ou superintendência.");
        }
    }

    private UsuarioAplicacao usuarioAtual() {
        return usuarioAtual(SecurityContextHolder.getContext().getAuthentication());
    }

    private UsuarioAplicacao usuarioAtual(Authentication auth) {
        String username = auth != null ? auth.getName() : "";
        return appUserRepositorio.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado."));
    }

    public record ResultadoBlocoFechamento(long inseridos, long atualizados, long preservados) {}

    private record ChaveFrequencia(Long empregadoId, LocalDate data) {}

    private record ContextoLote(
            Map<Long, Empregado> empregados,
            Map<ChaveFrequencia, Frequencia> frequencias,
            Map<LocalDate, List<CalendarioEvento>> calendario,
            Map<Long, List<FrequenciaExcecao>> excecoes,
            Map<ChaveFrequencia, FrequenciaAdicional> adicionaisRt
    ) {}

    private FrequenciaDTO paraDTO(Frequencia entity) {
        FrequenciaDTO dto = new FrequenciaDTO();
        dto.setId(entity.getId());
        dto.setDataReferencia(entity.getDataReferencia());
        dto.setMes(entity.getMes());
        dto.setAno(entity.getAno());
        dto.setStatus(entity.getStatus());
        dto.setEntrada1(entity.getEntrada1());
        dto.setSaida1(entity.getSaida1());
        dto.setEntrada2(entity.getEntrada2());
        dto.setSaida2(entity.getSaida2());
        dto.setHorasTrabalhadas(entity.getHorasTrabalhadas());
        dto.setHorarioSaida(entity.getHorarioSaida());
        dto.setHorasNoturnas(entity.getHorasNoturnas());
        dto.setObservacao(entity.getObservacao());
        dto.setPrazoEdicao(politicaTemporal.prazoLocal(entity.getDataReferencia()));
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setPessoaId(entity.getPessoa() == null ? null : entity.getPessoa().getId());
        dto.setVinculoContratualId(id(entity.getVinculoContratual()));
        dto.setAlocacaoVinculoId(id(entity.getAlocacaoVinculo()));
        dto.setContratoId(entity.getContratoSnapshotId());
        dto.setLotacaoId(entity.getLotacaoSnapshotId());
        dto.setJornadaId(entity.getJornadaSnapshotId());
        dto.setCargoId(entity.getCargoSnapshotId());
        dto.setGestaoLocalId(entity.getGestaoLocalSnapshotId());
        dto.setEstadoNegocial(politicaTemporal.fechada(entity.getDataReferencia(), null)
                ? EstadoNegocialFrequencia.FECHADA : entity.getEstadoNegocial());
        dto.setValidadeOperacional(entity.getValidadeOperacional());
        dto.setEstadoAuditoria(entity.getEstadoAuditoria());
        dto.setBloqueadaEdicaoOperacional(entity.getBloqueadaEdicaoOperacional());
        dto.setVersaoNegocial(entity.getVersaoNegocial());
        if (entity.getEmpregado() != null) {
            dto.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(
                    entity.getEmpregado().getId(), entity.getEmpregado().getNome()));
        }
        return dto;
    }
}
