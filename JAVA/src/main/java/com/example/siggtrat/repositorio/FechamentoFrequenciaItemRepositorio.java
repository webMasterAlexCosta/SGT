package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.FechamentoFrequenciaItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FechamentoFrequenciaItemRepositorio extends JpaRepository<FechamentoFrequenciaItem, Long> {

    @Modifying
    @Query("DELETE FROM FechamentoFrequenciaItem i WHERE i.fechamento.id = :fechamentoId")
    int excluirPorIdFechamento(@Param("fechamentoId") Long fechamentoId);

    @EntityGraph(attributePaths = {"empregado"})
    @Query("SELECT i FROM FechamentoFrequenciaItem i WHERE i.fechamento.id = :fechamentoId " +
            "AND i.empregado.id IN :empregadoIds")
    List<FechamentoFrequenciaItem> buscarPorIdFechamentoEIdsEmpregados(
            @Param("fechamentoId") Long fechamentoId,
            @Param("empregadoIds") Collection<Long> empregadoIds);


    @Query("SELECT i.empregado.id FROM FechamentoFrequenciaItem i " +
            "WHERE i.fechamento.id = :fechamentoId AND i.empregado.id > :aposId " +
            "ORDER BY i.empregado.id")
    List<Long> buscarIdsEmpregadosApos(
            @Param("fechamentoId") Long fechamentoId,
            @Param("aposId") Long aposId,
            Pageable pageable);

    long countByFechamentoId(Long fechamentoId);
}
