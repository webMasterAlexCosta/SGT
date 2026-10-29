package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RelacaoPrecedenciaEventoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface RelacaoPrecedenciaEventoContratoRepositorio
        extends JpaRepository<RelacaoPrecedenciaEventoContrato, Long> {

    List<RelacaoPrecedenciaEventoContrato> findByContratoIdOrderByRegistradoEmAsc(Long contratoId);

    List<RelacaoPrecedenciaEventoContrato> findByContratoIdAndRegistradoEmLessThanEqualOrderByRegistradoEmAsc(
            Long contratoId,
            Instant registradoAte
    );

    List<RelacaoPrecedenciaEventoContrato> findByEventoFonteIdInOrEventoAlvoIdIn(
            Collection<Long> fontes,
            Collection<Long> alvos
    );

    boolean existsByEventoFonteIdAndEventoAlvoId(Long eventoFonteId, Long eventoAlvoId);
}
