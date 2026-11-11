package com.example.siggtrat.servico;

import com.example.siggtrat.configuracao.propriedades.PropriedadesFechamentoFrequencia;
import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.FrequenciaDTO;
import com.example.siggtrat.evento.EventoFechamentoFrequenciaSolicitado;
import com.example.siggtrat.entidade.FechamentoFrequenciaItem;
import com.example.siggtrat.repositorio.EmpregadoRepositorio;
import com.example.siggtrat.repositorio.FechamentoFrequenciaItemRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FechamentoFrequenciaProcessador implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    @Override public org.slf4j.Logger loggerDiagnostico() { return log; }


    private static final Logger log = LoggerFactory.getLogger(FechamentoFrequenciaProcessador.class);

    /** Evita duplicidade local entre o evento inicial e o recuperador periódico. */
    private final Set<Long> emExecucao = ConcurrentHashMap.newKeySet();

    private final FechamentoFrequenciaEstadoServico stateServico;
    private final FechamentoFrequenciaItemRepositorio itemRepositorio;
    private final EmpregadoRepositorio empregadoRepositorio;
    private final FrequenciaServico frequenciaServico;
    private final FechamentoFrequenciaServico fechamentoServico;
    private final PropriedadesFechamentoFrequencia properties;
    private final InvalidacaoCacheServico cacheInvalidationServico;
    private final ElegibilidadeFechamentoFrequenciaServico elegibilidade;
    private final ZoneId zoneId = ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO);

    public FechamentoFrequenciaProcessador(FechamentoFrequenciaEstadoServico stateServico,
                                         FechamentoFrequenciaItemRepositorio itemRepositorio,
                                         EmpregadoRepositorio empregadoRepositorio,
                                         FrequenciaServico frequenciaServico,
                                         FechamentoFrequenciaServico fechamentoServico,
                                         PropriedadesFechamentoFrequencia properties,
                                         InvalidacaoCacheServico cacheInvalidationServico,
                                         ElegibilidadeFechamentoFrequenciaServico elegibilidade) {
        this.stateServico = stateServico;
        this.itemRepositorio = itemRepositorio;
        this.empregadoRepositorio = empregadoRepositorio;
        this.frequenciaServico = frequenciaServico;
        this.fechamentoServico = fechamentoServico;
        this.properties = properties;
        this.cacheInvalidationServico = cacheInvalidationServico;
        this.elegibilidade = elegibilidade;
    }

    @Async("fechamentoFrequenciaExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoSolicitar(EventoFechamentoFrequenciaSolicitado event) {
        processar(event.fechamentoId());
    }

    public void processar(Long fechamentoId) {
        if (fechamentoId == null || !emExecucao.add(fechamentoId)) return;

        try {
            var descriptorOpt = stateServico.reivindicar(fechamentoId);
            if (descriptorOpt.isEmpty()) return;

            var descritor = descriptorOpt.get();
            long aposId = descritor.ultimoEmpregadoIdProcessado();
            List<Long> contratoIdsQuery = FechamentoFrequenciaServico.idsParaConsulta(descritor.contratoIds());
            String modo = descritor.somenteAlteracoes() ? "ALTERACOES" : "COMPLETO";

            if (!descritor.somenteAlteracoes()) {
                long aposVinculo = 0;
                while (true) {
                    if (stateServico.cancelamentoSolicitado(fechamentoId)) {
                        stateServico.cancelar(fechamentoId);
                        return;
                    }
                    long cursor = elegibilidade.prepararProjecoesApos(descritor.dataReferencia(), descritor.lotacaoId(),
                            descritor.jornadaId(), descritor.contratoIds(), aposVinculo, properties.chunkSize());
                    if (cursor == 0) break;
                    aposVinculo = cursor;
                    stateServico.registrarPreparacao(fechamentoId);
                }
            }

            if (!descritor.somenteAlteracoes() && descritor.totalPrevisto() <= 0) {
                long totalPrevisto = fechamentoServico.contarElegiveisParaProcessamento(
                        fechamentoId,
                        descritor.dataReferencia(),
                        descritor.lotacaoId(),
                        descritor.jornadaId(),
                        descritor.contratoIds()
                );
                if (totalPrevisto == 0) {
                    // Pode ocorrer quando outro fechamento com escopo equivalente já
                    // materializou todas as frequências. O comando é idempotente.
                    stateServico.concluir(fechamentoId);
                    return;
                }
                stateServico.definirTotalPrevisto(fechamentoId, totalPrevisto);
            }

            log.info(
                    "Iniciando fechamento assíncrono: id={} modo={} data={} lotacao={} jornada={} aposEmpregado={}",
                    fechamentoId,
                    modo,
                    descritor.dataReferencia(),
                    descritor.lotacaoId(),
                    descritor.jornadaId(),
                    aposId
            );

            while (true) {
                if (stateServico.cancelamentoSolicitado(fechamentoId)) {
                    stateServico.cancelar(fechamentoId);
                    return;
                }

                List<Long> empregadoIds = buscarProximoBloco(
                        fechamentoId,
                        descritor,
                        contratoIdsQuery,
                        aposId
                );

                if (empregadoIds.isEmpty()) {
                    stateServico.concluir(fechamentoId);
                    return;
                }

                Map<Long, FrequenciaDTO> alteracoes = carregarAlteracoes(
                        fechamentoId,
                        empregadoIds,
                        descritor.dataReferencia()
                );

                frequenciaServico.processarBlocoFechamento(
                        fechamentoId,
                        empregadoIds,
                        descritor.dataReferencia(),
                        alteracoes,
                        descritor.usuarioCriadorId()
                );

                aposId = empregadoIds.get(empregadoIds.size() - 1);
            }
        } catch (Throwable erro) {
            try {
                stateServico.falhar(fechamentoId, erro);
            } catch (Throwable erroAoRegistrar) {
                log.error("Falha ao registrar erro: referencia=FECHAMENTO-{} falhaOriginal={} falhaRegistro={}", fechamentoId,
                        com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(erro),
                        com.example.siggtrat.diagnostico.DiagnosticoFalha.resumir(erroAoRegistrar));
            }
        } finally {
            emExecucao.remove(fechamentoId);
            // O processamento ocorre após a resposta HTTP e pode alterar milhares
            // de frequências. Garante que o painel não permaneça com dados antigos.
            cacheInvalidationServico.invalidarPainelEFrequencias();
        }
    }

    private List<Long> buscarProximoBloco(
            Long fechamentoId,
            FechamentoFrequenciaEstadoServico.DescritorProcessamento descritor,
            List<Long> contratoIdsQuery,
            long aposId
    ) {
        PageRequest pagina = PageRequest.of(0, properties.chunkSize());
        if (descritor.somenteAlteracoes()) {
            return itemRepositorio.buscarIdsEmpregadosApos(fechamentoId, aposId, pagina);
        }

        return elegibilidade.buscarPendentesApos(fechamentoId, descritor.dataReferencia(),
                descritor.lotacaoId(), descritor.jornadaId(), descritor.contratoIds(), aposId, properties.chunkSize());
    }

    private Map<Long, FrequenciaDTO> carregarAlteracoes(Long fechamentoId,
                                                        List<Long> empregadoIds,
                                                        LocalDate data) {
        Map<Long, FrequenciaDTO> mapa = new HashMap<>();
        for (FechamentoFrequenciaItem item : itemRepositorio
                .buscarPorIdFechamentoEIdsEmpregados(fechamentoId, empregadoIds)) {
            FrequenciaDTO dto = new FrequenciaDTO();
            dto.setEmpregado(new FrequenciaDTO.EmpregadoResumoDTO(
                    item.getEmpregado().getId(), item.getEmpregado().getNome()));
            dto.setDataReferencia(data);
            dto.setStatus(item.getStatus());
            dto.setObservacao(item.getObservacao());
            dto.setJustificativaRetificacao(item.getJustificativaRetificacao());
            dto.setChaveIdempotencia(item.getChaveIdempotencia());
            dto.setEntrada1(item.getEntrada1());
            dto.setSaida1(item.getSaida1());
            dto.setEntrada2(item.getEntrada2());
            dto.setSaida2(item.getSaida2());
            dto.setHorasTrabalhadas(item.getHorasTrabalhadas());
            dto.setHorarioSaida(item.getHorarioSaida());
            dto.setHorasNoturnas(item.getHorasNoturnas());
            mapa.put(item.getEmpregado().getId(), dto);
        }
        return mapa;
    }
}
