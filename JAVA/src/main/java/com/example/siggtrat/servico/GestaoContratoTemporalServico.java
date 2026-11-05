package com.example.siggtrat.servico;

import com.example.siggtrat.dto.VersaoContratoNegocialDTO;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.entidade.EstadoNegocialContrato;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.TipoEventoCicloVidaContrato;
import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.TransferenciaGestaoContratoRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Responsabilidade organizacional reconstruída sem depender da atualização da coluna materializada. */
@Service
@Transactional(readOnly = true)
public class GestaoContratoTemporalServico {
    private final MotorTemporalContratoServico motor;
    private final TransferenciaGestaoContratoRepositorio transferencias;
    private final GestaoLocalRepositorio gestoes;

    public GestaoContratoTemporalServico(MotorTemporalContratoServico motor,
                                         TransferenciaGestaoContratoRepositorio transferencias,
                                         GestaoLocalRepositorio gestoes) {
        this.motor = motor;
        this.transferencias = transferencias;
        this.gestoes = gestoes;
    }

    /** Usar para autorização e para o contexto corrente de consultas/comandos. */
    public GestaoLocal gestaoAtual(Contrato contrato) {
        return gestaoEm(contrato, RelogioSistema.hoje(), Instant.now());
    }
    public GestaoLocal gestaoAtual(Contrato contrato, LeituraHistoricaContratos entradas) {
        return gestaoEm(contrato, RelogioSistema.hoje(), Instant.now(), entradas);
    }

    /** Responsabilidades que intersectam o período, sem reduzir sua história ao primeiro dia. */
    public List<GestaoLocal> gestoesNoIntervalo(Contrato contrato, LocalDate inicio, LocalDate fim,
                                                Instant conhecidoAte) {
        if (contrato == null || inicio == null || fim == null || inicio.isAfter(fim)) return List.of();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        Map<Long, GestaoLocal> gestoesDoPeriodo = new LinkedHashMap<>();
        for (LocalDate data : datasDeMudancaNoIntervalo(contrato, inicio, fim, conhecimento)) {
            GestaoLocal gestao = gestaoEm(contrato, data, conhecimento);
            if (gestao != null && gestao.getId() != null) gestoesDoPeriodo.put(gestao.getId(), gestao);
        }
        return List.copyOf(gestoesDoPeriodo.values());
    }

    /** Fronteiras negociais para combinar responsabilidade e recortes na mesma data. */
    public List<LocalDate> datasDeMudancaNoIntervalo(Contrato contrato, LocalDate inicio, LocalDate fim,
                                                   Instant conhecidoAte) {
        if (contrato == null || inicio == null || fim == null || inicio.isAfter(fim)) return List.of();
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        var datas = new TreeSet<LocalDate>();
        datas.add(inicio);
        if (contrato.getId() != null && contrato.getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO) {
            // Fotografias legadas também delimitam a responsabilidade; o replay
            // canônico de cada fronteira continua sendo o de gestaoEm.
            motor.listarVersoes(contrato.getId()).stream()
                    .filter(v -> v.registradoEm() != null && !v.registradoEm().isAfter(conhecimento))
                    .map(VersaoContratoNegocialDTO::dataEfetiva)
                    .filter(Objects::nonNull)
                    .filter(d -> !d.isBefore(inicio) && !d.isAfter(fim)).forEach(datas::add);
            motor.eventosValidosOrdenados(contrato.getId(), fim, conhecimento).stream()
                    .map(e -> e.getDataEfetiva()).filter(Objects::nonNull)
                    .filter(d -> !d.isBefore(inicio) && !d.isAfter(fim)).forEach(datas::add);
        }
        return List.copyOf(datas);
    }

    /** Usar para catálogos/fotografias na data do fato; não concede acesso ao gestor histórico. */
    public GestaoLocal gestaoEm(Contrato contrato, LocalDate dataEfetiva, Instant conhecidoAte) {
        return gestaoEm(contrato,dataEfetiva,conhecidoAte,null);
    }
    public GestaoLocal gestaoEm(Contrato contrato,LocalDate dataEfetiva,Instant conhecidoAte,
                                LeituraHistoricaContratos entradas) {
        if (contrato == null) return null;
        if (contrato.getId() == null || contrato.getEstadoNegocial() == EstadoNegocialContrato.RASCUNHO
                && contrato.getPublicadoEm() == null)
            return contrato.getGestaoLocal();
        LocalDate data = dataEfetiva == null ? RelogioSistema.hoje() : dataEfetiva;
        Instant conhecido = conhecidoAte == null ? Instant.now() : conhecidoAte;
        var fotografias = (entradas==null?motor.listarVersoes(contrato.getId()):motor.listarVersoes(contrato.getId(),entradas)).stream()
                .filter(v -> v.registradoEm() != null && !v.registradoEm().isAfter(conhecido)).toList();
        var eventos = entradas==null?motor.eventosValidosOrdenados(contrato.getId(), data, conhecido)
                :motor.eventosValidosOrdenados(contrato.getId(), data, conhecido,entradas);
        // Somente a publicação original comprovada pode descrever também um
        // contrato FUTURO antes de sua vigência. Uma fotografia de outro ato
        // conhecido não comprova o período anterior à sua própria data.
        var original = (entradas==null?motor.fotografiaOriginalConhecida(contrato, conhecido)
                :motor.fotografiaOriginalConhecida(contrato, conhecido,entradas)).orElse(null);
        Long gestaoId = original == null ? null : original.getGestaoLocalId();
        // O acervo anterior à cadeia canônica pode registrar responsabilidade
        // apenas em fotografias datadas, sem eventoOrigemId. Sua base histórica
        // não pode ser substituída pela Gestão materializada de hoje. Novas
        // transferências canônicas continuam sendo reaplicadas abaixo.
        var legadasAplicaveis = fotografias.stream().filter(v -> v.eventoOrigemId() == null)
                .filter(v -> v.dataEfetiva() != null && !v.dataEfetiva().isAfter(data)).toList();
        LocalDate dataBaseLegada = legadasAplicaveis.stream().map(VersaoContratoNegocialDTO::dataEfetiva)
                .max(Comparator.naturalOrder()).orElse(null);
        if (dataBaseLegada != null) {
            var gestoesDaBase = legadasAplicaveis.stream()
                    .filter(v -> v.dataEfetiva().equals(dataBaseLegada))
                    .map(VersaoContratoNegocialDTO::gestaoLocalId).distinct().toList();
            if (gestoesDaBase.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fotografias legadas na mesma data não comprovam uma única Gestão responsável.");
            gestaoId = gestoesDaBase.get(0);
        }
        Map<Long, VersaoContratoNegocialDTO> fotoPorEvento = entradas==null?motor.fotografiasConhecidasPorEvento(fotografias, conhecido)
                :motor.fotografiasConhecidasPorEvento(fotografias, conhecido,entradas);
        if (gestaoId == null && dataBaseLegada == null) {
            // Na ausência da publicação, uma fonte posterior válida comprova
            // somente a responsabilidade do seu período. A ordem é a cadeia
            // negocial já reconstruída, não ID, versão técnica ou timestamp.
            for (var evento : eventos) {
                var foto = fotoPorEvento.get(evento.getId());
                if (foto != null && foto.dataEfetiva() != null && !foto.dataEfetiva().isAfter(data)
                        && foto.gestaoLocalId() != null) {
                    gestaoId = foto.gestaoLocalId();
                    break;
                }
            }
        }
        Map<Long, TransferenciaGestaoContrato> porEvento = transferencias
                .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                .filter(t -> t.getEvento() != null && t.getEvento().getId() != null)
                .filter(t -> t.getRegistradoEm() != null && !t.getRegistradoEm().isAfter(conhecido))
                .collect(Collectors.toMap(t -> t.getEvento().getId(), Function.identity(), (a, b) -> b));

        // Uma fotografia integral de ADI registrado antes da transferência pode conter gestão antiga.
        // Somente o delta de responsabilidade altera este campo durante o replay.
        for (var evento : eventos) {
            if (evento.getTipoEvento() != TipoEventoCicloVidaContrato.TRANSFERENCIA_GESTAO_LOCAL) continue;
            // Uma fonte legada explicitamente posterior possui seu próprio
            // alcance; um ato anterior não reescreve aquela fotografia datada.
            if (dataBaseLegada != null && evento.getDataEfetiva() != null
                    && evento.getDataEfetiva().isBefore(dataBaseLegada)) continue;
            TransferenciaGestaoContrato transferencia = porEvento.get(evento.getId());
            if (transferencia != null && transferencia.getGestaoDestino() != null) {
                gestaoId = transferencia.getGestaoDestino().getId();
            } else {
                var fotografia = fotoPorEvento.get(evento.getId());
                if (fotografia != null) gestaoId = fotografia.gestaoLocalId();
            }
        }
        if (gestaoId == null) return null;
        if (contrato.getGestaoLocal() != null && Objects.equals(gestaoId, contrato.getGestaoLocal().getId()))
            return contrato.getGestaoLocal();
        return gestoes.findById(gestaoId).orElse(null);
    }
}
