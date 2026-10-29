package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface EventoVinculoContratualRepositorio extends JpaRepository<EventoVinculoContratual, Long> {
    List<EventoVinculoContratual> findByVinculoIdOrderByRegistradoEmAscIdAsc(Long vinculoId);
    Optional<EventoVinculoContratual> findTopByVinculoIdOrderByRegistradoEmDescIdDesc(Long vinculoId);
    List<EventoVinculoContratual> findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(Long vinculoId, Instant conhecidoAte);
    List<EventoVinculoContratual> findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(Long contratoId);
}
