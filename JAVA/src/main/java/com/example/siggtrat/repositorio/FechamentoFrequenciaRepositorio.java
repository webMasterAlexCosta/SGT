package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.FechamentoFrequencia;
import com.example.siggtrat.entidade.FechamentoFrequenciaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FechamentoFrequenciaRepositorio extends JpaRepository<FechamentoFrequencia, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FechamentoFrequencia f WHERE f.chaveEscopo = :chave")
    Optional<FechamentoFrequencia> buscarPorChaveEscopoParaAtualizacao(@Param("chave") String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FechamentoFrequencia f WHERE f.id = :id")
    Optional<FechamentoFrequencia> buscarPorIdParaAtualizacao(@Param("id") Long id);

    @EntityGraph(attributePaths = {"lotacao", "jornada", "usuarioCriador"})
    @Query("SELECT f FROM FechamentoFrequencia f WHERE f.id = :id")
    Optional<FechamentoFrequencia> buscarDetalhadoPorId(@Param("id") Long id);

    @EntityGraph(attributePaths = {"lotacao", "jornada", "usuarioCriador"})
    @Query("SELECT f FROM FechamentoFrequencia f WHERE f.chaveEscopo = :chave")
    Optional<FechamentoFrequencia> buscarDetalhadoPorChaveEscopo(@Param("chave") String chaveEscopo);

    List<FechamentoFrequencia> findTop50ByStatusOrderByCriadoEmAsc(FechamentoFrequenciaStatus status);

    // A seleção e a transição do lease pertencem à mesma transação. Dois
    // recuperadores não podem carregar a mesma versão para reenfileirá-la.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FechamentoFrequencia f WHERE f.status = :status AND " +
            "(f.heartbeatEm IS NULL OR f.heartbeatEm < :limite) ORDER BY f.id")
    List<FechamentoFrequencia> buscarTravados(
            @Param("status") FechamentoFrequenciaStatus status,
            @Param("limite") LocalDateTime limite);

    List<FechamentoFrequencia> findByStatusIn(Collection<FechamentoFrequenciaStatus> statuses);
}
