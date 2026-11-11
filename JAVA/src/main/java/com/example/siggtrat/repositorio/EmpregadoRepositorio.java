package com.example.siggtrat.repositorio;

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

import java.util.Collection;
import java.util.List;

@Repository
public interface EmpregadoRepositorio extends JpaRepository<Empregado, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Empregado> {

    java.util.Optional<Empregado> findByPessoaCanonicaId(Long pessoaId);
    java.util.Optional<Empregado> findByCpf(String cpf);

    @Query("select e.id from Empregado e join e.contratos c "
            + "where c.id = :contratoId and e.pessoaCanonica is not null order by e.id")
    List<Long> buscarIdsProjetadosNoContrato(@Param("contratoId") Long contratoId);

    interface IdentidadeCanonica {
        Long getEmpregadoId();
        Long getPessoaId();
    }

    @Query("select e.id as empregadoId, e.pessoaCanonica.id as pessoaId from Empregado e "
            + "where e.id in :ids and e.pessoaCanonica is not null")
    List<IdentidadeCanonica> buscarIdentidadesCanonicas(@Param("ids") Collection<Long> ids);

    /** Descobre a identidade sem carregar ou bloquear a projeção antes da Pessoa. */
    @Query("SELECT e.pessoaCanonica.id FROM Empregado e WHERE e.id = :id")
    java.util.Optional<Long> buscarPessoaCanonicaId(@Param("id") Long id);

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


    @Query("SELECT e.matricula FROM Empregado e WHERE e.matricula LIKE CONCAT(:prefixo, '%')")
    java.util.List<String> buscarMatriculasPorPrefixo(@Param("prefixo") String prefixo);

    long countByMatriculaStartingWith(String prefixo);

    @Query(value = """
            SELECT COALESCE(
                MIN(CAST(SUBSTRING(matricula, CHAR_LENGTH(:prefixo) + 1) AS BIGINT)),
                0
            )
            FROM empregado
            WHERE matricula LIKE CONCAT(:prefixo, '%')
            """, nativeQuery = true)
    long findMenorIndiceMatriculaDaCarga(@Param("prefixo") String prefixo);

    @Query(value = """
            SELECT COALESCE(
                MAX(CAST(SUBSTRING(matricula, CHAR_LENGTH(:prefixo) + 1) AS BIGINT)),
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



}
