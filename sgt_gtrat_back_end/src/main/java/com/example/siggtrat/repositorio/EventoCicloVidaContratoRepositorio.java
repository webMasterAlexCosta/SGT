package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoCicloVidaContratoRepositorio extends JpaRepository<EventoCicloVidaContrato, Long> {
    List<EventoCicloVidaContrato> findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(Long contratoId);

    List<EventoCicloVidaContrato> findByContratoIdInOrderByContratoIdAscDataEfetivaAscRegistradoEmAsc(
            Collection<Long> contratoIds
    );

    List<EventoCicloVidaContrato> findByContratoIdAndDataEfetivaLessThanEqualOrderByDataEfetivaAsc(
            Long contratoId,
            LocalDate dataEfetiva
    );

    List<EventoCicloVidaContrato> findByContratoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaAsc(
            Long contratoId,
            LocalDate dataEfetiva,
            Instant registradoAte
    );

    List<EventoCicloVidaContrato> findByContratoIdAndChaveNegocialAndDataEfetiva(
            Long contratoId,
            String chaveNegocial,
            LocalDate dataEfetiva
    );

    Optional<EventoCicloVidaContrato> findTopByFatoLogicoIdOrderByVersaoNegocialDesc(String fatoLogicoId);

    boolean existsByContratoId(Long contratoId);
}
