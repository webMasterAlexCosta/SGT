package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface RelacaoPrecedenciaEventoContratoRepositorio
        extends JpaRepository<RelacaoPrecedenciaEventoContrato, Long> {

    List<RelacaoPrecedenciaEventoContrato> findByContratoIdOrderByRegistradoEmAsc(Long contratoId);

    List<RelacaoPrecedenciaEventoContrato> findByContratoIdInOrderByContratoIdAscRegistradoEmAsc(
            Collection<Long> contratoIds);

    default List<RelacaoPrecedenciaEventoContrato> findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(
            Long contratoId,
            Instant registradoAte
    ) {
        return buscarPorContratoConhecidoAteMicros(contratoId, PrecisaoTemporal.limiteInclusivoMicros(registradoAte));
    }

    @Query("""
            select r from RelacaoPrecedenciaEventoContrato r
             where r.contrato.id = :contratoId and r.registradoEm <= :registradoAte
             order by r.registradoEm asc
            """)
    List<RelacaoPrecedenciaEventoContrato> buscarPorContratoConhecidoAteMicros(
            @Param("contratoId") Long contratoId, @Param("registradoAte") Instant registradoAte);

    List<RelacaoPrecedenciaEventoContrato> findByEventoFonteIdInOrEventoAlvoIdIn(
            Collection<Long> fontes,
            Collection<Long> alvos
    );

    boolean existsByEventoFonteIdAndEventoAlvoId(Long eventoFonteId, Long eventoAlvoId);
}
