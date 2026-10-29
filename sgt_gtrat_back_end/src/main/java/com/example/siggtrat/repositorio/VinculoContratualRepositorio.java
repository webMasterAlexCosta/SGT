package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.entidade.VinculoContratual;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VinculoContratualRepositorio extends JpaRepository<VinculoContratual, Long> {
    List<VinculoContratual> findByPessoaIdOrderByDataInicioAscIdAsc(Long pessoaId);
    List<VinculoContratual> findByContratoIdOrderByDataInicioAscIdAsc(Long contratoId);
    List<VinculoContratual> findByPessoaIdAndEstadoNegocialNotOrderByDataInicioAscIdAsc(Long pessoaId, EstadoNegocialVinculo estado);
    boolean existsByContratoId(Long contratoId);
    boolean existsByPessoaId(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "contrato", "contrato.gestaoLocal",
            "contrato.gestaoLocal.superintendencia", "motivoDesligamento"})
    @Query(value = """
        select v from VinculoContratual v
        where (:acessoGlobal = true
            or (:gestaoLocalId is not null and v.contrato.gestaoLocal.id = :gestaoLocalId)
            or (:superintendenciaId is not null and v.contrato.gestaoLocal.superintendencia.id = :superintendenciaId))
          and (:contratoId is null or v.contrato.id = :contratoId)
          and (:pessoaId is null or v.pessoa.id = :pessoaId)
          and (:estado is null or v.estadoNegocial = :estado)
          and (:inicioDe is null or v.dataFim is null or v.dataFim >= :inicioDe)
          and (:inicioAte is null or v.dataInicio <= :inicioAte)
          and (:ativoEm is null or (v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                and v.dataInicio <= :ativoEm and (v.dataFim is null or v.dataFim >= :ativoEm)))
          and (:busca is null or lower(v.pessoa.nome) like concat('%', :busca, '%')
               or (:buscaCpf is not null and v.pessoa.cpf like concat('%', :buscaCpf, '%'))
               or lower(coalesce(v.matricula, '')) like concat('%', :busca, '%'))
        """,
        countQuery = """
        select count(v.id) from VinculoContratual v
        where (:acessoGlobal = true
            or (:gestaoLocalId is not null and v.contrato.gestaoLocal.id = :gestaoLocalId)
            or (:superintendenciaId is not null and v.contrato.gestaoLocal.superintendencia.id = :superintendenciaId))
          and (:contratoId is null or v.contrato.id = :contratoId)
          and (:pessoaId is null or v.pessoa.id = :pessoaId)
          and (:estado is null or v.estadoNegocial = :estado)
          and (:inicioDe is null or v.dataFim is null or v.dataFim >= :inicioDe)
          and (:inicioAte is null or v.dataInicio <= :inicioAte)
          and (:ativoEm is null or (v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                and v.dataInicio <= :ativoEm and (v.dataFim is null or v.dataFim >= :ativoEm)))
          and (:busca is null or lower(v.pessoa.nome) like concat('%', :busca, '%')
               or (:buscaCpf is not null and v.pessoa.cpf like concat('%', :buscaCpf, '%'))
               or lower(coalesce(v.matricula, '')) like concat('%', :busca, '%'))
        """)
    Page<VinculoContratual> pesquisarNoEscopo(@Param("acessoGlobal") boolean acessoGlobal,
            @Param("gestaoLocalId") Long gestaoLocalId, @Param("superintendenciaId") Long superintendenciaId,
            @Param("contratoId") Long contratoId, @Param("pessoaId") Long pessoaId,
            @Param("estado") EstadoNegocialVinculo estado, @Param("inicioDe") LocalDate inicioDe,
            @Param("inicioAte") LocalDate inicioAte, @Param("ativoEm") LocalDate ativoEm,
            @Param("busca") String busca, @Param("buscaCpf") String buscaCpf, Pageable pageable);

    @EntityGraph(attributePaths = {"pessoa", "contrato", "contrato.gestaoLocal", "contrato.gestaoLocal.superintendencia"})
    @Query("""
        select v from VinculoContratual v
        where v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and v.dataFim is null
          and v.contrato.dataFim is not null
          and v.contrato.dataFim < :hoje
        order by v.contrato.dataFim asc, v.id asc
        """)
    List<VinculoContratual> buscarAbertosAposFimNatural(@Param("hoje") LocalDate hoje);

    @Query("""
        select count(v) from VinculoContratual v
        where v.contrato.id = :contratoId
          and v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and v.contrato.ativo = true
          and v.contrato.estadoNegocial = com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO
          and (v.contrato.dataInicio is null or v.contrato.dataInicio <= :data)
          and (v.contrato.dataFim is null or v.contrato.dataFim >= :data)
          and v.dataInicio <= :data
          and (v.dataFim is null or v.dataFim >= :data)
        """)
    long countOperacionaisNoContrato(@Param("contratoId") Long contratoId, @Param("data") LocalDate data);

    @Query("""
        select v.contrato.id, count(v) from VinculoContratual v
        where v.contrato.id in :contratoIds
          and v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and v.contrato.ativo = true
          and v.contrato.estadoNegocial = com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO
          and (v.contrato.dataInicio is null or v.contrato.dataInicio <= :data)
          and (v.contrato.dataFim is null or v.contrato.dataFim >= :data)
          and v.dataInicio <= :data
          and (v.dataFim is null or v.dataFim >= :data)
        group by v.contrato.id
        """)
    List<Object[]> contarOperacionaisPorContrato(@Param("contratoIds") Collection<Long> contratoIds,
                                                  @Param("data") LocalDate data);
}
