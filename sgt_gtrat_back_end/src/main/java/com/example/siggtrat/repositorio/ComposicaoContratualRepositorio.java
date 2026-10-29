package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ComposicaoContratual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComposicaoContratualRepositorio extends JpaRepository<ComposicaoContratual, Long> {
    List<ComposicaoContratual> findByContratoIdOrderByLotacaoNomeAscJornadaDescricaoAscCargoNomeAsc(Long contratoId);
    boolean existsByContratoId(Long contratoId);
    void deleteByContratoId(Long contratoId);

    @Query("select count(c) from ComposicaoContratual c where c.cargo.id=:id and c.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and (c.contrato.dataFim is null or c.contrato.dataFim>=:data) and (c.dataFimVigencia is null or c.dataFimVigencia>=:data)")
    long contarUsoProgramadoPorCargo(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(c) from ComposicaoContratual c where c.lotacao.id=:id and c.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and (c.contrato.dataFim is null or c.contrato.dataFim>=:data) and (c.dataFimVigencia is null or c.dataFimVigencia>=:data)")
    long contarUsoProgramadoPorLotacao(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(c) from ComposicaoContratual c where c.jornada.id=:id and c.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and (c.contrato.dataFim is null or c.contrato.dataFim>=:data) and (c.dataFimVigencia is null or c.dataFimVigencia>=:data)")
    long contarUsoProgramadoPorJornada(@Param("id") Long id, @Param("data") LocalDate data);

    @org.springframework.data.jpa.repository.Query("""
            select c from ComposicaoContratual c
             where c.contrato.id = :contratoId
               and c.dataInicioVigencia <= :data
               and (c.dataFimVigencia is null or c.dataFimVigencia >= :data)
             order by c.lotacao.nome, c.jornada.descricao, c.cargo.nome
            """)
    List<ComposicaoContratual> findValidasEm(
            @org.springframework.data.repository.query.Param("contratoId") Long contratoId,
            @org.springframework.data.repository.query.Param("data") LocalDate data);
}
