package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AditamentoContratualServico {

    private record ItemPreparado(
            ComposicaoContratual composicaoBase,
            Lotacao lotacao,
            Jornada jornada,
            Cargo cargo,
            int anterior,
            int resultante,
            long colaboradoresAtuais
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
            List<String> alertas
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

    public List<AditamentoContratualDTO> listar(Long contratoId) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterContrato(contratoId);
        acesso.exigirAcessoContrato(user, contrato);
        return aditamentoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId)
                .stream().map(this::paraDTO).toList();
    }

    public PreviaAditamentoContratualDTO previa(Long contratoId, AditamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, false);
        validarCamposBasicos(contrato, req);
        Preparacao p = preparar(contrato, req);
        validarAlteracoesNaoQuantitativas(contrato, req, p.eventosIgnorados());
        return paraPrevia(contrato, req, p);
    }

    @Transactional
    public AditamentoContratualDTO registrar(Long contratoId, AditamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true);
        LocalDate fimContratoAntes = contrato.getDataFim();
        validarCamposBasicos(contrato, req);
        Preparacao p = preparar(contrato, req);
        validarAlteracoesNaoQuantitativas(contrato, req, p.eventosIgnorados());
        validarTokenPrevia(contrato, req, p);
        validarImpactoEExcecao(contrato, user, req, p);
        validarConflitosMesmaData(contrato, req, p.itens());

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
        a.setNovoEfetivoTotal(hasQuantitativo(req) ? p.efetivoResultante() : req.novoEfetivoTotal());
        a.setReducaoExcepcional(Boolean.TRUE.equals(req.reducaoExcepcional()));
        a.setFundamentoExcecao(limpar(req.fundamentoExcecao()));
        a.setImpactoConfirmado(Boolean.TRUE.equals(req.confirmarImpacto()));

        if (req.novoTipoContratacaoId() != null) {
            a.setNovoTipoContratacao(obterTipoContratacao(contrato, req.novoTipoContratacaoId(), req.dataEfetiva()));
        }
        if (req.novoFornecedorId() != null) {
            a.setNovoFornecedor(fornecedorRepositorio.findById(req.novoFornecedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor não encontrado.")));
        }
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
            vinculoContratualServico.preservarFimAntigoEmProrrogacaoTardia(
                    contrato, fimContratoAntes, contrato.getDataFim(), user,
                    "Aditamento de prorrogação registrado após o fim anterior; vínculos não são reabertos automaticamente.");
        }
        revalidarFrequenciasContrato(contrato, req.dataEfetiva(),
                "Aditamento/retificação contratual retroativa exige revalidação das frequências");
        registrarConflitosPosteriores(contrato, req.dataEfetiva(), "ADITAMENTO");

        return paraDTO(salvo);
    }

    @Transactional
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
                req.tokenPrevia()
        );
        return registrar(contratoId, ajustada);
    }

    @Transactional
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
        return paraDTO(obterAditamentoNoContrato(aditamentoId, contratoId));
    }

    /**
     * Reconstrói os dados escalares do contrato pela fotografia publicada + deltas
     * de aditamentos válidos. Isto evita que um fato retroativo apague mudanças
     * posteriores que continuam válidas.
     */
    public VersaoContratoNegocialDTO projetarVersaoContrato(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        Set<Long> ignorar = eventosIgnorados == null ? Set.of() : eventosIgnorados;

        List<VersaoContratoNegocialDTO> versoes = motor.listarVersoes(contrato.getId());
        VersaoContratoNegocialDTO base = versoes.stream()
                .filter(v -> v.eventoOrigemId() != null)
                .min(Comparator.comparingInt(v -> Optional.ofNullable(v.numeroVersao()).orElse(Integer.MAX_VALUE)))
                .orElse(null);

        String numero = base != null ? base.numeroContrato() : contrato.getNumeroContrato();
        Integer ano = base != null ? base.anoContrato() : contrato.getAnoContrato();
        String descricao = base != null ? base.descricao() : contrato.getDescricao();
        Long tipoId = base != null ? base.tipoContratacaoId() : contrato.getTipoContratacao() != null ? contrato.getTipoContratacao().getId() : null;
        String tipoDesc = base != null ? base.tipoContratacaoDescricao() : contrato.getTipo();
        Long fornecedorId = base != null ? base.fornecedorId() : contrato.getFornecedor() != null ? contrato.getFornecedor().getId() : null;
        String cnpj = base != null ? base.cnpjEmpresa() : contrato.getCnpjEmpresa();
        String razao = base != null ? base.razaoSocialEmpresa() : contrato.getRazaoSocialEmpresa();
        String fantasia = base != null ? base.nomeFantasiaEmpresa() : contrato.getNomeFantasiaEmpresa();
        LocalDate inicio = base != null ? base.dataInicio() : contrato.getDataInicio();
        LocalDate fim = base != null ? base.dataFim() : contrato.getDataFim();
        Integer efetivo = base != null ? base.efetivoPrevisto() : contrato.getEfetivoOriginalPublicado() != null
                ? contrato.getEfetivoOriginalPublicado() : contrato.getEfetivoPrevisto();
        Long gestaoId = base != null ? base.gestaoLocalId() : contrato.getGestaoLocal() != null ? contrato.getGestaoLocal().getId() : null;
        String gestaoNome = base != null ? base.gestaoLocalNome() : contrato.getGestaoLocal() != null ? contrato.getGestaoLocal().getNome() : null;

        Map<Long, AditamentoContratual> porEvento = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(a -> a.getEvento() != null && a.getEvento().getId() != null)
                .collect(Collectors.toMap(a -> a.getEvento().getId(), a -> a, (a, b) -> b));

        EventoCicloVidaContrato ultimoEvento = null;
        int qtdAplicados = 0;
        for (EventoCicloVidaContrato e : motor.eventosValidosOrdenados(contrato.getId(), data, conhecimento)) {
            if (e.getId() == null || ignorar.contains(e.getId()) || e.getTipoEvento() != TipoEventoCicloVidaContrato.ADITAMENTO) continue;
            AditamentoContratual a = porEvento.get(e.getId());
            if (a == null) continue;
            if (a.getNovaDescricao() != null) descricao = a.getNovaDescricao();
            if (a.getNovoTipoContratacao() != null) {
                tipoId = a.getNovoTipoContratacao().getId();
                tipoDesc = a.getNovoTipoContratacao().getDescricao();
            }
            if (a.getNovoFornecedor() != null) fornecedorId = a.getNovoFornecedor().getId();
            if (a.getNovoCnpjEmpresa() != null) cnpj = a.getNovoCnpjEmpresa();
            if (a.getNovaRazaoSocialEmpresa() != null) razao = a.getNovaRazaoSocialEmpresa();
            if (a.getNovoNomeFantasiaEmpresa() != null) fantasia = a.getNovoNomeFantasiaEmpresa();
            if (a.getNovaDataInicio() != null) inicio = a.getNovaDataInicio();
            if (a.getNovaDataFim() != null) fim = a.getNovaDataFim();
            if (a.getNovoEfetivoTotal() != null) efetivo = a.getNovoEfetivoTotal();
            ultimoEvento = e;
            qtdAplicados++;
        }

        return new VersaoContratoNegocialDTO(
                null, 1 + qtdAplicados, ultimoEvento != null ? ultimoEvento.getId() : base != null ? base.eventoOrigemId() : null,
                ultimoEvento != null ? ultimoEvento.getDataEfetiva() : base != null ? base.dataEfetiva() : contrato.getDataInicio(),
                ultimoEvento != null ? ultimoEvento.getRegistradoEm() : base != null ? base.registradoEm() : contrato.getPublicadoEm(),
                numero, ano, descricao, tipoId, tipoDesc, fornecedorId, cnpj, razao, fantasia,
                inicio, fim, efetivo, gestaoId, gestaoNome
        );
    }

    private Preparacao preparar(Contrato contrato, AditamentoContratualRequisicao req) {
        Set<Long> ignorados = eventosIgnoradosPelaPrecedencia(req);
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado =
                composicaoTemporal.projetarEntidades(contrato, req.dataEfetiva(), Instant.now(), ignorados);
        int totalAnterior = estado.stream().mapToInt(ComposicaoContratualTemporalServico.ComposicaoProjetada::efetivoPrevisto).sum();

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
                validarCatalogosDoContrato(contrato, lotacao, jornada, cargo, req.dataEfetiva());

                String chave = ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(lotacao.getId(), jornada.getId(), cargo.getId());
                if (!chaves.add(chave)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A mesma composição foi informada mais de uma vez no aditamento.");
                }
                long vinculados = ocupacaoEm(contrato.getId(), lotacao.getId(), jornada.getId(), cargo.getId(),
                        req.dataEfetiva());
                itens.add(new ItemPreparado(base, lotacao, jornada, cargo, anterior, itemReq.quantidadeResultante(), vinculados));
            }
        }

        int totalResultante = totalAnterior + itens.stream().mapToInt(ItemPreparado::variacao).sum();
        if (req.novoEfetivoTotal() != null && req.novoEfetivoTotal() != totalResultante) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O efetivo total informado (" + req.novoEfetivoTotal() + ") não corresponde à soma projetada das composições (" + totalResultante + ").");
        }
        if (itens.isEmpty() && req.novoEfetivoTotal() != null && req.novoEfetivoTotal() != totalAnterior) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Alteração do efetivo total exige informar as alterações das composições correspondentes.");
        }

        int original = Optional.ofNullable(contrato.getEfetivoOriginalPublicado()).orElse(totalAnterior);
        int piso = RegraEfetivoContratual.pisoNormal(original);
        boolean reducao = totalResultante < totalAnterior;
        boolean abaixoPiso = reducao && totalResultante < piso;
        boolean impacto = itens.stream().anyMatch(i -> i.colaboradoresAtuais() > i.resultante());
        List<String> alertas = new ArrayList<>();
        if (impacto) alertas.add("Há composição com ocupação atual superior ao quantitativo resultante; pessoas não serão movidas automaticamente.");
        if (abaixoPiso) alertas.add("A redução ultrapassa o limite normal de 25% do efetivo originalmente publicado e exige fluxo excepcional.");
        if (req.dataEfetiva().isBefore(RelogioSistema.hoje())) alertas.add("Aditamento retroativo: frequências, relatórios e eventos posteriores deverão ser reavaliados pelas etapas dependentes.");

        return new Preparacao(List.copyOf(itens), totalAnterior, totalResultante, original, piso,
                reducao, abaixoPiso, impacto, ignorados, List.copyOf(alertas));
    }

    private void validarImpactoEExcecao(Contrato contrato, UsuarioAplicacao user,
                                        AditamentoContratualRequisicao req, Preparacao p) {
        if (p.impactoOcupacao()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição resultante ficaria abaixo da ocupação válida na data efetiva. "
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
                    contrato, contrato.getGestaoLocal(), req.fundamentoExcecao(),
                    "Contrato", contrato.getId());
        }
    }

    private void registrarConflitosPosteriores(Contrato contrato, LocalDate desde, String origem) {
        if (pendencias == null) return;
        LocalDate ate = contrato.getDataFim() == null ? LocalDate.MAX : contrato.getDataFim();
        for (var conflito : composicaoTemporal.diagnosticarConflitos(contrato, ate, Instant.now()))
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

    private void validarAlteracoesNaoQuantitativas(Contrato contrato, AditamentoContratualRequisicao req, Set<Long> ignorados) {
        VersaoContratoNegocialDTO antes = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), ignorados);
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
        if (req.novoFornecedorId() != null && fornecedorRepositorio.findById(req.novoFornecedorId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor não encontrado.");
        }
    }

    private void validarConflitosMesmaData(Contrato contrato, AditamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> novasChaves = chavesAfetadas(req, itens);
        List<AditamentoContratual> mesmosDia = aditamentoRepositorio.findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva());
        Set<Long> ativos = motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()).stream()
                .filter(e -> e.getDataEfetiva().equals(req.dataEfetiva()))
                .map(EventoCicloVidaContrato::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
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
        if (conflitosAditamento.isEmpty() && conflitosRemanejamento.isEmpty()) return;
        if (req.eventoPrecedenteId() == null || req.relacaoPrecedencia() == null) {
            String idsA = conflitosAditamento.stream().map(a -> String.valueOf(a.getEvento().getId())).collect(Collectors.joining(", "));
            String idsR = conflitosRemanejamento.stream().map(r -> String.valueOf(r.getEvento().getId())).collect(Collectors.joining(", "));
            String ids = String.join(", ", java.util.stream.Stream.of(idsA, idsR).filter(x -> !x.isBlank()).toList());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existe fato contratual conflitante na mesma data. Informe precedência negocial explícita. Eventos: " + ids);
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campos conflitantes não admitem MANTEM_COEXISTE.");
        }
        boolean alvoConflitante = conflitosAditamento.stream().anyMatch(a -> Objects.equals(a.getEvento().getId(), req.eventoPrecedenteId()))
                || conflitosRemanejamento.stream().anyMatch(r -> Objects.equals(r.getEvento().getId(), req.eventoPrecedenteId()));
        if (!alvoConflitante) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O evento precedente informado não é o fato conflitante da mesma data.");
        }
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
                                             LocalDate dataEfetiva) {
        Long gestaoId = contrato.getGestaoLocal() != null ? contrato.getGestaoLocal().getId() : null;
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
        if (!jornadaPermitida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno não está vinculado à Lotação informada.");
        }
    }

    private TipoContratacao obterTipoContratacao(Contrato contrato, Long id, LocalDate dataEfetiva) {
        TipoContratacao tipo = tipoContratacaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação não encontrado."));
        if (tipo.getGestaoLocal() == null || contrato.getGestaoLocal() == null
                || !Objects.equals(tipo.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação pertence a outra Gestão Local.");
        }
        if (!catalogoAtivoEm(TipoCatalogoTemporal.TIPO_CONTRATACAO, tipo.getId(), dataEfetiva, tipo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tipo de Contratação inativo não pode ser usado em evento atual ou futuro.");
        }
        return tipo;
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
        List<String> partes = new ArrayList<>();
        partes.add("ADITAMENTO");
        partes.add(AssinaturaPreviaNegocial.valor(contrato.getId()));
        partes.add(AssinaturaPreviaNegocial.valor(contrato.getVersion()));
        partes.add(AssinaturaPreviaNegocial.valor(req.identificacao()));
        partes.add(AssinaturaPreviaNegocial.valor(req.dataDocumento()));
        partes.add(AssinaturaPreviaNegocial.valor(req.dataAssinatura()));
        partes.add(AssinaturaPreviaNegocial.valor(req.dataEfetiva()));
        partes.add(AssinaturaPreviaNegocial.valor(req.justificativa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.referenciaSei()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novaDescricao()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novoTipoContratacaoId()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novoFornecedorId()));
        partes.add(AssinaturaPreviaNegocial.valor(normalizarCnpj(req.novoCnpjEmpresa())));
        partes.add(AssinaturaPreviaNegocial.valor(req.novaRazaoSocialEmpresa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novoNomeFantasiaEmpresa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novaDataInicio()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novaDataFim()));
        partes.add(AssinaturaPreviaNegocial.valor(req.novoEfetivoTotal()));
        partes.add(AssinaturaPreviaNegocial.valor(req.reducaoExcepcional()));
        partes.add(AssinaturaPreviaNegocial.valor(req.fundamentoExcecao()));
        partes.add(AssinaturaPreviaNegocial.valor(req.eventoPrecedenteId()));
        partes.add(AssinaturaPreviaNegocial.valor(req.relacaoPrecedencia()));
        partes.add(AssinaturaPreviaNegocial.valor(req.ordemDocumental()));
        partes.add("TOTAL:" + p.efetivoAnterior() + ":" + p.efetivoResultante() + ":" + p.efetivoOriginal() + ":" + p.pisoNormal());
        p.itens().stream().sorted(Comparator.comparing(ItemPreparado::chave)).forEach(i ->
                partes.add("ITEM:" + i.chave() + ":" + i.anterior() + ":" + i.resultante() + ":" + i.colaboradoresAtuais()));
        VersaoContratoNegocialDTO antes = projetarVersaoContrato(contrato, req.dataEfetiva(), Instant.now(), p.eventosIgnorados());
        partes.add("ESCALAR:" + AssinaturaPreviaNegocial.valor(antes.descricao()) + ":" +
                AssinaturaPreviaNegocial.valor(antes.tipoContratacaoId()) + ":" + AssinaturaPreviaNegocial.valor(antes.fornecedorId()) + ":" +
                AssinaturaPreviaNegocial.valor(antes.cnpjEmpresa()) + ":" + AssinaturaPreviaNegocial.valor(antes.razaoSocialEmpresa()) + ":" +
                AssinaturaPreviaNegocial.valor(antes.nomeFantasiaEmpresa()) + ":" + AssinaturaPreviaNegocial.valor(antes.dataInicio()) + ":" +
                AssinaturaPreviaNegocial.valor(antes.dataFim()) + ":" + AssinaturaPreviaNegocial.valor(antes.efetivoPrevisto()));
        for (EventoCicloVidaContrato e : motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now())) {
            partes.add("EV:" + AssinaturaPreviaNegocial.valor(e.getId()) + ":" + AssinaturaPreviaNegocial.valor(e.getFatoLogicoId()) + ":" +
                    AssinaturaPreviaNegocial.valor(e.getVersaoNegocial()) + ":" + AssinaturaPreviaNegocial.valor(e.getTipoEvento()) + ":" +
                    AssinaturaPreviaNegocial.valor(e.getChaveNegocial()) + ":" + AssinaturaPreviaNegocial.valor(e.getDataEfetiva()));
        }
        return AssinaturaPreviaNegocial.sha256(partes);
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
        return new AditamentoContratualDTO(
                a.getId(), a.getContrato() != null ? a.getContrato().getId() : null,
                a.getEvento() != null ? a.getEvento().getId() : null,
                a.getIdentificacao(), a.getVersaoInstrumento(), a.getDataDocumento(), a.getDataAssinatura(), a.getDataEfetiva(),
                a.getJustificativa(), a.getReferenciaSei(), a.getRegistradoEm(), a.getUsuarioResponsavelId(), a.getUsuarioResponsavelUsername(),
                a.getEvento() != null ? a.getEvento().getSituacaoFato() : SituacaoFatoNegocial.VIGENTE,
                a.getNovaDescricao(), a.getNovoTipoContratacao() != null ? a.getNovoTipoContratacao().getId() : null,
                a.getNovoTipoContratacao() != null ? a.getNovoTipoContratacao().getDescricao() : null,
                a.getNovoFornecedor() != null ? a.getNovoFornecedor().getId() : null,
                a.getNovoCnpjEmpresa(), a.getNovaRazaoSocialEmpresa(), a.getNovoNomeFantasiaEmpresa(),
                a.getNovaDataInicio(), a.getNovaDataFim(), a.getNovoEfetivoTotal(), a.getReducaoExcepcional(),
                a.getFundamentoExcecao(), a.getImpactoConfirmado(),
                a.getAlteracoesComposicao().stream().map(i -> {
                    long vinculados = ocupacaoAtual(a.getContrato().getId(), i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId());
                    return new ImpactoComposicaoContratoDTO(
                            i.getComposicaoBase() != null ? i.getComposicaoBase().getId() : null,
                            i.getLotacao().getId(), i.getLotacao().getNome(), i.getJornada().getId(), i.getJornada().getDescricao(),
                            i.getCargo().getId(), i.getCargo().getNome(), i.getQuantidadeAnterior(), i.getVariacao(),
                            i.getQuantidadeResultante(), vinculados, Math.max(0L, vinculados - i.getQuantidadeResultante()),
                            vinculados > i.getQuantidadeResultante());
                }).toList()
        );
    }

    private void sincronizarProjecaoAtual(Contrato contrato) {
        VersaoContratoNegocialDTO atual = projetarVersaoContrato(contrato, RelogioSistema.hoje(), Instant.now(), Set.of());
        contrato.setDescricao(atual.descricao());
        contrato.setDataInicio(atual.dataInicio());
        LocalDate fimAtual = atual.dataFim();
        PosicaoTemporalContratoDTO ciclo = motor.reconstruirPosicao(contrato, RelogioSistema.hoje(), Instant.now());
        for (EventoCicloVidaContratoDTO evento : ciclo.eventosAplicados()) {
            if (evento.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO) {
                fimAtual = evento.dataEfetiva();
            }
        }
        contrato.setDataFim(fimAtual);
        contrato.setEfetivoPrevisto(Optional.ofNullable(atual.efetivoPrevisto()).orElse(0));
        contrato.setCnpjEmpresa(atual.cnpjEmpresa());
        contrato.setRazaoSocialEmpresa(atual.razaoSocialEmpresa());
        contrato.setNomeFantasiaEmpresa(atual.nomeFantasiaEmpresa());
        if (atual.tipoContratacaoId() != null) {
            tipoContratacaoRepositorio.findById(atual.tipoContratacaoId()).ifPresent(t -> {
                contrato.setTipoContratacao(t);
                contrato.setTipo(atual.tipoContratacaoDescricao());
            });
        }
        if (atual.fornecedorId() != null) fornecedorRepositorio.findById(atual.fornecedorId()).ifPresent(contrato::setFornecedor);
        contratoRepositorio.save(contrato);
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

    private Contrato obterContratoParaAtualizacao(Long id) {
        return contratoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }

    private Contrato obterPublicadoNoEscopo(Long id, UsuarioAplicacao user, boolean escrita) {
        Contrato c = escrita ? obterContratoParaAtualizacao(id) : obterContrato(id);
        acesso.exigirAcessoContrato(user, c);
        if (escrita) {
            acesso.exigirGestaoContrato(user);
            if (!acesso.ehGestaoLocal(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Aditamento é ato negocial do Gestor Local; perfis técnicos/consulta não executam esta operação.");
            }
        }
        if (c.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aditamento exige contrato publicado válido.");
        }
        if (escrita && !Boolean.TRUE.equals(c.getAtivo())) {
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
                || req.novoNomeFantasiaEmpresa() != null || req.novaDataInicio() != null || req.novaDataFim() != null;
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
