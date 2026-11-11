package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.regra.RegraEfetivoContratual;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.regra.RegraDisponibilidadeCatalogo;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AditamentoContratualServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(AditamentoContratualServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private record ItemPreparado(
            ComposicaoContratual composicaoBase,
            Lotacao lotacao,
            Jornada jornada,
            Cargo cargo,
            int anterior,
            int resultante,
            long colaboradoresAtuais,
            String lotacaoMunicipio,
            String jornadaSemanal
    ) {
        int variacao() { return resultante - anterior; }
        String chave() {
            return ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                    lotacao.getId(), jornada.getId(), cargo.getId());
        }
    }

    private record Preparacao(
            List<ItemPreparado> itens,
            int efetivoAnterior,
            int efetivoResultante,
            int efetivoOriginal,
            int pisoNormal,
            boolean reducao,
            boolean abaixoPiso,
            boolean impactoOcupacao,
            Set<Long> eventosIgnorados,
            List<String> conflitosVigencia,
            List<String> conflitosMatricula,
            List<String> alertas
    ) {}

    private record ProjecaoNegocial(
            VersaoContratoNegocialDTO versao,
            boolean politicaContratoConhecida,
            boolean configuracaoDiaReduzidoConhecida,
            Map<String,ProvaCampoHistoricoDTO> provaCampos
    ) {}

    private final ContratoRepositorio contratoRepositorio;
    private final AditamentoContratualRepositorio aditamentoRepositorio;
    private final RemanejamentoContratualRepositorio remanejamentoRepositorio;
    private final TipoContratacaoRepositorio tipoContratacaoRepositorio;
    private final FornecedorRepositorio fornecedorRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalContratoServico motor;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final PermissaoExcepcionalServico permissaoExcepcionalServico;
    private VinculoContratualServico vinculoContratualServico;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private PendenciaAdministrativaServico pendencias;
    private ImpactoProgramacaoComposicaoServico impactoProgramacao;
    private AssinaturaImpactoVinculoServico assinaturaImpacto;
    private GestaoContratoTemporalServico gestaoContratoTemporal;
    private VinculoContratualRepositorio vinculosRepositorio;
    private MotorTemporalVinculoServico motorVinculo;
    private TransferenciaGestaoContratoRepositorio transferenciasRepositorio;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarTransferencias(TransferenciaGestaoContratoRepositorio repositorio) {
        this.transferenciasRepositorio = repositorio;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarPoliticaDosVinculos(VinculoContratualRepositorio repositorio, MotorTemporalVinculoServico temporal) {
        this.vinculosRepositorio = repositorio; this.motorVinculo = temporal;
    }

    public AditamentoContratualServico(
            ContratoRepositorio contratoRepositorio,
            AditamentoContratualRepositorio aditamentoRepositorio,
            RemanejamentoContratualRepositorio remanejamentoRepositorio,
            TipoContratacaoRepositorio tipoContratacaoRepositorio,
            FornecedorRepositorio fornecedorRepositorio,
            ComposicaoContratualRepositorio composicaoRepositorio,
            LotacaoRepositorio lotacaoRepositorio,
            JornadaRepositorio jornadaRepositorio,
            CargoRepositorio cargoRepositorio,
            AcessoOrganizacionalServico acesso,
            MotorTemporalContratoServico motor,
            ComposicaoContratualTemporalServico composicaoTemporal,
            PermissaoExcepcionalServico permissaoExcepcionalServico) {
        this.contratoRepositorio = contratoRepositorio;
        this.aditamentoRepositorio = aditamentoRepositorio;
        this.remanejamentoRepositorio = remanejamentoRepositorio;
        this.tipoContratacaoRepositorio = tipoContratacaoRepositorio;
        this.fornecedorRepositorio = fornecedorRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.acesso = acesso;
        this.motor = motor;
        this.composicaoTemporal = composicaoTemporal;
        this.permissaoExcepcionalServico = permissaoExcepcionalServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarVinculosCanonicos(VinculoContratualServico vinculoContratualServico) {
        this.vinculoContratualServico = vinculoContratualServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDocumentos(DocumentoComprobatorioServico documentoComprobatorioServico) {
        this.documentoComprobatorioServico = documentoComprobatorioServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarRevalidacaoFrequencia(RevalidacaoFrequenciaTemporalServico servico) {
        this.revalidacaoFrequenciaTemporalServico = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarImpactoProgramacao(ImpactoProgramacaoComposicaoServico servico) {
        this.impactoProgramacao = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarAssinaturaImpacto(AssinaturaImpactoVinculoServico servico) {
        this.assinaturaImpacto = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    public List<AditamentoContratualDTO> listar(Long contratoId) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterContrato(contratoId);
        acesso.exigirAcessoContrato(user, contrato);
        Map<Long, VersaoContratoNegocialDTO> fotografias = fotografiasPorEvento(motor.listarVersoes(contratoId));
        return aditamentoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId)
                .stream().map(a -> paraDTO(a, fotografias)).toList();
    }

    public PreviaAditamentoContratualDTO previa(Long contratoId, AditamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, false);
        validarCamposBasicos(contrato, req);
        Preparacao p = preparar(contrato, req);
        validarAlteracoesNaoQuantitativas(contrato, req, p.eventosIgnorados(), obterFornecedor(req.novoFornecedorId(), false));
        return paraPrevia(contrato, req, p);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AditamentoContratualDTO registrar(Long contratoId, AditamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true, true);
        LocalDate fimContratoAntes = contrato.getDataFim();
        validarCamposBasicos(contrato, req);
        Fornecedor fornecedor = obterFornecedor(req.novoFornecedorId(), true);
        if (fornecedor != null) {
            acesso.revalidarAposEspera(user);
            acesso.exigirAcessoContrato(user, contrato);
        }
        Preparacao p = preparar(contrato, req);
        validarAlteracoesNaoQuantitativas(contrato, req, p.eventosIgnorados(), fornecedor);
        if (!administrativamenteAtivo(contrato)) {
            VersaoContratoNegocialDTO anterior = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), p.eventosIgnorados());
            boolean prorrogacaoRetroativa = req.dataEfetiva().isBefore(RelogioSistema.hoje())
                    && req.novaDataFim() != null && anterior.dataFim() != null && req.novaDataFim().isAfter(anterior.dataFim());
            if (!prorrogacaoRetroativa) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo somente admite prorrogação retroativa documentada neste fluxo.");
        }
        validarTokenPrevia(contrato, req, p);
        if (!p.conflitosVigencia().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", p.conflitosVigencia()));
        }
        if (!p.conflitosMatricula().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", p.conflitosMatricula()));
        }
        validarImpactoEExcecao(contrato, user, req, p);
        validarConflitosMesmaData(contrato, req, p.itens());
        LocalDate fimAnteriorAoAto = projetarVersaoContrato(
                contrato, req.dataEfetiva(), Instant.now(), Set.of()).dataFim();

        int versaoInstrumento = validarIdentificacaoECalcularVersao(contrato, req);
        AcaoCicloVidaContratoDTO acao = acaoDoAditamento(req);
        String chaveEvento = "ADITAMENTO:" + normalizarChave(req.identificacao()) + ":V" + versaoInstrumento;

        EventoCicloVidaContrato evento = motor.registrarEvento(
                contrato,
                TipoEventoCicloVidaContrato.ADITAMENTO,
                acao,
                user,
                "efetivo=" + p.efetivoAnterior(),
                "efetivo=" + p.efetivoResultante(),
                chaveEvento
        );

        AditamentoContratual a = new AditamentoContratual();
        a.setContrato(contrato);
        a.setEvento(evento);
        a.setIdentificacao(req.identificacao());
        a.setVersaoInstrumento(versaoInstrumento);
        a.setDataDocumento(req.dataDocumento());
        a.setDataAssinatura(req.dataAssinatura());
        a.setDataEfetiva(req.dataEfetiva());
        a.setJustificativa(req.justificativa());
        a.setReferenciaSei(req.referenciaSei());
        a.setUsuarioResponsavelId(user != null ? user.getId() : null);
        a.setUsuarioResponsavelUsername(user != null ? user.getUsername() : null);
        a.setNovaDescricao(limpar(req.novaDescricao()));
        a.setNovaDataInicio(req.novaDataInicio());
        a.setNovaDataFim(req.novaDataFim());
        a.setNovoEfetivoTotal(hasQuantitativo(req) ? Integer.valueOf(p.efetivoResultante()) : req.novoEfetivoTotal());
        a.setReducaoExcepcional(Boolean.TRUE.equals(req.reducaoExcepcional()));
        a.setFundamentoExcecao(limpar(req.fundamentoExcecao()));
        a.setImpactoConfirmado(Boolean.TRUE.equals(req.confirmarImpacto()));
        a.setNovaMatriculaVinculoObrigatoria(req.novaMatriculaVinculoObrigatoria());
        a.setNovoDiaSemanaReduzido(req.novoDiaSemanaReduzido());
        a.setNovasHorasDiaReduzido(req.novasHorasDiaReduzido());
        a.setRestaurarPoliticaMatriculaDoTipo(req.restaurarPoliticaMatriculaDoTipo());
        a.setRemoverConfiguracaoDiaReduzido(req.removerConfiguracaoDiaReduzido());

        if (req.novoTipoContratacaoId() != null) {
            a.setNovoTipoContratacao(obterTipoContratacao(contrato, req.novoTipoContratacaoId(), req.dataEfetiva()));
            a.setMatriculaNovoTipoFotografada(Boolean.TRUE.equals(a.getNovoTipoContratacao().getMatriculaVinculoObrigatoria()));
            a.setNovoTipoDescricaoFotografia(limpar(a.getNovoTipoContratacao().getDescricao()));
        }
        if (fornecedor != null) a.setNovoFornecedor(fornecedor);
        a.setNovoCnpjEmpresa(normalizarCnpj(req.novoCnpjEmpresa()));
        a.setNovaRazaoSocialEmpresa(limpar(req.novaRazaoSocialEmpresa()));
        a.setNovoNomeFantasiaEmpresa(limpar(req.novoNomeFantasiaEmpresa()));

        for (ItemPreparado ip : p.itens()) {
            AditamentoComposicao item = new AditamentoComposicao();
            item.setComposicaoBase(ip.composicaoBase());
            item.setLotacao(ip.lotacao());
            item.setJornada(ip.jornada());
            item.setCargo(ip.cargo());
            item.setQuantidadeAnterior(ip.anterior());
            item.setVariacao(ip.variacao());
            item.setQuantidadeResultante(ip.resultante());
            item.aplicarConfiguracaoDocumentada(ip.lotacaoMunicipio(), ip.jornadaSemanal());
            a.adicionarAlteracao(item);
        }

        AditamentoContratual salvo = aditamentoRepositorio.save(a);

        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(contrato, TipoFatoDocumental.ADITAMENTO,
                    req.identificacao(), salvo.getId(), req.referenciaSei(), req.dataEfetiva(), user);
        }

        VersaoContratoNegocialDTO projetada = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), Set.of());
        motor.registrarFotografiaProjetada(contrato, evento, req.dataEfetiva(), projetada);
        sincronizarProjecaoAtual(contrato);
        if (vinculoContratualServico != null) {
            vinculoContratualServico.estenderAlocacoesPorProrrogacaoTempestiva(
                    contrato, fimAnteriorAoAto, projetada.dataFim(), req.dataEfetiva(), evento.getId(), user,
                    "Continuidade da alocação após prorrogação tempestiva do contrato por aditamento " + salvo.getIdentificacao());
            vinculoContratualServico.preservarFimAntigoEmProrrogacaoTardia(
                    contrato, fimContratoAntes, contrato.getDataFim(), user,
                    "Aditamento de prorrogação registrado após o fim anterior; vínculos não são reabertos automaticamente.");
        }
        revalidarFrequenciasContrato(contrato, req.dataEfetiva(),
                "Aditamento/retificação contratual retroativa exige revalidação das frequências");
        registrarConflitosPosteriores(contrato, req.dataEfetiva(), "ADITAMENTO");

        return paraDTO(salvo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AditamentoContratualDTO retificar(Long contratoId, Long aditamentoId, AditamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true);
        AditamentoContratual original = obterAditamentoNoContrato(aditamentoId, contratoId);
        if (original.getEvento() == null || original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente aditamento vigente pode ser retificado.");
        }
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da retificação não informados.");
        AditamentoContratualRequisicao ajustada = new AditamentoContratualRequisicao(
                original.getIdentificacao(), req.dataDocumento(), req.dataAssinatura(), req.dataEfetiva(),
                req.justificativa(), req.referenciaSei(), req.novaDescricao(), req.novoTipoContratacaoId(),
                req.novoFornecedorId(), req.novoCnpjEmpresa(), req.novaRazaoSocialEmpresa(), req.novoNomeFantasiaEmpresa(),
                req.novaDataInicio(), req.novaDataFim(), req.novoEfetivoTotal(), req.reducaoExcepcional(),
                req.fundamentoExcecao(), req.confirmarImpacto(), req.alteracoesComposicao(),
                original.getEvento().getId(), TipoRelacaoPrecedenciaNegocial.RETIFICA,
                req.ordemDocumental() != null ? req.ordemDocumental() : original.getEvento().getOrdemDocumental(),
                req.tokenPrevia(), req.novaMatriculaVinculoObrigatoria(), req.novoDiaSemanaReduzido(), req.novasHorasDiaReduzido(),
                req.restaurarPoliticaMatriculaDoTipo(), req.removerConfiguracaoDiaReduzido()
        );
        return registrar(contratoId, ajustada);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AditamentoContratualDTO cancelarProgramado(Long contratoId, Long aditamentoId, AcaoCicloVidaContratoDTO acaoCancelamento) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true);
        AditamentoContratual original = obterAditamentoNoContrato(aditamentoId, contratoId);
        if (original.getEvento() == null || original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aditamento não está vigente para cancelamento.");
        }
        if (!original.getDataEfetiva().isAfter(RelogioSistema.hoje())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente aditamento futuro pode ser cancelado antes de produzir efeito.");
        }
        if (acaoCancelamento == null || acaoCancelamento.motivo() == null || acaoCancelamento.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo do cancelamento.");
        }
        if (impactoProgramacao != null) {
            List<String> dependentes = new ArrayList<>();
            if (!original.getAlteracoesComposicao().isEmpty()) dependentes.addAll(impactoProgramacao.diagnosticarCancelamento(
                    contrato, original.getDataEfetiva(), original.getEvento().getId()));
            if (original.getNovaDataInicio() != null || original.getNovaDataFim() != null) {
                Instant conhecido = Instant.now();
                dependentes.addAll(impactoProgramacao.diagnosticarVigenciaCancelamento(contrato, original.getDataEfetiva(),
                        data -> projetarVersaoContrato(contrato, data, conhecido, Set.of()),
                        data -> projetarVersaoContrato(contrato, data, conhecido, Set.of(original.getEvento().getId()))));
                dependentes.addAll(diagnosticarVigenciaInstrumentos(contrato, original.getDataEfetiva(), conhecido,
                        Set.of(original.getEvento().getId()), null));
            }
            if (!dependentes.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", dependentes));
        }
        if (original.getNovaMatriculaVinculoObrigatoria() != null || original.getNovoTipoContratacao() != null
                || Boolean.TRUE.equals(original.getRestaurarPoliticaMatriculaDoTipo())) {
            Instant conhecido = Instant.now();
            Set<Long> retirado = Set.of(original.getEvento().getId());
            List<Long> afetados = vinculosSemMatriculaNaPoliticaProjetada(contrato, original.getDataEfetiva(), conhecido,
                    retirado, data -> projetarVersaoContrato(contrato, data, conhecido, retirado));
            if (!afetados.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O cancelamento da política documentada exigiria matrícula nos vínculos " + afetados
                            + ". Trate os vínculos/programações antes de cancelar o aditamento.");
        }
        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                original.getDataEfetiva(),
                acaoCancelamento.dataAto() != null ? acaoCancelamento.dataAto() : RelogioSistema.hoje(),
                acaoCancelamento.motivo(), acaoCancelamento.referenciaSei(),
                original.getEvento().getId(), TipoRelacaoPrecedenciaNegocial.CANCELA,
                acaoCancelamento.ordemDocumental()
        );
        EventoCicloVidaContrato cancelamento = motor.registrarEvento(
                contrato, TipoEventoCicloVidaContrato.CANCELAMENTO_ADITAMENTO, acao, user,
                "ADITAMENTO_VIGENTE", "ADITAMENTO_CANCELADO",
                "CANCELAMENTO_ADITAMENTO:" + original.getId()
        );
        VersaoContratoNegocialDTO projetada = projetarVersaoContrato(contrato, original.getDataEfetiva(), Instant.now(), Set.of());
        motor.registrarFotografiaProjetada(contrato, cancelamento, original.getDataEfetiva(), projetada);
        sincronizarProjecaoAtual(contrato);
        registrarConflitosPosteriores(contrato, original.getDataEfetiva(), "CANCELAMENTO_ADITAMENTO");
        revalidarFrequenciasContrato(contrato, original.getDataEfetiva(),
                "Cancelamento de Aditamento programado alterou a previsão operacional");
        return paraDTO(obterAditamentoNoContrato(aditamentoId, contratoId));
    }

    /**
     * Reconstrói os dados escalares do contrato pela fotografia publicada + deltas
     * de aditamentos válidos. Isto evita que um fato retroativo apague mudanças
     * posteriores que continuam válidas.
     */
    public VersaoContratoNegocialDTO projetarVersaoContrato(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados) {
        return projetarVersaoContrato(contrato, dataReferencia, conhecidoAte, eventosIgnorados, (AditamentoContratualRequisicao) null);
    }
    public VersaoContratoNegocialDTO projetarVersaoContrato(Contrato contrato,LocalDate dataReferencia,
            Instant conhecidoAte,Set<Long> eventosIgnorados,LeituraHistoricaContratos entradas) {
        return reconstruirDadosNegociais(contrato,dataReferencia,conhecidoAte,eventosIgnorados,null,entradas).versao();
    }
    public VersaoContratoLeituraDTO projetarLeituraContrato(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte) {
        var projecao=reconstruirDadosNegociais(contrato,dataReferencia,conhecidoAte,Set.of(),null);
        return new VersaoContratoLeituraDTO(projecao.versao(),projecao.provaCampos());
    }
    public VersaoContratoLeituraDTO projetarLeituraContrato(Contrato contrato,LocalDate dataReferencia,
                                                          Instant conhecidoAte,LeituraHistoricaContratos entradas) {
        var projecao=reconstruirDadosNegociais(contrato,dataReferencia,conhecidoAte,Set.of(),null,entradas);
        return new VersaoContratoLeituraDTO(projecao.versao(),projecao.provaCampos());
    }

    /** A política publicada é reconstruída na data do vínculo, inclusive após alterações do catálogo mestre. */
    public boolean exigeMatriculaNoVinculoEm(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        if (contrato == null) return false;
        if (contrato.getId() == null) return contrato.exigeMatriculaNoVinculo();
        Boolean exigida = projetarVersaoContrato(contrato, dataReferencia, conhecidoAte, Set.of())
                .matriculaVinculoObrigatoriaEfetiva();
        if (exigida == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A política de matrícula não está comprovada no período. Informe matrícula válida ou registre a política por ato documentado.");
        return exigida;
    }

    /** Confere cada fronteira conhecida dentro da faixa proposta, antes de criar ou restaurar o vínculo. */
    public void exigirMatriculaCompativelNoPeriodo(VinculoContratual vinculo, LocalDate inicio, LocalDate fim) {
        Contrato contrato = vinculo.getContrato();
        Instant conhecido = Instant.now();
        for (LocalDate data : fronteirasPoliticaMatricula(contrato, inicio, conhecido, Set.of())) {
            if (fim != null && data.isAfter(fim)) break;
            String matricula = vinculo.getId() == null ? vinculo.getMatricula()
                    : motorVinculo.reconstruir(vinculo, data, conhecido).matricula();
            if (VinculoContratual.normalizarMatricula(matricula) != null) continue;
            VersaoContratoNegocialDTO politica = projetarVersaoContrato(contrato, data, conhecido, Set.of());
            if (politica.dataInicio() != null && data.isBefore(politica.dataInicio())
                    || politica.dataFim() != null && data.isAfter(politica.dataFim())) continue;
            if (!Boolean.FALSE.equals(politica.matriculaVinculoObrigatoriaEfetiva())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O período do vínculo alcança política de matrícula obrigatória ou não comprovada em "
                                + data + ". Informe matrícula válida ou trate documentalmente a política antes de confirmar.");
            }
        }
    }

    /** Simulação de vigência usa a mesma ordem dos fatos que a confirmação, sem persistir a proposta. */
    private VersaoContratoNegocialDTO projetarVersaoContrato(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados,
            AditamentoContratualRequisicao propostaVigencia) {
        return reconstruirDadosNegociais(contrato, dataReferencia, conhecidoAte, eventosIgnorados, propostaVigencia).versao();
    }

    private ProjecaoNegocial reconstruirDadosNegociais(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados,
            AditamentoContratualRequisicao propostaVigencia) {
        return reconstruirDadosNegociais(contrato,dataReferencia,conhecidoAte,eventosIgnorados,propostaVigencia,null);
    }
    private ProjecaoNegocial reconstruirDadosNegociais(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte,
            Set<Long> eventosIgnorados,AditamentoContratualRequisicao propostaVigencia,LeituraHistoricaContratos entradas) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        Set<Long> ignorar = eventosIgnorados == null ? Set.of() : eventosIgnorados;

        List<VersaoContratoNegocialDTO> versoes = (entradas==null?motor.listarVersoes(contrato.getId()):motor.listarVersoes(contrato.getId(),entradas)).stream()
                .filter(v -> v.registradoEm() != null && !v.registradoEm().isAfter(conhecimento)).toList();
        Map<Long, VersaoContratoNegocialDTO> fotografias = (entradas==null?motor.fotografiasConhecidasPorEvento(versoes, conhecimento):motor.fotografiasConhecidasPorEvento(versoes, conhecimento,entradas));
        VersaoContratoNegocialDTO base = (entradas==null?motor.fotografiaOriginalConhecida(contrato, conhecimento):motor.fotografiaOriginalConhecida(contrato, conhecimento,entradas))
                .map(motor::paraVersaoDTO).orElse(null);
        Map<String,ProvaCampoHistoricoDTO> provas=new LinkedHashMap<>((entradas==null?motor.provasDaFotografia(base):motor.provasDaFotografia(base,entradas)));
        ProvaCampoHistoricoDTO provaEfetivaDocumentada=provas.get("matriculaVinculoObrigatoriaEfetiva");
        boolean rascunhoNuncaPublicado = base == null && contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO
                && contrato.getPublicadoEm() == null;

        // Campos editáveis do rascunho ainda não são uma verdade histórica publicada.
        // Depois da publicação, ausência da fonte original não autoriza retrocarga da projeção atual.
        String numero = base != null ? base.numeroContrato() : rascunhoNuncaPublicado ? contrato.getNumeroContrato() : null;
        Integer ano = base != null ? base.anoContrato() : rascunhoNuncaPublicado ? contrato.getAnoContrato() : null;
        String descricao = base != null ? base.descricao() : rascunhoNuncaPublicado ? contrato.getDescricao() : null;
        Long tipoId = base != null ? base.tipoContratacaoId() : rascunhoNuncaPublicado && contrato.getTipoContratacao() != null
                ? contrato.getTipoContratacao().getId() : null;
        String tipoDesc = base != null ? base.tipoContratacaoDescricao() : rascunhoNuncaPublicado ? contrato.getTipo() : null;
        boolean tipoComprovado = tipoId != null && (base != null || rascunhoNuncaPublicado);
        Long fornecedorId = base != null ? base.fornecedorId() : rascunhoNuncaPublicado && contrato.getFornecedor() != null
                ? contrato.getFornecedor().getId() : null;
        String cnpj = base != null ? base.cnpjEmpresa() : rascunhoNuncaPublicado ? contrato.getCnpjEmpresa() : null;
        String razao = base != null ? base.razaoSocialEmpresa() : rascunhoNuncaPublicado ? contrato.getRazaoSocialEmpresa() : null;
        String fantasia = base != null ? base.nomeFantasiaEmpresa() : rascunhoNuncaPublicado ? contrato.getNomeFantasiaEmpresa() : null;
        LocalDate inicio = base != null ? base.dataInicio() : rascunhoNuncaPublicado ? contrato.getDataInicio() : null;
        LocalDate fim = base != null ? base.dataFim() : rascunhoNuncaPublicado ? contrato.getDataFim() : null;
        Integer efetivo = base != null ? base.efetivoPrevisto() : rascunhoNuncaPublicado ? contrato.getEfetivoPrevisto() : null;
        Long gestaoId = base != null ? base.gestaoLocalId() : rascunhoNuncaPublicado && contrato.getGestaoLocal() != null
                ? contrato.getGestaoLocal().getId() : null;
        String gestaoNome = base != null ? base.gestaoLocalNome() : rascunhoNuncaPublicado && contrato.getGestaoLocal() != null
                ? contrato.getGestaoLocal().getNome() : null;
        // Ausência de configuração na foto legada não prova que o valor atual
        // já valia naquele período, nem que a ausência de override era herança.
        boolean politicaContratoConhecida = rascunhoNuncaPublicado || base != null
                && (entradas==null?motor.campoConhecidoNaFotografia(base,"matriculaVinculoObrigatoria",
                    base.matriculaVinculoObrigatoriaEfetiva() != null || base.matriculaVinculoObrigatoria() != null)
                    :motor.campoConhecidoNaFotografia(base,"matriculaVinculoObrigatoria",
                    base.matriculaVinculoObrigatoriaEfetiva() != null || base.matriculaVinculoObrigatoria() != null,entradas));
        boolean diaReduzidoConhecido = rascunhoNuncaPublicado || base != null
                && (entradas==null?motor.campoConhecidoNaFotografia(base,"diaSemanaReduzido",base.matriculaVinculoObrigatoriaEfetiva() != null
                    || base.diaSemanaReduzido() != null || base.horasDiaReduzido() != null)
                    :motor.campoConhecidoNaFotografia(base,"diaSemanaReduzido",base.matriculaVinculoObrigatoriaEfetiva() != null
                    || base.diaSemanaReduzido() != null || base.horasDiaReduzido() != null,entradas));
        Boolean politicaEfetivaDocumentada = base == null ? null : base.matriculaVinculoObrigatoriaEfetiva();
        Boolean matriculaContrato = base != null ? base.matriculaVinculoObrigatoria()
                : rascunhoNuncaPublicado ? contrato.getMatriculaVinculoObrigatoria() : null;
        Boolean matriculaTipo = base != null ? base.matriculaTipoContratacaoObrigatoria()
                : rascunhoNuncaPublicado && contrato.getTipoContratacao() != null
                    ? Boolean.TRUE.equals(contrato.getTipoContratacao().getMatriculaVinculoObrigatoria()) : null;
        DayOfWeek diaReduzido = base != null ? base.diaSemanaReduzido() : rascunhoNuncaPublicado ? contrato.getDiaSemanaReduzido() : null;
        Integer horasReduzidas = base != null ? base.horasDiaReduzido() : rascunhoNuncaPublicado ? contrato.getHorasDiaReduzido() : null;

        Map<Long, AditamentoContratual> porEvento = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(a -> a.getEvento() != null && a.getEvento().getId() != null)
                .collect(Collectors.toMap(a -> a.getEvento().getId(), a -> a, (a, b) -> b));

        List<EventoCicloVidaContrato> eventos = new ArrayList<>((entradas==null?motor.eventosValidosOrdenados(contrato.getId(), data, conhecimento):motor.eventosValidosOrdenados(contrato.getId(), data, conhecimento,entradas)));
        EventoCicloVidaContrato eventoProposto = null;
        if (propostaVigencia != null && !propostaVigencia.dataEfetiva().isAfter(data)) {
            eventoProposto = new EventoCicloVidaContrato() {
                @Override public Long getId() { return Long.MIN_VALUE; }
            };
            eventoProposto.setContrato(contrato);
            eventoProposto.setTipoEvento(TipoEventoCicloVidaContrato.ADITAMENTO);
            eventoProposto.setDataEfetiva(propostaVigencia.dataEfetiva());
            eventoProposto.setOrdemDocumental(propostaVigencia.ordemDocumental());
            eventoProposto.setChaveNegocial("ADITAMENTO:" + normalizarChave(propostaVigencia.identificacao()) + ":PROPOSTA");
            eventoProposto.setFatoLogicoId("PROPOSTA_NAO_PERSISTIDA");
            EventoCicloVidaContrato precedente = eventos.stream()
                    .filter(e -> Objects.equals(e.getId(), propostaVigencia.eventoPrecedenteId())).findFirst().orElse(null);
            if (precedente != null && (propostaVigencia.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || propostaVigencia.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI))
                eventoProposto.setFatoLogicoId(precedente.getFatoLogicoId());
            List<RelacaoPrecedenciaEventoContrato> relacoes = new ArrayList<>((entradas==null?motor.relacoesConhecidas(contrato.getId(), conhecimento):motor.relacoesConhecidas(contrato.getId(), conhecimento,entradas)));
            if (precedente != null && propostaVigencia.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUCEDE) {
                RelacaoPrecedenciaEventoContrato relacao = new RelacaoPrecedenciaEventoContrato();
                relacao.setEventoFonte(eventoProposto); relacao.setEventoAlvo(precedente);
                relacao.setTipoRelacao(TipoRelacaoPrecedenciaNegocial.SUCEDE); relacoes.add(relacao);
            }
            eventos.removeIf(e -> ignorar.contains(e.getId()));
            eventos.add(eventoProposto);
            eventos = motor.ordenarEventosParaSimulacao(eventos, relacoes);
        }

        EventoCicloVidaContrato ultimoEvento = null;
        int qtdAplicados = 0;
        for (EventoCicloVidaContrato e : eventos) {
            if (e.getId() == null || ignorar.contains(e.getId())) continue;
            if (e == eventoProposto) {
                if (propostaVigencia.novaDataInicio() != null) inicio = propostaVigencia.novaDataInicio();
                if (propostaVigencia.novaDataFim() != null) fim = propostaVigencia.novaDataFim();
                if (Boolean.TRUE.equals(propostaVigencia.restaurarPoliticaMatriculaDoTipo())) {
                    matriculaContrato = null; politicaContratoConhecida = true;
                }
                if (propostaVigencia.novaMatriculaVinculoObrigatoria() != null) {
                    matriculaContrato = propostaVigencia.novaMatriculaVinculoObrigatoria();
                    politicaContratoConhecida = true;
                }
                if (Boolean.TRUE.equals(propostaVigencia.removerConfiguracaoDiaReduzido())) {
                    diaReduzido = null; horasReduzidas = null; diaReduzidoConhecido = true;
                } else if (propostaVigencia.novoDiaSemanaReduzido() != null) {
                    diaReduzido = propostaVigencia.novoDiaSemanaReduzido();
                    horasReduzidas = propostaVigencia.novasHorasDiaReduzido(); diaReduzidoConhecido = true;
                }
                if (propostaVigencia.novoTipoContratacaoId() != null) {
                    politicaEfetivaDocumentada = null;
                    TipoContratacao tipoProposto = obterTipoContratacao(contrato,
                            propostaVigencia.novoTipoContratacaoId(), propostaVigencia.dataEfetiva());
                    tipoId = tipoProposto.getId(); tipoDesc = tipoProposto.getDescricao();
                    tipoComprovado = true;
                    matriculaTipo = Boolean.TRUE.equals(tipoProposto.getMatriculaVinculoObrigatoria());
                }
                ultimoEvento = e; qtdAplicados++;
                continue;
            }
            var fonteEvento=ProvasCamposHistoricos.evento(e);
            if(e.getTipoEvento()==TipoEventoCicloVidaContrato.INATIVACAO_ADMINISTRATIVA
                    ||e.getTipoEvento()==TipoEventoCicloVidaContrato.CORRECAO_ANULACAO)
                provas.put("habilitacaoAdministrativa",ProvasCamposHistoricos.comprovado(HabilitacaoAdministrativaContrato.INATIVO,fonteEvento));
            if(e.getTipoEvento()==TipoEventoCicloVidaContrato.REATIVACAO_ADMINISTRATIVA)
                provas.put("habilitacaoAdministrativa",ProvasCamposHistoricos.comprovado(HabilitacaoAdministrativaContrato.ATIVO,fonteEvento));
            if (e.getTipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_CNPJ) {
                cnpj = e.getValorNovo();
                provas.put("cnpjEmpresa",ProvasCamposHistoricos.comprovado(cnpj,fonteEvento));
                ultimoEvento = e; qtdAplicados++;
                continue;
            }
            // Cada evento altera somente seus próprios campos; uma fotografia
            // integral posterior não pode apagar deltas retroativos válidos.
            if (e.getTipoEvento() == TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL) {
                politicaEfetivaDocumentada = null;
                provaEfetivaDocumentada=ProvasCamposHistoricos.desconhecido();
                VersaoContratoNegocialDTO fotografia = fotografias.get(e.getId());
                TransferenciaGestaoContrato transferencia = transferenciasRepositorio == null ? null
                        : transferenciasRepositorio.findByEventoId(e.getId()).orElse(null);
                if (transferencia != null) {
                    gestaoId = transferencia.getGestaoDestino().getId();
                    gestaoNome = limpar(transferencia.getGestaoDestinoNomeFotografia());
                } else if (fotografia != null) {
                    gestaoId = fotografia.gestaoLocalId();
                    gestaoNome = limpar(fotografia.gestaoLocalNome());
                }
                TipoContratacao destino = transferencia == null ? null : transferencia.getTipoContratacaoDestino();
                tipoComprovado = destino != null && destino.getGestaoLocal() != null
                        && Objects.equals(destino.getGestaoLocal().getId(), gestaoId);
                // A foto antiga podia copiar o Tipo da origem. Somente o mapeamento
                // explicitamente registrado prova o Tipo da Gestão de destino.
                tipoId = tipoComprovado ? destino.getId() : null;
                tipoDesc = tipoComprovado ? limpar(transferencia.getTipoDestinoDescricaoFotografia()) : null;
                if((entradas==null?motor.transferenciaComProva(e.getId(),conhecimento):motor.transferenciaComProva(e.getId(),conhecimento,entradas)) && fotografia!=null) {
                    // A cadeia documental identifica esta transferência e seu destino;
                    // não reinterpreta uma cópia legada da origem como mapeamento.
                    tipoId=fotografia.tipoContratacaoId();
                    tipoDesc=limpar(fotografia.tipoContratacaoDescricao());
                    tipoComprovado=tipoId!=null;
                }
                if(fotografia!=null && tipoComprovado
                        && (entradas==null?motor.campoHistoricoComprovado(e.getId(),"tipoContratacaoDescricao",conhecimento):motor.campoHistoricoComprovado(e.getId(),"tipoContratacaoDescricao",conhecimento,entradas)))
                    tipoDesc=limpar(fotografia.tipoContratacaoDescricao());
                if(fotografia!=null && (entradas==null?motor.campoHistoricoComprovado(e.getId(),"gestaoLocalNome",conhecimento):motor.campoHistoricoComprovado(e.getId(),"gestaoLocalNome",conhecimento,entradas)))
                    gestaoNome=limpar(fotografia.gestaoLocalNome());
                matriculaTipo = tipoComprovado && fotografia != null
                        && Objects.equals(tipoId, fotografia.tipoContratacaoId())
                        ? fotografia.matriculaTipoContratacaoObrigatoria() : null;
                var provasFoto=(entradas==null?motor.provasDiretasDaTransferencia(fotografia):motor.provasDiretasDaTransferencia(fotografia,entradas));
                for(String campo:List.of("gestaoLocalId","gestaoLocalNome","tipoContratacaoId","tipoContratacaoDescricao",
                        "matriculaTipoContratacaoObrigatoria"))provas.put(campo,ProvasCamposHistoricos.desconhecido());
                if(gestaoId!=null)provas.put("gestaoLocalId",ProvasCamposHistoricos.comprovado(gestaoId,fonteEvento));
                if(gestaoNome!=null)provas.put("gestaoLocalNome",ProvasCamposHistoricos.comprovado(gestaoNome,fonteEvento));
                if(tipoComprovado) {
                    provas.put("tipoContratacaoId",ProvasCamposHistoricos.comprovado(tipoId,fonteEvento));
                    if(tipoDesc!=null)provas.put("tipoContratacaoDescricao",ProvasCamposHistoricos.comprovado(tipoDesc,fonteEvento));
                    if(matriculaTipo!=null)provas.put("matriculaTipoContratacaoObrigatoria",provasFoto.getOrDefault(
                            "matriculaTipoContratacaoObrigatoria",ProvasCamposHistoricos.desconhecido()));
                }
                for(String campo:List.of("gestaoLocalNome","tipoContratacaoId","tipoContratacaoDescricao","matriculaTipoContratacaoObrigatoria"))
                    if(fotografia!=null&&(entradas==null?motor.campoHistoricoComprovado(e.getId(),campo,conhecimento):motor.campoHistoricoComprovado(e.getId(),campo,conhecimento,entradas))
                            &&(!campo.startsWith("tipoContratacao")||tipoComprovado))
                        provas.put(campo,provasFoto.getOrDefault(campo,ProvasCamposHistoricos.desconhecido()));
                ultimoEvento = e;
                qtdAplicados++;
                continue;
            }
            if (e.getTipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_IDENTIFICADOR) {
                VersaoContratoNegocialDTO fotografia = fotografias.get(e.getId());
                if (fotografia != null) {
                    numero = fotografia.numeroContrato();
                    ano = fotografia.anoContrato();
                    provas.put("numeroContrato",ProvasCamposHistoricos.comprovado(numero,fonteEvento));
                    provas.put("anoContrato",ProvasCamposHistoricos.comprovado(ano,fonteEvento));
                    ultimoEvento = e;
                    qtdAplicados++;
                }
                continue;
            }
            if (e.getTipoEvento() != TipoEventoCicloVidaContrato.ADITAMENTO) continue;
            AditamentoContratual a = porEvento.get(e.getId());
            if (a == null) continue;
            if (a.getNovaDescricao() != null) {
                descricao = a.getNovaDescricao();provas.put("descricao",ProvasCamposHistoricos.comprovado(descricao,fonteEvento));
            }
            if (a.getNovoTipoContratacao() != null) {
                politicaEfetivaDocumentada = null;
                provaEfetivaDocumentada=ProvasCamposHistoricos.desconhecido();
                tipoId = a.getNovoTipoContratacao().getId();
                tipoComprovado = true;
                tipoDesc = descricaoTipoFotografada(a, fotografias);
                VersaoContratoNegocialDTO fotografia = fotografias.get(e.getId());
                matriculaTipo = a.getMatriculaNovoTipoFotografada() != null ? a.getMatriculaNovoTipoFotografada()
                        : fotografia != null ? fotografia.matriculaTipoContratacaoObrigatoria() : null;
                provas.put("tipoContratacaoId",ProvasCamposHistoricos.comprovado(tipoId,fonteEvento));
                provas.put("tipoContratacaoDescricao",tipoDesc==null?ProvasCamposHistoricos.desconhecido()
                        :ProvasCamposHistoricos.comprovado(tipoDesc,fonteEvento));
                provas.put("matriculaTipoContratacaoObrigatoria",matriculaTipo==null?ProvasCamposHistoricos.desconhecido()
                        :ProvasCamposHistoricos.comprovado(matriculaTipo,fonteEvento));
            }
            if (a.getNovoFornecedor() != null) {fornecedorId = a.getNovoFornecedor().getId();provas.put("fornecedorId",ProvasCamposHistoricos.comprovado(fornecedorId,fonteEvento));}
            if (a.getNovoCnpjEmpresa() != null) {cnpj = a.getNovoCnpjEmpresa();provas.put("cnpjEmpresa",ProvasCamposHistoricos.comprovado(cnpj,fonteEvento));}
            if (a.getNovaRazaoSocialEmpresa() != null) {razao = a.getNovaRazaoSocialEmpresa();provas.put("razaoSocialEmpresa",ProvasCamposHistoricos.comprovado(razao,fonteEvento));}
            if (a.getNovoNomeFantasiaEmpresa() != null) {fantasia = a.getNovoNomeFantasiaEmpresa();provas.put("nomeFantasiaEmpresa",ProvasCamposHistoricos.comprovado(fantasia,fonteEvento));}
            if (a.getNovaDataInicio() != null) {inicio = a.getNovaDataInicio();provas.put("dataInicio",ProvasCamposHistoricos.comprovado(inicio,fonteEvento));}
            if (a.getNovaDataFim() != null) {fim = a.getNovaDataFim();provas.put("dataFim",ProvasCamposHistoricos.comprovado(fim,fonteEvento));}
            if (a.getNovoEfetivoTotal() != null) {efetivo = a.getNovoEfetivoTotal();provas.put("efetivoPrevisto",ProvasCamposHistoricos.comprovado(efetivo,fonteEvento));}
            if (Boolean.TRUE.equals(a.getRestaurarPoliticaMatriculaDoTipo())) {
                matriculaContrato = null; politicaContratoConhecida = true;
                provas.put("matriculaVinculoObrigatoria",ProvasCamposHistoricos.comprovado(null,fonteEvento));
            }
            if (a.getNovaMatriculaVinculoObrigatoria() != null) {
                matriculaContrato = a.getNovaMatriculaVinculoObrigatoria(); politicaContratoConhecida = true;
                provas.put("matriculaVinculoObrigatoria",ProvasCamposHistoricos.comprovado(matriculaContrato,fonteEvento));
            }
            if (Boolean.TRUE.equals(a.getRemoverConfiguracaoDiaReduzido())) {
                diaReduzido = null; horasReduzidas = null; diaReduzidoConhecido = true;
                provas.put("diaSemanaReduzido",ProvasCamposHistoricos.comprovado(null,fonteEvento));
                provas.put("horasDiaReduzido",ProvasCamposHistoricos.comprovado(null,fonteEvento));
            } else if (a.getNovoDiaSemanaReduzido() != null) {
                diaReduzido = a.getNovoDiaSemanaReduzido(); horasReduzidas = a.getNovasHorasDiaReduzido();
                diaReduzidoConhecido = true;
                provas.put("diaSemanaReduzido",ProvasCamposHistoricos.comprovado(diaReduzido,fonteEvento));
                provas.put("horasDiaReduzido",ProvasCamposHistoricos.comprovado(horasReduzidas,fonteEvento));
            }
            ultimoEvento = e;
            qtdAplicados++;
        }

        if (inicio == null || !data.isBefore(inicio)) {
            var composicoes = (entradas==null?composicaoTemporal.projetarEntidades(contrato, data, conhecimento, ignorar):composicaoTemporal.projetarEntidades(contrato, data, conhecimento, ignorar,entradas));
            if (!composicoes.isEmpty()) {
                efetivo = ComposicaoContratualTemporalServico.totalRepresentavel(composicoes);
                provas.put("efetivoPrevisto",ProvasCamposHistoricos.derivado(efetivo,
                        composicoes.stream().map(p->p.provaCampos().get("efetivoPrevisto")).toList()));
            }
        }
        VersaoContratoNegocialDTO versao = new VersaoContratoNegocialDTO(
                null, base == null && qtdAplicados == 0 ? null : (base == null ? 0 : 1) + qtdAplicados,
                ultimoEvento != null ? ultimoEvento.getId() : base != null ? base.eventoOrigemId() : null,
                ultimoEvento != null ? ultimoEvento.getDataEfetiva() : base != null ? base.dataEfetiva() : null,
                ultimoEvento != null ? ultimoEvento.getRegistradoEm() : base != null ? base.registradoEm() : null,
                numero, ano, descricao, tipoId, tipoDesc, fornecedorId, cnpj, razao, fantasia,
                inicio, fim, efetivo, gestaoId, gestaoNome,
                matriculaContrato, matriculaTipo, matriculaContrato != null ? matriculaContrato
                    : politicaContratoConhecida ? matriculaTipo : politicaEfetivaDocumentada,
                diaReduzido, horasReduzidas, (entradas==null?motor.habilitacaoAdministrativaEm(contrato, data, conhecimento):motor.habilitacaoAdministrativaEm(contrato, data, conhecimento,Set.of(),entradas)), tipoComprovado
        );
        if(ultimoEvento!=null)provas.put("dataEfetiva",ProvasCamposHistoricos.comprovado(versao.dataEfetiva(),ProvasCamposHistoricos.evento(ultimoEvento)));
        provas.put("matriculaVinculoObrigatoriaEfetiva",matriculaContrato!=null
                ?ProvasCamposHistoricos.derivado(versao.matriculaVinculoObrigatoriaEfetiva(),provas.get("matriculaVinculoObrigatoria"))
                :politicaContratoConhecida?ProvasCamposHistoricos.derivado(versao.matriculaVinculoObrigatoriaEfetiva(),
                        provas.get("matriculaVinculoObrigatoria"),provas.get("matriculaTipoContratacaoObrigatoria"))
                :provaEfetivaDocumentada==null?ProvasCamposHistoricos.desconhecido():provaEfetivaDocumentada);
        var completas=ProvasCamposHistoricos.desconhecidos(ProvasCamposHistoricos.valores(versao).keySet());completas.putAll(provas);
        return new ProjecaoNegocial(versao, politicaContratoConhecida, diaReduzidoConhecido,Map.copyOf(completas));
    }

    private Preparacao preparar(Contrato contrato, AditamentoContratualRequisicao req) {
        Set<Long> ignorados = eventosIgnoradosPelaPrecedencia(req);
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado =
                composicaoTemporal.projetarEntidades(contrato, req.dataEfetiva(), Instant.now(), ignorados);
        long somaAnterior = estado.stream().mapToLong(ComposicaoContratualTemporalServico.ComposicaoProjetada::efetivoPrevisto).sum();
        if (somaAnterior < 0 || somaAnterior > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O efetivo reconstruído não pode ser representado integralmente. Trate os fatos anteriores.");
        }
        int totalAnterior = (int) somaAnterior;

        List<ItemPreparado> itens = new ArrayList<>();
        Set<String> chaves = new HashSet<>();
        if (req.alteracoesComposicao() != null) {
            for (AditamentoComposicaoRequisicao itemReq : req.alteracoesComposicao()) {
                if (itemReq == null || itemReq.quantidadeResultante() == null || itemReq.quantidadeResultante() < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade resultante da composição deve ser zero ou positiva.");
                }
                Optional<ComposicaoContratualTemporalServico.ComposicaoProjetada> existente = composicaoTemporal.localizar(
                        estado, itemReq.composicaoId(), itemReq.lotacaoId(), itemReq.jornadaId(), itemReq.cargoId());

                ComposicaoContratual base = null;
                Lotacao lotacao;
                Jornada jornada;
                Cargo cargo;
                int anterior;
                if (existente.isPresent()) {
                    var p = existente.get();
                    lotacao = p.lotacao(); jornada = p.jornada(); cargo = p.cargo(); anterior = p.efetivoPrevisto();
                    if (p.composicaoBaseId() != null) base = composicaoRepositorio.findById(p.composicaoBaseId()).orElse(null);
                    validarRecorteInformadoCompativel(itemReq, p);
                } else {
                    if (itemReq.lotacaoId() == null || itemReq.jornadaId() == null || itemReq.cargoId() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Nova combinação exige Lotação, Turno e Cargo explícitos.");
                    }
                    lotacao = lotacaoRepositorio.findById(itemReq.lotacaoId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação não encontrada."));
                    jornada = jornadaRepositorio.findById(itemReq.jornadaId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno não encontrado."));
                    cargo = cargoRepositorio.findById(itemReq.cargoId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo não encontrado."));
                    anterior = 0;
                }
                validarCatalogosDoContrato(contrato, lotacao, jornada, cargo, req.dataEfetiva(), existente.isPresent());

                String chave = ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(lotacao.getId(), jornada.getId(), cargo.getId());
                if (!chaves.add(chave)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A mesma composição foi informada mais de uma vez no aditamento.");
                }
                long vinculados = ocupacaoEm(contrato.getId(), lotacao.getId(), jornada.getId(), cargo.getId(),
                        req.dataEfetiva());
                // Alteração quantitativa preserva a configuração da combinação.
                // A nova combinação, documentada pelo ADI, captura sua própria
                // configuração. Optional.map não serve aqui: null histórico
                // deve continuar null, sem cair no cadastro atual.
                String municipio = existente.isPresent() ? existente.get().lotacaoMunicipio() : lotacao.getMunicipio();
                String semana = existente.isPresent() ? existente.get().jornadaSemanal()
                        : com.example.siggtrat.utilitario.FotografiaJornada.capturar(jornada);
                itens.add(new ItemPreparado(base, lotacao, jornada, cargo, anterior, itemReq.quantidadeResultante(), vinculados,
                        municipio, semana));
            }
        }

        long somaResultante = totalAnterior + itens.stream().mapToLong(ItemPreparado::variacao).sum();
        if (somaResultante < 0 || somaResultante > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A soma real das composições (" + somaResultante + ") ultrapassa a representação do efetivo contratual.");
        }
        int totalResultante = (int) somaResultante;
        if (req.novoEfetivoTotal() != null && req.novoEfetivoTotal() != totalResultante) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O efetivo total informado (" + req.novoEfetivoTotal() + ") não corresponde à soma projetada das composições (" + totalResultante + ").");
        }
        if (itens.isEmpty() && req.novoEfetivoTotal() != null && req.novoEfetivoTotal() != totalAnterior) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Alteração do efetivo total exige informar as alterações das composições correspondentes.");
        }

        Integer originalDocumentado = motor.fotografiaOriginalConhecida(contrato, Instant.now())
                .map(VersaoContratoNegocial::getEfetivoPrevisto).orElse(contrato.getEfetivoOriginalPublicado());
        if (originalDocumentado == null) {
            // As composições-base são fotografias imutáveis da publicação;
            // atos posteriores só acrescentam deltas e nunca alteram esta soma.
            var bases = composicaoRepositorio.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contrato.getId());
            if (bases.isEmpty() || bases.stream().anyMatch(b -> b.getEfetivoPrevisto() == null))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O efetivo original não está comprovado. Incorpore a fonte antes de calcular o piso contratual.");
            long somaOriginal = bases.stream().mapToLong(ComposicaoContratual::getEfetivoPrevisto).sum();
            if (somaOriginal < 0 || somaOriginal > Integer.MAX_VALUE)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O efetivo original documentado é inconsistente.");
            originalDocumentado = (int) somaOriginal;
        }
        int original = originalDocumentado;
        int piso = RegraEfetivoContratual.pisoNormal(original);
        boolean reducao = totalResultante < totalAnterior;
        boolean abaixoPiso = reducao && totalResultante < piso;
        boolean impacto = itens.stream().anyMatch(i -> i.colaboradoresAtuais() > i.resultante());
        List<String> alertas = new ArrayList<>();
        if (impacto) alertas.add("Há composição com ocupação atual superior ao quantitativo resultante; pessoas não serão movidas automaticamente.");
        if (impactoProgramacao != null) {
            List<String> futuros = impactoProgramacao.diagnosticar(contrato, req.dataEfetiva(), itens.stream().map(i ->
                    new ImpactoProgramacaoComposicaoServico.Alteracao(i.composicaoBase() == null ? null : i.composicaoBase().getId(),
                            i.lotacao().getId(), i.jornada().getId(), i.cargo().getId(), i.variacao())).toList(), ignorados,
                    new ImpactoProgramacaoComposicaoServico.ContextoProposta(TipoEventoCicloVidaContrato.ADITAMENTO,
                            "ADITAMENTO:" + normalizarChave(req.identificacao()), req.ordemDocumental(),
                            req.eventoPrecedenteId(), req.relacaoPrecedencia()));
            alertas.addAll(futuros);
            impacto = impacto || !futuros.isEmpty();
        }
        if (abaixoPiso) alertas.add("A redução ultrapassa o limite normal de 25% do efetivo originalmente publicado e exige fluxo excepcional.");
        if (req.dataEfetiva().isBefore(RelogioSistema.hoje())) alertas.add("Aditamento retroativo: frequências, relatórios e eventos posteriores deverão ser reavaliados pelas etapas dependentes.");
        var proposta = new ComposicaoContratualTemporalServico.PropostaComposicao(req.dataEfetiva(),
                TipoEventoCicloVidaContrato.ADITAMENTO, "ADITAMENTO:" + normalizarChave(req.identificacao()) + ":PROPOSTA",
                req.ordemDocumental(), req.eventoPrecedenteId(), req.relacaoPrecedencia(), itens.stream().map(i ->
                new ComposicaoContratualTemporalServico.DeltaProposto(i.composicaoBase() == null ? null : i.composicaoBase().getId(),
                        i.lotacao(), i.jornada(), i.cargo(), i.variacao())).toList());
        for (var conflito : composicaoTemporal.diagnosticarConflitosSimulados(contrato, Instant.now(), ignorados, proposta)) {
            if (conflito.eventoId() != null && conflito.eventoId() > 0
                    && (conflito.dataEfetiva() == null || !conflito.dataEfetiva().isBefore(req.dataEfetiva())))
                alertas.add("Evento posterior " + conflito.eventoId() + " será preservado sem aplicar a alteração incompatível, "
                        + "com pendência de tratamento: " + conflito.motivo());
        }

        List<String> conflitosVigencia = diagnosticarImpactoVigencia(contrato, req, ignorados);
        alertas.addAll(conflitosVigencia);
        List<String> conflitosMatricula = diagnosticarImpactoMatricula(contrato, req, ignorados);
        alertas.addAll(conflitosMatricula);
        return new Preparacao(List.copyOf(itens), totalAnterior, totalResultante, original, piso,
                reducao, abaixoPiso, impacto, ignorados, conflitosVigencia, conflitosMatricula, List.copyOf(alertas));
    }

    private List<String> diagnosticarImpactoMatricula(Contrato contrato, AditamentoContratualRequisicao req,
                                                      Set<Long> ignorados) {
        if (vinculosRepositorio == null || motorVinculo == null
                || req.novaMatriculaVinculoObrigatoria() == null && req.novoTipoContratacaoId() == null
                    && !Boolean.TRUE.equals(req.restaurarPoliticaMatriculaDoTipo()) && ignorados.isEmpty())
            return List.of();
        Instant conhecido = Instant.now();
        List<Long> afetados = vinculosSemMatriculaNaPoliticaProjetada(contrato, req.dataEfetiva(), conhecido, ignorados,
                data -> projetarVersaoContrato(contrato, data, conhecido, ignorados, req));
        return afetados.isEmpty() ? List.of() : List.of("A exigência de matrícula afeta vínculos sem matrícula válida: "
                + afetados + ". Trate os vínculos/programações antes de confirmar o aditamento.");
    }

    /** Confere o contexto já fotografado na transação, inclusive atos posteriores válidos. */
    public void exigirMatriculasCompativeisAposTransferencia(Contrato contrato, LocalDate desde) {
        if (vinculosRepositorio == null || motorVinculo == null) throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Módulo temporal dos vínculos indisponível para validar a política de matrícula.");
        Instant conhecido = Instant.now();
        List<Long> afetados = vinculosSemMatriculaNaPoliticaProjetada(contrato, desde, conhecido, Set.of(),
                data -> projetarVersaoContrato(contrato, data, conhecido, Set.of()));
        if (!afetados.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A política de matrícula resultante da transferência afeta vínculos sem matrícula válida: "
                        + afetados + ". Trate os vínculos/programações antes de confirmar a transferência.");
    }

    private List<Long> vinculosSemMatriculaNaPoliticaProjetada(Contrato contrato, LocalDate desde, Instant conhecido,
                                                               Set<Long> ignorados,
                                                               Function<LocalDate, VersaoContratoNegocialDTO> projetarPolitica) {
        SortedSet<LocalDate> fronteiras = fronteirasPoliticaMatricula(contrato, desde, conhecido, ignorados);
        Map<LocalDate, VersaoContratoNegocialDTO> politicas = new HashMap<>();
        List<Long> afetados = new ArrayList<>();
        for (VinculoContratual vinculo : vinculosRepositorio.findByContratoIdOrderByDataInicioAscIdAsc(contrato.getId())) {
            if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            LocalDate inicio = vinculo.getDataInicio().isAfter(desde) ? vinculo.getDataInicio() : desde;
            SortedSet<LocalDate> datas = new TreeSet<>(fronteiras.tailSet(inicio));
            datas.add(inicio);
            for (LocalDate data : datas) {
                var posicao = motorVinculo.reconstruir(vinculo, data, conhecido);
                if (posicao.dataFim() != null && posicao.dataFim().isBefore(data)) break;
                if (data.isBefore(posicao.dataInicio()) || posicao.situacaoTemporal() == SituacaoTemporalVinculo.ANULADO) continue;
                var proposta = politicas.computeIfAbsent(data, projetarPolitica);
                if (proposta.dataInicio() != null && data.isBefore(proposta.dataInicio())
                        || proposta.dataFim() != null && data.isAfter(proposta.dataFim())) continue;
                if (!Boolean.FALSE.equals(proposta.matriculaVinculoObrigatoriaEfetiva())
                        && VinculoContratual.normalizarMatricula(posicao.matricula()) == null) {
                    afetados.add(vinculo.getId());
                    break;
                }
            }
        }
        return List.copyOf(afetados);
    }

    private SortedSet<LocalDate> fronteirasPoliticaMatricula(Contrato contrato, LocalDate desde, Instant conhecido,
                                                            Set<Long> ignorados) {
        SortedSet<LocalDate> datas = motor.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecido).stream()
                .filter(e -> !ignorados.contains(e.getId()))
                .map(EventoCicloVidaContrato::getDataEfetiva).filter(Objects::nonNull)
                .filter(data -> !data.isBefore(desde)).collect(Collectors.toCollection(TreeSet::new));
        datas.add(desde);
        return datas;
    }

    private List<String> diagnosticarImpactoVigencia(Contrato contrato, AditamentoContratualRequisicao req,
                                                     Set<Long> ignorados) {
        if (impactoProgramacao == null) return List.of();
        List<AditamentoContratual> retirados = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(a -> a.getEvento() != null && ignorados.contains(a.getEvento().getId())).toList();
        if (req.novaDataInicio() == null && req.novaDataFim() == null
                && retirados.stream().noneMatch(a -> a.getNovaDataInicio() != null || a.getNovaDataFim() != null))
            return List.of();
        LocalDate desde = retirados.stream().map(AditamentoContratual::getDataEfetiva).filter(Objects::nonNull)
                .min(LocalDate::compareTo).filter(d -> d.isBefore(req.dataEfetiva())).orElse(req.dataEfetiva());
        Instant conhecido = Instant.now();
        List<String> conflitos = new ArrayList<>(impactoProgramacao.diagnosticarVigencia(contrato, desde, Set.of(req.dataEfetiva()),
                data -> projetarVersaoContrato(contrato, data, conhecido, Set.of()),
                data -> projetarVersaoContrato(contrato, data, conhecido, ignorados, req)));
        conflitos.addAll(diagnosticarVigenciaInstrumentos(contrato, desde, conhecido, ignorados, req));
        return List.copyOf(conflitos);
    }

    /** Retirar uma fonte de vigência também deve preservar instrumentos, mesmo sem vínculos humanos. */
    private List<String> diagnosticarVigenciaInstrumentos(
            Contrato contrato, LocalDate desde, Instant conhecido, Set<Long> ignorados,
            AditamentoContratualRequisicao proposta) {
        Map<LocalDate, VersaoContratoNegocialDTO> antes = new HashMap<>(), depois = new HashMap<>();
        Map<Long, AditamentoContratual> aditamentos = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(a -> a.getEvento() != null && a.getEvento().getId() != null)
                .collect(Collectors.toMap(a -> a.getEvento().getId(), a -> a));
        List<String> conflitos = new ArrayList<>();
        long quantidade = 0;
        for (EventoCicloVidaContrato evento : motor.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecido)) {
            if (ignorados.contains(evento.getId()) || evento.getDataEfetiva().isBefore(desde)
                    || evento.getTipoEvento() != TipoEventoCicloVidaContrato.ADITAMENTO
                        && evento.getTipoEvento() != TipoEventoCicloVidaContrato.REMANEJAMENTO) continue;
            LocalDate data = evento.getDataEfetiva();
            VersaoContratoNegocialDTO anterior = antes.computeIfAbsent(data,
                    d -> projetarVersaoContrato(contrato, d, conhecido, Set.of()));
            VersaoContratoNegocialDTO resultante = depois.computeIfAbsent(data,
                    d -> projetarVersaoContrato(contrato, d, conhecido, ignorados, proposta));
            AditamentoContratual aditamento = aditamentos.get(evento.getId());
            boolean novaLacuna = false;
            if (aditamento != null && aditamento.getNovaDataFim() != null && data.isAfter(LocalDate.MIN)) {
                // O delta do próprio ato ainda não existe no dia anterior: não pode ocultar a lacuna que o inviabiliza.
                LocalDate vespera = data.minusDays(1);
                VersaoContratoNegocialDTO baseAnterior = antes.computeIfAbsent(vespera,
                        d -> projetarVersaoContrato(contrato, d, conhecido, Set.of()));
                VersaoContratoNegocialDTO baseResultante = depois.computeIfAbsent(vespera,
                        d -> projetarVersaoContrato(contrato, d, conhecido, ignorados, proposta));
                novaLacuna = !prorrogacaoComLacuna(aditamento, baseAnterior)
                        && prorrogacaoComLacuna(aditamento, baseResultante);
            }
            if (possuiCoberturaNaData(anterior, data) && !possuiCoberturaNaData(resultante, data) || novaLacuna) {
                quantidade++;
                if (conflitos.size() < 20) conflitos.add("Instrumento " + evento.getTipoEvento() + " (evento "
                        + evento.getId() + ") perderia cobertura ou continuidade contratual em " + data + ".");
            }
        }
        if (quantidade > 0) conflitos.add(quantidade
                + " instrumento(s) perderiam vigência; detalhes limitados a 20. Trate os dependentes antes de concluir o fato.");
        return List.copyOf(conflitos);
    }

    private boolean possuiCoberturaNaData(VersaoContratoNegocialDTO versao, LocalDate data) {
        return versao.dataInicio() != null && !data.isBefore(versao.dataInicio())
                && versao.dataFim() != null && !data.isAfter(versao.dataFim());
    }

    private boolean prorrogacaoComLacuna(AditamentoContratual aditamento, VersaoContratoNegocialDTO base) {
        LocalDate fimAnterior = base.dataFim();
        if (fimAnterior == null || aditamento.getNovaDataFim() == null
                || !aditamento.getNovaDataFim().isAfter(fimAnterior)) return false;
        LocalDate continuidade = fimAnterior.plusDays(1);
        return aditamento.getDataEfetiva().isAfter(continuidade)
                || aditamento.getNovaDataInicio() != null && aditamento.getNovaDataInicio().isAfter(continuidade);
    }

    private void validarImpactoEExcecao(Contrato contrato, UsuarioAplicacao user,
                                        AditamentoContratualRequisicao req, Preparacao p) {
        if (p.impactoOcupacao()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição resultante ficaria abaixo da ocupação válida na data efetiva ou em programação posterior. "
                            + "Transfira, desligue ou reprograme os vínculos afetados antes de concluir o aditamento.");
        }
        if (p.abaixoPiso() && !Boolean.TRUE.equals(req.confirmarImpacto())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A operação possui impacto relevante. Consulte a prévia vigente e confirme explicitamente o impacto.");
        }
        if (p.abaixoPiso()) {
            if (!Boolean.TRUE.equals(req.reducaoExcepcional())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Redução acima de 25% bloqueada no fluxo comum. Utilize o fluxo excepcional documentado.");
            }
            if (req.fundamentoExcecao() == null || req.fundamentoExcecao().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Redução excepcional exige fundamento jurídico/negocial e fonte comprobatória.");
            }
            if (documentoComprobatorioServico != null) {
                documentoComprobatorioServico.validarFonteObrigatoria(contrato,
                        TipoFatoDocumental.ADITAMENTO, req.identificacao(), req.referenciaSei());
            } else if (req.referenciaSei() == null || req.referenciaSei().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Redução excepcional exige fonte comprobatória.");
            }
            if (!acesso.ehGestaoLocal(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A redução excepcional somente pode ser executada pelo Gestor Local responsável.");
            }
            permissaoExcepcionalServico.exigirERegistrarUso(user,
                    TipoPermissaoExcepcional.REDUCAO_CONTRATUAL_EXCEPCIONAL,
                    contrato, gestaoContratoTemporal == null ? contrato.getGestaoLocal()
                            : gestaoContratoTemporal.gestaoAtual(contrato), req.fundamentoExcecao(),
                    "Contrato", contrato.getId());
        }
    }

    private void registrarConflitosPosteriores(Contrato contrato, LocalDate desde, String origem) {
        if (pendencias == null) return;
        for (var conflito : composicaoTemporal.diagnosticarConflitos(contrato, LocalDate.MAX, Instant.now()))
            if (conflito.dataEfetiva() == null || !conflito.dataEfetiva().isBefore(desde))
                pendencias.registrarConflitoReplay(contrato, conflito, origem);
    }

    private void validarCamposBasicos(Contrato contrato, AditamentoContratualRequisicao req) {
        if (req == null || req.identificacao() == null || req.identificacao().isBlank()
                || req.dataDocumento() == null || req.dataEfetiva() == null
                || req.justificativa() == null || req.justificativa().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Identificação, data do documento, data efetiva e justificativa são obrigatórias.");
        }
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.validarFonteObrigatoria(contrato, TipoFatoDocumental.ADITAMENTO,
                    req.identificacao(), req.referenciaSei());
        } else if (req.referenciaSei() == null || req.referenciaSei().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aditamento exige fonte comprobatória por anexo ou referência SEI.");
        }
        if ((req.eventoPrecedenteId() == null) != (req.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos.");
        }
        if (!hasAlgumaAlteracao(req)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aditamento deve alterar ao menos uma condição contratual.");
        }
    }

    private void validarAlteracoesNaoQuantitativas(Contrato contrato, AditamentoContratualRequisicao req,
            Set<Long> ignorados, Fornecedor fornecedor) {
        if (Boolean.TRUE.equals(req.restaurarPoliticaMatriculaDoTipo()) && req.novaMatriculaVinculoObrigatoria() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a nova política de matrícula ou a restauração da política do Tipo, sem combinar as duas ações.");
        }
        if (Boolean.TRUE.equals(req.removerConfiguracaoDiaReduzido())
                && (req.novoDiaSemanaReduzido() != null || req.novasHorasDiaReduzido() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A remoção do dia reduzido não pode informar uma nova configuração no mesmo instrumento.");
        }
        if ((req.novoDiaSemanaReduzido() == null) != (req.novasHorasDiaReduzido() == null)
                || req.novasHorasDiaReduzido() != null && (req.novasHorasDiaReduzido() < 1 || req.novasHorasDiaReduzido() > 24)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A alteração do dia reduzido exige dia da semana e horas entre 1 e 24 no mesmo instrumento.");
        }
        VersaoContratoNegocialDTO antes = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), ignorados);
        if (Boolean.TRUE.equals(req.restaurarPoliticaMatriculaDoTipo()) && req.novoTipoContratacaoId() == null
                && !Boolean.TRUE.equals(antes.tipoContratacaoComprovado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tipo de Contratação de destino não comprovado. Informe um Tipo válido da Gestão responsável "
                            + "em novo aditamento documentado antes de restaurar sua política de matrícula.");
        }
        if (req.novaDataFim() != null && antes.dataFim() != null && req.novaDataFim().isAfter(antes.dataFim())
                && (req.dataEfetiva().isAfter(antes.dataFim().plusDays(1))
                    || req.novaDataInicio() != null && req.novaDataInicio().isAfter(antes.dataFim().plusDays(1)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Prorrogação do mesmo contrato deve manter continuidade, sem lacuna após o fim anterior.");
        }
        LocalDate inicio = req.novaDataInicio() != null ? req.novaDataInicio() : antes.dataInicio();
        LocalDate fim = req.novaDataFim() != null ? req.novaDataFim() : antes.dataFim();
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A vigência resultante do aditamento é inválida.");
        }
        if (req.dataEfetiva() != null
                && ((inicio != null && req.dataEfetiva().isBefore(inicio))
                    || (fim != null && req.dataEfetiva().isAfter(fim)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data efetiva do aditamento deve estar contida na vigência resultante do próprio fato.");
        }
        if (req.novoCnpjEmpresa() != null && normalizarCnpj(req.novoCnpjEmpresa()).length() != 14) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ da empresa deve conter 14 dígitos.");
        }
        if (req.novoTipoContratacaoId() != null) {
            obterTipoContratacao(contrato, req.novoTipoContratacaoId(), req.dataEfetiva());
        }
        AditamentoContratual original = encontrarAditamentoAntecessorVersionado(contrato, req);
        String cnpjInformado = normalizarCnpj(req.novoCnpjEmpresa());
        String cnpjAnterior = normalizarCnpj(antes.cnpjEmpresa());
        validarDadosEmpresariaisDaNovaVersao(original, req, cnpjInformado);
        String cnpjResultante = cnpjInformado;
        if (fornecedor != null) {
            String cnpjFornecedor = normalizarCnpj(fornecedor.getCnpj());
            boolean preservaEmpresaDocumentada = original != null
                    && req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    && original.getIdentificacao().equalsIgnoreCase(req.identificacao().trim())
                    && Objects.equals(original.getDataEfetiva(), req.dataEfetiva())
                    && original.getNovoFornecedor() != null && Objects.equals(original.getNovoFornecedor().getId(), fornecedor.getId())
                    && Objects.equals(normalizarCnpj(original.getNovoCnpjEmpresa()), cnpjInformado);
            cnpjResultante = preservaEmpresaDocumentada ? cnpjInformado : cnpjFornecedor;
            boolean trocaEmpresa = !Objects.equals(antes.fornecedorId(), fornecedor.getId())
                    || cnpjResultante != null && !Objects.equals(cnpjAnterior, cnpjResultante);
            if (!preservaEmpresaDocumentada && trocaEmpresa && !Boolean.TRUE.equals(fornecedor.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O fornecedor escolhido está inativo. Selecione uma empresa ativa para a troca.");
            }
            if (!preservaEmpresaDocumentada && cnpjInformado != null && !Objects.equals(cnpjInformado, cnpjFornecedor)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O CNPJ informado no aditamento deve corresponder ao fornecedor escolhido.");
            }
        }
        // A projeção conserva os campos não informados. Se o CNPJ muda, ela
        // não pode herdar nomes ou uma associação da empresa anterior.
        if (cnpjResultante != null && !Objects.equals(cnpjAnterior, cnpjResultante)) {
            if (fornecedor == null && antes.fornecedorId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O contrato tem fornecedor associado. Selecione o fornecedor correspondente ao novo CNPJ.");
            }
            if (cnpjInformado == null || limpar(req.novaRazaoSocialEmpresa()) == null
                    || limpar(req.novoNomeFantasiaEmpresa()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A troca de empresa exige CNPJ, razão social e nome fantasia da nova empresa no aditamento.");
            }
        }
    }

    private Fornecedor obterFornecedor(Long id, boolean escrita) {
        if (id == null) return null;
        Fornecedor fornecedor = (escrita ? fornecedorRepositorio.buscarPorIdParaAtualizacao(id) : fornecedorRepositorio.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor não encontrado."));
        // O cadastro e a gravação disputam a mesma linha. Releia após a espera,
        // inclusive se a entidade já estava no contexto de persistência.
        if (escrita) entityManager.refresh(fornecedor);
        return fornecedor;
    }

    private AditamentoContratual encontrarAditamentoAntecessorVersionado(Contrato contrato, AditamentoContratualRequisicao req) {
        if (req.eventoPrecedenteId() == null || (req.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.RETIFICA
                && req.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.SUBSTITUI)) return null;
        return aditamentoRepositorio.findByEventoId(req.eventoPrecedenteId())
                .filter(original -> Objects.equals(original.getContrato().getId(), contrato.getId())
                        && original.getEvento().getSituacaoFato() == SituacaoFatoNegocial.VIGENTE)
                .orElse(null);
    }

    private void validarDadosEmpresariaisDaNovaVersao(AditamentoContratual original, AditamentoContratualRequisicao req,
            String cnpjInformado) {
        if (original == null) return;
        // A substituição pode retirar por inteiro o efeito empresarial anterior.
        // Se o novo ato trata da empresa, não pode combinar uma parte da
        // fotografia nova com outra herdada da empresa que antecedeu o ato.
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI
                && req.novoFornecedorId() == null && cnpjInformado == null
                && limpar(req.novaRazaoSocialEmpresa()) == null && limpar(req.novoNomeFantasiaEmpresa()) == null) return;
        if ((original.getNovoFornecedor() != null && req.novoFornecedorId() == null)
                || (normalizarCnpj(original.getNovoCnpjEmpresa()) != null && cnpjInformado == null)
                || (limpar(original.getNovaRazaoSocialEmpresa()) != null && limpar(req.novaRazaoSocialEmpresa()) == null)
                || (limpar(original.getNovoNomeFantasiaEmpresa()) != null && limpar(req.novoNomeFantasiaEmpresa()) == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe na nova versão todos os dados empresariais documentados no ato anterior.");
        }
    }

    private void validarConflitosMesmaData(Contrato contrato, AditamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> novasChaves = chavesAfetadas(req, itens);
        List<AditamentoContratual> mesmosDia = aditamentoRepositorio.findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva());
        List<EventoCicloVidaContrato> eventosAtivos = motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()).stream()
                .filter(e -> e.getDataEfetiva().equals(req.dataEfetiva()))
                .toList();
        Set<Long> ativos = eventosAtivos.stream().map(EventoCicloVidaContrato::getId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<AditamentoContratual> conflitosAditamento = mesmosDia.stream()
                .filter(a -> a.getEvento() != null && ativos.contains(a.getEvento().getId()))
                .filter(a -> !Collections.disjoint(novasChaves, chavesAfetadas(a)))
                .toList();
        List<RemanejamentoContratual> conflitosRemanejamento = remanejamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva()).stream()
                .filter(r -> r.getEvento() != null && ativos.contains(r.getEvento().getId()))
                .filter(r -> r.getItens().stream().anyMatch(i -> novasChaves.contains("COMPOSICAO:" +
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        List<EventoCicloVidaContrato> conflitosCorrecaoCnpj = req.novoCnpjEmpresa() == null ? List.of()
                : eventosAtivos.stream().filter(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.CORRECAO_CNPJ).toList();
        if (conflitosAditamento.isEmpty() && conflitosRemanejamento.isEmpty() && conflitosCorrecaoCnpj.isEmpty()) return;
        if (req.eventoPrecedenteId() == null || req.relacaoPrecedencia() == null) {
            String idsA = conflitosAditamento.stream().map(a -> String.valueOf(a.getEvento().getId())).collect(Collectors.joining(", "));
            String idsR = conflitosRemanejamento.stream().map(r -> String.valueOf(r.getEvento().getId())).collect(Collectors.joining(", "));
            String idsC = conflitosCorrecaoCnpj.stream().map(e -> String.valueOf(e.getId())).collect(Collectors.joining(", "));
            String ids = String.join(", ", java.util.stream.Stream.of(idsA, idsR, idsC).filter(x -> !x.isBlank()).toList());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existe fato contratual conflitante na mesma data. Informe precedência negocial explícita. Eventos: " + ids);
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campos conflitantes não admitem MANTEM_COEXISTE.");
        }
        List<EventoCicloVidaContrato> conflitos = new ArrayList<>();
        conflitosAditamento.forEach(a -> conflitos.add(a.getEvento()));
        conflitosRemanejamento.forEach(r -> conflitos.add(r.getEvento()));
        conflitos.addAll(conflitosCorrecaoCnpj);
        motor.validarPrecedenciaCompleta(contrato, req.dataEfetiva(), conflitos,
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental());
    }

    private int validarIdentificacaoECalcularVersao(Contrato contrato, AditamentoContratualRequisicao req) {
        Optional<AditamentoContratual> top = aditamentoRepositorio
                .findTopByContratoIdAndIdentificacaoIgnoreCaseOrderByVersaoInstrumentoDesc(contrato.getId(), req.identificacao().trim());
        if (top.isEmpty()) return 1;
        AditamentoContratual anterior = top.get();
        boolean versionando = req.eventoPrecedenteId() != null
                && anterior.getEvento() != null
                && Objects.equals(anterior.getEvento().getId(), req.eventoPrecedenteId())
                && (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI);
        if (!versionando) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe aditamento com esta identificação. Para corrigi-lo, use retificação/substituição versionada.");
        }
        return Optional.ofNullable(anterior.getVersaoInstrumento()).orElse(1) + 1;
    }

    private AcaoCicloVidaContratoDTO acaoDoAditamento(AditamentoContratualRequisicao req) {
        return new AcaoCicloVidaContratoDTO(
                req.dataEfetiva(), req.dataDocumento(), req.justificativa(), req.referenciaSei(),
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental());
    }

    private Set<Long> eventosIgnoradosPelaPrecedencia(AditamentoContratualRequisicao req) {
        if (req.eventoPrecedenteId() != null
                && (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI)) {
            return Set.of(req.eventoPrecedenteId());
        }
        return Set.of();
    }

    private Set<String> chavesAfetadas(AditamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> s = new LinkedHashSet<>();
        if (req.novaDescricao() != null) s.add("CONTRATO:DESCRICAO");
        if (req.novaMatriculaVinculoObrigatoria() != null || Boolean.TRUE.equals(req.restaurarPoliticaMatriculaDoTipo()))
            s.add("CONTRATO:MATRICULA");
        if (req.novoDiaSemanaReduzido() != null || req.novasHorasDiaReduzido() != null
                || Boolean.TRUE.equals(req.removerConfiguracaoDiaReduzido())) s.add("CONTRATO:DIA_REDUZIDO");
        if (req.novoTipoContratacaoId() != null) s.add("CONTRATO:TIPO_CONTRATACAO");
        if (req.novoFornecedorId() != null || req.novoCnpjEmpresa() != null || req.novaRazaoSocialEmpresa() != null || req.novoNomeFantasiaEmpresa() != null)
            s.add("CONTRATO:EMPRESA");
        if (req.novaDataInicio() != null || req.novaDataFim() != null) s.add("CONTRATO:VIGENCIA");
        if (hasQuantitativo(req)) s.add("CONTRATO:EFETIVO");
        for (ItemPreparado i : itens) s.add("COMPOSICAO:" + i.chave());
        return s;
    }

    private Set<String> chavesAfetadas(AditamentoContratual a) {
        Set<String> s = new LinkedHashSet<>();
        if (a.getNovaDescricao() != null) s.add("CONTRATO:DESCRICAO");
        if (a.getNovaMatriculaVinculoObrigatoria() != null || Boolean.TRUE.equals(a.getRestaurarPoliticaMatriculaDoTipo()))
            s.add("CONTRATO:MATRICULA");
        if (a.getNovoDiaSemanaReduzido() != null || Boolean.TRUE.equals(a.getRemoverConfiguracaoDiaReduzido()))
            s.add("CONTRATO:DIA_REDUZIDO");
        if (a.getNovoTipoContratacao() != null) s.add("CONTRATO:TIPO_CONTRATACAO");
        if (a.getNovoFornecedor() != null || a.getNovoCnpjEmpresa() != null || a.getNovaRazaoSocialEmpresa() != null || a.getNovoNomeFantasiaEmpresa() != null)
            s.add("CONTRATO:EMPRESA");
        if (a.getNovaDataInicio() != null || a.getNovaDataFim() != null) s.add("CONTRATO:VIGENCIA");
        if (a.getNovoEfetivoTotal() != null || !a.getAlteracoesComposicao().isEmpty()) s.add("CONTRATO:EFETIVO");
        for (AditamentoComposicao i : a.getAlteracoesComposicao()) {
            s.add("COMPOSICAO:" + ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                    i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()));
        }
        return s;
    }

    private void validarRecorteInformadoCompativel(AditamentoComposicaoRequisicao req,
                                                     ComposicaoContratualTemporalServico.ComposicaoProjetada p) {
        if (req.lotacaoId() != null && !Objects.equals(req.lotacaoId(), p.lotacao().getId())
                || req.jornadaId() != null && !Objects.equals(req.jornadaId(), p.jornada().getId())
                || req.cargoId() != null && !Objects.equals(req.cargoId(), p.cargo().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O recorte informado não corresponde à composição selecionada.");
        }
    }

    private void validarCatalogosDoContrato(Contrato contrato, Lotacao lotacao, Jornada jornada, Cargo cargo,
                                           LocalDate dataEfetiva, boolean combinacaoPublicada) {
        if (disponibilidadeCatalogo != null) {
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, lotacao.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, jornada.getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, cargo.getId());
        }
        GestaoLocal gestao = gestaoNaData(contrato, dataEfetiva);
        Long gestaoId = gestao == null ? null : gestao.getId();
        if (lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null
                || !Objects.equals(gestaoId, lotacao.getGestaoLocal().getId())
                || !Objects.equals(gestaoId, jornada.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação e Turno devem pertencer à Gestão Local do contrato.");
        }
        if (cargo.getGestaoLocal() != null && !Objects.equals(gestaoId, cargo.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo não pertence à Gestão Local do contrato.");
        }
        if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, lotacao.getId(), dataEfetiva, lotacao.getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, jornada.getId(), dataEfetiva, jornada.getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, cargo.getId(), dataEfetiva, cargo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Catálogo inativo não pode ser utilizado em aditamento atual ou futuro.");
        }
        boolean jornadaPermitida = lotacao.getTurnos() != null && lotacao.getTurnos().stream()
                .anyMatch(j -> j != null && Objects.equals(j.getId(), jornada.getId()));
        if (!combinacaoPublicada && !jornadaPermitida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno não está vinculado à Lotação informada.");
        }
    }

    private TipoContratacao obterTipoContratacao(Contrato contrato, Long id, LocalDate dataEfetiva) {
        TipoContratacao tipo = tipoContratacaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação não encontrado."));
        GestaoLocal gestao = gestaoNaData(contrato, dataEfetiva);
        if (tipo.getGestaoLocal() == null || gestao == null
                || !Objects.equals(tipo.getGestaoLocal().getId(), gestao.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação pertence a outra Gestão Local.");
        }
        if (!catalogoAtivoEm(TipoCatalogoTemporal.TIPO_CONTRATACAO, tipo.getId(), dataEfetiva, tipo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tipo de Contratação inativo não pode ser usado em evento atual ou futuro.");
        }
        return tipo;
    }

    private GestaoLocal gestaoNaData(Contrato contrato, LocalDate dataEfetiva) {
        return gestaoContratoTemporal == null ? contrato.getGestaoLocal()
                : gestaoContratoTemporal.gestaoEm(contrato, dataEfetiva, Instant.now());
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : RegraDisponibilidadeCatalogo.permiteUso(data, atual);
    }

    private PreviaAditamentoContratualDTO paraPrevia(Contrato contrato, AditamentoContratualRequisicao req, Preparacao p) {
        return new PreviaAditamentoContratualDTO(
                contrato.getId(), req.dataEfetiva(), p.efetivoAnterior(), p.efetivoResultante(), p.efetivoOriginal(), p.pisoNormal(),
                p.reducao(), p.abaixoPiso(), p.abaixoPiso(), p.impactoOcupacao(),
                p.itens().stream().map(this::impactoDTO).toList(), p.alertas(), gerarTokenPrevia(contrato, req, p));
    }

    private void validarTokenPrevia(Contrato contrato, AditamentoContratualRequisicao req, Preparacao p) {
        if (req.tokenPrevia() == null || req.tokenPrevia().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aditamento exige prévia imediatamente anterior à confirmação. Gere uma nova prévia e envie tokenPrevia.");
        }
        String atual = gerarTokenPrevia(contrato, req, p);
        if (!atual.equalsIgnoreCase(req.tokenPrevia().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A prévia do aditamento ficou desatualizada. Os impactos foram alterados; gere nova prévia e confirme novamente.");
        }
    }

    private String gerarTokenPrevia(Contrato contrato, AditamentoContratualRequisicao req, Preparacao p) {
        List<Object> partes = new ArrayList<>(AssinaturaPreviaNegocial.partes(contrato.getId(), contrato.getVersion(),
                req.identificacao(), req.dataDocumento(), req.dataAssinatura(), req.dataEfetiva(), req.justificativa(), req.referenciaSei(),
                req.novaDescricao(), req.novoTipoContratacaoId(), req.novoFornecedorId(), normalizarCnpj(req.novoCnpjEmpresa()),
                req.novaRazaoSocialEmpresa(), req.novoNomeFantasiaEmpresa(), req.novaDataInicio(), req.novaDataFim(), req.novoEfetivoTotal(),
                req.novaMatriculaVinculoObrigatoria(), req.novoDiaSemanaReduzido(), req.novasHorasDiaReduzido(),
                req.restaurarPoliticaMatriculaDoTipo(), req.removerConfiguracaoDiaReduzido(), req.reducaoExcepcional(), req.fundamentoExcecao(),
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental()));
        if (req.novoTipoContratacaoId() != null) {
            TipoContratacao tipoNovo = obterTipoContratacao(contrato, req.novoTipoContratacaoId(), req.dataEfetiva());
            partes.add(AssinaturaPreviaNegocial.partes("TIPO_NOVO", tipoNovo.getId(), tipoNovo.getVersion(), tipoNovo.getMatriculaVinculoObrigatoria()));
        }
        partes.add(AssinaturaPreviaNegocial.partes("TOTAL", p.efetivoAnterior(), p.efetivoResultante(), p.efetivoOriginal(), p.pisoNormal()));
        p.itens().stream().sorted(Comparator.comparing(ItemPreparado::chave)).forEach(i -> {
            partes.add(AssinaturaPreviaNegocial.partes("ITEM", i.chave(), i.anterior(), i.resultante(), i.colaboradoresAtuais()));
            partes.add(AssinaturaPreviaNegocial.partes("CONFIGURACAO", i.chave(), i.lotacaoMunicipio(), i.jornadaSemanal()));
        });
        VersaoContratoNegocialDTO antes = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), p.eventosIgnorados());
        partes.add(AssinaturaPreviaNegocial.partes("ESCALAR", antes.descricao(), antes.tipoContratacaoId(), antes.fornecedorId(),
                antes.cnpjEmpresa(), antes.razaoSocialEmpresa(), antes.nomeFantasiaEmpresa(), antes.dataInicio(), antes.dataFim(), antes.efetivoPrevisto(),
                antes.matriculaVinculoObrigatoria(), antes.matriculaTipoContratacaoObrigatoria(), antes.diaSemanaReduzido(), antes.horasDiaReduzido()));
        for (EventoCicloVidaContrato e : motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()))
            partes.add(AssinaturaPreviaNegocial.partes("EV", e.getId(), e.getFatoLogicoId(), e.getVersaoNegocial(), e.getTipoEvento(), e.getChaveNegocial(), e.getDataEfetiva()));
        if (assinaturaImpacto != null) partes.addAll(assinaturaImpacto.partesPorContrato(contrato.getId(), req.dataEfetiva(), null));
        p.alertas().forEach(alerta -> partes.add(AssinaturaPreviaNegocial.partes("ALERTA", alerta)));
        return AssinaturaPreviaNegocial.sha256("sgt.previa.aditamento.v1", partes);
    }

    private ImpactoComposicaoContratoDTO impactoDTO(ItemPreparado i) {
        long excedente = Math.max(0L, i.colaboradoresAtuais() - i.resultante());
        return new ImpactoComposicaoContratoDTO(
                i.composicaoBase() != null ? i.composicaoBase().getId() : null,
                i.lotacao().getId(), i.lotacao().getNome(),
                i.jornada().getId(), i.jornada().getDescricao(),
                i.cargo().getId(), i.cargo().getNome(),
                i.anterior(), i.variacao(), i.resultante(), i.colaboradoresAtuais(), excedente, excedente > 0);
    }

    private AditamentoContratualDTO paraDTO(AditamentoContratual a) {
        Map<Long, VersaoContratoNegocialDTO> fotografias = a.getContrato() == null ? Map.of()
                : fotografiasPorEvento(motor.listarVersoes(a.getContrato().getId()));
        return paraDTO(a, fotografias);
    }

    private AditamentoContratualDTO paraDTO(AditamentoContratual a, Map<Long, VersaoContratoNegocialDTO> fotografias) {
        return new AditamentoContratualDTO(
                a.getId(), a.getContrato() != null ? a.getContrato().getId() : null,
                a.getEvento() != null ? a.getEvento().getId() : null,
                a.getIdentificacao(), a.getVersaoInstrumento(), a.getDataDocumento(), a.getDataAssinatura(), a.getDataEfetiva(),
                a.getJustificativa(), a.getReferenciaSei(), a.getRegistradoEm(), a.getUsuarioResponsavelId(), a.getUsuarioResponsavelUsername(),
                a.getEvento() != null ? a.getEvento().getSituacaoFato() : SituacaoFatoNegocial.VIGENTE,
                a.getNovaDescricao(), a.getNovoTipoContratacao() != null ? a.getNovoTipoContratacao().getId() : null,
                descricaoTipoFotografada(a, fotografias),
                a.getNovoFornecedor() != null ? a.getNovoFornecedor().getId() : null,
                a.getNovoCnpjEmpresa(), a.getNovaRazaoSocialEmpresa(), a.getNovoNomeFantasiaEmpresa(),
                a.getNovaDataInicio(), a.getNovaDataFim(), a.getNovoEfetivoTotal(), a.getReducaoExcepcional(),
                a.getFundamentoExcecao(), a.getImpactoConfirmado(),
                a.getAlteracoesComposicao().stream().map(i -> {
                    long vinculados = ocupacaoAtual(a.getContrato().getId(), i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId());
                    return new ImpactoComposicaoContratoDTO(
                            i.getComposicaoBase() != null ? i.getComposicaoBase().getId() : null,
                            i.getLotacao().getId(), limpar(i.getLotacaoNomeFotografia()),
                            i.getJornada().getId(), limpar(i.getJornadaDescricaoFotografia()),
                            i.getCargo().getId(), limpar(i.getCargoNomeFotografia()),
                            i.getQuantidadeAnterior(), i.getVariacao(),
                            i.getQuantidadeResultante(), vinculados, Math.max(0L, vinculados - i.getQuantidadeResultante()),
                            vinculados > i.getQuantidadeResultante());
                }).toList(), a.getNovaMatriculaVinculoObrigatoria(), a.getNovoDiaSemanaReduzido(), a.getNovasHorasDiaReduzido(),
                a.getRestaurarPoliticaMatriculaDoTipo(), a.getRemoverConfiguracaoDiaReduzido()
        );
    }

    private Map<Long, VersaoContratoNegocialDTO> fotografiasPorEvento(List<VersaoContratoNegocialDTO> versoes) {
        return motor.fotografiasConhecidasPorEvento(versoes, Instant.now());
    }

    private String descricaoTipoFotografada(AditamentoContratual a, Map<Long, VersaoContratoNegocialDTO> fotografias) {
        if (a.getNovoTipoContratacao() == null) return null;
        VersaoContratoNegocialDTO fotografia = a.getEvento() == null ? null : fotografias.get(a.getEvento().getId());
        String descricao = fotografia == null ? null : limpar(fotografia.tipoContratacaoDescricao());
        // A captura do próprio ato permite criar sua primeira versão sem recorrer ao mestre
        // durante o replay. Campo legado ausente permanece desconhecido, sem retrocarga.
        return descricao != null ? descricao : limpar(a.getNovoTipoDescricaoFotografia());
    }

    public void sincronizarProjecaoAtual(Contrato contrato) {
        ProjecaoNegocial projecao = reconstruirDadosNegociais(contrato, RelogioSistema.hoje(), Instant.now(), Set.of(), null);
        VersaoContratoNegocialDTO atual = projecao.versao();
        // Desconhecimento não é um ato de limpeza da projeção materializada.
        // Apenas valores reconstruídos a partir de fontes conhecidas a atualizam.
        if (atual.descricao() != null) contrato.setDescricao(atual.descricao());
        if (atual.dataInicio() != null) contrato.setDataInicio(atual.dataInicio());
        LocalDate fimAtual = atual.dataFim();
        PosicaoTemporalContratoDTO ciclo = motor.reconstruirPosicao(contrato, RelogioSistema.hoje(), Instant.now());
        for (EventoCicloVidaContratoDTO evento : ciclo.eventosAplicados()) {
            if (evento.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO) {
                fimAtual = evento.dataEfetiva();
            }
        }
        if (fimAtual != null) contrato.setDataFim(fimAtual);
        if (atual.efetivoPrevisto() != null) contrato.setEfetivoPrevisto(atual.efetivoPrevisto());
        if (atual.cnpjEmpresa() != null) contrato.setCnpjEmpresa(atual.cnpjEmpresa());
        if (atual.razaoSocialEmpresa() != null) contrato.setRazaoSocialEmpresa(atual.razaoSocialEmpresa());
        if (atual.nomeFantasiaEmpresa() != null) contrato.setNomeFantasiaEmpresa(atual.nomeFantasiaEmpresa());
        // Uma projeção desconhecida não apaga configuração materializada legada.
        // Limpezas documentadas, por sua vez, são fatos conhecidos e escrevem null.
        if (projecao.politicaContratoConhecida()) contrato.setMatriculaVinculoObrigatoria(atual.matriculaVinculoObrigatoria());
        if (projecao.configuracaoDiaReduzidoConhecida()) {
            contrato.setDiaSemanaReduzido(atual.diaSemanaReduzido());
            contrato.setHorasDiaReduzido(atual.horasDiaReduzido());
        }
        if (atual.tipoContratacaoId() != null) {
            tipoContratacaoRepositorio.findById(atual.tipoContratacaoId()).ifPresent(t -> {
                contrato.setTipoContratacao(t);
                contrato.setTipo(atual.tipoContratacaoDescricao());
            });
        }
        if (atual.fornecedorId() != null) {
            fornecedorRepositorio.findById(atual.fornecedorId()).ifPresent(contrato::setFornecedor);
        } else {
            var provaFornecedor = projecao.provaCampos().get("fornecedorId");
            if (ProvasCamposHistoricos.conhecido(provaFornecedor)
                    && provaFornecedor.estado() == ProvaCampoHistoricoDTO.Estado.AUSENCIA_COMPROVADA) {
                contrato.setFornecedor(null);
            }
        }
        contratoRepositorio.save(contrato);
    }

    private void revalidarFrequenciasContrato(Contrato contrato, LocalDate desde, String motivo) {
        if (revalidacaoFrequenciaTemporalServico != null && contrato != null
                && contrato.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorContrato(contrato.getId(), desde, motivo);
        }
    }

    private boolean administrativamenteAtivo(Contrato contrato) {
        HabilitacaoAdministrativaContrato habilitacao = motor.habilitacaoAdministrativaEm(
                contrato, RelogioSistema.hoje(), Instant.now());
        return habilitacao == HabilitacaoAdministrativaContrato.ATIVO;
    }

    private Contrato obterContrato(Long id) {
        return contratoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterContratoParaAtualizacao(Long id) {
        return contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterPublicadoNoEscopo(Long id, UsuarioAplicacao user, boolean escrita) {
        return obterPublicadoNoEscopo(id, user, escrita, false);
    }

    private Contrato obterPublicadoNoEscopo(Long id, UsuarioAplicacao user, boolean escrita, boolean avaliarProrrogacaoInativa) {
        Contrato c = escrita ? obterContratoParaAtualizacao(id) : obterContrato(id);
        acesso.exigirAcessoContrato(user, c);
        if (escrita) {
            acesso.exigirGestaoContrato(user);
            if (!acesso.ehGestaoLocal(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Aditamento é ato negocial do Gestor Local; perfis técnicos/consulta não executam esta operação.");
            }
        }
        EstadoNegocialContrato estado = motor.estadoNegocialEm(c, RelogioSistema.hoje(), Instant.now());
        if ((estado != null ? estado : c.getEstadoNegocial()) != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aditamento exige contrato publicado válido.");
        }
        if (escrita && !administrativamenteAtivo(c) && !avaliarProrrogacaoInativa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo não aceita aditamento.");
        }
        return c;
    }

    private AditamentoContratual obterAditamentoNoContrato(Long id, Long contratoId) {
        return aditamentoRepositorio.buscarCompletoPorId(id)
                .filter(a -> a.getContrato() != null && Objects.equals(a.getContrato().getId(), contratoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aditamento não encontrado."));
    }

    private boolean hasQuantitativo(AditamentoContratualRequisicao req) {
        return req.novoEfetivoTotal() != null || req.alteracoesComposicao() != null && !req.alteracoesComposicao().isEmpty();
    }

    private boolean hasAlgumaAlteracao(AditamentoContratualRequisicao req) {
        return hasQuantitativo(req) || req.novaDescricao() != null || req.novoTipoContratacaoId() != null
                || req.novoFornecedorId() != null || req.novoCnpjEmpresa() != null || req.novaRazaoSocialEmpresa() != null
                || req.novoNomeFantasiaEmpresa() != null || req.novaDataInicio() != null || req.novaDataFim() != null
                || req.novaMatriculaVinculoObrigatoria() != null || req.novoDiaSemanaReduzido() != null
                || req.novasHorasDiaReduzido() != null || Boolean.TRUE.equals(req.restaurarPoliticaMatriculaDoTipo())
                || Boolean.TRUE.equals(req.removerConfiguracaoDiaReduzido());
    }

    private static String normalizarChave(String s) {
        return s == null ? "SEM_ID" : s.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private static String limpar(String s) {
        if (s == null) return null;
        String r = s.trim();
        return r.isBlank() ? null : r;
    }

    private long ocupacaoAtual(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId) {
        return ocupacaoEm(contratoId, lotacaoId, jornadaId, cargoId, RelogioSistema.hoje());
    }

    private long ocupacaoEm(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, LocalDate data) {
        return vinculoContratualServico == null ? 0
                : vinculoContratualServico.contarOcupacao(contratoId, lotacaoId, jornadaId, cargoId, data);
    }

    private static String normalizarCnpj(String s) {
        return s == null ? null : s.replaceAll("\\D", "");
    }
}
