package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.VersaoContratoNegocial;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface VersaoContratoNegocialRepositorio extends JpaRepository<VersaoContratoNegocial, Long> {
    List<VersaoContratoNegocial> findByContratoIdOrderByNumeroVersaoAsc(Long contratoId);
    List<VersaoContratoNegocial> findByContratoIdInOrderByContratoIdAscNumeroVersaoAsc(Collection<Long> contratoIds);
    default List<VersaoContratoNegocial> findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(
            Long contratoId,
            Instant registradoAte
    ) {
        return buscarPorContratoConhecidoAteMicros(contratoId, PrecisaoTemporal.limiteInclusivoMicros(registradoAte));
    }

    @Query("""
            select v from VersaoContratoNegocial v
             where v.contrato.id = :contratoId and v.registradoEm <= :registradoAte
             order by v.numeroVersao asc
            """)
    List<VersaoContratoNegocial> buscarPorContratoConhecidoAteMicros(
            @Param("contratoId") Long contratoId, @Param("registradoAte") Instant registradoAte);

    Optional<VersaoContratoNegocial> findTopByContratoIdOrderByNumeroVersaoDesc(Long contratoId);
    boolean existsByContratoId(Long contratoId);
}
