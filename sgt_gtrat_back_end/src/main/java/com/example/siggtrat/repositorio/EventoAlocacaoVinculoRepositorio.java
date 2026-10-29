package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventoAlocacaoVinculoRepositorio extends JpaRepository<EventoAlocacaoVinculo, Long> {
    List<EventoAlocacaoVinculo> findByVinculoIdOrderByRegistradoEmAscIdAsc(Long vinculoId);
    List<EventoAlocacaoVinculo> findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(Long vinculoId, Instant conhecidoAte);
    Optional<EventoAlocacaoVinculo> findTopByVinculoIdOrderByRegistradoEmDescIdDesc(Long vinculoId);
}
