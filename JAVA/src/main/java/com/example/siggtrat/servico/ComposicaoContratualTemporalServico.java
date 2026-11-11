package com.example.siggtrat.servico;

import com.example.siggtrat.dto.*;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import com.example.siggtrat.repositorio.TransferenciaGestaoContratoRepositorio;
import com.example.siggtrat.regra.RegraEfetivoContratual;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconstrói a composição contratual sem sobrescrever a fotografia publicada.
 *
 * A tabela composicao_contratual permanece como fotografia/base publicada.
 * Aditamentos e remanejamentos são deltas auditáveis aplicados, na ordem
 * temporal/negocial, para responder qual composição valia em qualquer data.
 */
@Service
public class ComposicaoContratualTemporalServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(ComposicaoContratualTemporalServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    public record ConflitoReplay(Long eventoId, TipoEventoCicloVidaContrato tipoEvento,
                                 LocalDate dataEfetiva, String motivo) {}

    public record DeltaProposto(Long composicaoBaseId, Lotacao lotacao, Jornada jornada, Cargo cargo, int variacao) {}

    public record PropostaComposicao(LocalDate dataEfetiva, TipoEventoCicloVidaContrato tipo,
                                    String chaveNegocial, Integer ordemDocumental, Long eventoPrecedenteId,
                                    TipoRelacaoPrecedenciaNegocial relacaoPrecedencia, List<DeltaProposto> deltas) {}

    /** Limite pactuado até a posição corrente do replay, com sua fonte própria. */
    private record FimContratual(LocalDate data, ProvaCampoHistoricoDTO prova) {}

    public record ComposicaoProjetada(
            Long composicaoBaseId,
            Lotacao lotacao,
            Jornada jornada,
            Cargo cargo,
            String lotacaoNome,
            String jornadaDescricao,
            String cargoNome,
            int efetivoPrevisto,
            LocalDate dataInicio,
            LocalDate dataFim,
            String lotacaoMunicipio,
            String jornadaSemanal,
            Long aditamentoComposicaoOrigemId,
            boolean municipioComprovado,
            Set<Long> mapeamentosOrigemIds,
            @com.fasterxml.jackson.annotation.JsonIgnore Map<String,ProvaCampoHistoricoDTO> provaCampos
    ) {
        public ComposicaoProjetada {
            mapeamentosOrigemIds = mapeamentosOrigemIds == null ? Set.of() : Set.copyOf(mapeamentosOrigemIds);
            provaCampos = Map.copyOf(provaCampos);
        }

        public ComposicaoProjetada(Long composicaoBaseId, Lotacao lotacao, Jornada jornada, Cargo cargo,
                String lotacaoNome, String jornadaDescricao, String cargoNome, int efetivoPrevisto,
                LocalDate dataInicio, LocalDate dataFim, String lotacaoMunicipio, String jornadaSemanal,
                Long aditamentoComposicaoOrigemId, boolean municipioComprovado, Set<Long> mapeamentosOrigemIds) {
            this(composicaoBaseId,lotacao,jornada,cargo,lotacaoNome,jornadaDescricao,cargoNome,efetivoPrevisto,
                    dataInicio,dataFim,lotacaoMunicipio,jornadaSemanal,aditamentoComposicaoOrigemId,
                    municipioComprovado,mapeamentosOrigemIds,Map.of());
        }

        public ComposicaoProjetada comProvas(Map<String,ProvaCampoHistoricoDTO> campos) {
            return new ComposicaoProjetada(composicaoBaseId,lotacao,jornada,cargo,lotacaoNome,jornadaDescricao,
                    cargoNome,efetivoPrevisto,dataInicio,dataFim,lotacaoMunicipio,jornadaSemanal,
                    aditamentoComposicaoOrigemId,municipioComprovado,mapeamentosOrigemIds,campos);
        }

        public ComposicaoProjetada(Long composicaoBaseId, Lotacao lotacao, Jornada jornada, Cargo cargo,
                String lotacaoNome, String jornadaDescricao, String cargoNome, int efetivoPrevisto,
                LocalDate dataInicio, LocalDate dataFim, String lotacaoMunicipio, String jornadaSemanal,
                Long aditamentoComposicaoOrigemId, boolean municipioComprovado) {
            this(composicaoBaseId, lotacao, jornada, cargo, lotacaoNome, jornadaDescricao, cargoNome,
                    efetivoPrevisto, dataInicio, dataFim, lotacaoMunicipio, jornadaSemanal,
                    aditamentoComposicaoOrigemId, municipioComprovado, Set.of());
        }

        public ComposicaoProjetada(Long composicaoBaseId, Lotacao lotacao, Jornada jornada, Cargo cargo,
                String lotacaoNome, String jornadaDescricao, String cargoNome, int efetivoPrevisto,
                LocalDate dataInicio, LocalDate dataFim, String lotacaoMunicipio, String jornadaSemanal) {
            this(composicaoBaseId,lotacao,jornada,cargo,lotacaoNome,jornadaDescricao,cargoNome,
                    efetivoPrevisto,dataInicio,dataFim,lotacaoMunicipio,jornadaSemanal,null,
                    lotacaoMunicipio!=null||jornadaSemanal!=null&&!jornadaSemanal.isBlank());
        }
        public String chave() {
            return chave(lotacao != null ? lotacao.getId() : null,
                    jornada != null ? jornada.getId() : null,
                    cargo != null ? cargo.getId() : null);
        }

        public static String chave(Long lotacaoId, Long jornadaId, Long cargoId) {
            return String.valueOf(lotacaoId) + ":" + String.valueOf(jornadaId) + ":" + String.valueOf(cargoId);
        }
    }

    private final ComposicaoContratualRepositorio composicaoRepositorio;
    private final AditamentoContratualRepositorio aditamentoRepositorio;
    private final RemanejamentoContratualRepositorio remanejamentoRepositorio;
    private final MotorTemporalContratoServico motorTemporal;
    private TransferenciaGestaoContratoRepositorio transferenciaGestaoRepositorio;
    private ConsultaProvaHistoricaContratoServico provasHistoricas;
    @Autowired(required=false)
    public void configurarProvasHistoricas(ConsultaProvaHistoricaContratoServico servico) { this.provasHistoricas=servico; }


    public ComposicaoContratualTemporalServico(
            ComposicaoContratualRepositorio composicaoRepositorio,
            AditamentoContratualRepositorio aditamentoRepositorio,
            RemanejamentoContratualRepositorio remanejamentoRepositorio,
            MotorTemporalContratoServico motorTemporal) {
        this.composicaoRepositorio = composicaoRepositorio;
        this.aditamentoRepositorio = aditamentoRepositorio;
        this.remanejamentoRepositorio = remanejamentoRepositorio;
        this.motorTemporal = motorTemporal;
    }

    /** Dependência opcional apenas para permitir a composição modular do serviço. */
    @Autowired(required = false)
    public void configurarTransferenciasGestao(TransferenciaGestaoContratoRepositorio transferenciaGestaoRepositorio) {
        this.transferenciaGestaoRepositorio = transferenciaGestaoRepositorio;
    }

    public List<ComposicaoProjetada> projetarEntidades(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        return projetarEntidades(contrato, dataReferencia, conhecidoAte, Set.of());
    }

    @Transactional(readOnly = true)
    public List<ComposicaoProjetada> projetarEntidades(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte, Set<Long> eventosIgnorados) {
        return projetarEntidadesInterno(contrato, dataReferencia, conhecidoAte, eventosIgnorados, null, null);
    }

    @Transactional(readOnly = true)
    public List<ComposicaoProjetada> projetarEntidades(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte,
            Set<Long> eventosIgnorados,LeituraHistoricaContratos entradas) {
        return projetarEntidadesInterno(contrato,dataReferencia,conhecidoAte,eventosIgnorados,null,null,entradas);
    }

    /** Aplica a proposta na ordem negocial real e então reaplica integralmente os fatos posteriores. */
    @Transactional(readOnly = true)
    public List<ComposicaoProjetada> simularEntidades(Contrato contrato, LocalDate dataReferencia,
                                                     Instant conhecidoAte, Set<Long> eventosIgnorados,
                                                     PropostaComposicao proposta) {
        return projetarEntidadesInterno(contrato, dataReferencia, conhecidoAte, eventosIgnorados, null, proposta);
    }

    @Transactional(readOnly = true)
    public List<ConflitoReplay> diagnosticarConflitos(Contrato contrato, LocalDate dataReferencia,
                                                      Instant conhecidoAte) {
        List<ConflitoReplay> conflitos = new ArrayList<>();
        projetarEntidadesInterno(contrato, dataReferencia, conhecidoAte, Set.of(), conflitos, null);
        return List.copyOf(conflitos);
    }

    @Transactional(readOnly = true)
    public List<ConflitoReplay> diagnosticarConflitosSimulados(Contrato contrato, Instant conhecidoAte,
                                                               Set<Long> ignorados, PropostaComposicao proposta) {
        List<ConflitoReplay> conflitos = new ArrayList<>();
        projetarEntidadesInterno(contrato, LocalDate.MAX, conhecidoAte, ignorados, conflitos, proposta);
        return List.copyOf(conflitos);
    }

    private List<ComposicaoProjetada> projetarEntidadesInterno(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte,
            Set<Long> eventosIgnorados, List<ConflitoReplay> conflitos, PropostaComposicao proposta) {
        return projetarEntidadesInterno(contrato,dataReferencia,conhecidoAte,eventosIgnorados,conflitos,proposta,null);
    }
    private List<ComposicaoProjetada> projetarEntidadesInterno(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte,
            Set<Long> eventosIgnorados,List<ConflitoReplay> conflitos,PropostaComposicao proposta,LeituraHistoricaContratos entradas) {
        if (contrato == null || contrato.getId() == null) return List.of();
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;

        LinkedHashMap<String, ComposicaoProjetada> estado = new LinkedHashMap<>();
        for (ComposicaoContratual base : composicaoRepositorio
                .findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contrato.getId())) {
            // Após a publicação, a composição-base é a fotografia original. Se a
            // vigência do MESMO contrato for prorrogada por aditamento, essa base
            // continua sendo o ponto de partida da projeção; o fim original não
            // pode fazer a composição desaparecer artificialmente. A validade
            // operacional do contrato é verificada pelo ciclo de vida temporal.
            if (base.getDataInicioVigencia() == null || data.isBefore(base.getDataInicioVigencia())) continue;
            ComposicaoProjetada p = new ComposicaoProjetada(
                    base.getId(), base.getLotacao(), base.getJornada(), base.getCargo(),
                    nomeFotografado(base.getLotacaoNomeFotografia()),
                    nomeFotografado(base.getJornadaDescricaoFotografia()),
                    nomeFotografado(base.getCargoNomeFotografia()),
                    Optional.ofNullable(base.getEfetivoPrevisto()).orElse(0),
                    base.getDataInicioVigencia(), base.getDataFimVigencia(),
                    base.getLotacaoMunicipioFotografia(), base.getJornadaSemanalFotografia());
            if(provasHistoricas!=null) {
                var config=entradas==null?provasHistoricas.configuracao(base,conhecimento):provasHistoricas.configuracao(base,conhecimento,entradas);
                var fonte=entradas==null?provasHistoricas.origemComposicao(base,conhecimento):provasHistoricas.origemComposicao(base,conhecimento,entradas);
                var campos=ProvasCamposHistoricos.desconhecidos(valores(p).keySet());
                campos.putAll(config.provaCampos());
                for(String campo:List.of("lotacaoId","jornadaId","cargoId","dataInicioVigencia","dataFimVigencia"))
                    campos.put(campo,ProvasCamposHistoricos.comprovado(valores(p).get(campo),fonte));
                campos.put("efetivoPrevisto",base.getEfetivoPrevisto()==null?ProvasCamposHistoricos.desconhecido()
                        :ProvasCamposHistoricos.comprovado(base.getEfetivoPrevisto(),fonte));
                campos.put("permiteNovaAlocacao",ProvasCamposHistoricos.derivado(p.efetivoPrevisto()>0,campos.get("efetivoPrevisto")));
                p=new ComposicaoProjetada(p.composicaoBaseId(),p.lotacao(),p.jornada(),p.cargo(),
                        config.lotacaoNome(),config.jornadaDescricao(),config.cargoNome(),p.efetivoPrevisto(),
                        p.dataInicio(),p.dataFim(),config.lotacaoMunicipio(),config.jornadaSemanal(),
                        p.aditamentoComposicaoOrigemId(),config.municipioComprovado()).comProvas(campos);
            }
            estado.put(p.chave(), p);
        }

        var fotografiaOriginal = entradas==null?motorTemporal.fotografiaOriginalConhecida(contrato,conhecimento)
                :motorTemporal.fotografiaOriginalConhecida(contrato,conhecimento,entradas);
        FimContratual fimContratual = fotografiaOriginal.map(v -> {
            var fotografia = ProvasCamposHistoricos.fotografia(v);
            var prova = (entradas==null?motorTemporal.provasDaFotografia(fotografia):motorTemporal.provasDaFotografia(fotografia,entradas))
                    .getOrDefault("dataFim", ProvasCamposHistoricos.desconhecido());
            return new FimContratual(v.getDataFim(), prova);
        }).orElseGet(() -> new FimContratual(null, ProvasCamposHistoricos.desconhecido()));
        int efetivoOriginal = fotografiaOriginal
                .map(VersaoContratoNegocial::getEfetivoPrevisto).orElseGet(() ->
                        Optional.ofNullable(contrato.getEfetivoOriginalPublicado()).orElseGet(() ->
                                totalRepresentavel(estado.values())));
        Map<Long, AditamentoContratual> aditamentos = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .stream().filter(a -> a.getEvento() != null && a.getEvento().getId() != null)
                .collect(Collectors.toMap(a -> a.getEvento().getId(), Function.identity(), (a, b) -> b, LinkedHashMap::new));

        Map<Long, RemanejamentoContratual> remanejamentos = remanejamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .stream().filter(r -> r.getEvento() != null && r.getEvento().getId() != null)
                .collect(Collectors.toMap(r -> r.getEvento().getId(), Function.identity(), (a, b) -> b, LinkedHashMap::new));

        Set<Long> ignorar = eventosIgnorados == null ? Set.of() : eventosIgnorados;
        List<EventoCicloVidaContrato> eventos = new ArrayList<>(entradas==null?motorTemporal.eventosValidosOrdenados(contrato.getId(), data, conhecimento)
                :motorTemporal.eventosValidosOrdenados(contrato.getId(), data, conhecimento,entradas));
        EventoCicloVidaContrato eventoProposto = null;
        if (proposta != null && !proposta.dataEfetiva().isAfter(data)) {
            // A identidade transitória serve apenas às arestas do grafo; não é desempate de precedência.
            eventoProposto = new EventoCicloVidaContrato() {
                @Override public Long getId() { return Long.MIN_VALUE; }
            };
            eventoProposto.setContrato(contrato); eventoProposto.setTipoEvento(proposta.tipo());
            eventoProposto.setDataEfetiva(proposta.dataEfetiva()); eventoProposto.setChaveNegocial(proposta.chaveNegocial());
            eventoProposto.setOrdemDocumental(proposta.ordemDocumental()); eventoProposto.setFatoLogicoId("PROPOSTA_NAO_PERSISTIDA");
            if (proposta.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.RETIFICA
                    || proposta.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUBSTITUI) {
                String fatoOriginal = eventos.stream().filter(e -> Objects.equals(e.getId(), proposta.eventoPrecedenteId()))
                        .map(EventoCicloVidaContrato::getFatoLogicoId).filter(Objects::nonNull).findFirst().orElse(null);
                if (fatoOriginal != null) eventoProposto.setFatoLogicoId(fatoOriginal);
            }
            List<RelacaoPrecedenciaEventoContrato> relacoes = new ArrayList<>(entradas==null?motorTemporal.relacoesConhecidas(contrato.getId(), conhecimento)
                    :motorTemporal.relacoesConhecidas(contrato.getId(), conhecimento,entradas));
            if (proposta.eventoPrecedenteId() != null && proposta.relacaoPrecedencia() == TipoRelacaoPrecedenciaNegocial.SUCEDE) {
                EventoCicloVidaContrato alvo = eventos.stream().filter(e -> Objects.equals(e.getId(), proposta.eventoPrecedenteId()))
                        .findFirst().orElse(null);
                if (alvo != null) {
                    RelacaoPrecedenciaEventoContrato relacao = new RelacaoPrecedenciaEventoContrato();
                    relacao.setContrato(contrato); relacao.setEventoFonte(eventoProposto); relacao.setEventoAlvo(alvo);
                    relacao.setTipoRelacao(TipoRelacaoPrecedenciaNegocial.SUCEDE); relacoes.add(relacao);
                }
            }
            eventos.removeIf(e -> ignorar.contains(e.getId())); eventos.add(eventoProposto);
            eventos = motorTemporal.ordenarEventosParaSimulacao(eventos, relacoes);
        }
        for (EventoCicloVidaContrato evento : eventos) {
            if (evento.getId() == null || ignorar.contains(evento.getId())) continue;
            LinkedHashMap<String, ComposicaoProjetada> antes = new LinkedHashMap<>(estado);
            try {
                if (evento == eventoProposto) {
                    aplicarProposta(estado, proposta, fimContratual);
                } else if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.ADITAMENTO) {
                    AditamentoContratual a = aditamentos.get(evento.getId());
                    if (a != null) {
                        // Um ADI contribui só com o próprio delta. Nem sua foto integral
                        // nem a projeção atual podem antecipar uma prorrogação em D/K.
                        if (a.getNovaDataFim() != null) fimContratual = new FimContratual(a.getNovaDataFim(),
                                ProvasCamposHistoricos.comprovado(a.getNovaDataFim(), ProvasCamposHistoricos.evento(evento)));
                        aplicarAditamento(estado, a, efetivoOriginal, conhecimento, fimContratual, entradas);
                    }
                } else if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.REMANEJAMENTO) {
                    RemanejamentoContratual r = remanejamentos.get(evento.getId());
                    if (r != null) aplicarRemanejamento(estado, r);
                } else if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL
                        && transferenciaGestaoRepositorio != null) {
                    transferenciaGestaoRepositorio.findByEventoId(evento.getId())
                            .ifPresent(t -> aplicarTransferenciaGestao(estado, t, conhecimento, entradas));
                }
            } catch (IllegalStateException ex) {
                estado.clear(); estado.putAll(antes);
                if (conflitos != null) conflitos.add(new ConflitoReplay(evento.getId(), evento.getTipoEvento(),
                        evento.getDataEfetiva(), ex.getMessage()));
            }
        }

        return estado.values().stream()
                .sorted(Comparator.comparing(ComposicaoProjetada::lotacaoNome,
                                Comparator.nullsFirst(String::compareToIgnoreCase))
                        .thenComparing(ComposicaoProjetada::jornadaDescricao,
                                Comparator.nullsFirst(String::compareToIgnoreCase))
                        .thenComparing(ComposicaoProjetada::cargoNome,
                                Comparator.nullsFirst(String::compareToIgnoreCase)))
                .toList();
    }

    private void aplicarProposta(Map<String, ComposicaoProjetada> estado, PropostaComposicao proposta, FimContratual fimContratual) {
        long soma = 0;
        for (DeltaProposto delta : proposta.deltas()) {
            String chave = ComposicaoProjetada.chave(delta.lotacao().getId(), delta.jornada().getId(), delta.cargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            if (anterior == null && proposta.tipo() == TipoEventoCicloVidaContrato.REMANEJAMENTO)
                throw new IllegalStateException("Remanejamento proposto depende de composição inexistente: " + chave);
            long quantidadeCalculada = (long) (anterior == null ? 0 : anterior.efetivoPrevisto()) + delta.variacao();
            if (quantidadeCalculada < 0) throw new IllegalStateException("Proposta incompatível com a composição " + chave);
            if (quantidadeCalculada > Integer.MAX_VALUE)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Proposta incompatível com a composição " + chave
                        + ": quantidade não representável integralmente.");
            int quantidade = (int) quantidadeCalculada;
            estado.put(chave, new ComposicaoProjetada(delta.composicaoBaseId(), delta.lotacao(), delta.jornada(), delta.cargo(),
                    anterior == null ? nome(delta.lotacao()) : anterior.lotacaoNome(),
                    anterior == null ? descricao(delta.jornada()) : anterior.jornadaDescricao(),
                    anterior == null ? cargoNome(delta.cargo()) : anterior.cargoNome(), quantidade,
                    anterior == null ? proposta.dataEfetiva() : anterior.dataInicio(),
                    anterior == null ? fimContratual.data() : anterior.dataFim(),
                    anterior == null ? delta.lotacao().getMunicipio() : anterior.lotacaoMunicipio(),
                    anterior == null ? com.example.siggtrat.utilitario.FotografiaJornada.capturar(delta.jornada())
                            : anterior.jornadaSemanal(),
                    anterior == null ? null : anterior.aditamentoComposicaoOrigemId(),
                    anterior == null || anterior.municipioComprovado(),
                    anterior == null ? Set.of() : anterior.mapeamentosOrigemIds()));
            soma += delta.variacao();
        }
        if (proposta.tipo() == TipoEventoCicloVidaContrato.REMANEJAMENTO && soma != 0)
            throw new IllegalStateException("Remanejamento proposto deve preservar o total contratual.");
        totalRepresentavel(estado.values());
    }

    public List<ComposicaoContratualDTO> projetarDTO(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        return projetarEntidades(contrato, data, conhecidoAte).stream().map(p -> paraDTO(contrato,p)).toList();
    }

    public List<ComposicaoContratualLeituraDTO> projetarLeituraDTO(Contrato contrato,LocalDate dataReferencia,Instant conhecidoAte) {
        LocalDate data=dataReferencia==null?RelogioSistema.hoje():dataReferencia;
        Instant conhecido=conhecidoAte==null?Instant.now():conhecidoAte;
        return projetarEntidades(contrato,data,conhecido).stream().map(p->{
            var campos=ProvasCamposHistoricos.desconhecidos(valores(p).keySet());campos.putAll(p.provaCampos());
            return new ComposicaoContratualLeituraDTO(paraDTO(contrato,p),p.lotacaoMunicipio(),p.jornadaSemanal(),data,conhecido,campos);
        }).toList();
    }

    private ComposicaoContratualDTO paraDTO(Contrato contrato,ComposicaoProjetada p) {
        return new ComposicaoContratualDTO(
                p.composicaoBaseId(), contrato.getId(),
                p.lotacao() != null ? p.lotacao().getId() : null,
                p.lotacaoNome(),
                p.jornada() != null ? p.jornada().getId() : null,
                p.jornadaDescricao(),
                p.cargo() != null ? p.cargo().getId() : null,
                p.cargoNome(),
                p.efetivoPrevisto(), p.dataInicio(), p.dataFim(),
                p.efetivoPrevisto() > 0,
                0L
        );
    }

    public int efetivoTotal(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        return totalRepresentavel(projetarEntidades(contrato, dataReferencia, conhecidoAte));
    }

    /** O contrato continua int32; nenhuma soma pode ser truncada antes da validação do replay. */
    static int totalRepresentavel(Collection<ComposicaoProjetada> composicoes) {
        long total = 0;
        for (ComposicaoProjetada composicao : composicoes) {
            if (composicao.efetivoPrevisto() < 0)
                throw new IllegalStateException("Composição reconstruída com quantidade negativa.");
            total += composicao.efetivoPrevisto();
            if (total > Integer.MAX_VALUE)
                // Não converter em pendência de replay: omitir o ato produziria outra composição.
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O efetivo contratual reconstruído não pode ser representado integralmente: "
                        + total + ". Trate os instrumentos quantitativos incompatíveis.");
        }
        return (int) total;
    }

    public SortedSet<LocalDate> datasMudancaCapacidade(Contrato contrato, LocalDate ate) {
        SortedSet<LocalDate> datas = new TreeSet<>();
        if (contrato == null || contrato.getId() == null) return datas;
        if (contrato.getDataInicio() != null) datas.add(contrato.getDataInicio());
        composicaoRepositorio.findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(contrato.getId())
                .forEach(c -> { if (c.getDataInicioVigencia() != null) datas.add(c.getDataInicioVigencia()); });
        aditamentoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .forEach(a -> { if (a.getDataEfetiva() != null) datas.add(a.getDataEfetiva()); });
        remanejamentoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .forEach(r -> { if (r.getDataEfetiva() != null) datas.add(r.getDataEfetiva()); });
        if (transferenciaGestaoRepositorio != null)
            transferenciaGestaoRepositorio.findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                    .forEach(t -> { if (t.getDataEfetiva() != null) datas.add(t.getDataEfetiva()); });
        if (ate != null) datas.removeIf(d -> d.isAfter(ate));
        return datas;
    }

    public Optional<ComposicaoProjetada> localizar(
            List<ComposicaoProjetada> estado,
            Long composicaoId,
            Long lotacaoId,
            Long jornadaId,
            Long cargoId) {
        if (estado == null) return Optional.empty();
        if (composicaoId != null) {
            Optional<ComposicaoProjetada> porId = estado.stream()
                    .filter(p -> Objects.equals(p.composicaoBaseId(), composicaoId)).findFirst();
            if (porId.isPresent()) return porId;
        }
        if (lotacaoId == null || jornadaId == null || cargoId == null) return Optional.empty();
        String chave = ComposicaoProjetada.chave(lotacaoId, jornadaId, cargoId);
        return estado.stream().filter(p -> p.chave().equals(chave)).findFirst();
    }

    private void aplicarAditamento(Map<String, ComposicaoProjetada> estado, AditamentoContratual a,
                                   int efetivoOriginal, Instant conhecimento, FimContratual fimContratual,
                                   LeituraHistoricaContratos entradas) {
        int totalAntes = totalRepresentavel(estado.values());
        for (AditamentoComposicao item : a.getAlteracoesComposicao()) {
            String chave = ComposicaoProjetada.chave(item.getLotacao().getId(), item.getJornada().getId(), item.getCargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            int quantidadeAntes = anterior != null ? anterior.efetivoPrevisto() : 0;
            long quantidadeCalculada = (long) quantidadeAntes + item.getVariacao();
            if (quantidadeCalculada < 0) {
                throw new IllegalStateException(
                        "Aditamento incompatível com a linha temporal reconstruída na composição " + chave + ".");
            }
            if (quantidadeCalculada > Integer.MAX_VALUE)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Aditamento incompatível com a linha temporal reconstruída na composição "
                        + chave + ": quantidade não representável integralmente.");
            int quantidadeDepois = (int) quantidadeCalculada;
            LocalDate inicio = anterior != null ? anterior.dataInicio() : a.getDataEfetiva();
            LocalDate fim = anterior != null ? anterior.dataFim() : fimContratual.data();
            var configuracao = anterior == null && provasHistoricas != null
                    ? (entradas==null?provasHistoricas.configuracao(item, conhecimento):provasHistoricas.configuracao(item, conhecimento,entradas)) : null;
            Map<String,ProvaCampoHistoricoDTO> campos=anterior==null
                    ?new LinkedHashMap<>():new LinkedHashMap<>(anterior.provaCampos());
            var fonte=ProvasCamposHistoricos.item(item);
            if(anterior==null) {
                if(configuracao!=null)campos.putAll(configuracao.provaCampos());
                campos.put("lotacaoId",ProvasCamposHistoricos.comprovado(item.getLotacao().getId(),fonte));
                campos.put("jornadaId",ProvasCamposHistoricos.comprovado(item.getJornada().getId(),fonte));
                campos.put("cargoId",ProvasCamposHistoricos.comprovado(item.getCargo().getId(),fonte));
                campos.put("dataInicioVigencia",ProvasCamposHistoricos.comprovado(inicio,fonte));
                // Combinação nova herda o limite comprovado no seu ponto de entrada.
                // A vigência operacional posterior continua sendo regida pelo contrato.
                campos.put("dataFimVigencia",fimContratual.prova());
            }
            var delta=ProvasCamposHistoricos.comprovado(item.getVariacao(),fonte);
            campos.put("efetivoPrevisto",anterior==null?ProvasCamposHistoricos.derivado(quantidadeDepois,delta)
                    :ProvasCamposHistoricos.derivado(quantidadeDepois,anterior.provaCampos().get("efetivoPrevisto"),delta));
            campos.put("permiteNovaAlocacao",ProvasCamposHistoricos.derivado(quantidadeDepois>0,campos.get("efetivoPrevisto")));
            estado.put(chave, new ComposicaoProjetada(
                    anterior != null ? anterior.composicaoBaseId()
                            : item.getComposicaoBase() != null ? item.getComposicaoBase().getId() : null,
                    item.getLotacao(), item.getJornada(), item.getCargo(),
                    anterior != null ? anterior.lotacaoNome() : configuracao != null ? configuracao.lotacaoNome()
                            : nomeFotografado(item.getLotacaoNomeFotografia()),
                    anterior != null ? anterior.jornadaDescricao() : configuracao != null ? configuracao.jornadaDescricao()
                            : nomeFotografado(item.getJornadaDescricaoFotografia()),
                    anterior != null ? anterior.cargoNome() : configuracao != null ? configuracao.cargoNome()
                            : nomeFotografado(item.getCargoNomeFotografia()),
                    quantidadeDepois, inicio, fim,
                    // O delta de combinação existente é somente quantitativo,
                    // inclusive para nomes ausentes. O contexto fotografado no
                    // registro do item não constitui uma renomeação retroativa.
                    // Combinação nova introduz sua própria fotografia documental.
                    anterior != null ? anterior.lotacaoMunicipio() : configuracao != null ? configuracao.lotacaoMunicipio()
                            : item.getLotacaoMunicipioFotografia(),
                    anterior != null ? anterior.jornadaSemanal() : configuracao != null ? configuracao.jornadaSemanal()
                            : item.getJornadaSemanalFotografia(),
                    anterior == null ? item.getId() : anterior.aditamentoComposicaoOrigemId(),
                    anterior != null ? anterior.municipioComprovado() : configuracao != null
                            ? configuracao.municipioComprovado()
                            : item.getLotacaoMunicipioFotografia()!=null||nomeFotografado(item.getJornadaSemanalFotografia())!=null,
                    anterior == null ? Set.of() : anterior.mapeamentosOrigemIds()).comProvas(campos));
        }
        int totalDepois = totalRepresentavel(estado.values());
        int piso = RegraEfetivoContratual.pisoNormal(efetivoOriginal);
        if (totalDepois < totalAntes && totalDepois < piso && !possuiExcecaoOriginalValidada(a, piso)) {
            throw new IllegalStateException("Aditamento incompatível com o piso de redução do efetivo original: "
                    + totalDepois + " resultantes, piso " + piso + ". A redução exige exceção própria documentada.");
        }
    }

    private boolean possuiExcecaoOriginalValidada(AditamentoContratual a, int piso) {
        // A autorização foi exigida no registro quando a redução original ultrapassou o piso.
        // Marcar a flag em um ato normal não pode criar uma autorização posterior por efeito do replay.
        return a.getNovoEfetivoTotal() != null && a.getNovoEfetivoTotal() < piso
                && Boolean.TRUE.equals(a.getReducaoExcepcional())
                && Boolean.TRUE.equals(a.getImpactoConfirmado())
                && a.getFundamentoExcecao() != null && !a.getFundamentoExcecao().isBlank();
    }

    private void aplicarRemanejamento(Map<String, ComposicaoProjetada> estado, RemanejamentoContratual r) {
        int totalAntes = totalRepresentavel(estado.values());
        long somaVariacoesAplicadas = 0;
        for (RemanejamentoComposicao item : r.getItens()) {
            String chave = ComposicaoProjetada.chave(item.getLotacao().getId(), item.getJornada().getId(), item.getCargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            if (anterior == null) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: composição inexistente " + chave + ".");
            }
            long quantidadeCalculada = (long) anterior.efetivoPrevisto() + item.getVariacao();
            if (quantidadeCalculada < 0) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: quantidade negativa em " + chave + ".");
            }
            if (quantidadeCalculada > Integer.MAX_VALUE)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Remanejamento incompatível com a linha temporal reconstruída: "
                        + "quantidade não representável integralmente em " + chave + ".");
            int quantidadeDepois = (int) quantidadeCalculada;
            somaVariacoesAplicadas += item.getVariacao();
            var campos=new LinkedHashMap<>(anterior.provaCampos());
            campos.put("efetivoPrevisto",ProvasCamposHistoricos.derivado(quantidadeDepois,
                    campos.get("efetivoPrevisto"),ProvasCamposHistoricos.comprovado(item.getVariacao(),ProvasCamposHistoricos.evento(r.getEvento()))));
            campos.put("permiteNovaAlocacao",ProvasCamposHistoricos.derivado(quantidadeDepois>0,campos.get("efetivoPrevisto")));
            estado.put(chave, new ComposicaoProjetada(
                    anterior.composicaoBaseId(), anterior.lotacao(), anterior.jornada(), anterior.cargo(),
                    anterior.lotacaoNome(), anterior.jornadaDescricao(), anterior.cargoNome(),
                    quantidadeDepois, anterior.dataInicio(), anterior.dataFim(),
                    // REM redistribui efetivo; a foto do item é prova do contexto
                    // conhecido no registro, não um delta de jornada/município.
                    anterior.lotacaoMunicipio(), anterior.jornadaSemanal(), anterior.aditamentoComposicaoOrigemId(),
                    anterior.municipioComprovado(), anterior.mapeamentosOrigemIds()).comProvas(campos));
        }
        int totalDepois = totalRepresentavel(estado.values());
        if (somaVariacoesAplicadas != 0 || totalDepois != totalAntes) {
            throw new IllegalStateException(
                    "Remanejamento incompatível com a linha temporal reconstruída: o total contratual deve ser preservado.");
        }
    }

    /**
     * A transferência não reescreve a fotografia anterior. Ela troca apenas o
     * recorte projetado depois da data efetiva, usando mapas explicitamente
     * aprovados pelo Gestor Local responsável.
     */
    private void aplicarTransferenciaGestao(Map<String, ComposicaoProjetada> estado,
                                            TransferenciaGestaoContrato transferencia, Instant conhecimento,
                                            LeituraHistoricaContratos entradas) {
        Map<String, MapeamentoTransferenciaGestao> mapas = transferencia.getMapeamentos().stream()
                .collect(Collectors.toMap(
                        m -> ComposicaoProjetada.chave(m.getLotacaoOrigem().getId(), m.getJornadaOrigem().getId(), m.getCargoOrigem().getId()),
                        Function.identity(), (a, b) -> a, LinkedHashMap::new));
        LinkedHashMap<String, ComposicaoProjetada> destino = new LinkedHashMap<>();
        for (ComposicaoProjetada atual : estado.values()) {
            MapeamentoTransferenciaGestao mapa = mapas.get(atual.chave());
            if (mapa == null) {
                if (atual.efetivoPrevisto() > 0) {
                    throw new IllegalStateException("Transferência de Gestão sem mapeamento para composição operacional "
                            + atual.chave() + ".");
                }
                destino.put(atual.chave(), atual);
                continue;
            }
            ComposicaoProjetada convertido = new ComposicaoProjetada(
                    atual.composicaoBaseId(), mapa.getLotacaoDestino(), mapa.getJornadaDestino(), mapa.getCargoDestino(),
                    nomeFotografado(mapa.getLotacaoDestinoNomeFotografia()),
                    nomeFotografado(mapa.getJornadaDestinoDescricaoFotografia()),
                    nomeFotografado(mapa.getCargoDestinoNomeFotografia()),
                    atual.efetivoPrevisto(), atual.dataInicio(), atual.dataFim(),
                    mapa.getLotacaoDestinoMunicipioFotografia(), mapa.getJornadaDestinoSemanalFotografia(),
                    null, mapa.getLotacaoDestinoMunicipioFotografia() != null
                            || nomeFotografado(mapa.getJornadaDestinoSemanalFotografia()) != null,
                    mapa.getId() == null ? Set.of() : Set.of(mapa.getId()));
            if(provasHistoricas!=null) {
                var config=entradas==null?provasHistoricas.configuracao(mapa,conhecimento):provasHistoricas.configuracao(mapa,conhecimento,entradas);
                var campos=new LinkedHashMap<>(atual.provaCampos());
                campos.putAll(config.provaCampos());
                var fonte=ProvasCamposHistoricos.mapa(mapa);
                campos.put("lotacaoId",ProvasCamposHistoricos.comprovado(mapa.getLotacaoDestino().getId(),fonte));
                campos.put("jornadaId",ProvasCamposHistoricos.comprovado(mapa.getJornadaDestino().getId(),fonte));
                campos.put("cargoId",ProvasCamposHistoricos.comprovado(mapa.getCargoDestino().getId(),fonte));
                convertido=new ComposicaoProjetada(convertido.composicaoBaseId(),convertido.lotacao(),convertido.jornada(),
                        convertido.cargo(),config.lotacaoNome(),config.jornadaDescricao(),config.cargoNome(),
                        convertido.efetivoPrevisto(),convertido.dataInicio(),convertido.dataFim(),
                        config.lotacaoMunicipio(),config.jornadaSemanal(),null,config.municipioComprovado(),
                        convertido.mapeamentosOrigemIds()).comProvas(campos);
            }
            ComposicaoProjetada jaExiste = destino.get(convertido.chave());
            if (jaExiste == null) {
                destino.put(convertido.chave(), convertido);
            } else {
                // A configuração agregada conserva todas as origens aplicadas.
                // O conjunto é novo: o estado anterior também é usado no rollback do replay.
                Set<Long> origens = new LinkedHashSet<>(jaExiste.mapeamentosOrigemIds());
                origens.addAll(convertido.mapeamentosOrigemIds());
                destino.put(convertido.chave(), new ComposicaoProjetada(
                        jaExiste.composicaoBaseId(), jaExiste.lotacao(), jaExiste.jornada(), jaExiste.cargo(),
                        // Nenhum rótulo de uma origem isolada certifica o agregado.
                        Objects.equals(jaExiste.lotacaoNome(), convertido.lotacaoNome()) ? jaExiste.lotacaoNome() : null,
                        Objects.equals(jaExiste.jornadaDescricao(), convertido.jornadaDescricao()) ? jaExiste.jornadaDescricao() : null,
                        Objects.equals(jaExiste.cargoNome(), convertido.cargoNome()) ? jaExiste.cargoNome() : null,
                        jaExiste.efetivoPrevisto() + convertido.efetivoPrevisto(),
                        jaExiste.dataInicio(), jaExiste.dataFim(),
                        // Mapas legados divergentes não comprovam uma única
                        // configuração do destino agregado. Conserva a lacuna.
                        Objects.equals(jaExiste.lotacaoMunicipio(), convertido.lotacaoMunicipio())
                                ? jaExiste.lotacaoMunicipio() : null,
                        Objects.equals(jaExiste.jornadaSemanal(), convertido.jornadaSemanal())
                                ? jaExiste.jornadaSemanal() : null,
                        null,jaExiste.municipioComprovado()&&convertido.municipioComprovado()
                                &&Objects.equals(jaExiste.lotacaoMunicipio(),convertido.lotacaoMunicipio()), origens)
                        .comProvas(convergirCampos(jaExiste,convertido)));
            }
        }
        estado.clear();
        estado.putAll(destino);
    }

    private Map<String,ProvaCampoHistoricoDTO> convergirCampos(ComposicaoProjetada a,ComposicaoProjetada b) {
        var va=valores(a);var vb=valores(b);
        var campos=ProvasCamposHistoricos.desconhecidos(va.keySet());
        for(String campo:va.keySet()) if(Objects.equals(va.get(campo),vb.get(campo)))
            campos.put(campo,ProvasCamposHistoricos.derivado(va.get(campo),a.provaCampos().get(campo),b.provaCampos().get(campo)));
        int quantidade=a.efetivoPrevisto()+b.efetivoPrevisto();
        campos.put("efetivoPrevisto",ProvasCamposHistoricos.derivado(quantidade,a.provaCampos().get("efetivoPrevisto"),b.provaCampos().get("efetivoPrevisto")));
        campos.put("permiteNovaAlocacao",ProvasCamposHistoricos.derivado(quantidade>0,campos.get("efetivoPrevisto")));
        return campos;
    }
    private Map<String,Object> valores(ComposicaoProjetada p) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("lotacaoId",p.lotacao()==null?null:p.lotacao().getId());m.put("lotacaoNome",p.lotacaoNome());
        m.put("jornadaId",p.jornada()==null?null:p.jornada().getId());m.put("jornadaDescricao",p.jornadaDescricao());
        m.put("cargoId",p.cargo()==null?null:p.cargo().getId());m.put("cargoNome",p.cargoNome());
        m.put("efetivoPrevisto",p.efetivoPrevisto());m.put("dataInicioVigencia",p.dataInicio());m.put("dataFimVigencia",p.dataFim());
        m.put("permiteNovaAlocacao",p.efetivoPrevisto()>0);m.put("lotacaoMunicipio",p.lotacaoMunicipio());m.put("jornadaSemanal",p.jornadaSemanal());
        return m;
    }

    private static String nome(Lotacao l) { return l == null || l.getNome() == null ? "" : l.getNome(); }
    private static String descricao(Jornada j) { return j == null || j.getDescricao() == null ? "" : j.getDescricao(); }
    private static String cargoNome(Cargo c) { return c == null || c.getNome() == null ? "" : c.getNome(); }
    private static String nomeFotografado(String fotografia) {
        // O cadastro mestre atual não comprova um rótulo que faltou no fato histórico.
        return fotografia == null || fotografia.isBlank() ? null : fotografia;
    }
}
