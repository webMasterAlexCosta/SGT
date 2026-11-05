package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.VersaoCalendarioEvento;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VersaoCalendarioEventoRepositorio extends JpaRepository<VersaoCalendarioEvento, Long> {
    List<VersaoCalendarioEvento> findByCalendarioEventoIdOrderByNumeroVersaoAsc(Long calendarioEventoId);
    Optional<VersaoCalendarioEvento> findTopByCalendarioEventoIdOrderByNumeroVersaoDesc(Long calendarioEventoId);

    default List<VersaoCalendarioEvento> buscarUltimasConhecidas(Instant conhecidoAte) {
        return buscarUltimasConhecidasEmMicros(PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte));
    }

    @Query("""
            select v from VersaoCalendarioEvento v
             where v.registradoEm <= :conhecidoAte
               and not exists (select posterior.id from VersaoCalendarioEvento posterior
                    where posterior.calendarioEvento = v.calendarioEvento
                      and posterior.numeroVersao > v.numeroVersao and posterior.registradoEm <= :conhecidoAte)
             order by v.calendarioEvento.id
            """)
    List<VersaoCalendarioEvento> buscarUltimasConhecidasEmMicros(@Param("conhecidoAte") Instant conhecidoAte);
}
