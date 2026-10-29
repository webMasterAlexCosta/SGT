package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.NomesCache;
import com.example.siggtrat.dto.AcaoCicloVidaContratoDTO;
import com.example.siggtrat.dto.ContratoDTO;
import com.example.siggtrat.dto.ContratoOpcaoDTO;
import com.example.siggtrat.dto.EventoCicloVidaContratoDTO;
import com.example.siggtrat.dto.PublicacaoContratoDTO;
import com.example.siggtrat.dto.PosicaoTemporalContratoDTO;
import com.example.siggtrat.dto.RelacaoPrecedenciaEventoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoDTO;
import com.example.siggtrat.dto.TransferenciaGestaoContratoRequisicao;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
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
    private AditamentoContratualServico aditamentoContratualServico;
    private VinculoContratualServico vinculoContratualServico;
    private TransferenciaGestaoContratoServico transferenciaGestaoContratoServico;
    private AlocacaoVinculoServico alocacaoVinculoServico;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;

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
            MotorTemporalContratoServico motorTemporalContratoServico
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

    @Cacheable(
            cacheNames = NomesCache.CONTRATOS,
            key = "@redisCacheKey.usuarioAtual()",
            sync = true
    )
    public List<ContratoDTO> listarTodos() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaContrato(user);

        List<Contrato> contratos;
        if (acessoOrganizacionalServico.ehGlobal(user) || acessoOrganizacionalServico.ehPresidente(user)) {
            contratos = contratoRepositorio.findAllByOrderByNumeroContratoAsc();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            contratos = contratoRepositorio.findByGestaoLocalSuperintendenciaIdOrderByNumeroContratoAsc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(user).getId()
            );
        } else if (user.getGestaoLocal() != null) {
            contratos = contratoRepositorio.findByGestaoLocalIdOrderByNumeroContratoAsc(user.getGestaoLocal().getId());
        } else {
            contratos = List.of();
        }

        Map<Long, HabilitacaoAdministrativaContrato> habilitacoes =
                habilitacoesAdministrativasAtuais(contratos);

        return contratos.stream()
                .map(contrato -> paraDTO(
                        contrato,
                        habilitacoes.getOrDefault(
                                contrato.getId(),
                                contrato.getHabilitacaoAdministrativa()
                        )
                ))
                .toList();
    }

    /**
     * Lista leve para filtros com a quantidade total atual de colaboradores por
     * contrato. A contagem é feita em uma única consulta agregada, sem N+1.
     */
    @Cacheable(
            cacheNames = NomesCache.CONTRATOS_OPCOES,
            key = "@redisCacheKey.usuarioAtual()",
            sync = true
    )
    public List<ContratoOpcaoDTO> obterOpcoes() {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirConsultaContrato(user);

        List<Contrato> contratos;
        if (acessoOrganizacionalServico.ehGlobal(user) || acessoOrganizacionalServico.ehPresidente(user)) {
            contratos = contratoRepositorio.findAllByOrderByNumeroContratoAsc();
        } else if (acessoOrganizacionalServico.ehGestorEstadual(user)
                && acessoOrganizacionalServico.superintendenciaEfetiva(user) != null) {
            contratos = contratoRepositorio.findByGestaoLocalSuperintendenciaIdOrderByNumeroContratoAsc(
                    acessoOrganizacionalServico.superintendenciaEfetiva(user).getId()
            );
        } else if (user.getGestaoLocal() != null) {
            contratos = contratoRepositorio.findByGestaoLocalIdOrderByNumeroContratoAsc(user.getGestaoLocal().getId());
        } else {
            contratos = List.of();
        }

        if (contratos.isEmpty()) return List.of();

        Set<Long> idsPermitidos = contratos.stream()
                .map(Contrato::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Long> quantidadePorContrato = new HashMap<>();
        if (vinculoContratualServico != null) {
            for (Long contratoId : idsPermitidos) {
                quantidadePorContrato.put(contratoId,
                        vinculoContratualServico.contarOperacionaisNoContrato(contratoId, RelogioSistema.hoje()));
            }
        }

        Map<Long, HabilitacaoAdministrativaContrato> habilitacoes =
                habilitacoesAdministrativasAtuais(contratos);

        return contratos.stream()
                .map(c -> new ContratoOpcaoDTO(
                        c.getId(),
                        c.getNumeroContrato(),
                        c.getDescricao(),
                        habilitacoes.getOrDefault(c.getId(), c.getHabilitacaoAdministrativa())
                                == HabilitacaoAdministrativaContrato.ATIVO,
                        quantidadePorContrato.getOrDefault(c.getId(), 0L)
                ))
                .toList();
    }

    @Cacheable(
            cacheNames = NomesCache.CONTRATO_POR_ID,
            key = "@redisCacheKey.usuarioAtual() + ':' + #p0",
            sync = true
    )
    public ContratoDTO buscarPorId(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        return paraDTO(contrato);
    }

    /** Cria um Contrato em estado RASCUNHO. */
    @Transactional
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

        validarRascunho(contrato, null);
        return paraDTO(contratoRepositorio.save(contrato));
    }

    /**
     * Depois de publicado o contrato não volta a ser formulário livre.
     */
    @Transactional
    public ContratoDTO atualizar(Long id, ContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContrato(id);
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
    @Transactional
    public ContratoDTO publicar(Long id, PublicacaoContratoDTO dto) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContrato(id);
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

        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setEfetivoOriginalPublicado(contrato.getEfetivoPrevisto());
        contrato.setReferenciaSeiPublicacao(limpar(dto.referenciaSei()));
        contrato.setPublicadoEm(java.time.Instant.now());
        contrato.setPublicadoPorId(user != null ? user.getId() : null);
        contrato.setPublicadoPorUsername(user != null ? user.getUsername() : null);

        Contrato salvo = contratoRepositorio.save(contrato);
        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                contrato.getDataInicio(),
                dto.dataPublicacao() != null ? dto.dataPublicacao() : RelogioSistema.hoje(),
                dto.justificativa() == null || dto.justificativa().isBlank() ? "Publicação do contrato" : dto.justificativa(),
                dto.referenciaSei()
        );
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

    private void validarComposicoesParaPublicacao(
            Contrato contrato,
            List<ComposicaoContratual> composicoes
    ) {
        if (composicoes == null || composicoes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato deve possuir ao menos uma composição para ser publicado.");
        }
        int soma = composicoes.stream()
                .mapToInt(c -> Optional.ofNullable(c.getEfetivoPrevisto()).orElse(0))
                .sum();
        if (soma != Optional.ofNullable(contrato.getEfetivoPrevisto()).orElse(0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A soma do efetivo das composições (" + soma + ") deve ser igual ao efetivo total do contrato ("
                            + contrato.getEfetivoPrevisto() + ").");
        }

        Set<String> chaves = new HashSet<>();
        for (ComposicaoContratual c : composicoes) {
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
        if (contrato.getTipoContratacao() == null || !Boolean.TRUE.equals(contrato.getTipoContratacao().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selecione um Tipo de Contratação ativo antes da publicação.");
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
    @Transactional
    public void excluir(Long id) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContrato(id);
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
    @Transactional
    public ContratoDTO inativarAdministrativamente(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        LocalDate dataEfetiva = acao.dataEfetiva();
        if (habilitacaoAdministrativaEm(contrato, dataEfetiva)
                == HabilitacaoAdministrativaContrato.INATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato já está administrativamente inativo na data informada.");
        }

        registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA,
                acao,
                user,
                "ATIVO",
                "INATIVO"
        );

        if (!dataEfetiva.isAfter(RelogioSistema.hoje())) {
            HabilitacaoAdministrativaContrato atual = habilitacaoAdministrativaEm(contrato, RelogioSistema.hoje());
            contrato.setAtivo(atual == HabilitacaoAdministrativaContrato.ATIVO);
            contratoRepositorio.save(contrato);
        }
        revalidarFrequenciasContrato(contrato, dataEfetiva,
                "Inativação administrativa do Contrato alterou a cobertura operacional");

        return paraDTO(contrato);
    }

    /**
     * Reativação é um novo evento; não apaga a inativação anterior.
     */
    @Transactional
    public ContratoDTO reativarAdministrativamente(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);

        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        validarAcaoComMotivo(acao);

        if (habilitacaoAdministrativaEm(contrato, acao.dataEfetiva())
                == HabilitacaoAdministrativaContrato.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato já está administrativamente ativo na data informada.");
        }

        if (alocacaoVinculoServico != null) {
            alocacaoVinculoServico.validarRetomadaOperacional(contrato, acao.dataEfetiva());
        }

        registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA,
                acao,
                user,
                "INATIVO",
                "ATIVO"
        );

        if (!acao.dataEfetiva().isAfter(RelogioSistema.hoje())) {
            HabilitacaoAdministrativaContrato atual = habilitacaoAdministrativaEm(contrato, RelogioSistema.hoje());
            contrato.setAtivo(atual == HabilitacaoAdministrativaContrato.ATIVO);
            contratoRepositorio.save(contrato);
        }
        revalidarFrequenciasContrato(contrato, acao.dataEfetiva(),
                "Reativação administrativa retroativa do Contrato exige revalidação das frequências");

        return paraDTO(contrato);
    }

    /**
     * Encerramento antecipado altera a data final efetiva e encerra os Vínculos
     * ainda abertos por meio de evento temporal explícito.
     */
    @Transactional
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
        if (contrato.getDataInicio() == null || contrato.getDataFim() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contrato publicado sem vigência completa deve ser regularizado antes do encerramento."
            );
        }
        if (acao.dataEfetiva().isBefore(contrato.getDataInicio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A data efetiva do encerramento não pode ser anterior ao início do contrato."
            );
        }
        if (!acao.dataEfetiva().isBefore(contrato.getDataFim())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Encerramento antecipado deve reduzir a data final vigente do contrato."
            );
        }

        LocalDate dataFimAnterior = contrato.getDataFim();

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
    @Transactional
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
    @Transactional
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
        if (contrato.getEstadoNegocial() == EstadoNegocialContrato.ANULADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contrato já está anulado.");
        }

        EventoCicloVidaContrato eventoAnulacao = registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.ANULACAO,
                acao,
                user,
                contrato.getEstadoNegocial().name(),
                EstadoNegocialContrato.ANULADO.name()
        );

        contrato.setEstadoNegocial(EstadoNegocialContrato.ANULADO);
        contrato.setAtivo(false);
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
    @Transactional
    public ContratoDTO corrigirAnulacao(Long id, AcaoCicloVidaContratoDTO acao) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        validarAcaoComMotivo(acao);
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.ANULADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente contrato atualmente ANULADO admite correção de anulação.");
        }
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
        if (!Objects.equals(acao.dataEfetiva(), anulacao.getDataEfetiva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A correção deve possuir a mesma data efetiva da anulação cancelada.");
        }
        if (acao.referenciaSei() == null || acao.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Correção de anulação exige referência documental/SEI.");
        }

        registrarEvento(contrato, TipoEventoCicloVidaContrato.CORRECAO_ANULACAO, acao, user,
                EstadoNegocialContrato.ANULADO.name(), EstadoNegocialContrato.PUBLICADO.name());
        contrato.setEstadoNegocial(EstadoNegocialContrato.PUBLICADO);
        contrato.setAtivo(false);
        Contrato corrigido = contratoRepositorio.save(contrato);
        revalidarFrequenciasContrato(corrigido, acao.dataEfetiva(),
                "Correção auditada de anulação alterou a verdade temporal do Contrato");
        return paraDTO(corrigido);
    }

    /**
     * Operação excepcional: muda a responsabilidade atual do contrato somente
     * após preservar a fotografia anterior e validar os mapas para o destino.
     */
    @Transactional
    public ContratoDTO transferirGestao(Long id, TransferenciaGestaoContratoRequisicao req) {
        UsuarioAplicacao user = acessoOrganizacionalServico.usuarioAtual();
        acessoOrganizacionalServico.exigirGestaoContrato(user);
        Contrato contrato = obterContratoPublicadoNoEscopo(id, user);
        if (!acessoOrganizacionalServico.ehGestaoLocal(user) || user.getGestaoLocal() == null
                || !Objects.equals(user.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Transferência excepcional deve ser executada pelo Gestor Local autorizado da Gestão de origem.");
        }
        if (transferenciaGestaoContratoServico == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Módulo de transferência excepcional de Gestão indisponível.");
        }
        transferenciaGestaoContratoServico.transferir(contrato, req, user);
        return paraDTO(contratoRepositorio.save(contrato));
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
        if (aditamentoContratualServico == null || base.estadoNegocial() == EstadoNegocialContrato.RASCUNHO) {
            return base;
        }

        VersaoContratoNegocialDTO versao = aditamentoContratualServico.projetarVersaoContrato(
                contrato, base.dataReferencia(), base.conhecidoAte(), Set.of());
        LocalDate inicio = versao.dataInicio();
        LocalDate fim = versao.dataFim();
        // Eventos próprios do ciclo de vida prevalecem sobre aditamento.
        for (EventoCicloVidaContratoDTO evento : base.eventosAplicados()) {
            if (evento.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO) {
                fim = evento.dataEfetiva();
            }
        }
        SituacaoTemporalContrato situacao = situacaoTemporalReconstruida(base.estadoNegocial(), inicio, fim, base.dataReferencia());
        return new PosicaoTemporalContratoDTO(
                base.contratoId(), base.dataReferencia(), base.conhecidoAte(), base.estadoNegocial(), situacao,
                base.habilitacaoAdministrativa(), inicio, fim, versao, base.eventosAplicados(), base.relacoesConsideradas());
    }

    private SituacaoTemporalContrato situacaoTemporalReconstruida(
            EstadoNegocialContrato estado, LocalDate inicio, LocalDate fim, LocalDate data) {
        if (estado == EstadoNegocialContrato.RASCUNHO) return SituacaoTemporalContrato.RASCUNHO;
        if (estado == EstadoNegocialContrato.CANCELADO) return SituacaoTemporalContrato.CANCELADO;
        if (estado == EstadoNegocialContrato.ANULADO) return SituacaoTemporalContrato.ANULADO;
        if (inicio != null && data.isBefore(inicio)) return SituacaoTemporalContrato.FUTURO;
        if (fim != null && data.isAfter(fim)) return SituacaoTemporalContrato.ENCERRADO;
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
        List<EventoCicloVidaContrato> eventos = motorTemporalContratoServico
                .eventosValidosOrdenados(contrato.getId(), data, Instant.now());

        return resolverHabilitacaoPorEventos(contrato, eventos, data);
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
        Contrato contrato = obterContrato(id);
        acessoOrganizacionalServico.exigirAcessoContrato(user, contrato);
        if (contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A operação de ciclo de vida exige contrato publicado."
            );
        }
        return contrato;
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
        if (contrato.getNumeroContrato() == null || contrato.getNumeroContrato().isBlank()
                || contrato.getAnoContrato() == null) {
            return;
        }

        Optional<Contrato> duplicado =
                contratoRepositorio.findByNumeroContratoIgnoreCaseAndAnoContrato(
                        contrato.getNumeroContrato(),
                        contrato.getAnoContrato()
                );

        if (duplicado.isPresent() && !Objects.equals(duplicado.get().getId(), idAtual)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um contrato com este número e ano."
            );
        }
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
            if (!Boolean.TRUE.equals(tipo.getAtivo())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de Contratação inativo não pode ser usado em novo fato."
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

    private Map<Long, HabilitacaoAdministrativaContrato> habilitacoesAdministrativasAtuais(
            List<Contrato> contratos
    ) {
        if (contratos == null || contratos.isEmpty()) return Map.of();

        Map<Long, HabilitacaoAdministrativaContrato> resultado = new HashMap<>();
        for (Contrato contrato : contratos) {
            if (contrato != null && contrato.getId() != null) {
                resultado.put(contrato.getId(), contrato.getHabilitacaoAdministrativa());
            }
        }

        Set<Long> ids = contratos.stream()
                .filter(Objects::nonNull)
                .map(Contrato::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return resultado;

        Map<Long, List<EventoCicloVidaContrato>> porContrato = eventoCicloVidaRepositorio
                .findByContratoIdInOrderByContratoIdAscDataEfetivaAscRegistradoEmAsc(ids)
                .stream()
                .filter(evento -> evento.getContrato() != null && evento.getContrato().getId() != null)
                .collect(Collectors.groupingBy(evento -> evento.getContrato().getId()));

        LocalDate hoje = RelogioSistema.hoje();
        for (Contrato contrato : contratos) {
            if (contrato == null || contrato.getId() == null) continue;
            List<EventoCicloVidaContrato> eventos = porContrato.getOrDefault(contrato.getId(), List.of());
            resultado.put(
                    contrato.getId(),
                    resolverHabilitacaoPorEventos(contrato, eventos, hoje)
            );
        }
        return resultado;
    }

    private HabilitacaoAdministrativaContrato resolverHabilitacaoPorEventos(
            Contrato contrato,
            List<EventoCicloVidaContrato> eventos,
            LocalDate data
    ) {
        List<EventoCicloVidaContrato> administrativos = eventos == null
                ? List.of()
                : eventos.stream()
                .filter(evento -> evento.getTipoEvento() == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                        || evento.getTipoEvento() == TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA)
                .toList();

        if (administrativos.isEmpty()) {
            return contrato.getHabilitacaoAdministrativa();
        }

        HabilitacaoAdministrativaContrato resultado = HabilitacaoAdministrativaContrato.ATIVO;
        for (EventoCicloVidaContrato evento : administrativos) {
            if (evento.getDataEfetiva() != null && evento.getDataEfetiva().isAfter(data)) break;
            resultado = evento.getTipoEvento() == TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                    ? HabilitacaoAdministrativaContrato.INATIVO
                    : HabilitacaoAdministrativaContrato.ATIVO;
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
        GestaoLocal gestaoLocal = entidade.getGestaoLocal();
        SuperintendenciaEstadual se = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;

        long efetivoAtual = entidade.getId() == null || vinculoContratualServico == null
                ? 0
                : vinculoContratualServico.contarOperacionaisNoContrato(entidade.getId(), RelogioSistema.hoje());

        int previsto = entidade.getEfetivoPrevisto() == null ? 0 : entidade.getEfetivoPrevisto();
        long saldo = previsto - efetivoAtual;
        double percentual = previsto > 0
                ? Math.round((efetivoAtual * 10000.0 / previsto)) / 100.0
                : 0.0;

        TipoContratacao tipoContratacao = entidade.getTipoContratacao();
        Fornecedor fornecedor = entidade.getFornecedor();
        boolean ativoAdministrativo = habilitacao == HabilitacaoAdministrativaContrato.ATIVO;

        return new ContratoDTO(
                entidade.getId(),
                null,
                entidade.getNumeroContrato(),
                entidade.getDescricao(),
                entidade.getTipo(),
                entidade.getDataInicio(),
                entidade.getDataFim(),
                ativoAdministrativo,
                entidade.getDiaSemanaReduzido(),
                entidade.getHorasDiaReduzido(),
                gestaoLocal != null ? gestaoLocal.getId() : null,
                gestaoLocal != null ? gestaoLocal.getNome() : null,
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                se != null ? se.getUf() : null,
                previsto,
                efetivoAtual,
                saldo,
                percentual,
                entidade.getAnoContrato(),
                tipoContratacao != null ? tipoContratacao.getId() : null,
                // A descrição exibida no contrato é a fotografia negocial,
                // não o nome atual do cadastro mestre.
                entidade.getTipo(),
                fornecedor != null ? fornecedor.getId() : null,
                primeiroNaoVazio(entidade.getCnpjEmpresa(), fornecedor != null ? fornecedor.getCnpj() : null),
                primeiroNaoVazio(entidade.getRazaoSocialEmpresa(), fornecedor != null ? fornecedor.getRazaoSocial() : null),
                entidade.getNomeFantasiaEmpresa(),
                entidade.getEstadoNegocial(),
                entidade.situacaoTemporalEm(RelogioSistema.hoje()),
                habilitacao,
                entidade.getEfetivoOriginalPublicado(),
                entidade.getMatriculaVinculoObrigatoria(),
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
