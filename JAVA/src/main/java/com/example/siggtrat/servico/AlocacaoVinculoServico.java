package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.regra.RegraDisponibilidadeCatalogo;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mantém a linha do tempo completa de Lotação + Turno + Cargo. O serviço
 * preserva as faixas anteriores, registra o evento correspondente e impede
 * lacunas/sobreposições dentro do Vínculo.
 */
@Service
@Transactional(readOnly = true)
public class AlocacaoVinculoServico {
    public record CapacidadeInclusao(int efetivoPrevisto, long ocupacaoAtual,
                                     long ocupacaoAposInclusao, long excedenteAposInclusao,
                                     LocalDate primeiroDiaExcedente, LocalDate fimAvaliado, String assinaturaFaixa) {
        public CapacidadeInclusao(int efetivoPrevisto, long ocupacaoAtual,
                                   long ocupacaoAposInclusao, long excedenteAposInclusao) {
            this(efetivoPrevisto, ocupacaoAtual, ocupacaoAposInclusao, excedenteAposInclusao, null, null, null);
        }
        public boolean excede() { return excedenteAposInclusao > 0; }
        public String detalheTemporal(LocalDate dataEfetiva) {
            return primeiroDiaExcedente != null && primeiroDiaExcedente.isAfter(dataEfetiva)
                    ? " [EXCEDENTE_DESDE: " + primeiroDiaExcedente + "; FAIXA_AVALIADA_ATE: " + fimAvaliado + "]" : "";
        }
    }
    private record FotoAlocacao(Long id, Long composicaoId, Long lotacaoId, String lotacaoNome,
                                Long jornadaId, String jornadaDescricao, Long cargoId, String cargoNome,
                                LocalDate inicio, LocalDate fim, EstadoNegocialAlocacao estado) {}

    private record FaixaPlanejada(AlocacaoVinculo alocacao, LocalDate inicio, LocalDate fim,
                                  EstadoNegocialAlocacao estado, TransferenciaGestaoContrato transferencia,
                                  AlocacaoVinculo anterior, boolean reconstituida) {
        private FaixaPlanejada(AlocacaoVinculo alocacao, LocalDate inicio, LocalDate fim,
                              EstadoNegocialAlocacao estado, TransferenciaGestaoContrato transferencia,
                              AlocacaoVinculo anterior) {
            this(alocacao, inicio, fim, estado, transferencia, anterior, false);
        }
        private FaixaPlanejada(AlocacaoVinculo alocacao, LocalDate inicio, LocalDate fim, EstadoNegocialAlocacao estado) {
            this(alocacao, inicio, fim, estado, null, null, false);
        }
    }

    /** Seleção validada na data efetiva; a fotografia-base pode não existir em um aditamento. */
    public record CombinacaoResolvida(ComposicaoContratual composicao,
                                      Lotacao lotacao, Jornada jornada, Cargo cargo, int efetivo) {}

    /** Diagnóstico da parte adicional, no início ou fim, sem alterar os limites humanos. */
    public record AmpliacaoFim(CombinacaoResolvida combinacao, CapacidadeInclusao capacidade,
                               LocalDate inicio, LocalDate fim, List<AmpliacaoFim> trechos) {
        public AmpliacaoFim(CombinacaoResolvida combinacao, CapacidadeInclusao capacidade, LocalDate inicio, LocalDate fim) {
            this(combinacao, capacidade, inicio, fim, List.of());
        }
        public AmpliacaoFim {
            trechos = trechos == null ? List.of() : List.copyOf(trechos);
        }
        public String assinatura() {
            return AssinaturaPreviaNegocial.sha256(List.of(
                    String.valueOf(combinacao.lotacao().getId()), String.valueOf(combinacao.jornada().getId()),
                    String.valueOf(combinacao.cargo().getId()), String.valueOf(inicio), String.valueOf(fim),
                    AssinaturaPreviaNegocial.valor(capacidade.assinaturaFaixa())));
        }
    }

    private final AlocacaoVinculoRepositorio alocacaoRepositorio;
    private final EventoAlocacaoVinculoRepositorio eventoRepositorio;
    private final VinculoContratualRepositorio vinculoRepositorio;
    private final ContratoRepositorio contratoRepositorio;
    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final ComposicaoContratualTemporalServico composicaoTemporal;
    private final AcessoOrganizacionalServico acesso;
    private final MotorTemporalAlocacaoServico motor;
    private RevalidacaoFrequenciaTemporalServico revalidacaoFrequenciaTemporalServico;
    private DisponibilidadeCatalogoTemporalServico disponibilidadeCatalogo;
    private MotorTemporalContratoServico motorContrato;
    private AssinaturaImpactoVinculoServico assinaturaImpacto;
    private EscopoLeituraPessoaVinculoServico escopoLeitura;
    private EventoVinculoContratualRepositorio eventosVinculo;
    private TransferenciaGestaoContratoRepositorio transferenciasGestao;
    private ConsultaProvaHistoricaContratoServico provasHistoricas;
    private MinimizadorDadosPessoaisServico minimizador = new MinimizadorDadosPessoaisServico();
    @PersistenceContext private EntityManager entityManager;

    public AlocacaoVinculoServico(AlocacaoVinculoRepositorio alocacaoRepositorio,
                                  EventoAlocacaoVinculoRepositorio eventoRepositorio,
                                  VinculoContratualRepositorio vinculoRepositorio,
                                  ContratoRepositorio contratoRepositorio,
                                  ComposicaoContratualRepositorio composicaoRepositorio,
                                  ComposicaoContratualTemporalServico composicaoTemporal,
                                  AcessoOrganizacionalServico acesso,
                                  MotorTemporalAlocacaoServico motor) {
        this.alocacaoRepositorio = alocacaoRepositorio;
        this.eventoRepositorio = eventoRepositorio;
        this.vinculoRepositorio = vinculoRepositorio;
        this.contratoRepositorio = contratoRepositorio;
        this.composicaoRepositorio = composicaoRepositorio;
        this.composicaoTemporal = composicaoTemporal;
        this.acesso = acesso;
        this.motor = motor;
    }

    @Autowired(required = false)
    public void configurarRevalidacaoFrequencia(RevalidacaoFrequenciaTemporalServico servico) {
        this.revalidacaoFrequenciaTemporalServico = servico;
    }

    @Autowired(required = false)
    public void configurarDisponibilidadeCatalogo(DisponibilidadeCatalogoTemporalServico servico) {
        this.disponibilidadeCatalogo = servico;
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
    public void configurarEscopoLeitura(EscopoLeituraPessoaVinculoServico servico) { this.escopoLeitura = servico; }

    @Autowired
    public void configurarMinimizacao(MinimizadorDadosPessoaisServico servico) { this.minimizador = servico; }

    @Autowired(required = false)
    public void configurarEventosVinculo(EventoVinculoContratualRepositorio repositorio) { this.eventosVinculo = repositorio; }

    @Autowired
    public void configurarTransferenciasGestao(TransferenciaGestaoContratoRepositorio repositorio) { this.transferenciasGestao = repositorio; }

    @Autowired
    public void configurarProvasHistoricas(ConsultaProvaHistoricaContratoServico servico) { this.provasHistoricas = servico; }

    public List<AlocacaoVinculoDTO> listar(Long vinculoId, YearMonth mes) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        exigirLeitura(usuario, vinculo, null, null);
        return alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculoId).stream()
                .filter(a -> sobrepoeMes(a, mes)).filter(a -> escopoLeitura == null || escopoLeitura.permiteAlocacao(usuario, a)).map(this::paraDTO).toList();
    }

    public List<EventoAlocacaoVinculoDTO> listarEventos(Long vinculoId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        exigirLeitura(usuario, vinculo, null, null);
        return eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId).stream()
                .filter(e -> escopoLeitura == null || escopoLeitura.permiteEvento(usuario, e)).map(e -> paraDTO(e, usuario)).toList();
    }

    public PosicaoAlocacaoTemporalDTO posicao(Long vinculoId, LocalDate data, Instant conhecidoAte) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        exigirLeitura(usuario, vinculo, null, null);
        PosicaoAlocacaoTemporalDTO posicao = motor.reconstruir(vinculo, data, conhecidoAte);
        if (escopoLeitura != null && !escopoLeitura.permitePosicao(usuario, posicao))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocação não encontrada no seu escopo na data informada.");
        return posicao;
    }

    private void exigirLeitura(UsuarioAplicacao usuario, VinculoContratual vinculo, LocalDate data, Instant conhecidoAte) {
        if (escopoLeitura != null && acesso.ehOperacional(usuario)) escopoLeitura.exigirVinculo(usuario, vinculo, data, conhecidoAte);
        else acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
    }

    /**
     * A inativação administrativa preserva as faixas. Antes da reativação, as
     * Alocações que voltariam a operar precisam continuar representadas por uma
     * composição temporal válida na data efetiva.
     */
    public void validarRetomadaOperacional(Contrato contrato, LocalDate dataEfetiva) {
        if (contrato == null || contrato.getId() == null || dataEfetiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato e data efetiva são obrigatórios para revalidar a Alocação.");
        }
        alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contrato.getId()).stream()
                .filter(a -> a.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                .filter(a -> a.getVinculo().getEstadoNegocial() == EstadoNegocialVinculo.REGISTRADO)
                .filter(a -> !dataEfetiva.isBefore(a.getDataInicio()))
                .filter(a -> a.getDataFim() == null || !dataEfetiva.isAfter(a.getDataFim()))
                .forEach(a -> validarCombinacaoDireta(a.getVinculo(), dataEfetiva, a));
    }

    /** Uma lista filtrável de combinações para a seleção encadeada no frontend. */
    public List<OpcaoAlocacaoTemporalDTO> opcoes(Long contratoId, LocalDate data, Long lotacaoId, Long jornadaId) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        Contrato contrato = contratoRepositorio.findById(contratoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(usuario, contrato);
        LocalDate referencia = data == null ? RelogioSistema.hoje() : data;
        return composicaoTemporal.projetarEntidades(contrato, referencia, Instant.now()).stream()
                .filter(p -> p.efetivoPrevisto() > 0)
                .filter(p -> escopoLeitura == null || escopoLeitura.permiteCombinacao(usuario, p.lotacao().getId(), p.jornada().getId()))
                .filter(p -> lotacaoId == null || Objects.equals(lotacaoId, p.lotacao().getId()))
                .filter(p -> jornadaId == null || Objects.equals(jornadaId, p.jornada().getId()))
                .map(p -> {
                    long ocupacao = alocacaoRepositorio.countOcupacao(contratoId, p.lotacao().getId(),
                            p.jornada().getId(), p.cargo().getId(), referencia);
                    long apos = ocupacao + 1;
                    return new OpcaoAlocacaoTemporalDTO(p.composicaoBaseId(), p.lotacao().getId(), p.lotacaoNome(),
                            p.jornada().getId(), p.jornadaDescricao(), p.cargo().getId(), p.cargoNome(),
                            p.efetivoPrevisto(), ocupacao, apos, Math.max(0L, apos - p.efetivoPrevisto()),
                            apos > p.efetivoPrevisto(), true);
                }).toList();
    }

    /** Capacidade e ocupação são reconstruídas na data efetiva, não na data do comando. */
    public CapacidadeInclusao capacidadeInclusao(Contrato contrato, ComposicaoContratual composicao,
                                                  LocalDate dataEfetiva) {
        if (contrato == null || composicao == null || dataEfetiva == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato, composição e data efetiva são obrigatórios para avaliar capacidade.");
        }
        CombinacaoResolvida resolvida = resolverCombinacao(contrato, dataEfetiva, composicao.getId(),
                composicao.getLotacao().getId(), composicao.getJornada().getId(), composicao.getCargo().getId());
        return capacidadeInclusao(contrato, resolvida, dataEfetiva);
    }

    public CapacidadeInclusao capacidadeInclusao(Contrato contrato, CombinacaoResolvida composicao,
                                                  LocalDate dataEfetiva) {
        long ocupacao = alocacaoRepositorio.countOcupacao(contrato.getId(), composicao.lotacao().getId(),
                composicao.jornada().getId(), composicao.cargo().getId(), dataEfetiva);
        long apos = ocupacao + 1;
        return new CapacidadeInclusao(composicao.efetivo(), ocupacao, apos, Math.max(0L, apos - composicao.efetivo()));
    }

    /** Avalia a faixa inteira que o novo vínculo passará a ocupar. */
    public CapacidadeInclusao capacidadeInclusao(Contrato contrato, CombinacaoResolvida composicao,
                                                  LocalDate inicio, LocalDate fimHumano) {
        return avaliarInclusao(contrato, composicao, inicio, fimHumano).capacidade();
    }

    /** O mesmo plano documental governa a capacidade e a gravação do cadastro. */
    public AmpliacaoFim avaliarInclusao(Contrato contrato, CombinacaoResolvida composicao,
                                       LocalDate inicio, LocalDate fimHumano) {
        VinculoContratual proposta = new VinculoContratual();
        proposta.setContrato(contrato); proposta.setDataInicio(inicio); proposta.setDataFim(fimHumano);
        LocalDate fim = fimCoberturaPlanejada(contrato, inicio, fimHumano);
        if (fim != null && fim.isBefore(inicio)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "O período de inclusão deve estar contido na vigência do Contrato.");
        List<FaixaPlanejada> faixas = planejarInclusao(proposta, composicao);
        for (FaixaPlanejada faixa : faixas) {
            if (faixa.estado() == EstadoNegocialAlocacao.REGISTRADA)
                validarExtensaoFaixa(proposta, faixa.alocacao(), faixa.inicio(), faixa.fim());
        }
        return avaliarOcupacaoPlanejada(proposta, faixas, inicio, fim);
    }

    private List<FaixaPlanejada> planejarInclusao(VinculoContratual vinculo, CombinacaoResolvida composicao) {
        LocalDate fim = fimCobertura(vinculo);
        AlocacaoVinculo inicial = novaFaixa(vinculo, composicao.composicao(), composicao.lotacao(),
                composicao.jornada(), composicao.cargo(), vinculo.getDataInicio(), fim, null);
        // Uma inclusão que nasce no destino pertence ao recorte agregado. Não
        // há origem individual que autorize escolher o primeiro/último mapa.
        var recorteInicial = composicaoTemporal.projetarEntidades(
                        vinculo.getContrato(), vinculo.getDataInicio(), Instant.now()).stream()
                .filter(p -> p.lotacao() != null && p.jornada() != null && p.cargo() != null
                        && Objects.equals(p.lotacao().getId(), inicial.getLotacao().getId())
                        && Objects.equals(p.jornada().getId(), inicial.getJornada().getId())
                        && Objects.equals(p.cargo().getId(), inicial.getCargo().getId()))
                .findFirst().orElse(null);
        if (recorteInicial != null && !recorteInicial.mapeamentosOrigemIds().isEmpty()) {
            if (vazio(recorteInicial.jornadaSemanal())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O recorte de destino da transferência não possui grade histórica comprovada em todas as origens. "
                            + "Trate as fontes dos mapas; o cadastro atual não substitui essa prova.");
            inicial.aplicarFotografiaDocumentada(recorteInicial.lotacaoNome(), recorteInicial.jornadaDescricao(),
                    recorteInicial.cargoNome(), recorteInicial.lotacaoMunicipio(), recorteInicial.jornadaSemanal(),
                    recorteInicial.municipioComprovado(), null);
        }
        return reaplicarTransferenciasDocumentadas(vinculo,
                List.of(new FaixaPlanejada(inicial, vinculo.getDataInicio(), fim, EstadoNegocialAlocacao.REGISTRADA)), fim);
    }

    private CapacidadeInclusao capacidadeInclusao(VinculoContratual vinculo, CombinacaoResolvida composicao,
                                                  LocalDate inicio) {
        LocalDate fim = faixaEfetiva(vinculo.getId(), inicio).map(AlocacaoVinculo::getDataFim)
                .orElseGet(() -> fimCobertura(vinculo));
        return capacidadeDuranteFaixa(vinculo.getContrato(), composicao, inicio, fim, vinculo.getId());
    }

    /** Avalia todas as combinações históricas que voltam a existir na extensão do término. */
    public AmpliacaoFim avaliarAmpliacaoFim(VinculoContratual vinculo, LocalDate novoFimHumano) {
        if (vinculo == null || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return null;
        List<AlocacaoVinculo> faixas = ativas(vinculo.getId());
        if (faixas.isEmpty()) return null;
        AlocacaoVinculo ultima = faixas.get(faixas.size() - 1);
        LocalDate fimAnterior = menorFim(ultima.getDataFim(), vinculo.getDataFim());
        LocalDate fimNovo = fimCoberturaPlanejada(vinculo.getContrato(), vinculo.getDataInicio(), novoFimHumano);
        if (fimAnterior == null || fimAnterior.equals(LocalDate.MAX)
                || fimNovo != null && !fimNovo.isAfter(fimAnterior)) return null;
        return avaliarOcupacaoAcrescida(vinculo, vinculo.getDataInicio(), novoFimHumano,
                fimAnterior.plusDays(1), fimNovo);
    }

    public AmpliacaoFim avaliarAmpliacaoInicio(VinculoContratual vinculo, LocalDate novoInicio) {
        if (vinculo == null || novoInicio == null || !novoInicio.isBefore(vinculo.getDataInicio())
                || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return null;
        return avaliarOcupacaoAcrescida(vinculo, novoInicio, vinculo.getDataFim(),
                novoInicio, vinculo.getDataInicio().minusDays(1));
    }

    private AmpliacaoFim avaliarOcupacaoAcrescida(VinculoContratual vinculo, LocalDate novoInicio,
                                                  LocalDate novoFim, LocalDate desde, LocalDate ate) {
        return avaliarOcupacaoPlanejada(vinculo, planejarFaixas(vinculo, novoInicio, novoFim), desde, ate);
    }

    private AmpliacaoFim avaliarOcupacaoPlanejada(VinculoContratual vinculo, List<FaixaPlanejada> faixas,
                                                 LocalDate desde, LocalDate ate) {
        AmpliacaoFim maior = null;
        LocalDate primeiroExcedente = null;
        List<String> assinaturas = new ArrayList<>();
        List<AmpliacaoFim> trechos = new ArrayList<>();
        for (FaixaPlanejada faixa : faixas) {
            if (faixa.estado() != EstadoNegocialAlocacao.REGISTRADA) continue;
            LocalDate inicio = faixa.inicio().isAfter(desde) ? faixa.inicio() : desde;
            LocalDate fim = menorFim(faixa.fim(), ate);
            if (fim != null && inicio.isAfter(fim)) continue;
            AlocacaoVinculo a = faixa.alocacao();
            CombinacaoResolvida combinacao = new CombinacaoResolvida(a.getComposicao(), a.getLotacao(),
                    a.getJornada(), a.getCargo(), 0);
            CapacidadeInclusao capacidade = capacidadeDuranteFaixa(vinculo.getContrato(), combinacao,
                    inicio, fim, vinculo.getId());
            AmpliacaoFim trecho = new AmpliacaoFim(combinacao, capacidade, inicio, fim);
            trechos.add(trecho);
            assinaturas.add(trecho.assinatura());
            if (maior == null || capacidade.excedenteAposInclusao() > maior.capacidade().excedenteAposInclusao()) maior = trecho;
            if (capacidade.primeiroDiaExcedente() != null && (primeiroExcedente == null
                    || capacidade.primeiroDiaExcedente().isBefore(primeiroExcedente))) {
                primeiroExcedente = capacidade.primeiroDiaExcedente();
            }
        }
        if (maior == null) return null;
        CapacidadeInclusao c = maior.capacidade();
        return new AmpliacaoFim(maior.combinacao(), new CapacidadeInclusao(c.efetivoPrevisto(), c.ocupacaoAtual(),
                c.ocupacaoAposInclusao(), c.excedenteAposInclusao(), primeiroExcedente, ate,
                AssinaturaPreviaNegocial.sha256(assinaturas)), desde, ate, trechos);
    }

    /** Varredura de fronteiras: uma consulta de ocupação e deltas, sem consulta para cada pessoa/dia. */
    private CapacidadeInclusao capacidadeDuranteFaixa(Contrato contrato, CombinacaoResolvida composicao,
                                                       LocalDate inicio, LocalDate fim, Long vinculoSubstituido) {
        List<AlocacaoVinculoRepositorio.PeriodoOcupacao> periodos = alocacaoRepositorio.buscarPeriodosOcupacao(
                contrato.getId(), composicao.lotacao().getId(), composicao.jornada().getId(), composicao.cargo().getId(), inicio, fim);
        long ocupacao = alocacaoRepositorio.countOcupacao(contrato.getId(), composicao.lotacao().getId(),
                composicao.jornada().getId(), composicao.cargo().getId(), inicio);
        SortedMap<LocalDate, Long> deltas = new TreeMap<>();
        boolean proprioContado = false;
        for (var periodo : periodos) {
            LocalDate entrada = periodo.getInicioAlocacao().isAfter(periodo.getInicioVinculo())
                    ? periodo.getInicioAlocacao() : periodo.getInicioVinculo();
            LocalDate saida = menorFim(periodo.getFimAlocacao(), periodo.getFimVinculo());
            if (saida != null && saida.isBefore(entrada)) continue;
            if (Objects.equals(vinculoSubstituido, periodo.getVinculoId())) {
                if (!entrada.isAfter(inicio) && (saida == null || !saida.isBefore(inicio))) proprioContado = true;
                continue;
            }
            if (entrada.isAfter(inicio) && (fim == null || !entrada.isAfter(fim))) deltas.merge(entrada, 1L, Long::sum);
            if (saida != null && !saida.equals(LocalDate.MAX)) {
                LocalDate depois = saida.plusDays(1);
                if (depois.isAfter(inicio) && (fim == null || !depois.isAfter(fim))) deltas.merge(depois, -1L, Long::sum);
            }
        }
        if (proprioContado) ocupacao--;
        SortedSet<LocalDate> datas = new TreeSet<>(deltas.keySet());
        var mudancas = composicaoTemporal.datasMudancaCapacidade(contrato, fim);
        if (mudancas != null) datas.addAll(mudancas);
        datas.add(inicio);
        if (fim != null) datas.add(fim);
        datas.removeIf(d -> d.isBefore(inicio) || fim != null && d.isAfter(fim));
        CapacidadeInclusao maior = null;
        LocalDate primeiroExcedente = null;
        List<String> assinatura = new ArrayList<>();
        for (LocalDate data : datas) {
            ocupacao += deltas.getOrDefault(data, 0L);
            var projetadas = composicaoTemporal.projetarEntidades(contrato, data, Instant.now());
            int previsto = composicaoTemporal.localizar(projetadas,
                    composicao.composicao() == null ? null : composicao.composicao().getId(),
                    composicao.lotacao().getId(), composicao.jornada().getId(), composicao.cargo().getId())
                    .filter(p -> Objects.equals(p.lotacao().getId(), composicao.lotacao().getId())
                            && Objects.equals(p.jornada().getId(), composicao.jornada().getId())
                            && Objects.equals(p.cargo().getId(), composicao.cargo().getId()))
                    .map(ComposicaoContratualTemporalServico.ComposicaoProjetada::efetivoPrevisto).orElse(0);
            long apos = ocupacao + 1;
            CapacidadeInclusao atual = new CapacidadeInclusao(previsto, ocupacao, apos, Math.max(0L, apos - previsto));
            if (atual.excede() && primeiroExcedente == null) primeiroExcedente = data;
            if (maior == null || atual.excedenteAposInclusao() > maior.excedenteAposInclusao()) maior = atual;
            assinatura.add(data + ":" + previsto + ":" + ocupacao);
        }
        if (maior == null) return capacidadeInclusao(contrato, composicao, inicio);
        return new CapacidadeInclusao(maior.efetivoPrevisto(), maior.ocupacaoAtual(), maior.ocupacaoAposInclusao(),
                maior.excedenteAposInclusao(), primeiroExcedente, fim, AssinaturaPreviaNegocial.sha256(assinatura));
    }

    public CombinacaoResolvida resolverCombinacaoInicial(Contrato contrato, LocalDate dataEfetiva,
                                                        Long composicaoId, Long lotacaoId,
                                                        Long jornadaId, Long cargoId) {
        CombinacaoResolvida resolvida = resolverCombinacao(contrato, dataEfetiva, composicaoId,
                lotacaoId, jornadaId, cargoId);
        validarCatalogos(resolvida, dataEfetiva);
        return resolvida;
    }

    /** Criação integrada da primeira faixa, chamada pela transação de Pessoa + Vínculo. */
    @Transactional
    public AlocacaoVinculo criarInicial(VinculoContratual vinculo, ComposicaoContratual composicao,
                                        UsuarioAplicacao usuario) {
        if (vinculo == null || composicao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vínculo e composição inicial são obrigatórios.");
        }
        return criarInicial(vinculo, resolverCombinacaoInicial(vinculo.getContrato(), vinculo.getDataInicio(),
                composicao.getId(), null, null, null), usuario);
    }

    @Transactional
    public AlocacaoVinculo criarInicial(VinculoContratual vinculo, CombinacaoResolvida composicao,
                                        UsuarioAplicacao usuario) {
        return criarInicial(vinculo, composicao, usuario, false, null, null, null);
    }

    @Transactional
    public AlocacaoVinculo criarInicial(VinculoContratual vinculo, CombinacaoResolvida composicao,
                                        UsuarioAplicacao usuario, Boolean confirmarExcedente,
                                        String justificativaExcedente, String evidenciaExcedente, String autorizacaoExcedente) {
        if (vinculo == null || composicao == null || composicao.efetivo() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição inicial possui efetivo previsto zero e não aceita nova Alocação.");
        }
        validarCatalogos(composicao, vinculo.getDataInicio());
        List<FaixaPlanejada> plano = planejarInclusao(vinculo, composicao);
        AmpliacaoFim diagnostico = avaliarOcupacaoPlanejada(vinculo, plano, vinculo.getDataInicio(), fimCobertura(vinculo));
        exigirAutorizacaoExcedente(usuario, confirmarExcedente, diagnostico.capacidade());
        if (diagnostico.capacidade().excede())
            exigirJustificativaCompleta(justificativaExcedente, evidenciaExcedente, autorizacaoExcedente);
        FaixaPlanejada primeira = plano.get(0);
        AlocacaoVinculo nova = primeira.alocacao();
        nova.setCriadoPorId(usuario == null ? null : usuario.getId());
        nova.setCriadoPorUsername(usuario == null ? null : usuario.getUsername());
        nova = alocacaoRepositorio.save(nova);
        AmpliacaoFim excedenteInicial = trechoExcedente(diagnostico, primeira);
        registrarEvento(vinculo, nova, TipoEventoAlocacaoVinculo.CRIACAO, TipoRelacaoEventoAlocacao.ORIGEM,
                vinculo.getDataInicio(), usuario, excedenteInicial == null
                        ? "Cadastro integrado de Pessoa, Vínculo e Alocação inicial" : justificativaExcedente,
                excedenteInicial == null ? null : evidenciaExcedente,
                excedenteInicial == null ? null : autorizacaoExcedente,
                null, foto(nova), null, excedenteInicial == null ? null : excedenteInicial.capacidade());
        for (FaixaPlanejada faixa : plano) {
            if (faixa.transferencia() == null || faixa.estado() != EstadoNegocialAlocacao.REGISTRADA) continue;
            TransferenciaGestaoContrato transferencia = faixa.transferencia();
            AlocacaoVinculo destino = faixa.alocacao();
            AmpliacaoFim excedente = trechoExcedente(diagnostico, faixa);
            substituir(vinculo, faixa.inicio(), new CombinacaoResolvida(destino.getComposicao(),
                            destino.getLotacao(), destino.getJornada(), destino.getCargo(), 1), usuario,
                    "[APLICACAO_DE_MAPEAMENTO_DOCUMENTADO: " + transferencia.getId() + "] "
                            + (excedente == null ? transferencia.getMotivo() : justificativaExcedente),
                    excedente == null ? transferencia.getReferenciaSei() : evidenciaExcedente,
                    excedente == null ? transferencia.getAutorizacaoAdministrativa() : autorizacaoExcedente,
                    transferencia, TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO,
                    excedente == null ? null : excedente.capacidade());
        }
        garantirCobertura(vinculo);
        return faixaEfetiva(vinculo.getId(), RelogioSistema.hoje()).orElse(nova);
    }

    public PreviaImpactoAlocacaoDTO previaAlteracao(Long vinculoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        VinculoContratual vinculo = obterVinculo(vinculoId);
        acesso.exigirAcessoContrato(usuario, vinculo.getContrato());
        validarRequisicao(req);
        validarDataNoVinculo(vinculo, req.dataEfetiva());
        CombinacaoResolvida alvo = resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(),
                req.lotacaoId(), req.jornadaId(), req.cargoId());
        validarCatalogos(alvo, req.dataEfetiva());

        AlocacaoVinculo atual = faixaEfetiva(vinculoId, req.dataEfetiva()).orElse(null);
        List<String> incompatibilidades = new ArrayList<>();
        if (atual == null) incompatibilidades.add("Não existe alocação efetiva a retificar na data proposta.");
        if (atual != null && mesmaCombinacao(atual, alvo)) {
            incompatibilidades.add("A combinação proposta já é a alocação efetiva na data informada.");
        }
        if (atual != null) {
            AlocacaoVinculo proposta = novaFaixa(vinculo, alvo.composicao(), alvo.lotacao(), alvo.jornada(), alvo.cargo(),
                    req.dataEfetiva(), atual.getDataFim(), usuario);
            try { validarComposicaoDuranteFaixa(vinculo, proposta); }
            catch (ResponseStatusException e) {
                incompatibilidades.add("A composição não permanece válida durante a faixa proposta: " + e.getReason());
            }
        }
        List<String> impactos = new ArrayList<>(List.of(
                "faixa temporal de Lotação + Turno + Cargo",
                "eventos de alocação posteriores preservados",
                "frequências históricas a revalidar",
                "relatórios e indicadores temporais a reprocessar"));
        CapacidadeInclusao capacidade = capacidadeInclusao(vinculo, alvo, req.dataEfetiva());
        if (capacidade.excede()) {
            impactos.add("ALERTA DE EXCEDENTE: previsto=" + capacidade.efetivoPrevisto()
                    + ", ocupação atual=" + capacidade.ocupacaoAtual()
                    + ", ocupação após inclusão=" + capacidade.ocupacaoAposInclusao()
                    + ", excedente=" + capacidade.excedenteAposInclusao()
                    + ", primeiro dia excedente=" + capacidade.primeiroDiaExcedente()
                    + ". Exige confirmação específica e autorização do Gestor Local.");
        }
        if (atual != null && atual.getDataFim() != null && req.dataEfetiva().equals(atual.getDataFim())) {
            impactos.add("mudança no último dia da faixa; desligamento na mesma data exige precedência inequívoca");
        }
        return new PreviaImpactoAlocacaoDTO(tokenPrevia(vinculo, req, alvo), vinculoId,
                atual == null ? null : atual.getId(), req.dataEfetiva(), atual == null ? null : atual.getDataFim(),
                List.copyOf(impactos), List.copyOf(incompatibilidades));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AlocacaoVinculoDTO alterar(Long vinculoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculoParaAtualizacao(vinculoId, usuario);
        exigirContratoOperacional(vinculo.getContrato());
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(),
                req == null ? null : req.autorizacao());
        PreviaImpactoAlocacaoDTO previa = previaAlteracao(vinculoId, req);
        confirmarPrevia(req, previa);
        exigirPrecedenciaUltimoDia(vinculo, req);
        if (!previa.incompatibilidades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", previa.incompatibilidades()));
        }
        CombinacaoResolvida destino = resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(),
                req.lotacaoId(), req.jornadaId(), req.cargoId());
        CapacidadeInclusao capacidade = capacidadeInclusao(vinculo, destino, req.dataEfetiva());
        exigirAutorizacaoExcedente(usuario, req, capacidade);
        AlocacaoVinculo nova = substituir(vinculo, req.dataEfetiva(), destino,
                usuario, justificativaComPrecedencia(vinculo, req), req.evidencia(), req.autorizacao(), null,
                TipoEventoAlocacaoVinculo.ALTERACAO, capacidade);
        garantirCobertura(vinculo);
        revalidarFrequencias(vinculo, req.dataEfetiva(), nova.getDataFim(), "Alteração retroativa de Alocação");
        return paraDTO(nova);
    }

    /** A mesma data substitui formalmente a faixa anterior, sem criar dois fatos efetivos no dia. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AlocacaoVinculoDTO retificar(Long vinculoId, Long alocacaoId, AlteracaoAlocacaoVinculoRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculoParaAtualizacao(vinculoId, usuario);
        exigirContratoOperacional(vinculo.getContrato());
        AlocacaoVinculo alvo = alocacaoRepositorio.findById(alocacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocação não encontrada."));
        if (!Objects.equals(alvo.getVinculo().getId(), vinculoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alocação pertence a outro vínculo.");
        }
        if (req == null || !Objects.equals(req.dataEfetiva(), alvo.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A retificação deve usar o primeiro dia da alocação informada.");
        }
        exigirJustificativaCompleta(req.justificativa(), req.evidencia(), req.autorizacao());
        PreviaImpactoAlocacaoDTO previa = previaAlteracao(vinculoId, req);
        confirmarPrevia(req, previa);
        exigirPrecedenciaUltimoDia(vinculo, req);
        if (!previa.incompatibilidades().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join(" ", previa.incompatibilidades()));
        }
        CombinacaoResolvida combinacao = resolverCombinacao(vinculo, req.dataEfetiva(), req.composicaoId(),
                req.lotacaoId(), req.jornadaId(), req.cargoId());
        if (mesmaCombinacao(alvo, combinacao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A retificação não altera a combinação existente.");
        }
        CapacidadeInclusao capacidade = capacidadeInclusao(vinculo, combinacao, req.dataEfetiva());
        exigirAutorizacaoExcedente(usuario, req, capacidade);
        AlocacaoVinculo nova = substituir(vinculo, req.dataEfetiva(), combinacao, usuario,
                justificativaComPrecedencia(vinculo, req), req.evidencia(), req.autorizacao(), null,
                TipoEventoAlocacaoVinculo.RETIFICACAO, capacidade);
        garantirCobertura(vinculo);
        revalidarFrequencias(vinculo, req.dataEfetiva(), nova.getDataFim(), "Retificação retroativa de Alocação");
        return paraDTO(nova);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AlocacaoVinculoDTO cancelarProgramacao(Long vinculoId, Long alocacaoId, AcaoVinculoContratualRequisicao req) {
        UsuarioAplicacao usuario = acesso.usuarioAtual();
        acesso.exigirGestaoEmpregado(usuario);
        VinculoContratual vinculo = obterVinculoParaAtualizacao(vinculoId, usuario);
        exigirJustificativaCompleta(req == null ? null : req.justificativa(), req == null ? null : req.evidencia(),
                req == null ? null : req.autorizacao());
        AlocacaoVinculo alvo = alocacaoRepositorio.findById(alocacaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alocação não encontrada."));
        if (!Objects.equals(alvo.getVinculo().getId(), vinculoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alocação pertence a outro vínculo.");
        }
        if (alvo.getEstadoNegocial() != EstadoNegocialAlocacao.REGISTRADA || !RelogioSistema.hoje().isBefore(alvo.getDataInicio())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente alocação futura ainda registrada pode ter a programação cancelada.");
        }
        FotoAlocacao anterior = foto(alvo);
        AlocacaoVinculo predecessora = anteriorAtiva(vinculoId, alvo.getDataInicio()).orElse(null);
        FotoAlocacao fotoPredecessora = predecessora == null ? null : foto(predecessora);
        CapacidadeInclusao capacidadeRestaurada = null;
        if (predecessora != null) {
            CombinacaoResolvida combinacao = resolverCombinacao(vinculo, alvo.getDataInicio(),
                    predecessora.getComposicao() == null ? null : predecessora.getComposicao().getId(),
                    predecessora.getLotacao().getId(), predecessora.getJornada().getId(), predecessora.getCargo().getId());
            validarExtensaoFaixa(vinculo, predecessora, alvo.getDataInicio(), anterior.fim());
            capacidadeRestaurada = capacidadeDuranteFaixa(vinculo.getContrato(), combinacao,
                    alvo.getDataInicio(), anterior.fim(), vinculoId);
            exigirAutorizacaoExcedente(usuario, req.confirmarInclusaoExcedente(), capacidadeRestaurada);
        }
        alvo.setEstadoNegocial(EstadoNegocialAlocacao.CANCELADA);
        alocacaoRepositorio.save(alvo);
        if (predecessora != null) {
            predecessora.setDataFim(anterior.fim());
            alocacaoRepositorio.save(predecessora);
        }
        registrarEvento(vinculo, alvo, TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO,
                TipoRelacaoEventoAlocacao.CANCELA, alvo.getDataInicio(), usuario, req.justificativa(), req.evidencia(),
                req.autorizacao(), anterior, foto(alvo), null);
        if (predecessora != null) {
            registrarEvento(vinculo, predecessora, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                    TipoRelacaoEventoAlocacao.AJUSTA, alvo.getDataInicio(), usuario,
                    "Restauração da faixa anterior após cancelamento de programação: " + req.justificativa(), req.evidencia(), req.autorizacao(),
                    fotoPredecessora, foto(predecessora), null, capacidadeRestaurada);
        }
        garantirCobertura(vinculo);
        revalidarFrequencias(vinculo, alvo.getDataInicio(), anterior.fim(),
                "Cancelamento de Alocação programada alterou a previsão operacional");
        return paraDTO(alvo);
    }

    /**
     * Uma decisão futura independente precisa ser tratada antes de perder todo
     * seu período. Fatos históricos podem ser corrigidos retroativamente; o
     * ajuste derivado mantém sua origem para eventual retificação posterior.
     */
    public void exigirTratamentoProgramacoesFuturas(VinculoContratual vinculo, LocalDate novoInicio, LocalDate novoFim) {
        List<Long> incompativeis = programacoesFuturasIncompativeis(vinculo, novoInicio, novoFim);
        if (!incompativeis.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Os novos limites inviabilizam Alocações futuras " + incompativeis
                        + ". Cancele ou retifique explicitamente essas programações antes de concluir a alteração do vínculo.");
    }

    public List<Long> programacoesFuturasIncompativeis(VinculoContratual vinculo, LocalDate novoInicio, LocalDate novoFim) {
        Set<Long> mudancas = eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).stream()
                .filter(e -> e.getTipoEvento() == TipoEventoAlocacaoVinculo.ALTERACAO
                        || e.getTipoEvento() == TipoEventoAlocacaoVinculo.RETIFICACAO
                        || e.getTipoEvento() == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO)
                .map(EventoAlocacaoVinculo::getAlocacaoNova).filter(Objects::nonNull)
                .map(AlocacaoVinculo::getId).collect(Collectors.toSet());
        return planejarFaixas(vinculo, novoInicio, novoFim).stream()
                .filter(f -> f.estado() != EstadoNegocialAlocacao.REGISTRADA
                        && f.alocacao().getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA
                        && f.alocacao().getDataInicio().isAfter(RelogioSistema.hoje())
                        && mudancas.contains(f.alocacao().getId()))
                .map(f -> f.alocacao().getId()).toList();
    }

    /** Aplica os limites à linha de decisões preservada, sem reaproveitar um corte anterior como origem. */
    @Transactional
    public void sincronizarLimitesVinculo(VinculoContratual vinculo, UsuarioAplicacao usuario, String justificativa) {
        sincronizarLimitesVinculo(vinculo, usuario, justificativa, null, null, null);
    }

    @Transactional
    public void sincronizarLimitesVinculo(VinculoContratual vinculo, UsuarioAplicacao usuario, String justificativa,
                                           String evidencia, String autorizacao, AmpliacaoFim ampliacao) {
        if (vinculo == null || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return;
        List<FaixaPlanejada> planejadas = planejarFaixas(vinculo, vinculo.getDataInicio(), vinculo.getDataFim());
        if (planejadas.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Vínculo operacional não possui Alocação para sincronizar.");
        for (FaixaPlanejada planejada : planejadas) {
            AlocacaoVinculo faixa = planejada.alocacao();
            boolean nova = faixa.getId() == null;
            boolean novaPorTransferencia = nova && planejada.transferencia() != null && !planejada.reconstituida();
            if (planejada.estado() == EstadoNegocialAlocacao.REGISTRADA) {
                validarExtensaoFaixa(vinculo, faixa, planejada.inicio(), planejada.fim());
                // Uma ampliação cria cobertura a partir do primeiro dia acrescido;
                // a situação do catálogo nesse dia também precisa ser comprovada.
                if (!nova && faixa.getDataFim() != null && !faixa.getDataFim().equals(LocalDate.MAX)
                        && (planejada.fim() == null || planejada.fim().isAfter(faixa.getDataFim()))) {
                    LocalDate acrescido = faixa.getDataFim().plusDays(1);
                    if (acrescido.isBefore(planejada.inicio())) acrescido = planejada.inicio();
                    validarExtensaoFaixa(vinculo, faixa, acrescido, planejada.fim());
                }
            }
            if (!nova && Objects.equals(faixa.getDataInicio(), planejada.inicio())
                    && Objects.equals(faixa.getDataFim(), planejada.fim()) && faixa.getEstadoNegocial() == planejada.estado()) continue;
            FotoAlocacao antes = nova ? foto(planejada.anterior()) : foto(faixa);
            faixa.setDataInicio(planejada.inicio());
            faixa.setDataFim(planejada.fim());
            faixa.setEstadoNegocial(planejada.estado());
            if (nova) {
                faixa.setCriadoPorId(usuario == null ? null : usuario.getId());
                faixa.setCriadoPorUsername(usuario == null ? null : usuario.getUsername());
            }
            alocacaoRepositorio.save(faixa);
            AmpliacaoFim trecho = trechoExcedente(ampliacao, planejada);
            LocalDate dataEvento = novaPorTransferencia || trecho == null ? planejada.inicio()
                    : trecho.capacidade().primeiroDiaExcedente() == null ? trecho.inicio() : trecho.capacidade().primeiroDiaExcedente();
            registrarEvento(vinculo, faixa, planejada.reconstituida() ? TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA
                            : novaPorTransferencia ? TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                            : TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                    novaPorTransferencia ? TipoRelacaoEventoAlocacao.SUBSTITUI : TipoRelacaoEventoAlocacao.AJUSTA,
                    dataEvento, usuario, novaPorTransferencia || planejada.reconstituida()
                            ? "[APLICACAO_DE_MAPEAMENTO_DOCUMENTADO: " + planejada.transferencia().getId() + "] " + justificativa : justificativa,
                    novaPorTransferencia || planejada.reconstituida() || trecho != null ? evidencia : "Revalidação da linha de Alocação pelos limites do vínculo",
                    novaPorTransferencia || planejada.reconstituida() || trecho != null ? autorizacao : "Fluxo de vínculo", antes, foto(faixa), planejada.transferencia(),
                    trecho == null ? null : trecho.capacidade());
        }
        garantirCobertura(vinculo);
    }

    private AmpliacaoFim trechoExcedente(AmpliacaoFim ampliacao, FaixaPlanejada faixa) {
        if (ampliacao == null || faixa.estado() != EstadoNegocialAlocacao.REGISTRADA) return null;
        List<AmpliacaoFim> trechos = ampliacao.trechos().isEmpty() ? List.of(ampliacao) : ampliacao.trechos();
        return trechos.stream().filter(t -> t.capacidade().excede()
                        && mesmaCombinacao(faixa.alocacao(), t.combinacao())
                        && (faixa.fim() == null || !t.inicio().isAfter(faixa.fim()))
                        && (t.fim() == null || !faixa.inicio().isAfter(t.fim())))
                .findFirst().orElse(null);
    }

    private List<FaixaPlanejada> planejarFaixas(VinculoContratual vinculo, LocalDate inicio, LocalDate fimHumano) {
        Map<Long, AlocacaoVinculo> entidades = alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculo.getId())
                .stream().collect(Collectors.toMap(AlocacaoVinculo::getId, a -> a));
        List<MotorTemporalAlocacaoServico.FaixaProjetada> historicas = motor.reconstruirFaixasNegociais(vinculo, Instant.now());
        List<FaixaPlanejada> originais;
        if (historicas == null || historicas.isEmpty()) {
            // Registros legados sem cadeia de eventos conservam a melhor faixa conhecida.
            originais = ativas(vinculo.getId()).stream()
                    .map(a -> new FaixaPlanejada(a, a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial())).toList();
        } else {
            originais = historicas.stream().filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA)
                    .map(f -> {
                        AlocacaoVinculo a = entidades.get(f.id());
                        if (a == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "A origem da Alocação histórica não foi localizada para revalidar o vínculo.");
                        return new FaixaPlanejada(a, f.dataInicio(), f.dataFim(), f.estado());
                    }).toList();
        }
        originais = reconstituirOrigemNaAntecipacao(vinculo, originais, inicio);
        LocalDate fim = fimCoberturaPlanejada(vinculo.getContrato(), inicio, fimHumano);
        List<FaixaPlanejada> resultado = new ArrayList<>();
        for (int i = 0; i < originais.size(); i++) {
            FaixaPlanejada original = originais.get(i);
            LocalDate inicioBase = i == 0 && inicio.isBefore(original.inicio()) ? inicio : original.inicio();
            LocalDate fimBase = i == originais.size() - 1 ? fim : menorFim(original.fim(), fim);
            LocalDate inicioRecortado = inicioBase.isBefore(inicio) ? inicio : inicioBase;
            if (fimBase != null && fimBase.isBefore(inicioRecortado)) {
                resultado.add(new FaixaPlanejada(original.alocacao(), original.inicio(), original.fim(), EstadoNegocialAlocacao.CANCELADA));
            } else {
                resultado.add(new FaixaPlanejada(original.alocacao(), inicioRecortado, fimBase, EstadoNegocialAlocacao.REGISTRADA,
                        original.transferencia(), original.anterior(), original.reconstituida()));
            }
        }
        return reaplicarTransferenciasDocumentadas(vinculo, resultado, fim);
    }

    /**
     * Uma faixa encerrada antes da transferência ainda não tinha sucessora no
     * destino. Sua extensão explícita reaplica o mapeamento já autorizado, sem
     * inferir um novo recorte a partir dos cadastros atuais. Planejar é só leitura.
     */
    private List<FaixaPlanejada> reaplicarTransferenciasDocumentadas(VinculoContratual vinculo,
            List<FaixaPlanejada> originais, LocalDate limite) {
        List<FaixaPlanejada> resultado = new ArrayList<>(originais);
        for (TransferenciaGestaoContrato transferencia : transferenciasValidas(vinculo.getContrato(), limite)) {
            LocalDate data = transferencia.getDataEfetiva();
            List<FaixaPlanejada> seguintes = new ArrayList<>();
            for (FaixaPlanejada faixa : resultado) {
                AlocacaoVinculo origem = faixa.alocacao();
                if (faixa.estado() != EstadoNegocialAlocacao.REGISTRADA
                        || data.isBefore(faixa.inicio())
                        || faixa.fim() != null && faixa.fim().isBefore(data)
                        || origem.getId() != null && faixa.transferencia() == null
                            && (origem.getDataFim() == null || !origem.getDataFim().isBefore(data))
                        || origem.getLotacao().getGestaoLocal() == null
                        || !Objects.equals(origem.getLotacao().getGestaoLocal().getId(), transferencia.getGestaoOrigem().getId())) {
                    seguintes.add(faixa); continue;
                }
                MapeamentoTransferenciaGestao mapa = transferencia.getMapeamentos().stream().filter(m ->
                        Objects.equals(m.getLotacaoOrigem().getId(), origem.getLotacao().getId())
                                && Objects.equals(m.getJornadaOrigem().getId(), origem.getJornada().getId())
                                && Objects.equals(m.getCargoOrigem().getId(), origem.getCargo().getId()))
                        .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "A extensão do Vínculo cruza a transferência " + transferencia.getId()
                                        + " sem mapeamento documental para a Alocação de origem. Trate o contexto posterior explicitamente."));
                LocalDate inicioDestino = faixa.inicio().isAfter(data) ? faixa.inicio() : data;
                boolean substituiDesdeInicio = inicioDestino.equals(faixa.inicio());
                seguintes.add(new FaixaPlanejada(origem, faixa.inicio(),
                        substituiDesdeInicio ? faixa.fim() : inicioDestino.minusDays(1),
                        substituiDesdeInicio ? EstadoNegocialAlocacao.SUBSTITUIDA : EstadoNegocialAlocacao.REGISTRADA,
                        faixa.transferencia(), faixa.anterior()));
                AlocacaoVinculo nova = novaFaixa(vinculo, mapa.getComposicaoOrigem(), mapa.getLotacaoDestino(),
                        mapa.getJornadaDestino(), mapa.getCargoDestino(), inicioDestino, faixa.fim(), null);
                aplicarFotografiaDestino(nova, mapa);
                seguintes.add(new FaixaPlanejada(nova, inicioDestino, faixa.fim(), EstadoNegocialAlocacao.REGISTRADA,
                        transferencia, origem));
            }
            resultado = seguintes;
        }
        return resultado;
    }

    private List<TransferenciaGestaoContrato> transferenciasValidas(Contrato contrato, LocalDate limite) {
        if (transferenciasGestao == null || motorContrato == null || contrato == null) return List.of();
        Map<Long, TransferenciaGestaoContrato> porEvento = transferenciasGestao
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(t -> t.getEvento() != null && t.getEvento().getId() != null)
                .collect(Collectors.toMap(t -> t.getEvento().getId(), t -> t));
        return motorContrato.eventosValidosOrdenados(contrato.getId(), limite == null ? LocalDate.MAX : limite, Instant.now())
                .stream().map(e -> porEvento.get(e.getId())).filter(Objects::nonNull).toList();
    }

    /** Retrocede apenas por mapas inequívocos; o mestre de hoje não prova a origem. */
    private List<FaixaPlanejada> reconstituirOrigemNaAntecipacao(VinculoContratual vinculo,
            List<FaixaPlanejada> originais, LocalDate inicio) {
        if (originais.isEmpty() || !inicio.isBefore(originais.get(0).inicio())) return originais;
        FaixaPlanejada primeira = originais.get(0);
        AlocacaoVinculo corrente = primeira.alocacao();
        LocalDate limiteDireita = primeira.fim();
        LocalDate inicioConhecido = primeira.inicio();
        List<FaixaPlanejada> prefixo = new ArrayList<>();
        List<TransferenciaGestaoContrato> transferencias = new ArrayList<>(transferenciasValidas(vinculo.getContrato(), inicioConhecido));
        Collections.reverse(transferencias);
        TransferenciaGestaoContrato fonteOrigem = null;
        for (TransferenciaGestaoContrato transferencia : transferencias) {
            LocalDate data = transferencia.getDataEfetiva();
            if (!data.isAfter(inicio) || data.isAfter(inicioConhecido)
                    || !Objects.equals(transferencia.getGestaoDestino().getId(), corrente.getLotacao().getGestaoLocal().getId())) continue;
            AlocacaoVinculo destino = corrente;
            List<MapeamentoTransferenciaGestao> candidatos = transferencia.getMapeamentos().stream()
                    .filter(m -> mesmoDestino(m, destino))
                    .filter(m -> destino.getComposicao() == null || m.getComposicaoOrigem() != null
                            && Objects.equals(destino.getComposicao().getId(), m.getComposicaoOrigem().getId())).toList();
            if (candidatos.size() != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A antecipação atravessa transferência sem origem documental inequívoca. Trate explicitamente a Alocação anterior.");
            MapeamentoTransferenciaGestao mapa = candidatos.get(0);
            if (limiteDireita == null || !limiteDireita.isBefore(data)) {
                prefixo.add(0, new FaixaPlanejada(corrente, data, limiteDireita, EstadoNegocialAlocacao.REGISTRADA,
                        transferencia, null, true));
            }
            corrente = novaFaixa(vinculo, mapa.getComposicaoOrigem(), mapa.getLotacaoOrigem(), mapa.getJornadaOrigem(),
                    mapa.getCargoOrigem(), inicio, data.minusDays(1), null);
            corrente.aplicarFotografiaDaOrigemDoMapeamento(mapa);
            limiteDireita = data.minusDays(1);
            inicioConhecido = data;
            fonteOrigem = transferencia;
        }
        if (fonteOrigem == null) return originais;
        prefixo.add(0, new FaixaPlanejada(corrente, inicio, limiteDireita, EstadoNegocialAlocacao.REGISTRADA,
                fonteOrigem, null, true));
        prefixo.addAll(originais.subList(1, originais.size()));
        return prefixo;
    }

    private boolean mesmoDestino(MapeamentoTransferenciaGestao mapa, AlocacaoVinculo faixa) {
        return Objects.equals(mapa.getLotacaoDestino().getId(), faixa.getLotacao().getId())
                && Objects.equals(mapa.getJornadaDestino().getId(), faixa.getJornada().getId())
                && Objects.equals(mapa.getCargoDestino().getId(), faixa.getCargo().getId());
    }

    private void aplicarFotografiaDestino(AlocacaoVinculo alocacao, MapeamentoTransferenciaGestao mapa) {
        Instant conhecido = Instant.now();
        var configuracao = provasHistoricas == null ? new ConsultaProvaHistoricaContratoServico.Configuracao(
                mapa.getLotacaoDestinoNomeFotografia(), mapa.getJornadaDestinoDescricaoFotografia(),
                mapa.getCargoDestinoNomeFotografia(), mapa.getLotacaoDestinoMunicipioFotografia(),
                mapa.getJornadaDestinoSemanalFotografia()) : provasHistoricas.configuracao(mapa, conhecido);
        if (vazio(configuracao.jornadaSemanal())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O mapeamento da transferência não possui fotografia histórica comprovada da jornada do destino. "
                        + "Trate a fonte legada; o cadastro atual não substitui essa prova.");
        alocacao.aplicarFotografiaDocumentada(configuracao.lotacaoNome(), configuracao.jornadaDescricao(),
                configuracao.cargoNome(), configuracao.lotacaoMunicipio(), configuracao.jornadaSemanal(),
                configuracao.municipioComprovado(),null);
    }

    public List<String> impactosTransferenciasRestauradas(VinculoContratual vinculo, LocalDate inicio, LocalDate fim) {
        return planejarFaixas(vinculo, inicio, fim).stream()
                .filter(f -> f.transferencia() != null && f.estado() == EstadoNegocialAlocacao.REGISTRADA)
                .map(f -> "Reaplicação do mapeamento documental da transferência " + f.transferencia().getId()
                        + " na Alocação desde " + f.inicio() + ": " + f.alocacao().getLotacaoNomeFotografia()
                        + " / " + f.alocacao().getJornadaDescricaoFotografia() + " / " + f.alocacao().getCargoNomeFotografia()
                        + "; capacidade e validade reavaliadas na faixa restaurada.")
                .toList();
    }

    /** Ajuste derivado do aditamento; não altera início, combinação, sucessoras ou término humano. */
    @Transactional
    public void estenderUltimaFaixaPorProrrogacao(VinculoContratual vinculo, LocalDate fimAnterior,
            LocalDate limitePlanejado, LocalDate dataEfetiva, Long eventoContratoOrigemId,
            UsuarioAplicacao usuario, String justificativa) {
        List<AlocacaoVinculo> faixas = ativas(vinculo.getId());
        if (faixas.isEmpty() || limitePlanejado == null || !limitePlanejado.isAfter(fimAnterior)) return;
        AlocacaoVinculo ultima = faixas.get(faixas.size() - 1);
        if (!Objects.equals(ultima.getDataFim(), fimAnterior)) return;
        LocalDate primeiroDia = fimAnterior.plusDays(1);
        validarExtensaoFaixa(vinculo, ultima, primeiroDia, limitePlanejado);
        CombinacaoResolvida combinacao = resolverCombinacao(vinculo, primeiroDia,
                ultima.getComposicao() == null ? null : ultima.getComposicao().getId(),
                ultima.getLotacao().getId(), ultima.getJornada().getId(), ultima.getCargo().getId());
        CapacidadeInclusao capacidade = capacidadeDuranteFaixa(vinculo.getContrato(), combinacao,
                primeiroDia, limitePlanejado, vinculo.getId());
        if (capacidade.excede() && !possuiExcedenteAutorizado(vinculo, ultima)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A prorrogação produziria nova ocupação excedente sem autorização específica. "
                            + "Trate explicitamente a programação/composição antes de confirmar o aditamento.");
        }
        FotoAlocacao anterior = foto(ultima);
        ultima.setDataFim(limitePlanejado);
        alocacaoRepositorio.save(ultima);
        registrarEvento(vinculo, ultima, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                TipoRelacaoEventoAlocacao.AJUSTA, dataEfetiva, usuario, justificativa,
                "Aditamento: evento contratual " + eventoContratoOrigemId,
                "Prorrogação tempestiva documentada; preservados os limites humanos e autorizações de excedente existentes",
                anterior, foto(ultima), null, capacidade);
        garantirCobertura(vinculo);
    }

    private boolean possuiExcedenteAutorizado(VinculoContratual vinculo, AlocacaoVinculo faixa) {
        boolean origemAutorizada = eventosVinculo != null
                && eventosVinculo.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).stream()
                .anyMatch(e -> e.isInclusaoExcedente()
                        && Objects.equals(e.getLotacaoExcedenteId(), faixa.getLotacao().getId())
                        && Objects.equals(e.getJornadaExcedenteId(), faixa.getJornada().getId())
                        && Objects.equals(e.getCargoExcedenteId(), faixa.getCargo().getId())
                        && !vazio(e.getJustificativa()) && !vazio(e.getEvidencia()) && !vazio(e.getAutorizacao()));
        return origemAutorizada || eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).stream()
                .anyMatch(e -> e.isInclusaoExcedente()
                        && Objects.equals(e.getLotacaoNovaId(), faixa.getLotacao().getId())
                        && Objects.equals(e.getJornadaNovaId(), faixa.getJornada().getId())
                        && Objects.equals(e.getCargoNovoId(), faixa.getCargo().getId())
                        && !vazio(e.getJustificativa()) && !vazio(e.getEvidencia()) && !vazio(e.getAutorizacao()));
    }

    /** Aplica a nova responsabilidade aos vínculos existentes sem criar outro vínculo ou desligamento. */
    @Transactional
    public void aplicarTransferenciaGestao(TransferenciaGestaoContrato transferencia, UsuarioAplicacao usuario) {
        if (transferencia == null || transferencia.getContrato() == null) return;
        Map<String, MapeamentoTransferenciaGestao> mapas = transferencia.getMapeamentos().stream().collect(Collectors.toMap(
                m -> chave(m.getLotacaoOrigem().getId(), m.getJornadaOrigem().getId(), m.getCargoOrigem().getId()),
                m -> m, (a, b) -> a, LinkedHashMap::new));
        Map<Long, VinculoContratual> vinculos = new LinkedHashMap<>();
        for (AlocacaoVinculo faixa : alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(
                transferencia.getContrato().getId())) {
            VinculoContratual vinculo = faixa.getVinculo();
            vinculos.put(vinculo.getId(), vinculo);
        }
        for (VinculoContratual vinculo : vinculos.values()) {
            if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            for (AlocacaoVinculo faixa : new ArrayList<>(ativas(vinculo.getId()))) {
                if (faixa.getDataFim() != null && faixa.getDataFim().isBefore(transferencia.getDataEfetiva())) continue;
                LocalDate efetiva = faixa.getDataInicio().isAfter(transferencia.getDataEfetiva())
                        ? faixa.getDataInicio() : transferencia.getDataEfetiva();
                MapeamentoTransferenciaGestao mapa = mapas.get(chave(faixa.getLotacao().getId(), faixa.getJornada().getId(), faixa.getCargo().getId()));
                if (mapa == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Transferência sem mapeamento para uma Alocação posterior do vínculo " + vinculo.getId() + ".");
                }
                CombinacaoResolvida destino = new CombinacaoResolvida(mapa.getComposicaoOrigem(), mapa.getLotacaoDestino(),
                        mapa.getJornadaDestino(), mapa.getCargoDestino(), 1);
                substituir(vinculo, efetiva, destino, usuario, transferencia.getMotivo(), transferencia.getReferenciaSei(),
                        transferencia.getAutorizacaoAdministrativa(), transferencia, TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO);
            }
            garantirCobertura(vinculo);
            revalidarFrequencias(vinculo, transferencia.getDataEfetiva(),
                    "Transferência excepcional de Gestão alterou a Alocação histórica");
        }
    }

    /**
     * Reaplica a cadeia vigente após retificação/cancelamento de transferência.
     * As decisões independentes são restauradas de suas fotografias e os efeitos
     * derivados antigos ficam preservados, encerrados por novos eventos.
     * Qualquer incompatibilidade posterior aborta a transação contratual inteira.
     */
    @Transactional
    public void reprocessarTransferenciasGestao(Contrato contrato, LocalDate desde,
                                               UsuarioAplicacao usuario, String motivo) {
        if (contrato == null || contrato.getId() == null || desde == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrato e data são obrigatórios para reprocessar transferências.");
        Map<Long, TransferenciaGestaoContrato> porEvento = transferenciasGestao
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(t -> t.getEvento() != null).collect(Collectors.toMap(t -> t.getEvento().getId(), t -> t));
        List<TransferenciaGestaoContrato> vigentes = motorContrato
                .eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, Instant.now()).stream()
                .map(e -> porEvento.get(e.getId())).filter(Objects::nonNull).toList();
        Map<Long, VinculoContratual> vinculos = new TreeMap<>();
        alocacaoRepositorio.findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(contrato.getId())
                .forEach(a -> vinculos.put(a.getVinculo().getId(), a.getVinculo()));
        for (Long vinculoId : vinculos.keySet()) {
            VinculoContratual vinculo = vinculoRepositorio.buscarPorIdParaAtualizacao(vinculoId).orElseThrow();
            if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) continue;
            Map<Long, AlocacaoVinculo> existentes = alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculoId)
                    .stream().collect(Collectors.toMap(AlocacaoVinculo::getId, a -> a, (a, b) -> a, LinkedHashMap::new));
            List<MotorTemporalAlocacaoServico.FaixaProjetada> decisoes = motor
                    .reconstruirDecisoesIndependentes(vinculo, Instant.now()).stream()
                    .filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA).toList();
            if (decisoes.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há fonte documental da Alocação anterior à transferência do vínculo " + vinculoId + ".");
            LocalDate inicio = vinculo.getDataInicio();
            LocalDate fim = fimCoberturaPlanejada(contrato, inicio, vinculo.getDataFim());
            List<FaixaPlanejada> planejadas = new ArrayList<>();
            for (int i = 0; i < decisoes.size(); i++) {
                var decisao = decisoes.get(i);
                AlocacaoVinculo entidade = existentes.get(decisao.id());
                if (entidade == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Alocação documentada não encontrada para reprocessar o vínculo " + vinculoId + ".");
                LocalDate inicioFaixa = i == 0 ? inicio : decisao.dataInicio().isBefore(inicio) ? inicio : decisao.dataInicio();
                LocalDate fimFaixa = i == decisoes.size() - 1 ? fim : menorFim(decisao.dataFim(), fim);
                if (fimFaixa != null && fimFaixa.isBefore(inicioFaixa)) continue;
                planejadas.add(new FaixaPlanejada(entidade, inicioFaixa, fimFaixa, EstadoNegocialAlocacao.REGISTRADA));
            }
            for (TransferenciaGestaoContrato transferencia : vigentes) {
                List<FaixaPlanejada> seguintes = new ArrayList<>();
                for (FaixaPlanejada faixa : planejadas) {
                    AlocacaoVinculo origem = faixa.alocacao();
                    LocalDate data = transferencia.getDataEfetiva();
                    if (faixa.estado() != EstadoNegocialAlocacao.REGISTRADA
                            || faixa.fim() != null && faixa.fim().isBefore(data)
                            || origem.getLotacao().getGestaoLocal() == null
                            || !Objects.equals(origem.getLotacao().getGestaoLocal().getId(), transferencia.getGestaoOrigem().getId())) {
                        seguintes.add(faixa); continue;
                    }
                    MapeamentoTransferenciaGestao mapa = transferencia.getMapeamentos().stream().filter(m ->
                            Objects.equals(m.getLotacaoOrigem().getId(), origem.getLotacao().getId())
                                    && Objects.equals(m.getJornadaOrigem().getId(), origem.getJornada().getId())
                                    && Objects.equals(m.getCargoOrigem().getId(), origem.getCargo().getId()))
                            .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                    "Transferência " + transferencia.getId() + " sem mapa para decisão de Alocação do vínculo "
                                            + vinculoId + ". Trate explicitamente os fatos posteriores."));
                    LocalDate inicioDestino = faixa.inicio().isAfter(data) ? faixa.inicio() : data;
                    boolean substitui = inicioDestino.equals(faixa.inicio());
                    seguintes.add(new FaixaPlanejada(origem, faixa.inicio(),
                            substitui ? faixa.fim() : inicioDestino.minusDays(1),
                            substitui ? EstadoNegocialAlocacao.SUBSTITUIDA : EstadoNegocialAlocacao.REGISTRADA,
                            faixa.transferencia(), faixa.anterior()));
                    AlocacaoVinculo nova = novaFaixa(vinculo, mapa.getComposicaoOrigem(), mapa.getLotacaoDestino(),
                            mapa.getJornadaDestino(), mapa.getCargoDestino(), inicioDestino, faixa.fim(), usuario);
                    aplicarFotografiaDestino(nova, mapa);
                    seguintes.add(new FaixaPlanejada(nova, inicioDestino, faixa.fim(), EstadoNegocialAlocacao.REGISTRADA,
                            transferencia, origem));
                }
                planejadas = seguintes;
            }
            Set<Long> conservadas = planejadas.stream().map(f -> f.alocacao().getId()).filter(Objects::nonNull).collect(Collectors.toSet());
            for (AlocacaoVinculo anterior : existentes.values()) {
                if (!conservadas.contains(anterior.getId()) && anterior.getEstadoNegocial() == EstadoNegocialAlocacao.REGISTRADA)
                    registrarReprocessamento(vinculo, new FaixaPlanejada(anterior, anterior.getDataInicio(), anterior.getDataFim(),
                            EstadoNegocialAlocacao.CANCELADA), desde, usuario, motivo);
            }
            for (FaixaPlanejada faixa : planejadas) registrarReprocessamento(vinculo, faixa, desde, usuario, motivo);
            try {
                garantirCobertura(vinculo);
            } catch (ResponseStatusException e) {
                if (e.getStatusCode().value() != HttpStatus.BAD_REQUEST.value()) throw e;
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A transferência corrigida é incompatível com uma decisão posterior de Alocação do vínculo "
                                + vinculoId + ". Trate o contexto posterior antes de confirmar. " + e.getReason(), e);
            }
            revalidarFrequencias(vinculo, desde, "Reprocessamento documentado da transferência: " + motivo);
        }
    }

    private void registrarReprocessamento(VinculoContratual vinculo, FaixaPlanejada planejada,
            LocalDate desde, UsuarioAplicacao usuario, String motivo) {
        AlocacaoVinculo faixa = planejada.alocacao();
        boolean nova = faixa.getId() == null;
        if (!nova && Objects.equals(faixa.getDataInicio(), planejada.inicio())
                && Objects.equals(faixa.getDataFim(), planejada.fim()) && faixa.getEstadoNegocial() == planejada.estado()) return;
        FotoAlocacao antes = nova ? foto(planejada.anterior()) : foto(faixa);
        faixa.setDataInicio(planejada.inicio()); faixa.setDataFim(planejada.fim()); faixa.setEstadoNegocial(planejada.estado());
        alocacaoRepositorio.save(faixa);
        registrarEvento(vinculo, faixa, TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA,
                TipoRelacaoEventoAlocacao.AJUSTA, desde, usuario, motivo,
                "Reaplicação da cadeia documental vigente do contrato " + vinculo.getContrato().getId(),
                "Retificação/cancelamento de transferência autorizada pelo fluxo contratual",
                antes, foto(faixa), planejada.transferencia());
    }

    private void revalidarFrequencias(VinculoContratual vinculo, LocalDate desde, String motivo) {
        revalidarFrequencias(vinculo, desde, null, motivo);
    }

    private void revalidarFrequencias(VinculoContratual vinculo, LocalDate desde, LocalDate ate, String motivo) {
        if (revalidacaoFrequenciaTemporalServico != null && vinculo != null && vinculo.getId() != null && desde != null) {
            revalidacaoFrequenciaTemporalServico.revalidarPorVinculo(vinculo.getId(), desde, ate, motivo);
        }
    }

    /** Verifica a ordem inversa: a mudança já existe quando o término é informado/retificado. */
    public boolean possuiMudancaEfetivaNoDia(VinculoContratual vinculo, LocalDate data) {
        if (vinculo == null || data == null) return false;
        AlocacaoVinculo faixa = faixaEfetiva(vinculo.getId(), data).orElse(null);
        if (faixa != null && data.equals(faixa.getDataInicio()) && !data.equals(vinculo.getDataInicio())) return true;
        Set<Long> mudancas = eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).stream()
                .filter(e -> e.getAlocacaoNova() != null && data.equals(e.getDataEfetiva())
                        && (e.getTipoEvento() == TipoEventoAlocacaoVinculo.ALTERACAO
                            || e.getTipoEvento() == TipoEventoAlocacaoVinculo.RETIFICACAO
                            || e.getTipoEvento() == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO))
                .map(e -> e.getAlocacaoNova().getId()).collect(Collectors.toSet());
        if (faixa != null && data.equals(faixa.getDataInicio()) && mudancas.contains(faixa.getId())) return true;
        // A sucessora pode estar cortada pelo término atual e voltar exatamente
        // no novo último dia. CRIACAO não é mudança interna; cancelamento
        // explícito continua excluído da linha de decisões a restaurar.
        List<MotorTemporalAlocacaoServico.FaixaProjetada> decisoes = motor.reconstruirFaixasNegociais(vinculo, Instant.now());
        if (decisoes != null && decisoes.stream().anyMatch(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA
                && data.equals(f.dataInicio()) && mudancas.contains(f.id()))) return true;
        if (transferenciasGestao == null) return false;
        return planejarFaixas(vinculo, vinculo.getDataInicio(), data).stream().anyMatch(f ->
                f.transferencia() != null && f.estado() == EstadoNegocialAlocacao.REGISTRADA && data.equals(f.inicio()));
    }

    private void exigirPrecedenciaUltimoDia(VinculoContratual vinculo, AlteracaoAlocacaoVinculoRequisicao req) {
        if (vinculo.getDataFim() != null && vinculo.getDataFim().equals(req.dataEfetiva())
                && !Boolean.TRUE.equals(req.confirmarPrecedenciaUltimoDia())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Alteração de Alocação no último dia exige confirmarPrecedenciaUltimoDia=true: "
                            + "a Alocação vale no último dia e o desligamento produz efeito após esse dia.");
        }
    }

    private void exigirContratoOperacional(Contrato contrato) {
        if (contrato == null || contrato.getEstadoNegocial() != EstadoNegocialContrato.PUBLICADO
                || !Boolean.TRUE.equals(contrato.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contrato administrativamente inativo não aceita alteração operacional de Alocação.");
        }
    }

    private String justificativaComPrecedencia(VinculoContratual vinculo,
                                                AlteracaoAlocacaoVinculoRequisicao req) {
        if (vinculo.getDataFim() != null && vinculo.getDataFim().equals(req.dataEfetiva())) {
            return "[PRECEDENCIA: ALOCACAO_VALE_NO_ULTIMO_DIA; DESLIGAMENTO_APOS_O_DIA] "
                    + req.justificativa();
        }
        return req.justificativa();
    }

    private AlocacaoVinculo substituir(VinculoContratual vinculo, LocalDate dataEfetiva, CombinacaoResolvida destino,
                                       UsuarioAplicacao usuario, String justificativa, String evidencia, String autorizacao,
                                       TransferenciaGestaoContrato transferencia, TipoEventoAlocacaoVinculo tipoSolicitado) {
        return substituir(vinculo, dataEfetiva, destino, usuario, justificativa, evidencia, autorizacao,
                transferencia, tipoSolicitado, null);
    }

    private AlocacaoVinculo substituir(VinculoContratual vinculo, LocalDate dataEfetiva, CombinacaoResolvida destino,
                                       UsuarioAplicacao usuario, String justificativa, String evidencia, String autorizacao,
                                       TransferenciaGestaoContrato transferencia, TipoEventoAlocacaoVinculo tipoSolicitado,
                                       CapacidadeInclusao capacidade) {
        validarDataNoVinculo(vinculo, dataEfetiva);
        validarCatalogos(destino, dataEfetiva);
        AlocacaoVinculo atual = faixaEfetiva(vinculo.getId(), dataEfetiva).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Não existe Alocação efetiva para a data informada."));
        if (mesmaCombinacao(atual, destino)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A nova Alocação é igual à faixa efetiva atual.");
        }
        FotoAlocacao antes = foto(atual);
        LocalDate fimNova = atual.getDataFim();
        TipoEventoAlocacaoVinculo tipo = tipoSolicitado == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                ? TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                : dataEfetiva.equals(atual.getDataInicio()) ? TipoEventoAlocacaoVinculo.RETIFICACAO : tipoSolicitado;
        TipoRelacaoEventoAlocacao relacao = tipo == TipoEventoAlocacaoVinculo.RETIFICACAO
                ? TipoRelacaoEventoAlocacao.RETIFICA : TipoRelacaoEventoAlocacao.SUBSTITUI;
        if (dataEfetiva.equals(atual.getDataInicio())) {
            atual.setEstadoNegocial(EstadoNegocialAlocacao.SUBSTITUIDA);
        } else {
            atual.setDataFim(dataEfetiva.minusDays(1));
        }
        alocacaoRepositorio.save(atual);
        FotoAlocacao depoisDaFaixaAnterior = foto(atual);
        AlocacaoVinculo nova = novaFaixa(vinculo, destino.composicao(), destino.lotacao(), destino.jornada(), destino.cargo(),
                dataEfetiva, fimNova, usuario);
        if (transferencia != null) {
            AlocacaoVinculo alvoMapeado = nova;
            MapeamentoTransferenciaGestao mapa = transferencia.getMapeamentos().stream()
                    .filter(m -> Objects.equals(m.getLotacaoOrigem().getId(), atual.getLotacao().getId())
                            && Objects.equals(m.getJornadaOrigem().getId(), atual.getJornada().getId())
                            && Objects.equals(m.getCargoOrigem().getId(), atual.getCargo().getId())
                            && mesmoDestino(m, alvoMapeado)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Transferência sem mapa para a Alocação."));
            aplicarFotografiaDestino(nova, mapa);
        }
        nova = alocacaoRepositorio.save(nova);
        registrarEvento(vinculo, nova, tipo, relacao, dataEfetiva, usuario, justificativa, evidencia, autorizacao,
                antes, foto(nova), transferencia, capacidade);
        // O evento de substituição preserva o estado anterior. Este segundo fato
        // registra o resultado da faixa sucedida, permitindo reconstrução
        // bitemporal sem inferir silenciosamente o seu novo fim/estado.
        registrarEvento(vinculo, atual, TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO,
                TipoRelacaoEventoAlocacao.AJUSTA, dataEfetiva, usuario,
                "Ajuste da faixa anterior após " + tipo.name().toLowerCase(Locale.ROOT), evidencia, autorizacao,
                antes, depoisDaFaixaAnterior, transferencia);
        return nova;
    }

    private AlocacaoVinculo novaFaixa(VinculoContratual vinculo, ComposicaoContratual composicao,
                                      Lotacao lotacao, Jornada jornada, Cargo cargo, LocalDate inicio, LocalDate fim,
                                      UsuarioAplicacao usuario) {
        AlocacaoVinculo nova = new AlocacaoVinculo();
        nova.setVinculo(vinculo); nova.setComposicao(composicao); nova.setLotacao(lotacao); nova.setJornada(jornada); nova.setCargo(cargo);
        nova.setDataInicio(inicio); nova.setDataFim(fim); nova.setCriadoPorId(usuario == null ? null : usuario.getId());
        nova.setCriadoPorUsername(usuario == null ? null : usuario.getUsername()); nova.setEstadoNegocial(EstadoNegocialAlocacao.REGISTRADA);
        if (inicio.isBefore(RelogioSistema.hoje())) {
            // Selecionar IDs num ato retroativo não documenta horários,
            // município ou nomes atuais para o passado. Reutiliza o contexto
            // do recorte que valia nessa data, inclusive a ausência de prova.
            // Mapas explícitos de transferência ainda prevalecem nos chamadores.
            var fonte = composicaoTemporal.projetarEntidades(vinculo.getContrato(), inicio, Instant.now()).stream()
                    .filter(p -> p.lotacao() != null && p.jornada() != null && p.cargo() != null
                            && Objects.equals(lotacao.getId(), p.lotacao().getId())
                            && Objects.equals(jornada.getId(), p.jornada().getId())
                            && Objects.equals(cargo.getId(), p.cargo().getId())).findFirst();
            nova.aplicarFotografiaDocumentada(fonte.map(p -> p.lotacaoNome()).orElse(null),
                    fonte.map(p -> p.jornadaDescricao()).orElse(null), fonte.map(p -> p.cargoNome()).orElse(null),
                    fonte.map(p -> p.lotacaoMunicipio()).orElse(null), fonte.map(p -> p.jornadaSemanal()).orElse(null),
                    fonte.map(p -> p.municipioComprovado()).orElse(false),
                    fonte.map(p -> p.aditamentoComposicaoOrigemId())
                            .map(id -> entityManager.getReference(AditamentoComposicao.class,id)).orElse(null));
        }
        return nova;
    }

    private Optional<AlocacaoVinculo> faixaEfetiva(Long vinculoId, LocalDate data) {
        return ativas(vinculoId).stream().filter(a -> !data.isBefore(a.getDataInicio())
                && (a.getDataFim() == null || !data.isAfter(a.getDataFim()))).findFirst();
    }

    private Optional<AlocacaoVinculo> anteriorAtiva(Long vinculoId, LocalDate inicio) {
        return ativas(vinculoId).stream().filter(a -> a.getDataInicio().isBefore(inicio)
                && (a.getDataFim() == null || a.getDataFim().isBefore(inicio))).max(Comparator.comparing(AlocacaoVinculo::getDataFim,
                        Comparator.nullsLast(LocalDate::compareTo)));
    }

    private List<AlocacaoVinculo> ativas(Long vinculoId) {
        return alocacaoRepositorio.findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(vinculoId,
                EstadoNegocialAlocacao.REGISTRADA);
    }

    private CombinacaoResolvida resolverCombinacao(VinculoContratual vinculo, LocalDate data, Long composicaoId,
                                                    Long lotacaoId, Long jornadaId, Long cargoId) {
        return resolverCombinacao(vinculo.getContrato(), data, composicaoId, lotacaoId, jornadaId, cargoId);
    }

    private CombinacaoResolvida resolverCombinacao(Contrato contrato, LocalDate data, Long composicaoId,
                                                   Long lotacaoId, Long jornadaId, Long cargoId) {
        if (contrato == null || data == null || composicaoId == null
                && (lotacaoId == null || jornadaId == null || cargoId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contrato, data efetiva e combinação completa são obrigatórios.");
        }
        List<ComposicaoContratualTemporalServico.ComposicaoProjetada> estado = composicaoTemporal
                .projetarEntidades(contrato, data, Instant.now());
        Optional<ComposicaoContratualTemporalServico.ComposicaoProjetada> encontrada = composicaoTemporal
                .localizar(estado, composicaoId, lotacaoId, jornadaId, cargoId);
        var p = encontrada.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A combinação Contrato → Lotação → Turno → Cargo não existe na composição temporal."));
        if (composicaoId != null && !Objects.equals(composicaoId, p.composicaoBaseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A composição-base informada não pertence à combinação temporal deste contrato.");
        }
        if (lotacaoId != null && !Objects.equals(lotacaoId, p.lotacao().getId())
                || jornadaId != null && !Objects.equals(jornadaId, p.jornada().getId())
                || cargoId != null && !Objects.equals(cargoId, p.cargo().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O recorte informado não corresponde à composição temporal selecionada.");
        }
        if (p.efetivoPrevisto() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A composição possui efetivo previsto zero e não está disponível para nova Alocação.");
        }
        ComposicaoContratual base = p.composicaoBaseId() == null ? null : composicaoRepositorio.findById(p.composicaoBaseId()).orElse(null);
        return new CombinacaoResolvida(base, p.lotacao(), p.jornada(), p.cargo(), p.efetivoPrevisto());
    }

    private void validarCombinacaoDireta(VinculoContratual vinculo, LocalDate data, AlocacaoVinculo faixa) {
        resolverCombinacao(vinculo, data, faixa.getComposicao() == null ? null : faixa.getComposicao().getId(),
                faixa.getLotacao().getId(), faixa.getJornada().getId(), faixa.getCargo().getId());
    }

    private void validarExtensaoFaixa(VinculoContratual vinculo, AlocacaoVinculo faixa,
                                       LocalDate inicio, LocalDate fim) {
        SortedSet<LocalDate> datas = new TreeSet<>();
        var mudancas = composicaoTemporal.datasMudancaCapacidade(vinculo.getContrato(), fim);
        if (mudancas != null) datas.addAll(mudancas);
        datas.add(inicio);
        if (fim != null) datas.add(fim);
        for (LocalDate data : datas) {
            if (data.isBefore(inicio) || fim != null && data.isAfter(fim)) continue;
            CombinacaoResolvida combinacao = resolverCombinacao(vinculo, data,
                    faixa.getComposicao() == null ? null : faixa.getComposicao().getId(),
                    faixa.getLotacao().getId(), faixa.getJornada().getId(), faixa.getCargo().getId());
            // A disponibilidade é exigida na data em que a nova decisão começa.
            // O fim de uma faixa já iniciada não é uma nova utilização do catálogo.
            // A composição e sua capacidade continuam verificadas em todas as fronteiras.
            if (data.equals(inicio)) validarCatalogos(combinacao, data);
        }
    }

    private void validarCatalogos(CombinacaoResolvida combinacao, LocalDate dataEfetiva) {
        if (disponibilidadeCatalogo != null && combinacao.lotacao() != null && combinacao.jornada() != null && combinacao.cargo() != null) {
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.LOTACAO, combinacao.lotacao().getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.JORNADA, combinacao.jornada().getId());
            disponibilidadeCatalogo.bloquearParaUso(com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal.CARGO, combinacao.cargo().getId());
        }
        if (combinacao.lotacao() == null || combinacao.jornada() == null || combinacao.cargo() == null
                || combinacao.lotacao().getGestaoLocal() == null || combinacao.jornada().getGestaoLocal() == null
                || combinacao.cargo().getGestaoLocal() == null
                || !Objects.equals(combinacao.lotacao().getGestaoLocal().getId(), combinacao.jornada().getGestaoLocal().getId())
                || !Objects.equals(combinacao.lotacao().getGestaoLocal().getId(), combinacao.cargo().getGestaoLocal().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lotação, Turno e Cargo devem pertencer à mesma Gestão Local.");
        }
        // A associação foi validada na composição publicada e reconstruída por
        // resolverCombinacao. A lista mestre de hoje não reescreve essa decisão.
        if (!catalogoAtivoEm(TipoCatalogoTemporal.LOTACAO, combinacao.lotacao().getId(), dataEfetiva, combinacao.lotacao().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.JORNADA, combinacao.jornada().getId(), dataEfetiva, combinacao.jornada().getAtivo())
                || !catalogoAtivoEm(TipoCatalogoTemporal.CARGO, combinacao.cargo().getId(), dataEfetiva, combinacao.cargo().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Catálogo inativo não pode ser utilizado em nova Alocação atual ou futura.");
        }
    }

    private boolean catalogoAtivoEm(TipoCatalogoTemporal tipo, Long id, LocalDate data, Boolean atual) {
        return disponibilidadeCatalogo != null
                ? disponibilidadeCatalogo.ativoEm(tipo, id, data, atual)
                : RegraDisponibilidadeCatalogo.permiteUso(data, atual);
    }

    private void validarDataNoVinculo(VinculoContratual vinculo, LocalDate data) {
        if (data == null || vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocação exige Vínculo registrado e data efetiva.");
        }
        if (data.isBefore(inicioCobertura(vinculo)) || fimCobertura(vinculo) != null && data.isAfter(fimCobertura(vinculo))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data da Alocação deve estar contida no período operacional do Vínculo e do Contrato.");
        }
    }

    private LocalDate inicioCobertura(VinculoContratual vinculo) {
        LocalDate inicioContrato = motorContrato == null ? vinculo.getContrato().getDataInicio()
                : motorContrato.reconstruirPosicao(vinculo.getContrato(), vinculo.getDataInicio(), Instant.now()).vigenciaInicio();
        return inicioContrato != null && inicioContrato.isAfter(vinculo.getDataInicio()) ? inicioContrato : vinculo.getDataInicio();
    }

    private LocalDate fimCobertura(VinculoContratual vinculo) {
        return fimCoberturaPlanejada(vinculo.getContrato(), vinculo.getDataInicio(), vinculo.getDataFim());
    }

    /**
     * Limite contínuo conhecido, inclusive atos programados. A projeção de hoje
     * permanece intacta; uma lacuna, cancelamento ou anulação interrompe a faixa.
     * Pausas administrativas não apagam a identidade da alocação.
     */
    public LocalDate fimCoberturaPlanejada(Contrato contrato, LocalDate inicio, LocalDate fimHumano) {
        if (motorContrato == null) return menorFim(contrato.getDataFim(), fimHumano);
        Instant conhecimento = Instant.now();
        PosicaoTemporalContratoDTO inicial = motorContrato.reconstruirPosicao(contrato, inicio, conhecimento);
        LocalDate limite = inicial.vigenciaFim();
        SortedSet<LocalDate> fronteiras = new TreeSet<>();
        for (EventoCicloVidaContrato evento : motorContrato.eventosValidosOrdenados(contrato.getId(), LocalDate.MAX, conhecimento)) {
            LocalDate data = evento.getDataEfetiva();
            if (data != null && data.isAfter(inicio) && (fimHumano == null || !data.isAfter(fimHumano))) fronteiras.add(data);
        }
        for (LocalDate data : fronteiras) {
            if (limite != null && !limite.equals(LocalDate.MAX) && data.isAfter(limite.plusDays(1))) break;
            PosicaoTemporalContratoDTO posicao = motorContrato.reconstruirPosicao(contrato, data, conhecimento);
            if (posicao.estadoNegocial() != EstadoNegocialContrato.PUBLICADO
                    || posicao.vigenciaInicio() != null && data.isBefore(posicao.vigenciaInicio())) {
                limite = menorFim(limite, data.minusDays(1));
                break;
            }
            if (posicao.vigenciaFim() != null && data.isAfter(posicao.vigenciaFim())) {
                limite = menorFim(limite, posicao.vigenciaFim());
                break;
            }
            limite = posicao.vigenciaFim();
        }
        return menorFim(limite, fimHumano);
    }

    private static LocalDate menorFim(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private void garantirCobertura(VinculoContratual vinculo) {
        if (vinculo.getEstadoNegocial() != EstadoNegocialVinculo.REGISTRADO) return;
        List<AlocacaoVinculo> faixas = ativas(vinculo.getId());
        if (faixas.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Todo Vínculo operacional deve possuir exatamente uma Alocação por dia.");
        LocalDate inicio = inicioCobertura(vinculo); LocalDate fim = fimCobertura(vinculo);
        if (fim != null && faixas.stream().anyMatch(f -> f.getDataInicio().isAfter(fim))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existe Alocação ativa posterior ao fim do Vínculo.");
        }
        LocalDate proximo = inicio;
        for (AlocacaoVinculo faixa : faixas) {
            if (faixa.getDataInicio() == null || faixa.getDataInicio().isAfter(proximo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Foi detectada lacuna na linha temporal de Alocação do vínculo.");
            }
            if (faixa.getDataInicio().isBefore(proximo)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Foi detectada sobreposição de Alocações no mesmo dia.");
            }
            validarComposicaoDuranteFaixa(vinculo, faixa);
            if (faixa.getDataFim() == null) {
                if (fim != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Alocação aberta excede o último dia do Vínculo.");
                return;
            }
            proximo = faixa.getDataFim().plusDays(1);
            if (fim != null && proximo.isAfter(fim)) {
                if (!proximo.equals(fim.plusDays(1))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Alocação excede o período do Vínculo.");
                }
                return;
            }
        }
        if (fim == null || !proximo.equals(fim.plusDays(1))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A linha temporal não cobre todos os dias do Vínculo operacional.");
        }
    }

    /** Uma inclusão posterior também deve respeitar reduções já programadas para o intervalo. */
    private void validarComposicaoDuranteFaixa(VinculoContratual vinculo, AlocacaoVinculo faixa) {
        LocalDate inicio = faixa.getDataInicio();
        LocalDate fim = faixa.getDataFim();
        SortedSet<LocalDate> datas = new TreeSet<>();
        var mudancas = composicaoTemporal.datasMudancaCapacidade(vinculo.getContrato(), fim);
        if (mudancas != null) datas.addAll(mudancas);
        datas.add(inicio);
        if (fim != null) datas.add(fim);
        for (LocalDate data : datas) {
            if (data.isBefore(inicio) || fim != null && data.isAfter(fim)) continue;
            validarCombinacaoDireta(vinculo, data, faixa);
        }
    }

    private void validarRequisicao(AlteracaoAlocacaoVinculoRequisicao req) {
        if (req == null || req.dataEfetiva() == null || (req.composicaoId() == null
                && (req.lotacaoId() == null || req.jornadaId() == null || req.cargoId() == null))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data efetiva e uma combinação válida de Alocação são obrigatórias.");
        }
    }

    private void confirmarPrevia(AlteracaoAlocacaoVinculoRequisicao req, PreviaImpactoAlocacaoDTO previa) {
        if (!Boolean.TRUE.equals(req.confirmarImpacto()) || req.tokenPrevia() == null || !req.tokenPrevia().equals(previa.token())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prévia de impacto da Alocação não foi confirmada ou expirou.");
        }
    }

    private String tokenPrevia(VinculoContratual vinculo, AlteracaoAlocacaoVinculoRequisicao req, CombinacaoResolvida alvo) {
        LocalDate data = req.dataEfetiva();
        String hash = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(vinculo.getId())
                .map(EventoAlocacaoVinculo::getHashEvento).orElse("SEM_EVENTO_ALOCACAO");
        List<String> partes = new ArrayList<>(List.of(String.valueOf(vinculo.getId()), String.valueOf(vinculo.getVersion()),
                AssinaturaPreviaNegocial.valor(data), AssinaturaPreviaNegocial.valor(alvo.composicao() == null ? null : alvo.composicao().getId()),
                AssinaturaPreviaNegocial.valor(alvo.lotacao().getId()), AssinaturaPreviaNegocial.valor(alvo.jornada().getId()),
                AssinaturaPreviaNegocial.valor(alvo.cargo().getId()), hash));
        ativas(vinculo.getId()).forEach(a -> partes.add(a.getId() + ":" + a.getVersion() + ":" + a.getDataInicio() + ":" + a.getDataFim()));
        CapacidadeInclusao capacidade = capacidadeInclusao(vinculo, alvo, data);
        partes.add("capacidade:" + capacidade.efetivoPrevisto() + ":" + capacidade.ocupacaoAtual());
        partes.add(AssinaturaPreviaNegocial.valor(capacidade.assinaturaFaixa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.justificativa()));
        partes.add(AssinaturaPreviaNegocial.valor(req.evidencia()));
        partes.add(AssinaturaPreviaNegocial.valor(req.autorizacao()));
        if (assinaturaImpacto != null) partes.addAll(assinaturaImpacto.partesPorVinculo(vinculo, data, null));
        return AssinaturaPreviaNegocial.sha256(partes);
    }

    private void exigirAutorizacaoExcedente(UsuarioAplicacao usuario, AlteracaoAlocacaoVinculoRequisicao req,
                                            CapacidadeInclusao capacidade) {
        exigirAutorizacaoExcedente(usuario, req.confirmarInclusaoExcedente(), capacidade);
    }

    private void exigirAutorizacaoExcedente(UsuarioAplicacao usuario, Boolean confirmar, CapacidadeInclusao capacidade) {
        if (!capacidade.excede()) return;
        if (!acesso.ehGestaoLocal(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente Gestor Local pode autorizar Alocação acima do efetivo previsto.");
        }
        if (!Boolean.TRUE.equals(confirmar)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A Alocação excedente exige confirmarInclusaoExcedente=true, justificativa, evidência e autorização.");
        }
    }

    public List<JustificativaExcedenteDTO> justificativasExcedente(Long contratoId, Long lotacaoId,
                                                                  Long jornadaId, Long cargoId, LocalDate ate) {
        var nivel = minimizador.nivelPadrao(acesso.usuarioAtual());
        return eventoRepositorio.findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(contratoId)
                .stream().filter(e -> ate == null || !e.getDataEfetiva().isAfter(ate))
                .filter(e -> Objects.equals(lotacaoId, e.getLotacaoNovaId()))
                .filter(e -> Objects.equals(jornadaId, e.getJornadaNovaId()))
                .filter(e -> Objects.equals(cargoId, e.getCargoNovoId()))
                .map(e -> new JustificativaExcedenteDTO(e.getId(), e.getVinculo().getId(), e.getDataEfetiva(),
                        e.getRegistradoEm(), e.getResponsavelUsername(), minimizador.restrito(e.getJustificativa(), nivel),
                        minimizador.restrito(e.getEvidencia(), nivel), minimizador.restrito(e.getAutorizacao(), nivel),
                        Optional.ofNullable(e.getEfetivoPrevistoExcedente()).orElse(0),
                        Optional.ofNullable(e.getOcupacaoAposInclusaoExcedente()).orElse(0L), "ALOCACAO"))
                .toList();
    }

    private EventoAlocacaoVinculo registrarEvento(VinculoContratual vinculo, AlocacaoVinculo nova,
                                                    TipoEventoAlocacaoVinculo tipo, TipoRelacaoEventoAlocacao relacao,
                                                    LocalDate dataEfetiva, UsuarioAplicacao usuario,
                                                    String justificativa, String evidencia, String autorizacao,
                                                    FotoAlocacao anterior, FotoAlocacao novaFoto,
                                                    TransferenciaGestaoContrato transferencia) {
        return registrarEvento(vinculo, nova, tipo, relacao, dataEfetiva, usuario, justificativa, evidencia,
                autorizacao, anterior, novaFoto, transferencia, null);
    }

    private EventoAlocacaoVinculo registrarEvento(VinculoContratual vinculo, AlocacaoVinculo nova,
                                                    TipoEventoAlocacaoVinculo tipo, TipoRelacaoEventoAlocacao relacao,
                                                    LocalDate dataEfetiva, UsuarioAplicacao usuario,
                                                    String justificativa, String evidencia, String autorizacao,
                                                    FotoAlocacao anterior, FotoAlocacao novaFoto,
                                                    TransferenciaGestaoContrato transferencia, CapacidadeInclusao capacidade) {
        if (capacidade != null && capacidade.excede()) justificativa += capacidade.detalheTemporal(dataEfetiva);
        EventoAlocacaoVinculo precedente = eventoRepositorio.findTopByVinculoIdOrderByRegistradoEmDescIdDesc(vinculo.getId()).orElse(null);
        EventoAlocacaoVinculo evento = new EventoAlocacaoVinculo();
        evento.setVinculo(vinculo); evento.setAlocacaoNova(nova); evento.setTipoEvento(tipo); evento.setRelacaoPrecedencia(
                precedente == null ? TipoRelacaoEventoAlocacao.ORIGEM : relacao); evento.setEventoPrecedente(precedente);
        evento.setDataEfetiva(dataEfetiva); evento.setResponsavelId(usuario == null ? null : usuario.getId());
        evento.setResponsavelUsername(usuario == null ? null : usuario.getUsername()); evento.setJustificativa(justificativa);
        evento.setEvidencia(evidencia); evento.setAutorizacao(autorizacao); aplicarAnterior(evento, anterior); aplicarNovo(evento, novaFoto);
        if (transferencia != null) {
            evento.setTransferenciaGestaoId(transferencia.getId());
            evento.setGestaoOrigemIdFotografia(transferencia.getGestaoOrigem().getId());
            evento.setGestaoOrigemNomeFotografia(transferencia.getGestaoOrigemNomeFotografia());
            evento.setGestaoDestinoIdFotografia(transferencia.getGestaoDestino().getId());
            evento.setGestaoDestinoNomeFotografia(transferencia.getGestaoDestinoNomeFotografia());
        }
        String hashAnterior = precedente == null ? null : precedente.getHashEvento();
        evento.setHashAnterior(hashAnterior);
        evento.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|",
                AssinaturaPreviaNegocial.valor(hashAnterior), tipo.name(), AssinaturaPreviaNegocial.valor(dataEfetiva),
                fotoTexto(anterior), fotoTexto(novaFoto), AssinaturaPreviaNegocial.valor(usuario == null ? null : usuario.getId()),
                AssinaturaPreviaNegocial.valor(justificativa), AssinaturaPreviaNegocial.valor(transferencia == null ? null : transferencia.getId()))));
        if (capacidade != null && capacidade.excede()) {
            evento.setInclusaoExcedente(true);
            evento.setEfetivoPrevistoExcedente(capacidade.efetivoPrevisto());
            evento.setOcupacaoAposInclusaoExcedente(capacidade.ocupacaoAposInclusao());
            evento.setHashEvento(UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|", evento.getHashEvento(),
                    "EXCEDENTE", String.valueOf(capacidade.efetivoPrevisto()),
                    String.valueOf(capacidade.ocupacaoAposInclusao()),
                    AssinaturaPreviaNegocial.valor(evidencia), AssinaturaPreviaNegocial.valor(autorizacao))));
        }
        return eventoRepositorio.save(evento);
    }

    private void aplicarAnterior(EventoAlocacaoVinculo e, FotoAlocacao f) {
        if (f == null) return;
        e.setAlocacaoAnteriorId(f.id()); e.setComposicaoAnteriorId(f.composicaoId()); e.setLotacaoAnteriorId(f.lotacaoId());
        e.setLotacaoAnteriorNome(f.lotacaoNome()); e.setJornadaAnteriorId(f.jornadaId());
        e.setJornadaAnteriorDescricao(f.jornadaDescricao()); e.setCargoAnteriorId(f.cargoId());
        e.setCargoAnteriorNome(f.cargoNome()); e.setInicioAnterior(f.inicio());
        e.setFimAnterior(f.fim()); e.setEstadoAnterior(f.estado());
    }

    private void aplicarNovo(EventoAlocacaoVinculo e, FotoAlocacao f) {
        if (f == null) return;
        e.setComposicaoNovaId(f.composicaoId()); e.setLotacaoNovaId(f.lotacaoId()); e.setLotacaoNovaNome(f.lotacaoNome());
        e.setJornadaNovaId(f.jornadaId()); e.setJornadaNovaDescricao(f.jornadaDescricao()); e.setCargoNovoId(f.cargoId());
        e.setCargoNovoNome(f.cargoNome()); e.setInicioNovo(f.inicio()); e.setFimNovo(f.fim()); e.setEstadoNovo(f.estado());
    }

    private FotoAlocacao foto(AlocacaoVinculo a) {
        return a == null ? null : new FotoAlocacao(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), nomeLotacao(a), a.getJornada().getId(), descricaoJornada(a),
                a.getCargo().getId(), nomeCargo(a), a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial());
    }

    private String nomeLotacao(AlocacaoVinculo a) {
        return rotuloFotografado(a.getLotacaoNomeFotografia());
    }
    private String descricaoJornada(AlocacaoVinculo a) {
        return rotuloFotografado(a.getJornadaDescricaoFotografia());
    }
    private String nomeCargo(AlocacaoVinculo a) {
        return rotuloFotografado(a.getCargoNomeFotografia());
    }

    private static String rotuloFotografado(String valor) { return valor == null || valor.isBlank() ? null : valor; }

    private String fotoTexto(FotoAlocacao f) {
        return f == null ? "" : String.join(":", AssinaturaPreviaNegocial.valor(f.id()), AssinaturaPreviaNegocial.valor(f.composicaoId()),
                AssinaturaPreviaNegocial.valor(f.lotacaoId()), AssinaturaPreviaNegocial.valor(f.jornadaId()),
                AssinaturaPreviaNegocial.valor(f.cargoId()), AssinaturaPreviaNegocial.valor(f.inicio()),
                AssinaturaPreviaNegocial.valor(f.fim()), AssinaturaPreviaNegocial.valor(f.estado()));
    }

    private boolean mesmaCombinacao(AlocacaoVinculo a, CombinacaoResolvida b) {
        return Objects.equals(a.getLotacao().getId(), b.lotacao().getId()) && Objects.equals(a.getJornada().getId(), b.jornada().getId())
                && Objects.equals(a.getCargo().getId(), b.cargo().getId());
    }

    private boolean sobrepoeMes(AlocacaoVinculo a, YearMonth mes) {
        if (mes == null) return true;
        LocalDate inicio = mes.atDay(1); LocalDate fim = mes.atEndOfMonth();
        return !a.getDataInicio().isAfter(fim) && (a.getDataFim() == null || !a.getDataFim().isBefore(inicio));
    }

    private String chave(Long lotacao, Long jornada, Long cargo) { return lotacao + ":" + jornada + ":" + cargo; }

    private VinculoContratual obterVinculo(Long id) {
        return vinculoRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
    }

    private VinculoContratual obterVinculoParaAtualizacao(Long id, UsuarioAplicacao usuario) {
        var identidade = vinculoRepositorio.buscarIdentidade(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
        Contrato contrato = contratoRepositorio.buscarPorIdParaAtualizacao(identidade.getContratoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado."));
        acesso.exigirAcessoContrato(usuario, contrato);
        return vinculoRepositorio.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo contratual não encontrado."));
    }

    private void exigirJustificativaCompleta(String justificativa, String evidencia, String autorizacao) {
        if (vazio(justificativa) || vazio(evidencia) || vazio(autorizacao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justificativa, evidência e autorização são obrigatórias.");
        }
    }

    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }

    private AlocacaoVinculoDTO paraDTO(AlocacaoVinculo a) {
        return new AlocacaoVinculoDTO(a.getId(), a.getVinculo().getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), nomeLotacao(a), a.getJornada().getId(), descricaoJornada(a),
                a.getCargo().getId(), nomeCargo(a), a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial(),
                a.getCriadoEm(), a.getCriadoPorId(), a.getCriadoPorUsername(), a.getVersion());
    }

    private EventoAlocacaoVinculoDTO paraDTO(EventoAlocacaoVinculo e, UsuarioAplicacao usuario) {
        var nivel = minimizador.nivelPadrao(usuario);
        return new EventoAlocacaoVinculoDTO(e.getId(), e.getVinculo().getId(), e.getAlocacaoNova() == null ? null : e.getAlocacaoNova().getId(),
                e.getTipoEvento(), e.getRelacaoPrecedencia(), e.getEventoPrecedente() == null ? null : e.getEventoPrecedente().getId(),
                e.getDataEfetiva(), e.getRegistradoEm(), e.getResponsavelId(), e.getResponsavelUsername(), minimizador.restrito(e.getJustificativa(), nivel),
                minimizador.restrito(e.getEvidencia(), nivel), minimizador.restrito(e.getAutorizacao(), nivel),
                e.getAlocacaoAnteriorId(), e.getInicioAnterior(), e.getFimAnterior(),
                e.getEstadoAnterior(), e.getComposicaoAnteriorId(), e.getLotacaoAnteriorId(), e.getLotacaoAnteriorNome(),
                e.getJornadaAnteriorId(), e.getJornadaAnteriorDescricao(), e.getCargoAnteriorId(), e.getCargoAnteriorNome(),
                e.getInicioNovo(), e.getFimNovo(), e.getEstadoNovo(), e.getComposicaoNovaId(),
                e.getLotacaoNovaId(), e.getLotacaoNovaNome(), e.getJornadaNovaId(), e.getJornadaNovaDescricao(),
                e.getCargoNovoId(), e.getCargoNovoNome(), e.getTransferenciaGestaoId(),
                e.getGestaoOrigemIdFotografia(), e.getGestaoOrigemNomeFotografia(), e.getGestaoDestinoIdFotografia(),
                e.getGestaoDestinoNomeFotografia(), e.getHashAnterior(), e.getHashEvento());
    }
}
