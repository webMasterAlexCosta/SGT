package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.entidade.ProvaHistoricaContrato;
import com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato;
import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.utilitario.PrecisaoTemporal;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entradas brutas de uma única leitura. Não guarda projeções nem resolve validade,
 * precedência ou provas: cada consumidor continua aplicando os próprios D e K.
 * A instância é criada depois da seleção de contratos e nunca fica em um serviço.
 */
public final class LeituraHistoricaContratos {
    private final Set<Long> contratos;
    private final Map<Long, List<EventoCicloVidaContrato>> eventos;
    private final Map<Long, List<RelacaoPrecedenciaEventoContrato>> relacoes;
    private final Map<Long, List<VersaoContratoNegocial>> versoes;
    private final Map<Long, List<ProvaHistoricaContrato>> provasPorContrato;
    private final Map<Long, List<ProvaHistoricaContrato>> provasPorEvento;
    private final Map<Long, VersaoContratoNegocial> versoesPorId;
    private final Map<Long, ProvaHistoricaContrato> provasPorVersao;
    private final Set<Long> eventosCarregados;

    LeituraHistoricaContratos(Collection<Long> contratos, List<EventoCicloVidaContrato> eventos,
                              List<RelacaoPrecedenciaEventoContrato> relacoes,
                              List<VersaoContratoNegocial> versoes, List<ProvaHistoricaContrato> provas) {
        this.contratos = Set.copyOf(contratos);
        this.eventos = agrupar(eventos, e -> e.getContrato().getId());
        this.relacoes = agrupar(relacoes, r -> r.getContrato().getId());
        this.versoes = agrupar(versoes, v -> v.getContrato().getId());
        this.provasPorContrato = agrupar(provas, p -> p.getContrato().getId());
        this.provasPorEvento = agrupar(provas, p -> p.getEventoOrigem().getId());
        this.versoesPorId = versoes.stream().collect(Collectors.toUnmodifiableMap(
                VersaoContratoNegocial::getId, Function.identity()));
        Map<Long, ProvaHistoricaContrato> porVersao = new HashMap<>();
        for (var prova : provas) porVersao.putIfAbsent(prova.getVersaoGerada().getId(), prova);
        this.provasPorVersao = Map.copyOf(porVersao);
        Set<Long> idsEventos = eventos.stream().map(EventoCicloVidaContrato::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        versoes.stream().map(VersaoContratoNegocial::getEventoOrigem).filter(Objects::nonNull)
                .map(EventoCicloVidaContrato::getId).filter(Objects::nonNull).forEach(idsEventos::add);
        idsEventos.addAll(this.provasPorEvento.keySet());
        this.eventosCarregados = Set.copyOf(idsEventos);
    }

    boolean contemContrato(Long contrato) { return contratos.contains(contrato); }
    boolean contemEvento(Long evento) { return eventosCarregados.contains(evento); }
    boolean contemVersao(Long versao) { return versoesPorId.containsKey(versao); }

    List<EventoCicloVidaContrato> eventos(Long contrato) {
        return eventos.getOrDefault(contrato, List.of());
    }

    List<RelacaoPrecedenciaEventoContrato> relacoes(Long contrato, Instant conhecido) {
        var entradas = relacoes.getOrDefault(contrato, List.of());
        return conhecido == null ? entradas : conhecidas(entradas, conhecido,
                RelacaoPrecedenciaEventoContrato::getRegistradoEm);
    }

    List<VersaoContratoNegocial> versoes(Long contrato) {
        return versoes.getOrDefault(contrato, List.of());
    }

    List<VersaoContratoNegocial> versoesConhecidas(Long contrato, Instant conhecido) {
        return conhecidas(versoes(contrato), conhecido, VersaoContratoNegocial::getRegistradoEm);
    }

    Optional<VersaoContratoNegocial> versao(Long id) { return Optional.ofNullable(versoesPorId.get(id)); }
    Optional<ProvaHistoricaContrato> provaDaVersao(Long id) { return Optional.ofNullable(provasPorVersao.get(id)); }

    List<ProvaHistoricaContrato> provasDoContrato(Long contrato, Instant conhecido) {
        return conhecidas(provasPorContrato.getOrDefault(contrato, List.of()), conhecido,
                ProvaHistoricaContrato::getRegistradoEm);
    }

    List<ProvaHistoricaContrato> provasDoEvento(Long evento, Instant conhecido) {
        return conhecidas(provasPorEvento.getOrDefault(evento, List.of()), conhecido,
                ProvaHistoricaContrato::getRegistradoEm);
    }

    private static <T> List<T> conhecidas(List<T> entradas, Instant conhecido, Function<T, Instant> registro) {
        Instant limite = PrecisaoTemporal.limiteInclusivoMicros(conhecido);
        return entradas.stream().filter(v -> registro.apply(v) != null
                && !registro.apply(v).isAfter(limite)).toList();
    }

    private static <T> Map<Long, List<T>> agrupar(List<T> entradas, Function<T, Long> chave) {
        Map<Long, List<T>> grupos = entradas.stream().collect(Collectors.groupingBy(chave));
        grupos.replaceAll((id, itens) -> List.copyOf(itens));
        return Map.copyOf(grupos);
    }
}
