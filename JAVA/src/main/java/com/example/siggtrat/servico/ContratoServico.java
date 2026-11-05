package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.CancelamentoTransferenciaGestaoRequisicao;
import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.dto.EventoCicloVidaContratoDTO;
import com.example.siggtrat.dto.PublicacaoContratoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.PreviaCicloVidaContratoDTO;
import com.example.siggtrat.dto.RelacaoPrecedenciaEventoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoRequisicao;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.dto.VersaoContratoLeituraDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoLeituraDTO;
import com.example.siggtrat.dto.ProvaCampoHistoricoDTO;
import com.example.siggtrat.dto.OrigemProvaCampoDTO;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ContratoServico {

    private final ContratoRepositorio contratoRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final HistoricoVinculoContratoRepositorio historicoVinculoRepositorio;
    private final AcessoOrganizacionalServico acessoOrganizacionalServico;

    private final TipoContratacaoRepositorio tipoContratacaoRepositorio;
    private final FornecedorRepositorio fornecedorRepositorio;
    private final EventoCicloVidaContratoRepositorio eventoCicloVidaRepositorio;
    private final ComposicaoContratualRepositorio composicaoContratualRepositorio;
    private final MotorTemporalContratoServico motorTemporalContratoServico;
    private final IdentidadeContratualServico identidadeContratualServico;
    private AditamentoContratualServico aditamentoContratualServico;
    private VinculoContratualServico vinculoContratualServico;
    private TransferenciaGestaoContratoServico transferenciaGestaoContratoServico;
    private AlocacaoVinculoServico alocacaoVinculoServico;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;
    private PreviaCicloVidaContratoServico previaCicloVidaContratoServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private GestaoContratoTemporalServico gestaoContratoTemporal;
    private ProjecaoOperacionalPessoaServico projecaoOperacionalPessoaServico;

    /**
     * Construtor principal usado pelo Spring.
     */
    @Autowired
    public ContratoServico(
            ContratoRepositorio contratoRepositorio,
            EmpregadoRepositorio empregadoRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            HistoricoVinculoContratoRepositorio historicoVinculoRepositorio,
            AcessoOrganizacionalServico acessoOrganizacionalServico,
            TipoContratacaoRepositorio tipoContratacaoRepositorio,
            FornecedorRepositorio fornecedorRepositorio,
            EventoCicloVidaContratoRepositorio eventoCicloVidaRepositorio,
            ComposicaoContratualRepositorio composicaoContratualRepositorio,
            MotorTemporalContratoServico motorTemporalContratoServico,
            IdentidadeContratualServico identidadeContratualServico
    ) {
        this.contratoRepositorio = contratoRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.historicoVinculoRepositorio = historicoVinculoRepositorio;
        this.acessoOrganizacionalServico = acessoOrganizacionalServico;
        this.tipoContratacaoRepositorio = tipoContratacaoRepositorio;
        this.fornecedorRepositorio = fornecedorRepositorio;
        this.eventoCicloVidaRepositorio = eventoCicloVidaRepositorio;
        this.composicaoContratualRepositorio = composicaoContratualRepositorio;
        this.motorTemporalContratoServico = motorTemporalContratoServico;
        this.identidadeContratualServico = identidadeContratualServico;
    }

    @Autowired(required = false)
    public void configurarAditamentoTemporal(AditamentoContratualServico aditamentoContratualServico) {
        this.aditamentoContratualServico = aditamentoContratualServico;
    }

    @Autowired(required = false)
    public void configurarVinculosCanonicos(VinculoContratualServico vinculoContratualServico) {
        this.vinculoContratualServico = vinculoContratualServico;
    }

    @Autowired(required = false)
    public void configurarTransferenciaGestao(TransferenciaGestaoContratoServico transferenciaGestaoContratoServico) {
        this.transferenciaGestaoContratoServico = transferenciaGestaoContratoServico;
    }

    @Autowired(required = false)
    public void configurarAlocacaoTemporal(AlocacaoVinculoServico alocacaoVinculoServico) {
        this.alocacaoVinculoServico = alocacaoVinculoServico;
    }

    @Autowired(required = false)
    public void configurarDocumentos(DocumentoComprobatorioServico documentoComprobatorioServico) {
        this.documentoComprobatorioServico = documentoComprobatorioServico;
    }

    @Autowired(required = false)
    public void configurarRevalidacaoFrequencia(RevalidacaoFrequenciaTemporalServico servico) {
        this.revalidacaoFrequenciaTemporalServico = servico;
    }

    @Autowired
    public void configurarPreviaCicloVida(PreviaCicloVidaContratoServico servico) {
        this.previaCicloVidaContratoServico = servico;
    }

    @Autowired
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    @Autowired(required = false)
    public void configurarProjecaoOperacionalPessoa(
            @org.springframework.context.annotation.Lazy ProjecaoOperacionalPessoaServico servico) {
        this.projecaoOperacionalPessoaServico = servico;
    }

    public PreviaCicloVidaContratoDTO previaCicloVida(Long id, String operacao, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A operação de ciclo de vida exige contrato publicado.");
        }
        return previaCicloVidaContratoServico.gerar(contrato, operacao, acao, user);
    }

    public PreviaCicloVidaContratoDTO previaPublicacao(Long id, PublicacaoContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente contrato RASCUNHO pode ser publicado.");
        }
        validarContratoParaPublicacao(contrato, dto);
        validarComposicoesParaPublicacao(contrato, composicaoContratualRepositorio
                .findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(id));
        return previaCicloVidaContratoServico.gerar(contrato, "publicar", acaoPublicacao(contrato, dto), user);
    }

    @Cacheable(
            cacheNames = NomesCache.CONTRATOS,
            key = "@redisCacheKey.usuarioAtual() + ':' + T(com.example.siggtrat.utilitario.RelogioSistema).hoje()",
            sync = true
    )
    public List<ContratoDTO> listarTodos() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaContrato(user);

        List<Contrato> contratos = contratosNoEscopoAtual(user);

        LeituraHistoricaContratos entradas = carregarLeitura(contratos);
        Map<Long, Long> efetivos = efetivosOperacionaisAtuais(contratos);

        Map<Long, HabilitacaoAdministrativaContrato> habilitacoes =
                habilitacoesAdministrativasAtuais(contratos, entradas);

        return contratos.stream()
                .map(contrato -> paraDTO(
                        contrato,
                        habilitacoes.getOrDefault(
                                contrato.getId(),
                                contrato.getHabilitacaoAdministrativa()
                        ),
                        efetivos.getOrDefault(contrato.getId(), 0L), entradas
                ))
                .toList();
    }

    /**
     * Lista leve para filtros com a quantidade total atual de colaboradores por
     * contrato. A contagem é feita em uma única consulta agregada, sem N+1.
     */
    @Cacheable(
            cacheNames = NomesCache.CONTRATOS_OPCOES,
            key = "@redisCacheKey.usuarioAtual() + ':' + T(com.example.siggtrat.utilitario.RelogioSistema).hoje()",
            sync = true
    )
    public List<ContratoOpcaoDTO> obterOpcoes() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaContrato(user);

        List<Contrato> contratos = contratosNoEscopoAtual(user);

        if (contratos.isEmpty()) return List.of();

        LeituraHistoricaContratos entradas = carregarLeitura(contratos);
        Map<Long, Long> quantidadePorContrato = efetivosOperacionaisAtuais(contratos);

        Map<Long, HabilitacaoAdministrativaContrato> habilitacoes =
                habilitacoesAdministrativasAtuais(contratos, entradas);

        LocalDate hoje = RelogioSistema.hoje();
        Instant conhecido = Instant.now();
        return contratos.stream()
                .map(c -> {
                    VersaoContratoNegocialDTO foto = c.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO
                            && aditamentoContratualServico != null
                            ? (entradas == null ? aditamentoContratualServico.projetarVersaoContrato(c, hoje, conhecido, Set.of())
                                : aditamentoContratualServico.projetarVersaoContrato(c, hoje, conhecido, Set.of(), entradas)) : null;
                    return new ContratoOpcaoDTO(
                        c.getId(),
                        foto == null ? c.getNumeroContrato() : foto.numeroContrato(),
                        foto == null ? c.getDescricao() : foto.descricao(),
                        habilitacoes.getOrDefault(c.getId(), c.getHabilitacaoAdministrativa())
                                == HabilitacaoAdministrativaContrato.ATIVO,
                        quantidadePorContrato.getOrDefault(c.getId(), 0L)
                    );
                })
                .toList();
    }

    private List<Contrato> contratosNoEscopoAtual(UsuarioAplicacao user) {
        if (acessoOrganizacionalServico.ehGlobal(user) || acessoOrganizacionalServico.ehPresidente(user))
            return contratoRepositorio.findAllByOrderByNumeroContratoAsc();
        List<Contrato> candidatos;
        if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            Long estadoId = acessoOrganizacionalServico.superintendenciaEfetiva(user).getId();
            candidatos = gestaoContratoTemporal == null
                    ? contratoRepositorio.findByGestaoLocalSuperintendenciaIdOrderByNumeroContratoAsc(estadoId)
                    : contratoRepositorio.buscarCandidatosSuperintendenciaTemporal(estadoId, RelogioSistema.hoje());
        } else if (user.getGestaoLocal() != null) {
            Long gestaoId = user.getGestaoLocal().getId();
            candidatos = gestaoContratoTemporal == null
                    ? contratoRepositorio.findByGestaoLocalIdOrderByNumeroContratoAsc(gestaoId)
                    : contratoRepositorio.buscarCandidatosGestaoTemporal(gestaoId, RelogioSistema.hoje());
        } else {
            return List.of();
        }
        return gestaoContratoTemporal == null ? candidatos : candidatos.stream()
                .filter(c -> acessoOrganizacionalServico.podeAcessarContrato(user, c)).toList();
    }

    private GestaoLocal gestaoAtual(Contrato contrato) {
        return gestaoContratoTemporal == null ? contrato.getGestaoLocal() : gestaoContratoTemporal.gestaoAtual(contrato);
    }

    @Cacheable(
            cacheNames = NomesCache.CONTRATO_POR_ID,
            key = "@redisCacheKey.usuarioAtual() + ':' + #p0 + ':' + T(com.example.siggtrat.utilitario.RelogioSistema).hoje()",
            sync = true
    )
    public ContratoDTO buscarPorId(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        return paraDTO(contrato);
    }

    /** Cria um Contrato em estado RASCUNHO. */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO criar(ContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = new Contrato();
        contrato.setEstadoNegocial(EstadoNegocialContrato.RASCUNHO);
        contrato.setAtivo(true);

        // Resolve o escopo antes dos catálogos para impedir que um Tipo de
        // Contratação de outra Gestão Local seja aceito quando gestaoLocalId
        // não vier explicitamente no JSON.
        contrato.setGestaoLocal(resolverGestaoLocalParaEscrita(
                user,
                dto != null ? dto.gestaoLocalId() : null,
                null
        ));
        mapearDTORascunho(dto, contrato);

        // A resolução do Tipo pode aguardar um escritor do catálogo.
        acessoOrganizacionalServico.revalidarAposEspera(user);
        validarRascunho(contrato, null);
        return paraDTO(contratoRepositorio.save(contrato));
    }

    /**
     * Depois de publicado o contrato não volta a ser formulário livre.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO atualizar(Long id, ContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        // A espera de escrita antecede a revalidação do executor e do estado.
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);

        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contrato publicado não pode ser editado livremente. Utilize o evento negocial correspondente."
            );
        }

        GestaoLocal gestaoLocalDestino = resolverGestaoLocalParaEscrita(
                user,
                dto != null ? dto.gestaoLocalId() : null,
                contrato.getGestaoLocal()
        );

        contrato.setGestaoLocal(gestaoLocalDestino);
        mapearDTORascunho(dto, contrato);
        validarRascunho(contrato, id);

        return paraDTO(contratoRepositorio.save(contrato));
    }

    /**
     * Publica o contrato somente após validar integralmente os dados e a
     * composição original. Após este ponto a composição original deixa de ser
     * editável diretamente e mudanças passam a exigir fato negocial versionado.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO publicar(Long id, PublicacaoContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente contrato RASCUNHO pode ser publicado.");
        }
        if (composicaoContratualRepositorio == null || eventoCicloVidaRepositorio == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Infraestrutura de publicação contratual indisponível.");
        }
        validarContratoParaPublicacao(contrato, dto);

        List<ComposicaoContratual> composicoes = composicaoContratualRepositorio
                .findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(id);
        validarComposicoesParaPublicacao(contrato, composicoes);

        if (contrato.getDataInicio().isBefore(RelogioSistema.hoje())) {
            validarConfirmacaoCicloVida(contrato, "publicar", acaoPublicacao(contrato, dto), user);
        }

        identidadeContratualServico.reservar(contrato, contrato.getNumeroContrato(), contrato.getAnoContrato());
        composicoes.forEach(ComposicaoContratual::fotografarCatalogosEmRascunho);
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setEfetivoOriginalPublicado(contrato.getEfetivoPrevisto());
        contrato.setReferenciaSeiPublicacao(limpar(dto.referenciaSei()));
        contrato.setPublicadoEm(java.time.Instant.now());
        contrato.setPublicadoPorId(user != null ? user.getId() : null);
        contrato.setPublicadoPorUsername(user != null ? user.getUsername() : null);

        Contrato salvo = contratoRepositorio.save(contrato);
        AcaoCicloVidaContratoDTO acao = acaoPublicacao(contrato, dto);
        EventoCicloVidaContrato eventoPublicacao = registrarEvento(
                salvo, TipoEventoCicloVidaContrato.PUBLICACAO, acao, user,
                EstadoNegocialContrato.RASCUNHO.name(), EstadoNegocialContrato.PUBLICADO.name()
        );
        if (motorTemporalContratoServico != null) {
            motorTemporalContratoServico.registrarFotografia(salvo, eventoPublicacao, salvo.getDataInicio());
        }
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(salvo, TipoFatoDocumental.CONTRATO_ORIGINAL,
                    "ORIGINAL", eventoPublicacao.getId(), dto.referenciaSei(), salvo.getDataInicio(), user);
        }
        return paraDTO(salvo);
    }

    private AcaoCicloVidaContratoDTO acaoPublicacao(Contrato contrato, PublicacaoContratoDTO dto) {
        return new AcaoCicloVidaContratoDTO(contrato.getDataInicio(),
                dto.dataPublicacao() != null ? dto.dataPublicacao() : RelogioSistema.hoje(),
                dto.justificativa() == null || dto.justificativa().isBlank() ? "Publicação do contrato" : dto.justificativa(),
                dto.referenciaSei(), null, null, null, dto.tokenPrevia(), dto.confirmarImpacto());
    }

    private void validarComposicoesParaPublicacao(
            Contrato contrato,
            List<ComposicaoContratual> composicoes
    ) {
        if (composicoes == null || composicoes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato deve possuir ao menos uma composição para ser publicado.");
        }
        long soma = composicoes.stream()
                .mapToLong(c -> Optional.ofNullable(c.getEfetivoPrevisto()).orElse(0))
                .sum();
        if (soma != Optional.ofNullable(contrato.getEfetivoPrevisto()).orElse(0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A soma do efetivo das composições (" + soma + ") deve ser igual ao efetivo total do contrato ("
                            + contrato.getEfetivoPrevisto() + ").");
        }

        Set<String> chaves = new HashSet<>();
        for (ComposicaoContratual c : composicoes) {
            validarCatalogosDaComposicaoParaPublicacao(contrato, c);
            String chave = c.getLotacao().getId() + ":" + c.getJornada().getId() + ":" + c.getCargo().getId();
            if (!chaves.add(chave)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Existem composições duplicadas na fotografia original do contrato.");
            }
            if (!Objects.equals(c.getDataInicioVigencia(), contrato.getDataInicio())
                    || !Objects.equals(c.getDataFimVigencia(), contrato.getDataFim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Na publicação, cada composição original deve cobrir integralmente a vigência do contrato. "
                                + "Alterações posteriores devem ser registradas por fato negocial versionado.");
            }
        }
    }

    private void validarCatalogosDaComposicaoParaPublicacao(Contrato contrato, ComposicaoContratual composicao) {
        Lotacao lotacao = composicao.getLotacao();
        Jornada jornada = composicao.getJornada();
        Cargo cargo = composicao.getCargo();
        if (disponibilidadeCatalogo != null && lotacao != null && jornada != null && cargo != null) {
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, lotacao.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, jornada.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, cargo.getId());
        }
        Long gestaoId = contrato.getGestaoLocal() == null ? null : contrato.getGestaoLocal().getId();
        if (lotacao == null || jornada == null || cargo == null || gestaoId == null
                || lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null || cargo.getGestaoLocal() == null
                || !Objects.equals(gestaoId, lotacao.getGestaoLocal().getId())
                || !Objects.equals(gestaoId, jornada.getGestaoLocal().getId())
                || !Objects.equals(gestaoId, cargo.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo da composição devem pertencer à Gestão Local do contrato.");
        }
        if (lotacao.getTurnos() == null || lotacao.getTurnos().stream()
                .noneMatch(turno -> turno != null && Objects.equals(turno.getId(), jornada.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Turno da composição deve continuar vinculado à Lotação na publicação.");
        }
        LocalDate data = contrato.getDataInicio();
        if (!catalogoDisponivelEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO,
                    lotacao.getId(), data, lotacao.getAtivo())
                || !catalogoDisponivelEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA,
                    jornada.getId(), data, jornada.getAtivo())
                || !catalogoDisponivelEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO,
                    cargo.getId(), data, cargo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo devem estar disponíveis na data inicial da composição publicada.");
        }
    }

    private boolean catalogoDisponivelEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal tipo,
                                         Long id, LocalDate data, Boolean ativo) {
        return disponibilidadeCatalogo == null ? Boolean.TRUE.equals(ativo)
                : disponibilidadeCatalogo.ativoEm(tipo, id, data, ativo);
    }

    private void validarContratoParaPublicacao(Contrato contrato, PublicacaoContratoDTO dto) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados de publicação não informados.");
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.validarFonteObrigatoria(contrato,
                    TipoFatoDocumental.CONTRATO_ORIGINAL, "ORIGINAL", dto.referenciaSei());
        } else if (dto.referenciaSei() == null || dto.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe ao menos uma fonte comprobatória por anexo ou referência SEI.");
        }
        if (contrato.getNumeroContrato() == null || contrato.getNumeroContrato().isBlank()
                || contrato.getAnoContrato() == null
                || contrato.getDescricao() == null || contrato.getDescricao().isBlank()
                || contrato.getDataInicio() == null || contrato.getDataFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Número, ano, descrição e vigência completa são obrigatórios para publicação.");
        }
        if (!tipoDisponivelEm(contrato.getTipoContratacao(), contrato.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecione um Tipo de Contratação disponível na data inicial do contrato.");
        }
        if (contrato.getCnpjEmpresa() == null || contrato.getCnpjEmpresa().isBlank()
                || contrato.getRazaoSocialEmpresa() == null || contrato.getRazaoSocialEmpresa().isBlank()
                || contrato.getNomeFantasiaEmpresa() == null || contrato.getNomeFantasiaEmpresa().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CNPJ, razão social e nome fantasia da empresa são obrigatórios para publicação.");
        }
        if (contrato.getCnpjEmpresa().replaceAll("\\D", "").length() != 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ da empresa deve conter 14 dígitos.");
        }
        if (contrato.getEfetivoPrevisto() == null || contrato.getEfetivoPrevisto() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Efetivo total previsto inválido.");
        }
        validarUnicidadeNumeroAno(contrato, contrato.getId());
    }

    /**
     * Exclusão física é restrita a rascunho que nunca produziu fato negocial.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void excluir(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        // Mantém a ordem Contrato -> executor antes de descartar dependências.
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);

        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contrato publicado não pode ser excluído fisicamente."
            );
        }
        if (empregadoRepositorio.existsByContratosId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Não é possível descartar o rascunho enquanto houver empregados vinculados."
            );
        }
        if (historicoVinculoRepositorio.existsByContratoId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O contrato possui histórico de vínculo e não pode ser descartado."
            );
        }
        if (eventoCicloVidaRepositorio != null && eventoCicloVidaRepositorio.existsByContratoId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O contrato já possui fato negocial registrado e não pode ser descartado."
            );
        }

        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.descartarRascunhosExclusivos(contrato, user);
        }

        if (composicaoContratualRepositorio != null) {
            composicaoContratualRepositorio.deleteByContratoId(id);
        }
        contratoRepositorio.delete(contrato);
        contratoRepositorio.flush();
    }

    /**
     * Inativação administrativa não altera vigência nem encerra vínculos.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO inativarAdministrativamente(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        LocalDate dataEfetiva = acao.dataEfetiva();
        EventoCicloVidaContrato substituido = eventoAdministrativoSubstituido(contrato, acao);
        HabilitacaoAdministrativaContrato anterior = habilitacaoAntesDaAcao(contrato, acao, substituido);
        LocalDate desde = inicioDoAlcance(acao, substituido);
        if (substituido == null && anterior == HabilitacaoAdministrativaContrato.INATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato já está administrativamente inativo na data informada.");
        }

        validarConfirmacaoCicloVida(contrato, "inativar", acao, user);

        registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA,
                acao,
                user,
                anterior == null ? null : anterior.name(),
                "INATIVO"
        );

        if (!desde.isAfter(RelogioSistema.hoje())) {
            HabilitacaoAdministrativaContrato atual = habilitacaoAdministrativaEm(contrato, RelogioSistema.hoje());
            contrato.setAtivo(atual == HabilitacaoAdministrativaContrato.ATIVO);
            contratoRepositorio.save(contrato);
            if (projecaoOperacionalPessoaServico != null)
                projecaoOperacionalPessoaServico.sincronizarAposAlteracaoContratual(contrato);
        }
        revalidarFrequenciasContrato(contrato, desde,
                "Inativação administrativa do Contrato alterou a cobertura operacional");

        return paraDTO(contrato);
    }

    /**
     * Reativação é um novo evento; não apaga a inativação anterior.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO reativarAdministrativamente(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        EventoCicloVidaContrato substituido = eventoAdministrativoSubstituido(contrato, acao);
        HabilitacaoAdministrativaContrato anterior = habilitacaoAntesDaAcao(contrato, acao, substituido);
        LocalDate desde = inicioDoAlcance(acao, substituido);
        if (substituido == null && anterior == HabilitacaoAdministrativaContrato.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato já está administrativamente ativo na data informada.");
        }

        validarConfirmacaoCicloVida(contrato, "reativar", acao, user);

        if (alocacaoVinculoServico != null) {
            alocacaoVinculoServico.validarRetomadaOperacional(contrato, acao.dataEfetiva());
        }

        registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA,
                acao,
                user,
                anterior == null ? null : anterior.name(),
                "ATIVO"
        );

        if (!desde.isAfter(RelogioSistema.hoje())) {
            HabilitacaoAdministrativaContrato atual = habilitacaoAdministrativaEm(contrato, RelogioSistema.hoje());
            contrato.setAtivo(atual == HabilitacaoAdministrativaContrato.ATIVO);
            contratoRepositorio.save(contrato);
            if (projecaoOperacionalPessoaServico != null)
                projecaoOperacionalPessoaServico.sincronizarAposAlteracaoContratual(contrato);
        }
        revalidarFrequenciasContrato(contrato, desde,
                "Reativação administrativa retroativa do Contrato exige revalidação das frequências");

        return paraDTO(contrato);
    }

    private EventoCicloVidaContrato eventoAdministrativoSubstituido(Contrato contrato, AcaoCicloVidaContratoDTO acao) {
        if (acao.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.RETIFICA
                && acao.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.SUBSTITUI) return null;
        if (acao.eventoPrecedenteId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A retificação administrativa exige evento precedente explícito.");
        EventoCicloVidaContrato anterior = eventoCicloVidaRepositorio.findById(acao.eventoPrecedenteId())
                .filter(e -> e.getContrato() != null && Objects.equals(e.getContrato().getId(), contrato.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Evento precedente não encontrado neste contrato."));
        if (anterior.getTipoEvento() != TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                && anterior.getTipoEvento() != TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A retificação administrativa não pode substituir um fato de outra operação negocial.");
        if (anterior.getSituacaoFato() != SituacaoFatoNegocial.VIGENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente a versão vigente do fato administrativo pode ser retificada.");
        return anterior;
    }

    private HabilitacaoAdministrativaContrato habilitacaoAntesDaAcao(Contrato contrato, AcaoCicloVidaContratoDTO acao,
            EventoCicloVidaContrato substituido) {
        return substituido == null ? habilitacaoAdministrativaEm(contrato, acao.dataEfetiva())
                : motorTemporalContratoServico.habilitacaoAdministrativaEm(
                        contrato, acao.dataEfetiva(), Instant.now(), Set.of(substituido.getId()));
    }

    private LocalDate inicioDoAlcance(AcaoCicloVidaContratoDTO acao, EventoCicloVidaContrato substituido) {
        return substituido != null && substituido.getDataEfetiva().isBefore(acao.dataEfetiva())
                ? substituido.getDataEfetiva() : acao.dataEfetiva();
    }

    /**
     * Encerramento antecipado altera a data final efetiva e encerra os Vínculos
     * ainda abertos por meio de evento temporal explícito.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO encerrarAntecipadamente(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.validarFonteObrigatoria(contrato,
                    TipoFatoDocumental.ENCERRAMENTO_ANTECIPADO,
                    TipoFatoDocumental.ENCERRAMENTO_ANTECIPADO.name(), acao.referenciaSei());
        } else if (acao.referenciaSei() == null || acao.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Encerramento antecipado exige fonte comprobatória por anexo ou SEI.");
        }
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente contrato publicado válido pode ser encerrado antecipadamente."
            );
        }
        VersaoContratoNegocialDTO vigencia = aditamentoContratualServico.projetarVersaoContrato(
                contrato, acao.dataEfetiva(), Instant.now(), Set.of());
        LocalDate inicioVigente = vigencia.dataInicio();
        LocalDate fimVigente = vigencia.dataFim();
        for (EventoCicloVidaContrato evento : motorTemporalContratoServico.eventosValidosOrdenados(
                contrato.getId(), acao.dataEfetiva(), Instant.now())) {
            if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO)
                fimVigente = evento.getDataEfetiva();
        }
        if (inicioVigente == null || fimVigente == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contrato publicado sem vigência completa deve ser regularizado antes do encerramento."
            );
        }
        if (acao.dataEfetiva().isBefore(inicioVigente)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data efetiva do encerramento não pode ser anterior ao início do contrato."
            );
        }
        if (!acao.dataEfetiva().isBefore(fimVigente)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Encerramento antecipado deve reduzir a data final vigente do contrato."
            );
        }

        validarConfirmacaoCicloVida(contrato, "encerrar-antecipadamente", acao, user);
        LocalDate dataFimAnterior = fimVigente;

        EventoCicloVidaContrato eventoEncerramento = registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO,
                acao,
                user,
                dataFimAnterior.toString(),
                acao.dataEfetiva().toString()
        );

        contrato.setDataFim(acao.dataEfetiva());
        Contrato salvo = contratoRepositorio.save(contrato);
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(salvo,
                    TipoFatoDocumental.ENCERRAMENTO_ANTECIPADO,
                    TipoFatoDocumental.ENCERRAMENTO_ANTECIPADO.name(), eventoEncerramento.getId(),
                    acao.referenciaSei(), acao.dataEfetiva(), user);
        }
        if (vinculoContratualServico != null) {
            vinculoContratualServico.encerrarPorContrato(salvo, acao.dataEfetiva(),
                    com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.ENCERRAMENTO_ANTECIPADO_CONTRATO,
                    user, acao.motivo());
        }
        revalidarFrequenciasContrato(salvo, acao.dataEfetiva().plusDays(1),
                "Encerramento antecipado do Contrato retirou cobertura posterior");
        return paraDTO(salvo);
    }

    /**
     * CANCELADO é permitido para instrumento publicado que ainda não iniciou.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO cancelarContratoFuturo(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || contrato.situacaoTemporalEm(RelogioSistema.hoje()) != SituacaoTemporalContrato.FUTURO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente contrato publicado FUTURO pode ser cancelado antes do início."
            );
        }
        LocalDate inicioReconstruido = aditamentoContratualServico.projetarVersaoContrato(
                contrato, acao.dataEfetiva(), Instant.now(), Set.of()).dataInicio();
        if (inicioReconstruido == null || !acao.dataEfetiva().isBefore(inicioReconstruido)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O alcance do cancelamento de contrato FUTURO deve ser anterior ao início da vigência.");
        }

        validarConfirmacaoCicloVida(contrato, "cancelar-futuro", acao, user);

        registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.CANCELAMENTO_CONTRATO_FUTURO,
                acao,
                user,
                contrato.getEstadoNegocial().name(),
                EstadoNegocialContrato.CANCELADO.name()
        );

        contrato.setEstadoNegocial(EstadoNegocialContrato.CANCELADO);
        contrato.setAtivo(false);
        Contrato cancelado = contratoRepositorio.save(contrato);
        if (vinculoContratualServico != null) {
            vinculoContratualServico.cancelarProgramacoesPorContrato(cancelado, user, acao.motivo());
        }
        return paraDTO(cancelado);
    }

    /**
     * Anulação é explícita, preserva o contrato e registra separadamente a data
     * do ato, a data/hora de registro e o alcance efetivo.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO anular(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        if (acao.dataAto() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a data do ato anulatório.");
        }
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.validarFonteObrigatoria(contrato, TipoFatoDocumental.ANULACAO,
                    TipoFatoDocumental.ANULACAO.name(), acao.referenciaSei());
        } else if (acao.referenciaSei() == null || acao.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Anulação exige fonte comprobatória por anexo ou referência SEI.");
        }
        if (motorTemporalContratoServico.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, Instant.now()).stream()
                .anyMatch(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.ANULACAO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contrato já possui anulação válida, inclusive de alcance programado.");
        }

        validarConfirmacaoCicloVida(contrato, "anular", acao, user);

        EventoCicloVidaContrato eventoAnulacao = registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.ANULACAO,
                acao,
                user,
                contrato.getEstadoNegocial().name(),
                EstadoNegocialContrato.ANULADO.name()
        );

        contrato.setEstadoNegocial(motorTemporalContratoServico.estadoNegocialEm(contrato, RelogioSistema.hoje(), Instant.now()));
        if (!acao.dataEfetiva().isAfter(RelogioSistema.hoje())) contrato.setAtivo(false);
        Contrato anulado = contratoRepositorio.save(contrato);
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(anulado, TipoFatoDocumental.ANULACAO,
                    TipoFatoDocumental.ANULACAO.name(), eventoAnulacao.getId(), acao.referenciaSei(),
                    acao.dataEfetiva(), user);
        }
        if (vinculoContratualServico != null) {
            vinculoContratualServico.encerrarPorContrato(anulado, acao.dataEfetiva(),
                    com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo.ANULACAO_CONTRATO,
                    user, acao.motivo());
        }
        // A anulação pode retirar validade já no próprio dia de alcance, portanto
        // a revalidação contratual começa na data efetiva (não no dia seguinte).
        revalidarFrequenciasContrato(anulado, acao.dataEfetiva(),
                "Anulação do Contrato alterou a validade operacional das frequências");
        return paraDTO(anulado);
    }

    /**
     * Cancela negocialmente uma anulação indevida sem apagar o evento original.
     * O contrato permanece administrativamente inativo: a retomada operacional
     * exige o comando explícito de reativação e não reabre vínculos encerrados.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO corrigirAnulacao(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        validarAcaoComMotivo(acao);
        if (acao.eventoPrecedenteId() == null
                || acao.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.CANCELA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A correção deve informar o evento de ANULACAO e relacaoPrecedencia=CANCELA.");
        }
        EventoCicloVidaContrato anulacao = eventoCicloVidaRepositorio.findById(acao.eventoPrecedenteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Evento de anulação informado não encontrado."));
        if (anulacao.getTipoEvento() != TipoEventoCicloVidaContrato.ANULACAO
                || anulacao.getContrato() == null || !Objects.equals(anulacao.getContrato().getId(), contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O evento precedente não é a anulação deste contrato.");
        }
        boolean anulacaoValida = motorTemporalContratoServico
                .eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, Instant.now()).stream()
                .anyMatch(e -> Objects.equals(e.getId(), anulacao.getId()));
        if (!anulacaoValida) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Somente anulação ainda válida, inclusive programada, admite esta correção.");
        if (!Objects.equals(acao.dataEfetiva(), anulacao.getDataEfetiva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A correção deve possuir a mesma data efetiva da anulação cancelada.");
        }
        if (acao.referenciaSei() == null || acao.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correção de anulação exige referência documental/SEI.");
        }

        validarConfirmacaoCicloVida(contrato, "corrigir-anulacao", acao, user);
        registrarEvento(contrato, TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, acao, user,
                EstadoNegocialContrato.ANULADO.name(), EstadoNegocialContrato.PUBLICADO.name());
        contrato.setEstadoNegocial(motorTemporalContratoServico.estadoNegocialEm(contrato, RelogioSistema.hoje(), Instant.now()));
        contrato.setAtivo(habilitacaoAdministrativaEm(contrato, RelogioSistema.hoje()) == HabilitacaoAdministrativaContrato.ATIVO);
        Contrato corrigido = contratoRepositorio.save(contrato);
        revalidarFrequenciasContrato(corrigido, acao.dataEfetiva(),
                "Correção auditada de anulação alterou a verdade temporal do Contrato");
        return paraDTO(corrigido);
    }

    /**
     * Operação excepcional: muda a responsabilidade atual do contrato somente
     * após preservar a fotografia anterior e validar os mapas para o destino.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO transferirGestao(Long id, TransferenciaGestaoContratoRequisicao req) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        exigirGestorResponsavelPelaTransferencia(contrato, user);
        transferenciaGestaoContratoServico.transferir(contrato, req, user);
        return paraDTO(contratoRepositorio.save(contrato));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PreviaCicloVidaContratoDTO previaTransferenciaGestao(Long id, TransferenciaGestaoContratoRequisicao req) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        exigirGestorResponsavelPelaTransferencia(contrato, user);
        return transferenciaGestaoContratoServico.previa(contrato, req, user);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ContratoDTO cancelarTransferenciaGestao(Long id, Long transferenciaId,
            CancelamentoTransferenciaGestaoRequisicao req) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        exigirGestorResponsavelPelaTransferencia(contrato, user);
        transferenciaGestaoContratoServico.cancelarProgramada(contrato, transferenciaId, req, user);
        return paraDTO(contratoRepositorio.save(contrato));
    }

    private void exigirGestorResponsavelPelaTransferencia(Contrato contrato, UsuarioAplicacao user) {
        GestaoLocal gestaoResponsavel = gestaoAtual(contrato);
        if (!acessoOrganizacionalServico.ehGestaoLocal(user) || user.getGestaoLocal() == null
                || gestaoResponsavel == null || !Objects.equals(user.getGestaoLocal().getId(), gestaoResponsavel.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Transferência excepcional deve ser executada pelo Gestor Local autorizado da Gestão de origem.");
        }
        if (transferenciaGestaoContratoServico == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Módulo de transferência excepcional de Gestão indisponível.");
        }
    }

    public List<TransferenciaGestaoContratoDTO> listarTransferenciasGestao(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (transferenciaGestaoContratoServico == null) return List.of();
        return transferenciaGestaoContratoServico.listar(id);
    }

    public List<EventoCicloVidaContratoDTO> listarEventos(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);

        if (eventoCicloVidaRepositorio == null) return List.of();

        return eventoCicloVidaRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(id)
                .stream()
                .map(this::paraEventoDTO)
                .toList();
    }

    public List<RelacaoPrecedenciaEventoContratoDTO> listarRelacoesPrecedencia(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (motorTemporalContratoServico == null) return List.of();
        return motorTemporalContratoServico.relacoesConhecidas(id, null).stream()
                .map(motorTemporalContratoServico::paraRelacaoDTO)
                .toList();
    }

    public List<VersaoContratoNegocialDTO> listarVersoesNegociais(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (motorTemporalContratoServico == null) return List.of();
        return motorTemporalContratoServico.listarVersoes(id);
    }

    public List<VersaoContratoLeituraDTO> listarVersoesNegociaisLeitura(Long id,Instant conhecidoAte) {
        UsuarioAplicacao user=acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato=obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user,contrato);
        if(motorTemporalContratoServico==null)return List.of();
        return motorTemporalContratoServico.listarVersoesLeitura(id,conhecidoAte);
    }

    public PosicaoTemporalContratoLeituraDTO consultarPosicaoTemporalLeitura(Long id,LocalDate data,Instant conhecidoAte) {
        UsuarioAplicacao user=acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato=obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user,contrato);
        if(motorTemporalContratoServico==null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Motor temporal contratual indisponível.");
        var base=motorTemporalContratoServico.reconstruirPosicao(contrato,data,conhecidoAte);
        VersaoContratoLeituraDTO leitura=base.versaoContratual()==null?null
                :aditamentoContratualServico!=null&&base.estadoNegocial()!=EstadoNegocialContrato.RASCUNHO
                    ?aditamentoContratualServico.projetarLeituraContrato(contrato,base.dataReferencia(),base.conhecidoAte())
                    :new VersaoContratoLeituraDTO(base.versaoContratual(),motorTemporalContratoServico.provasDaFotografia(base.versaoContratual()));
        Map<String,ProvaCampoHistoricoDTO> provasVersao=leitura==null?new LinkedHashMap<>():new LinkedHashMap<>(leitura.provaCampos());
        var original=motorTemporalContratoServico.fotografiaOriginalConhecida(contrato,base.conhecidoAte());
        var estado=original.filter(v->v.getEventoOrigem()!=null
                &&v.getEventoOrigem().getTipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO)
                .map(v->ProvasCamposHistoricos.comprovado(base.estadoNegocial(),
                ProvasCamposHistoricos.versao(motorTemporalContratoServico.paraVersaoDTO(v)))).orElseGet(ProvasCamposHistoricos::desconhecido);
        for(var e:base.eventosAplicados()) {
            var fonte=new OrigemProvaCampoDTO(OrigemProvaCampoDTO.Tipo.EVENTO_CONTRATUAL,null,e.id(),null,null,null,null,
                    e.dataEfetiva(),e.registradoEm());
            if(e.tipoEvento()==TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO)
                provasVersao.put("dataFim",ProvasCamposHistoricos.comprovado(e.dataEfetiva(),fonte));
            if(e.tipoEvento()==TipoEventoCicloVidaContrato.PUBLICACAO||e.tipoEvento()==TipoEventoCicloVidaContrato.ANULACAO
                    ||e.tipoEvento()==TipoEventoCicloVidaContrato.CANCELAMENTO_CONTRATO_FUTURO)
                estado=ProvasCamposHistoricos.comprovado(base.estadoNegocial(),fonte);
        }
        if(leitura!=null)leitura=new VersaoContratoLeituraDTO(leitura.dados().comVigencia(base.vigenciaInicio(),base.vigenciaFim()),provasVersao);
        var inicio=provasVersao.getOrDefault("dataInicio",ProvasCamposHistoricos.desconhecido());
        var fim=provasVersao.getOrDefault("dataFim",ProvasCamposHistoricos.desconhecido());
        var situacao=switch(base.situacaoTemporal()==null?"":base.situacaoTemporal().name()) {
            case "CANCELADO","ANULADO" -> ProvasCamposHistoricos.derivado(base.situacaoTemporal(),estado);
            case "FUTURO" -> ProvasCamposHistoricos.derivado(base.situacaoTemporal(),estado,inicio);
            case "ENCERRADO" -> ProvasCamposHistoricos.derivado(base.situacaoTemporal(),estado,fim);
            case "VIGENTE" -> ProvasCamposHistoricos.derivado(base.situacaoTemporal(),estado,inicio,fim);
            default -> ProvasCamposHistoricos.desconhecido();
        };
        return new PosicaoTemporalContratoLeituraDTO(base.contratoId(),base.dataReferencia(),base.conhecidoAte(),
                base.estadoNegocial(),base.situacaoTemporal(),base.habilitacaoAdministrativa(),base.vigenciaInicio(),base.vigenciaFim(),
                leitura,base.eventosAplicados(),base.relacoesConsideradas(),Map.of("estadoNegocial",estado,
                        "situacaoTemporal",situacao,"habilitacaoAdministrativa",provasVersao.getOrDefault("habilitacaoAdministrativa",ProvasCamposHistoricos.desconhecido()),
                        "vigenciaInicio",inicio,"vigenciaFim",fim));
    }

    public PosicaoTemporalContratoDTO consultarPosicaoTemporal(
            Long id,
            LocalDate dataReferencia,
            Instant conhecidoAte
    ) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (motorTemporalContratoServico == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Motor temporal contratual indisponível.");
        }
        PosicaoTemporalContratoDTO base = motorTemporalContratoServico.reconstruirPosicao(contrato, dataReferencia, conhecidoAte);
        if (aditamentoContratualServico == null || base.estadoNegocial() == EstadoNegocialContrato.RASCUNHO
                || base.versaoContratual() == null) {
            return base;
        }

        VersaoContratoNegocialDTO versao = aditamentoContratualServico.projetarVersaoContrato(
                contrato, base.dataReferencia(), base.conhecidoAte(), Set.of())
                .comVigencia(base.vigenciaInicio(), base.vigenciaFim());
        return new PosicaoTemporalContratoDTO(
                base.contratoId(), base.dataReferencia(), base.conhecidoAte(), base.estadoNegocial(), base.situacaoTemporal(),
                base.habilitacaoAdministrativa(), base.vigenciaInicio(), base.vigenciaFim(), versao,
                base.eventosAplicados(), base.relacoesConsideradas());
    }

    private SituacaoTemporalContrato situacaoTemporalReconstruida(
            EstadoNegocialContrato estado, LocalDate inicio, LocalDate fim, LocalDate data) {
        if (estado == EstadoNegocialContrato.RASCUNHO) return SituacaoTemporalContrato.RASCUNHO;
        if (estado == EstadoNegocialContrato.CANCELADO) return SituacaoTemporalContrato.CANCELADO;
        if (estado == EstadoNegocialContrato.ANULADO) return SituacaoTemporalContrato.ANULADO;
        if (inicio != null && data.isBefore(inicio)) return SituacaoTemporalContrato.FUTURO;
        if (fim != null && data.isAfter(fim)) return SituacaoTemporalContrato.ENCERRADO;
        if (inicio == null || fim == null) return null;
        return SituacaoTemporalContrato.VIGENTE;
    }

    /**
     * Retorna a habilitação administrativa para uma data sem usar a situação
     * temporal como substituta. Eventos futuros não alteram o valor atual antes
     * da data efetiva.
     */
    public HabilitacaoAdministrativaContrato habilitacaoAdministrativaEm(
            Contrato contrato,
            LocalDate dataReferencia
    ) {
        if (contrato == null) return HabilitacaoAdministrativaContrato.INATIVO;
        if (contrato.getId() == null) {
            return contrato.getHabilitacaoAdministrativa();
        }

        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        return motorTemporalContratoServico.habilitacaoAdministrativaEm(contrato, data, Instant.now());
    }

    private void revalidarFrequenciasContrato(Contrato contrato, LocalDate desde, String motivo) {
        if (revalidacaoFrequenciaTemporalServico != null && contrato != null
                && contrato.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorContrato(contrato.getId(), desde, motivo);
        }
    }

    private Contrato obterContrato(Long id) {
        return contratoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterContratoPublicadoNoEscopo(Long id, UsuarioAplicacao user) {
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A operação de ciclo de vida exige contrato publicado."
            );
        }
        return contrato;
    }

    private void validarConfirmacaoCicloVida(Contrato contrato, String operacao,
                                            AcaoCicloVidaContratoDTO acao, UsuarioAplicacao usuario) {
        if (previaCicloVidaContratoServico != null) {
            previaCicloVidaContratoServico.validarConfirmacao(contrato, operacao, acao, usuario);
        }
    }

    private GestaoLocal resolverGestaoLocalParaEscrita(
            UsuarioAplicacao user,
            Long requestedGestaoLocalId,
            GestaoLocal currentGestaoLocal
    ) {
        if (acessoOrganizacionalServico.ehGestaoLocal(user)) {
            GestaoLocal own = user.getGestaoLocal();
            if (own == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Usuário Gestão Local sem vínculo com uma Gestão Local."
                );
            }
            if (requestedGestaoLocalId != null && !Objects.equals(requestedGestaoLocalId, own.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "O contrato deve permanecer na sua Gestão Local."
                );
            }
            validarGestaoLocalAtiva(own);
            return own;
        }

        Long target = requestedGestaoLocalId != null
                ? requestedGestaoLocalId
                : currentGestaoLocal != null ? currentGestaoLocal.getId() : null;

        if (target == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecione a Gestão Local responsável pelo contrato."
            );
        }

        GestaoLocal gestaoLocal = gestaoLocalRepositorio.findById(target)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Gestão Local informada não encontrada."
                ));

        validarGestaoLocalAtiva(gestaoLocal);
        return gestaoLocal;
    }

    private void validarGestaoLocalAtiva(GestaoLocal gestaoLocal) {
        if (gestaoLocal == null
                || Boolean.FALSE.equals(gestaoLocal.getAtivo())
                || gestaoLocal.getSuperintendencia() == null
                || Boolean.FALSE.equals(gestaoLocal.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A Gestão Local e seu gestor estadual precisam estar ativos."
            );
        }
    }

    private void validarRascunho(Contrato contrato, Long idAtual) {
        if (contrato.getAnoContrato() != null
                && (contrato.getAnoContrato() < 1900 || contrato.getAnoContrato() > 9999)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um ano de contrato válido.");
        }

        if (contrato.getDataInicio() != null
                && contrato.getDataFim() != null
                && contrato.getDataFim().isBefore(contrato.getDataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data final não pode ser anterior à data inicial."
            );
        }

        if ((contrato.getDiaSemanaReduzido() == null) != (contrato.getHorasDiaReduzido() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dia da semana reduzido e quantidade de horas devem ser informados juntos."
            );
        }

        if (contrato.getHorasDiaReduzido() != null
                && (contrato.getHorasDiaReduzido() < 1 || contrato.getHorasDiaReduzido() > 24)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "As horas do dia reduzido devem estar entre 1 e 24."
            );
        }

        validarUnicidadeNumeroAno(contrato, idAtual);
    }

    private void validarUnicidadeNumeroAno(Contrato contrato, Long idAtual) {
        identidadeContratualServico.validarDisponibilidade(contrato.getNumeroContrato(), contrato.getAnoContrato(), idAtual);
    }

    private void mapearDTORascunho(ContratoDTO dto, Contrato entidade) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do contrato não informados.");
        }

        entidade.setNumeroContrato(limpar(dto.numeroContrato()));
        entidade.setAnoContrato(dto.anoContrato());
        entidade.setDescricao(limpar(dto.descricao()));
        entidade.setDataInicio(dto.dataInicio());
        entidade.setDataFim(dto.dataFim());

        if (dto.ativo() != null) entidade.setAtivo(dto.ativo());

        entidade.setDiaSemanaReduzido(dto.diaSemanaReduzido());
        entidade.setHorasDiaReduzido(dto.horasDiaReduzido());
        entidade.setEfetivoPrevisto(dto.efetivoPrevisto() == null ? 0 : Math.max(0, dto.efetivoPrevisto()));
        entidade.setMatriculaVinculoObrigatoria(dto.matriculaVinculoObrigatoria());

        resolverTipoContratacao(dto, entidade);
        resolverEmpresa(dto, entidade);
    }

    private void resolverTipoContratacao(ContratoDTO dto, Contrato entidade) {
        if (dto.tipoContratacaoId() != null) {
            TipoContratacao tipo = tipoContratacaoRepositorio.findById(dto.tipoContratacaoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Tipo de Contratação não encontrado."
                    ));

            GestaoLocal gestaoTipo = tipo.getGestaoLocal();
            GestaoLocal gestaoContrato = entidade.getGestaoLocal();
            if (gestaoContrato == null || gestaoTipo == null
                    || !Objects.equals(gestaoTipo.getId(), gestaoContrato.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de Contratação não pertence à Gestão Local do contrato."
                );
            }
            if (!tipoDisponivelEm(tipo, entidade.getDataInicio())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de Contratação não está disponível na data inicial do contrato."
                );
            }

            entidade.setTipoContratacao(tipo);
            entidade.setTipo(tipo.getDescricao());
            return;
        }

        if (dto.tipo() != null && !dto.tipo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de Contratação deve ser informado pelo identificador do cadastro próprio.");
        }
        entidade.setTipo(null);
        entidade.setTipoContratacao(null);
    }

    private boolean tipoDisponivelEm(TipoContratacao tipo, LocalDate data) {
        if (tipo == null) return false;
        return disponibilidadeCatalogo == null ? Boolean.TRUE.equals(tipo.getAtivo())
                : disponibilidadeCatalogo.ativoEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.TIPO_CONTRATACAO,
                        tipo.getId(), data == null ? RelogioSistema.hoje() : data, tipo.getAtivo());
    }

    private void resolverEmpresa(ContratoDTO dto, Contrato entidade) {
        Fornecedor fornecedor = null;

        if (dto.fornecedorId() != null) {
            fornecedor = fornecedorRepositorio.findById(dto.fornecedorId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Empresa/fornecedor informado não encontrado."
                    ));
            entidade.setFornecedor(fornecedor);
        } else if (entidade.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO) {
            entidade.setFornecedor(null);
        }

        String cnpj = limpar(dto.cnpjEmpresa());
        String razao = limpar(dto.razaoSocialEmpresa());
        String fantasia = limpar(dto.nomeFantasiaEmpresa());

        if (cnpj == null && fornecedor != null) cnpj = fornecedor.getCnpj();
        if (razao == null && fornecedor != null) razao = fornecedor.getRazaoSocial();

        if (cnpj != null) {
            cnpj = cnpj.replaceAll("\\D", "");
            if (!cnpj.isBlank() && cnpj.length() != 14) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "CNPJ da empresa deve conter 14 dígitos."
                );
            }
        }

        // Valida somente a identidade escrita no rascunho. Nomes documentais
        // e fatos publicados não são reinterpretados pelo cadastro mestre atual.
        if (fornecedor != null && cnpj != null && !Objects.equals(cnpj, fornecedor.getCnpj())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O CNPJ informado no contrato deve corresponder ao fornecedor escolhido.");
        }

        entidade.setCnpjEmpresa(cnpj);
        entidade.setRazaoSocialEmpresa(razao);
        entidade.setNomeFantasiaEmpresa(fantasia);
    }

    private void validarAcaoComMotivo(AcaoCicloVidaContratoDTO acao) {
        if (acao == null || acao.dataEfetiva() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a data efetiva da operação.");
        }
        if (acao.motivo() == null || acao.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo/justificativa da operação.");
        }
    }

    private EventoCicloVidaContrato registrarEvento(
            Contrato contrato,
            TipoEventoCicloVidaContrato tipo,
            AcaoCicloVidaContratoDTO acao,
            UsuarioAplicacao usuario,
            String valorAnterior,
            String valorNovo
    ) {
        return motorTemporalContratoServico.registrarEvento(
                contrato, tipo, acao, usuario, valorAnterior, valorNovo
        );
    }

    private Map<Long, Long> efetivosOperacionaisAtuais(List<Contrato> contratos) {
        if (vinculoContratualServico == null || contratos.isEmpty()) return Map.of();
        Set<Long> ids = contratos.stream().map(Contrato::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        return vinculoContratualServico.contarOperacionaisPorContrato(ids, RelogioSistema.hoje());
    }

    private LeituraHistoricaContratos carregarLeitura(List<Contrato> contratos) {
        if (contratos.isEmpty() || motorTemporalContratoServico == null) return null;
        return motorTemporalContratoServico.carregarLeitura(contratos.stream().map(Contrato::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    private Map<Long, HabilitacaoAdministrativaContrato> habilitacoesAdministrativasAtuais(
            List<Contrato> contratos, LeituraHistoricaContratos entradas) {
        Map<Long, HabilitacaoAdministrativaContrato> resultado = new HashMap<>();
        if (contratos == null) return resultado;
        LocalDate hoje = RelogioSistema.hoje();
        for (Contrato contrato : contratos) {
            if (contrato != null && contrato.getId() != null) {
                resultado.put(contrato.getId(), entradas == null ? habilitacaoAdministrativaEm(contrato, hoje)
                        : motorTemporalContratoServico.habilitacaoAdministrativaEm(contrato, hoje, Instant.now(), Set.of(), entradas));
            }
        }
        return resultado;
    }

    private ContratoDTO paraDTO(Contrato entidade) {
        return paraDTO(entidade, habilitacaoAdministrativaEm(entidade, RelogioSistema.hoje()));
    }

    private ContratoDTO paraDTO(
            Contrato entidade,
            HabilitacaoAdministrativaContrato habilitacao
    ) {
        long efetivoAtual = entidade.getId() == null || vinculoContratualServico == null
                ? 0
                : vinculoContratualServico.contarOperacionaisNoContrato(entidade.getId(), RelogioSistema.hoje());
        return paraDTO(entidade, habilitacao, efetivoAtual);
    }

    private ContratoDTO paraDTO(Contrato entidade, HabilitacaoAdministrativaContrato habilitacao, long efetivoAtual) {
        return paraDTO(entidade, habilitacao, efetivoAtual, null);
    }

    private ContratoDTO paraDTO(Contrato entidade, HabilitacaoAdministrativaContrato habilitacao, long efetivoAtual,
                                LeituraHistoricaContratos entradas) {
        GestaoLocal gestaoLocal = entradas != null && gestaoContratoTemporal != null
                ? gestaoContratoTemporal.gestaoAtual(entidade, entradas) : gestaoAtual(entidade);
        SuperintendenciaEstadual se = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;

        VersaoContratoNegocialDTO foto = entidade.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO
                && aditamentoContratualServico != null
                ? (entradas == null ? aditamentoContratualServico.projetarVersaoContrato(entidade, RelogioSistema.hoje(), Instant.now(), Set.of())
                    : aditamentoContratualServico.projetarVersaoContrato(entidade, RelogioSistema.hoje(), Instant.now(), Set.of(), entradas))
                : null;
        LocalDate inicioAtual = foto == null ? entidade.getDataInicio() : foto.dataInicio();
        LocalDate fimAtual = foto == null ? entidade.getDataFim() : foto.dataFim();
        if (foto != null && motorTemporalContratoServico != null) {
            for (EventoCicloVidaContrato evento : entradas == null
                    ? motorTemporalContratoServico.eventosValidosOrdenados(entidade.getId(), RelogioSistema.hoje(), Instant.now())
                    : motorTemporalContratoServico.eventosValidosOrdenados(entidade.getId(), RelogioSistema.hoje(), Instant.now(), entradas)) {
                if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO) fimAtual = evento.getDataEfetiva();
            }
        }
        int previsto = Optional.ofNullable(foto == null ? entidade.getEfetivoPrevisto() : foto.efetivoPrevisto()).orElse(0);
        long saldo = previsto - efetivoAtual;
        double percentual = previsto > 0
                ? Math.round((efetivoAtual * 10000.0 / previsto)) / 100.0
                : 0.0;

        TipoContratacao tipoContratacao = entidade.getTipoContratacao();
        Fornecedor fornecedor = entidade.getFornecedor();
        boolean ativoAdministrativo = habilitacao == HabilitacaoAdministrativaContrato.ATIVO;
        EstadoNegocialContrato estadoAtual = motorTemporalContratoServico == null ? entidade.getEstadoNegocial()
                : entradas == null ? motorTemporalContratoServico.estadoNegocialEm(entidade, RelogioSistema.hoje(), Instant.now())
                    : motorTemporalContratoServico.estadoNegocialEm(entidade, RelogioSistema.hoje(), Instant.now(), entradas);
        if (estadoAtual == null) estadoAtual = entidade.getEstadoNegocial();

        return new ContratoDTO(
                entidade.getId(),
                foto == null ? entidade.getNumeroContrato() : foto.numeroContrato(),
                foto == null ? entidade.getDescricao() : foto.descricao(),
                foto == null ? entidade.getTipo() : foto.tipoContratacaoDescricao(),
                inicioAtual,
                fimAtual,
                ativoAdministrativo,
                foto == null ? entidade.getDiaSemanaReduzido() : foto.diaSemanaReduzido(),
                foto == null ? entidade.getHorasDiaReduzido() : foto.horasDiaReduzido(),
                gestaoLocal != null ? gestaoLocal.getId() : null,
                gestaoLocal != null ? gestaoLocal.getNome() : null,
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                se != null ? se.getUf() : null,
                previsto,
                efetivoAtual,
                saldo,
                percentual,
                foto == null ? entidade.getAnoContrato() : foto.anoContrato(),
                foto == null ? tipoContratacao != null ? tipoContratacao.getId() : null : foto.tipoContratacaoId(),
                // A descrição exibida no contrato é a fotografia negocial,
                // não o nome atual do cadastro mestre.
                foto == null ? entidade.getTipo() : foto.tipoContratacaoDescricao(),
                foto == null ? fornecedor != null ? fornecedor.getId() : null : foto.fornecedorId(),
                foto == null ? primeiroNaoVazio(entidade.getCnpjEmpresa(), fornecedor != null ? fornecedor.getCnpj() : null) : foto.cnpjEmpresa(),
                foto == null ? primeiroNaoVazio(entidade.getRazaoSocialEmpresa(), fornecedor != null ? fornecedor.getRazaoSocial() : null) : foto.razaoSocialEmpresa(),
                foto == null ? entidade.getNomeFantasiaEmpresa() : foto.nomeFantasiaEmpresa(),
                estadoAtual,
                situacaoTemporalReconstruida(estadoAtual, inicioAtual, fimAtual, RelogioSistema.hoje()),
                habilitacao,
                entidade.getEfetivoOriginalPublicado(),
                foto == null ? entidade.getMatriculaVinculoObrigatoria() : foto.matriculaVinculoObrigatoria(),
                entidade.getPublicadoEm(),
                entidade.getPublicadoPorId(),
                entidade.getPublicadoPorUsername(),
                entidade.getReferenciaSeiPublicacao(),
                entidade.getVersion()
        );
    }

    private EventoCicloVidaContratoDTO paraEventoDTO(EventoCicloVidaContrato evento) {
        return motorTemporalContratoServico.paraEventoDTO(evento);
    }

    private static String limpar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isBlank() ? null : limpo;
    }

    private static String primeiroNaoVazio(String primeiro, String segundo) {
        if (primeiro != null && !primeiro.isBlank()) return primeiro;
        return segundo;
    }
}
