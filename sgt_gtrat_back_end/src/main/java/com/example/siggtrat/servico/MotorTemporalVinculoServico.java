package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dto.PosicaoVinculoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/** Reconstrói o vínculo por data efetiva e pelo instante em que o sistema conhecia os fatos. */
@Service
public class MotorTemporalVinculoServico {
    private final EventoVinculoContratualRepositorio eventoRepositorio;

    public MotorTemporalVinculoServico(EventoVinculoContratualRepositorio eventoRepositorio) {
        this.eventoRepositorio = eventoRepositorio;
    }

    public PosicaoVinculoTemporalDTO reconstruir(VinculoContratual vinculo, LocalDate dataReferencia, Instant conhecidoAte) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        Instant conhecimento = conhecidoAte == null ? Instant.now() : conhecidoAte;
        List<EventoVinculoContratual> eventos = eventoRepositorio
                .findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), conhecimento);

        LocalDate inicio = vinculo.getDataInicio();
        LocalDate fim = vinculo.getDataFim();
        String matricula = vinculo.getMatricula();
        EstadoNegocialVinculo estado = vinculo.getEstadoNegocial();
        Long ultimoEventoId = null;

        Optional<EventoVinculoContratual> criacao = eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO).findFirst();
        if (criacao.isPresent()) {
            EventoVinculoContratual e = criacao.get();
            inicio = e.getInicioNovo() != null ? e.getInicioNovo() : inicio;
            fim = e.getFimNovo();
            matricula = e.getMatriculaNova();
            estado = EstadoNegocialVinculo.REGISTRADO;
        }

        for (EventoVinculoContratual evento : eventos) {
            ultimoEventoId = evento.getId();
            switch (evento.getTipoEvento()) {
                case CRIACAO -> { }
                case RETIFICACAO_INICIO -> inicio = evento.getInicioNovo();
                case ALTERACAO_MATRICULA -> {
                    // Matrícula muda somente a partir da data efetiva do evento.
                    if (!evento.getDataEfetiva().isAfter(data)) matricula = evento.getMatriculaNova();
                }
                case CANCELAMENTO_PROGRAMACAO -> estado = EstadoNegocialVinculo.CANCELADO;
                case DESLIGAMENTO, RETIFICACAO_DESLIGAMENTO,
                     FIM_NATURAL_CONTRATO, ENCERRAMENTO_ANTECIPADO_CONTRATO,
                     ANULACAO_CONTRATO -> fim = evento.getFimNovo();
                case CANCELAMENTO_DESLIGAMENTO_PROGRAMADO -> fim = null;
            }
        }

        LocalDate limiteOperacional = fim;
        if (estado == EstadoNegocialVinculo.REGISTRADO && vinculo.getContrato() != null
                && vinculo.getContrato().getDataFim() != null
                && (limiteOperacional == null || vinculo.getContrato().getDataFim().isBefore(limiteOperacional))) {
            limiteOperacional = vinculo.getContrato().getDataFim();
        }

        SituacaoTemporalVinculo situacao;
        if (estado == EstadoNegocialVinculo.CANCELADO) situacao = SituacaoTemporalVinculo.CANCELADO;
        else if (estado == EstadoNegocialVinculo.ANULADO) situacao = SituacaoTemporalVinculo.ANULADO;
        else if (data.isBefore(inicio)) situacao = SituacaoTemporalVinculo.PROGRAMADO;
        else if (limiteOperacional != null && data.isAfter(limiteOperacional)) situacao = SituacaoTemporalVinculo.ENCERRADO;
        else situacao = SituacaoTemporalVinculo.VIGENTE;

        return new PosicaoVinculoTemporalDTO(vinculo.getId(), vinculo.getPessoa().getId(), vinculo.getContrato().getId(),
                data, conhecimento, inicio, fim, matricula, estado, situacao, ultimoEventoId);
    }

    public boolean cadeiaHashIntegra(Long vinculoId) {
        String anterior = null;
        for (EventoVinculoContratual e : eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId)) {
            if (!Objects.equals(anterior, e.getHashAnterior()) || e.getHashEvento() == null || e.getHashEvento().length() != 64) return false;
            anterior = e.getHashEvento();
        }
        return true;
    }
}
