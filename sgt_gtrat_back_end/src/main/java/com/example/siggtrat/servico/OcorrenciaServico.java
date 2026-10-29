package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesArmazenamentoArquivo;
import com.example.siggtrat.configuracao.propriedades.PropriedadesOcorrencia;
import com.example.siggtrat.configuracao.propriedades.PropriedadesEnvioArquivo;
import com.example.siggtrat.dto.BaixaAnexoDTO;
import com.example.siggtrat.dto.CriacaoOcorrenciaDTO;
import com.example.siggtrat.dto.OcorrenciaResumoDTO;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.especificacao.EspecificacaoEscopo;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class OcorrenciaServico {

    private static final Logger log = LoggerFactory.getLogger(OcorrenciaServico.class);

    private final OcorrenciaRepositorio repositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final HistoricoEmpregadoRepositorio historicoRepositorio;
    private final FrequenciaRepositorio frequenciaRepositorio;
    private final FrequenciaServico frequenciaServico;
    private final UsuarioAplicacaoRepositorio appUserRepositorio;
    private final AnexoOcorrenciaRepositorio anexoRepositorio;
    private final RegistroAuditoriaServico auditLogServico;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;
    private final HistoricoIntegridadeServico historicoIntegridadeServico;

    private final long maxFileSizeBytes;
    private final long maxTotalUploadBytes;
    private final int maxFilesPerRequest;
    private final Path uploadRoot;
    private final Path uploadTempRoot;
    private final int maxPeriodDays;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public OcorrenciaServico(
            OcorrenciaRepositorio repositorio,
            EmpregadoRepositorio empregadoRepositorio,
            HistoricoEmpregadoRepositorio historicoRepositorio,
            FrequenciaRepositorio frequenciaRepositorio,
            FrequenciaServico frequenciaServico,
            UsuarioAplicacaoRepositorio appUserRepositorio,
            AnexoOcorrenciaRepositorio anexoRepositorio,
            RegistroAuditoriaServico auditLogServico,
            EscopoAcessoUsuarioServico userAccessScopeServico,
            HistoricoIntegridadeServico historicoIntegridadeServico,
            PropriedadesEnvioArquivo uploadProperties,
            PropriedadesArmazenamentoArquivo fileStorageProperties,
            PropriedadesOcorrencia ocorrenciaProperties) {
        this.repositorio = repositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.frequenciaRepositorio = frequenciaRepositorio;
        this.frequenciaServico = frequenciaServico;
        this.appUserRepositorio = appUserRepositorio;
        this.anexoRepositorio = anexoRepositorio;
        this.auditLogServico = auditLogServico;
        this.userAccessScopeServico = userAccessScopeServico;
        this.historicoIntegridadeServico = historicoIntegridadeServico;
        this.maxFileSizeBytes = uploadProperties.maxFileSizeBytes();
        this.maxTotalUploadBytes = uploadProperties.maxTotalBytes();
        this.maxFilesPerRequest = uploadProperties.maxFilesPerRequest();
        this.uploadRoot = fileStorageProperties.uploadDir().toAbsolutePath().normalize();
        this.uploadTempRoot = this.uploadRoot.resolve(".staging").normalize();
        this.maxPeriodDays = ocorrenciaProperties.maxPeriodDays();
    }

    @PostConstruct
    public void inicializar() {
        try {
            Files.createDirectories(uploadRoot);
            Files.createDirectories(uploadTempRoot);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads!");
        }
    }

    private UsuarioAplicacao obterUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "";
        return appUserRepositorio.findByUsername(username).orElse(null);
    }

    private boolean podeAcessarEmpregado(Empregado empregado) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        return user != null && empregado != null && userAccessScopeServico.podeConsultar(user, empregado);
    }

    private boolean podeGerenciarEmpregado(Empregado empregado) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        return user != null && user.getPerfil() == PerfilAcesso.GESTOR_LOCAL
                && empregado != null && userAccessScopeServico.podeGerenciar(user, empregado);
    }

    private boolean possuiConsultaGlobal(UsuarioAplicacao user) {
        return user != null && (user.getPerfil() == PerfilAcesso.ADMINISTRADOR
                || user.getPerfil() == PerfilAcesso.GESTOR_NACIONAL);
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<OcorrenciaResumoDTO> listarTodos(
            Long empregadoId,
            LocalDate data,
            Long lotacaoId,
            Long jornadaId,
            Long contratoId,
            Pageable pageable
    ) {
        UsuarioAplicacao user = obterUsuarioAutenticado();
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        if (empregadoId == null && data == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o colaborador ou a data para consultar ocorrências.");
        }
        if (possuiConsultaGlobal(user) && empregadoId == null && lotacaoId == null
                && jornadaId == null && contratoId == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe lotação, turno ou contrato para a consulta nacional de ocorrências.");
        }

        Specification<Ocorrencia> spec = (root, query, cb) -> cb.conjunction();
        if (empregadoId != null) {
            Empregado empregado = empregadoRepositorio.findById(empregadoId)
                    .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado"));
            if (!podeAcessarEmpregado(empregado)) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Sem permissão para consultar este colaborador.");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empregado").get("id"), empregadoId));
        }
        if (data != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("dataOcorrencia"), data));
        }
        if (lotacaoId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empregado").get("lotacao").get("id"), lotacaoId));
        }
        if (jornadaId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("empregado").get("jornada").get("id"), jornadaId));
        }
        if (contratoId != null) {
            spec = spec.and((root, query, cb) -> {
                // EXISTS evita multiplicar a entidade raiz pela coleção de contratos.
                // Além de dispensar DISTINCT, preserva a ordenação
                // por empregado.nome usada pela frequência no MySQL.
                var contratoDoEmpregado = query.subquery(Long.class);
                var empregadoContrato = contratoDoEmpregado.from(Empregado.class);
                var contratos = empregadoContrato.join("contratos", jakarta.persistence.criteria.JoinType.INNER);
                contratoDoEmpregado.select(empregadoContrato.get("id"));
                contratoDoEmpregado.where(
                        cb.equal(empregadoContrato.get("id"), root.get("empregado").get("id")),
                        cb.equal(contratos.get("id"), contratoId));
                return cb.exists(contratoDoEmpregado);
            });
        }
        if (!possuiConsultaGlobal(user)) {
            spec = spec.and(EspecificacaoEscopo.filtrarPorEscopo(user, userAccessScopeServico));
        }

        Pageable limitado = UtilitarioPaginacao.limitar(pageable,
                Sort.by("dataOcorrencia").descending().and(Sort.by("id").descending()));
        return repositorio.findAll(spec, limitado).map(this::paraResumoDTO);
    }
    public BaixaAnexoDTO baixarAnexo(Long anexoId) {
        AnexoOcorrencia anexo = anexoRepositorio.findById(anexoId)
                .orElseThrow(() -> new IllegalArgumentException("Anexo não encontrado"));

        Ocorrencia ocorrencia = anexo.getOcorrencia();
        if (ocorrencia == null || ocorrencia.getEmpregado() == null || !podeAcessarEmpregado(ocorrencia.getEmpregado())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para acessar este recurso.");
        }

        try {
            Path filePath = resolverArquivoArmazenado(anexo.getNomeSalvo());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                auditLogServico.registrarAcao("ANEXO_DOWNLOAD", "Ocorrencia", anexo.getOcorrencia().getId(), "Download Anexo",
                        RegistroAuditoriaServico.detalhes("arquivo", anexo.getNomeOriginal(), "empregado", anexo.getOcorrencia().getEmpregado().getNome()));

                String safeType = resolverTipoConteudoSeguro(anexo.getTipoArquivo());
                String disposition = safeType.startsWith("image/") || "application/pdf".equals(safeType) || "text/plain".equals(safeType)
                        ? "inline"
                        : "attachment";

                return new BaixaAnexoDTO(
                        resource, safeType, disposition, higienizarNomeArquivo(anexo.getNomeOriginal()));
            } else {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado.");
            }
        } catch (MalformedURLException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo inválido.", e);
        }
    }
    @Transactional(rollbackOn = Exception.class)
    public OcorrenciaResumoDTO criar(CriacaoOcorrenciaDTO dto, List<MultipartFile> arquivos) {
        if (dto == null) {
            throw new IllegalArgumentException("Dados da ocorrência são obrigatórios.");
        }
        Long empregadoId = dto.empregadoId();
        String motivo = dto.motivo();
        LocalDate dataOcorrencia = dto.dataOcorrencia();
        String observacao = dto.observacao();
        String tipoRo = dto.tipoRo();
        String numeroSei = dto.numeroSei();
        Integer dias = dto.dias();
        if (dataOcorrencia == null) {
            throw new IllegalArgumentException("Data da ocorrência é obrigatória.");
        }
        if (dataOcorrencia.isAfter(LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)))) {
            throw new IllegalArgumentException("Não é permitido registrar ocorrência em data futura.");
        }
        if (dias != null && dias > maxPeriodDays) {
            throw new IllegalArgumentException("O período máximo permitido é de " + maxPeriodDays + " dias.");
        }

        Empregado empregado = empregadoRepositorio.buscarPorIdParaAtualizacao(empregadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado"));
        if (!podeGerenciarEmpregado(empregado)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para gerir este colaborador.");
        }

        validarEnvios(arquivos);

        Ocorrencia obj = new Ocorrencia();
        obj.setEmpregado(empregado);
        obj.setMotivo(normalizarObrigatorio(motivo, "motivo"));
        obj.setDataOcorrencia(dataOcorrencia != null ? dataOcorrencia : LocalDate.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)));
        obj.setObservacao(normalizarOpcional(observacao));
        obj.setTipoRo(normalizarOpcional(tipoRo));
        obj.setNumeroSei(normalizarOpcional(numeroSei));
        obj.setQuantidadeDias((dias != null && dias > 0) ? dias : 1);

        if (ConstantesDominio.FREQ_SUSPENSAO.equalsIgnoreCase(motivo)) {
            obj.setDiasSuspensao(dias);
        } else if (ConstantesDominio.FREQ_ATESTADO.equalsIgnoreCase(motivo) || ConstantesDominio.OCORRENCIA_LICENCA_MEDICA.equalsIgnoreCase(motivo)) {
            obj.setDiasAfastamento(dias);
        }

        List<EnvioPreparado> stagedUploads = prepararEnvios(arquivos);
        registrarEnviosPreparados(stagedUploads);

        Ocorrencia saved = repositorio.save(obj);

        for (EnvioPreparado staged : stagedUploads) {
            AnexoOcorrencia anexo = new AnexoOcorrencia(
                    staged.nomeOriginal(),
                    staged.nomeSalvo(),
                    staged.tipoArquivo(),
                    saved
            );
            anexoRepositorio.save(anexo);
            saved.getAnexos().add(anexo);
        }

        registrarHistorico(saved, empregado);

        auditLogServico.registrarAcao(
                "OCORRENCIA_CREATE",
                "Ocorrencia",
                saved.getId(),
                "Ocorrência de " + empregado.getNome(),
                RegistroAuditoriaServico.detalhes(
                        "empregadoId", empregado.getId(),
                        "motivo", saved.getMotivo(),
                        "data", saved.getDataOcorrencia(),
                        "dias", saved.getQuantidadeDias(),
                        "arquivos", saved.getAnexos().stream().map(AnexoOcorrencia::getNomeOriginal).collect(Collectors.toList())
                )
        );

        int diasParaProcessar = (dias != null && dias > 0) ? dias : 1;
        reconciliarFrequenciaPorOcorrencias(empregado, saved.getDataOcorrencia(), diasParaProcessar);

        return paraResumoDTO(saved);
    }

    private void registrarHistorico(Ocorrencia ocorrencia, Empregado empregado) {
        String username = ConstantesDominio.USUARIO_SISTEMA;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
        }

        UsuarioAplicacao usuarioAtual = appUserRepositorio.findByUsername(username).orElse(null);

        HistoricoEmpregado historico = new HistoricoEmpregado();
        historico.setEmpregado(empregado);
        historico.setDataEvento(LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)));
        historico.setUsuario(usuarioAtual);

        StringBuilder desc = new StringBuilder();
        desc.append("Nova Ocorrência: ").append(ocorrencia.getMotivo());

        if (ocorrencia.getDataOcorrencia() != null) {
            desc.append(" em ").append(ocorrencia.getDataOcorrencia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (ocorrencia.getQuantidadeDias() != null && ocorrencia.getQuantidadeDias() > 0) {
            desc.append(" (Duração: ").append(ocorrencia.getQuantidadeDias()).append(" dias)");
        }
        if (!ocorrencia.getAnexos().isEmpty()) {
            desc.append(". Anexos: ");
            String arquivos = ocorrencia.getAnexos().stream()
                    .map(AnexoOcorrencia::getNomeOriginal)
                    .collect(Collectors.joining(", "));
            desc.append(arquivos);
        }
        if (ocorrencia.getNumeroSei() != null) {
            desc.append(". SEI: ").append(ocorrencia.getNumeroSei());
        }

        historico.setDescricao(desc.toString());
        historicoIntegridadeServico.salvarAssinado(historico);
    }

    private void reconciliarFrequenciaPorOcorrencias(Empregado empregado, LocalDate dataInicio, int dias) {
        if (empregado == null || dataInicio == null || dias <= 0) return;
        LocalDate dataFim = dataInicio.plusDays(dias - 1L);
        LocalDate inicioCandidatos = dataInicio.minusDays(Math.max(0, maxPeriodDays - 1L));
        List<Ocorrencia> ocorrenciasDoEmpregado = repositorio
                .findByEmpregadoIdAndDataOcorrenciaBetweenOrderByDataOcorrenciaDesc(
                        empregado.getId(), inicioCandidatos, dataFim);
        java.util.Map<LocalDate, Frequencia> frequenciasPorData = frequenciaRepositorio
                .findByEmpregadoIdAndDataReferenciaBetween(empregado.getId(), dataInicio, dataFim).stream()
                .collect(java.util.stream.Collectors.toMap(Frequencia::getDataReferencia, f -> f));
        java.util.Map<LocalDate, Contrato> contratosPorData =
                frequenciaServico.resolverContratosVigentesNoPeriodo(empregado, dataInicio, dataFim);

        for (int i = 0; i < dias; i++) {
            LocalDate dataAtual = dataInicio.plusDays(i);
            reconciliarFrequenciaDia(empregado, dataAtual, ocorrenciasDoEmpregado,
                    frequenciasPorData, contratosPorData.get(dataAtual));
        }
    }

    private void reconciliarFrequenciaDia(Empregado empregado,
                                          LocalDate dataAtual,
                                          List<Ocorrencia> ocorrenciasDoEmpregado,
                                          java.util.Map<LocalDate, Frequencia> frequenciasPorData,
                                          Contrato contratoVigente) {
        Frequencia frequencia = frequenciasPorData.get(dataAtual);
        if (frequencia == null) {
            frequencia = new Frequencia();
            frequencia.setEmpregado(empregado);
            frequencia.setDataReferencia(dataAtual);
            frequencia.setMes(dataAtual.getMonthValue());
            frequencia.setAno(dataAtual.getYear());
        }

        List<Ocorrencia> ocorrenciasAtivas = ocorrenciasDoEmpregado.stream()
                .filter(o -> abrangeData(o, dataAtual))
                .sorted(Comparator.comparingInt(this::prioridadeDoStatus))
                .toList();

        String novoStatus = ocorrenciasAtivas.stream()
                .map(o -> mapearMotivoParaStatus(normalizarMotivo(o.getMotivo())))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        String observacaoManual = removerNotasOcorrencia(frequencia.getObservacao());
        List<String> markers = new ArrayList<>();
        for (Ocorrencia ocorrencia : ocorrenciasAtivas) {
            String motivo = normalizarMotivo(ocorrencia.getMotivo());
            String label = obterRotuloFrequencia(motivo);
            markers.add(label + " (Ocorrência ID: " + ocorrencia.getId() + ")");
        }

        StringBuilder obsFinal = new StringBuilder();
        if (observacaoManual != null && !observacaoManual.isBlank()) {
            obsFinal.append(observacaoManual.trim());
        }
        if (!markers.isEmpty()) {
            if (obsFinal.length() > 0) obsFinal.append(" | ");
            obsFinal.append(String.join(" | ", markers));
        }

        if (novoStatus != null) {
            if (!Boolean.TRUE.equals(frequencia.getOcorrenciaOverrideAtivo())) {
                frequencia.setStatusBaseAntesOcorrencia(frequencia.getStatus());
                frequencia.setOcorrenciaOverrideAtivo(true);
            }
            frequencia.setStatus(novoStatus);
            // Batidas, horas físicas e memória de cálculo são evidências e não
            // podem ser destruídas por uma classificação administrativa.
        } else if (Boolean.TRUE.equals(frequencia.getOcorrenciaOverrideAtivo())) {
            frequencia.setStatus(frequencia.getStatusBaseAntesOcorrencia());
            frequencia.setStatusBaseAntesOcorrencia(null);
            frequencia.setOcorrenciaOverrideAtivo(false);
        }

        frequencia.setObservacao(obsFinal.length() == 0 ? null : obsFinal.toString());

        boolean shouldPersist = frequencia.getId() != null
                || frequencia.getStatus() != null
                || frequencia.getObservacao() != null
                || frequencia.getHorasTrabalhadas() != null
                || frequencia.getHorarioSaida() != null;

        if (shouldPersist) {
            frequenciaServico.garantirSnapshotHistorico(frequencia, empregado, dataAtual, contratoVigente);
            Frequencia salva = frequenciaRepositorio.save(frequencia);
            frequenciasPorData.put(dataAtual, salva);
        }
    }

    private boolean abrangeData(Ocorrencia ocorrencia, LocalDate data) {
        if (ocorrencia == null || ocorrencia.getDataOcorrencia() == null || data == null) return false;
        int dias = ocorrencia.getQuantidadeDias() != null && ocorrencia.getQuantidadeDias() > 0 ? ocorrencia.getQuantidadeDias() : 1;
        LocalDate inicio = ocorrencia.getDataOcorrencia();
        LocalDate fim = inicio.plusDays(dias - 1L);
        return !data.isBefore(inicio) && !data.isAfter(fim);
    }

    private int prioridadeDoStatus(Ocorrencia ocorrencia) {
        String status = mapearMotivoParaStatus(normalizarMotivo(ocorrencia != null ? ocorrencia.getMotivo() : null));
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(status)) return 0;
        if (ConstantesDominio.FREQ_FALTA.equals(status)) return 1;
        if (ConstantesDominio.FREQ_ATESTADO.equals(status)) return 2;
        if (ConstantesDominio.FREQ_ATRASO.equals(status)) return 3;
        return 10;
    }

    private String removerNotasOcorrencia(String value) {
        if (value == null || value.isBlank()) return null;

        String regexDyn = "(?i)(^|\\s*\\|\\s*)(" +
                obterRotuloFrequencia(ConstantesDominio.FREQ_FALTA) + "|" +
                obterRotuloFrequencia(ConstantesDominio.FREQ_SUSPENSAO) + "|" +
                obterRotuloFrequencia(ConstantesDominio.FREQ_ATESTADO) + "|" +
                obterRotuloFrequencia(ConstantesDominio.FREQ_ATRASO) +
                ") \\(Ocorrência ID: \\d+\\)";

        String cleaned = value
                .replaceAll(regexDyn, "")
                .replaceAll("\\s*\\|\\s*\\|\\s*", " | ")
                .replaceAll("^\\s*\\|\\s*", "")
                .replaceAll("\\s*\\|\\s*$", "")
                .trim();
        return cleaned.isBlank() ? null : cleaned;
    }


    private List<EnvioPreparado> prepararEnvios(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) return List.of();

        List<EnvioPreparado> staged = new ArrayList<>();
        try {
            for (MultipartFile arquivo : arquivos) {
                if (arquivo == null || arquivo.isEmpty()) continue;

                String nomeOriginal = higienizarNomeArquivo(arquivo.getOriginalFilename());
                String extensao = extrairExtensaoSegura(nomeOriginal);
                String nomeSalvo = UUID.randomUUID() + extensao;
                Path temporario = uploadTempRoot.resolve(UUID.randomUUID() + ".upload").normalize();
                Path definitivo = resolverArquivoArmazenado(nomeSalvo);
                garantirDentroDiretorio(temporario, uploadTempRoot);

                try (var entrada = arquivo.getInputStream()) {
                    Files.copy(entrada, temporario, StandardCopyOption.REPLACE_EXISTING);
                }
                staged.add(new EnvioPreparado(temporario, definitivo, nomeOriginal, nomeSalvo, resolverTipoConteudoSeguro(arquivo.getContentType())));
            }
            return List.copyOf(staged);
        } catch (Exception e) {
            limparArquivos(staged.stream().map(EnvioPreparado::temporario).toList());
            throw new IllegalStateException("Não foi possível preparar os anexos para gravação.", e);
        }
    }

    private void registrarEnviosPreparados(List<EnvioPreparado> staged) {
        if (staged == null || staged.isEmpty()) return;
        exigirTransacaoAtiva();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            private final List<Path> moved = new ArrayList<>();

            @Override
            public void beforeCommit(boolean readOnly) {
                try {
                    for (EnvioPreparado item : staged) {
                        moverAtomicamente(item.temporario(), item.definitivo());
                        moved.add(item.definitivo());
                    }
                } catch (IOException e) {
                    limparArquivos(moved);
                    throw new IllegalStateException("Falha ao concluir a gravação dos anexos; a operação foi cancelada.", e);
                }
            }

            @Override
            public void afterCompletion(int status) {
                limparArquivos(staged.stream().map(EnvioPreparado::temporario).toList());
                if (status != STATUS_COMMITTED) {
                    limparArquivos(moved);
                }
            }
        });
    }

    private void registrarExclusaoAposCommit(List<Path> files) {
        if (files == null || files.isEmpty()) return;
        exigirTransacaoAtiva();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Path file : files) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        log.error("Registro removido, mas o arquivo físico não pôde ser excluído: {}", file, e);
                    }
                }
            }
        });
    }

    private void exigirTransacaoAtiva() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Operação de anexos exige uma transação ativa.");
        }
    }

    private Path resolverArquivoArmazenado(String nomeSalvo) {
        String nomeSeguro = Paths.get(nomeSalvo == null ? "" : nomeSalvo).getFileName().toString();
        if (nomeSeguro.isBlank() || !nomeSeguro.equals(nomeSalvo)) {
            throw new IllegalArgumentException("Nome de anexo inválido.");
        }
        Path result = uploadRoot.resolve(nomeSeguro).normalize();
        garantirDentroDiretorio(result, uploadRoot);
        return result;
    }

    private void garantirDentroDiretorio(Path path, Path root) {
        if (!path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
    }

    private String extrairExtensaoSegura(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        if (dot < 0) return "";
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private void moverAtomicamente(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void limparArquivos(List<Path> files) {
        if (files == null) return;
        for (Path file : files) {
            if (file == null) continue;
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.warn("Não foi possível limpar arquivo temporário: {}", file, e);
            }
        }
    }

    private record EnvioPreparado(
            Path temporario,
            Path definitivo,
            String nomeOriginal,
            String nomeSalvo,
            String tipoArquivo
    ) {}

    private void validarEnvios(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) return;
        if (arquivos.size() > maxFilesPerRequest) {
            throw new IllegalArgumentException("Quantidade máxima de anexos por envio: " + maxFilesPerRequest + ".");
        }

        long total = 0L;
        for (MultipartFile arquivo : arquivos) {
            if (arquivo == null || arquivo.isEmpty()) continue;
            total += arquivo.getSize();
            if (arquivo.getSize() > maxFileSizeBytes) {
                throw new IllegalArgumentException("Arquivo excede o limite individual definido de upload: " + higienizarNomeArquivo(arquivo.getOriginalFilename()));
            }
            String contentType = resolverTipoConteudoSeguro(arquivo.getContentType());
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Tipo de arquivo não permitido: " + higienizarNomeArquivo(arquivo.getOriginalFilename()));
            }
        }

        if (total > maxTotalUploadBytes) {
            throw new IllegalArgumentException("Os anexos excedem o limite total de envio.");
        }
    }

    private String higienizarNomeArquivo(String value) {
        if (value == null || value.isBlank()) return "arquivo";
        return value.replaceAll("[\\r\\n\\t\\\\/]+", "_").trim();
    }

    private String resolverTipoConteudoSeguro(String contentType) {
        String normalized = contentType == null ? "application/octet-stream" : contentType.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_CONTENT_TYPES.contains(normalized)) return normalized;
        return "application/octet-stream";
    }

    private String normalizarMotivo(String motivo) {
        return motivo == null ? null : motivo.trim().toUpperCase(Locale.ROOT);
    }

    private String mapearMotivoParaStatus(String motivo) {
        if (ConstantesDominio.FREQ_FALTA.equals(motivo)) return ConstantesDominio.FREQ_FALTA;
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(motivo)) return ConstantesDominio.FREQ_SUSPENSAO;
        if (ConstantesDominio.FREQ_ATESTADO.equals(motivo) || ConstantesDominio.OCORRENCIA_LICENCA_MEDICA.equals(motivo)) return ConstantesDominio.FREQ_ATESTADO;
        if (ConstantesDominio.FREQ_ATRASO.equals(motivo)) return ConstantesDominio.FREQ_ATRASO;
        return null;
    }

    private String obterRotuloFrequencia(String motivo) {
        if (ConstantesDominio.FREQ_FALTA.equals(motivo)) return "Falta";
        if (ConstantesDominio.FREQ_SUSPENSAO.equals(motivo)) return "Suspensão";
        if (ConstantesDominio.FREQ_ATESTADO.equals(motivo) || ConstantesDominio.OCORRENCIA_LICENCA_MEDICA.equals(motivo)) return "Atestado/Abono";
        if (ConstantesDominio.FREQ_ATRASO.equals(motivo)) return "Atraso registrado";
        return "Evento";
    }

    private static String normalizarOpcional(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String normalizarObrigatorio(String value, String field) {
        String v = normalizarOpcional(value);
        if (v == null) {
            throw new IllegalArgumentException("Campo '" + field + "' é obrigatório.");
        }
        return v;
    }

    private OcorrenciaResumoDTO paraResumoDTO(Ocorrencia entity) {
        List<OcorrenciaResumoDTO.AnexoResumo> anexos = entity.getAnexos() == null ? List.of()
                : entity.getAnexos().stream()
                .map(a -> new OcorrenciaResumoDTO.AnexoResumo(a.getId(), a.getNomeOriginal(), a.getTipoArquivo()))
                .toList();
        return new OcorrenciaResumoDTO(
                entity.getId(),
                new OcorrenciaResumoDTO.EmpregadoResumo(entity.getEmpregado().getId(), entity.getEmpregado().getNome()),
                entity.getDataOcorrencia(), entity.getMotivo(), entity.getTipoRo(), entity.getNumeroSei(),
                entity.getObservacao(), entity.getQuantidadeDias(), entity.getDiasSuspensao(),
                entity.getDiasAfastamento(), anexos
        );
    }
    @Transactional(rollbackOn = Exception.class)
    public void excluir(Long id) {
        Ocorrencia ocorrencia = repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ocorrência não encontrada"));

        Long empregadoId = ocorrencia.getEmpregado().getId();
        Empregado empregado = empregadoRepositorio.buscarPorIdParaAtualizacao(empregadoId)
                .orElseThrow(() -> new IllegalArgumentException("Empregado não encontrado"));
        if (!podeGerenciarEmpregado(empregado)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para gerir este colaborador.");
        }

        int dias = (ocorrencia.getQuantidadeDias() != null && ocorrencia.getQuantidadeDias() > 0)
                ? ocorrencia.getQuantidadeDias()
                : 1;

        List<Path> arquivosParaExcluir = ocorrencia.getAnexos() == null
                ? List.of()
                : ocorrencia.getAnexos().stream()
                .map(AnexoOcorrencia::getNomeSalvo)
                .map(this::resolverArquivoArmazenado)
                .toList();

        auditLogServico.registrarAcao(
                "OCORRENCIA_DELETE",
                "Ocorrencia",
                id,
                "Remoção Ocorrência (" + ocorrencia.getMotivo() + ")",
                RegistroAuditoriaServico.detalhes(
                        "empregadoId", ocorrencia.getEmpregado().getId(),
                        "motivo", ocorrencia.getMotivo(),
                        "data", ocorrencia.getDataOcorrencia()
                )
        );

        UsuarioAplicacao usuarioAtual = appUserRepositorio.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElse(null);

        HistoricoEmpregado hist = new HistoricoEmpregado();
        hist.setEmpregado(empregado);
        hist.setDataEvento(LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO)));
        hist.setUsuario(usuarioAtual);
        hist.setDescricao("Ocorrência REMOVIDA: " + ocorrencia.getMotivo() + " de " + ocorrencia.getDataOcorrencia());
        historicoIntegridadeServico.salvarAssinado(hist);

        repositorio.delete(ocorrencia);
        repositorio.flush();
        registrarExclusaoAposCommit(arquivosParaExcluir);
        reconciliarFrequenciaPorOcorrencias(empregado, ocorrencia.getDataOcorrencia(), dias);
        return;
    }
}
