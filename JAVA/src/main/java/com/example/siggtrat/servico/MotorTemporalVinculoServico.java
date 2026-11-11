package com.example.siggtrat.servico;

import com.example.siggtrat.regra.MaterialHashEventos;
import static com.example.siggtrat.regra.MaterialHashEventos.*;


import com.example.siggtrat.dominio.vinculo.*;
import com.example.siggtrat.dto.PosicaoVinculoTemporalDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoVinculoContratualRepositorio;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

/** Reconstrói o vínculo por data efetiva e pelo instante em que o sistema conhecia os fatos. */
@Service
@Transactional(readOnly = true)
public class MotorTemporalVinculoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(MotorTemporalVinculoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }

    private final EventoVinculoContratualRepositorio eventoRepositorio;
    private MotorTemporalContratoServico motorContrato;

    public MotorTemporalVinculoServico(EventoVinculoContratualRepositorio eventoRepositorio) {
        this.eventoRepositorio = eventoRepositorio;
    }

    @Autowired
    public void configurarMotorContrato(MotorTemporalContratoServico motorContrato) {
        this.motorContrato = motorContrato;
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
        LocalDate ultimaDataMatricula = null;

        Optional<EventoVinculoContratual> criacao = eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO).findFirst();
        if (criacao.isPresent()) {
            EventoVinculoContratual e = criacao.get();
            inicio = e.getInicioNovo() != null ? e.getInicioNovo() : inicio;
            fim = e.getFimNovo();
            matricula = e.getMatriculaNova();
            estado = EstadoNegocialVinculo.REGISTRADO;
        } else if (!eventos.isEmpty()) {
            // O primeiro fato também fotografia o estado imediatamente anterior.
            // A projeção materializada pode já conter alterações conhecidas depois do corte.
            EventoVinculoContratual fonte = eventos.get(0);
            if (fonte.getInicioAnterior() == null) throw semFonte(vinculo.getId());
            inicio = fonte.getInicioAnterior();
            fim = fonte.getFimAnterior();
            matricula = fonte.getMatriculaAnterior();
            estado = EstadoNegocialVinculo.REGISTRADO;
        } else if (!eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculo.getId()).isEmpty()) {
            throw semFonte(vinculo.getId());
        }

        for (EventoVinculoContratual evento : eventos) {
            ultimoEventoId = evento.getId();
            switch (evento.getTipoEvento()) {
                case CRIACAO -> { }
                case RETIFICACAO_INICIO -> inicio = evento.getInicioNovo();
                case ALTERACAO_MATRICULA -> {
                    // O corte de conhecimento seleciona os fatos, mas a data
                    // efetiva define qual matrícula vigora. Um registro tardio
                    // retroativo não substitui uma mudança efetiva posterior.
                    LocalDate efetiva = evento.getDataEfetiva();
                    if (!efetiva.isAfter(data) && (ultimaDataMatricula == null
                            || !efetiva.isBefore(ultimaDataMatricula))) {
                        matricula = evento.getMatriculaNova();
                        ultimaDataMatricula = efetiva;
                    }
                }
                case CANCELAMENTO_PROGRAMACAO -> estado = EstadoNegocialVinculo.CANCELADO;
                case DESLIGAMENTO, RETIFICACAO_DESLIGAMENTO,
                     FIM_NATURAL_CONTRATO, ENCERRAMENTO_ANTECIPADO_CONTRATO,
                     ANULACAO_CONTRATO -> fim = evento.getFimNovo();
                case CANCELAMENTO_DESLIGAMENTO_PROGRAMADO -> fim = null;
            }
        }

        LocalDate limiteOperacional = fim;
        // A projeção de hoje não participa de uma consulta com corte de conhecimento.
        var posicaoContrato = vinculo.getContrato() == null || motorContrato == null ? null
                : motorContrato.reconstruirPosicao(vinculo.getContrato(), data, conhecimento);
        LocalDate fimContrato = posicaoContrato != null ? posicaoContrato.vigenciaFim()
                : vinculo.getContrato() == null ? null : vinculo.getContrato().getDataFim();
        boolean contratoAnulado = posicaoContrato != null && posicaoContrato.estadoNegocial() == EstadoNegocialContrato.ANULADO;
        if (estado == EstadoNegocialVinculo.REGISTRADO && fimContrato != null
                && (limiteOperacional == null || fimContrato.isBefore(limiteOperacional))) {
            limiteOperacional = fimContrato;
        }

        SituacaoTemporalVinculo situacao;
        if (estado == EstadoNegocialVinculo.CANCELADO) situacao = SituacaoTemporalVinculo.CANCELADO;
        else if (estado == EstadoNegocialVinculo.ANULADO || contratoAnulado) situacao = SituacaoTemporalVinculo.ANULADO;
        else if (data.isBefore(inicio)) situacao = SituacaoTemporalVinculo.PROGRAMADO;
        else if (limiteOperacional != null && data.isAfter(limiteOperacional)) situacao = SituacaoTemporalVinculo.ENCERRADO;
        else situacao = SituacaoTemporalVinculo.VIGENTE;

        return new PosicaoVinculoTemporalDTO(vinculo.getId(), vinculo.getPessoa().getId(), vinculo.getContrato().getId(),
                data, conhecimento, inicio, fim, matricula, estado, situacao, ultimoEventoId);
    }

    public boolean cadeiaHashIntegra(Long vinculoId) {
        String anterior = null;
        for (EventoVinculoContratual e : eventoRepositorio.findByVinculoIdOrderByRegistradoEmAscIdAsc(vinculoId)) {
            if (e == null || !Objects.equals(anterior, e.getHashAnterior()) || e.getHashEvento() == null
                    || !e.getHashEvento().equals(hashEsperado(e))) return false;
            anterior = e.getHashEvento();
        }
        return true;
    }

    private String hashEsperado(EventoVinculoContratual e) {
        if (e.getTipoEvento() == null || e.getDataEfetiva() == null || e.getVinculo() == null
                || e.getVinculo().getContrato() == null || e.getPessoaCpfFotografia() == null) return null;
        // O cadastro excedente substitui a justificativa depois de gerar o digest base.
        String justificativaBase = e.isInclusaoExcedente() && e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO
                ? "Cadastro integrado de Pessoa, Vínculo e Alocação inicial" : e.getJustificativa();
        String hash = MaterialHashEventos.sha256("sgt.evento-vinculo.base",
                texto(e.getHashAnterior()), enumeracao(e.getTipoEvento()), data(e.getDataEfetiva()),
                data(e.getInicioAnterior()), data(e.getInicioNovo()), data(e.getFimAnterior()), data(e.getFimNovo()),
                texto(e.getMatriculaAnterior()), texto(e.getMatriculaNova()), texto(e.getPessoaCpfFotografia()),
                longo(e.getVinculo().getContrato().getId()), longo(e.getResponsavelId()), texto(justificativaBase),
                longo(e.getMotivoDesligamentoIdFotografia()), texto(e.getMotivoDesligamentoDescricaoFotografia()),
                enumeracao(e.getMotivoDesligamentoCategoriaFotografia()));
        if (e.isInclusaoExcedente()) {
            hash = MaterialHashEventos.sha256("sgt.evento-vinculo.excedente", texto(hash),
                    longo(e.getComposicaoExcedenteId()), longo(e.getLotacaoExcedenteId()),
                    longo(e.getJornadaExcedenteId()), longo(e.getCargoExcedenteId()),
                    inteiro(e.getEfetivoPrevistoExcedente()), longo(e.getOcupacaoAposInclusaoExcedente()),
                    texto(e.getJustificativa()), texto(e.getEvidencia()), texto(e.getAutorizacao()));
        }
        return hash;
    }

    /** Mesma regra do motor, disponível para a grade carregar eventos de vários vínculos em lote. */
    public String matriculaNaData(VinculoContratual vinculo, LocalDate data, List<EventoVinculoContratual> eventos) {
        String matricula = vinculo.getMatricula();
        Optional<EventoVinculoContratual> criacao = eventos.stream()
                .filter(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO).findFirst();
        if (criacao.isPresent()) matricula = criacao.get().getMatriculaNova();
        else if (!eventos.isEmpty()) matricula = eventos.get(0).getMatriculaAnterior();
        LocalDate ultimaData = null;
        for (EventoVinculoContratual evento : eventos) {
            if (evento.getTipoEvento() != TipoEventoVinculoContratual.ALTERACAO_MATRICULA) continue;
            LocalDate efetiva = evento.getDataEfetiva();
            if (!efetiva.isAfter(data) && (ultimaData == null || !efetiva.isBefore(ultimaData))) {
                matricula = evento.getMatriculaNova(); ultimaData = efetiva;
            }
        }
        return matricula;
    }

    private ResponseStatusException semFonte(Long vinculoId) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Estado histórico do vínculo " + vinculoId + " não comprovado no corte de conhecimento informado.");
    }
}
