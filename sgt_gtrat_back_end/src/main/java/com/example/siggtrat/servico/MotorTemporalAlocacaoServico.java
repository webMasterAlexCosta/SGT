package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoAlocacaoVinculoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Reconstrói a posição de Alocação por data efetiva e data de conhecimento.
 * A projeção usa as fotografias dos eventos; a tabela de faixas é apenas a
 * projeção atual usada para listagens e ocupação.
 */
@Service
public class MotorTemporalAlocacaoServico {
    public record FaixaProjetada(
            Long id,
            Long composicaoBaseId,
            Long lotacaoId,
            String lotacaoNome,
            Long jornadaId,
            String jornadaDescricao,
            Long cargoId,
            String cargoNome,
            LocalDate dataInicio,
            LocalDate dataFim,
            EstadoNegocialAlocacao estado
    ) {
        boolean contem(LocalDate data) {
            return estado == EstadoNegocialAlocacao.REGISTRADA && data != null
                    && !data.isBefore(dataInicio) && (dataFim == null || !data.isAfter(dataFim));
        }
    }

    private final EventoAlocacaoVinculoRepositorio eventoRepositorio;
    private final AlocacaoVinculoRepositorio alocacaoRepositorio;

    public MotorTemporalAlocacaoServico(EventoAlocacaoVinculoRepositorio eventoRepositorio,
                                        AlocacaoVinculoRepositorio alocacaoRepositorio) {
        this.eventoRepositorio = eventoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
    }

    public PosicaoAlocacaoTemporalDTO reconstruir(VinculoContratual vinculo, LocalDate dataReferencia,
                                                   Instant conhecidoAte) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<FaixaProjetada> faixas = reconstruirFaixas(vinculo, conhecimento);
        Long ultimoEventoId = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento)
                .stream().map(EventoAlocacaoVinculo::getId).filter(Objects::nonNull).reduce((a, b) -> b).orElse(null);

        if (vinculo.getEstadoNegocial() != null && vinculo.getEstadoNegocial()
                != com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO) {
            return semFaixa(vinculo, data, conhecimento, SituacaoTemporalAlocacao.CANCELADA, ultimoEventoId);
        }

        List<FaixaProjetada> encontradas = faixas.stream().filter(f -> f.contem(data)).toList();
        if (encontradas.size() > 1) {
            throw new IllegalStateException("Mais de uma alocação efetiva foi reconstruída para o vínculo "
                    + vinculo.getId() + " em " + data + ".");
        }
        if (encontradas.size() == 1) {
            FaixaProjetada f = encontradas.get(0);
            return new PosicaoAlocacaoTemporalDTO(vinculo.getId(), data, conhecimento, SituacaoTemporalAlocacao.VIGENTE,
                    f.id(), f.composicaoBaseId(), f.lotacaoId(), f.lotacaoNome(), f.jornadaId(), f.jornadaDescricao(),
                    f.cargoId(), f.cargoNome(), f.dataInicio(), f.dataFim(), ultimoEventoId);
        }

        boolean programada = faixas.stream().filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA)
                .map(FaixaProjetada::dataInicio).filter(Objects::nonNull).anyMatch(i -> data.isBefore(i));
        return semFaixa(vinculo, data, conhecimento,
                programada ? SituacaoTemporalAlocacao.PROGRAMADA : SituacaoTemporalAlocacao.SEM_ALOCACAO,
                ultimoEventoId);
    }

    public List<FaixaProjetada> reconstruirFaixas(VinculoContratual vinculo, Instant conhecidoAte) {
        if (vinculo == null || vinculo.getId() == null) return List.of();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoAlocacaoVinculo> eventos = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento);
        if (eventos.isEmpty()) {
            return alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculo.getId()).stream()
                    .map(this::daEntidade).sorted(Comparator.comparing(FaixaProjetada::dataInicio).thenComparing(FaixaProjetada::id,
                            Comparator.nullsLast(Long::compareTo))).toList();
        }

        Map<Long, FaixaProjetada> estado = new LinkedHashMap<>();
        for (EventoAlocacaoVinculo e : eventos) {
            if (e.getAlocacaoAnteriorId() != null && e.getEstadoAnterior() != null) {
                estado.put(e.getAlocacaoAnteriorId(), new FaixaProjetada(
                        e.getAlocacaoAnteriorId(), e.getComposicaoAnteriorId(), e.getLotacaoAnteriorId(), e.getLotacaoAnteriorNome(),
                        e.getJornadaAnteriorId(), e.getJornadaAnteriorDescricao(), e.getCargoAnteriorId(), e.getCargoAnteriorNome(),
                        e.getInicioAnterior(), e.getFimAnterior(), e.getEstadoAnterior()));
            }
            if (e.getAlocacaoNova() != null && e.getAlocacaoNova().getId() != null && e.getEstadoNovo() != null) {
                AlocacaoVinculo nova = e.getAlocacaoNova();
                estado.put(nova.getId(), new FaixaProjetada(
                        nova.getId(), e.getComposicaoNovaId(), e.getLotacaoNovaId(), e.getLotacaoNovaNome(),
                        e.getJornadaNovaId(), e.getJornadaNovaDescricao(), e.getCargoNovoId(), e.getCargoNovoNome(),
                        e.getInicioNovo(), e.getFimNovo(), e.getEstadoNovo()));
            }
        }
        return estado.values().stream().sorted(Comparator.comparing(FaixaProjetada::dataInicio,
                        Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(FaixaProjetada::id, Comparator.nullsLast(Long::compareTo))).toList();
    }

    public boolean cadeiaHashIntegra(Long vinculoId) {
        String anterior = null;
        for (EventoAlocacaoVinculo e : eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId)) {
            if (!Objects.equals(anterior, e.getHashAnterior()) || e.getHashEvento() == null || e.getHashEvento().length() != 64) {
                return false;
            }
            anterior = e.getHashEvento();
        }
        return true;
    }

    private PosicaoAlocacaoTemporalDTO semFaixa(VinculoContratual vinculo, LocalDate data, Instant conhecimento,
                                                 SituacaoTemporalAlocacao situacao, Long ultimoEventoId) {
        return new PosicaoAlocacaoTemporalDTO(vinculo.getId(), data, conhecimento, situacao,
                null, null, null, null, null, null, null, null, null, null, ultimoEventoId);
    }

    private FaixaProjetada daEntidade(AlocacaoVinculo a) {
        return new FaixaProjetada(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), nome(a.getLotacao()), a.getJornada().getId(), descricao(a.getJornada()),
                a.getCargo().getId(), nome(a.getCargo()), a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial());
    }

    private static String nome(Lotacao valor) { return valor == null ? null : valor.getNome(); }
    private static String nome(Cargo valor) { return valor == null ? null : valor.getNome(); }
    private static String descricao(Jornada valor) { return valor == null ? null : valor.getDescricao(); }
}
