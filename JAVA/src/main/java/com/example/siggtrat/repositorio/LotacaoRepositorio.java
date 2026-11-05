package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Lotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotacaoRepositorio extends JpaRepository<Lotacao, Long> {
    @Query("SELECT DISTINCT l FROM Lotacao l LEFT JOIN FETCH l.turnos WHERE l.id IN :ids ORDER BY l.nome")
    List<Lotacao> buscarTodosComTurnosPorIds(@Param("ids") List<Long> ids);
    List<Lotacao> findByTurnos_Id(Long idJornada);
    List<Lotacao> findByGestaoLocalIdOrderByNomeAsc(Long gestaoLocalId);
    Optional<Lotacao> findByNomeIgnoreCaseAndGestaoLocalId(String nome, Long gestaoLocalId);
    List<Lotacao> findByGestaoLocalSuperintendenciaIdOrderByNomeAsc(Long superintendenciaId);
    boolean existsByGestaoLocalId(Long gestaoLocalId);
    long countByGestaoLocalId(Long gestaoLocalId);
    long countByGestaoLocalSuperintendenciaId(Long superintendenciaId);
}
