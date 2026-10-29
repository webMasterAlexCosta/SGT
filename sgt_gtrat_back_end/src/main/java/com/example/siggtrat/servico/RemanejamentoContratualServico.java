package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.regra.RegraDisponibilidadeCatalogo;
import com.example.siggtrat.regra.RegraEfetivoContratual;
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
public class RemanejamentoContratualServico {

    private record ItemPreparado(
            ComposicaoContratual composicaoBase,
            Lotacao lotacao,
            Jornada jornada,
            Cargo cargo,
            int anterior,
            int variacao,
            int resultante,
            long colaboradoresAtuais
    ) {
        String chave() {
            return ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                    lotacao.getId(), jornada.getId(), cargo.getId());
        }
    }

    private record Preparacao(
            List<ItemPreparado> itens,
            int totalAntes,
            int totalDepois,
            int somaVariacoes,
            boolean impacto,
            Set<Long> eventosIgnorados,
            List<String> alertas
    ) {}

    private final ContratoRepositorio contratoRepositorio;
    private final RemanejamentoContratualRepositorio remanejamentoRepositorio;
    private final AditamentoContratualRepositorio aditamentoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalContratoServico motor;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private VinculoContratualServico vinculoContratualServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private PendenciaAdministrativaServico pendencias;

    public RemanejamentoContratualServico(
            ContratoRepositorio contratoRepositorio,
            RemanejamentoContratualRepositorio remanejamentoRepositorio,
            AditamentoContratualRepositorio aditamentoRepositorio,
            ComposicaoContratualRepositorio composicaoRepositorio,
            AcessoOrganizacionalServico acesso,
            MotorTemporalContratoServico motor,
            ComposicaoContratualTemporalServico composicaoTemporal) {
        this.contratoRepositorio = contratoRepositorio;
        this.remanejamentoRepositorio = remanejamentoRepositorio;
        this.aditamentoRepositorio = aditamentoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.acesso = acesso;
        this.motor = motor;
        this.composicaoTemporal = composicaoTemporal;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDocumentos(DocumentoComprobatorioServico documentoComprobatorioServico) {
        this.documentoComprobatorioServico = documentoComprobatorioServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarVinculosCanonicos(VinculoContratualServico vinculoContratualServico) {
        this.vinculoContratualServico = vinculoContratualServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarPendencias(PendenciaAdministrativaServico pendencias) {
        this.pendencias = pendencias;
    }

    public List<RemanejamentoContratualDTO> listar(Long contratoId) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterContrato(contratoId);
        acesso.exigirAcessoContrato(user, contrato);
        return remanejamentoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId)
                .stream().map(this::paraDTO).toList();
    }

    public PreviaRemanejamentoContratualDTO previa(Long contratoId, RemanejamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, false);
        validarBasico(req);
        Preparacao p = preparar(contrato, req);
        return paraPrevia(contrato, req, p);
    }

    @Transactional
    public RemanejamentoContratualDTO registrar(Long contratoId, RemanejamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true);
        validarBasico(req);
        Preparacao p = preparar(contrato, req);
        validarTokenPrevia(contrato, req, p);
        if (!RegraEfetivoContratual.remanejamentoPreservaTotal(
                p.itens().stream().map(ItemPreparado::variacao).toList())
                || p.totalAntes() != p.totalDepois()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Remanejamento deve preservar o efetivo total; a soma das variações deve ser zero.");
        }
        if (p.impacto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O remanejamento deixaria composição abaixo da ocupação válida na data efetiva. "
                            + "Transfira, desligue ou reprograme os vínculos afetados antes de concluir.");
        }
        validarConflitosMesmaData(contrato, req, p.itens());
        int versao = validarIdentificacaoECalcularVersao(contrato, req);

        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                req.dataEfetiva(), req.dataEfetiva(), req.justificativa(), req.referenciaSei(),
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental());
        EventoCicloVidaContrato evento = motor.registrarEvento(
                contrato, TipoEventoCicloVidaContrato.REMANEJAMENTO, acao, user,
                "total=" + p.totalAntes(), "total=" + p.totalDepois(),
                "REMANEJAMENTO:" + normalizarChave(req.identificacao()) + ":V" + versao);

        RemanejamentoContratual r = new RemanejamentoContratual();
        r.setContrato(contrato);
        r.setEvento(evento);
        r.setIdentificacao(req.identificacao());
        r.setVersaoInstrumento(versao);
        r.setDataEfetiva(req.dataEfetiva());
        r.setJustificativa(req.justificativa());
        r.setReferenciaSei(req.referenciaSei());
        r.setUsuarioResponsavelId(user != null ? user.getId() : null);
        r.setUsuarioResponsavelUsername(user != null ? user.getUsername() : null);
        for (ItemPreparado ip : p.itens()) {
            RemanejamentoComposicao item = new RemanejamentoComposicao();
            item.setComposicaoBase(ip.composicaoBase());
            item.setLotacao(ip.lotacao());
            item.setJornada(ip.jornada());
            item.setCargo(ip.cargo());
            item.setQuantidadeAnterior(ip.anterior());
            item.setVariacao(ip.variacao());
            item.setQuantidadeResultante(ip.resultante());
            r.adicionarItem(item);
        }
        RemanejamentoContratual salvo = remanejamentoRepositorio.save(r);
        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(contrato, TipoFatoDocumental.REMANEJAMENTO,
                    req.identificacao(), salvo.getId(), req.referenciaSei(), req.dataEfetiva(), user);
        }
        registrarConflitosPosteriores(contrato, req.dataEfetiva(), "REMANEJAMENTO");
        return paraDTO(salvo);
    }

    @Transactional
    public RemanejamentoContratualDTO retificar(Long contratoId, Long remanejamentoId, RemanejamentoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        obterPublicadoNoEscopo(contratoId, user, true);
        RemanejamentoContratual original = obterRemanejamentoNoContrato(remanejamentoId, contratoId);
        if (original.getEvento() == null || original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente remanejamento vigente pode ser retificado.");
        }
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da retificação não informados.");
        RemanejamentoContratualRequisicao ajustada = new RemanejamentoContratualRequisicao(
                original.getIdentificacao(), req.dataEfetiva(), req.justificativa(), req.referenciaSei(),
                req.confirmarImpacto(), req.itens(), original.getEvento().getId(),
                TipoRelacaoPrecedenciaNegocial.RETIFICA,
                req.ordemDocumental() != null ? req.ordemDocumental() : original.getEvento().getOrdemDocumental(),
                req.tokenPrevia());
        return registrar(contratoId, ajustada);
    }

    @Transactional
    public RemanejamentoContratualDTO cancelarProgramado(Long contratoId, Long remanejamentoId, AcaoCicloVidaContratoDTO acaoCancelamento) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Contrato contrato = obterPublicadoNoEscopo(contratoId, user, true);
        RemanejamentoContratual original = obterRemanejamentoNoContrato(remanejamentoId, contratoId);
        if (original.getEvento() == null || original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remanejamento não está vigente para cancelamento.");
        }
        if (!original.getDataEfetiva().isAfter(RelogioSistema.hoje())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente remanejamento futuro pode ser cancelado antes de produzir efeito.");
        }
        if (acaoCancelamento == null || acaoCancelamento.motivo() == null || acaoCancelamento.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o motivo do cancelamento.");
        }
        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(
                original.getDataEfetiva(),
                acaoCancelamento.dataAto() != null ? acaoCancelamento.dataAto() : RelogioSistema.hoje(),
                acaoCancelamento.motivo(), acaoCancelamento.referenciaSei(),
                original.getEvento().getId(), TipoRelacaoPrecedenciaNegocial.CANCELA,
                acaoCancelamento.ordemDocumental());
        motor.registrarEvento(
                contrato, TipoEventoCicloVidaContrato.CANCELAMENTO_REMANEJAMENTO, acao, user,
                "REMANEJAMENTO_VIGENTE", "REMANEJAMENTO_CANCELADO",
                "CANCELAMENTO_REMANEJAMENTO:" + original.getId());
        return paraDTO(obterRemanejamentoNoContrato(remanejamentoId, contratoId));
    }

    private Preparacao preparar(Contrato contrato, RemanejamentoContratualRequisicao req) {
        validarDataDentroVigencia(contrato, req.dataEfetiva());
        Set<Long> ignorados = eventosIgnorados(req);
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado =
                composicaoTemporal.projetarEntidades(contrato, req.dataEfetiva(), Instant.now(), ignorados);
        int totalAntes = estado.stream().mapToInt(ComposicaoContratualTemporalServico.ComposicaoProjetada::efetivoPrevisto).sum();
        if (req.itens() == null || req.itens().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Remanejamento deve envolver pelo menos duas composições existentes.");
        }
        Set<String> chaves = new HashSet<>();
        List<ItemPreparado> itens = new ArrayList<>();
        int soma = 0;
        for (RemanejamentoComposicaoRequisicao ir : req.itens()) {
            if (ir == null || ir.variacao() == null || ir.variacao() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada item do remanejamento deve possuir variação diferente de zero.");
            }
            var proj = composicaoTemporal.localizar(estado, ir.composicaoId(), ir.lotacaoId(), ir.jornadaId(), ir.cargoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Remanejamento só pode usar composição já válida na data efetiva."));
            validarCatalogos(proj, req.dataEfetiva());
            if (!chaves.add(proj.chave())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A mesma composição foi informada mais de uma vez no remanejamento.");
            }
            int resultante = proj.efetivoPrevisto() + ir.variacao();
            if (resultante < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Remanejamento não pode resultar em quantitativo negativo.");
            }
            ComposicaoContratual base = proj.composicaoBaseId() != null
                    ? composicaoRepositorio.findById(proj.composicaoBaseId()).orElse(null) : null;
            long vinculados = ocupacaoEm(contrato.getId(), proj.lotacao().getId(), proj.jornada().getId(),
                    proj.cargo().getId(), req.dataEfetiva());
            itens.add(new ItemPreparado(base, proj.lotacao(), proj.jornada(), proj.cargo(),
                    proj.efetivoPrevisto(), ir.variacao(), resultante, vinculados));
            soma += ir.variacao();
        }
        int totalDepois = totalAntes + soma;
        boolean impacto = itens.stream().anyMatch(i -> i.colaboradoresAtuais() > i.resultante());
        List<String> alertas = new ArrayList<>();
        if (soma != 0) alertas.add("A soma das variações é " + soma + "; remanejamento válido exige soma zero.");
        if (impacto) alertas.add("Há composição que ficará abaixo da ocupação atual; pessoas não serão movidas automaticamente.");
        if (req.dataEfetiva().isBefore(RelogioSistema.hoje())) alertas.add("Remanejamento retroativo exige reavaliação dos efeitos posteriores dependentes.");
        return new Preparacao(List.copyOf(itens), totalAntes, totalDepois, soma, impacto, ignorados, List.copyOf(alertas));
    }

    private void validarCatalogos(ComposicaoContratualTemporalServico.ComposicaoProjetada composicao,
                                   LocalDate dataEfetiva) {
        if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, composicao.lotacao().getId(), dataEfetiva, composicao.lotacao().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, composicao.jornada().getId(), dataEfetiva, composicao.jornada().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, composicao.cargo().getId(), dataEfetiva, composicao.cargo().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Catálogo inativo não pode ser utilizado em remanejamento atual ou futuro.");
        }
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : RegraDisponibilidadeCatalogo.permiteUso(data, atual);
    }

    private void registrarConflitosPosteriores(Contrato contrato, LocalDate desde, String origem) {
        if (pendencias == null) return;
        LocalDate ate = contrato.getDataFim() == null ? LocalDate.MAX : contrato.getDataFim();
        for (var conflito : composicaoTemporal.diagnosticarConflitos(contrato, ate, Instant.now()))
            if (conflito.dataEfetiva() == null || !conflito.dataEfetiva().isBefore(desde))
                pendencias.registrarConflitoReplay(contrato, conflito, origem);
    }


    private void validarDataDentroVigencia(Contrato contrato, LocalDate dataEfetiva) {
        if (dataEfetiva == null) return;
        PosicaoTemporalContratoDTO posicao = motor.reconstruirPosicao(contrato, dataEfetiva, Instant.now());
        LocalDate inicio = posicao.vigenciaInicio();
        LocalDate fim = posicao.vigenciaFim();
        if ((inicio != null && dataEfetiva.isBefore(inicio)) || (fim != null && dataEfetiva.isAfter(fim))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Remanejamento só pode produzir efeito dentro da vigência reconstruída do contrato.");
        }
    }

    private void validarBasico(RemanejamentoContratualRequisicao req) {
        if (req == null || req.identificacao() == null || req.identificacao().isBlank()
                || req.dataEfetiva() == null || req.justificativa() == null || req.justificativa().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Identificação, data efetiva e justificativa do remanejamento são obrigatórias.");
        }
        if ((req.eventoPrecedenteId() == null) != (req.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos.");
        }
    }

    private void validarConflitosMesmaData(Contrato contrato, RemanejamentoContratualRequisicao req, List<ItemPreparado> itens) {
        Set<String> novas = itens.stream().map(ItemPreparado::chave).collect(Collectors.toSet());
        List<RemanejamentoContratual> mesmosDia = remanejamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva());
        Set<Long> ativos = motor.eventosValidosOrdenados(contrato.getId(), req.dataEfetiva(), Instant.now()).stream()
                .filter(e -> e.getDataEfetiva().equals(req.dataEfetiva()))
                .map(EventoCicloVidaContrato::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<RemanejamentoContratual> conflitosRemanejamento = mesmosDia.stream()
                .filter(r -> r.getEvento() != null && ativos.contains(r.getEvento().getId()))
                .filter(r -> r.getItens().stream().anyMatch(i -> novas.contains(
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        List<AditamentoContratual> conflitosAditamento = aditamentoRepositorio
                .findByContratoIdAndDataEfetiva(contrato.getId(), req.dataEfetiva()).stream()
                .filter(a -> a.getEvento() != null && ativos.contains(a.getEvento().getId()))
                .filter(a -> a.getAlteracoesComposicao().stream().anyMatch(i -> novas.contains(
                        ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(
                                i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId()))))
                .toList();
        if (conflitosRemanejamento.isEmpty() && conflitosAditamento.isEmpty()) return;
        if (req.eventoPrecedenteId() == null || req.relacaoPrecedencia() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fatos conflitantes sobre a mesma composição e data exigem precedência negocial explícita.");
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.MANTEM_COEXISTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fatos sobre o mesmo recorte não admitem MANTEM_COEXISTE.");
        }
        boolean alvo = conflitosRemanejamento.stream().anyMatch(r -> Objects.equals(r.getEvento().getId(), req.eventoPrecedenteId()))
                || conflitosAditamento.stream().anyMatch(a -> Objects.equals(a.getEvento().getId(), req.eventoPrecedenteId()));
        if (!alvo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O evento precedente informado não é o fato conflitante da mesma data.");
        }
    }

    private int validarIdentificacaoECalcularVersao(Contrato contrato, RemanejamentoContratualRequisicao req) {
        Optional<RemanejamentoContratual> top = remanejamentoRepositorio
                .findTopByContratoIdAndIdentificacaoIgnoreCaseOrderByVersaoInstrumentoDesc(contrato.getId(), req.identificacao().trim());
        if (top.isEmpty()) return 1;
        RemanejamentoContratual anterior = top.get();
        boolean versionando = req.eventoPrecedenteId() != null && anterior.getEvento() != null
                && Objects.equals(anterior.getEvento().getId(), req.eventoPrecedenteId())
                && (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI);
        if (!versionando) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe remanejamento com esta identificação. Utilize retificação/substituição versionada.");
        }
        return Optional.ofNullable(anterior.getVersaoInstrumento()).orElse(1) + 1;
    }

    private Set<Long> eventosIgnorados(RemanejamentoContratualRequisicao req) {
        if (req.eventoPrecedenteId() != null
                && (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI)) {
            return Set.of(req.eventoPrecedenteId());
        }
        return Set.of();
    }

    private PreviaRemanejamentoContratualDTO paraPrevia(Contrato contrato, RemanejamentoContratualRequisicao req, Preparacao p) {
        return new PreviaRemanejamentoContratualDTO(
                contrato.getId(), req.dataEfetiva(), p.totalAntes(), p.totalDepois(), p.somaVariacoes(),
                p.somaVariacoes() == 0 && p.totalAntes() == p.totalDepois(), p.impacto(),
                p.itens().stream().map(this::impactoDTO).toList(), p.alertas(), gerarTokenPrevia(contrato, req, p));
    }

    private void validarTokenPrevia(Contrato contrato, RemanejamentoContratualRequisicao req, Preparacao p) {
        if (req.tokenPrevia() == null || req.tokenPrevia().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Remanejamento exige prévia imediatamente anterior à confirmação. Gere uma nova prévia e envie tokenPrevia.");
        }
        String atual = gerarTokenPrevia(contrato, req, p);
        if (!atual.equalsIgnoreCase(req.tokenPrevia().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A prévia do remanejamento ficou desatualizada. Os impactos foram alterados; gere nova prévia e confirme novamente.");
        }
    }

    private String gerarTokenPrevia(Contrato contrato, RemanejamentoContratualRequisicao req, Preparacao p) {
        List<String> partes = new ArrayList<>();
        partes.add("REMANEJAMENTO");
        partes.add(AssinaturaPreviaNegocial.valor(contrato.getId()));
        partes.add(AssinaturaPreviaNegocial.valor(contrato.getVersion()));
        partes.add(AssinaturaPreviaNegocial.valor(req.identificacao()));
        partes.add(AssinaturaPreviaNegocial.valor(req.dataEfetiva()));
        partes.add(AssinaturaPreviaNegocial.valor(req.justificativa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.referenciaSei()));
        partes.add(AssinaturaPreviaNegocial.valor(req.eventoPrecedenteId()));
        partes.add(AssinaturaPreviaNegocial.valor(req.relacaoPrecedencia()));
        partes.add(AssinaturaPreviaNegocial.valor(req.ordemDocumental()));
        partes.add("TOTAL:" + p.totalAntes() + ":" + p.totalDepois() + ":" + p.somaVariacoes());
        p.itens().stream().sorted(Comparator.comparing(ItemPreparado::chave)).forEach(i ->
                partes.add("ITEM:" + i.chave() + ":" + i.anterior() + ":" + i.variacao() + ":" + i.resultante() + ":" + i.colaboradoresAtuais()));
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
                i.lotacao().getId(), i.lotacao().getNome(), i.jornada().getId(), i.jornada().getDescricao(),
                i.cargo().getId(), i.cargo().getNome(), i.anterior(), i.variacao(), i.resultante(),
                i.colaboradoresAtuais(), excedente, excedente > 0);
    }

    private RemanejamentoContratualDTO paraDTO(RemanejamentoContratual r) {
        int soma = r.getItens().stream().mapToInt(RemanejamentoComposicao::getVariacao).sum();
        return new RemanejamentoContratualDTO(
                r.getId(), r.getContrato() != null ? r.getContrato().getId() : null,
                r.getEvento() != null ? r.getEvento().getId() : null,
                r.getIdentificacao(), r.getVersaoInstrumento(), r.getDataEfetiva(), r.getJustificativa(), r.getReferenciaSei(),
                r.getRegistradoEm(), r.getUsuarioResponsavelId(), r.getUsuarioResponsavelUsername(),
                r.getEvento() != null ? r.getEvento().getSituacaoFato() : SituacaoFatoNegocial.VIGENTE,
                soma,
                r.getItens().stream().map(i -> {
                    long vinculados = ocupacaoAtual(r.getContrato().getId(), i.getLotacao().getId(), i.getJornada().getId(), i.getCargo().getId());
                    return new ImpactoComposicaoContratoDTO(
                            i.getComposicaoBase() != null ? i.getComposicaoBase().getId() : null,
                            i.getLotacao().getId(), i.getLotacao().getNome(), i.getJornada().getId(), i.getJornada().getDescricao(),
                            i.getCargo().getId(), i.getCargo().getNome(), i.getQuantidadeAnterior(), i.getVariacao(),
                            i.getQuantidadeResultante(), vinculados, Math.max(0L, vinculados - i.getQuantidadeResultante()),
                            vinculados > i.getQuantidadeResultante());
                }).toList());
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
                        "Remanejamento é ato negocial do Gestor Local.");
            }
        }
        if (c.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Remanejamento exige contrato publicado válido.");
        }
        if (escrita && !Boolean.TRUE.equals(c.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo não aceita remanejamento.");
        }
        return c;
    }

    private RemanejamentoContratual obterRemanejamentoNoContrato(Long id, Long contratoId) {
        return remanejamentoRepositorio.buscarCompletoPorId(id)
                .filter(r -> r.getContrato() != null && Objects.equals(r.getContrato().getId(), contratoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Remanejamento não encontrado."));
    }

    private static String normalizarChave(String s) {
        return s == null ? "SEM_ID" : s.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private long ocupacaoAtual(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId) {
        return ocupacaoEm(contratoId, lotacaoId, jornadaId, cargoId, RelogioSistema.hoje());
    }

    private long ocupacaoEm(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, LocalDate data) {
        return vinculoContratualServico == null ? 0
                : vinculoContratualServico.contarOcupacao(contratoId, lotacaoId, jornadaId, cargoId, data);
    }
}
