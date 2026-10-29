package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.VersaoContratoNegocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VersaoContratoNegocialRepositorio extends JpaRepository<VersaoContratoNegocial, Long> {
    List<VersaoContratoNegocial> findByContratoIdOrderByNumeroVersaoAsc(Long contratoId);
    List<VersaoContratoNegocial> findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroVersaoAsc(
            Long contratoId,
            Instant registradoAte
    );
    Optional<VersaoContratoNegocial> findTopByContratoIdOrderByNumeroVersaoDesc(Long contratoId);
    boolean existsByContratoId(Long contratoId);
}
