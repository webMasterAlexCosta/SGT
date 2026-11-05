package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import com.example.siggtrat.dto.ContextoComplementoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.*;
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
 * Reconstitui o escopo de uma prova já existente, mesmo sem cobertura operacional.
 * A fotografia informa a identidade do fato; decisões temporais documentadas
 * informam a responsabilidade efetiva. Nenhuma fotografia ou assinatura é alterada.
 */
@Service
@Transactional(readOnly = true)
public class ResolvedorEscopoProvaFrequenciaServico {
    private final ContratoRepositorio contratos;
    private final VinculoContratualRepositorio vinculos;
    private final AlocacaoVinculoRepositorio alocacoes;
    private final LotacaoRepositorio lotacoes;
    private final JornadaRepositorio jornadas;
    private final TransferenciaGestaoContratoRepositorio transferencias;
    private final MotorTemporalAlocacaoServico motorAlocacao;
    private final MotorTemporalContratoServico motorContrato;
    private final GestaoContratoTemporalServico gestoes;

    public ResolvedorEscopoProvaFrequenciaServico(ContratoRepositorio contratos,
            VinculoContratualRepositorio vinculos, AlocacaoVinculoRepositorio alocacoes,
            LotacaoRepositorio lotacoes, JornadaRepositorio jornadas,
            TransferenciaGestaoContratoRepositorio transferencias, MotorTemporalAlocacaoServico motorAlocacao,
            MotorTemporalContratoServico motorContrato, GestaoContratoTemporalServico gestoes) {
        this.contratos = contratos; this.vinculos = vinculos; this.alocacoes = alocacoes;
        this.lotacoes = lotacoes; this.jornadas = jornadas; this.transferencias = transferencias;
        this.motorAlocacao = motorAlocacao; this.motorContrato = motorContrato; this.gestoes = gestoes;
    }

    /** Null significa prova insuficiente; o consumidor deve negar acesso, sem usar a projeção atual. */
    public ContextoComplementoFrequenciaDTO realinhar(ContextoComplementoFrequenciaDTO foto, Instant conhecidoAte) {
        if (foto == null || foto.contratoId() == null || foto.dataReferencia() == null
                || foto.gestaoLocalId() == null || foto.lotacaoId() == null || foto.jornadaId() == null) return null;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        LocalDate data = foto.dataReferencia();
        Contrato contrato = contratos.findById(foto.contratoId()).orElse(null);
        if (contrato == null) return null;
        GestaoLocal responsabilidade = gestoes.gestaoEm(contrato, data, conhecimento);
        if (responsabilidade == null || responsabilidade.getSuperintendencia() == null) return null;

        Long alocacaoId = foto.alocacaoId(), lotacaoId = foto.lotacaoId(), jornadaId = foto.jornadaId(), cargoId = null;
        LocalDate inicioDecisao = null;
        VinculoContratual vinculo = foto.vinculoId() == null ? null : vinculos.findById(foto.vinculoId()).orElse(null);
        if (vinculo != null && (vinculo.getContrato() == null || !Objects.equals(vinculo.getContrato().getId(), contrato.getId())))
            return null;
        if (vinculo != null) {
            // O recorte derivado de desligamento/anulação não apaga a decisão de alocação.
            var faixas = motorAlocacao.reconstruirFaixasNegociais(vinculo, conhecimento).stream()
                    .filter(f -> f.estado() == EstadoNegocialAlocacao.REGISTRADA && f.dataInicio() != null
                            && !data.isBefore(f.dataInicio()) && (f.dataFim() == null || !data.isAfter(f.dataFim()))).toList();
            if (faixas.size() > 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mais de uma decisão de Alocação abrange o dia da prova histórica.");
            if (faixas.size() == 1) {
                var f = faixas.get(0);
                alocacaoId = f.id(); lotacaoId = f.lotacaoId(); jornadaId = f.jornadaId(); cargoId = f.cargoId();
                inicioDecisao = f.dataInicio();
            }
        }
        AlocacaoVinculo alocacao = alocacaoId == null ? null : alocacoes.findById(alocacaoId).orElse(null);
        if (alocacao != null) {
            if (foto.vinculoId() != null && !Objects.equals(alocacao.getVinculo().getId(), foto.vinculoId())) return null;
            if (cargoId == null && alocacao.getCargo() != null) cargoId = alocacao.getCargo().getId();
            if (inicioDecisao == null) inicioDecisao = alocacao.getDataInicio();
        }
        Long gestaoRecorte = gestaoDoRecorte(lotacaoId, jornadaId);
        if (gestaoRecorte == null) return null;
        if (!Objects.equals(gestaoRecorte, responsabilidade.getId())) {
            // Há acervos em que a transferência ocorreu depois do término então conhecido.
            // Somente seu mapa explícito pode reposicionar a prova, nunca o cadastro de Empregado.
            Map<Long, TransferenciaGestaoContrato> porEvento = transferencias
                    .findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(contrato.getId()).stream()
                    .filter(t -> t.getEvento() != null && t.getEvento().getId() != null)
                    .collect(Collectors.toMap(t -> t.getEvento().getId(), Function.identity(), (a,b) -> b));
            for (EventoCicloVidaContrato evento : motorContrato.eventosValidosOrdenados(contrato.getId(), data, conhecimento)) {
                TransferenciaGestaoContrato transferencia = porEvento.get(evento.getId());
                if (transferencia == null || transferencia.getDataEfetiva() == null
                        || inicioDecisao != null && transferencia.getDataEfetiva().isBefore(inicioDecisao)
                        || !Objects.equals(gestaoRecorte, transferencia.getGestaoOrigem().getId())) continue;
                Long l = lotacaoId, j = jornadaId, c = cargoId;
                var mapas = transferencia.getMapeamentos().stream().filter(m ->
                        Objects.equals(l, m.getLotacaoOrigem().getId())
                                && Objects.equals(j, m.getJornadaOrigem().getId())
                                && c != null && Objects.equals(c, m.getCargoOrigem().getId())).toList();
                if (mapas.size() != 1) return null;
                var mapa = mapas.get(0);
                lotacaoId = mapa.getLotacaoDestino().getId(); jornadaId = mapa.getJornadaDestino().getId();
                cargoId = mapa.getCargoDestino().getId(); gestaoRecorte = transferencia.getGestaoDestino().getId();
            }
        }
        if (!Objects.equals(gestaoRecorte, responsabilidade.getId())
                || !Objects.equals(gestaoDoRecorte(lotacaoId, jornadaId), responsabilidade.getId())) return null;
        return new ContextoComplementoFrequenciaDTO(data, contrato.getId(), foto.vinculoId(), alocacaoId,
                responsabilidade.getId(), responsabilidade.getSuperintendencia().getId(), lotacaoId, jornadaId);
    }

    private Long gestaoDoRecorte(Long lotacaoId, Long jornadaId) {
        if (lotacaoId == null || jornadaId == null) return null;
        Lotacao lotacao = lotacoes.findById(lotacaoId).orElse(null);
        Jornada jornada = jornadas.findById(jornadaId).orElse(null);
        if (lotacao == null || jornada == null || lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null
                || !Objects.equals(lotacao.getGestaoLocal().getId(), jornada.getGestaoLocal().getId())) return null;
        return lotacao.getGestaoLocal().getId();
    }
}
