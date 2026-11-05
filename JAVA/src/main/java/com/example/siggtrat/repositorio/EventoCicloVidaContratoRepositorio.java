package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoCicloVidaContrato;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    default List<EventoCicloVidaContrato> findByContratoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaAsc(
            Long contratoId,
            LocalDate dataEfetiva,
            Instant registradoAte
    ) {
        return buscarPorContratoDataEfetivaEConhecimentoEmMicros(
                contratoId, dataEfetiva, PrecisaoTemporal.limiteInclusivoMicros(registradoAte));
    }

    @Query("""
            select e from EventoCicloVidaContrato e
             where e.contrato.id = :contratoId and e.dataEfetiva <= :dataEfetiva
               and e.registradoEm <= :registradoAte
             order by e.dataEfetiva asc
            """)
    List<EventoCicloVidaContrato> buscarPorContratoDataEfetivaEConhecimentoEmMicros(
            @Param("contratoId") Long contratoId, @Param("dataEfetiva") LocalDate dataEfetiva,
            @Param("registradoAte") Instant registradoAte);

    List<EventoCicloVidaContrato> findByContratoIdAndChaveNegocialAndDataEfetiva(
            Long contratoId,
            String chaveNegocial,
            LocalDate dataEfetiva
    );

    Optional<EventoCicloVidaContrato> findTopByFatoLogicoIdOrderByVersaoNegocialDesc(String fatoLogicoId);

    boolean existsByContratoId(Long contratoId);
}
