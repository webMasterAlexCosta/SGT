package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ReservaMatriculaContrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaMatriculaContratoRepositorio extends JpaRepository<ReservaMatriculaContrato, Long> {
    boolean existsByContratoIdAndMatricula(Long contratoId, String matricula);
}
