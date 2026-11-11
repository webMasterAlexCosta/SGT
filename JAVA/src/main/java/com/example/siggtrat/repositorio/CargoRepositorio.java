package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Cargo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargoRepositorio extends JpaRepository<Cargo, Long> {
    Page<Cargo> findByGestaoLocalId(Long gestaoLocalId, Pageable pageable);
    Page<Cargo> findByGestaoLocalSuperintendenciaId(Long superintendenciaId, Pageable pageable);
    Optional<Cargo> findByGestaoLocalIdAndNomeIgnoreCase(Long gestaoLocalId, String nome);

    /** Serializa a identidade nominal da Gestão sem bloquear suas referências por FK. */
    @Query(value = "select ativo from gestao_local where id_gestao_local=:gestaoLocalId for no key update",
            nativeQuery = true)
    Optional<Boolean> bloquearNamespaceDaGestaoLocal(@Param("gestaoLocalId") Long gestaoLocalId);
}
