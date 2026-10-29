package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Ocorrencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OcorrenciaRepositorio extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {
    List<Ocorrencia> findByEmpregado_IdOrderByDataOcorrenciaDesc(Long idEmpregado);

    @EntityGraph(attributePaths = {"empregado"})
    List<Ocorrencia> findByEmpregadoIdAndDataOcorrenciaBetweenOrderByDataOcorrenciaDesc(
            Long empregadoId, LocalDate inicio, LocalDate fim);
}
