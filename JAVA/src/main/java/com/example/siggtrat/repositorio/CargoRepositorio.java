package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Cargo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CargoRepositorio extends JpaRepository<Cargo, Long> {
    Page<Cargo> findByGestaoLocalId(Long gestaoLocalId, Pageable pageable);
    Page<Cargo> findByGestaoLocalSuperintendenciaId(Long superintendenciaId, Pageable pageable);
    Optional<Cargo> findByGestaoLocalIdAndNomeIgnoreCase(Long gestaoLocalId, String nome);
}
