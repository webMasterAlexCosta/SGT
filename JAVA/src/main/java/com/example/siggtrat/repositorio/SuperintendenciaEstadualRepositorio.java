package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuperintendenciaEstadualRepositorio extends JpaRepository<SuperintendenciaEstadual, Long> {
    Optional<SuperintendenciaEstadual> findByUfIgnoreCase(String uf);
    List<SuperintendenciaEstadual> findAllByOrderByUfAsc();
    List<SuperintendenciaEstadual> findByAtivoTrueOrderByUfAsc();
}
