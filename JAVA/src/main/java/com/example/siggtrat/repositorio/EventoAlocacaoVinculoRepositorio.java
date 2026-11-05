package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoAlocacaoVinculo;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventoAlocacaoVinculoRepositorio extends JpaRepository<EventoAlocacaoVinculo, Long> {
    List<EventoAlocacaoVinculo> findByVinculoIdOrderByRegistradoEmAscIdAsc(Long vinculoId);
    default List<EventoAlocacaoVinculo> findByVinculoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAscIdAsc(
            Long vinculoId, Instant conhecidoAte) {
        return buscarPorVinculoConhecidoAteMicros(vinculoId, PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte));
    }

    @Query("""
            select e from EventoAlocacaoVinculo e
             where e.vinculo.id = :vinculoId and e.registradoEm <= :conhecidoAte
             order by e.registradoEm asc, e.id asc
            """)
    List<EventoAlocacaoVinculo> buscarPorVinculoConhecidoAteMicros(
            @Param("vinculoId") Long vinculoId, @Param("conhecidoAte") Instant conhecidoAte);

    Optional<EventoAlocacaoVinculo> findTopByVinculoIdOrderByRegistradoEmDescIdDesc(Long vinculoId);
    List<EventoAlocacaoVinculo> findByVinculoContratoIdAndInclusaoExcedenteTrueOrderByDataEfetivaAscIdAsc(Long contratoId);
}
