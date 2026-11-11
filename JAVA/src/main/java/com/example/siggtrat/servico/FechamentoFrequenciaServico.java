package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.RequisicaoCriacaoFechamentoFrequencia;
import com.example.siggtrat.dto.FechamentoFrequenciaDTO;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.evento.EventoFechamentoFrequenciaSolicitado;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FechamentoFrequenciaServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(FechamentoFrequenciaServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final FechamentoFrequenciaRepositorio repositorio;
    private final FechamentoFrequenciaItemRepositorio itemRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;
    private final ApplicationEventPublisher eventPublisher;
    private final PropriedadesFechamentoFrequencia properties;
    private final PoliticaTemporalFrequenciaServico politicaTemporal;
    private final ElegibilidadeFechamentoFrequenciaServico elegibilidade;
    private final GestaoContratoTemporalServico gestaoContratoTemporal;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public FechamentoFrequenciaServico(FechamentoFrequenciaRepositorio repositorio,
                                       FechamentoFrequenciaItemRepositorio itemRepositorio,
                                       EmpregadoRepositorio empregadoRepositorio,
                                       LotacaoRepositorio lotacaoRepositorio,
                                       JornadaRepositorio jornadaRepositorio,
                                       ContratoRepositorio contratoRepositorio,
                                       AcessoOrganizacionalServico acessoOrganizacionalServico,
                                       ApplicationEventPublisher eventPublisher,
                                       PropriedadesFechamentoFrequencia properties,
                                       PoliticaTemporalFrequenciaServico politicaTemporal,
                                       ElegibilidadeFechamentoFrequenciaServico elegibilidade,
                                       GestaoContratoTemporalServico gestaoContratoTemporal) {
        this.repositorio = repositorio;
        this.itemRepositorio = itemRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.politicaTemporal = politicaTemporal;
        this.elegibilidade = elegibilidade;
        this.gestaoContratoTemporal = gestaoContratoTemporal;
    }

    @Transactional
    public FechamentoFrequenciaDTO criarOuReiniciar(RequisicaoCriacaoFechamentoFrequencia request) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirFrequenciaDiaria(user);
        validarData(user, request.dataReferencia());

        Lotacao lotacao = lotacaoRepositorio.findById(request.lotacaoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Lotação não encontrada."));
        Jornada jornada = jornadaRepositorio.findById(request.jornadaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Turno não encontrado."));
        validarEscopo(user, lotacao, jornada);

        List<Long> contratoIds = normalizarIds(request.contratoIds());
        validarContratos(user, lotacao, contratoIds, request.dataReferencia());

        String chave = montarChaveEscopo(request.dataReferencia(), lotacao.getId(), jornada.getId(), contratoIds);
        FechamentoFrequencia fechamento = repositorio.buscarPorChaveEscopoParaAtualizacao(chave).orElse(null);

        // Uma repetição do clique enquanto o trabalho está ativo é idempotente e
        // retorna o mesmo fechamento sem repetir contagens sobre um quadro massivo.
        if (fechamento != null && EnumSet.of(
                FechamentoFrequenciaStatus.PENDENTE,
                FechamentoFrequenciaStatus.PROCESSANDO
        ).contains(fechamento.getStatus())) {
            return paraDTO(fechamento);
        }

        if (request.alteracoes().size() > properties.maxAlteracoes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "O fechamento excede o limite de " + properties.maxAlteracoes() + " alterações explícitas.");
        }

        Map<Long, FrequenciaDTO> alteracoes = normalizarAlteracoes(request);
        boolean somenteAlteracoes = false;
        if (fechamento != null && fechamento.getStatus() == FechamentoFrequenciaStatus.CONCLUIDO) {
            long faltantes = contarElegiveisSemFrequencia(
                    request.dataReferencia(), lotacao.getId(), jornada.getId(), contratoIds);
            if (faltantes == 0 && alteracoes.isEmpty()) {
                return paraDTO(fechamento);
            }
            somenteAlteracoes = faltantes == 0 && !alteracoes.isEmpty();
        }

        // A contagem completa de 100 mil ou mais colaboradores é feita pelo worker,
        // depois do HTTP 202. Aqui usamos apenas uma consulta limitada a um registro
        // para rejeitar rapidamente um escopo realmente vazio.
        if (!somenteAlteracoes && !existeElegivel(
                request.dataReferencia(), lotacao.getId(), jornada.getId(), contratoIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhum colaborador elegível foi encontrado para a lotação, turno, contratos e data informados.");
        }
        validarAlteracoesElegiveis(alteracoes.keySet(), request.dataReferencia(), lotacao.getId(), jornada.getId(), contratoIds);
        validarIntencaoGerencial(user, request, alteracoes, contratoIds);

        if (fechamento == null) {
            fechamento = new FechamentoFrequencia();
            fechamento.setChaveEscopo(chave);
            fechamento.setDataReferencia(request.dataReferencia());
            fechamento.setLotacao(lotacao);
            fechamento.setJornada(jornada);
            fechamento.setUsuarioCriador(user);
        } else {
            itemRepositorio.excluirPorIdFechamento(fechamento.getId());
            fechamento.setTentativa(fechamento.getTentativa() + 1);
        }

        fechamento.setContratoIds(serializarIds(contratoIds));
        fechamento.setJustificativaRetificacao(request.justificativaRetificacao());
        fechamento.setChaveIdempotencia(request.chaveIdempotencia());
        fechamento.setStatus(FechamentoFrequenciaStatus.PENDENTE);
        fechamento.setSomenteAlteracoes(somenteAlteracoes);
        // Em modo completo, o total é apurado no processo assíncrono para que a
        // requisição de criação não espere uma contagem nacional/organizacional.
        fechamento.setTotalPrevisto(somenteAlteracoes ? alteracoes.size() : 0);
        fechamento.setTotalProcessado(0);
        fechamento.setTotalInserido(0);
        fechamento.setTotalAtualizado(0);
        fechamento.setTotalPreservado(0);
        fechamento.setTotalAlteracoes(alteracoes.size());
        fechamento.setUltimoEmpregadoIdProcessado(null);
        fechamento.setCancelamentoSolicitado(false);
        fechamento.setMensagem(somenteAlteracoes
                ? "Alterações enfileiradas para processamento assíncrono."
                : "Fechamento enfileirado para processamento assíncrono.");
        fechamento.setDetalheErro(null);
        fechamento.setIniciadoEm(null);
        fechamento.setHeartbeatEm(null);
        fechamento.setConcluidoEm(null);
        fechamento.setUsuarioCriador(user);
        fechamento = repositorio.saveAndFlush(fechamento);

        List<FechamentoFrequenciaItem> itens = new ArrayList<>(alteracoes.size());
        for (Map.Entry<Long, FrequenciaDTO> entry : alteracoes.entrySet()) {
            FrequenciaDTO dto = entry.getValue();
            FechamentoFrequenciaItem item = new FechamentoFrequenciaItem();
            item.setFechamento(fechamento);
            item.setEmpregado(empregadoRepositorio.getReferenceById(entry.getKey()));
            item.setStatus(dto.getStatus());
            item.setObservacao(dto.getObservacao());
            item.setJustificativaRetificacao(dto.getJustificativaRetificacao());
            item.setChaveIdempotencia(dto.getChaveIdempotencia());
            item.setEntrada1(dto.getEntrada1());
            item.setSaida1(dto.getSaida1());
            item.setEntrada2(dto.getEntrada2());
            item.setSaida2(dto.getSaida2());
            item.setHorasTrabalhadas(dto.getHorasTrabalhadas());
            item.setHorarioSaida(dto.getHorarioSaida());
            item.setHorasNoturnas(dto.getHorasNoturnas());
            itens.add(item);
        }
        if (!itens.isEmpty()) itemRepositorio.saveAll(itens);

        eventPublisher.publishEvent(new EventoFechamentoFrequenciaSolicitado(fechamento.getId()));
        return paraDTO(fechamento);
    }

    @Transactional(readOnly = true)
    public FechamentoFrequenciaDTO buscar(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirConsultaFechamento(user);
        FechamentoFrequencia fechamento = repositorio.buscarDetalhadoPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fechamento de frequência não encontrado."));
        validarConsultaDoFechamento(user, fechamento);
        return paraDTO(fechamento);
    }

    @Transactional(readOnly = true)
    public Optional<FechamentoFrequenciaDTO> buscarAtual(LocalDate data,
                                                         Long lotacaoId,
                                                         Long jornadaId,
                                                         List<Long> contratoIds) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        exigirConsultaFechamento(user);
        String chave = montarChaveEscopo(data, lotacaoId, jornadaId, normalizarIds(contratoIds));
        return repositorio.buscarDetalhadoPorChaveEscopo(chave).map(fechamento -> {
            validarConsultaDoFechamento(user, fechamento);
            return paraDTO(fechamento);
        });
    }

    @Transactional
    public FechamentoFrequenciaDTO solicitarCancelamento(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirFrequenciaDiaria(user);
        FechamentoFrequencia fechamento = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fechamento de frequência não encontrado."));
        validarAcessoAoFechamento(user, fechamento);
        if (!EnumSet.of(FechamentoFrequenciaStatus.PENDENTE, FechamentoFrequenciaStatus.PROCESSANDO)
                .contains(fechamento.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente um fechamento pendente ou em processamento pode ser cancelado.");
        }
        fechamento.setCancelamentoSolicitado(true);
        fechamento.setMensagem("Cancelamento solicitado. O processamento será interrompido ao final do bloco atual.");
        return paraDTO(repositorio.save(fechamento));
    }

    @Transactional
    public FechamentoFrequenciaDTO reprocessar(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirFrequenciaDiaria(user);
        FechamentoFrequencia fechamento = repositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Fechamento de frequência não encontrado."));
        validarAcessoAoFechamento(user, fechamento);
        if (!EnumSet.of(FechamentoFrequenciaStatus.ERRO, FechamentoFrequenciaStatus.CANCELADO)
                .contains(fechamento.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente fechamentos com erro ou cancelados podem ser reprocessados.");
        }
        validarData(user, fechamento.getDataReferencia());
        List<Long> contratoIds = desserializarIds(fechamento.getContratoIds());
        long totalPrevisto;
        if (fechamento.isSomenteAlteracoes()) {
            totalPrevisto = itemRepositorio.countByFechamentoId(fechamento.getId());
            if (totalPrevisto == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O fechamento não possui alterações para reprocessamento.");
            }
        } else {
            if (!existeElegivel(
                    fechamento.getDataReferencia(),
                    fechamento.getLotacao().getId(),
                    fechamento.getJornada().getId(),
                    contratoIds)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O fechamento não possui mais colaboradores elegíveis para reprocessamento.");
            }
            totalPrevisto = 0;
        }

        fechamento.setStatus(FechamentoFrequenciaStatus.PENDENTE);
        fechamento.setTotalPrevisto(totalPrevisto);
        fechamento.setTentativa(fechamento.getTentativa() + 1);
        fechamento.setTotalProcessado(0);
        fechamento.setTotalInserido(0);
        fechamento.setTotalAtualizado(0);
        fechamento.setTotalPreservado(0);
        fechamento.setUltimoEmpregadoIdProcessado(null);
        fechamento.setCancelamentoSolicitado(false);
        fechamento.setMensagem("Fechamento reenfileirado.");
        fechamento.setDetalheErro(null);
        fechamento.setIniciadoEm(null);
        fechamento.setHeartbeatEm(null);
        fechamento.setConcluidoEm(null);
        repositorio.save(fechamento);
        eventPublisher.publishEvent(new EventoFechamentoFrequenciaSolicitado(fechamento.getId()));
        return paraDTO(fechamento);
    }

    private Map<Long, FrequenciaDTO> normalizarAlteracoes(RequisicaoCriacaoFechamentoFrequencia request) {
        Map<Long, FrequenciaDTO> mapa = new LinkedHashMap<>();
        for (FrequenciaDTO dto : request.alteracoes()) {
            if (dto == null || dto.getEmpregado() == null || dto.getEmpregado().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Toda alteração deve informar o ID do colaborador.");
            }
            Long empregadoId = dto.getEmpregado().getId();
            if (mapa.putIfAbsent(empregadoId, dto) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O fechamento contém mais de uma alteração para o mesmo colaborador.");
            }
            dto.setDataReferencia(request.dataReferencia());
            String status = dto.getStatus() == null ? "" : dto.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!ConstantesDominio.FREQUENCIA_STATUS_PERMITIDOS.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Status de frequência inválido para o colaborador " + empregadoId + ".");
            }
            dto.setStatus(status);
            if ("ATESTADO".equals(status) && (dto.getObservacao() == null || dto.getObservacao().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Atestado exige justificativa para o colaborador " + empregadoId + ".");
            }
        }
        return mapa;
    }

    private void validarAlteracoesElegiveis(Collection<Long> ids,
                                             LocalDate data,
                                             Long lotacaoId,
                                             Long jornadaId,
                                             List<Long> contratoIds) {
        if (ids.isEmpty()) return;
        List<Long> lista = new ArrayList<>(ids);
        long elegiveis = 0;
        for (int inicio = 0; inicio < lista.size(); inicio += 800) {
            List<Long> bloco = lista.subList(inicio, Math.min(lista.size(), inicio + 800));
            elegiveis += elegibilidade.contarExplicitos(bloco, data, lotacaoId, jornadaId, contratoIds);
        }
        if (elegiveis != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Uma ou mais alterações pertencem a colaboradores fora do escopo ou sem contrato vigente na data.");
        }
    }

    private boolean existeElegivel(LocalDate data,
                                   Long lotacaoId,
                                   Long jornadaId,
                                   List<Long> contratoIds) {
        return elegibilidade.existe(data, lotacaoId, jornadaId, contratoIds);
    }

    /** Executado pelo worker, fora do caminho crítico da resposta HTTP 202. */
    long contarElegiveisParaProcessamento(Long fechamentoId,
                                          LocalDate data,
                                          Long lotacaoId,
                                          Long jornadaId,
                                          List<Long> contratoIds) {
        return elegibilidade.contarPendentes(fechamentoId, data, lotacaoId, jornadaId, contratoIds);
    }

    private long contarElegiveisSemFrequencia(LocalDate data,
                                                Long lotacaoId,
                                                Long jornadaId,
                                                List<Long> contratoIds) {
        return elegibilidade.contarSemFrequencia(data, lotacaoId, jornadaId, contratoIds);
    }

    private void validarIntencaoGerencial(UsuarioAplicacao usuario,
                                           RequisicaoCriacaoFechamentoFrequencia request,
                                           Map<Long, FrequenciaDTO> alteracoes,
                                           List<Long> contratoIds) {
        if (!acessoOrganizacionalServico.ehGestaoLocal(usuario)) return;
        boolean coletiva = textoMinimo(request.justificativaRetificacao(), 5)
                && textoMinimo(request.chaveIdempotencia(), 8);
        for (var item : alteracoes.entrySet()) {
            FrequenciaDTO dto = item.getValue();
            if (coletiva && !textoMinimo(dto.getJustificativaRetificacao(), 5)) {
                dto.setJustificativaRetificacao(request.justificativaRetificacao());
            }
            if (coletiva && !textoMinimo(dto.getChaveIdempotencia(), 8)) {
                dto.setChaveIdempotencia("fechamento-item:" + com.example.siggtrat.utilitario.UtilitarioHashSeguranca
                        .sha256Hexadecimal(request.chaveIdempotencia() + "|" + item.getKey() + "|" + request.dataReferencia()));
            }
            if (!textoMinimo(dto.getJustificativaRetificacao(), 5) || !textoMinimo(dto.getChaveIdempotencia(), 8)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Alteração do Gestor Local exige justificativa e chave de idempotência por item ou no comando coletivo.");
            }
        }
        if (!coletiva && elegibilidade.existeImplicitoSemIntencao(request.dataReferencia(),
                request.lotacaoId(), request.jornadaId(), contratoIds, alteracoes.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O fechamento gerencial contém lançamentos implícitos: informe justificativaRetificacao e chaveIdempotencia no comando.");
        }
    }

    private static boolean textoMinimo(String texto, int tamanho) {
        return texto != null && texto.trim().length() >= tamanho;
    }

    private void validarEscopo(UsuarioAplicacao user, Lotacao lotacao, Jornada jornada) {
        if (!acessoOrganizacionalServico.podeLancarFrequenciaDiaria(user, lotacao, jornada)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Lotação ou turno fora do escopo do usuário.");
        }
        boolean vinculado = jornadaRepositorio.findByLotacoes_Id(lotacao.getId()).stream()
                .anyMatch(j -> Objects.equals(j.getId(), jornada.getId()));
        if (!vinculado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O turno selecionado não está vinculado à lotação.");
        }
    }

    private void validarContratos(UsuarioAplicacao user, Lotacao lotacao, List<Long> ids, LocalDate dataReferencia) {
        if (ids.isEmpty()) return;
        List<Contrato> contratos = contratoRepositorio.findAllById(ids);
        if (contratos.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais contratos não existem.");
        }
        for (Contrato contrato : contratos) {
            // O contrato pode estar hoje em outra Gestão. O comando pertence ao dia
            // da frequência, com autorização atual sobre a responsabilidade daquele dia.
            GestaoLocal gestaoHistorica = gestaoContratoTemporal.gestaoEm(contrato, dataReferencia, Instant.now());
            if (gestaoHistorica == null
                    || !acessoOrganizacionalServico.podeAcessarGestaoLocal(user, gestaoHistorica)
                    || lotacao.getGestaoLocal() == null
                    || !Objects.equals(gestaoHistorica.getId(), lotacao.getGestaoLocal().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Um ou mais contratos estão fora do escopo da lotação.");
            }
        }
    }

    private void validarData(UsuarioAplicacao user, LocalDate data) {
        if (data == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data obrigatória.");
        LocalDate hoje = LocalDate.now(zoneId);
        if (data.isAfter(hoje)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é permitido fechar frequência em data futura.");
        }
        boolean gestor = acessoOrganizacionalServico.ehGestaoLocal(user);
        if (!gestor && !politicaTemporal.abertaParaOperacional(data, null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A frequência está FECHADA; a janela operacional de " + politicaTemporal.diasJanela()
                            + " dias encerrou. O Operacional não pode iniciar fechamento tardio.");
        }
    }

    private void exigirConsultaFechamento(UsuarioAplicacao user) {
        if (!acessoOrganizacionalServico.ehPresidente(user)) {
            acessoOrganizacionalServico.exigirConsultaFrequencia(user);
        }
    }

    private void validarConsultaDoFechamento(UsuarioAplicacao user, FechamentoFrequencia fechamento) {
        if (acessoOrganizacionalServico.ehPresidente(user)) return;
        if (!acessoOrganizacionalServico.podeAcessarLotacao(user, fechamento.getLotacao())
                || !acessoOrganizacionalServico.podeAcessarJornada(user, fechamento.getJornada())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Fechamento de frequência não encontrado no seu escopo.");
        }
    }

    private void validarAcessoAoFechamento(UsuarioAplicacao user, FechamentoFrequencia fechamento) {
        if (!acessoOrganizacionalServico.podeLancarFrequenciaDiaria(
                user,
                fechamento.getLotacao(),
                fechamento.getJornada()
        )) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Fechamento de frequência não encontrado no seu escopo.");
        }
    }

    public static String montarChaveEscopo(LocalDate data,
                                           Long lotacaoId,
                                           Long jornadaId,
                                           List<Long> contratoIds) {
        if (data == null || lotacaoId == null || jornadaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data, lotação e turno são obrigatórios.");
        }
        String escopoNormalizado = data + "|L:" + lotacaoId + "|J:" + jornadaId
                + "|C:" + serializarIds(normalizarIds(contratoIds));
        return sha256(escopoNormalizado);
    }

    private static String sha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível na JVM.", ex);
        }
    }

    public static List<Long> normalizarIds(Collection<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).filter(id -> id > 0)
                .distinct().sorted().toList();
    }

    public static String serializarIds(Collection<Long> ids) {
        return normalizarIds(ids).stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static List<Long> desserializarIds(String valor) {
        if (valor == null || valor.isBlank()) return List.of();
        return Arrays.stream(valor.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(Long::valueOf).distinct().sorted().toList();
    }

    public static List<Long> idsParaConsulta(List<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of(-1L) : ids;
    }

    public static FechamentoFrequenciaDTO paraDTO(FechamentoFrequencia f) {
        int percentual = f.getTotalPrevisto() <= 0 ? 0
                : (int) Math.min(100, Math.round((f.getTotalProcessado() * 100.0) / f.getTotalPrevisto()));
        if (f.getStatus() == FechamentoFrequenciaStatus.CONCLUIDO) percentual = 100;
        return new FechamentoFrequenciaDTO(
                f.getId(),
                f.getStatus().name(),
                f.getDataReferencia(),
                f.getLotacao().getId(),
                f.getLotacao().getNome(),
                f.getJornada().getId(),
                f.getJornada().getDescricao(),
                desserializarIds(f.getContratoIds()),
                f.getTotalPrevisto(),
                f.getTotalProcessado(),
                f.getTotalInserido(),
                f.getTotalAtualizado(),
                f.getTotalPreservado(),
                f.getTotalAlteracoes(),
                f.isSomenteAlteracoes(),
                percentual,
                f.getTentativa(),
                f.isCancelamentoSolicitado(),
                f.getMensagem(),
                f.getDetalheErro(),
                f.getCriadoEm(),
                f.getIniciadoEm(),
                f.getAtualizadoEm(),
                f.getConcluidoEm()
        );
    }
}
