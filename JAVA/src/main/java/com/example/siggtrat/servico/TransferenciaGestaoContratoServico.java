package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.dominio.documento.TipoFatoDocumental;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.IntencaoAtoTransferencia;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.FotografiaJornada;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Fluxo excepcional, explícito e auditável de transferência de Gestão Local. */
@Service
@Transactional(readOnly = true)
public class TransferenciaGestaoContratoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(TransferenciaGestaoContratoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final TransferenciaGestaoContratoRepositorio transferenciaRepositorio;
    private final GestaoLocalRepositorio gestaoRepositorio;
    private final LotacaoRepositorio lotacaoRepositorio;
    private final JornadaRepositorio jornadaRepositorio;
    private final CargoRepositorio cargoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final MotorTemporalContratoServico motorTemporal;
    private final AlocacaoVinculoServico alocacaoServico;
    private final PermissaoExcepcionalServico permissaoExcepcionalServico;
    private DocumentoComprobatorioServico documentoComprobatorioServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private GestaoContratoTemporalServico gestaoContratoTemporal;
    private TipoContratacaoRepositorio tipoContratacaoRepositorio;
    private AditamentoContratualServico projecaoContratual;
    private ProjecaoOperacionalPessoaServico projecaoPessoas;
    private PreviaCicloVidaContratoServico previaCicloVida;

    private record Preparacao(TransferenciaGestaoContrato retificada, GestaoLocal origem, String nomeOrigem, GestaoLocal destino,
                             TipoContratacao tipoDestino, List<MapeamentoTransferenciaGestao> mapeamentos,
                             LocalDate inicioVigencia, LocalDate fimVigencia) {}

    public TransferenciaGestaoContratoServico(TransferenciaGestaoContratoRepositorio transferenciaRepositorio,
                                              GestaoLocalRepositorio gestaoRepositorio,
                                              LotacaoRepositorio lotacaoRepositorio,
                                              JornadaRepositorio jornadaRepositorio,
                                              CargoRepositorio cargoRepositorio,
                                              ComposicaoContratualRepositorio composicaoRepositorio,
                                              AlocacaoVinculoRepositorio alocacaoRepositorio,
                                              ComposicaoContratualTemporalServico composicaoTemporal,
                                              MotorTemporalContratoServico motorTemporal,
                                              AlocacaoVinculoServico alocacaoServico,
                                              PermissaoExcepcionalServico permissaoExcepcionalServico) {
        this.transferenciaRepositorio = transferenciaRepositorio;
        this.gestaoRepositorio = gestaoRepositorio;
        this.lotacaoRepositorio = lotacaoRepositorio;
        this.jornadaRepositorio = jornadaRepositorio;
        this.cargoRepositorio = cargoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.composicaoTemporal = composicaoTemporal;
        this.motorTemporal = motorTemporal;
        this.alocacaoServico = alocacaoServico;
        this.permissaoExcepcionalServico = permissaoExcepcionalServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDocumentos(DocumentoComprobatorioServico documentoComprobatorioServico) {
        this.documentoComprobatorioServico = documentoComprobatorioServico;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarGestaoContratoTemporal(GestaoContratoTemporalServico servico) {
        this.gestaoContratoTemporal = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarTipos(TipoContratacaoRepositorio repositorio) {
        this.tipoContratacaoRepositorio = repositorio;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarProjecaoContratual(@org.springframework.context.annotation.Lazy AditamentoContratualServico servico) {
        this.projecaoContratual = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarProjecaoPessoas(ProjecaoOperacionalPessoaServico servico) {
        this.projecaoPessoas = servico;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void configurarPreviaCicloVida(PreviaCicloVidaContratoServico servico) {
        this.previaCicloVida = servico;
    }

    public PreviaCicloVidaContratoDTO previa(Contrato contrato, TransferenciaGestaoContratoRequisicao req,
                                            UsuarioAplicacao usuario) {
        Preparacao p = preparar(contrato, req);
        return previaCicloVida.gerar(contrato, "transferir-gestao", acao(req), usuario, partesDaIntencao(contrato, req, p));
    }

    @Transactional
    public TransferenciaGestaoContrato transferir(Contrato contrato, TransferenciaGestaoContratoRequisicao req,
                                                   UsuarioAplicacao usuario) {
        Preparacao p = preparar(contrato, req);
        TransferenciaGestaoContrato retificada = p.retificada();
        GestaoLocal origem = p.origem(), destino = p.destino();
        TipoContratacao tipoDestino = p.tipoDestino();
        List<MapeamentoTransferenciaGestao> mapeamentos = p.mapeamentos();
        AcaoCicloVidaContratoDTO acao = acao(req);
        boolean retificacaoComAlcanceAtualOuPassado = retificada != null
                && (!retificada.getDataEfetiva().isAfter(RelogioSistema.hoje()) || !req.dataEfetiva().isAfter(RelogioSistema.hoje()));
        if (retificacaoComAlcanceAtualOuPassado || !vazio(req.tokenPrevia()) || Boolean.TRUE.equals(req.confirmarImpacto()))
            previaCicloVida.validarConfirmacao(contrato, "transferir-gestao", acao, usuario, partesDaIntencao(contrato, req, p));
        exigirConferencia(req.conferenciaDocumentalConfirmada(), req.versaoConferenciaDocumental());
        var consumo = exigirAutorizacao(contrato, usuario, req.motivo());
        var conferencia = new ConferenciaDocumentalTransferencia(intencao(contrato, req, p),
                normalizar(req.autorizacaoAdministrativa()), consumo.uso());
        EventoCicloVidaContrato evento = motorTemporal.registrarEvento(contrato,
                TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL, acao, usuario,
                resumoGestao(origem.getId(), p.nomeOrigem()), resumoGestao(destino), null, conferencia);

        TransferenciaGestaoContrato transferencia = new TransferenciaGestaoContrato();
        transferencia.setContrato(contrato); transferencia.setEvento(evento); transferencia.setGestaoOrigem(origem);
        transferencia.setGestaoDestino(destino); transferencia.setDataEfetiva(req.dataEfetiva()); transferencia.setMotivo(req.motivo());
        transferencia.setGestaoOrigemNomeFotografia(p.nomeOrigem());
        transferencia.setGestaoDestinoNomeFotografia(destino.getNome());
        transferencia.setTipoContratacaoDestino(tipoDestino);
        transferencia.setTipoDestinoDescricaoFotografia(tipoDestino.getDescricao());
        transferencia.setReferenciaSei(req.referenciaSei()); transferencia.setAutorizacaoAdministrativa(normalizar(req.autorizacaoAdministrativa()));
        transferencia.setResponsavelId(usuario == null ? null : usuario.getId());
        transferencia.setResponsavelUsername(usuario == null ? null : usuario.getUsername());
        transferencia.setMapeamentos(mapeamentos);
        transferencia = transferenciaRepositorio.save(transferencia);

        if (documentoComprobatorioServico != null) {
            documentoComprobatorioServico.publicarFontesDoFato(contrato, TipoFatoDocumental.TRANSFERENCIA_GESTAO,
                    TipoFatoDocumental.TRANSFERENCIA_GESTAO.name(), transferencia.getId(), req.referenciaSei(),
                    req.dataEfetiva(), usuario);
        }

        motorTemporal.registrarFotografiaProjetada(contrato, evento, req.dataEfetiva(),
                fotografiaComGestaoDestino(contrato, destino, tipoDestino, req.dataEfetiva(), evento.getId()));
        // O replay inclui a política fotografada deste ato e os deltas posteriores.
        // Conflito desfaz foto, evento, transferência e uso da concessão na mesma transação.
        LocalDate desde = retificada == null || req.dataEfetiva().isBefore(retificada.getDataEfetiva())
                ? req.dataEfetiva() : retificada.getDataEfetiva();
        validarContextoNasPosicoesPosteriores(contrato, desde);
        projecaoContratual.exigirMatriculasCompativeisAposTransferencia(contrato, desde);
        // As alocações só são movimentadas depois da validação, sem novo vínculo ou desligamento.
        if (retificada == null) alocacaoServico.aplicarTransferenciaGestao(transferencia, usuario);
        else alocacaoServico.reprocessarTransferenciasGestao(contrato, desde, usuario, req.motivo());
        sincronizarResponsabilidadeAtual(contrato, desde);
        return transferencia;
    }

    private Preparacao preparar(Contrato contrato, TransferenciaGestaoContratoRequisicao req) {
        validarRequisicao(contrato, req);
        TransferenciaGestaoContrato retificada = obterTransferenciaRetificada(contrato, req);
        Set<Long> ignorados = retificada == null ? Set.of() : Set.of(retificada.getEvento().getId());
        VersaoContratoNegocialDTO posicaoAnterior = projecaoContratual.projetarVersaoContrato(
                contrato, req.dataEfetiva(), Instant.now(), ignorados);
        validarVigenciaReconstruida(contrato, req.dataEfetiva(), posicaoAnterior);
        GestaoLocal origem = posicaoAnterior.gestaoLocalId() == null ? null
                : gestaoRepositorio.findById(posicaoAnterior.gestaoLocalId()).orElse(null);
        if (origem == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A Gestão responsável anterior à transferência não está comprovada.");
        GestaoLocal destino = gestaoRepositorio.findById(req.gestaoLocalDestinoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gestão Local de destino não encontrada."));
        validarDestino(destino);
        TipoContratacao tipoDestino = obterTipoDestino(req, destino);
        if (Objects.equals(origem.getId(), destino.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A Gestão Local de destino deve ser diferente da origem.");
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> contexto = composicaoTemporal
                .projetarEntidades(contrato, req.dataEfetiva(), Instant.now(), ignorados);
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> operacionais = contexto.stream()
                .filter(c -> c.efetivoPrevisto() > 0).toList();
        if (operacionais.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A transferência é bloqueada enquanto não existir composição operacional válida para o período posterior.");
        List<MapeamentoTransferenciaGestao> mapeamentos = prepararMapeamentos(contrato, origem, destino, req, contexto);
        validarCoberturaDoContexto(operacionais, mapeamentos);
        // Projeções atuais ainda contêm o mapa substituído; a retificação valida
        // as decisões independentes após reconstruir toda a cadeia na transação.
        if (retificada == null) validarCoberturaDasAlocacoesPosteriores(contrato, req.dataEfetiva(), mapeamentos);
        return new Preparacao(retificada, origem, nomeFotografado(posicaoAnterior.gestaoLocalNome()), destino, tipoDestino, mapeamentos,
                posicaoAnterior.dataInicio(), posicaoAnterior.dataFim());
    }

    private AcaoCicloVidaContratoDTO acao(TransferenciaGestaoContratoRequisicao req) {
        return new AcaoCicloVidaContratoDTO(req.dataEfetiva(), req.dataAto(), req.motivo(), req.referenciaSei(),
                req.eventoPrecedenteId(), req.relacaoPrecedencia(), req.ordemDocumental(), req.tokenPrevia(), req.confirmarImpacto());
    }

    private List<Object> partesDaIntencao(Contrato contrato, TransferenciaGestaoContratoRequisicao req, Preparacao p) {
        // Um digest sem delimitadores livres integra a prévia. Consentimento é exigido separadamente no definitivo.
        return List.of(IntencaoAtoTransferencia.VERSAO, intencao(contrato, req, p).hash());
    }

    private IntencaoAtoTransferencia.Material intencao(Contrato contrato,
            TransferenciaGestaoContratoRequisicao req, Preparacao p) {
        return IntencaoAtoTransferencia.capturar(campos("versao", IntencaoAtoTransferencia.VERSAO,
                "operacao", p.retificada() == null ? "TRANSFERIR_GESTAO" : "RETIFICAR_TRANSFERENCIA_GESTAO",
                "contratoId", contrato.getId(), "dataEfetiva", textoData(req.dataEfetiva()),
                "dataAto", textoData(req.dataAto()), "inicioVigencia", textoData(p.inicioVigencia()),
                "fimVigencia", textoData(p.fimVigencia()), "gestaoOrigemId", p.origem().getId(),
                "gestaoOrigemNome", p.nomeOrigem(), "gestaoDestinoId", p.destino().getId(),
                "gestaoDestinoNome", p.destino().getNome(), "gestaoDestinoVersao", p.destino().getVersion(),
                "tipoDestinoId", p.tipoDestino().getId(), "tipoDestinoVersao", p.tipoDestino().getVersion(),
                "tipoDestinoDescricao", p.tipoDestino().getDescricao(),
                "tipoDestinoMatriculaObrigatoria", p.tipoDestino().getMatriculaVinculoObrigatoria(),
                "eventoPrecedenteId", req.eventoPrecedenteId(), "relacaoPrecedencia", req.relacaoPrecedencia() == null ? null : req.relacaoPrecedencia().name(),
                "transferenciaAlvoId", p.retificada() == null ? null : p.retificada().getId(),
                "ordemDocumental", req.ordemDocumental(), "motivo", normalizar(req.motivo()),
                "referenciaSei", req.referenciaSei().trim(), "autorizacaoAdministrativaAto", normalizar(req.autorizacaoAdministrativa()),
                "versaoConferenciaDocumental", ConferenciaDocumentalTransferencia.VERSAO_DECLARACAO,
                "mapeamentos", mapasCanonicos(p.mapeamentos(), false)));
    }

    private List<Map<String, Object>> mapasCanonicos(List<MapeamentoTransferenciaGestao> mapas, boolean fotografados) {
        return mapas.stream().sorted(Comparator.comparing(m -> chave(m.getLotacaoOrigem(), m.getJornadaOrigem(), m.getCargoOrigem())))
                .map(m -> campos("composicaoOrigemId", m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                        "lotacaoOrigemId", m.getLotacaoOrigem().getId(), "jornadaOrigemId", m.getJornadaOrigem().getId(),
                        "cargoOrigemId", m.getCargoOrigem().getId(), "lotacaoOrigemNome", m.getLotacaoOrigemNomeFotografia(),
                        "jornadaOrigemDescricao", m.getJornadaOrigemDescricaoFotografia(), "cargoOrigemNome", m.getCargoOrigemNomeFotografia(),
                        "lotacaoDestinoId", m.getLotacaoDestino().getId(), "jornadaDestinoId", m.getJornadaDestino().getId(),
                        "cargoDestinoId", m.getCargoDestino().getId(),
                        "lotacaoDestinoNome", fotografados ? m.getLotacaoDestinoNomeFotografia() : m.getLotacaoDestino().getNome(),
                        "jornadaDestinoDescricao", fotografados ? m.getJornadaDestinoDescricaoFotografia() : m.getJornadaDestino().getDescricao(),
                        "cargoDestinoNome", fotografados ? m.getCargoDestinoNomeFotografia() : m.getCargoDestino().getNome(),
                        "lotacaoDestinoMunicipio", fotografados ? m.getLotacaoDestinoMunicipioFotografia() : m.getLotacaoDestino().getMunicipio(),
                        "jornadaDestinoSemanal", fotografados ? m.getJornadaDestinoSemanalFotografia() : FotografiaJornada.capturar(m.getJornadaDestino()),
                        "lotacaoDestinoVersao", fotografados ? null : m.getLotacaoDestino().getVersion(),
                        "jornadaDestinoVersao", fotografados ? null : m.getJornadaDestino().getVersion(),
                        "cargoDestinoVersao", fotografados ? null : m.getCargoDestino().getVersion())).toList();
    }

    private Map<String, Object> campos(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) resultado.put((String) pares[i], pares[i + 1]);
        return resultado;
    }
    private String textoData(LocalDate data) { return data == null ? null : data.toString(); }

    private void exigirConferencia(Boolean confirmada, String versao) {
        if (!Boolean.TRUE.equals(confirmada)
                || !ConferenciaDocumentalTransferencia.VERSAO_DECLARACAO.equals(versao))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Confirme a conferência documental específica deste ato com a declaração vigente.");
    }

    @Transactional
    public TransferenciaGestaoContrato cancelarProgramada(Contrato contrato, Long transferenciaId,
            CancelamentoTransferenciaGestaoRequisicao req, UsuarioAplicacao usuario) {
        TransferenciaGestaoContrato original = transferenciaRepositorio.findById(transferenciaId)
                .filter(t -> Objects.equals(t.getContrato().getId(), contrato.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transferência não encontrada no contrato."));
        if (original.getEvento() == null || original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente transferência vigente pode ser cancelada.");
        if (!original.getDataEfetiva().isAfter(RelogioSistema.hoje()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente transferência futura pode ser cancelada antes de produzir efeito.");
        if (req == null || vazio(req.motivo()) || vazio(req.referenciaSei()) || vazio(req.autorizacaoAdministrativa()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cancelamento exige motivo, referência SEI e autorização administrativa específica.");
        if (req.dataEfetiva() != null && !req.dataEfetiva().equals(original.getDataEfetiva()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O cancelamento retira o efeito da transferência original na sua data programada.");
        exigirConferencia(req.conferenciaDocumentalConfirmada(), req.versaoConferenciaDocumental());
        var consumo = exigirAutorizacao(contrato, usuario, req.motivo());
        AcaoCicloVidaContratoDTO acao = new AcaoCicloVidaContratoDTO(original.getDataEfetiva(),
                req.dataAto() == null ? RelogioSistema.hoje() : req.dataAto(), req.motivo(), req.referenciaSei(),
                original.getEvento().getId(), TipoRelacaoPrecedenciaNegocial.CANCELA, null);
        var contextoReposto = projecaoContratual.projetarVersaoContrato(contrato,
                original.getDataEfetiva(), Instant.now(), Set.of(original.getEvento().getId()));
        var intencao = IntencaoAtoTransferencia.capturar(campos("versao", IntencaoAtoTransferencia.VERSAO,
                "operacao", "CANCELAR_TRANSFERENCIA_GESTAO", "contratoId", contrato.getId(),
                "inicioVigencia", textoData(contextoReposto.dataInicio()), "fimVigencia", textoData(contextoReposto.dataFim()),
                "tipoDestinoId", contextoReposto.tipoContratacaoId(), "tipoDestinoDescricao", contextoReposto.tipoContratacaoDescricao(),
                "tipoDestinoMatriculaObrigatoria", contextoReposto.matriculaTipoContratacaoObrigatoria(),
                "dataEfetiva", textoData(acao.dataEfetiva()), "dataAto", textoData(acao.dataAto()),
                "gestaoOrigemId", original.getGestaoDestino().getId(),
                "gestaoOrigemNome", original.getGestaoDestinoNomeFotografia(),
                "gestaoDestinoId", original.getGestaoOrigem().getId(),
                "gestaoDestinoNome", original.getGestaoOrigemNomeFotografia(),
                "tipoTransferenciaOriginalId", original.getTipoContratacaoDestino() == null ? null : original.getTipoContratacaoDestino().getId(),
                "tipoTransferenciaOriginalDescricao", original.getTipoDestinoDescricaoFotografia(),
                "transferenciaAlvoId", original.getId(), "eventoPrecedenteId", original.getEvento().getId(),
                "relacaoPrecedencia", TipoRelacaoPrecedenciaNegocial.CANCELA.name(),
                "motivo", normalizar(req.motivo()), "referenciaSei", req.referenciaSei().trim(),
                "autorizacaoAdministrativaAto", normalizar(req.autorizacaoAdministrativa()),
                "versaoConferenciaDocumental", ConferenciaDocumentalTransferencia.VERSAO_DECLARACAO,
                "hashIntencaoOriginal", original.getEvento().getConferenciaDocumental() == null ? null
                        : original.getEvento().getConferenciaDocumental().getHashIntencao(),
                "mapeamentosDaTransferenciaCancelada", mapasCanonicos(original.getMapeamentos(), true)));
        var conferencia = new ConferenciaDocumentalTransferencia(intencao,
                normalizar(req.autorizacaoAdministrativa()), consumo.uso());
        motorTemporal.registrarEvento(contrato, TipoEventoCicloVidaContrato.CANCELAMENTO_TRANSFERENCIA_GESTAO,
                acao, usuario, resumoGestao(original.getGestaoDestino().getId(), original.getGestaoDestinoNomeFotografia()),
                resumoGestao(original.getGestaoOrigem().getId(), original.getGestaoOrigemNomeFotografia()),
                "CANCELAMENTO_TRANSFERENCIA_GESTAO:" + original.getId(), conferencia);
        validarContextoNasPosicoesPosteriores(contrato, original.getDataEfetiva());
        projecaoContratual.exigirMatriculasCompativeisAposTransferencia(contrato, original.getDataEfetiva());
        alocacaoServico.reprocessarTransferenciasGestao(contrato, original.getDataEfetiva(), usuario, req.motivo());
        if (documentoComprobatorioServico != null)
            documentoComprobatorioServico.publicarFontesDoFato(contrato, TipoFatoDocumental.TRANSFERENCIA_GESTAO,
                    "CANCELAMENTO_TRANSFERENCIA_GESTAO:" + original.getId(), original.getId(), req.referenciaSei(),
                    original.getDataEfetiva(), usuario);
        sincronizarResponsabilidadeAtual(contrato, original.getDataEfetiva());
        return original;
    }

    private PermissaoExcepcionalServico.ConsumoPermissao exigirAutorizacao(
            Contrato contrato, UsuarioAplicacao usuario, String motivo) {
        // Capacidade vigente HOJE, separada da data efetiva e da referência declarada para o ato.
        return permissaoExcepcionalServico.exigirERegistrarUsoComRecibo(usuario,
                TipoPermissaoExcepcional.TRANSFERENCIA_GESTAO_LOCAL, contrato,
                gestaoContratoTemporal.gestaoAtual(contrato), motivo, "Contrato", contrato.getId());
    }

    private TransferenciaGestaoContrato obterTransferenciaRetificada(Contrato contrato, TransferenciaGestaoContratoRequisicao req) {
        if (req.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.RETIFICA
                && req.relacaoPrecedencia() != TipoRelacaoPrecedenciaNegocial.SUBSTITUI) return null;
        TransferenciaGestaoContrato original = transferenciaRepositorio.findByEventoId(req.eventoPrecedenteId())
                .filter(t -> Objects.equals(t.getContrato().getId(), contrato.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Retificação deve referenciar uma transferência deste contrato."));
        if (original.getEvento().getSituacaoFato() != SituacaoFatoNegocial.VIGENTE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente a versão vigente da transferência pode ser retificada.");
        return original;
    }

    private void validarVigenciaReconstruida(Contrato contrato, LocalDate data, VersaoContratoNegocialDTO versao) {
        LocalDate fim = versao.dataFim();
        for (EventoCicloVidaContrato evento : motorTemporal.eventosValidosOrdenados(contrato.getId(), data, Instant.now()))
            if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.ENCERRAMENTO_ANTECIPADO) fim = evento.getDataEfetiva();
        if (versao.dataInicio() == null || fim == null || data.isBefore(versao.dataInicio()) || data.isAfter(fim))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data da transferência deve estar contida na vigência reconstruída do contrato.");
    }

    private void sincronizarResponsabilidadeAtual(Contrato contrato, LocalDate desde) {
        if (desde.isAfter(RelogioSistema.hoje())) return;
        VersaoContratoNegocialDTO atual = projecaoContratual.projetarVersaoContrato(
                contrato, RelogioSistema.hoje(), Instant.now(), Set.of());
        if (atual.gestaoLocalId() != null) gestaoRepositorio.findById(atual.gestaoLocalId()).ifPresent(contrato::setGestaoLocal);
        if (atual.tipoContratacaoId() != null) tipoContratacaoRepositorio.findById(atual.tipoContratacaoId()).ifPresent(tipo -> {
            contrato.setTipoContratacao(tipo); contrato.setTipo(atual.tipoContratacaoDescricao());
        });
        contrato.setAtivo(atual.habilitacaoAdministrativa() == HabilitacaoAdministrativaContrato.ATIVO);
        if (projecaoPessoas != null) projecaoPessoas.sincronizarAposTransferencia(contrato);
    }

    private void validarContextoNasPosicoesPosteriores(Contrato contrato, LocalDate desde) {
        Instant conhecido = Instant.now();
        List<ComposicaoContratualTemporalServico.ConflitoReplay> conflitos = composicaoTemporal
                .diagnosticarConflitos(contrato, LocalDate.MAX, conhecido).stream()
                .filter(c -> c.dataEfetiva() == null || !c.dataEfetiva().isBefore(desde)).toList();
        if (!conflitos.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A transferência exige tratamento dos fatos posteriores incompatíveis: " + conflitos);
        SortedSet<LocalDate> datas = motorTemporal.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecido).stream()
                .map(EventoCicloVidaContrato::getDataEfetiva).filter(Objects::nonNull)
                .filter(data -> !data.isBefore(desde)).collect(Collectors.toCollection(TreeSet::new));
        datas.add(desde);
        for (LocalDate data : datas) {
            VersaoContratoNegocialDTO posicao = projecaoContratual.projetarVersaoContrato(contrato, data, conhecido, Set.of());
            TipoContratacao tipo = posicao.tipoContratacaoId() == null ? null
                    : tipoContratacaoRepositorio.findById(posicao.tipoContratacaoId()).orElse(null);
            if (tipo == null || tipo.getGestaoLocal() == null || posicao.gestaoLocalId() == null
                    || !Objects.equals(tipo.getGestaoLocal().getId(), posicao.gestaoLocalId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tipo de Contratação não comprovado ou incompatível com a Gestão responsável em " + data
                                + ". Trate os atos posteriores antes de confirmar a transferência.");
            }
            for (var composicao : composicaoTemporal.projetarEntidades(contrato, data, conhecido)) {
                if (composicao.efetivoPrevisto() <= 0) continue;
                if (composicao.lotacao() == null || composicao.jornada() == null || composicao.cargo() == null
                        || composicao.lotacao().getGestaoLocal() == null || composicao.jornada().getGestaoLocal() == null
                        || composicao.cargo().getGestaoLocal() == null
                        || !Objects.equals(composicao.lotacao().getGestaoLocal().getId(), posicao.gestaoLocalId())
                        || !Objects.equals(composicao.jornada().getGestaoLocal().getId(), posicao.gestaoLocalId())
                        || !Objects.equals(composicao.cargo().getGestaoLocal().getId(), posicao.gestaoLocalId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Composição " + composicao.chave() + " incompatível com a Gestão responsável em " + data
                                    + ". Trate os atos posteriores antes de confirmar a transferência.");
            }
        }
    }

    private VersaoContratoNegocialDTO fotografiaComGestaoDestino(Contrato contrato, GestaoLocal destino,
                                                                  TipoContratacao tipoDestino, LocalDate dataEfetiva,
                                                                  Long eventoTransferenciaId) {
        // A base de herança é a posição anterior a este ato. Sua própria fotografia
        // ainda não existe, e não deve transformar uma herança conhecida em desconhecida.
        VersaoContratoNegocialDTO base = projecaoContratual != null
                ? projecaoContratual.projetarVersaoContrato(contrato, dataEfetiva, Instant.now(), Set.of(eventoTransferenciaId))
                : motorTemporal.versaoAplicavel(contrato, dataEfetiva, Instant.now())
                    .map(motorTemporal::paraVersaoDTO).orElse(null);
        Boolean override = base != null ? base.matriculaVinculoObrigatoria() : null;
        boolean exigenciaTipo = Boolean.TRUE.equals(tipoDestino.getMatriculaVinculoObrigatoria());
        Boolean exigenciaEfetiva = override != null ? override
                : base != null && base.matriculaVinculoObrigatoriaEfetiva() != null ? Boolean.valueOf(exigenciaTipo) : null;
        return new VersaoContratoNegocialDTO(null, null, null, dataEfetiva, null,
                base != null ? base.numeroContrato() : contrato.getNumeroContrato(),
                base != null ? base.anoContrato() : contrato.getAnoContrato(),
                base != null ? base.descricao() : contrato.getDescricao(),
                tipoDestino.getId(), tipoDestino.getDescricao(),
                base != null ? base.fornecedorId() : contrato.getFornecedor() == null ? null : contrato.getFornecedor().getId(),
                base != null ? base.cnpjEmpresa() : contrato.getCnpjEmpresa(),
                base != null ? base.razaoSocialEmpresa() : contrato.getRazaoSocialEmpresa(),
                base != null ? base.nomeFantasiaEmpresa() : contrato.getNomeFantasiaEmpresa(),
                base != null ? base.dataInicio() : contrato.getDataInicio(),
                base != null ? base.dataFim() : contrato.getDataFim(),
                base != null ? base.efetivoPrevisto() : contrato.getEfetivoPrevisto(), destino.getId(), destino.getNome(),
                override, exigenciaTipo, exigenciaEfetiva,
                base != null ? base.diaSemanaReduzido() : null,
                base != null ? base.horasDiaReduzido() : null,
                motorTemporal.habilitacaoAdministrativaEm(contrato, dataEfetiva, Instant.now()));
    }

    private TipoContratacao obterTipoDestino(TransferenciaGestaoContratoRequisicao req, GestaoLocal destino) {
        if (req.tipoContratacaoDestinoId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A transferência exige o Tipo de Contratação da Gestão Local de destino.");
        TipoContratacao tipo = tipoContratacaoRepositorio.findById(req.tipoContratacaoDestinoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação de destino não encontrado."));
        if (tipo.getGestaoLocal() == null || !Objects.equals(tipo.getGestaoLocal().getId(), destino.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de Contratação não pertence à Gestão Local de destino.");
        }
        if (!catalogoAtivoEm(TipoCatalogoTemporal.TIPO_CONTRATACAO, tipo.getId(), req.dataEfetiva(), tipo.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tipo de Contratação de destino inativo na data efetiva.");
        }
        return tipo;
    }

    public List<TransferenciaGestaoContratoDTO> listar(Long contratoId) {
        return transferenciaRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contratoId).stream()
                .map(this::paraDTO).toList();
    }

    public TransferenciaGestaoContratoDTO paraDTO(TransferenciaGestaoContrato t) {
        return new TransferenciaGestaoContratoDTO(t.getId(), t.getContrato().getId(),
                t.getEvento() == null ? null : t.getEvento().getId(), t.getGestaoOrigem().getId(),
                nomeFotografado(t.getGestaoOrigemNomeFotografia()),
                t.getGestaoDestino().getId(), nomeFotografado(t.getGestaoDestinoNomeFotografia()), t.getDataEfetiva(), t.getMotivo(),
                t.getReferenciaSei(), t.getAutorizacaoAdministrativa(), t.getRegistradoEm(), t.getResponsavelId(),
                t.getResponsavelUsername(), t.getMapeamentos().stream().map(this::paraDTO).toList(),
                t.getTipoContratacaoDestino() == null ? null : t.getTipoContratacaoDestino().getId(),
                nomeFotografado(t.getTipoDestinoDescricaoFotografia()), MotorTemporalContratoServico.paraConferenciaDocumentalDTO(t.getEvento()));
    }

    private List<MapeamentoTransferenciaGestao> prepararMapeamentos(Contrato contrato, GestaoLocal origem, GestaoLocal destino,
                                                                     TransferenciaGestaoContratoRequisicao req,
                                                                     List<ComposicaoContratualTemporalServico.ComposicaoProjetada> contexto) {
        if (req.mapeamentos() == null || req.mapeamentos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A transferência exige mapeamento explícito para os catálogos da Gestão de destino.");
        }
        Map<String, ComposicaoContratualTemporalServico.ComposicaoProjetada> porOrigem = contexto.stream()
                .collect(Collectors.toMap(ComposicaoContratualTemporalServico.ComposicaoProjetada::chave,
                        p -> p, (a, b) -> a, LinkedHashMap::new));
        Set<String> usados = new HashSet<>();
        List<MapeamentoTransferenciaGestao> resultado = new ArrayList<>();
        for (MapeamentoTransferenciaGestaoRequisicao item : req.mapeamentos()) {
            if (item == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mapeamento de transferência inválido.");
            ComposicaoContratual origemComposicao = resolverComposicaoOrigem(contrato, item.composicaoOrigemId());
            // O ID é estável, mas seu recorte pode já ter sido mapeado por uma
            // transferência anterior. A fotografia física original não é o contexto corrente.
            var recorteOrigem = composicaoTemporal.localizar(contexto, item.composicaoOrigemId(),
                    item.lotacaoOrigemId(), item.jornadaOrigemId(), item.cargoOrigemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "O recorte de origem não pertence à composição temporal válida do contrato."));
            Lotacao lotacaoOrigem = recorteOrigem.lotacao();
            Jornada jornadaOrigem = recorteOrigem.jornada();
            Cargo cargoOrigem = recorteOrigem.cargo();
            String chave = chave(lotacaoOrigem, jornadaOrigem, cargoOrigem);
            if (!porOrigem.containsKey(chave)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O recorte de origem não pertence à composição temporal válida do contrato.");
            }
            if (!usados.add(chave)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A mesma composição de origem foi mapeada mais de uma vez.");
            validarGestaoDoRecorte(lotacaoOrigem, jornadaOrigem, cargoOrigem, origem, "origem");

            Lotacao lotacaoDestino = obterLotacao(item.lotacaoDestinoId(), "destino");
            Jornada jornadaDestino = obterJornada(item.jornadaDestinoId(), "destino");
            Cargo cargoDestino = obterCargo(item.cargoDestinoId(), "destino");
            if (disponibilidadeCatalogo != null) {
                disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, lotacaoDestino.getId());
                disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, jornadaDestino.getId());
                disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, cargoDestino.getId());
            }
            validarGestaoDoRecorte(lotacaoDestino, jornadaDestino, cargoDestino, destino, "destino");
            boolean turnoDaLotacao = lotacaoDestino.getTurnos().stream().anyMatch(j -> Objects.equals(j.getId(), jornadaDestino.getId()));
            if (!turnoDaLotacao) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O Turno de destino não pertence à Lotação de destino.");
            if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, lotacaoDestino.getId(), req.dataEfetiva(), lotacaoDestino.getAtivo())
                    || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, jornadaDestino.getId(), req.dataEfetiva(), jornadaDestino.getAtivo())
                    || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, cargoDestino.getId(), req.dataEfetiva(), cargoDestino.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Catálogo inativo não pode ser mapeado para transferência atual ou futura.");
            }
            MapeamentoTransferenciaGestao mapa = new MapeamentoTransferenciaGestao();
            mapa.setComposicaoOrigem(origemComposicao); mapa.setLotacaoOrigem(lotacaoOrigem); mapa.setJornadaOrigem(jornadaOrigem);
            mapa.setCargoOrigem(cargoOrigem); mapa.setLotacaoDestino(lotacaoDestino); mapa.setJornadaDestino(jornadaDestino);
            var fotografiaOrigem = porOrigem.get(chave);
            mapa.setLotacaoOrigemNomeFotografia(fotografiaOrigem.lotacaoNome());
            mapa.setJornadaOrigemDescricaoFotografia(fotografiaOrigem.jornadaDescricao());
            mapa.setCargoOrigemNomeFotografia(fotografiaOrigem.cargoNome());
            mapa.setLotacaoDestinoNomeFotografia(lotacaoDestino.getNome());
            mapa.setJornadaDestinoDescricaoFotografia(jornadaDestino.getDescricao());
            mapa.setCargoDestinoNomeFotografia(cargoDestino.getNome());
            mapa.setCargoDestino(cargoDestino); resultado.add(mapa);
        }
        return resultado;
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : Boolean.TRUE.equals(atual);
    }

    private void validarCoberturaDoContexto(List<ComposicaoContratualTemporalServico.ComposicaoProjetada> operacionais,
                                             List<MapeamentoTransferenciaGestao> mapeamentos) {
        Set<String> fontes = mapeamentos.stream().map(m -> chave(m.getLotacaoOrigem(), m.getJornadaOrigem(), m.getCargoOrigem()))
                .collect(Collectors.toSet());
        List<String> faltantes = operacionais.stream().map(ComposicaoContratualTemporalServico.ComposicaoProjetada::chave)
                .filter(chave -> !fontes.contains(chave)).toList();
        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A transferência não possui mapeamento para todas as composições operacionais: " + faltantes);
        }
    }

    private void validarCoberturaDasAlocacoesPosteriores(Contrato contrato, LocalDate data,
                                                          List<MapeamentoTransferenciaGestao> mapeamentos) {
        Set<String> fontes = mapeamentos.stream().map(m -> chave(m.getLotacaoOrigem(), m.getJornadaOrigem(), m.getCargoOrigem()))
                .collect(Collectors.toSet());
        List<String> faltantes = alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contrato.getId()).stream()
                .filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                .filter(a -> a.getDataFim() == null || !a.getDataFim().isBefore(data))
                .map(a -> chave(a.getLotacao(), a.getJornada(), a.getCargo())).filter(chave -> !fontes.contains(chave))
                .distinct().toList();
        if (!faltantes.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Existem Alocações posteriores sem mapeamento de destino: " + faltantes);
    }

    private void validarRequisicao(Contrato contrato, TransferenciaGestaoContratoRequisicao req) {
        if (contrato == null || contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transferência exige contrato publicado válido.");
        }
        if (req == null || req.dataEfetiva() == null || req.gestaoLocalDestinoId() == null || vazio(req.motivo())
                || vazio(req.referenciaSei()) || vazio(req.autorizacaoAdministrativa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data efetiva, Gestão destino, motivo, referência SEI e autorização administrativa são obrigatórios.");
        }
        if (req.mapeamentos() == null || req.mapeamentos().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A transferência exige mapeamento explícito para os catálogos da Gestão de destino.");
        if ((req.eventoPrecedenteId() == null) != (req.relacaoPrecedencia() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "eventoPrecedenteId e relacaoPrecedencia devem ser informados juntos.");
        }
        if (req.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.CANCELA)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use o cancelamento próprio da transferência futura; cancelar não cria uma nova transferência.");
    }

    private void validarDestino(GestaoLocal destino) {
        if (destino == null || Boolean.FALSE.equals(destino.getAtivo()) || destino.getSuperintendencia() == null
                || Boolean.FALSE.equals(destino.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A Gestão Local de destino e sua Superintendência precisam estar ativas.");
        }
    }

    private ComposicaoContratual resolverComposicaoOrigem(Contrato contrato, Long id) {
        if (id == null) return null;
        ComposicaoContratual composicao = composicaoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição de origem não encontrada."));
        if (!Objects.equals(composicao.getContrato().getId(), contrato.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Composição de origem pertence a outro contrato.");
        }
        return composicao;
    }

    private Lotacao obterLotacao(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lotação de " + lado + " é obrigatória.");
        return lotacaoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Lotação de " + lado + " não encontrada."));
    }
    private Jornada obterJornada(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno de " + lado + " é obrigatório.");
        return jornadaRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Turno de " + lado + " não encontrado."));
    }
    private Cargo obterCargo(Long id, String lado) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo de " + lado + " é obrigatório.");
        return cargoRepositorio.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cargo de " + lado + " não encontrado."));
    }

    private void validarGestaoDoRecorte(Lotacao lotacao, Jornada jornada, Cargo cargo, GestaoLocal gestao, String lado) {
        if (!Objects.equals(lotacao.getGestaoLocal().getId(), gestao.getId())
                || !Objects.equals(jornada.getGestaoLocal().getId(), gestao.getId())
                || !Objects.equals(cargo.getGestaoLocal().getId(), gestao.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo de " + lado + " devem pertencer à Gestão Local correspondente.");
        }
    }

    private MapeamentoTransferenciaGestaoDTO paraDTO(MapeamentoTransferenciaGestao m) {
        return new MapeamentoTransferenciaGestaoDTO(m.getId(), m.getComposicaoOrigem() == null ? null : m.getComposicaoOrigem().getId(),
                m.getLotacaoOrigem().getId(), nomeFotografado(m.getLotacaoOrigemNomeFotografia()),
                m.getJornadaOrigem().getId(), nomeFotografado(m.getJornadaOrigemDescricaoFotografia()),
                m.getCargoOrigem().getId(), nomeFotografado(m.getCargoOrigemNomeFotografia()),
                m.getLotacaoDestino().getId(), nomeFotografado(m.getLotacaoDestinoNomeFotografia()),
                m.getJornadaDestino().getId(), nomeFotografado(m.getJornadaDestinoDescricaoFotografia()),
                m.getCargoDestino().getId(), nomeFotografado(m.getCargoDestinoNomeFotografia()));
    }

    private String nomeFotografado(String fotografia) {
        // Ausência de foto legada não autoriza apresentar o nome mestre atual como passado.
        return fotografia == null || fotografia.isBlank() ? null : fotografia.trim();
    }

    private String chave(Lotacao l, Jornada j, Cargo c) { return l.getId() + ":" + j.getId() + ":" + c.getId(); }
    private String resumoGestao(GestaoLocal g) { return resumoGestao(g.getId(), g.getNome()); }
    private String resumoGestao(Long id, String nome) { return id + ":" + (nome == null ? "" : nome); }
    private boolean vazio(String texto) { return texto == null || texto.isBlank(); }
    private String normalizar(String texto) { return texto.trim().replaceAll("\\s+", " "); }
}
