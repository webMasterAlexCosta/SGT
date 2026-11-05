package com.example.siggtrat.servico;

import com.example.siggtrat.dto.IdentidadeHistoricaResolvidaDTO;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.repositorio.*;
import com.example.siggtrat.utilitario.RelogioSistema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Resolve apenas fontes da identidade canônica e do vínculo consultado. */
@Service
@Transactional(readOnly = true)
public class IdentidadeHistoricaServico {
    private final ProvaNomePessoaHistoricoRepositorio provas;
    private final EventoVinculoContratualRepositorio eventos;
    public IdentidadeHistoricaServico(ProvaNomePessoaHistoricoRepositorio provas, EventoVinculoContratualRepositorio eventos) {
        this.provas = provas; this.eventos = eventos;
    }
    public IdentidadeHistoricaResolvidaDTO resolver(Pessoa pessoa, VinculoContratual vinculo,
                                                    LocalDate dataEfetiva, Instant conhecidoAte) {
        if (pessoa == null || vinculo == null || dataEfetiva == null || vinculo.getId() == null)
            return IdentidadeHistoricaResolvidaDTO.ausente();
        if (vinculo.getPessoa() == null || !Objects.equals(pessoa.getId(), vinculo.getPessoa().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identidade não encontrada no vínculo.");
        Instant k = conhecidoAte == null ? Instant.now() : conhecidoAte;
        return resolverCarregado(dataEfetiva, k,
                provas.findByPessoaIdAndVinculoIdOrderByRegistradoEmAscIdAsc(pessoa.getId(), vinculo.getId()),
                eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), k));
    }
    /** Duas consultas por vínculo para todo o período; avaliação diária em memória. */
    public Map<LocalDate, IdentidadeHistoricaResolvidaDTO> resolverPeriodo(Pessoa pessoa, VinculoContratual vinculo,
            LocalDate inicio, LocalDate fim, Instant conhecidoAte) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) return Map.of();
        if (pessoa != null && vinculo != null && (vinculo.getPessoa() == null
                || !Objects.equals(pessoa.getId(), vinculo.getPessoa().getId())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identidade não encontrada no vínculo.");
        Instant k = conhecidoAte == null ? Instant.now() : conhecidoAte;
        boolean ancorado = pessoa != null && vinculo != null && vinculo.getId() != null;
        List<ProvaNomePessoaHistorico> ps = ancorado ? provas.findByPessoaIdAndVinculoIdOrderByRegistradoEmAscIdAsc(pessoa.getId(), vinculo.getId()) : List.of();
        List<EventoVinculoContratual> es = ancorado ? eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculo.getId(), k) : List.of();
        Map<LocalDate, IdentidadeHistoricaResolvidaDTO> resultado = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            resultado.put(dia, resolverCarregado(dia, k, ps, es));
            if (dia.equals(LocalDate.MAX)) break;
        }
        return resultado;
    }
    private IdentidadeHistoricaResolvidaDTO resolverCarregado(LocalDate dataEfetiva, Instant k,
            List<ProvaNomePessoaHistorico> todas, List<EventoVinculoContratual> todosEventos) {
        List<ProvaNomePessoaHistorico> conhecidas = todas.stream().filter(p -> !p.getRegistradoEm().isAfter(k))
                .filter(p -> contem(p.getDataInicioEfetiva(), p.getDataFimEfetiva(), dataEfetiva)).toList();
        List<ProvaNomePessoaHistorico> vigentes = conhecidas.stream()
                .filter(p -> conhecidas.stream().noneMatch(n -> descendeDe(n, p.getId()))).toList();
        if (!vigentes.isEmpty()) {
            if (vigentes.stream().map(ProvaNomePessoaHistorico::getNomeComprovado).distinct().count() != 1)
                throw conflito();
            // Concordância não escolhe uma verdade por ID: todas as fontes atestam o mesmo valor.
            ProvaNomePessoaHistorico p = vigentes.get(0);
            for (EventoVinculoContratual e : filtrarEventos(todosEventos, dataEfetiva)) {
                if (!Objects.equals(e.getPessoaNomeFotografia(), p.getNomeComprovado())
                        && !retificaEvento(p, e)) throw conflito();
            }
            return new IdentidadeHistoricaResolvidaDTO(p.getNomeComprovado(), true, "PROVA_NOME_HISTORICO",
                    p.getId(), p.getRegistradoEm(), p.getDataInicioEfetiva(), p.getDataFimEfetiva());
        }
        List<EventoVinculoContratual> fontes = filtrarEventos(todosEventos, dataEfetiva);
        if (fontes.isEmpty()) return IdentidadeHistoricaResolvidaDTO.ausente();
        List<EventoVinculoContratual> vigentesEventos = fontes.stream()
                .filter(e -> fontes.stream().noneMatch(n -> eventoDescendeDe(n, e.getId()))).toList();
        if (vigentesEventos.stream().map(EventoVinculoContratual::getPessoaNomeFotografia).distinct().count() != 1)
            throw conflito();
        EventoVinculoContratual e = vigentesEventos.get(0);
        boolean origem = e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO;
        return new IdentidadeHistoricaResolvidaDTO(e.getPessoaNomeFotografia(), true, "EVENTO_VINCULO", e.getId(),
                e.getRegistradoEm(), origem && e.getInicioNovo() != null ? e.getInicioNovo() : e.getDataEfetiva(),
                origem ? e.getFimNovo() : e.getDataEfetiva());
    }
    public IdentidadeHistoricaResolvidaDTO capturarNovoFato(Pessoa pessoa, VinculoContratual vinculo,
                                                           LocalDate dataEfetiva, Instant registradoEm) {
        if (pessoa == null || vinculo == null || dataEfetiva == null) return IdentidadeHistoricaResolvidaDTO.ausente();
        if (dataEfetiva.isBefore(RelogioSistema.hoje())) return resolver(pessoa, vinculo, dataEfetiva, registradoEm);
        if (vinculo.getPessoa() == null || !Objects.equals(pessoa.getId(), vinculo.getPessoa().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Identidade não encontrada no vínculo.");
        if (pessoa.getNome() == null || pessoa.getNome().isBlank()) return IdentidadeHistoricaResolvidaDTO.ausente();
        return new IdentidadeHistoricaResolvidaDTO(pessoa.getNome(), true, "CADASTRO_CONTEMPORANEO", pessoa.getId(),
                registradoEm == null ? Instant.now() : registradoEm, dataEfetiva, dataEfetiva);
    }
    List<EventoVinculoContratual> fontesEventos(Long vinculoId, LocalDate data, Instant k) {
        return filtrarEventos(eventos.findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(vinculoId, k), data);
    }
    private List<EventoVinculoContratual> filtrarEventos(List<EventoVinculoContratual> fontes, LocalDate data) {
        return fontes.stream()
                .filter(e -> e.getPessoaNomeFotografia() != null && !e.getPessoaNomeFotografia().isBlank())
                .filter(e -> e.getTipoEvento() == TipoEventoVinculoContratual.CRIACAO
                        ? contem(e.getInicioNovo() == null ? e.getDataEfetiva() : e.getInicioNovo(), e.getFimNovo(), data)
                        : data.equals(e.getDataEfetiva())).toList();
    }
    static boolean contem(LocalDate inicio, LocalDate fim, LocalDate data) {
        return inicio != null && !data.isBefore(inicio) && (fim == null || !data.isAfter(fim));
    }
    static boolean descendeDe(ProvaNomePessoaHistorico p, Long id) {
        Set<Long> vistos = new HashSet<>();
        for (var a = p.getProvaAnterior(); a != null && vistos.add(a.getId()); a = a.getProvaAnterior())
            if (Objects.equals(a.getId(), id)) return true;
        return false;
    }
    static boolean retificaEvento(ProvaNomePessoaHistorico p, EventoVinculoContratual evento) {
        Set<Long> vistos = new HashSet<>();
        for (var a = p; a != null && vistos.add(a.getId()); a = a.getProvaAnterior()) {
            var origem = a.getEventoOrigemRetificado();
            if (origem != null && (Objects.equals(origem.getId(), evento.getId())
                    || eventoDescendeDe(evento, origem.getId())
                    && Objects.equals(origem.getPessoaNomeFotografia(), evento.getPessoaNomeFotografia()))) return true;
        }
        return false;
    }
    static boolean eventoDescendeDe(EventoVinculoContratual e, Long id) {
        Set<Long> vistos = new HashSet<>();
        for (var a = e.getEventoPrecedente(); a != null && vistos.add(a.getId()); a = a.getEventoPrecedente())
            if (Objects.equals(a.getId(), id)) return true;
        return false;
    }
    private ResponseStatusException conflito() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Fontes nominais conflitantes exigem retificação explícita.");
    }
}
