package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.PainelCalculo;
import com.example.siggtrat.entidade.PainelCalculoStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PainelCalculoRepositorio extends JpaRepository<PainelCalculo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PainelCalculo p WHERE p.chaveConsulta = :chave")
    Optional<PainelCalculo> buscarPorChaveParaAtualizacao(@Param("chave") String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PainelCalculo p WHERE p.id = :id")
    Optional<PainelCalculo> buscarPorIdParaAtualizacao(@Param("id") Long id);

    List<PainelCalculo> findTop20ByStatusOrderByAtualizadoEmAsc(PainelCalculoStatus status);

    @Query("SELECT p FROM PainelCalculo p WHERE p.status = :status AND " +
            "(p.heartbeatEm IS NULL OR p.heartbeatEm < :limite)")
    List<PainelCalculo> buscarProcessamentosTravados(
            @Param("status") PainelCalculoStatus status,
            @Param("limite") LocalDateTime limite);
}
