package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.CalendarioEvento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarioEventoRepositorio extends JpaRepository<CalendarioEvento, Long> {
    List<CalendarioEvento> findByData(LocalDate data);
    @EntityGraph(attributePaths = {"gestaoLocal", "gestaoLocal.superintendencia", "superintendencia", "lotacao"})
    List<CalendarioEvento> findByDataBetween(LocalDate inicio, LocalDate fim);
    List<CalendarioEvento> findByDataBetweenAndGestaoLocalId(LocalDate inicio, LocalDate fim, Long gestaoLocalId);
    List<CalendarioEvento> findByDataBetweenAndSuperintendenciaId(LocalDate inicio, LocalDate fim, Long superintendenciaId);
    List<CalendarioEvento> findByDataAndGestaoLocalId(LocalDate data, Long gestaoLocalId);
    List<CalendarioEvento> findByDataAndSuperintendenciaId(LocalDate data, Long superintendenciaId);

    @Query("""
            select (count(e) > 0) from CalendarioEvento e
             where e.data = :data
               and lower(e.tipo) = lower(:tipo)
               and e.gestaoLocal.id = :gestaoLocalId
               and e.lotacao.id = :lotacaoId
            """)
    boolean existsByDataAndTipoIgnoreCaseAndGestaoLocalIdAndLotacaoId(
            @Param("data") LocalDate data, @Param("tipo") String tipo,
            @Param("gestaoLocalId") Long gestaoLocalId, @Param("lotacaoId") Long lotacaoId);

    @Query("""
            select (count(e) > 0) from CalendarioEvento e
             where e.data = :data
               and lower(e.tipo) = lower(:tipo)
               and e.gestaoLocal.id = :gestaoLocalId
               and e.lotacao.id = :lotacaoId
               and e.id <> :id
            """)
    boolean existsByDataAndTipoIgnoreCaseAndGestaoLocalIdAndLotacaoIdAndIdNot(
            @Param("data") LocalDate data, @Param("tipo") String tipo,
            @Param("gestaoLocalId") Long gestaoLocalId, @Param("lotacaoId") Long lotacaoId,
            @Param("id") Long id);

    boolean existsByDataAndTipoIgnoreCaseAndGestaoLocalIsNullAndSuperintendenciaIsNull(
            LocalDate data, String tipo);

    boolean existsByDataAndTipoIgnoreCaseAndGestaoLocalIsNullAndSuperintendenciaIsNullAndIdNot(
            LocalDate data, String tipo, Long id);

    boolean existsByDataAndTipoIgnoreCaseAndAtivoTrue(LocalDate data, String tipo);

    @Query("select (count(e) > 0) from CalendarioEvento e where e.lotacao.id = :lotacaoId")
    boolean existsByLotacaoId(@Param("lotacaoId") Long lotacaoId);
}
