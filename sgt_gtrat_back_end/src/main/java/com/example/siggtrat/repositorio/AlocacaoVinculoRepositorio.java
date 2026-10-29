package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface AlocacaoVinculoRepositorio extends JpaRepository<AlocacaoVinculo, Long> {
    List<AlocacaoVinculo> findByVinculoIdOrderByDataInicioAscIdAsc(Long vinculoId);
    List<AlocacaoVinculo> findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(Long vinculoId, EstadoNegocialAlocacao estadoNegocial);
    List<AlocacaoVinculo> findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(Long contratoId);

    @EntityGraph(attributePaths = {"vinculo", "composicao", "lotacao", "jornada", "cargo"})
    @Query("select a from AlocacaoVinculo a where a.vinculo.id in :vinculoIds order by a.vinculo.id, a.dataInicio, a.id")
    List<AlocacaoVinculo> buscarPorVinculos(@Param("vinculoIds") Collection<Long> vinculoIds);

    @Query("select count(a) from AlocacaoVinculo a where a.cargo.id=:id and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA and a.vinculo.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO and (a.dataFim is null or a.dataFim>=:data)")
    long contarUsoProgramadoPorCargo(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(a) from AlocacaoVinculo a where a.lotacao.id=:id and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA and a.vinculo.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO and (a.dataFim is null or a.dataFim>=:data)")
    long contarUsoProgramadoPorLotacao(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(a) from AlocacaoVinculo a where a.jornada.id=:id and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA and a.vinculo.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO and (a.dataFim is null or a.dataFim>=:data)")
    long contarUsoProgramadoPorJornada(@Param("id") Long id, @Param("data") LocalDate data);

    @Query("""
        select count(distinct a.vinculo.id) from AlocacaoVinculo a
        where a.vinculo.contrato.id = :contratoId
          and a.lotacao.id = :lotacaoId and a.jornada.id = :jornadaId and a.cargo.id = :cargoId
          and a.vinculo.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and a.vinculo.contrato.ativo = true
          and a.vinculo.contrato.estadoNegocial = com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO
          and (a.vinculo.contrato.dataInicio is null or a.vinculo.contrato.dataInicio <= :data)
          and (a.vinculo.contrato.dataFim is null or a.vinculo.contrato.dataFim >= :data)
          and a.vinculo.dataInicio <= :data and (a.vinculo.dataFim is null or a.vinculo.dataFim >= :data)
          and a.dataInicio <= :data and (a.dataFim is null or a.dataFim >= :data)
          and a.estadoNegocial = com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
        """)
    long countOcupacao(@Param("contratoId") Long contratoId, @Param("lotacaoId") Long lotacaoId,
                       @Param("jornadaId") Long jornadaId, @Param("cargoId") Long cargoId,
                       @Param("data") LocalDate data);
}
