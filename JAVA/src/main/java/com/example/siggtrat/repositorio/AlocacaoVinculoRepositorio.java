package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.AlocacaoVinculo;
import com.example.siggtrat.entidade.Contrato;
import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface AlocacaoVinculoRepositorio extends JpaRepository<AlocacaoVinculo, Long> {
    List<AlocacaoVinculo> findByVinculoIdOrderByDataInicioAscIdAsc(Long vinculoId);
    List<AlocacaoVinculo> findByVinculoIdAndEstadoNegocialOrderByDataInicioAscIdAsc(Long vinculoId, EstadoNegocialAlocacao estadoNegocial);
    List<AlocacaoVinculo> findByVinculoContratoIdOrderByVinculoIdAscDataInicioAscIdAsc(Long contratoId);

    interface PeriodoOcupacao {
        Long getVinculoId();
        LocalDate getInicioAlocacao();
        LocalDate getFimAlocacao();
        LocalDate getInicioVinculo();
        LocalDate getFimVinculo();
    }

    /** Fronteiras escalares da combinação, sem materializar todas as Pessoas/Alocações do contrato. */
    @Query("""
        select a.vinculo.id as vinculoId, a.dataInicio as inicioAlocacao, a.dataFim as fimAlocacao,
               a.vinculo.dataInicio as inicioVinculo, a.vinculo.dataFim as fimVinculo
        from AlocacaoVinculo a
        where a.vinculo.contrato.id=:contratoId
          and a.lotacao.id=:lotacaoId and a.jornada.id=:jornadaId and a.cargo.id=:cargoId
          and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
          and a.vinculo.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and a.vinculo.contrato.ativo=true
          and a.vinculo.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO
          and (a.dataFim is null or a.dataFim>=:inicio)
          and (a.vinculo.dataFim is null or a.vinculo.dataFim>=:inicio)
          and (cast(:fim as date) is null or (a.dataInicio<=:fim and a.vinculo.dataInicio<=:fim))
        order by a.dataInicio, a.vinculo.id, a.id
        """)
    List<PeriodoOcupacao> buscarPeriodosOcupacao(@Param("contratoId") Long contratoId,
            @Param("lotacaoId") Long lotacaoId, @Param("jornadaId") Long jornadaId,
            @Param("cargoId") Long cargoId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
        select distinct a.vinculo.contrato from AlocacaoVinculo a
        where a.lotacao.id=:lotacaoId and a.jornada.id=:jornadaId
          and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
          and a.vinculo.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and a.dataInicio<=:data and (a.dataFim is null or a.dataFim>=:data)
          and a.vinculo.dataInicio<=:data and (a.vinculo.dataFim is null or a.vinculo.dataFim>=:data)
        """)
    List<Contrato> buscarContratosDaGradeCanonica(@Param("data") LocalDate data,
            @Param("lotacaoId") Long lotacaoId, @Param("jornadaId") Long jornadaId);

    @Query("""
        select e.id, a from Empregado e, AlocacaoVinculo a
        join fetch a.vinculo v join fetch v.pessoa
        join fetch a.lotacao join fetch a.jornada join fetch a.cargo
        where e.id in :empregadoIds and e.pessoaCanonica.id=v.pessoa.id
          and v.contrato.id in :contratoIds
          and a.lotacao.id=:lotacaoId and a.jornada.id=:jornadaId
          and a.estadoNegocial=com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
          and v.estadoNegocial=com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and a.dataInicio<=:data and (a.dataFim is null or a.dataFim>=:data)
          and v.dataInicio<=:data and (v.dataFim is null or v.dataFim>=:data)
        """)
    List<Object[]> buscarContextosDaGradeCanonica(@Param("empregadoIds") Collection<Long> empregadoIds,
            @Param("contratoIds") Collection<Long> contratoIds, @Param("data") LocalDate data,
            @Param("lotacaoId") Long lotacaoId, @Param("jornadaId") Long jornadaId);

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
          and a.vinculo.dataInicio <= :data and (a.vinculo.dataFim is null or a.vinculo.dataFim >= :data)
          and a.dataInicio <= :data and (a.dataFim is null or a.dataFim >= :data)
          and a.estadoNegocial = com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao.REGISTRADA
        """)
    long countOcupacao(@Param("contratoId") Long contratoId, @Param("lotacaoId") Long lotacaoId,
                       @Param("jornadaId") Long jornadaId, @Param("cargoId") Long cargoId,
                       @Param("data") LocalDate data);
}
