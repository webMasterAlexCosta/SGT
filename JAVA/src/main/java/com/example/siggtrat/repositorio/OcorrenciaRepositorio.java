package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Ocorrencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcorrenciaRepositorio extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {
    Optional<Ocorrencia> findByEmpregadoIdAndChaveIdempotencia(Long empregadoId, String chaveIdempotencia);
    /** Descobre a raiz sem anexar a ocorrência antes de adquirir os locks do contexto. */
    @Query("select o.empregado.id from Ocorrencia o where o.id = :id")
    Optional<Long> buscarEmpregadoId(@Param("id") Long id);

    /** Consulta histórica: inclui os fatos que tiveram seu efeito cancelado. */
    List<Ocorrencia> findByEmpregado_IdOrderByDataOcorrenciaDesc(Long idEmpregado);

    /** Somente fontes operacionais podem compor a classificação vigente da frequência. */
    @EntityGraph(attributePaths = {"empregado"})
    List<Ocorrencia> findByEmpregadoIdAndCanceladoEmIsNullAndDataOcorrenciaBetweenOrderByDataOcorrenciaDesc(
            Long empregadoId, LocalDate inicio, LocalDate fim);
}
