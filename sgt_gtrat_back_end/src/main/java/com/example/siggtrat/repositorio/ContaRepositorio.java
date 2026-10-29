package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContaRepositorio extends JpaRepository<Conta, Long> {
    boolean existsByAgenciaId(Long agenciaId);
}
