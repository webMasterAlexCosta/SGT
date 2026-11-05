package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoVinculoContratual;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface EventoVinculoContratualRepositorio extends JpaRepository<EventoVinculoContratual, Long> {
    List<EventoVinculoContratual> findByVinculoIdOrderByRegistradoEmAscIdAsc(Long vinculoId);
    List<EventoVinculoContratual> findByVinculoIdInOrderByRegistradoEmAscIdAsc(Collection<Long> vinculoIds);
    Optional<EventoVinculoContratual> findTopByVinculoIdOrderByRegistradoEmDescIdDesc(Long vinculoId);
    default List<EventoVinculoContratual> findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(
            Long vinculoId, Instant conhecidoAte) {
        return buscarPorVinculoConhecidoAteMicros(vinculoId, PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte));
    }

    @Query("""
            select e from EventoVinculoContratual e
             where e.vinculo.id = :vinculoId and e.registradoEm <= :conhecidoAte
             order by e.registradoEm asc, e.id asc
            """)
    List<EventoVinculoContratual> buscarPorVinculoConhecidoAteMicros(
            @Param("vinculoId") Long vinculoId, @Param("conhecidoAte") Instant conhecidoAte);

    List<EventoVinculoContratual> findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(Long contratoId);
}
