package com.example.siggtrat.servico;

import com.example.siggtrat.regra.MaterialHashEventos;
import static com.example.siggtrat.regra.MaterialHashEventos.*;


import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;

@Service
@Transactional(readOnly = true)
public class VinculoContratualServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(VinculoContratualServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final VinculoContratualRepositorio vinculoRepositorio;
    private final PessoaServico pessoaServico;
    private final ContratoRepositorio contratoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final ReservaMatriculaContratoRepositorio reservaRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final EventoVinculoContratualRepositorio eventoRepositorio;
    private final MotivoDesligamentoRepositorio motivoRepositorio;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalVinculoServico motor;
    private final MinimizadorDadosPessoaisServico minimizador;
    private AlocacaoVinculoServico alocacaoVinculoServico;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;
    private PendenciaAdministrativaServico pendencias;
    private InvalidacaoEmissaoRelatorioServico invalidacaoRelatorios;
    private ProjecaoOperacionalPessoaServico projecaoOperacionalPessoa;
    private MotorTemporalContratoServico motorContrato;
    private AssinaturaImpactoVinculoServico assinaturaImpacto;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private EscopoLeituraPessoaVinculoServico escopoLeitura;
    private GestaoContratoTemporalServico gestaoTemporal;
    private AditamentoContratualServico aditamentoContratualServico;
    private IdentidadeHistoricaServico identidadeHistorica;
    private MotivoDesligamentoHistoricoServico motivoHistorico;
    private GradeFrequenciaCanonicaServico selecaoFrequencia;

    @Autowired
    public void configurarSelecaoFrequencia(GradeFrequenciaCanonicaServico servico) {
        this.selecaoFrequencia = servico;
    }

    @Autowired
    public void configurarProvasHistoricas(IdentidadeHistoricaServico identidade,
                                           MotivoDesligamentoHistoricoServico motivos) {
        this.identidadeHistorica = identidade; this.motivoHistorico = motivos;
    }

    public VinculoContratualServico(VinculoContratualRepositorio vinculoRepositorio,
                                    PessoaServico pessoaServico,
                                    ContratoRepositorio contratoRepositorio,
                                    ComposicaoContratualRepositorio composicaoRepositorio,
                                    ReservaMatriculaContratoRepositorio reservaRepositorio,
                                    AlocacaoVinculoRepositorio alocacaoRepositorio,
                                    EventoVinculoContratualRepositorio eventoRepositorio,
                                    MotivoDesligamentoRepositorio motivoRepositorio,
                                    AcessoOrganizacionalServico acesso,
                                    MotorTemporalVinculoServico motor,
                                    MinimizadorDadosPessoaisServico minimizador) {
        this.vinculoRepositorio = vinculoRepositorio;
        this.pessoaServico = pessoaServico;
        this.contratoRepositorio = contratoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.reservaRepositorio = reservaRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.motivoRepositorio = motivoRepositorio;
        this.acesso = acesso;
        this.motor = motor;
        this.minimizador = minimizador;
    }

    /** Liga o módulo de alocação após a construção para evitar dependência circular. */
    @Autowired(required = false)
    public void configurarAlocacaoTemporal(AlocacaoVinculoServico alocacaoVinculoServico) {
        this.alocacaoVinculoServico = alocacaoVinculoServico;
    }

    @Autowired(required = false)
    public void configurarRevalidacaoFrequencia(RevalidacaoFrequenciaTemporalServico servico) {
        this.revalidacaoFrequenciaTemporalServico = servico;
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

    @Autowired
    public void configurarMotorContrato(MotorTemporalContratoServico motorContrato) {
        this.motorContrato = motorContrato;
    }

    @Autowired(required = false)
    public void configurarAssinaturaImpacto(AssinaturaImpactoVinculoServico servico) {
        this.assinaturaImpacto = servico;
    }

    @Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @Autowired(required = false)
    public void configurarEscopoLeitura(EscopoLeituraPessoaVinculoServico servico) { this.escopoLeitura = servico; }

    @Autowired
    public void configurarGestaoTemporal(GestaoContratoTemporalServico servico) { this.gestaoTemporal = servico; }

    @Autowired(required = false)
    public void configurarPoliticaMatricula(@Lazy AditamentoContratualServico servico) {
        this.aditamentoContratualServico = servico;
    }

    public Page<VinculoContratualDTO> listar(Long contratoId, Long pessoaId, EstadoNegocialVinculo estado,
                                             LocalDate inicioDe, LocalDate inicioAte, LocalDate fimDe, LocalDate fimAte, LocalDate ativoEm,
                                             String busca, Pageable pageable) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        if (!acesso.podeConsultarEmpregados(user) && !acesso.ehPresidente(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a vínculos.");
        if (inicioDe != null && inicioAte != null && inicioDe.isAfter(inicioAte)
                || fimDe != null && fimAte != null && fimDe.isAfter(fimAte)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período de pesquisa é inválido.");
        }
        EscopoConsulta escopo = escopoConsulta(user);
        String termo = busca == null || busca.isBlank() ? null : busca.trim().toLowerCase(Locale.ROOT);
        String cpf = termo == null ? null : termo.replaceAll("\\D", "");
        if (cpf != null && cpf.isBlank()) cpf = null;
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by(Sort.Direction.DESC, "dataInicio").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<VinculoContratual> pagina = escopoLeitura != null
                ? vinculoRepositorio.findAll(escopoLeitura.vinculos(user, contratoId, pessoaId, estado,
                        inicioDe, inicioAte, fimDe, fimAte, ativoEm, termo, cpf), limitado)
                : vinculoRepositorio.pesquisarNoEscopo(escopo.global(), escopo.gestaoLocalId(), escopo.superintendenciaId(),
                        contratoId, pessoaId, estado, inicioDe, inicioAte, fimDe, fimAte, ativoEm, termo, cpf, limitado);
        List<Long> ids = pagina.getContent().stream().map(VinculoContratual::getId).toList();
        Map<Long, AlocacaoVinculo> iniciais = ids.isEmpty() ? Map.of()
                : alocacaoRepositorio.buscarPorVinculos(ids).stream().collect(Collectors.toMap(
                        a -> a.getVinculo().getId(), a -> a, (primeira, ignorada) -> primeira,
                        LinkedHashMap::new));
        return pagina.map(v -> paraDTO(v, user, iniciais.get(v.getId())));
    }

    /** Consulta operacional própria; a listagem cadastral/histórica permanece independente. */
    public Page<VinculoElegivelFrequenciaDTO> listarElegiveisParaFrequencia(LocalDate data,
            Long lotacaoId, Long jornadaId, Long contratoId, List<Long> contratoIds, String busca, Pageable pageable) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirConsultaFrequencia(usuario);
        Set<Long> solicitados = new LinkedHashSet<>();
        if (contratoId != null) solicitados.add(contratoId);
        if (contratoIds != null) contratoIds.stream().filter(Objects::nonNull).forEach(solicitados::add);
        var consulta = selecaoFrequencia.preparar(usuario, data, lotacaoId, jornadaId, solicitados, busca);
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by(Sort.Direction.DESC, "dataInicio").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<VinculoContratual> pagina = vinculoRepositorio.findAll(consulta.vinculos(), limitado);
        List<Long> ids = pagina.getContent().stream().map(VinculoContratual::getId).toList();
        Map<Long, AlocacaoVinculo> porVinculo = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AlocacaoVinculo a : alocacaoRepositorio.buscarPorVinculos(ids)) {
                if (a.getEstadoNegocial() != com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
                        || a.getDataInicio().isAfter(data) || a.getDataFim() != null && a.getDataFim().isBefore(data)
                        || !Objects.equals(a.getLotacao().getId(), lotacaoId)
                        || !Objects.equals(a.getJornada().getId(), jornadaId)) continue;
                if (porVinculo.put(a.getVinculo().getId(), a) != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Mais de uma Alocação foi encontrada no recorte da frequência.");
                }
            }
        }
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return pagina.map(v -> {
            AlocacaoVinculo a = porVinculo.get(v.getId());
            var posicao = motor.reconstruir(v, data, consulta.conhecidoAte());
            GestaoLocal historica = gestaoTemporal.gestaoEm(v.getContrato(), data, consulta.conhecidoAte());
            if (a == null || historica == null || !Objects.equals(historica.getId(), consulta.gestaoId())
                    || posicao.situacaoTemporal() != SituacaoTemporalVinculo.VIGENTE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O contexto da frequência mudou durante a consulta. Refaça a seleção.");
            }
            return new VinculoElegivelFrequenciaDTO(v.getId(), v.getPessoa().getId(),
                    minimizador.nome(v.getPessoa().getNome(), nivel), minimizador.cpf(v.getPessoa().getCpf(), nivel),
                    v.getContrato().getId(), v.getContrato().getNumeroContrato(), v.getContrato().getAnoContrato(),
                    minimizador.matricula(posicao.matricula(), nivel), data, consulta.conhecidoAte(),
                    historica.getId(), a.getId(), a.getLotacao().getId(), a.getJornada().getId(), a.getCargo().getId());
        });
    }

    public VinculoContratualDTO buscar(Long id) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id);
        exigirLeitura(user, v, null, null);
        return paraDTO(v, user);
    }

    public List<EventoVinculoContratualDTO> listarEventos(Long id) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id);
        exigirLeitura(user, v, null, null);
        return eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(id).stream()
                .filter(e -> escopoLeitura == null || escopoLeitura.permiteVinculo(user, v, e.getDataEfetiva(), null))
                .map(e -> paraDTO(e, user)).toList();
    }

    public PosicaoPessoaDTO posicaoPessoa(Long pessoaId, LocalDate data, Instant conhecidoAte) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Pessoa pessoa = pessoaServico.obterEntidade(pessoaId);
        LocalDate referencia = data == null ? RelogioSistema.hoje() : data;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<VinculoContratual> permitidos = vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(pessoaId).stream()
                .filter(v -> escopoLeitura != null && acesso.ehOperacional(user)
                        ? escopoLeitura.permiteVinculo(user, v, referencia, conhecimento)
                        : acesso.podeAcessarContrato(user, v.getContrato())).toList();
        if (permitidos.isEmpty() && !acesso.ehGlobal(user) && !acesso.ehPresidente(user))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada no seu escopo.");
        // A visibilidade atual autoriza a consulta, mas um vínculo cadastrado
        // depois de K não participa de nenhuma reconstrução daquele corte.
        // O motor continua recusando vínculos já conhecidos sem fonte histórica.
        List<VinculoContratual> conhecidos = permitidos.stream()
                .filter(v -> conhecidoNoInstante(v, conhecimento)).toList();
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(user);
        EventoVinculoContratual fonteIdentidade = conhecidos.stream()
                .flatMap(v -> eventoRepositorio
                        .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(v.getId(), conhecimento).stream()
                        .filter(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO
                                ? IdentidadeHistoricaServico.contem(e.getInicioNovo() == null
                                        ? e.getDataEfetiva() : e.getInicioNovo(), e.getFimNovo(), referencia)
                                : referencia.equals(e.getDataEfetiva()))
                        .filter(e -> escopoLeitura == null || !acesso.ehOperacional(user)
                                || escopoLeitura.permiteVinculo(user, v, e.getDataEfetiva(), conhecimento)))
                .max(Comparator.comparing(EventoVinculoContratual::getDataEfetiva)
                        .thenComparing(EventoVinculoContratual::getRegistradoEm)
                        .thenComparing(EventoVinculoContratual::getId)).orElse(null);
        String nomeFotografia = fonteIdentidade == null ? null : fonteIdentidade.getPessoaNomeFotografia();
        boolean comprovada = nomeFotografia != null && !nomeFotografia.isBlank();
        List<VinculoContratual> ancoras = conhecidos.stream()
                .filter(v -> { var pos = motor.reconstruir(v, referencia, conhecimento);
                    return pos.dataInicio() != null && !referencia.isBefore(pos.dataInicio())
                            && (pos.dataFim() == null || !referencia.isAfter(pos.dataFim())); }).toList();
        IdentidadeHistoricaResolvidaDTO resolvida = identidadeHistorica != null && ancoras.size() == 1
                ? identidadeHistorica.resolver(pessoa, ancoras.get(0), referencia, conhecimento)
                : IdentidadeHistoricaResolvidaDTO.ausente();
        resolvida = new IdentidadeHistoricaResolvidaDTO(minimizador.nome(resolvida.nome(), nivel), resolvida.comprovado(),
                resolvida.tipoFonte(), resolvida.fonteId(), resolvida.fonteRegistradaEm(), resolvida.inicioComprovado(), resolvida.fimComprovado());
        return new PosicaoPessoaDTO(pessoaId, minimizador.nome(pessoa.getNome(), nivel),
                minimizador.cpf(pessoa.getCpf(), nivel), referencia, conhecimento,
                conhecidos.stream()
                        .map(v -> minimizarPosicao(motor.reconstruir(v, referencia, conhecimento), nivel)).toList(),
                comprovada ? minimizador.nome(nomeFotografia, nivel) : null,
                comprovada ? fonteIdentidade.getId() : null, comprovada, resolvida);
    }

    private PosicaoVinculoTemporalDTO minimizarPosicao(PosicaoVinculoTemporalDTO posicao, NivelExposicaoDadosPessoais nivel) {
        return new PosicaoVinculoTemporalDTO(posicao.vinculoId(), posicao.pessoaId(), posicao.contratoId(),
                posicao.dataReferencia(), posicao.conhecidoAte(), posicao.dataInicio(), posicao.dataFim(),
                minimizador.matricula(posicao.matricula(), nivel), posicao.estadoNegocial(),
                posicao.situacaoTemporal(), posicao.eventoConhecidoMaisRecenteId());
    }

    private void exigirLeitura(UsuarioAplicacao usuario, VinculoContratual vinculo, LocalDate data, Instant conhecidoAte) {
        if (escopoLeitura != null && acesso.ehOperacional(usuario)) escopoLeitura.exigirVinculo(usuario, vinculo, data, conhecidoAte);
        else acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
    }

    private boolean conhecidoNoInstante(VinculoContratual vinculo, Instant conhecimento) {
        // Um cadastro posterior não existia na visão de conhecimento consultada.
        // Eventos preservados permitem leitura de registros importados cuja data
        // técnica de criação foi posterior ao fato de origem já conhecido.
        if (!eventoRepositorio.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(
                vinculo.getId(), conhecimento).isEmpty()) return true;
        return vinculo.getCriadoEm() != null && !vinculo.getCriadoEm().isAfter(conhecimento);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO cadastrar(CadastroVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(user);
        validarCadastro(req);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(req.contratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(user, contrato);
        exigirContratoPublicado(contrato);
        if (motorContrato != null) {
            PosicaoTemporalContratoDTO posicao = motorContrato.reconstruirPosicao(contrato, req.dataInicio(), Instant.now());
            if (posicao != null && (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                    || posicao.habilitacaoAdministrativa() != HabilitacaoAdministrativaContrato.ATIVO)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Novo vínculo exige contrato publicado e administrativamente ativo na data efetiva de início.");
            }
        }

        if (alocacaoVinculoServico == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Validador temporal de capacidade indisponível.");
        }
        AlocacaoVinculoServico.CombinacaoResolvida composicao = alocacaoVinculoServico.resolverCombinacaoInicial(
                contrato, req.dataInicio(), req.composicaoId(), req.lotacaoId(), req.jornadaId(), req.cargoId());
        AlocacaoVinculoServico.AmpliacaoFim inclusao =
                alocacaoVinculoServico.avaliarInclusao(contrato, composicao, req.dataInicio(), req.dataFim());
        AlocacaoVinculoServico.CapacidadeInclusao capacidade = inclusao.capacidade();
        if (capacidade.efetivoPrevisto() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição possui efetivo previsto zero na data efetiva e não aceita inclusão.");
        }
        if (capacidade.excede()) {
            exigirAutorizacaoExcedente(req, user);
        }

        Pessoa pessoa = req.pessoaId() == null
                ? pessoaServico.criarParaVinculo(req.pessoa(), user)
                : pessoaServico.obterEntidadeParaAtualizacao(req.pessoaId());
        validarPeriodoNoContrato(contrato, req.dataInicio(), req.dataFim());
        validarSemSobreposicao(pessoa.getId(), null, req.dataInicio(), req.dataFim());
        String matricula = validarMatricula(contrato, req.matricula(), req.dataInicio());

        VinculoContratual vinculo = new VinculoContratual();
        vinculo.setPessoa(pessoa); vinculo.setContrato(contrato); vinculo.setDataInicio(req.dataInicio());
        vinculo.setDataFim(req.dataFim()); vinculo.setMatricula(matricula);
        vinculo.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO);
        vinculo.setCriadoPorId(user.getId()); vinculo.setCriadoPorUsername(user.getUsername());
        if (aditamentoContratualServico != null)
            aditamentoContratualServico.exigirMatriculaCompativelNoPeriodo(vinculo, req.dataInicio(), req.dataFim());
        vinculo = vinculoRepositorio.save(vinculo);

        if (matricula != null) reservarMatricula(vinculo, matricula);
        AlocacaoVinculo alocacaoInicial = alocacaoVinculoServico.criarInicial(vinculo, composicao, user,
                req.confirmarInclusaoExcedente(), req.justificativaExcedente(), req.evidenciaExcedente(), req.autorizacaoExcedente());
        if (projecaoOperacionalPessoa != null) {
            projecaoOperacionalPessoa.projetar(vinculo, alocacaoInicial);
        }
        EventoVinculoContratual evento = montarEvento(vinculo, TipoEventoVinculoContratual.CRIACAO, req.dataInicio(), user,
                "Cadastro integrado de Pessoa, Vínculo e Alocação inicial", null, null,
                null, req.dataInicio(), null, req.dataFim(), null, matricula, TipoRelacaoEventoVinculo.ORIGEM);
        // Somente Pessoa nova foi nominalmente declarada no payload deste vínculo.
        if (req.pessoaId() == null) evento.setPessoaNomeFotografia(pessoa.getNome());
        if (capacidade.excede()) {
            completarEventoExcedente(evento, inclusao.combinacao(), capacidade, req);
        }
        // Os campos da trilha são imutáveis (updatable=false). A evidência precisa
        // estar completa antes do primeiro INSERT, que ocorre com a geração IDENTITY.
        eventoRepositorio.save(evento);
        invalidarRelatorios(vinculo, req.dataInicio(), req.dataFim(),
                "Cadastro de Vínculo alterou a população/previsão operacional", user);
        return paraDTO(vinculo, user);
    }

    public PreviaImpactoVinculoDTO previaRetificacaoInicio(Long id, RetificacaoInicioVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        if (req == null || req.novoInicio() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Novo início é obrigatório.");
        validarPeriodoNoContrato(v.getContrato(), req.novoInicio(), v.getDataFim());
        List<Long> conflitos = conflitos(v.getPessoa().getId(), v.getId(), req.novoInicio(), v.getDataFim());
        List<String> impactos = new ArrayList<>(List.of("alocação inicial", "frequências", "matrícula", "relatórios", "eventos posteriores"));
        if (!conflitos.isEmpty()) impactos.add("sobreposição global com vínculos " + conflitos);
        adicionarImpactosProgramacoes(impactos, v, req.novoInicio(), v.getDataFim());
        AlocacaoVinculoServico.AmpliacaoFim ampliacao = alocacaoVinculoServico == null ? null
                : alocacaoVinculoServico.avaliarAmpliacaoInicio(v, req.novoInicio());
        if (ampliacao != null && ampliacao.capacidade().excede()) {
            impactos.add("A antecipação produz ocupação excedente desde " + ampliacao.capacidade().primeiroDiaExcedente()
                    + "; previsto " + ampliacao.capacidade().efetivoPrevisto() + ", ocupação após a antecipação "
                    + ampliacao.capacidade().ocupacaoAposInclusao()
                    + ". Exige confirmarInclusaoExcedente=true, justificativa, evidência e autorização.");
        }
        String token = tokenPrevia(v, req.novoInicio(), v.getDataFim(), conflitos, "INICIO", req.justificativa(), req.evidencia(), req.autorizacao(),
                ampliacao == null ? null : ampliacao.assinatura());
        return new PreviaImpactoVinculoDTO(token, id, v.getDataInicio(), req.novoInicio(), List.copyOf(impactos), conflitos);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO retificarInicio(Long id, RetificacaoInicioVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        PreviaImpactoVinculoDTO previa = previaRetificacaoInicio(id, req);
        validarConfirmacao(req.confirmarImpacto(), req.tokenPrevia(), previa.token());
        if (!previa.vinculosConflitantes().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Retificação produziria sobreposição global de vínculos.");
        validarSemSobreposicao(v.getPessoa().getId(), v.getId(), req.novoInicio(), v.getDataFim());
        var posicaoInicio = motor.reconstruir(v, req.novoInicio(), Instant.now());
        validarObrigatoriedadeMatricula(v.getContrato(), posicaoInicio == null ? v.getMatricula() : posicaoInicio.matricula(), req.novoInicio());
        if (aditamentoContratualServico != null)
            aditamentoContratualServico.exigirMatriculaCompativelNoPeriodo(v, req.novoInicio(), v.getDataFim());
        exigirProgramacoesCompativeis(v, req.novoInicio(), v.getDataFim());
        AlocacaoVinculoServico.AmpliacaoFim ampliacao = alocacaoVinculoServico == null ? null
                : alocacaoVinculoServico.avaliarAmpliacaoInicio(v, req.novoInicio());
        exigirConsentimentoAmpliacao(ampliacao, req.confirmarInclusaoExcedente(), req.justificativa(),
                req.evidencia(), req.autorizacao(), user);
        String justificativa = justificativaComAmpliacao(req.justificativa(), ampliacao);
        LocalDate anterior = v.getDataInicio();
        LocalDate desdeImpacto = anterior.isBefore(req.novoInicio()) ? anterior : req.novoInicio();
        LocalDate ateImpacto = (anterior.isAfter(req.novoInicio()) ? anterior : req.novoInicio()).minusDays(1);
        // O primeiro ajuste de uma Alocação sem eventos retira a faixa atual e
        // não pode fabricar sua origem no K da emissão. Avaliar o recorte ainda
        // disponível antes da mutação preserva a invalidação do trecho retirado.
        // A chamada posterior cobre o trecho acrescido; ambas usam o intervalo
        // exato e a mesma transação, inclusive a auditoria publicada após commit.
        invalidarRelatorios(v, desdeImpacto, ateImpacto,
                "Retificação retroativa do início do Vínculo Contratual", user);
        v.setDataInicio(req.novoInicio());
        vinculoRepositorio.save(v);
        sincronizarAlocacao(v, user, justificativa, req.evidencia(), req.autorizacao(), ampliacao);
        registrarEvento(v, TipoEventoVinculoContratual.RETIFICACAO_INICIO, req.novoInicio(), user,
                justificativa, req.evidencia(), req.autorizacao(), anterior, req.novoInicio(),
                v.getDataFim(), v.getDataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.RETIFICA, ampliacao);
        if (projecaoOperacionalPessoa != null) projecaoOperacionalPessoa.sincronizarAposAlteracaoVinculo(v);
        revalidarFrequencias(v, desdeImpacto, ateImpacto,
                "Retificação retroativa do início do Vínculo Contratual");
        invalidarRelatorios(v, desdeImpacto, ateImpacto,
                "Retificação retroativa do início do Vínculo Contratual", user);
        return paraDTO(v, user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO alterarMatricula(Long id, AlteracaoMatriculaVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        if (req == null || req.dataEfetiva() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data efetiva da matrícula é obrigatória.");
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        if (req.dataEfetiva().isAfter(RelogioSistema.hoje())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alteração futura de matrícula não é permitida.");
        if (req.dataEfetiva().isBefore(v.getDataInicio()) || v.getDataFim() != null && req.dataEfetiva().isAfter(v.getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da matrícula deve estar dentro do vínculo.");
        }
        String nova = validarMatricula(v.getContrato(), req.novaMatricula(), req.dataEfetiva());
        if (nova == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova matrícula é obrigatória nesta operação.");
        if (Objects.equals(nova, VinculoContratual.normalizarMatricula(v.getMatricula()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova matrícula é igual à atual.");
        reservarMatricula(v, nova);
        String anterior = motor.reconstruir(v, req.dataEfetiva(), Instant.now()).matricula();
        v.setMatricula(nova); vinculoRepositorio.save(v);
        registrarEvento(v, TipoEventoVinculoContratual.ALTERACAO_MATRICULA, req.dataEfetiva(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                v.getDataFim(), v.getDataFim(), anterior, nova, TipoRelacaoEventoVinculo.SUBSTITUI);
        // A coluna operacional acompanha a matrícula vigente hoje, mesmo quando
        // o fato recém-registrado produz efeito antes de outra alteração conhecida.
        v.setMatricula(motor.reconstruir(v, RelogioSistema.hoje(), Instant.now()).matricula());
        return paraDTO(v, user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO cancelarProgramacao(Long id, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        if (v.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO || !RelogioSistema.hoje().isBefore(v.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente vínculo futuro ainda não iniciado pode ter sua programação cancelada.");
        }
        v.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO); vinculoRepositorio.save(v);
        registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO, v.getDataInicio(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                v.getDataFim(), v.getDataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA);
        invalidarRelatorios(v, v.getDataInicio(), v.getDataFim(),
                "Cancelamento de Vínculo programado alterou a população/previsão operacional", user);
        return paraDTO(v, user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO desligar(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        if (v.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente Vínculo registrado admite desligamento comum. Programação cancelada preserva seu próprio histórico.");
        }
        validarDesligamento(v, req, false);
        validarSemSobreposicao(v.getPessoa().getId(), v.getId(), v.getDataInicio(), req.dataFim());
        exigirProgramacoesCompativeis(v, v.getDataInicio(), req.dataFim());
        String justificativa = justificativaDesligamentoComPrecedencia(v, req);
        MotivoDesligamento motivo = obterMotivoParaDesligamento(v, req);
        LocalDate anterior = v.getDataFim();
        // Avalia a faixa materializada antes de o primeiro evento de Alocação
        // recortar seu fim. O último dia inclusive não pertence ao impacto.
        invalidarRelatorios(v, req.dataFim().plusDays(1),
                "Desligamento com repercussão temporal em frequência", user);
        aplicarDesligamento(v, req.dataFim(), motivo, user);
        sincronizarAlocacao(v, user, justificativa);
        registrarEvento(v, TipoEventoVinculoContratual.DESLIGAMENTO, req.dataFim(), user,
                justificativa, req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, req.dataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.ORIGEM);
        revalidarFrequencias(v, req.dataFim().plusDays(1), "Desligamento com repercussão temporal em frequência");
        return paraDTO(v, user);
    }

    public PreviaImpactoVinculoDTO previaRetificacaoDesligamento(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        validarDesligamento(v, req, true);
        MotivoDesligamento motivo = obterMotivoParaDesligamento(v, req);
        List<Long> conflitos = conflitos(v.getPessoa().getId(), v.getId(), v.getDataInicio(), req.dataFim());
        List<String> impactos = new ArrayList<>(List.of("alocações", "frequências", "matrícula", "relatórios", "eventos posteriores"));
        if (!conflitos.isEmpty()) impactos.add("sobreposição global com vínculos " + conflitos);
        adicionarImpactosProgramacoes(impactos, v, v.getDataInicio(), req.dataFim());
        if (mudancaAlocacaoNoUltimoDia(v, req.dataFim())) {
            impactos.add("Mudança de Alocação no último dia exige precedência explícita: a Alocação vale nesse dia e o desligamento após ele.");
        }
        AlocacaoVinculoServico.AmpliacaoFim ampliacao = avaliarAmpliacaoFim(v, req.dataFim());
        if (ampliacao != null && ampliacao.capacidade().excede()) {
            impactos.add("A ampliação produz ocupação excedente desde " + ampliacao.capacidade().primeiroDiaExcedente()
                    + "; previsto " + ampliacao.capacidade().efetivoPrevisto() + ", ocupação após a ampliação "
                    + ampliacao.capacidade().ocupacaoAposInclusao()
                    + ". Exige confirmarInclusaoExcedente=true, justificativa, evidência e autorização.");
        }
        String token = tokenPrevia(v, v.getDataInicio(), req.dataFim(), conflitos, "DESLIGAMENTO",
                motivo.getId(), motivo.getVersion(), req.justificativa(), req.evidencia(), req.autorizacao(),
                ampliacao == null ? null : ampliacao.assinatura());
        return new PreviaImpactoVinculoDTO(token, id, v.getDataFim(), req.dataFim(), List.copyOf(impactos), conflitos);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO retificarDesligamento(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        if (v.getDataFim() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo ainda não possui desligamento para retificar.");
        validarDesligamento(v, req, true);
        PreviaImpactoVinculoDTO previa = previaRetificacaoDesligamento(id, req);
        validarConfirmacao(req.confirmarImpacto(), req.tokenPrevia(), previa.token());
        if (!previa.vinculosConflitantes().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Retificação produziria sobreposição global de vínculos.");
        validarSemSobreposicao(v.getPessoa().getId(), v.getId(), v.getDataInicio(), req.dataFim());
        if (aditamentoContratualServico != null)
            aditamentoContratualServico.exigirMatriculaCompativelNoPeriodo(v, v.getDataInicio(), req.dataFim());
        exigirProgramacoesCompativeis(v, v.getDataInicio(), req.dataFim());
        AlocacaoVinculoServico.AmpliacaoFim ampliacao = avaliarAmpliacaoFim(v, req.dataFim());
        exigirConsentimentoAmpliacao(ampliacao, req.confirmarInclusaoExcedente(), req.justificativa(),
                req.evidencia(), req.autorizacao(), user);
        String justificativa = justificativaComAmpliacao(justificativaDesligamentoComPrecedencia(v, req), ampliacao);
        MotivoDesligamento motivo = obterMotivoParaDesligamento(v, req);
        LocalDate anterior = v.getDataFim();
        LocalDate desdeImpacto = (req.dataFim().isBefore(anterior) ? req.dataFim() : anterior).plusDays(1);
        LocalDate ateImpacto = anterior.isAfter(req.dataFim()) ? anterior : req.dataFim();
        // A leitura anterior preserva o recorte retirado; a posterior conserva
        // o alcance de ampliações. Ambas participam da transação do comando.
        invalidarRelatorios(v, desdeImpacto, ateImpacto, "Retificação retroativa do desligamento", user);
        aplicarDesligamento(v, req.dataFim(), motivo, user);
        sincronizarAlocacao(v, user, justificativa, req.evidencia(), req.autorizacao(), ampliacao);
        registrarEvento(v, TipoEventoVinculoContratual.RETIFICACAO_DESLIGAMENTO, req.dataFim(), user,
                justificativa, req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, req.dataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.RETIFICA, ampliacao);
        revalidarFrequencias(v, desdeImpacto, ateImpacto, "Retificação retroativa do desligamento");
        invalidarRelatorios(v, desdeImpacto, ateImpacto, "Retificação retroativa do desligamento", user);
        return paraDTO(v, user);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VinculoContratualDTO cancelarDesligamentoProgramado(Long id, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obterParaAtualizacao(id, user);
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        if (v.getDataFim() == null || v.getDataFim().isBefore(RelogioSistema.hoje())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente desligamento ainda programado pode ser cancelado.");
        }
        validarSemSobreposicao(v.getPessoa().getId(), v.getId(), v.getDataInicio(), null);
        if (aditamentoContratualServico != null)
            aditamentoContratualServico.exigirMatriculaCompativelNoPeriodo(v, v.getDataInicio(), null);
        AlocacaoVinculoServico.AmpliacaoFim ampliacao = avaliarAmpliacaoFim(v, null);
        exigirConsentimentoAmpliacao(ampliacao, req.confirmarInclusaoExcedente(), req.justificativa(),
                req.evidencia(), req.autorizacao(), user);
        String justificativa = justificativaComAmpliacao(req.justificativa(), ampliacao);
        LocalDate anterior = v.getDataFim();
        v.setDataFim(null); v.setMotivoDesligamento(null);
        v.setMotivoDesligamentoDescricaoFotografia(null); v.setMotivoDesligamentoCategoriaFotografia(null);
        v.setDesligamentoRegistradoEm(null);
        v.setDesligamentoRegistradoPorId(null); v.setDesligamentoRegistradoPorUsername(null);
        vinculoRepositorio.save(v);
        sincronizarAlocacao(v, user, justificativa, req.evidencia(), req.autorizacao(), ampliacao);
        registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_DESLIGAMENTO_PROGRAMADO, anterior, user,
                justificativa, req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, null, v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA, ampliacao);
        revalidarFrequencias(v, anterior.plusDays(1), "Cancelamento/retificação de desligamento programado");
        invalidarRelatorios(v, anterior.plusDays(1), "Cancelamento de desligamento programado", user);
        return paraDTO(v, user);
    }

    /** Trata explicitamente os vínculos que ficaram abertos no fim natural. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<VinculoContratualDTO> tratarFimNatural(Long contratoId) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        // O mesmo lock dos aditamentos impede decidir o fim enquanto uma
        // prorrogação concorrente publica seu efeito. A coluna materializada
        // pode continuar antiga depois de uma programação tornar-se efetiva.
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(user, contrato);
        LocalDate hoje = RelogioSistema.hoje();
        PosicaoTemporalContratoDTO posicao = motorContrato.reconstruirPosicao(contrato, hoje, Instant.now());
        LocalDate fimNatural = posicao.vigenciaFim();
        if (fimNatural == null || !fimNatural.isBefore(hoje)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O contrato ainda não alcançou o fim natural de vigência.");
        }
        if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || posicao.eventosAplicados().stream().anyMatch(e ->
                        e.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A causa contratual vigente não admite tratamento como fim natural de vigência.");
        }
        MotivoDesligamento motivo = obterMotivo(contrato, null, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO, fimNatural);
        encerrarVinculosAbertos(contrato, fimNatural, motivo, user,
                TipoEventoVinculoContratual.FIM_NATURAL_CONTRATO, "Tratamento administrativo do fim natural da vigência");
        if (pendencias != null) {
            pendencias.resolverFimNaturalContrato(contratoId, user,
                    "Fim natural tratado com data final igual ao término da vigência.");
        }
        return vinculoRepositorio.findByContratoIdOrderByDataInicioAscIdAsc(contratoId).stream()
                .map(v -> paraDTO(v, user)).toList();
    }

    /** Gancho do ciclo de vida contratual: inativação administrativa não chama este método. */
    @Transactional
    public void encerrarPorContrato(Contrato contrato, LocalDate dataEfetiva, CategoriaTerminoVinculo categoria,
                                    UsuarioAplicacao user, String justificativa) {
        MotivoDesligamento motivo = obterMotivo(contrato, null, categoria, dataEfetiva);
        TipoEventoVinculoContratual tipo = categoria == CategoriaTerminoVinculo.ANULACAO_CONTRATO
                ? TipoEventoVinculoContratual.ANULACAO_CONTRATO
                : TipoEventoVinculoContratual.ENCERRAMENTO_ANTECIPADO_CONTRATO;
        encerrarVinculosAbertos(contrato, dataEfetiva, motivo, user, tipo, justificativa);
    }

    @Transactional
    public void cancelarProgramacoesPorContrato(Contrato contrato, UsuarioAplicacao user, String justificativa) {
        for (VinculoContratual v : vinculoRepositorio.findByContratoIdOrderByDataInicioAscIdAsc(contrato.getId())) {
            if (v.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            v.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO);
            vinculoRepositorio.save(v);
            registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO, v.getDataInicio(), user,
                    justificativa, "Cancelamento do contrato futuro", "Ciclo de vida contratual",
                    v.getDataInicio(), v.getDataInicio(), v.getDataFim(), v.getDataFim(),
                    v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA);
        }
    }

    /** Impede que uma prorrogação registrada depois do fim antigo reabra vínculos automaticamente. */
    @Transactional
    public void preservarFimAntigoEmProrrogacaoTardia(Contrato contrato, LocalDate fimAntigo, LocalDate fimNovo,
                                                       UsuarioAplicacao user, String justificativa) {
        if (fimAntigo == null || fimNovo == null || !fimNovo.isAfter(fimAntigo) || !RelogioSistema.hoje().isAfter(fimAntigo)) return;
        MotivoDesligamento motivo = obterMotivo(contrato, null, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO, fimAntigo);
        encerrarVinculosAbertos(contrato, fimAntigo, motivo, user, TipoEventoVinculoContratual.FIM_NATURAL_CONTRATO,
                justificativa == null ? "Prorrogação tardia: preservação do fim antigo; reabertura exige retificação explícita" : justificativa);
    }

    /** Chamado pelo aditamento na mesma transação e sob o lock do contrato. */
    @Transactional
    public void estenderAlocacoesPorProrrogacaoTempestiva(Contrato contrato, LocalDate fimAnterior,
            LocalDate fimNovo, LocalDate dataEfetivaAditamento, Long eventoContratoOrigemId,
            UsuarioAplicacao responsavel, String justificativa) {
        if (alocacaoVinculoServico == null || fimAnterior == null || fimNovo == null
                || !fimNovo.isAfter(fimAnterior) || RelogioSistema.hoje().isAfter(fimAnterior)) return;
        List<VinculoContratual> candidatos = vinculoRepositorio.findByContratoIdOrderByDataInicioAscIdAsc(contrato.getId())
                .stream().filter(v -> v.getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO)
                .filter(v -> v.getDataFim() == null || v.getDataFim().isAfter(fimAnterior))
                .sorted(Comparator.comparing((VinculoContratual v) -> v.getPessoa().getId()).thenComparing(VinculoContratual::getId))
                .toList();
        for (VinculoContratual candidato : candidatos) {
            VinculoContratual vinculo = obterParaAtualizacao(candidato.getId(), responsavel);
            if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO
                    || vinculo.getDataFim() != null && !vinculo.getDataFim().isAfter(fimAnterior)) continue;
            LocalDate limite = alocacaoVinculoServico.fimCoberturaPlanejada(contrato, vinculo.getDataInicio(), vinculo.getDataFim());
            if (limite == null || !limite.isAfter(fimAnterior)) continue;
            validarSemSobreposicao(vinculo.getPessoa().getId(), vinculo.getId(), vinculo.getDataInicio(), limite);
            alocacaoVinculoServico.estenderUltimaFaixaPorProrrogacao(vinculo, fimAnterior, limite,
                    dataEfetivaAditamento, eventoContratoOrigemId, responsavel, justificativa);
        }
    }

    public long contarOperacionaisNoContrato(Long contratoId, LocalDate data) {
        return vinculoRepositorio.countOperacionaisNoContrato(contratoId, data == null ? RelogioSistema.hoje() : data);
    }

    /** Mesmos predicados da contagem individual, para o conjunto autorizado pelo chamador. */
    public Map<Long, Long> contarOperacionaisPorContrato(Collection<Long> contratoIds, LocalDate data) {
        if (contratoIds == null || contratoIds.isEmpty()) return Map.of();
        Set<Long> ids = contratoIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        Map<Long, Long> resultado = new HashMap<>();
        for (Object[] linha : vinculoRepositorio.contarOperacionaisPorContrato(
                ids, data == null ? RelogioSistema.hoje() : data)) {
            Long id = ((Number) linha[0]).longValue();
            if (ids.contains(id)) resultado.put(id, ((Number) linha[1]).longValue());
        }
        return Map.copyOf(resultado);
    }

    public long contarOcupacao(Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, LocalDate data) {
        return alocacaoRepositorio.countOcupacao(contratoId, lotacaoId, jornadaId, cargoId,
                data == null ? RelogioSistema.hoje() : data);
    }

    public SortedSet<LocalDate> datasMudancaOcupacao(Long contratoId, LocalDate ate) {
        SortedSet<LocalDate> datas = new TreeSet<>();
        alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contratoId).stream()
                .filter(a -> a.getEstadoNegocial() == com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA)
                .forEach(a -> {
                    if (a.getDataInicio() != null && (ate == null || !a.getDataInicio().isAfter(ate))) {
                        datas.add(a.getDataInicio());
                    }
                    if (a.getDataFim() != null && !a.getDataFim().equals(LocalDate.MAX)) {
                        LocalDate saida = a.getDataFim().plusDays(1);
                        if (ate == null || !saida.isAfter(ate)) datas.add(saida);
                    }
                });
        return datas;
    }

    public List<JustificativaExcedenteDTO> justificativasExcedente(Long contratoId, Long lotacaoId,
                                                                   Long jornadaId, Long cargoId,
                                                                   LocalDate ate) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(acesso.usuarioAtual());
        List<JustificativaExcedenteDTO> justificativas = new ArrayList<>(eventoRepositorio
                .findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(contratoId)
                .stream()
                .filter(e -> ate == null || !e.getDataEfetiva().isAfter(ate))
                .filter(e -> Objects.equals(lotacaoId, e.getLotacaoExcedenteId()))
                .filter(e -> Objects.equals(jornadaId, e.getJornadaExcedenteId()))
                .filter(e -> Objects.equals(cargoId, e.getCargoExcedenteId()))
                .map(e -> new JustificativaExcedenteDTO(e.getId(), e.getVinculo().getId(), e.getDataEfetiva(),
                        e.getRegistradoEm(), e.getResponsavelUsername(), minimizador.restrito(e.getJustificativa(), nivel),
                        minimizador.restrito(e.getEvidencia(), nivel), minimizador.restrito(e.getAutorizacao(), nivel),
                        Optional.ofNullable(e.getEfetivoPrevistoExcedente()).orElse(0),
                        Optional.ofNullable(e.getOcupacaoAposInclusaoExcedente()).orElse(0L)))
                .toList());
        if (alocacaoVinculoServico != null) {
            justificativas.addAll(alocacaoVinculoServico.justificativasExcedente(contratoId, lotacaoId,
                    jornadaId, cargoId, ate));
        }
        justificativas.sort(Comparator.comparing(JustificativaExcedenteDTO::dataEfetiva)
                .thenComparing(JustificativaExcedenteDTO::registradoEm)
                .thenComparing(JustificativaExcedenteDTO::origemEvento)
                .thenComparing(JustificativaExcedenteDTO::eventoId, Comparator.nullsLast(Long::compareTo)));
        return List.copyOf(justificativas);
    }

    public boolean possuiVinculosCanonicos(Long contratoId) {
        return vinculoRepositorio.existsByContratoId(contratoId);
    }

    private void encerrarVinculosAbertos(Contrato contrato, LocalDate dataEfetiva, MotivoDesligamento motivo,
                                         UsuarioAplicacao user, TipoEventoVinculoContratual tipo, String justificativa) {
        if (dataEfetiva == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data efetiva do encerramento é obrigatória.");
        for (VinculoContratual v : vinculoRepositorio.findByContratoIdOrderByDataInicioAscIdAsc(contrato.getId())) {
            if (v.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            if (dataEfetiva.isBefore(v.getDataInicio())) {
                v.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO);
                vinculoRepositorio.save(v);
                registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO, v.getDataInicio(), user,
                        justificativa, "Ciclo de vida do contrato", "Regra sistêmica", v.getDataInicio(), v.getDataInicio(),
                        v.getDataFim(), v.getDataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA);
                continue;
            }
            if (v.getDataFim() != null && !v.getDataFim().isAfter(dataEfetiva)) continue;
            LocalDate anterior = v.getDataFim();
            aplicarDesligamento(v, dataEfetiva, motivo, user);
            sincronizarAlocacao(v, user, justificativa);
            registrarEvento(v, tipo, dataEfetiva, user, justificativa, "Ciclo de vida do contrato", "Regra sistêmica",
                    v.getDataInicio(), v.getDataInicio(), anterior, dataEfetiva, v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.SUBSTITUI);
            revalidarFrequencias(v, dataEfetiva.plusDays(1), "Ciclo de vida contratual alterou a cobertura do Vínculo");
            invalidarRelatorios(v, dataEfetiva.plusDays(1),
                    "Ciclo de vida contratual alterou a cobertura do Vínculo", user);
        }
    }

    private void revalidarFrequencias(VinculoContratual vinculo, LocalDate desde, String motivo) {
        revalidarFrequencias(vinculo, desde, null, motivo);
    }

    private void revalidarFrequencias(VinculoContratual vinculo, LocalDate desde, LocalDate ate, String motivo) {
        if (revalidacaoFrequenciaTemporalServico != null && vinculo != null && vinculo.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorVinculo(vinculo.getId(), desde, ate, motivo);
        }
    }

    private void invalidarRelatorios(VinculoContratual vinculo, LocalDate desde,
                                     String motivo, UsuarioAplicacao usuario) {
        invalidarRelatorios(vinculo, desde, null, motivo, usuario);
    }

    private void invalidarRelatorios(VinculoContratual vinculo, LocalDate desde, LocalDate ate,
                                     String motivo, UsuarioAplicacao usuario) {
        if (invalidacaoRelatorios != null) {
            invalidacaoRelatorios.invalidarPorVinculo(vinculo, desde, ate, motivo, usuario);
        }
    }

    private void validarCadastro(CadastroVinculoContratualRequisicao req) {
        if (req == null || req.contratoId() == null || req.dataInicio() == null
                || req.composicaoId() == null && (req.lotacaoId() == null || req.jornadaId() == null || req.cargoId() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato, início efetivo e alocação inicial são obrigatórios.");
        }
        if ((req.pessoaId() == null) == (req.pessoa() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma pessoa existente ou os dados de uma nova pessoa, nunca ambos.");
        }
    }

    private VinculoContratual obter(Long id) {
        return vinculoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
    }

    private VinculoContratual obterParaAtualizacao(Long id, UsuarioAplicacao usuario) {
        var identidade = vinculoRepositorio.buscarIdentidade(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(identidade.getContratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(usuario, contrato);
        // Mesma ordem do cadastro: Contrato → Pessoa → Vínculo. O lock da Pessoa
        // serializa inclusive retificações em dois contratos/Gestões diferentes.
        pessoaServico.obterEntidadeParaAtualizacao(identidade.getPessoaId());
        return vinculoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
    }
    private void exigirContratoPublicado(Contrato contrato) {
        if (contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Novo vínculo exige contrato publicado.");
        }
        if (!Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo não aceita escrita operacional.");
        }
    }

    private void exigirAutorizacaoExcedente(CadastroVinculoContratualRequisicao req, UsuarioAplicacao user) {
        exigirAutorizacaoExcedente(req.confirmarInclusaoExcedente(), req.justificativaExcedente(),
                req.evidenciaExcedente(), req.autorizacaoExcedente(), user);
    }

    private void exigirAutorizacaoExcedente(Boolean confirmar, String justificativa, String evidencia,
                                            String autorizacao, UsuarioAplicacao user) {
        if (!acesso.ehGestaoLocal(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente Gestor Local pode autorizar inclusão acima do efetivo previsto.");
        }
        if (!Boolean.TRUE.equals(confirmar) || vazio(justificativa) || vazio(evidencia) || vazio(autorizacao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A inclusão excedente exige confirmação, justificativa, evidência e autorização.");
        }
    }

    private AlocacaoVinculoServico.AmpliacaoFim avaliarAmpliacaoFim(VinculoContratual vinculo, LocalDate fim) {
        return alocacaoVinculoServico == null ? null : alocacaoVinculoServico.avaliarAmpliacaoFim(vinculo, fim);
    }

    private void exigirConsentimentoAmpliacao(AlocacaoVinculoServico.AmpliacaoFim ampliacao, Boolean confirmar,
                                              String justificativa, String evidencia, String autorizacao,
                                              UsuarioAplicacao usuario) {
        if (ampliacao != null && ampliacao.capacidade().excede()) {
            exigirAutorizacaoExcedente(confirmar, justificativa, evidencia, autorizacao, usuario);
        }
    }

    private String justificativaComAmpliacao(String justificativa, AlocacaoVinculoServico.AmpliacaoFim ampliacao) {
        return ampliacao == null || !ampliacao.capacidade().excede() ? justificativa
                : justificativa + ampliacao.capacidade().detalheTemporal(ampliacao.inicio().minusDays(1));
    }

    private void validarPeriodoNoContrato(Contrato contrato, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim != null && fim.isBefore(inicio)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período do vínculo é inválido.");
        Instant conhecimento = Instant.now();
        PosicaoTemporalContratoDTO posicaoInicio = motorContrato == null ? null
                : motorContrato.reconstruirPosicao(contrato, inicio, conhecimento);
        LocalDate inicioContrato = posicaoInicio == null ? contrato.getDataInicio() : posicaoInicio.vigenciaInicio();
        LocalDate fimNoInicio = posicaoInicio == null ? contrato.getDataFim() : posicaoInicio.vigenciaFim();
        if (inicioContrato != null && inicio.isBefore(inicioContrato)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo não pode iniciar antes do contrato.");
        }
        // A vigência efetiva pode mudar entre o início e o fim solicitado por
        // aditamento já registrado, sem antecipar a projeção operacional de hoje.
        LocalDate fimNoTermino = fim == null || motorContrato == null ? fimNoInicio
                : motorContrato.reconstruirPosicao(contrato, fim, conhecimento).vigenciaFim();
        if (fimNoInicio != null && inicio.isAfter(fimNoInicio)
                || fim != null && fimNoTermino != null && fim.isAfter(fimNoTermino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo deve estar contido na vigência do contrato.");
        }
    }

    private String validarObrigatoriedadeMatricula(Contrato contrato, String valor, LocalDate dataEfetiva) {
        String matricula = VinculoContratual.normalizarMatricula(valor);
        if (matricula == null) {
            boolean obrigatoria = aditamentoContratualServico == null ? contrato.exigeMatriculaNoVinculo()
                    : aditamentoContratualServico.exigeMatriculaNoVinculoEm(contrato, dataEfetiva, Instant.now());
            if (obrigatoria) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Matrícula é obrigatória para este contrato/tipo de contratação.");
        }
        if (matricula != null && matricula.length() > 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matrícula deve ter no máximo 30 caracteres.");
        return matricula;
    }

    private String validarMatricula(Contrato contrato, String valor, LocalDate dataEfetiva) {
        String matricula = validarObrigatoriedadeMatricula(contrato, valor, dataEfetiva);
        if (matricula != null && reservaRepositorio.existsByContratoIdAndMatricula(contrato.getId(), matricula)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já foi utilizada historicamente neste contrato e permanece reservada.");
        }
        return matricula;
    }

    private void reservarMatricula(VinculoContratual vinculo, String matricula) {
        ReservaMatriculaContrato reserva = new ReservaMatriculaContrato();
        reserva.setContrato(vinculo.getContrato()); reserva.setVinculo(vinculo); reserva.setMatricula(matricula);
        try { reservaRepositorio.saveAndFlush(reserva); }
        catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Matrícula já reservada historicamente neste contrato.", ex);
        }
    }

    private void validarSemSobreposicao(Long pessoaId, Long ignorarId, LocalDate inicio, LocalDate fim) {
        LocalDate finalNovo = fim == null ? LocalDate.MAX : fim;
        List<Long> conflitos = vinculoRepositorio.buscarPeriodosRegistradosParaAtualizacao(pessoaId).stream()
                .filter(v -> !Objects.equals(v.getId(), ignorarId))
                .filter(v -> !finalNovo.isBefore(v.getDataInicio())
                        && (v.getDataFim() == null || !v.getDataFim().isBefore(inicio)))
                .map(VinculoContratualRepositorio.PeriodoRegistrado::getId).toList();
        if (!conflitos.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A pessoa já possui vínculo sobreposto, inclusive em outro contrato/Gestão: " + conflitos);
    }

    private List<Long> conflitos(Long pessoaId, Long ignorarId, LocalDate inicio, LocalDate fim) {
        LocalDate finalNovo = fim == null ? LocalDate.MAX : fim;
        return vinculoRepositorio.findByPessoaIdAndEstadoNegocialNotOrderByDataInicioAscIdAsc(pessoaId, EstadoNegocialVinculo.CANCELADO).stream()
                .filter(v -> !Objects.equals(v.getId(), ignorarId) && v.getEstadoNegocial() != EstadoNegocialVinculo.ANULADO)
                .filter(v -> {
                    LocalDate finalExistente = v.getDataFim() == null ? LocalDate.MAX : v.getDataFim();
                    return !finalNovo.isBefore(v.getDataInicio()) && !finalExistente.isBefore(inicio);
                }).map(VinculoContratual::getId).filter(Objects::nonNull).toList();
    }

    private boolean mudancaAlocacaoNoUltimoDia(VinculoContratual vinculo, LocalDate data) {
        return alocacaoVinculoServico != null && alocacaoVinculoServico.possuiMudancaEfetivaNoDia(vinculo, data);
    }

    private String justificativaDesligamentoComPrecedencia(VinculoContratual vinculo, DesligamentoVinculoRequisicao req) {
        if (!mudancaAlocacaoNoUltimoDia(vinculo, req.dataFim())) return req.justificativa();
        if (!Boolean.TRUE.equals(req.confirmarPrecedenciaUltimoDia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mudança de Alocação no último dia exige confirmarPrecedenciaUltimoDia=true: "
                            + "a Alocação vale no último dia e o desligamento produz efeito após esse dia.");
        }
        return "[PRECEDENCIA: ALOCACAO_VALE_NO_ULTIMO_DIA; DESLIGAMENTO_APOS_O_DIA] " + req.justificativa();
    }

    private void validarDesligamento(VinculoContratual v, DesligamentoVinculoRequisicao req, boolean retificacao) {
        if (req == null || req.dataFim() == null || req.motivoDesligamentoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data final e motivo de desligamento são obrigatórios.");
        }
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        if (req.dataFim().isBefore(v.getDataInicio())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desligamento não pode anteceder o vínculo.");
        validarPeriodoNoContrato(v.getContrato(), v.getDataInicio(), req.dataFim());
        if (!retificacao && v.getDataFim() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo já possui desligamento; use retificação.");
    }

    private MotivoDesligamento obterMotivo(Contrato contrato, Long id, CategoriaTerminoVinculo categoria, LocalDate dataEfetiva) {
        GestaoLocal gestaoDoFato = gestaoTemporal == null ? contrato.getGestaoLocal()
                : gestaoTemporal.gestaoEm(contrato, dataEfetiva, Instant.now());
        if (gestaoDoFato == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Não foi possível reconstruir a Gestão Local responsável na data do término.");
        MotivoDesligamento motivo;
        if (id != null) motivo = motivoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de desligamento não encontrado."));
        else {
            List<MotivoDesligamento> opcoes = motivoRepositorio.findByGestaoLocalIdOrderByDescricaoAsc(gestaoDoFato.getId()).stream()
                    .filter(m -> motivoAtivoEm(m, dataEfetiva))
                    .filter(m -> contextoMotivo(m, contrato, dataEfetiva).categoria() == categoria).toList();
            if (opcoes.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A Gestão Local deve manter exatamente um motivo ativo para " + categoria + ".");
            motivo = opcoes.get(0);
        }
        if (disponibilidadeCatalogo != null)
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO, motivo.getId());
        if (motivo.getGestaoLocal() == null || !Objects.equals(motivo.getGestaoLocal().getId(), gestaoDoFato.getId())
                || !motivoAtivoEm(motivo, dataEfetiva) || categoria != null && contextoMotivo(motivo, contrato, dataEfetiva).categoria() != categoria) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de desligamento inválido para o contrato.");
        }
        return motivo;
    }

    private MotivoDesligamento obterMotivoParaDesligamento(VinculoContratual vinculo,
                                                            DesligamentoVinculoRequisicao req) {
        MotivoDesligamento motivo = obterMotivo(vinculo.getContrato(), req.motivoDesligamentoId(), null, req.dataFim());
        CategoriaTerminoVinculo categoria = exigirContextoMotivo(motivo, vinculo.getContrato(), req.dataFim()).categoria();
        if (categoria == CategoriaTerminoVinculo.DESLIGAMENTO_INDIVIDUAL || categoria == CategoriaTerminoVinculo.OUTRO)
            return motivo;
        if (motorContrato == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A causa contratual do término não pôde ser reconstruída.");
        Instant conhecimento = Instant.now();
        List<EventoCicloVidaContrato> eventos = motorContrato.eventosValidosOrdenados(
                vinculo.getContrato().getId(), req.dataFim(), conhecimento);
        boolean comprovada = switch (categoria) {
            case ANULACAO_CONTRATO -> eventos.stream().anyMatch(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.ANULACAO
                    && req.dataFim().equals(e.getDataEfetiva()));
            case ENCERRAMENTO_ANTECIPADO_CONTRATO -> eventos.stream().anyMatch(e ->
                    e.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO
                            && req.dataFim().equals(e.getDataEfetiva()));
            case FIM_VIGENCIA_CONTRATO -> fimNaturalComprovado(vinculo, req.dataFim(), conhecimento, eventos);
            default -> false;
        };
        if (!comprovada) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O motivo de término não corresponde a um fato contratual válido na data informada.");
        return motivo;
    }

    private boolean fimNaturalComprovado(VinculoContratual vinculo, LocalDate dataFim, Instant conhecimento,
                                          List<EventoCicloVidaContrato> eventos) {
        PosicaoTemporalContratoDTO posicao = motorContrato.reconstruirPosicao(vinculo.getContrato(), dataFim, conhecimento);
        // A rescisão também reduz vigenciaFim; igualdade da data, sozinha, não
        // transforma esse ato em término natural. Anulação/cancelamento tampouco.
        if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || eventos.stream().anyMatch(e -> e.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO))
            return false;
        if (dataFim.equals(posicao.vigenciaFim())) return true;
        if (!dataFim.equals(vinculo.getDataFim())
                || vinculo.getMotivoDesligamentoCategoriaFotografia() != CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO
                || posicao.vigenciaFim() == null || !posicao.vigenciaFim().isAfter(dataFim)) return false;

        // A prorrogação tardia não invalida o fim natural já realizado. Exige-se
        // a prova do próprio vínculo: uma causa individual não ganha essa exceção.
        for (EventoVinculoContratual evento : eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId())) {
            if (!dataFim.equals(evento.getFimNovo())
                    || evento.getMotivoDesligamentoCategoriaFotografia() != CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO)
                continue;
            if (evento.getTipoEvento() == TipoEventoVinculoContratual.FIM_NATURAL_CONTRATO) return true;
            if (evento.getRegistradoEm() == null || evento.getTipoEvento() != TipoEventoVinculoContratual.DESLIGAMENTO
                    && evento.getTipoEvento() != TipoEventoVinculoContratual.RETIFICACAO_DESLIGAMENTO) continue;
            PosicaoTemporalContratoDTO anterior = motorContrato.reconstruirPosicao(
                    vinculo.getContrato(), dataFim, evento.getRegistradoEm());
            if (anterior.estadoNegocial() == EstadoNegocialContrato.PUBLICADO && dataFim.equals(anterior.vigenciaFim())
                    && anterior.eventosAplicados().stream().noneMatch(e -> e.tipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO))
                return true;
        }
        return false;
    }

    private boolean motivoAtivoEm(MotivoDesligamento motivo, LocalDate dataEfetiva) {
        return disponibilidadeCatalogo == null ? Boolean.TRUE.equals(motivo.getAtivo())
                : disponibilidadeCatalogo.ativoEm(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.MOTIVO_DESLIGAMENTO,
                        motivo.getId(), dataEfetiva, motivo.getAtivo());
    }

    private ContextoMotivoHistoricoDTO contextoMotivo(MotivoDesligamento motivo, Contrato contrato, LocalDate data) {
        return motivoHistorico == null ? ContextoMotivoHistoricoDTO.ausente()
                : motivoHistorico.capturarNovoFato(motivo, contrato, data, Instant.now());
    }

    private ContextoMotivoHistoricoDTO exigirContextoMotivo(MotivoDesligamento motivo, Contrato contrato, LocalDate data) {
        ContextoMotivoHistoricoDTO contexto = contextoMotivo(motivo, contrato, data);
        if (!contexto.comprovado() || contexto.categoria() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Descrição/categoria do motivo sem fonte comprovada na data. Incorpore a prova histórica antes do término.");
        return contexto;
    }

    private void aplicarDesligamento(VinculoContratual v, LocalDate dataFim, MotivoDesligamento motivo, UsuarioAplicacao user) {
        ContextoMotivoHistoricoDTO contexto = exigirContextoMotivo(motivo, v.getContrato(), dataFim);
        v.setDataFim(dataFim); v.setMotivoDesligamento(motivo);
        v.setMotivoDesligamentoDescricaoFotografia(contexto.descricao());
        v.setMotivoDesligamentoCategoriaFotografia(contexto.categoria());
        v.setDesligamentoRegistradoEm(Instant.now());
        v.setDesligamentoRegistradoPorId(user == null ? null : user.getId());
        v.setDesligamentoRegistradoPorUsername(user == null ? null : user.getUsername());
        vinculoRepositorio.save(v);
    }

    private void sincronizarAlocacao(VinculoContratual vinculo, UsuarioAplicacao user, String justificativa) {
        if (alocacaoVinculoServico != null) {
            alocacaoVinculoServico.sincronizarLimitesVinculo(vinculo, user,
                    justificativa == null || justificativa.isBlank() ? "Sincronização com alteração temporal do vínculo" : justificativa);
        }
    }

    private void sincronizarAlocacao(VinculoContratual vinculo, UsuarioAplicacao user, String justificativa,
                                      String evidencia, String autorizacao, AlocacaoVinculoServico.AmpliacaoFim ampliacao) {
        if (alocacaoVinculoServico != null) {
            alocacaoVinculoServico.sincronizarLimitesVinculo(vinculo, user, justificativa, evidencia, autorizacao, ampliacao);
        }
    }

    private void exigirProgramacoesCompativeis(VinculoContratual vinculo, LocalDate inicio, LocalDate fim) {
        if (alocacaoVinculoServico != null) alocacaoVinculoServico.exigirTratamentoProgramacoesFuturas(vinculo, inicio, fim);
    }

    private void adicionarImpactosProgramacoes(List<String> impactos, VinculoContratual vinculo, LocalDate inicio, LocalDate fim) {
        if (alocacaoVinculoServico == null) return;
        List<Long> programacoes = alocacaoVinculoServico.programacoesFuturasIncompativeis(vinculo, inicio, fim);
        if (!programacoes.isEmpty()) impactos.add("Alocações futuras " + programacoes
                + " ficam incompatíveis e exigem cancelamento ou retificação explícita antes da conclusão.");
        impactos.addAll(alocacaoVinculoServico.impactosTransferenciasRestauradas(vinculo, inicio, fim));
    }

    private void exigirJustificativaCompleta(String justificativa, String evidencia, String autorizacao) {
        if (vazio(justificativa) || vazio(evidencia) || vazio(autorizacao)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Justificativa, evidência e autorização são obrigatórias.");
    }
    private void validarConfirmacao(Boolean confirmar, String recebido, String esperado) {
        if (!Boolean.TRUE.equals(confirmar) || recebido == null || !recebido.equals(esperado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prévia de impacto expirou ou não foi confirmada.");
        }
    }
    private String tokenPrevia(VinculoContratual v, LocalDate inicio, LocalDate fim, List<Long> conflitos, Object... intencao) {
        String hash = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(v.getId()).map(EventoVinculoContratual::getHashEvento).orElse("SEM_EVENTO");
        List<Object> partes = new ArrayList<>(AssinaturaPreviaNegocial.partes(v.getId(), v.getVersion(), v.getDataInicio(), v.getDataFim(), inicio, fim, hash));
        partes.add(conflitos.stream().sorted().toList());
        partes.add(AssinaturaPreviaNegocial.partes(intencao));
        if (assinaturaImpacto != null) {
            LocalDate desde = inicio.isBefore(v.getDataInicio()) ? inicio : v.getDataInicio();
            partes.addAll(assinaturaImpacto.partesPorVinculo(v, desde, null));
        }
        return AssinaturaPreviaNegocial.sha256("sgt.previa.vinculo.v1", partes);
    }

    private EventoVinculoContratual registrarEvento(VinculoContratual v, TipoEventoVinculoContratual tipo,
                                                     LocalDate dataEfetiva, UsuarioAplicacao user,
                                                     String justificativa, String evidencia, String autorizacao,
                                                     LocalDate inicioAnterior, LocalDate inicioNovo,
                                                     LocalDate fimAnterior, LocalDate fimNovo,
                                                     String matriculaAnterior, String matriculaNova,
                                                     TipoRelacaoEventoVinculo relacao) {
        return eventoRepositorio.save(montarEvento(v, tipo, dataEfetiva, user, justificativa, evidencia,
                autorizacao, inicioAnterior, inicioNovo, fimAnterior, fimNovo, matriculaAnterior,
                matriculaNova, relacao));
    }

    private EventoVinculoContratual registrarEvento(VinculoContratual v, TipoEventoVinculoContratual tipo,
                                                     LocalDate dataEfetiva, UsuarioAplicacao user,
                                                     String justificativa, String evidencia, String autorizacao,
                                                     LocalDate inicioAnterior, LocalDate inicioNovo,
                                                     LocalDate fimAnterior, LocalDate fimNovo,
                                                     String matriculaAnterior, String matriculaNova,
                                                     TipoRelacaoEventoVinculo relacao,
                                                     AlocacaoVinculoServico.AmpliacaoFim ampliacao) {
        EventoVinculoContratual evento = montarEvento(v, tipo, dataEfetiva, user, justificativa, evidencia,
                autorizacao, inicioAnterior, inicioNovo, fimAnterior, fimNovo, matriculaAnterior, matriculaNova, relacao);
        if (ampliacao != null && ampliacao.capacidade().excede()) {
            completarEventoExcedente(evento, ampliacao.combinacao(), ampliacao.capacidade(),
                    justificativa, evidencia, autorizacao);
        }
        // A trilha é imutável: o consentimento precisa fazer parte do primeiro INSERT.
        return eventoRepositorio.save(evento);
    }

    private EventoVinculoContratual montarEvento(VinculoContratual v, TipoEventoVinculoContratual tipo,
                                                  LocalDate dataEfetiva, UsuarioAplicacao user,
                                                  String justificativa, String evidencia, String autorizacao,
                                                  LocalDate inicioAnterior, LocalDate inicioNovo,
                                                  LocalDate fimAnterior, LocalDate fimNovo,
                                                  String matriculaAnterior, String matriculaNova,
                                                  TipoRelacaoEventoVinculo relacao) {
        EventoVinculoContratual precedente = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(v.getId()).orElse(null);
        EventoVinculoContratual e = new EventoVinculoContratual();
        e.setVinculo(v); e.setTipoEvento(tipo); e.setDataEfetiva(dataEfetiva); e.setResponsavelId(user == null ? null : user.getId());
        e.setResponsavelUsername(user == null ? null : user.getUsername()); e.setJustificativa(justificativa); e.setEvidencia(evidencia); e.setAutorizacao(autorizacao);
        e.setEventoPrecedente(precedente); e.setRelacaoPrecedencia(precedente == null ? TipoRelacaoEventoVinculo.ORIGEM : relacao);
        e.setInicioAnterior(inicioAnterior); e.setInicioNovo(inicioNovo); e.setFimAnterior(fimAnterior); e.setFimNovo(fimNovo);
        e.setMatriculaAnterior(matriculaAnterior); e.setMatriculaNova(matriculaNova);
        MotivoDesligamento motivoAplicado = v.getMotivoDesligamento();
        if (motivoAplicado != null) {
            e.setMotivoDesligamentoIdFotografia(motivoAplicado.getId());
            e.setMotivoDesligamentoDescricaoFotografia(descricaoMotivoFotografada(v));
            e.setMotivoDesligamentoCategoriaFotografia(v.getMotivoDesligamentoCategoriaFotografia());
        }
        e.setPessoaNomeFotografia(nomeFotografadoParaEvento(v, tipo, dataEfetiva, inicioAnterior));
        e.setPessoaCpfFotografia(v.getPessoa().getCpf());
        e.setContratoNumeroFotografia(v.getContrato().getNumeroContrato()); e.setContratoAnoFotografia(v.getContrato().getAnoContrato());
        GestaoLocal gestao = gestaoTemporal == null ? v.getContrato().getGestaoLocal()
                : gestaoTemporal.gestaoEm(v.getContrato(), dataEfetiva, Instant.now());
        e.setGestaoLocalIdFotografia(gestao == null ? null : gestao.getId());
        e.setGestaoLocalNomeFotografia(nomeGestaoFotografadoParaEvento(v, dataEfetiva, gestao));
        String hashAnterior = precedente == null ? null : precedente.getHashEvento(); e.setHashAnterior(hashAnterior);
        e.setHashEvento(MaterialHashEventos.sha256("sgt.evento-vinculo.base",
                texto(hashAnterior), enumeracao(tipo), data(dataEfetiva), data(inicioAnterior), data(inicioNovo),
                data(fimAnterior), data(fimNovo), texto(matriculaAnterior), texto(matriculaNova),
                texto(v.getPessoa().getCpf()), longo(v.getContrato().getId()), longo(user == null ? null : user.getId()),
                texto(justificativa), longo(e.getMotivoDesligamentoIdFotografia()),
                texto(e.getMotivoDesligamentoDescricaoFotografia()), enumeracao(e.getMotivoDesligamentoCategoriaFotografia())));
        return e;
    }

    private String nomeGestaoFotografadoParaEvento(VinculoContratual vinculo,
                                                    LocalDate dataEfetiva, GestaoLocal gestao) {
        if (gestao == null) return null;
        if (!dataEfetiva.isBefore(RelogioSistema.hoje())) return gestao.getNome();
        // A responsabilidade é resolvida por data; seu nome também precisa de
        // fonte histórica. O cadastro atual não documenta uma renomeação no
        // passado. A projeção negocial reaplica publicação e transferências.
        if (aditamentoContratualServico == null) return null;
        VersaoContratoNegocialDTO fonte = aditamentoContratualServico.projetarVersaoContrato(
                vinculo.getContrato(), dataEfetiva, Instant.now(), Set.of());
        if (fonte == null || !Objects.equals(gestao.getId(), fonte.gestaoLocalId())) return null;
        String nome = fonte.gestaoLocalNome();
        return nome == null || nome.isBlank() ? null : nome;
    }

    private String nomeFotografadoParaEvento(VinculoContratual vinculo,
                                               TipoEventoVinculoContratual tipo, LocalDate dataEfetiva,
                                               LocalDate inicioAnterior) {
        return identidadeHistorica == null ? null
                : identidadeHistorica.capturarNovoFato(vinculo.getPessoa(), vinculo, dataEfetiva, Instant.now()).nome();
    }

    private void completarEventoExcedente(EventoVinculoContratual evento, AlocacaoVinculoServico.CombinacaoResolvida composicao,
                                           AlocacaoVinculoServico.CapacidadeInclusao capacidade,
                                           CadastroVinculoContratualRequisicao req) {
        completarEventoExcedente(evento, composicao, capacidade,
                req.justificativaExcedente() + capacidade.detalheTemporal(req.dataInicio()),
                req.evidenciaExcedente(), req.autorizacaoExcedente());
    }

    private void completarEventoExcedente(EventoVinculoContratual evento, AlocacaoVinculoServico.CombinacaoResolvida composicao,
                                           AlocacaoVinculoServico.CapacidadeInclusao capacidade,
                                           String justificativa, String evidencia, String autorizacao) {
        evento.setInclusaoExcedente(true);
        Long baseId = composicao.composicao() == null ? null : composicao.composicao().getId();
        evento.setComposicaoExcedenteId(baseId);
        evento.setLotacaoExcedenteId(composicao.lotacao().getId());
        evento.setJornadaExcedenteId(composicao.jornada().getId());
        evento.setCargoExcedenteId(composicao.cargo().getId());
        evento.setEfetivoPrevistoExcedente(capacidade.efetivoPrevisto());
        evento.setOcupacaoAposInclusaoExcedente(capacidade.ocupacaoAposInclusao());
        evento.setJustificativa(justificativa);
        evento.setEvidencia(evidencia);
        evento.setAutorizacao(autorizacao);
        evento.setHashEvento(MaterialHashEventos.sha256("sgt.evento-vinculo.excedente",
                texto(evento.getHashEvento()), longo(baseId), longo(composicao.lotacao().getId()),
                longo(composicao.jornada().getId()), longo(composicao.cargo().getId()),
                inteiro(capacidade.efetivoPrevisto()), longo(capacidade.ocupacaoAposInclusao()),
                texto(justificativa), texto(evidencia), texto(autorizacao)));
    }

    private VinculoContratualDTO paraDTO(VinculoContratual v, UsuarioAplicacao usuario) {
        AlocacaoVinculo inicial = alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(v.getId())
                .stream().findFirst().orElse(null);
        return paraDTO(v, usuario, inicial);
    }

    private VinculoContratualDTO paraDTO(VinculoContratual v, UsuarioAplicacao usuario, AlocacaoVinculo a) {
        if (escopoLeitura != null && !escopoLeitura.permiteAlocacao(usuario, a)) a = null;
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        Contrato c = v.getContrato(); GestaoLocal g = gestaoTemporal == null ? c.getGestaoLocal() : gestaoTemporal.gestaoAtual(c);
        MotivoDesligamento m = v.getMotivoDesligamento();
        return new VinculoContratualDTO(v.getId(), v.getPessoa().getId(),
                minimizador.nome(v.getPessoa().getNome(), nivel), minimizador.cpf(v.getPessoa().getCpf(), nivel),
                c.getId(), c.getNumeroContrato(), c.getAnoContrato(), g == null ? null : g.getId(), g == null ? null : g.getNome(),
                v.getDataInicio(), v.getDataFim(), minimizador.matricula(v.getMatricula(), nivel),
                v.getEstadoNegocial(), motor.reconstruir(v, RelogioSistema.hoje(), Instant.now()).situacaoTemporal(),
                m == null ? null : m.getId(), m == null ? null : descricaoMotivoFotografada(v), v.getDesligamentoRegistradoEm(),
                a == null || a.getComposicao() == null ? null : a.getComposicao().getId(), a == null ? null : a.getLotacao().getId(),
                a == null ? null : a.getJornada().getId(), a == null ? null : a.getCargo().getId(), v.getVersion());
    }

    private String descricaoMotivoFotografada(VinculoContratual vinculo) {
        String valor = vinculo.getMotivoDesligamentoDescricaoFotografia();
        return valor == null || valor.isBlank() ? null : valor;
    }

    private EscopoConsulta escopoConsulta(UsuarioAplicacao user) {
        if (acesso.ehGlobal(user) || acesso.ehPresidente(user)) return new EscopoConsulta(true, null, null);
        if (acesso.ehGestorEstadual(user)) {
            SuperintendenciaEstadual se = acesso.superintendenciaEfetiva(user);
            return new EscopoConsulta(false, null, se == null ? null : se.getId());
        }
        GestaoLocal gestao = acesso.gestaoLocalEfetiva(user);
        return new EscopoConsulta(false, gestao == null ? null : gestao.getId(), null);
    }

    private record EscopoConsulta(boolean global, Long gestaoLocalId, Long superintendenciaId) {}
    private EventoVinculoContratualDTO paraDTO(EventoVinculoContratual e, UsuarioAplicacao usuario) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        return new EventoVinculoContratualDTO(e.getId(), e.getVinculo().getId(), e.getTipoEvento(), e.getDataEfetiva(), e.getRegistradoEm(),
                e.getResponsavelId(), e.getResponsavelUsername(), minimizador.restrito(e.getJustificativa(), nivel),
                minimizador.restrito(e.getEvidencia(), nivel), minimizador.restrito(e.getAutorizacao(), nivel),
                e.getEventoPrecedente() == null ? null : e.getEventoPrecedente().getId(), e.getRelacaoPrecedencia(),
                e.getInicioAnterior(), e.getInicioNovo(), e.getFimAnterior(), e.getFimNovo(),
                minimizador.matricula(e.getMatriculaAnterior(), nivel), minimizador.matricula(e.getMatriculaNova(), nivel),
                e.getMotivoDesligamentoIdFotografia(), e.getMotivoDesligamentoDescricaoFotografia(),
                e.getMotivoDesligamentoCategoriaFotografia(),
                e.isInclusaoExcedente(), e.getComposicaoExcedenteId(), e.getLotacaoExcedenteId(),
                e.getJornadaExcedenteId(), e.getCargoExcedenteId(), e.getEfetivoPrevistoExcedente(),
                e.getOcupacaoAposInclusaoExcedente(),
                minimizador.nome(e.getPessoaNomeFotografia(), nivel), minimizador.cpf(e.getPessoaCpfFotografia(), nivel),
                e.getContratoNumeroFotografia(), e.getContratoAnoFotografia(),
                e.getGestaoLocalIdFotografia(), e.getGestaoLocalNomeFotografia(), e.getHashAnterior(), e.getHashEvento());
    }
    private boolean vazio(String v) { return v == null || v.isBlank(); }
}
