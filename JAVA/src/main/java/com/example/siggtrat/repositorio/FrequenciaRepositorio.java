package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Frequencia;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FrequenciaRepositorio extends JpaRepository<Frequencia, Long>, JpaSpecificationExecutor<Frequencia> {

    @Override
    @EntityGraph(attributePaths = {"empregado"})
    Page<Frequencia> findAll(Specification<Frequencia> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferencia(LocalDate data);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaAndEmpregadoLotacaoId(LocalDate data, Long lotacaoId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaAndEmpregadoGestaoLocalId(LocalDate data, Long gestaoLocalId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaAndEmpregadoGestaoLocalSuperintendenciaId(LocalDate data, Long superintendenciaId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaAndEmpregadoLotacaoIdAndEmpregadoJornadaId(LocalDate data, Long lotacaoId, Long jornadaId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaBetween(LocalDate dataInicio, LocalDate dataFim);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaBetweenAndEmpregadoLotacaoId(LocalDate dataInicio, LocalDate dataFim, Long lotacaoId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaBetweenAndEmpregadoGestaoLocalId(LocalDate dataInicio, LocalDate dataFim, Long gestaoLocalId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaBetweenAndEmpregadoGestaoLocalSuperintendenciaId(LocalDate dataInicio, LocalDate dataFim, Long superintendenciaId);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByDataReferenciaBetweenAndEmpregadoLotacaoIdAndEmpregadoJornadaId(LocalDate dataInicio, LocalDate dataFim, Long lotacaoId, Long jornadaId);


    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByEmpregadoIdInAndDataReferenciaBetween(
            java.util.Collection<Long> empregadoIds, LocalDate inicio, LocalDate fim);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByEmpregadoIdInAndDataReferenciaBetweenAndValidadeOperacional(
            java.util.Collection<Long> empregadoIds, LocalDate inicio, LocalDate fim,
            ValidadeOperacionalFrequencia validadeOperacional);

    @EntityGraph(attributePaths = {"empregado"})
    List<Frequencia> findByEmpregadoIdAndDataReferenciaBetween(
            Long empregadoId, LocalDate inicio, LocalDate fim);



    @Query("SELECT DISTINCT f.empregado.id FROM Frequencia f " +
            "WHERE f.empregado.id IN :empregadoIds " +
            "AND f.dataReferencia BETWEEN :inicio AND :fim " +
            "AND f.validadeOperacional = com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia.VALIDA " +
            "AND UPPER(f.status) = 'FALTA'")
    List<Long> buscarIdsEmpregadosComFaltaNoPeriodo(
            @org.springframework.data.repository.query.Param("empregadoIds") java.util.Collection<Long> empregadoIds,
            @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
            @org.springframework.data.repository.query.Param("fim") LocalDate fim);

    Optional<Frequencia> findByEmpregadoIdAndDataReferencia(Long empregadoId, LocalDate dataReferencia);
    Optional<Frequencia> findByVinculoContratualIdAndDataReferencia(Long vinculoId, LocalDate dataReferencia);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Frequencia f where f.id = :id")
    Optional<Frequencia> buscarPorIdParaAtualizacao(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("select f.contratoSnapshotId from Frequencia f where f.id = :id")
    Optional<Long> buscarContratoSnapshotId(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("select f.id from Frequencia f where f.estadoAuditoria = " +
            "com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia.SUJEITA_REVISAO " +
            "and f.id > :ultimoId order by f.id")
    List<Long> buscarIdsSujeitasRevisaoApos(@org.springframework.data.repository.query.Param("ultimoId") long ultimoId,
                                          Pageable pagina);

    boolean existsByEmpregadoAndDataReferencia(Empregado empregado, LocalDate dataReferencia);

    Optional<Frequencia> findByEmpregadoAndDataReferencia(Empregado empregado, LocalDate dataReferencia);
    List<Frequencia> findByVinculoContratualIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(Long vinculoId, LocalDate data);

    List<Frequencia> findByVinculoContratualIdAndDataReferenciaBetweenOrderByDataReferenciaAscIdAsc(Long vinculoId, LocalDate inicio, LocalDate fim);

    List<Frequencia> findByContratoSnapshotIdAndDataReferenciaGreaterThanEqualOrderByDataReferenciaAscIdAsc(
            Long contratoId, LocalDate data);

    @EntityGraph(attributePaths = {"empregado", "pessoa"})
    List<Frequencia> findByEstadoAuditoriaAndDataReferenciaBetweenOrderByDataReferenciaAscIdAsc(
            EstadoAuditoriaFrequencia estado, LocalDate inicio, LocalDate fim);

    List<Frequencia> findByEstadoNegocialAndDataReferenciaLessThanEqual(
            com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia estado, LocalDate dataLimite);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Frequencia f where f.estadoNegocial = :estado " +
            "and f.dataReferencia <= :limite and f.id > :ultimoId order by f.id")
    List<Frequencia> buscarAbertasVencidasApos(
            @org.springframework.data.repository.query.Param("estado") com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia estado,
            @org.springframework.data.repository.query.Param("limite") LocalDate limite,
            @org.springframework.data.repository.query.Param("ultimoId") long ultimoId,
            Pageable pagina);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Frequencia f where f.vinculoContratual.id = :origemId " +
            "and f.dataReferencia >= :desde and f.id > :ultimoId order by f.id")
    List<Frequencia> buscarPorVinculoApos(
            @org.springframework.data.repository.query.Param("origemId") Long origemId,
            @org.springframework.data.repository.query.Param("desde") LocalDate desde,
            @org.springframework.data.repository.query.Param("ultimoId") long ultimoId, Pageable pagina);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Frequencia f where f.contratoSnapshotId = :origemId " +
            "and f.dataReferencia >= :desde and f.id > :ultimoId order by f.id")
    List<Frequencia> buscarPorContratoApos(
            @org.springframework.data.repository.query.Param("origemId") Long origemId,
            @org.springframework.data.repository.query.Param("desde") LocalDate desde,
            @org.springframework.data.repository.query.Param("ultimoId") long ultimoId, Pageable pagina);

}
