package com.example.siggtrat.servico;

import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/** Verifica os pontos de mudança futuros, sem percorrer artificialmente cada dia. */
@Service
@Transactional(readOnly = true)
public class ImpactoProgramacaoComposicaoServico {
    public record Alteracao(Long composicaoId, Long lotacaoId, Long jornadaId, Long cargoId, int variacao) {
        String chave() { return ComposicaoContratualTemporalServico.ComposicaoProjetada.chave(lotacaoId, jornadaId, cargoId); }
    }
    private final EntityManager em;
    private final ComposicaoContratualTemporalServico temporal;

    public ImpactoProgramacaoComposicaoServico(EntityManager em, ComposicaoContratualTemporalServico temporal) {
        this.em = em; this.temporal = temporal;
    }

    public record ContextoProposta(TipoEventoCicloVidaContrato tipo, String prefixoChaveNegocial,
                                   Integer ordemDocumental, Long eventoPrecedenteId,
                                   TipoRelacaoPrecedenciaNegocial relacaoPrecedencia) {}

    private static final int TAMANHO_PAGINA = 200;
    private static final int LIMITE_DETALHES = 20;
    private static final String ALOCACOES_REGISTRADAS = """
            from AlocacaoVinculo a join a.vinculo v
            where v.contrato.id=:contrato
              and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
              and v.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
            """;
    private static final String OCUPACAO_EM = ALOCACOES_REGISTRADAS + """
              and a.dataInicio<=:data and (a.dataFim is null or a.dataFim>=:data)
              and v.dataInicio<=:data and (v.dataFim is null or v.dataFim>=:data)
              and a.lotacao.id=:lotacao and a.jornada.id=:jornada and a.cargo.id=:cargo
            """;

    public List<String> diagnosticar(Contrato contrato, LocalDate desde, List<Alteracao> alteracoes,
                                    Set<Long> ignorados, ContextoProposta contexto) {
        Set<Long> retirados = ignorados == null ? Set.of() : ignorados;
        if (retirados.isEmpty() && alteracoes.isEmpty()) return List.of();
        LocalDate inicioAnalise = desde;
        if (!retirados.isEmpty()) {
            LocalDate efeitoAnterior = em.createQuery("""
                    select min(e.dataEfetiva) from EventoCicloVidaContrato e
                    where e.contrato.id=:contrato and e.id in :eventos
                    """, LocalDate.class).setParameter("contrato", contrato.getId())
                    .setParameter("eventos", retirados).getSingleResult();
            if (efeitoAnterior != null && efeitoAnterior.isBefore(inicioAnalise)) inicioAnalise = efeitoAnterior;
        }
        int versao = 1;
        if (!retirados.isEmpty() && contexto.eventoPrecedenteId() != null) {
            EventoCicloVidaContrato precedente = em.find(EventoCicloVidaContrato.class, contexto.eventoPrecedenteId());
            if (precedente != null && Objects.equals(precedente.getContrato().getId(), contrato.getId()))
                versao = precedente.getVersaoNegocial() + 1;
        }
        var proposta = new ComposicaoContratualTemporalServico.PropostaComposicao(desde, contexto.tipo(),
                contexto.prefixoChaveNegocial() + ":V" + versao, contexto.ordemDocumental(), contexto.eventoPrecedenteId(),
                contexto.relacaoPrecedencia(), alteracoes.stream().map(a ->
                new ComposicaoContratualTemporalServico.DeltaProposto(a.composicaoId(),
                        em.getReference(Lotacao.class, a.lotacaoId()), em.getReference(Jornada.class, a.jornadaId()),
                        em.getReference(Cargo.class, a.cargoId()), a.variacao())).toList());
        Instant conhecido = Instant.now();
        return diagnosticarCapacidades(contrato, inicioAnalise, Set.of(desde), conhecido,
                data -> temporal.simularEntidades(contrato, data, conhecido, retirados, proposta));
    }

    /** O cancelamento retira o fato e reaplica os posteriores antes de validar dependentes. */
    public List<String> diagnosticarCancelamento(Contrato contrato, LocalDate desde, Long eventoRetirado) {
        Instant conhecido = Instant.now();
        return diagnosticarCapacidades(contrato, desde, Set.of(), conhecido,
                data -> temporal.projetarEntidades(contrato, data, conhecido, Set.of(eventoRetirado)));
    }

    private List<String> diagnosticarCapacidades(Contrato contrato, LocalDate desde, Set<LocalDate> adicionais,
                                                Instant conhecido,
                                                Function<LocalDate, List<ComposicaoContratualTemporalServico.ComposicaoProjetada>> resultante) {
        SortedSet<LocalDate> datasContrato = new TreeSet<>(temporal.datasMudancaCapacidade(contrato, null));
        datasContrato.add(desde); datasContrato.addAll(adicionais); datasContrato.headSet(desde).clear();
        Iterator<LocalDate> datas = datasContrato.iterator();
        LocalDate dataContrato = datas.hasNext() ? datas.next() : null;
        CursorDatas alocacoes = new CursorDatas(contrato.getId(), desde, "a.dataInicio");
        CursorDatas vinculos = new CursorDatas(contrato.getId(), desde, "v.dataInicio");
        List<String> detalhes = new ArrayList<>();
        long conflitos = 0; int pessoasExibidas = 0;
        while (dataContrato != null || alocacoes.proxima() != null || vinculos.proxima() != null) {
            LocalDate data = menor(dataContrato, alocacoes.proxima(), vinculos.proxima());
            Map<String, Integer> depois = new HashMap<>();
            resultante.apply(data).forEach(p -> depois.put(p.chave(), p.efetivoPrevisto()));
            for (var anterior : temporal.projetarEntidades(contrato, data, conhecido)) {
                int capacidade = depois.getOrDefault(anterior.chave(), 0);
                if (capacidade >= anterior.efetivoPrevisto()) continue;
                long ocupantes = recorte(em.createQuery("select count(a.id) " + OCUPACAO_EM, Long.class),
                        contrato, data, anterior).getSingleResult();
                if (ocupantes <= Math.max(0, capacidade)) continue;
                conflitos++;
                if (detalhes.size() >= LIMITE_DETALHES) continue;
                int disponiveis = LIMITE_DETALHES - pessoasExibidas;
                List<Object[]> amostra = disponiveis == 0 ? List.of()
                        : recorte(em.createQuery("select v.id,v.pessoa.nome " + OCUPACAO_EM + " order by v.id,a.id", Object[].class),
                        contrato, data, anterior).setMaxResults(disponiveis).getResultList();
                pessoasExibidas += amostra.size();
                String pessoas = amostra.stream().map(a -> a[1] + " (vínculo " + a[0] + ")")
                        .collect(java.util.stream.Collectors.joining(", "));
                detalhes.add("Em " + data + ", a composição " + anterior.chave() + " teria capacidade " + capacidade
                        + " para " + ocupantes + " alocação(ões). Pessoas exibidas em amostra (limite total de 20): "
                        + (pessoas.isEmpty() ? "limite da amostra atingido" : pessoas)
                        + ". Trate as programações antes da confirmação.");
            }
            if (Objects.equals(data, dataContrato)) dataContrato = datas.hasNext() ? datas.next() : null;
            if (Objects.equals(data, alocacoes.proxima())) alocacoes.avancar();
            if (Objects.equals(data, vinculos.proxima())) vinculos.avancar();
        }
        if (conflitos > LIMITE_DETALHES) detalhes.add(conflitos
                + " conflitos de composição/data identificados; detalhes em amostra dos primeiros 20.");
        return List.copyOf(detalhes);
    }

    private <T> TypedQuery<T> recorte(TypedQuery<T> query, Contrato contrato, LocalDate data,
                                     ComposicaoContratualTemporalServico.ComposicaoProjetada composicao) {
        return query.setParameter("contrato", contrato.getId()).setParameter("data", data)
                .setParameter("lotacao", composicao.lotacao().getId()).setParameter("jornada", composicao.jornada().getId())
                .setParameter("cargo", composicao.cargo().getId());
    }

    private LocalDate menor(LocalDate... datas) {
        LocalDate menor = null;
        for (LocalDate data : datas) if (data != null && (menor == null || data.isBefore(menor))) menor = data;
        return menor;
    }

    /** Datas escalares em páginas por chave, sem carregar vínculos/alocações/pessoas completos. */
    private final class CursorDatas {
        private final Long contratoId;
        private final LocalDate desde;
        private final String campo;
        private LocalDate apos;
        private List<LocalDate> pagina = List.of();
        private int indice;
        private boolean fim;

        private CursorDatas(Long contratoId, LocalDate desde, String campo) {
            this.contratoId = contratoId; this.desde = desde; this.apos = desde; this.campo = campo;
        }
        private LocalDate proxima() {
            if (indice >= pagina.size() && !fim) {
                pagina = em.createQuery("select distinct " + campo + " " + ALOCACOES_REGISTRADAS
                                + " and (a.dataFim is null or a.dataFim>=:desde) and (v.dataFim is null or v.dataFim>=:desde)"
                                + " and " + campo + ">:apos order by " + campo, LocalDate.class)
                        .setParameter("contrato", contratoId).setParameter("desde", desde).setParameter("apos", apos)
                        .setMaxResults(TAMANHO_PAGINA).getResultList();
                indice = 0; fim = pagina.size() < TAMANHO_PAGINA;
            }
            return indice < pagina.size() ? pagina.get(indice) : null;
        }
        private void avancar() { LocalDate data = proxima(); if (data != null) { apos = data; indice++; } }
    }

    /** Verifica cobertura do vínculo inteiro, incluindo o dia seguinte aos fins que desapareceriam. */
    public List<String> diagnosticarVigenciaCancelamento(
            Contrato contrato, LocalDate desde,
            Function<LocalDate, VersaoContratoNegocialDTO> projetarAntes,
            Function<LocalDate, VersaoContratoNegocialDTO> projetarDepois) {
        return diagnosticarVigencia(contrato, desde, Set.of(), projetarAntes, projetarDepois);
    }

    public List<String> diagnosticarVigencia(
            Contrato contrato, LocalDate desde, Collection<LocalDate> fronteirasPropostas,
            Function<LocalDate, VersaoContratoNegocialDTO> projetarAntes,
            Function<LocalDate, VersaoContratoNegocialDTO> projetarDepois) {
        Map<LocalDate, VersaoContratoNegocialDTO> antes = new HashMap<>(), depois = new HashMap<>();
        Function<LocalDate, VersaoContratoNegocialDTO> anterior = d -> antes.computeIfAbsent(d, projetarAntes);
        Function<LocalDate, VersaoContratoNegocialDTO> resultante = d -> depois.computeIfAbsent(d, projetarDepois);
        SortedSet<LocalDate> datas = new TreeSet<>(temporal.datasMudancaCapacidade(contrato, null));
        datas.add(desde);
        datas.addAll(fronteirasPropostas);
        for (LocalDate data : List.copyOf(datas)) {
            for (VersaoContratoNegocialDTO versao : List.of(anterior.apply(data), resultante.apply(data))) {
                if (versao.dataInicio() != null) datas.add(versao.dataInicio());
                if (versao.dataFim() != null && versao.dataFim().isBefore(LocalDate.MAX)) datas.add(versao.dataFim().plusDays(1));
            }
        }
        datas.removeIf(d -> d.isBefore(desde));
        List<String> detalhes = new ArrayList<>();
        long quantidadeConflitos = 0, ultimoId = 0;
        while (true) {
            List<Object[]> pagina = em.createQuery("""
                    select v.id,v.pessoa.nome,v.dataInicio,v.dataFim from VinculoContratual v
                    where v.contrato.id=:contrato and v.id>:apos
                      and v.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                      and (v.dataFim is null or v.dataFim>=:desde)
                    order by v.id
                    """, Object[].class).setParameter("contrato", contrato.getId()).setParameter("apos", ultimoId)
                    .setParameter("desde", desde).setMaxResults(200).getResultList();
            if (pagina.isEmpty()) break;
            for (Object[] v : pagina) {
                ultimoId = (Long) v[0]; LocalDate inicio = (LocalDate) v[2], fim = (LocalDate) v[3];
                SortedSet<LocalDate> pontos = new TreeSet<>(datas); pontos.add(inicio.isBefore(desde) ? desde : inicio);
                for (LocalDate data : pontos) {
                    if (data.isBefore(inicio) || fim != null && data.isAfter(fim)) continue;
                    if (coberto(anterior.apply(data), data) && !coberto(resultante.apply(data), data)) {
                        quantidadeConflitos++;
                        if (detalhes.size() < 20) detalhes.add("Vínculo " + v[0] + " (" + v[1]
                                + ") perderia cobertura contratual em " + data + ".");
                        break;
                    }
                }
            }
            if (pagina.size() < 200) break;
        }
        if (quantidadeConflitos > 0) detalhes.add(quantidadeConflitos
                + " vínculo(s) perderiam vigência; detalhes limitados a 20. Trate explicitamente os dependentes antes de concluir o fato.");
        return List.copyOf(detalhes);
    }

    private boolean coberto(VersaoContratoNegocialDTO versao, LocalDate data) {
        return (versao.dataInicio() == null || !data.isBefore(versao.dataInicio()))
                && (versao.dataFim() == null || !data.isAfter(versao.dataFim()));
    }
}
