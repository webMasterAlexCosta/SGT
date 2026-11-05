package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Collection;

import java.util.List;
import java.util.Optional;

@Repository
public interface JornadaRepositorio extends JpaRepository<Jornada, Long> {
    /** Retorna os turnos vinculados à lotação pela tabela JPA lotacao_jornada. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct j from Jornada j join j.lotacoes l where l.id = :lotacaoId"
    )
    List<Jornada> findByLotacaoOrLotacoes(
            @org.springframework.data.repository.query.Param("lotacaoId") Long lotacaoId);

    List<Jornada> findByGestaoLocalIdOrderByDescricaoAsc(Long gestaoLocalId);
    List<Jornada> findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(Long superintendenciaId);
    Optional<Jornada> findByDescricaoIgnoreCaseAndGestaoLocalId(String descricao, Long gestaoLocalId);

    List<Jornada> findByLotacoes_Id(Long lotacaoId);

    @EntityGraph(attributePaths = {"dias"})
    List<Jornada> findDistinctByIdIn(Collection<Long> ids);
}