package com.example.siggtrat.repositorio;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.dto.EmpregadoContratoListaDTO;
import com.example.siggtrat.entidade.Empregado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EmpregadoRepositorio extends JpaRepository<Empregado, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Empregado> {

    java.util.Optional<Empregado> findByPessoaCanonicaId(Long pessoaId);
    java.util.Optional<Empregado> findByCpf(String cpf);

    @Query("SELECT e.id FROM Empregado e ORDER BY e.id")
    List<Long> buscarTodosIdsOrdenadosPorId();

    @Query("SELECT DISTINCT e FROM Empregado e " +
            "LEFT JOIN FETCH e.contratos " +
            "LEFT JOIN FETCH e.lotacao " +
            "LEFT JOIN FETCH e.jornada " +
            "LEFT JOIN FETCH e.cargo " +
            "ORDER BY e.id")
    List<Empregado> buscarTodosComAlocacaoAtual();

    @Override
    @EntityGraph(attributePaths = {"lotacao", "jornada", "cargo", "status", "gestaoLocal", "gestaoLocal.superintendencia"})
    Page<Empregado> findAll(Specification<Empregado> spec, Pageable pageable);

    @Query("SELECT new com.example.siggtrat.dto.EmpregadoContratoListaDTO(" +
            "e.id, c.id, c.numeroContrato, c.descricao, c.ativo) " +
            "FROM Empregado e JOIN e.contratos c " +
            "WHERE e.id IN :empregadoIds " +
            "ORDER BY e.id, c.numeroContrato")
    List<EmpregadoContratoListaDTO> buscarContratosDaListaPorIdsEmpregados(
            @Param("empregadoIds") List<Long> empregadoIds);

    /**
     * Conta todos os vínculos atuais por contrato em uma única consulta agregada.
     * Evita usar o tamanho da página da listagem e evita uma consulta por contrato.
     */
    @Query("SELECT new com.example.siggtrat.dto.ContratoContagemDTO(" +
            "c.id, COUNT(DISTINCT e.id)) " +
            "FROM Empregado e JOIN e.contratos c " +
            "WHERE c.id IN :contratoIds " +
            "GROUP BY c.id")
    List<com.example.siggtrat.dto.ContratoContagemDTO> contarColaboradoresPorContrato(
            @Param("contratoIds") Collection<Long> contratoIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empregado e WHERE e.id = :id")
    java.util.Optional<Empregado> buscarPorIdParaAtualizacao(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empregado e WHERE e.id IN :ids ORDER BY e.id")
    List<Empregado> buscarTodosPorIdParaAtualizacao(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"jornada", "lotacao", "gestaoLocal", "gestaoLocal.superintendencia", "status"})
    @Query("SELECT DISTINCT e FROM Empregado e WHERE e.id IN :ids")
    List<Empregado> buscarTodosPorIdComContextoFrequencia(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT e FROM Empregado e LEFT JOIN FETCH e.contratos WHERE e.id IN :ids")
    List<Empregado> buscarTodosPorIdComContratos(@Param("ids") Collection<Long> ids);

    @Query("SELECT DISTINCT e FROM Empregado e LEFT JOIN FETCH e.contratos " +
            "WHERE e.matricula LIKE CONCAT(:prefixo, '%')")
    List<Empregado> buscarPorPrefixoMatriculaComContratos(@Param("prefixo") String prefixo);

    @Query("SELECT DISTINCT e FROM Empregado e " +
            "LEFT JOIN e.contratos c " +
            "LEFT JOIN e.jornada j " +
            "WHERE (:termo IS NULL OR (" +
            "   LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "   e.cpf LIKE CONCAT('%', :termo, '%') OR " +
            "   e.matricula LIKE CONCAT('%', :termo, '%')" +
            ")) " +
            "AND (:status IS NULL OR e.status.descricao = :status) " +
            "AND (:lotacaoId IS NULL OR e.lotacao.id = :lotacaoId) " +
            "AND (:cargoId IS NULL OR e.cargo.id = :cargoId) " +
            "AND (" +
            "   (:semContrato = true AND c.id IS NULL) OR " +
            "   (:semContrato = false AND (:contratoId IS NULL OR c.id = :contratoId))" +
            ") " +
            "AND (:restricaoLotacaoId IS NULL OR e.lotacao.id = :restricaoLotacaoId) " +
            "AND (:restricaoJornadaId IS NULL OR e.jornada.id = :restricaoJornadaId)")
    Page<Empregado> findComFiltros(
            @Param("termo") String termo,
            @Param("status") String status,
            @Param("lotacaoId") Long lotacaoId,
            @Param("cargoId") Long cargoId,
            @Param("contratoId") Long contratoId,
            @Param("semContrato") Boolean semContrato,
            @Param("restricaoLotacaoId") Long restricaoLotacaoId,
            @Param("restricaoJornadaId") Long restricaoJornadaId,
            Pageable pageable
    );

    // === QUERY ANTIGA (Corrigida para usar um @Param normal e evitar o erro do IntelliJ) ===
    @Query("SELECT DISTINCT e FROM Empregado e LEFT JOIN e.contratos c WHERE " +
            "e.status.descricao = :statusAtivo " +
            "AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef) " +
            "AND (:lotacaoId IS NULL OR e.lotacao.id = :lotacaoId) " +
            "AND (:turnoId IS NULL OR e.jornada.id = :turnoId) " +
            "AND (:contratoId IS NULL OR c.id = :contratoId) " +
            "AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(e.nome) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR e.cpf LIKE CONCAT('%', :searchTerm, '%'))")
    Page<Empregado> findParaFrequenciaInternal(
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("contratoId") Long contratoId,
            @Param("lotacaoId") Long lotacaoId,
            @Param("turnoId") Long turnoId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    // O Controller chama este método, que injeta a constante automaticamente na query interna
    default Page<Empregado> buscarParaFrequencia(
            LocalDate dataRef, Long contratoId, Long lotacaoId, Long turnoId, String searchTerm, Pageable pageable) {
        return findParaFrequenciaInternal(ConstantesDominio.STATUS_ATIVO, dataRef, contratoId, lotacaoId, turnoId, searchTerm, pageable);
    }

    // === NOVA QUERY OTIMIZADA COM DTO (Corrigida para usar um @Param normal) ===
    @Query("SELECT new com.example.siggtrat.dto.EmpregadoFrequenciaDTO(" +
            "e.id, e.nome, e.matricula, e.cpf, l.nome, l.id, j.descricao, j.id, c.nome) " +
            "FROM Empregado e " +
            "LEFT JOIN e.lotacao l " +
            "LEFT JOIN e.jornada j " +
            "LEFT JOIN e.cargo c " +
            "WHERE e.status.descricao = :statusAtivo " +
            "AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef) " +
            "AND (:lotacaoId IS NULL OR l.id = :lotacaoId) " +
            "AND (:turnoId IS NULL OR j.id = :turnoId) " +
            "AND EXISTS (SELECT 1 FROM e.contratos ct WHERE ct.ativo = true AND (:contratoId IS NULL OR ct.id = :contratoId)) " +
            "AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(e.nome) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR e.cpf LIKE CONCAT('%', :searchTerm, '%'))")
    Page<EmpregadoFrequenciaDTO> findFrequenciaGridInternal(
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("contratoId") Long contratoId,
            @Param("lotacaoId") Long lotacaoId,
            @Param("turnoId") Long turnoId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    // O Controller chama este método, que injeta a constante automaticamente na query interna
    default Page<EmpregadoFrequenciaDTO> buscarGradeFrequencia(
            LocalDate dataRef, Long contratoId, Long lotacaoId, Long turnoId, String searchTerm, Pageable pageable) {
        return findFrequenciaGridInternal(ConstantesDominio.STATUS_ATIVO, dataRef, contratoId, lotacaoId, turnoId, searchTerm, pageable);
    }



    @Query("""
            SELECT COUNT(e.id)
            FROM Empregado e
            WHERE e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            """)
    long countElegiveisParaFechamento(
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds);

    @Query("""
            SELECT COUNT(e.id)
            FROM Empregado e
            WHERE e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND NOT EXISTS (
                    SELECT f.id FROM Frequencia f
                    WHERE f.empregado = e AND f.dataReferencia = :dataRef
              )
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            """)
    long countElegiveisSemFrequencia(
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds);

    @Query("""
            SELECT e.id
            FROM Empregado e
            WHERE e.id > :aposId
              AND e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            ORDER BY e.id
            """)
    List<Long> findIdsElegiveisParaFechamentoApos(
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds,
            @Param("aposId") Long aposId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(e.id)
            FROM Empregado e
            WHERE e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND (
                    NOT EXISTS (
                        SELECT f.id FROM Frequencia f
                        WHERE f.empregado = e AND f.dataReferencia = :dataRef
                    )
                    OR EXISTS (
                        SELECT i.id FROM FechamentoFrequenciaItem i
                        WHERE i.fechamento.id = :fechamentoId AND i.empregado = e
                    )
              )
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            """)
    long countPendentesParaFechamento(
            @Param("fechamentoId") Long fechamentoId,
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds);

    @Query("""
            SELECT e.id
            FROM Empregado e
            WHERE e.id > :aposId
              AND e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND (
                    NOT EXISTS (
                        SELECT f.id FROM Frequencia f
                        WHERE f.empregado = e AND f.dataReferencia = :dataRef
                    )
                    OR EXISTS (
                        SELECT i.id FROM FechamentoFrequenciaItem i
                        WHERE i.fechamento.id = :fechamentoId AND i.empregado = e
                    )
              )
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            ORDER BY e.id
            """)
    List<Long> findIdsPendentesParaFechamentoApos(
            @Param("fechamentoId") Long fechamentoId,
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds,
            @Param("aposId") Long aposId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(e.id)
            FROM Empregado e
            WHERE e.id IN :empregadoIds
              AND e.status.descricao = :statusAtivo
              AND (e.dataAdmissao IS NULL OR e.dataAdmissao <= :dataRef)
              AND e.lotacao.id = :lotacaoId
              AND e.jornada.id = :jornadaId
              AND (
                    EXISTS (
                        SELECT h.id FROM HistoricoVinculoContrato h
                        WHERE h.empregado = e
                          AND h.dataInicio < :fimExclusivo
                          AND (h.dataFim IS NULL OR h.dataFim >= :inicioDia)
                          AND (:filtrarContratos = false OR h.contrato.id IN :contratoIds)
                    )
                    OR (
                        NOT EXISTS (SELECT h2.id FROM HistoricoVinculoContrato h2 WHERE h2.empregado = e)
                        AND EXISTS (
                            SELECT ct.id FROM e.contratos ct
                            WHERE ct.ativo = true
                              AND (ct.dataInicio IS NULL OR ct.dataInicio <= :dataRef)
                              AND (ct.dataFim IS NULL OR ct.dataFim >= :dataRef)
                              AND (:filtrarContratos = false OR ct.id IN :contratoIds)
                        )
                    )
              )
            """)
    long countIdsElegiveisParaFechamento(
            @Param("empregadoIds") Collection<Long> empregadoIds,
            @Param("statusAtivo") String statusAtivo,
            @Param("dataRef") LocalDate dataRef,
            @Param("inicioDia") java.time.ZonedDateTime inicioDia,
            @Param("fimExclusivo") java.time.ZonedDateTime fimExclusivo,
            @Param("lotacaoId") Long lotacaoId,
            @Param("jornadaId") Long jornadaId,
            @Param("filtrarContratos") boolean filtrarContratos,
            @Param("contratoIds") Collection<Long> contratoIds);


    @Query("SELECT e.id FROM Empregado e JOIN e.contratos c GROUP BY e.id HAVING COUNT(c.id) > 1 ORDER BY e.id")
    List<Long> buscarIdsComMultiplosContratos();

    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);
    boolean existsByContratosId(Long contratoId);
    long countDistinctByContratos_IdAndLotacao_IdAndJornada_IdAndCargo_IdAndStatus_DescricaoIgnoreCase(
            Long contratoId, Long lotacaoId, Long jornadaId, Long cargoId, String statusDescricao);
    boolean existsByJornada_Id(Long jornadaId);
    boolean existsByLotacao_Id(Long lotacaoId);
    boolean existsByStatus_Id(Long statusId);
    long countByStatus_Descricao(String descricao);
    long countByGestaoLocalId(Long gestaoLocalId);
    long countByGestaoLocalIdAndStatus_Descricao(Long gestaoLocalId, String descricao);
    long countByGestaoLocalSuperintendenciaId(Long superintendenciaId);
    long countByGestaoLocalSuperintendenciaIdAndStatus_Descricao(Long superintendenciaId, String descricao);
    @Query("SELECT COUNT(DISTINCT e.cargo.id) FROM Empregado e WHERE e.gestaoLocal.id = :gestaoLocalId")
    long contarCargosDistintosPorIdGestaoLocal(@Param("gestaoLocalId") Long gestaoLocalId);

    @Query("SELECT COUNT(DISTINCT e.cargo.id) FROM Empregado e WHERE e.gestaoLocal.superintendencia.id = :superintendenciaId")
    long contarCargosDistintosPorIdSuperintendencia(@Param("superintendenciaId") Long superintendenciaId);

    boolean existsByMatricula(String matricula);

    @Query("SELECT e.matricula FROM Empregado e WHERE e.matricula LIKE CONCAT(:prefixo, '%')")
    java.util.List<String> buscarMatriculasPorPrefixo(@Param("prefixo") String prefixo);

    long countByMatriculaStartingWith(String prefixo);

    @Query(value = """
            SELECT COALESCE(
                MIN(CAST(SUBSTRING(matricula, CHAR_LENGTH(:prefixo) + 1) AS UNSIGNED)),
                0
            )
            FROM empregado
            WHERE matricula LIKE CONCAT(:prefixo, '%')
            """, nativeQuery = true)
    long findMenorIndiceMatriculaDaCarga(@Param("prefixo") String prefixo);

    @Query(value = """
            SELECT COALESCE(
                MAX(CAST(SUBSTRING(matricula, CHAR_LENGTH(:prefixo) + 1) AS UNSIGNED)),
                0
            )
            FROM empregado
            WHERE matricula LIKE CONCAT(:prefixo, '%')
            """, nativeQuery = true)
    long findMaiorIndiceMatriculaDaCarga(@Param("prefixo") String prefixo);

    @Query("SELECT DISTINCT e FROM Empregado e " +
            "LEFT JOIN FETCH e.contratos " +
            "WHERE e.matricula IN :matriculas")
    List<Empregado> buscarPorMatriculasComContratos(
            @Param("matriculas") Collection<String> matriculas
    );

    @Query("SELECT e.cpf FROM Empregado e WHERE e.cpf IN :cpfs")
    List<String> buscarCpfsExistentes(@Param("cpfs") Collection<String> cpfs);

    @Query("SELECT e.cpf FROM Empregado e")
    java.util.List<String> buscarTodosCpfs();

    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    java.util.Optional<Empregado> findByConta_Id(Long contaId);

    java.util.Optional<Empregado> findByEndereco_Id(Long enderecoId);
}
