package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import com.example.siggtrat.dto.EventoFrequenciaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.EventoFrequenciaRepositorio;
import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class AuditoriaFrequenciaServico {
    private final EventoFrequenciaRepositorio repositorio;

    public AuditoriaFrequenciaServico(EventoFrequenciaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<EventoFrequenciaDTO> listar(Long frequenciaId) {
        List<EventoFrequencia> eventos = repositorio.findByFrequenciaIdOrderByNumeroVersaoAscIdAsc(frequenciaId);
        validarCadeia(eventos);
        return eventos.stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public boolean chaveJaAplicada(Long frequenciaId, String chave, String hashIntencao) {
        String normalizada = normalizar(chave);
        if (normalizada == null) return false;
        Optional<EventoFrequencia> existente = repositorio.findByChaveIdempotencia(normalizada);
        if (existente.isEmpty()) return false;
        EventoFrequencia evento = existente.get();
        if (!Objects.equals(evento.getFrequencia().getId(), frequenciaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência já pertence a outra frequência.");
        }
        if (!Objects.equals(evento.getHashIntencao(), hashIntencao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A chave de idempotência foi reutilizada para uma intenção diferente.");
        }
        return true;
    }

    @Transactional
    public EventoFrequencia registrar(Frequencia frequencia,
                                      TipoEventoFrequencia tipo,
                                      String snapshotAnterior,
                                      String motivo,
                                      UsuarioAplicacao usuario,
                                      String chaveIdempotencia,
                                      String hashIntencao) {
        String chave = normalizar(chaveIdempotencia);
        if (chave != null && repositorio.findByChaveIdempotencia(chave).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chave de idempotência já utilizada.");
        }
        EventoFrequencia evento = new EventoFrequencia();
        evento.setFrequencia(frequencia);
        evento.setNumeroVersao(frequencia.getVersaoNegocial());
        evento.setTipoEvento(tipo);
        evento.setDataReferencia(frequencia.getDataReferencia());
        evento.setSnapshotAnterior(snapshotAnterior);
        evento.setSnapshotNovo(snapshot(frequencia));
        evento.setMotivo(motivo);
        evento.setUsuarioId(usuario == null ? null : usuario.getId());
        evento.setUsuarioUsername(usuario == null ? "SISTEMA" : usuario.getUsername());
        evento.setRegistradoEm(Instant.now());
        evento.setChaveIdempotencia(chave);
        evento.setHashIntencao(hashIntencao);
        String hashAnterior = repositorio.findTopByFrequenciaIdOrderByNumeroVersaoDescIdDesc(frequencia.getId())
                .map(EventoFrequencia::getHashEvento).orElse(null);
        evento.setHashAnterior(hashAnterior);
        evento.setHashEvento(calcularHash(evento));
        return repositorio.save(evento);
    }

    private void validarCadeia(List<EventoFrequencia> eventos) {
        String anterior = null;
        Integer versaoAnterior = null;
        for (EventoFrequencia evento : eventos) {
            if (!Objects.equals(anterior, evento.getHashAnterior())
                    || evento.getHashEvento() == null
                    || !Objects.equals(evento.getHashEvento(), calcularHash(evento))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A cadeia de auditoria da frequência está inconsistente.");
            }
            if (versaoAnterior != null && evento.getNumeroVersao() <= versaoAnterior) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A sequência de versões da frequência está inconsistente.");
            }
            anterior = evento.getHashEvento();
            versaoAnterior = evento.getNumeroVersao();
        }
    }

    private String calcularHash(EventoFrequencia evento) {
        return UtilitarioHashSeguranca.sha256Hexadecimal(
                valor(evento.getHashAnterior()) + "|"
                        + (evento.getFrequencia() == null ? "" : valor(evento.getFrequencia().getId())) + "|"
                        + valor(evento.getNumeroVersao()) + "|" + valor(evento.getTipoEvento()) + "|"
                        + valor(evento.getDataReferencia()) + "|" + valor(evento.getSnapshotAnterior()) + "|"
                        + valor(evento.getSnapshotNovo()) + "|" + valor(evento.getMotivo()) + "|"
                        + valor(evento.getRegistradoEm()) + "|" + valor(evento.getUsuarioId()) + "|"
                        + valor(evento.getUsuarioUsername()) + "|" + valor(evento.getChaveIdempotencia()) + "|"
                        + valor(evento.getHashIntencao()));
    }

    public String snapshot(Frequencia f) {
        if (f == null) return null;
        return "status=" + valor(f.getStatus())
                + ";entrada1=" + valor(f.getEntrada1()) + ";saida1=" + valor(f.getSaida1())
                + ";entrada2=" + valor(f.getEntrada2()) + ";saida2=" + valor(f.getSaida2())
                + ";horas=" + valor(f.getHorasTrabalhadas()) + ";observacao=" + valor(f.getObservacao())
                + ";vinculo=" + id(f.getVinculoContratual()) + ";alocacao=" + id(f.getAlocacaoVinculo())
                + ";contrato=" + valor(f.getContratoSnapshotId()) + ";lotacao=" + valor(f.getLotacaoSnapshotId())
                + ";jornada=" + valor(f.getJornadaSnapshotId()) + ";cargo=" + valor(f.getCargoSnapshotId())
                + ";estado=" + valor(f.getEstadoNegocial()) + ";validade=" + valor(f.getValidadeOperacional())
                + ";auditoria=" + valor(f.getEstadoAuditoria()) + ";bloqueada=" + valor(f.getBloqueadaEdicaoOperacional());
    }

    private EventoFrequenciaDTO paraDTO(EventoFrequencia e) {
        return new EventoFrequenciaDTO(e.getId(), e.getFrequencia().getId(), e.getNumeroVersao(), e.getTipoEvento(),
                e.getDataReferencia(), e.getSnapshotAnterior(), e.getSnapshotNovo(), e.getMotivo(), e.getUsuarioId(),
                e.getUsuarioUsername(), e.getRegistradoEm(), e.getHashAnterior(), e.getHashEvento());
    }

    private static Long id(Object entidade) {
        if (entidade instanceof VinculoContratual v) return v.getId();
        if (entidade instanceof AlocacaoVinculo a) return a.getId();
        return null;
    }
    private static String valor(Object o) { return o == null ? "" : String.valueOf(o).replace(";", ","); }
    private static String normalizar(String s) { if (s == null) return null; String n=s.trim(); return n.isBlank()?null:n; }
}
