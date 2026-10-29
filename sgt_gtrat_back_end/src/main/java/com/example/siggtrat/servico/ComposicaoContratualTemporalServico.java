package com.example.siggtrat.servico;

import com.example.siggtrat.dto.ComposicaoContratualDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AditamentoContratualRepositorio;
import com.example.siggtrat.repositorio.ComposicaoContratualRepositorio;
import com.example.siggtrat.repositorio.RemanejamentoContratualRepositorio;
import com.example.siggtrat.repositorio.TransferenciaGestaoContratoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ComposicaoContratualTemporalServico {

    public record ConflitoReplay(Long eventoId, TipoEventoCicloVidaContrato tipoEvento,
                                 LocalDate dataEfetiva, String motivo) {}

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
            LocalDate dataFim
    ) {
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
        return projetarEntidadesInterno(contrato, dataReferencia, conhecidoAte, eventosIgnorados, null);
    }

    @Transactional(readOnly = true)
    public List<ConflitoReplay> diagnosticarConflitos(Contrato contrato, LocalDate dataReferencia,
                                                      Instant conhecidoAte) {
        List<ConflitoReplay> conflitos = new ArrayList<>();
        projetarEntidadesInterno(contrato, dataReferencia, conhecidoAte, Set.of(), conflitos);
        return List.copyOf(conflitos);
    }

    private List<ComposicaoProjetada> projetarEntidadesInterno(
            Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte,
            Set<Long> eventosIgnorados, List<ConflitoReplay> conflitos) {
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
                    valorOuAtual(base.getLotacaoNomeFotografia(), nome(base.getLotacao())),
                    valorOuAtual(base.getJornadaDescricaoFotografia(), descricao(base.getJornada())),
                    valorOuAtual(base.getCargoNomeFotografia(), cargoNome(base.getCargo())),
                    Optional.ofNullable(base.getEfetivoPrevisto()).orElse(0),
                    base.getDataInicioVigencia(), base.getDataFimVigencia());
            estado.put(p.chave(), p);
        }

        Map<Long, AditamentoContratual> aditamentos = aditamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .stream().filter(a -> a.getEvento() != null && a.getEvento().getId() != null)
                .collect(Collectors.toMap(a -> a.getEvento().getId(), Function.identity(), (a, b) -> b, LinkedHashMap::new));

        Map<Long, RemanejamentoContratual> remanejamentos = remanejamentoRepositorio
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId())
                .stream().filter(r -> r.getEvento() != null && r.getEvento().getId() != null)
                .collect(Collectors.toMap(r -> r.getEvento().getId(), Function.identity(), (a, b) -> b, LinkedHashMap::new));

        Set<Long> ignorar = eventosIgnorados == null ? Set.of() : eventosIgnorados;
        for (EventoCicloVidaContrato evento : motorTemporal.eventosValidosOrdenados(contrato.getId(), data, conhecimento)) {
            if (evento.getId() == null || ignorar.contains(evento.getId())) continue;
            LinkedHashMap<String, ComposicaoProjetada> antes = new LinkedHashMap<>(estado);
            try {
                if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.ADITAMENTO) {
                    AditamentoContratual a = aditamentos.get(evento.getId());
                    if (a != null) aplicarAditamento(estado, a);
                } else if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.REMANEJAMENTO) {
                    RemanejamentoContratual r = remanejamentos.get(evento.getId());
                    if (r != null) aplicarRemanejamento(estado, r);
                } else if (evento.getTipoEvento() == TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL
                        && transferenciaGestaoRepositorio != null) {
                    transferenciaGestaoRepositorio.findByEventoId(evento.getId())
                            .ifPresent(t -> aplicarTransferenciaGestao(estado, t));
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

    public List<ComposicaoContratualDTO> projetarDTO(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        return projetarEntidades(contrato, data, conhecidoAte).stream().map(p -> new ComposicaoContratualDTO(
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
        )).toList();
    }

    public int efetivoTotal(Contrato contrato, LocalDate dataReferencia, Instant conhecidoAte) {
        return projetarEntidades(contrato, dataReferencia, conhecidoAte).stream()
                .mapToInt(ComposicaoProjetada::efetivoPrevisto).sum();
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

    private void aplicarAditamento(Map<String, ComposicaoProjetada> estado, AditamentoContratual a) {
        for (AditamentoComposicao item : a.getAlteracoesComposicao()) {
            String chave = ComposicaoProjetada.chave(item.getLotacao().getId(), item.getJornada().getId(), item.getCargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            int quantidadeAntes = anterior != null ? anterior.efetivoPrevisto() : 0;
            int quantidadeDepois = quantidadeAntes + item.getVariacao();
            if (quantidadeDepois < 0) {
                throw new IllegalStateException(
                        "Aditamento incompatível com a linha temporal reconstruída na composição " + chave + ".");
            }
            LocalDate inicio = anterior != null ? anterior.dataInicio() : a.getDataEfetiva();
            LocalDate fim = anterior != null ? anterior.dataFim() : a.getContrato().getDataFim();
            estado.put(chave, new ComposicaoProjetada(
                    item.getComposicaoBase() != null ? item.getComposicaoBase().getId() : null,
                    item.getLotacao(), item.getJornada(), item.getCargo(),
                    valorOuAtual(item.getLotacaoNomeFotografia(), nome(item.getLotacao())),
                    valorOuAtual(item.getJornadaDescricaoFotografia(), descricao(item.getJornada())),
                    valorOuAtual(item.getCargoNomeFotografia(), cargoNome(item.getCargo())),
                    quantidadeDepois, inicio, fim));
        }
    }

    private void aplicarRemanejamento(Map<String, ComposicaoProjetada> estado, RemanejamentoContratual r) {
        int totalAntes = estado.values().stream().mapToInt(ComposicaoProjetada::efetivoPrevisto).sum();
        int somaVariacoesAplicadas = 0;
        for (RemanejamentoComposicao item : r.getItens()) {
            String chave = ComposicaoProjetada.chave(item.getLotacao().getId(), item.getJornada().getId(), item.getCargo().getId());
            ComposicaoProjetada anterior = estado.get(chave);
            if (anterior == null) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: composição inexistente " + chave + ".");
            }
            int quantidadeDepois = anterior.efetivoPrevisto() + item.getVariacao();
            if (quantidadeDepois < 0) {
                throw new IllegalStateException(
                        "Remanejamento incompatível com a linha temporal reconstruída: quantidade negativa em " + chave + ".");
            }
            somaVariacoesAplicadas += item.getVariacao();
            estado.put(chave, new ComposicaoProjetada(
                    anterior.composicaoBaseId(), anterior.lotacao(), anterior.jornada(), anterior.cargo(),
                    valorOuAtual(item.getLotacaoNomeFotografia(), anterior.lotacaoNome()),
                    valorOuAtual(item.getJornadaDescricaoFotografia(), anterior.jornadaDescricao()),
                    valorOuAtual(item.getCargoNomeFotografia(), anterior.cargoNome()),
                    quantidadeDepois, anterior.dataInicio(), anterior.dataFim()));
        }
        int totalDepois = estado.values().stream().mapToInt(ComposicaoProjetada::efetivoPrevisto).sum();
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
                                            TransferenciaGestaoContrato transferencia) {
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
                    valorOuAtual(mapa.getLotacaoDestinoNomeFotografia(), nome(mapa.getLotacaoDestino())),
                    valorOuAtual(mapa.getJornadaDestinoDescricaoFotografia(), descricao(mapa.getJornadaDestino())),
                    valorOuAtual(mapa.getCargoDestinoNomeFotografia(), cargoNome(mapa.getCargoDestino())),
                    atual.efetivoPrevisto(), atual.dataInicio(), atual.dataFim());
            ComposicaoProjetada jaExiste = destino.get(convertido.chave());
            if (jaExiste == null) {
                destino.put(convertido.chave(), convertido);
            } else {
                destino.put(convertido.chave(), new ComposicaoProjetada(
                        jaExiste.composicaoBaseId(), jaExiste.lotacao(), jaExiste.jornada(), jaExiste.cargo(),
                        jaExiste.lotacaoNome(), jaExiste.jornadaDescricao(), jaExiste.cargoNome(),
                        jaExiste.efetivoPrevisto() + convertido.efetivoPrevisto(),
                        jaExiste.dataInicio(), jaExiste.dataFim()));
            }
        }
        estado.clear();
        estado.putAll(destino);
    }

    private static String nome(Lotacao l) { return l == null || l.getNome() == null ? "" : l.getNome(); }
    private static String descricao(Jornada j) { return j == null || j.getDescricao() == null ? "" : j.getDescricao(); }
    private static String cargoNome(Cargo c) { return c == null || c.getNome() == null ? "" : c.getNome(); }
    private static String valorOuAtual(String fotografia, String atual) {
        return fotografia == null ? atual : fotografia;
    }
}
