package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dominio.acesso.NivelExposicaoDadosPessoais;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.siggtrat.utilitario.UtilitarioPaginacao;

@Service
@Transactional(readOnly = true)
public class VinculoContratualServico {
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

    public Page<VinculoContratualDTO> listar(Long contratoId, Long pessoaId, EstadoNegocialVinculo estado,
                                             LocalDate inicioDe, LocalDate inicioAte, LocalDate ativoEm,
                                             String busca, Pageable pageable) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        if (!acesso.podeConsultarEmpregados(user)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a vínculos.");
        EscopoConsulta escopo = escopoConsulta(user);
        String termo = busca == null || busca.isBlank() ? null : busca.trim().toLowerCase(Locale.ROOT);
        String cpf = termo == null ? null : termo.replaceAll("\\D", "");
        if (cpf != null && cpf.isBlank()) cpf = null;
        Pageable limitado = UtilitarioPaginacao.limitarComOrdenacaoFixa(pageable,
                Sort.by(Sort.Direction.DESC, "dataInicio").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<VinculoContratual> pagina = vinculoRepositorio.pesquisarNoEscopo(
                escopo.global(), escopo.gestaoLocalId(), escopo.superintendenciaId(),
                contratoId, pessoaId, estado, inicioDe, inicioAte, ativoEm, termo, cpf, limitado);
        List<Long> ids = pagina.getContent().stream().map(VinculoContratual::getId).toList();
        Map<Long, AlocacaoVinculo> iniciais = ids.isEmpty() ? Map.of()
                : alocacaoRepositorio.buscarPorVinculos(ids).stream().collect(Collectors.toMap(
                        a -> a.getVinculo().getId(), a -> a, (primeira, ignorada) -> primeira,
                        LinkedHashMap::new));
        return pagina.map(v -> paraDTO(v, user, iniciais.get(v.getId())));
    }

    public VinculoContratualDTO buscar(Long id) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id);
        acesso.exigirAcessoContrato(user, v.getContrato());
        return paraDTO(v, user);
    }

    public List<EventoVinculoContratualDTO> listarEventos(Long id) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id);
        acesso.exigirAcessoContrato(user, v.getContrato());
        return eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(id).stream()
                .map(e -> paraDTO(e, user)).toList();
    }

    public PosicaoPessoaDTO posicaoPessoa(Long pessoaId, LocalDate data, Instant conhecidoAte) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        Pessoa pessoa = pessoaServico.obterEntidade(pessoaId);
        List<VinculoContratual> permitidos = vinculoRepositorio.findByPessoaIdOrderByDataInicioAscIdAsc(pessoaId).stream()
                .filter(v -> acesso.podeAcessarContrato(user, v.getContrato())).toList();
        if (permitidos.isEmpty() && !acesso.ehGlobal(user)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada no seu escopo.");
        LocalDate referencia = data == null ? RelogioSistema.hoje() : data;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(user);
        return new PosicaoPessoaDTO(pessoaId, minimizador.nome(pessoa.getNome(), nivel),
                minimizador.cpf(pessoa.getCpf(), nivel), referencia, conhecimento,
                permitidos.stream().map(v -> motor.reconstruir(v, referencia, conhecimento)).toList());
    }

    @Transactional
    public VinculoContratualDTO cadastrar(CadastroVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(user);
        validarCadastro(req);
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(req.contratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(user, contrato);
        exigirContratoPublicado(contrato);

        ComposicaoContratual composicao = validarComposicao(contrato, req.composicaoId(), req.dataInicio());
        if (alocacaoVinculoServico == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Validador temporal de capacidade indisponível.");
        }
        AlocacaoVinculoServico.CapacidadeInclusao capacidade =
                alocacaoVinculoServico.capacidadeInclusao(contrato, composicao, req.dataInicio());
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
        String matricula = validarMatricula(contrato, req.matricula());

        VinculoContratual vinculo = new VinculoContratual();
        vinculo.setPessoa(pessoa); vinculo.setContrato(contrato); vinculo.setDataInicio(req.dataInicio());
        vinculo.setDataFim(req.dataFim()); vinculo.setMatricula(matricula);
        vinculo.setEstadoNegocial(EstadoNegocialVinculo.REGISTRADO);
        vinculo.setCriadoPorId(user.getId()); vinculo.setCriadoPorUsername(user.getUsername());
        vinculo = vinculoRepositorio.save(vinculo);

        if (matricula != null) reservarMatricula(vinculo, matricula);
        AlocacaoVinculo alocacaoInicial = alocacaoVinculoServico.criarInicial(vinculo, composicao, user);
        if (projecaoOperacionalPessoa != null) {
            projecaoOperacionalPessoa.projetar(vinculo, alocacaoInicial);
        }
        EventoVinculoContratual evento = registrarEvento(vinculo, TipoEventoVinculoContratual.CRIACAO, req.dataInicio(), user,
                "Cadastro integrado de Pessoa, Vínculo e Alocação inicial", null, null,
                null, req.dataInicio(), null, req.dataFim(), null, matricula, TipoRelacaoEventoVinculo.ORIGEM);
        if (capacidade.excede()) {
            completarEventoExcedente(evento, composicao, capacidade, req);
        }
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
        String token = tokenPrevia(v, req.novoInicio(), v.getDataFim(), conflitos);
        return new PreviaImpactoVinculoDTO(token, id, v.getDataInicio(), req.novoInicio(), List.copyOf(impactos), conflitos);
    }

    @Transactional
    public VinculoContratualDTO retificarInicio(Long id, RetificacaoInicioVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        PreviaImpactoVinculoDTO previa = previaRetificacaoInicio(id, req);
        validarConfirmacao(req.confirmarImpacto(), req.tokenPrevia(), previa.token());
        if (!previa.vinculosConflitantes().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Retificação produziria sobreposição global de vínculos.");
        LocalDate anterior = v.getDataInicio();
        v.setDataInicio(req.novoInicio());
        vinculoRepositorio.save(v);
        sincronizarAlocacao(v, user, req.justificativa());
        registrarEvento(v, TipoEventoVinculoContratual.RETIFICACAO_INICIO, req.novoInicio(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), anterior, req.novoInicio(),
                v.getDataFim(), v.getDataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.RETIFICA);
        revalidarFrequencias(v, anterior.isBefore(req.novoInicio()) ? anterior : req.novoInicio(),
                "Retificação retroativa do início do Vínculo Contratual");
        invalidarRelatorios(v, anterior.isBefore(req.novoInicio()) ? anterior : req.novoInicio(),
                "Retificação retroativa do início do Vínculo Contratual", user);
        return paraDTO(v, user);
    }

    @Transactional
    public VinculoContratualDTO alterarMatricula(Long id, AlteracaoMatriculaVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        if (req == null || req.dataEfetiva() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data efetiva da matrícula é obrigatória.");
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        if (req.dataEfetiva().isAfter(RelogioSistema.hoje())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alteração futura de matrícula não é permitida.");
        if (req.dataEfetiva().isBefore(v.getDataInicio()) || v.getDataFim() != null && req.dataEfetiva().isAfter(v.getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da matrícula deve estar dentro do vínculo.");
        }
        String nova = validarMatricula(v.getContrato(), req.novaMatricula());
        if (nova == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova matrícula é obrigatória nesta operação.");
        if (Objects.equals(nova, VinculoContratual.normalizarMatricula(v.getMatricula()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova matrícula é igual à atual.");
        reservarMatricula(v, nova);
        String anterior = v.getMatricula(); v.setMatricula(nova); vinculoRepositorio.save(v);
        registrarEvento(v, TipoEventoVinculoContratual.ALTERACAO_MATRICULA, req.dataEfetiva(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                v.getDataFim(), v.getDataFim(), anterior, nova, TipoRelacaoEventoVinculo.SUBSTITUI);
        return paraDTO(v, user);
    }

    @Transactional
    public VinculoContratualDTO cancelarProgramacao(Long id, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        if (v.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO || !RelogioSistema.hoje().isBefore(v.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente vínculo futuro ainda não iniciado pode ter sua programação cancelada.");
        }
        v.setEstadoNegocial(EstadoNegocialVinculo.CANCELADO); vinculoRepositorio.save(v);
        registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_PROGRAMACAO, v.getDataInicio(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                v.getDataFim(), v.getDataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA);
        return paraDTO(v, user);
    }

    @Transactional
    public VinculoContratualDTO desligar(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        validarDesligamento(v, req, false);
        validarSemSobreposicao(v.getPessoa().getId(), v.getId(), v.getDataInicio(), req.dataFim());
        MotivoDesligamento motivo = obterMotivo(v.getContrato(), req.motivoDesligamentoId(), null);
        LocalDate anterior = v.getDataFim();
        aplicarDesligamento(v, req.dataFim(), motivo, user);
        sincronizarAlocacao(v, user, req.justificativa());
        registrarEvento(v, TipoEventoVinculoContratual.DESLIGAMENTO, req.dataFim(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, req.dataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.ORIGEM);
        revalidarFrequencias(v, req.dataFim().plusDays(1), "Desligamento com repercussão temporal em frequência");
        return paraDTO(v, user);
    }

    public PreviaImpactoVinculoDTO previaRetificacaoDesligamento(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual();
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        validarDesligamento(v, req, true);
        List<Long> conflitos = conflitos(v.getPessoa().getId(), v.getId(), v.getDataInicio(), req.dataFim());
        List<String> impactos = new ArrayList<>(List.of("alocações", "frequências", "matrícula", "relatórios", "eventos posteriores"));
        if (!conflitos.isEmpty()) impactos.add("sobreposição global com vínculos " + conflitos);
        String token = tokenPrevia(v, v.getDataInicio(), req.dataFim(), conflitos);
        return new PreviaImpactoVinculoDTO(token, id, v.getDataFim(), req.dataFim(), List.copyOf(impactos), conflitos);
    }

    @Transactional
    public VinculoContratualDTO retificarDesligamento(Long id, DesligamentoVinculoRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        if (v.getDataFim() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo ainda não possui desligamento para retificar.");
        validarDesligamento(v, req, true);
        PreviaImpactoVinculoDTO previa = previaRetificacaoDesligamento(id, req);
        validarConfirmacao(req.confirmarImpacto(), req.tokenPrevia(), previa.token());
        if (!previa.vinculosConflitantes().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Retificação produziria sobreposição global de vínculos.");
        MotivoDesligamento motivo = obterMotivo(v.getContrato(), req.motivoDesligamentoId(), null);
        LocalDate anterior = v.getDataFim();
        aplicarDesligamento(v, req.dataFim(), motivo, user);
        sincronizarAlocacao(v, user, req.justificativa());
        registrarEvento(v, TipoEventoVinculoContratual.RETIFICACAO_DESLIGAMENTO, req.dataFim(), user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, req.dataFim(), v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.RETIFICA);
        LocalDate desdeImpacto = (anterior == null || req.dataFim().isBefore(anterior) ? req.dataFim() : anterior).plusDays(1);
        revalidarFrequencias(v, desdeImpacto, "Retificação retroativa do desligamento");
        invalidarRelatorios(v, desdeImpacto, "Retificação retroativa do desligamento", user);
        return paraDTO(v, user);
    }

    @Transactional
    public VinculoContratualDTO cancelarDesligamentoProgramado(Long id, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        VinculoContratual v = obter(id); acesso.exigirAcessoContrato(user, v.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(), req == null ? null : req.autorizacao());
        if (v.getDataFim() == null || v.getDataFim().isBefore(RelogioSistema.hoje())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente desligamento ainda programado pode ser cancelado.");
        }
        LocalDate anterior = v.getDataFim();
        v.setDataFim(null); v.setMotivoDesligamento(null); v.setDesligamentoRegistradoEm(null);
        v.setDesligamentoRegistradoPorId(null); v.setDesligamentoRegistradoPorUsername(null);
        vinculoRepositorio.save(v);
        sincronizarAlocacao(v, user, req.justificativa());
        registrarEvento(v, TipoEventoVinculoContratual.CANCELAMENTO_DESLIGAMENTO_PROGRAMADO, anterior, user,
                req.justificativa(), req.evidencia(), req.autorizacao(), v.getDataInicio(), v.getDataInicio(),
                anterior, null, v.getMatricula(), v.getMatricula(), TipoRelacaoEventoVinculo.CANCELA);
        revalidarFrequencias(v, anterior.plusDays(1), "Cancelamento/retificação de desligamento programado");
        return paraDTO(v, user);
    }

    /** Trata explicitamente os vínculos que ficaram abertos no fim natural. */
    @Transactional
    public List<VinculoContratualDTO> tratarFimNatural(Long contratoId) {
        UsuarioAplicacao user = acesso.usuarioAtual(); acesso.exigirGestaoEmpregado(user);
        Contrato contrato = obterContrato(contratoId); acesso.exigirAcessoContrato(user, contrato);
        if (contrato.getDataFim() == null || !contrato.getDataFim().isBefore(RelogioSistema.hoje())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O contrato ainda não alcançou o fim natural de vigência.");
        }
        MotivoDesligamento motivo = obterMotivo(contrato, null, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        encerrarVinculosAbertos(contrato, contrato.getDataFim(), motivo, user,
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
        MotivoDesligamento motivo = obterMotivo(contrato, null, categoria);
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
        MotivoDesligamento motivo = obterMotivo(contrato, null, CategoriaTerminoVinculo.FIM_VIGENCIA_CONTRATO);
        encerrarVinculosAbertos(contrato, fimAntigo, motivo, user, TipoEventoVinculoContratual.FIM_NATURAL_CONTRATO,
                justificativa == null ? "Prorrogação tardia: preservação do fim antigo; reabertura exige retificação explícita" : justificativa);
    }

    public long contarOperacionaisNoContrato(Long contratoId, LocalDate data) {
        return vinculoRepositorio.countOperacionaisNoContrato(contratoId, data == null ? RelogioSistema.hoje() : data);
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
        return eventoRepositorio.findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(contratoId)
                .stream()
                .filter(e -> ate == null || !e.getDataEfetiva().isAfter(ate))
                .filter(e -> Objects.equals(lotacaoId, e.getLotacaoExcedenteId()))
                .filter(e -> Objects.equals(jornadaId, e.getJornadaExcedenteId()))
                .filter(e -> Objects.equals(cargoId, e.getCargoExcedenteId()))
                .map(e -> new JustificativaExcedenteDTO(e.getId(), e.getVinculo().getId(), e.getDataEfetiva(),
                        e.getRegistradoEm(), e.getResponsavelUsername(), e.getJustificativa(), e.getEvidencia(),
                        e.getAutorizacao(), Optional.ofNullable(e.getEfetivoPrevistoExcedente()).orElse(0),
                        Optional.ofNullable(e.getOcupacaoAposInclusaoExcedente()).orElse(0L)))
                .toList();
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
        if (revalidacaoFrequenciaTemporalServico != null && vinculo != null && vinculo.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorVinculo(vinculo.getId(), desde, motivo);
        }
    }

    private void invalidarRelatorios(VinculoContratual vinculo, LocalDate desde,
                                     String motivo, UsuarioAplicacao usuario) {
        if (invalidacaoRelatorios != null) {
            invalidacaoRelatorios.invalidarPorVinculo(vinculo, desde, motivo, usuario);
        }
    }

    private void validarCadastro(CadastroVinculoContratualRequisicao req) {
        if (req == null || req.contratoId() == null || req.dataInicio() == null || req.composicaoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato, início efetivo e alocação inicial são obrigatórios.");
        }
        if ((req.pessoaId() == null) == (req.pessoa() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma pessoa existente ou os dados de uma nova pessoa, nunca ambos.");
        }
    }

    private Contrato obterContrato(Long id) {
        return contratoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
    }
    private VinculoContratual obter(Long id) {
        return vinculoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
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
        if (!acesso.ehGestaoLocal(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente Gestor Local pode autorizar inclusão acima do efetivo previsto.");
        }
        if (!Boolean.TRUE.equals(req.confirmarInclusaoExcedente())
                || vazio(req.justificativaExcedente()) || vazio(req.evidenciaExcedente())
                || vazio(req.autorizacaoExcedente())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A inclusão excedente exige confirmação, justificativa, evidência e autorização.");
        }
    }

    private void validarPeriodoNoContrato(Contrato contrato, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim != null && fim.isBefore(inicio)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período do vínculo é inválido.");
        if (contrato.getDataInicio() != null && inicio.isBefore(contrato.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo não pode iniciar antes do contrato.");
        }
        if (contrato.getDataFim() != null && (inicio.isAfter(contrato.getDataFim()) || fim != null && fim.isAfter(contrato.getDataFim()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo deve estar contido na vigência do contrato.");
        }
    }

    private ComposicaoContratual validarComposicao(Contrato contrato, Long composicaoId, LocalDate inicio) {
        ComposicaoContratual c = composicaoRepositorio.findById(composicaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição inicial não encontrada."));
        if (c.getContrato() == null || !Objects.equals(c.getContrato().getId(), contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição inicial pertence a outro contrato.");
        }
        if (c.getDataInicioVigencia() != null && inicio.isBefore(c.getDataInicioVigencia())
                || c.getDataFimVigencia() != null && inicio.isAfter(c.getDataFimVigencia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Composição inicial não é válida no início do vínculo.");
        }
        return c;
    }

    private String validarMatricula(Contrato contrato, String valor) {
        String matricula = VinculoContratual.normalizarMatricula(valor);
        if (contrato.exigeMatriculaNoVinculo() && matricula == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matrícula é obrigatória para este contrato/tipo de contratação.");
        }
        if (matricula != null && matricula.length() > 30) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matrícula deve ter no máximo 30 caracteres.");
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
        List<Long> conflitos = conflitos(pessoaId, ignorarId, inicio, fim);
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

    private void validarDesligamento(VinculoContratual v, DesligamentoVinculoRequisicao req, boolean retificacao) {
        if (req == null || req.dataFim() == null || req.motivoDesligamentoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data final e motivo de desligamento são obrigatórios.");
        }
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        if (req.dataFim().isBefore(v.getDataInicio())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desligamento não pode anteceder o vínculo.");
        if (v.getContrato().getDataFim() != null && req.dataFim().isAfter(v.getContrato().getDataFim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Desligamento não pode ultrapassar o fim da vigência contratual.");
        }
        if (!retificacao && v.getDataFim() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vínculo já possui desligamento; use retificação.");
    }

    private MotivoDesligamento obterMotivo(Contrato contrato, Long id, CategoriaTerminoVinculo categoria) {
        MotivoDesligamento motivo;
        if (id != null) motivo = motivoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de desligamento não encontrado."));
        else {
            List<MotivoDesligamento> opcoes = motivoRepositorio.findByGestaoLocalIdAndCategoriaAndAtivoTrueOrderByDescricaoAsc(
                    contrato.getGestaoLocal().getId(), categoria);
            if (opcoes.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A Gestão Local deve manter exatamente um motivo ativo para " + categoria + ".");
            motivo = opcoes.get(0);
        }
        if (motivo.getGestaoLocal() == null || !Objects.equals(motivo.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())
                || Boolean.FALSE.equals(motivo.getAtivo()) || categoria != null && motivo.getCategoria() != categoria) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motivo de desligamento inválido para o contrato.");
        }
        return motivo;
    }

    private void aplicarDesligamento(VinculoContratual v, LocalDate dataFim, MotivoDesligamento motivo, UsuarioAplicacao user) {
        v.setDataFim(dataFim); v.setMotivoDesligamento(motivo); v.setDesligamentoRegistradoEm(Instant.now());
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

    private void exigirJustificativaCompleta(String justificativa, String evidencia, String autorizacao) {
        if (vazio(justificativa) || vazio(evidencia) || vazio(autorizacao)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Justificativa, evidência e autorização são obrigatórias.");
    }
    private void validarConfirmacao(Boolean confirmar, String recebido, String esperado) {
        if (!Boolean.TRUE.equals(confirmar) || recebido == null || !recebido.equals(esperado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prévia de impacto expirou ou não foi confirmada.");
        }
    }
    private String tokenPrevia(VinculoContratual v, LocalDate inicio, LocalDate fim, List<Long> conflitos) {
        String hash = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(v.getId()).map(EventoVinculoContratual::getHashEvento).orElse("SEM_EVENTO");
        List<String> partes = new ArrayList<>(List.of(String.valueOf(v.getId()), String.valueOf(v.getVersion()),
                AssinaturaPreviaNegocial.valor(v.getDataInicio()), AssinaturaPreviaNegocial.valor(v.getDataFim()),
                AssinaturaPreviaNegocial.valor(inicio), AssinaturaPreviaNegocial.valor(fim), hash));
        conflitos.stream().sorted().forEach(x -> partes.add(String.valueOf(x)));
        return AssinaturaPreviaNegocial.sha256(partes);
    }

    private EventoVinculoContratual registrarEvento(VinculoContratual v, TipoEventoVinculoContratual tipo,
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
        e.setPessoaNomeFotografia(v.getPessoa().getNome()); e.setPessoaCpfFotografia(v.getPessoa().getCpf());
        e.setContratoNumeroFotografia(v.getContrato().getNumeroContrato()); e.setContratoAnoFotografia(v.getContrato().getAnoContrato());
        GestaoLocal gestao = v.getContrato().getGestaoLocal();
        e.setGestaoLocalIdFotografia(gestao == null ? null : gestao.getId()); e.setGestaoLocalNomeFotografia(gestao == null ? null : gestao.getNome());
        String hashAnterior = precedente == null ? null : precedente.getHashEvento(); e.setHashAnterior(hashAnterior);
        e.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|",
                AssinaturaPreviaNegocial.valor(hashAnterior), tipo.name(), AssinaturaPreviaNegocial.valor(dataEfetiva),
                AssinaturaPreviaNegocial.valor(inicioAnterior), AssinaturaPreviaNegocial.valor(inicioNovo),
                AssinaturaPreviaNegocial.valor(fimAnterior), AssinaturaPreviaNegocial.valor(fimNovo),
                AssinaturaPreviaNegocial.valor(matriculaAnterior), AssinaturaPreviaNegocial.valor(matriculaNova),
                v.getPessoa().getCpf(), AssinaturaPreviaNegocial.valor(v.getContrato().getId()),
                AssinaturaPreviaNegocial.valor(user == null ? null : user.getId()), AssinaturaPreviaNegocial.valor(justificativa))));
        return eventoRepositorio.save(e);
    }

    private void completarEventoExcedente(EventoVinculoContratual evento, ComposicaoContratual composicao,
                                           AlocacaoVinculoServico.CapacidadeInclusao capacidade,
                                           CadastroVinculoContratualRequisicao req) {
        evento.setInclusaoExcedente(true);
        evento.setComposicaoExcedenteId(composicao.getId());
        evento.setLotacaoExcedenteId(composicao.getLotacao().getId());
        evento.setJornadaExcedenteId(composicao.getJornada().getId());
        evento.setCargoExcedenteId(composicao.getCargo().getId());
        evento.setEfetivoPrevistoExcedente(capacidade.efetivoPrevisto());
        evento.setOcupacaoAposInclusaoExcedente(capacidade.ocupacaoAposInclusao());
        evento.setJustificativa(req.justificativaExcedente());
        evento.setEvidencia(req.evidenciaExcedente());
        evento.setAutorizacao(req.autorizacaoExcedente());
        evento.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|", evento.getHashEvento(),
                String.valueOf(composicao.getId()), String.valueOf(composicao.getLotacao().getId()),
                String.valueOf(composicao.getJornada().getId()), String.valueOf(composicao.getCargo().getId()),
                String.valueOf(capacidade.efetivoPrevisto()), String.valueOf(capacidade.ocupacaoAposInclusao()),
                req.justificativaExcedente(), req.evidenciaExcedente(), req.autorizacaoExcedente())));
        eventoRepositorio.save(evento);
    }

    private VinculoContratualDTO paraDTO(VinculoContratual v, UsuarioAplicacao usuario) {
        AlocacaoVinculo inicial = alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(v.getId())
                .stream().findFirst().orElse(null);
        return paraDTO(v, usuario, inicial);
    }

    private VinculoContratualDTO paraDTO(VinculoContratual v, UsuarioAplicacao usuario, AlocacaoVinculo a) {
        NivelExposicaoDadosPessoais nivel = minimizador.nivelPadrao(usuario);
        Contrato c = v.getContrato(); GestaoLocal g = c.getGestaoLocal(); MotivoDesligamento m = v.getMotivoDesligamento();
        return new VinculoContratualDTO(v.getId(), v.getPessoa().getId(),
                minimizador.nome(v.getPessoa().getNome(), nivel), minimizador.cpf(v.getPessoa().getCpf(), nivel),
                c.getId(), c.getNumeroContrato(), c.getAnoContrato(), g == null ? null : g.getId(), g == null ? null : g.getNome(),
                v.getDataInicio(), v.getDataFim(), minimizador.matricula(v.getMatricula(), nivel),
                v.getEstadoNegocial(), motor.reconstruir(v, RelogioSistema.hoje(), Instant.now()).situacaoTemporal(),
                m == null ? null : m.getId(), m == null ? null : m.getDescricao(), v.getDesligamentoRegistradoEm(),
                a == null ? null : a.getComposicao().getId(), a == null ? null : a.getLotacao().getId(),
                a == null ? null : a.getJornada().getId(), a == null ? null : a.getCargo().getId(), v.getVersion());
    }

    private EscopoConsulta escopoConsulta(UsuarioAplicacao user) {
        if (acesso.ehGlobal(user)) return new EscopoConsulta(true, null, null);
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
                e.getResponsavelId(), e.getResponsavelUsername(), e.getJustificativa(),
                minimizador.restrito(e.getEvidencia(), nivel), minimizador.restrito(e.getAutorizacao(), nivel),
                e.getEventoPrecedente() == null ? null : e.getEventoPrecedente().getId(), e.getRelacaoPrecedencia(),
                e.getInicioAnterior(), e.getInicioNovo(), e.getFimAnterior(), e.getFimNovo(),
                minimizador.matricula(e.getMatriculaAnterior(), nivel), minimizador.matricula(e.getMatriculaNova(), nivel),
                e.isInclusaoExcedente(), e.getComposicaoExcedenteId(), e.getLotacaoExcedenteId(),
                e.getJornadaExcedenteId(), e.getCargoExcedenteId(), e.getEfetivoPrevistoExcedente(),
                e.getOcupacaoAposInclusaoExcedente(),
                minimizador.nome(e.getPessoaNomeFotografia(), nivel), minimizador.cpf(e.getPessoaCpfFotografia(), nivel),
                e.getContratoNumeroFotografia(), e.getContratoAnoFotografia(),
                e.getGestaoLocalIdFotografia(), e.getGestaoLocalNomeFotografia(), e.getHashAnterior(), e.getHashEvento());
    }
    private boolean vazio(String v) { return v == null || v.isBlank(); }
}
