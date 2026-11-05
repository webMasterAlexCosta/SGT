package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.*;
import com.example.siggtrat.dto.PosicaoAlocacaoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.regra.AssinaturaPreviaNegocial;
import com.example.siggtrat.repositorio.AlocacaoVinculoRepositorio;
import com.example.siggtrat.repositorio.EventoAlocacaoVinculoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Reconstrói a posição de Alocação por data efetiva e data de conhecimento.
 * A projeção usa as fotografias dos eventos; a tabela de faixas é apenas a
 * projeção atual usada para listagens e ocupação.
 */
@Service
@Transactional(readOnly = true)
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
    private MotorTemporalVinculoServico motorVinculo;

    public MotorTemporalAlocacaoServico(EventoAlocacaoVinculoRepositorio eventoRepositorio,
                                        AlocacaoVinculoRepositorio alocacaoRepositorio) {
        this.eventoRepositorio = eventoRepositorio;
        this.alocacaoRepositorio = alocacaoRepositorio;
    }

    @Autowired
    public void configurarMotorVinculo(MotorTemporalVinculoServico motorVinculo) {
        this.motorVinculo = motorVinculo;
    }

    public PosicaoAlocacaoTemporalDTO reconstruir(VinculoContratual vinculo, LocalDate dataReferencia,
                                                   Instant conhecidoAte) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<FaixaProjetada> faixas = reconstruirFaixas(vinculo, conhecimento);
        Long ultimoEventoId = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento)
                .stream().map(EventoAlocacaoVinculo::getId).filter(Objects::nonNull).reduce((a, b) -> b).orElse(null);

        var estadoVinculo = motorVinculo == null ? vinculo.getEstadoNegocial()
                : motorVinculo.reconstruir(vinculo, data, conhecimento).estadoNegocial();
        if (estadoVinculo != null && estadoVinculo
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
            // Existirem eventos somente depois do corte significa que o sistema
            // ainda não conhecia a faixa. A tabela atual não pode preencher esse vazio.
            if (!eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).isEmpty()) {
                return List.of();
            }
            return alocacaoRepositorio.findByVinculoIdOrderByDataInicioAscIdAsc(vinculo.getId()).stream()
                    .filter(a -> a.getCriadoEm() != null && !a.getCriadoEm().isAfter(conhecimento))
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

    /**
     * Reconstitui as decisões de alocação antes do recorte pelos limites do vínculo.
     * Os ajustes derivados de início/fim não cancelam a decisão original. Uma
     * retificação posterior pode voltar a tornar aquele trecho válido. Somente
     * criação, mudança, retificação e cancelamento explícito mudam esta linha.
     */
    public List<FaixaProjetada> reconstruirFaixasNegociais(VinculoContratual vinculo, Instant conhecidoAte) {
        if (vinculo == null || vinculo.getId() == null) return List.of();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoAlocacaoVinculo> eventos = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento);
        Map<Long, FaixaProjetada> estado = new LinkedHashMap<>();
        for (EventoAlocacaoVinculo e : eventos) {
            if (e.getTipoEvento() == null || e.getTipoEvento() == TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO
                    || e.getAlocacaoNova() == null || e.getEstadoNovo() == null) continue;
            Long id = e.getAlocacaoNova().getId();
            FaixaProjetada nova = new FaixaProjetada(id, e.getComposicaoNovaId(), e.getLotacaoNovaId(), e.getLotacaoNovaNome(),
                    e.getJornadaNovaId(), e.getJornadaNovaDescricao(), e.getCargoNovoId(), e.getCargoNovoNome(),
                    e.getInicioNovo(), e.getFimNovo(), e.getEstadoNovo());
            if (e.getTipoEvento() == TipoEventoAlocacaoVinculo.CRIACAO
                    || e.getTipoEvento() == TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA) {
                estado.put(id, nova);
                continue;
            }
            if (e.getTipoEvento() == TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO) {
                FaixaProjetada cancelada = estado.getOrDefault(id, nova);
                estado.values().stream().filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA
                                && f.dataInicio().isBefore(cancelada.dataInicio()))
                        .max(Comparator.comparing(FaixaProjetada::dataInicio)).ifPresent(anterior ->
                                estado.put(anterior.id(), comLimites(anterior, anterior.dataInicio(),
                                        cancelada.dataFim(), EstadoNegocialAlocacao.REGISTRADA)));
                estado.put(id, comLimites(cancelada, cancelada.dataInicio(), cancelada.dataFim(), EstadoNegocialAlocacao.CANCELADA));
                continue;
            }
            FaixaProjetada anterior = estado.get(e.getAlocacaoAnteriorId());
            if (anterior == null && e.getAlocacaoAnteriorId() != null && e.getEstadoAnterior() != null) {
                anterior = new FaixaProjetada(e.getAlocacaoAnteriorId(), e.getComposicaoAnteriorId(),
                        e.getLotacaoAnteriorId(), e.getLotacaoAnteriorNome(), e.getJornadaAnteriorId(),
                        e.getJornadaAnteriorDescricao(), e.getCargoAnteriorId(), e.getCargoAnteriorNome(),
                        e.getInicioAnterior(), e.getFimAnterior(), e.getEstadoAnterior());
            }
            if (anterior != null) {
                // A data da decisão divide a faixa original, inclusive quando a
                // projeção visível estava temporariamente cortada pelo vínculo.
                LocalDate fimAnterior = anterior.dataFim();
                boolean substituiDesdeOrigem = !nova.dataInicio().isAfter(anterior.dataInicio());
                estado.put(anterior.id(), comLimites(anterior, anterior.dataInicio(),
                        substituiDesdeOrigem ? fimAnterior : nova.dataInicio().minusDays(1),
                        substituiDesdeOrigem ? EstadoNegocialAlocacao.SUBSTITUIDA : EstadoNegocialAlocacao.REGISTRADA));
                nova = comLimites(nova, nova.dataInicio(), fimAnterior, EstadoNegocialAlocacao.REGISTRADA);
            }
            estado.put(id, nova);
        }
        if (estado.isEmpty()) return reconstruirFaixas(vinculo, conhecimento);
        return estado.values().stream().sorted(Comparator.comparing(FaixaProjetada::dataInicio)
                .thenComparing(FaixaProjetada::id, Comparator.nullsLast(Long::compareTo))).toList();
    }

    private FaixaProjetada comLimites(FaixaProjetada faixa, LocalDate inicio, LocalDate fim,
                                      EstadoNegocialAlocacao estado) {
        return new FaixaProjetada(faixa.id(), faixa.composicaoBaseId(), faixa.lotacaoId(), faixa.lotacaoNome(),
                faixa.jornadaId(), faixa.jornadaDescricao(), faixa.cargoId(), faixa.cargoNome(), inicio, fim, estado);
    }

    /**
     * Linha das decisões humanas sem os recortes derivados das transferências.
     * Uma escolha posterior no destino conserva sua identidade e só será aceita
     * se continuar compatível com a cadeia contratual corrigida.
     */
    public List<FaixaProjetada> reconstruirDecisoesIndependentes(VinculoContratual vinculo, Instant conhecidoAte) {
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoAlocacaoVinculo> eventos = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento);
        if (eventos.isEmpty()) return reconstruirFaixas(vinculo, conhecimento);
        Set<Long> derivadas = new HashSet<>();
        for (EventoAlocacaoVinculo e : eventos) {
            if (e.getAlocacaoNova() != null && (e.getTipoEvento() == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO
                    || e.getTipoEvento() == TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA
                    && e.getTransferenciaGestaoId() != null
                    // Novos reprocessamentos preservam a origem. O formato
                    // legado sem foto anterior continua sendo uma derivação.
                    && (e.getAlocacaoAnteriorId() == null
                        || !Objects.equals(e.getAlocacaoAnteriorId(), e.getAlocacaoNova().getId()))))
                derivadas.add(e.getAlocacaoNova().getId());
        }
        Map<Long, FaixaProjetada> estado = new LinkedHashMap<>();
        for (EventoAlocacaoVinculo e : eventos) {
            if (e.getTipoEvento() == TipoEventoAlocacaoVinculo.TRANSFERENCIA_GESTAO) {
                // Uma origem importada sem CRIACAO só pode ser recuperada da
                // fotografia anterior preservada, nunca do mestre atual.
                if (e.getAlocacaoAnteriorId() != null && !derivadas.contains(e.getAlocacaoAnteriorId())
                        && !estado.containsKey(e.getAlocacaoAnteriorId()) && e.getInicioAnterior() != null)
                    estado.put(e.getAlocacaoAnteriorId(), anterior(e));
                continue;
            }
            if (e.getTipoEvento() == null || e.getTipoEvento() == TipoEventoAlocacaoVinculo.AJUSTE_LIMITE_VINCULO
                    || e.getTipoEvento() == TipoEventoAlocacaoVinculo.REPROCESSAMENTO_TRANSFERENCIA
                    || e.getAlocacaoNova() == null || e.getEstadoNovo() == null) continue;
            Long id = e.getAlocacaoNova().getId();
            FaixaProjetada nova = nova(e);
            if (e.getTipoEvento() == TipoEventoAlocacaoVinculo.CRIACAO) {
                estado.put(id, nova); continue;
            }
            if (e.getTipoEvento() == TipoEventoAlocacaoVinculo.CANCELAMENTO_PROGRAMACAO) {
                FaixaProjetada cancelada = estado.getOrDefault(id, nova);
                estado.values().stream().filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA
                                && f.dataInicio().isBefore(cancelada.dataInicio()))
                        .max(Comparator.comparing(FaixaProjetada::dataInicio)).ifPresent(f ->
                                estado.put(f.id(), comLimites(f, f.dataInicio(), cancelada.dataFim(), EstadoNegocialAlocacao.REGISTRADA)));
                estado.put(id, comLimites(cancelada, cancelada.dataInicio(), cancelada.dataFim(), EstadoNegocialAlocacao.CANCELADA));
                continue;
            }
            FaixaProjetada anterior = estado.get(e.getAlocacaoAnteriorId());
            if (anterior == null) {
                LocalDate inicioNova = nova.dataInicio();
                anterior = estado.values().stream().filter(f -> f.contem(inicioNova)).findFirst().orElse(null);
            }
            if (anterior == null && e.getAlocacaoAnteriorId() != null
                    && !derivadas.contains(e.getAlocacaoAnteriorId()) && e.getInicioAnterior() != null)
                anterior = anterior(e);
            if (anterior != null) {
                LocalDate fim = anterior.dataFim();
                boolean substitui = !nova.dataInicio().isAfter(anterior.dataInicio());
                estado.put(anterior.id(), comLimites(anterior, anterior.dataInicio(),
                        substitui ? fim : nova.dataInicio().minusDays(1),
                        substitui ? EstadoNegocialAlocacao.SUBSTITUIDA : EstadoNegocialAlocacao.REGISTRADA));
                nova = comLimites(nova, nova.dataInicio(), fim, EstadoNegocialAlocacao.REGISTRADA);
            }
            estado.put(id, nova);
        }
        return estado.values().stream().sorted(Comparator.comparing(FaixaProjetada::dataInicio)
                .thenComparing(FaixaProjetada::id)).toList();
    }

    private FaixaProjetada anterior(EventoAlocacaoVinculo e) {
        return new FaixaProjetada(e.getAlocacaoAnteriorId(), e.getComposicaoAnteriorId(),
                e.getLotacaoAnteriorId(), e.getLotacaoAnteriorNome(), e.getJornadaAnteriorId(),
                e.getJornadaAnteriorDescricao(), e.getCargoAnteriorId(), e.getCargoAnteriorNome(),
                e.getInicioAnterior(), e.getFimAnterior(), e.getEstadoAnterior());
    }

    private FaixaProjetada nova(EventoAlocacaoVinculo e) {
        return new FaixaProjetada(e.getAlocacaoNova().getId(), e.getComposicaoNovaId(),
                e.getLotacaoNovaId(), e.getLotacaoNovaNome(), e.getJornadaNovaId(), e.getJornadaNovaDescricao(),
                e.getCargoNovoId(), e.getCargoNovoNome(), e.getInicioNovo(), e.getFimNovo(), e.getEstadoNovo());
    }

    public boolean cadeiaHashIntegra(Long vinculoId) {
        String anterior = null;
        for (EventoAlocacaoVinculo e : eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId)) {
            if (e == null || !Objects.equals(anterior, e.getHashAnterior()) || e.getHashEvento() == null
                    || !e.getHashEvento().equals(hashEsperado(e))) {
                return false;
            }
            anterior = e.getHashEvento();
        }
        return true;
    }

    private String hashEsperado(EventoAlocacaoVinculo e) {
        if (e.getTipoEvento() == null || e.getDataEfetiva() == null) return null;
        // IDs e valores fotografados são os mesmos usados pelo gerador; não usar a projeção atual.
        String fotoAnterior = e.getAlocacaoAnteriorId() == null ? "" : textoHashFotografia(anterior(e));
        String fotoNova = e.getAlocacaoNova() == null ? "" : textoHashFotografia(nova(e));
        String hash = UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|",
                AssinaturaPreviaNegocial.valor(e.getHashAnterior()), e.getTipoEvento().name(),
                AssinaturaPreviaNegocial.valor(e.getDataEfetiva()), fotoAnterior, fotoNova,
                AssinaturaPreviaNegocial.valor(e.getResponsavelId()), AssinaturaPreviaNegocial.valor(e.getJustificativa()),
                AssinaturaPreviaNegocial.valor(e.getTransferenciaGestaoId())));
        if (e.isInclusaoExcedente()) {
            hash = UtilitarioHashSeguranca.sha256Hexadecimal(String.join("|", hash, "EXCEDENTE",
                    String.valueOf(e.getEfetivoPrevistoExcedente()), String.valueOf(e.getOcupacaoAposInclusaoExcedente()),
                    AssinaturaPreviaNegocial.valor(e.getEvidencia()), AssinaturaPreviaNegocial.valor(e.getAutorizacao())));
        }
        return hash;
    }

    private String textoHashFotografia(FaixaProjetada f) {
        return String.join(":", AssinaturaPreviaNegocial.valor(f.id()), AssinaturaPreviaNegocial.valor(f.composicaoBaseId()),
                AssinaturaPreviaNegocial.valor(f.lotacaoId()), AssinaturaPreviaNegocial.valor(f.jornadaId()),
                AssinaturaPreviaNegocial.valor(f.cargoId()), AssinaturaPreviaNegocial.valor(f.dataInicio()),
                AssinaturaPreviaNegocial.valor(f.dataFim()), AssinaturaPreviaNegocial.valor(f.estado()));
    }

    private PosicaoAlocacaoTemporalDTO semFaixa(VinculoContratual vinculo, LocalDate data, Instant conhecimento,
                                                 SituacaoTemporalAlocacao situacao, Long ultimoEventoId) {
        return new PosicaoAlocacaoTemporalDTO(vinculo.getId(), data, conhecimento, situacao,
                null, null, null, null, null, null, null, null, null, null, ultimoEventoId);
    }

    private FaixaProjetada daEntidade(AlocacaoVinculo a) {
        return new FaixaProjetada(a.getId(), a.getComposicao() == null ? null : a.getComposicao().getId(),
                a.getLotacao().getId(), rotuloFotografado(a.getLotacaoNomeFotografia()),
                a.getJornada().getId(), rotuloFotografado(a.getJornadaDescricaoFotografia()),
                a.getCargo().getId(), rotuloFotografado(a.getCargoNomeFotografia()),
                a.getDataInicio(), a.getDataFim(), a.getEstadoNegocial());
    }

    private static String rotuloFotografado(String valor) { return valor == null || valor.isBlank() ? null : valor; }
}
