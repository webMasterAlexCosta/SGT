package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface PessoaRepositorio extends JpaRepository<Pessoa, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Pessoa> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pessoa p where p.id = :id")
    Optional<Pessoa> buscarPorIdParaAtualizacao(@Param("id") Long id);
    Optional<Pessoa> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    List<Pessoa> findByCpfIn(Collection<String> cpfs);
    List<Pessoa> findAllByOrderByNomeAscIdAsc();

    @Query(value = """
        select distinct p from Pessoa p
        join VinculoContratual v on v.pessoa = p
        where (:acessoGlobal = true
            or (:gestaoLocalId is not null and v.contrato.gestaoLocal.id = :gestaoLocalId)
            or (:superintendenciaId is not null and v.contrato.gestaoLocal.superintendencia.id = :superintendenciaId))
          and (:contratoId is null or v.contrato.id = :contratoId)
          and (cast(:ativoEm as date) is null or (v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                and v.dataInicio <= :ativoEm and (v.dataFim is null or v.dataFim >= :ativoEm)))
          and (:busca is null or lower(p.nome) like concat('%', :busca, '%')
               or (:buscaCpf is not null and p.cpf like concat('%', :buscaCpf, '%')))
        """,
        countQuery = """
        select count(distinct p.id) from Pessoa p
        join VinculoContratual v on v.pessoa = p
        where (:acessoGlobal = true
            or (:gestaoLocalId is not null and v.contrato.gestaoLocal.id = :gestaoLocalId)
            or (:superintendenciaId is not null and v.contrato.gestaoLocal.superintendencia.id = :superintendenciaId))
          and (:contratoId is null or v.contrato.id = :contratoId)
          and (cast(:ativoEm as date) is null or (v.estadoNegocial = com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo.REGISTRADO
                and v.dataInicio <= :ativoEm and (v.dataFim is null or v.dataFim >= :ativoEm)))
          and (:busca is null or lower(p.nome) like concat('%', :busca, '%')
               or (:buscaCpf is not null and p.cpf like concat('%', :buscaCpf, '%')))
        """)
    Page<Pessoa> pesquisarNoEscopo(@Param("acessoGlobal") boolean acessoGlobal,
            @Param("gestaoLocalId") Long gestaoLocalId,
            @Param("superintendenciaId") Long superintendenciaId,
            @Param("contratoId") Long contratoId, @Param("ativoEm") LocalDate ativoEm,
            @Param("busca") String busca, @Param("buscaCpf") String buscaCpf, Pageable pageable);
}
