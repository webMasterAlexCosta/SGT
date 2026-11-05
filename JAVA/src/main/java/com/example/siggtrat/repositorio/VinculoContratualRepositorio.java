package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.entidade.VinculoContratual;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;

public interface VinculoContratualRepositorio extends JpaRepository<VinculoContratual, Long>, JpaSpecificationExecutor<VinculoContratual> {
    interface IdentidadeVinculo {
        Long getContratoId();
        Long getPessoaId();
    }

    /** Descobre chaves imutáveis sem carregar uma projeção mutável antes dos locks. */
    @Query("select v.contrato.id as contratoId, v.pessoa.id as pessoaId from VinculoContratual v where v.id = :id")
    Optional<IdentidadeVinculo> buscarIdentidade(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VinculoContratual v where v.id = :id")
    Optional<VinculoContratual> buscarPorIdParaAtualizacao(@Param("id") Long id);

    interface PeriodoRegistrado {
        Long getId();
        LocalDate getDataInicio();
        LocalDate getDataFim();
    }

    /** Leitura atual, mesmo em transação externa REPEATABLE READ; não é DDL. */
    @Query(value = """
            select id_vinculo_contratual as id, data_inicio as dataInicio, data_fim as dataFim
              from vinculo_contratual
             where id_pessoa = :pessoaId and estado_negocial = 'REGISTRADO'
             order by id_vinculo_contratual
             for update
            """, nativeQuery = true)
    List<PeriodoRegistrado> buscarPeriodosRegistradosParaAtualizacao(@Param("pessoaId") Long pessoaId);

    List<VinculoContratual> findByPessoaIdOrderByDataInicioAscIdAsc(Long pessoaId);

    @EntityGraph(attributePaths = {"pessoa", "contrato", "contrato.gestaoLocal", "contrato.gestaoLocal.superintendencia"})
    @Query("select v from VinculoContratual v where v.pessoa.id in :pessoaIds order by v.pessoa.id, v.dataInicio, v.id")
    List<VinculoContratual> buscarPorPessoasComContexto(@Param("pessoaIds") Collection<Long> pessoaIds);
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
          and (:inicioDe is null or v.dataInicio >= :inicioDe)
          and (:inicioAte is null or v.dataInicio <= :inicioAte)
          and (:fimDe is null or v.dataFim >= :fimDe)
          and (:fimAte is null or v.dataFim <= :fimAte)
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
          and (:inicioDe is null or v.dataInicio >= :inicioDe)
          and (:inicioAte is null or v.dataInicio <= :inicioAte)
          and (:fimDe is null or v.dataFim >= :fimDe)
          and (:fimAte is null or v.dataFim <= :fimAte)
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
            @Param("inicioAte") LocalDate inicioAte, @Param("fimDe") LocalDate fimDe,
            @Param("fimAte") LocalDate fimAte, @Param("ativoEm") LocalDate ativoEm,
            @Param("busca") String busca, @Param("buscaCpf") String buscaCpf, Pageable pageable);

    @EntityGraph(attributePaths = {"pessoa", "contrato", "contrato.gestaoLocal", "contrato.gestaoLocal.superintendencia"})
    @Query("""
        select v from VinculoContratual v
        where v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and v.dataFim is null
          and (v.contrato.dataFim < :hoje
               or exists (select h.id from VersaoContratoNegocial h
                          where h.contrato.id = v.contrato.id and h.dataFim < :hoje)
               or exists (select a.id from AditamentoContratual a
                          where a.contrato.id = v.contrato.id and a.novaDataFim < :hoje))
          and (v.contrato.id > :ultimoContrato
               or (v.contrato.id = :ultimoContrato and v.id > :ultimoVinculo))
        order by v.contrato.id asc, v.id asc
        """)
    List<VinculoContratual> buscarCandidatosFimNatural(@Param("hoje") LocalDate hoje,
            @Param("ultimoContrato") Long ultimoContrato, @Param("ultimoVinculo") Long ultimoVinculo,
            Pageable pageable);

    @EntityGraph(attributePaths = {"pessoa", "contrato", "contrato.gestaoLocal", "contrato.gestaoLocal.superintendencia"})
    @Query("""
        select v from VinculoContratual v
        where v.contrato.id in :contratoIds
          and v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
          and v.dataFim is null
          and (v.contrato.dataFim < :hoje
               or exists (select h.id from VersaoContratoNegocial h
                          where h.contrato.id = v.contrato.id and h.dataFim < :hoje)
               or exists (select a.id from AditamentoContratual a
                          where a.contrato.id = v.contrato.id and a.novaDataFim < :hoje))
          and (v.contrato.id > :ultimoContrato
               or (v.contrato.id = :ultimoContrato and v.id > :ultimoVinculo))
        order by v.contrato.id asc, v.id asc
        """)
    List<VinculoContratual> buscarCandidatosFimNaturalNoRecorte(@Param("contratoIds") Collection<Long> contratoIds,
            @Param("hoje") LocalDate hoje, @Param("ultimoContrato") Long ultimoContrato,
            @Param("ultimoVinculo") Long ultimoVinculo, Pageable pageable);

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
