package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.HistoricoVinculoContrato;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public interface HistoricoVinculoContratoRepositorio extends JpaRepository<HistoricoVinculoContrato, Long> {

    @EntityGraph(attributePaths = {"empregado", "contrato", "contrato.gestaoLocal", "contrato.gestaoLocal.superintendencia"})
    List<HistoricoVinculoContrato> findByEmpregadoIdOrderByDataInicioDesc(Long idEmpregado);

    @EntityGraph(attributePaths = {"contrato"})
    List<HistoricoVinculoContrato> findByEmpregadoIdAndDataFimIsNullOrderByDataInicioDesc(Long idEmpregado);

    List<HistoricoVinculoContrato> findByEmpregadoIdAndContratoIdAndDataFimIsNullOrderByDataInicioDesc(
            Long idEmpregado,
            Long idContrato
    );

    boolean existsByContratoId(Long idContrato);

    boolean existsByEmpregadoId(Long idEmpregado);

    boolean existsByLotacao_Id(Long idLotacao);

    @Query("""
            SELECT h FROM HistoricoVinculoContrato h
            JOIN FETCH h.contrato c
            LEFT JOIN FETCH h.lotacao
            LEFT JOIN FETCH h.jornada
            LEFT JOIN FETCH h.cargo
            WHERE h.empregado.id = :empregadoId
              AND h.dataInicio < :fimExclusivo
              AND (h.dataFim IS NULL OR h.dataFim >= :inicio)
            ORDER BY h.dataInicio DESC
            """)
    List<HistoricoVinculoContrato> buscarVigentesNaData(
            @Param("empregadoId") Long empregadoId,
            @Param("inicio") ZonedDateTime inicio,
            @Param("fimExclusivo") ZonedDateTime fimExclusivo
    );


    @Query("""
            SELECT h FROM HistoricoVinculoContrato h
            JOIN FETCH h.contrato c
            LEFT JOIN FETCH h.lotacao
            LEFT JOIN FETCH h.jornada
            LEFT JOIN FETCH h.cargo
            WHERE h.empregado.id IN :empregadoIds
              AND h.dataInicio < :fimExclusivo
              AND (h.dataFim IS NULL OR h.dataFim >= :inicio)
            ORDER BY h.empregado.id, h.dataInicio DESC
            """)
    List<HistoricoVinculoContrato> buscarVigentesNoIntervalo(
            @Param("empregadoIds") Collection<Long> empregadoIds,
            @Param("inicio") ZonedDateTime inicio,
            @Param("fimExclusivo") ZonedDateTime fimExclusivo
    );

    @Query("""
            SELECT DISTINCT h FROM HistoricoVinculoContrato h
            JOIN FETCH h.empregado e
            JOIN FETCH h.contrato c
            LEFT JOIN FETCH c.gestaoLocal cg
            LEFT JOIN FETCH cg.superintendencia
            LEFT JOIN FETCH h.lotacao l
            LEFT JOIN FETCH l.gestaoLocal lg
            LEFT JOIN FETCH h.jornada j
            LEFT JOIN FETCH j.gestaoLocal jg
            LEFT JOIN FETCH h.cargo
            WHERE c.id IN :contratoIds
              AND h.dataInicio < :fimExclusivo
              AND (h.dataFim IS NULL OR h.dataFim >= :inicio)
            ORDER BY e.nome, h.dataInicio
            """)
    List<HistoricoVinculoContrato> buscarAlocacoesPorContratosNoIntervalo(
            @Param("contratoIds") Collection<Long> contratoIds,
            @Param("inicio") ZonedDateTime inicio,
            @Param("fimExclusivo") ZonedDateTime fimExclusivo
    );

    @Query("""
            SELECT DISTINCT h FROM HistoricoVinculoContrato h
            JOIN FETCH h.empregado e
            JOIN FETCH h.contrato c
            LEFT JOIN FETCH c.gestaoLocal cg
            LEFT JOIN FETCH cg.superintendencia
            LEFT JOIN FETCH h.lotacao l
            LEFT JOIN FETCH l.gestaoLocal lg
            LEFT JOIN FETCH h.jornada j
            LEFT JOIN FETCH j.gestaoLocal jg
            LEFT JOIN FETCH h.cargo
            WHERE e.id IN :empregadoIds
              AND c.id IN :contratoIds
              AND h.dataInicio < :fimExclusivo
              AND (h.dataFim IS NULL OR h.dataFim >= :inicio)
            ORDER BY e.nome, e.id, h.dataInicio DESC
            """)
    List<HistoricoVinculoContrato> buscarAlocacoesPorEmpregadosEContratosNoIntervalo(
            @Param("empregadoIds") Collection<Long> empregadoIds,
            @Param("contratoIds") Collection<Long> contratoIds,
            @Param("inicio") ZonedDateTime inicio,
            @Param("fimExclusivo") ZonedDateTime fimExclusivo
    );

    @Query("SELECT DISTINCT h.empregado.id FROM HistoricoVinculoContrato h WHERE h.empregado.id IN :empregadoIds")
    List<Long> buscarIdsEmpregadosComHistorico(@Param("empregadoIds") Collection<Long> empregadoIds);

    List<HistoricoVinculoContrato> findByVinculoAtivoIsNullAndDataFimIsNull();


    @Query("SELECT DISTINCT h FROM HistoricoVinculoContrato h " +
            "JOIN FETCH h.empregado e " +
            "JOIN FETCH h.contrato c " +
            "LEFT JOIN FETCH h.lotacao " +
            "LEFT JOIN FETCH h.jornada " +
            "LEFT JOIN FETCH h.cargo " +
            "WHERE e.id IN :empregadoIds " +
            "AND h.dataFim IS NULL")
    List<HistoricoVinculoContrato> buscarAtivosPorIdsEmpregados(
            @Param("empregadoIds") Collection<Long> empregadoIds
    );

    @Query("SELECT h FROM HistoricoVinculoContrato h " +
            "JOIN FETCH h.empregado e " +
            "JOIN FETCH h.contrato c " +
            "WHERE e.matricula LIKE CONCAT(:prefixo, '%') " +
            "AND h.dataFim IS NULL")
    List<HistoricoVinculoContrato> buscarAtivosPorPrefixoMatriculaEmpregado(@Param("prefixo") String prefixo);
}
