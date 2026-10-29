package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.GestaoLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GestaoLocalRepositorio extends JpaRepository<GestaoLocal, Long> {
    List<GestaoLocal> findAllByOrderByNomeAsc();
    List<GestaoLocal> findBySuperintendenciaIdOrderByNomeAsc(Long superintendenciaId);
    List<GestaoLocal> findBySuperintendenciaIdAndAtivoTrueOrderByNomeAsc(Long superintendenciaId);
    Optional<GestaoLocal> findBySuperintendenciaIdAndCodigoIgnoreCase(Long superintendenciaId, String codigo);
    Optional<GestaoLocal> findFirstBySuperintendenciaIdOrderByIdAsc(Long superintendenciaId);
    boolean existsBySuperintendenciaId(Long superintendenciaId);
}
