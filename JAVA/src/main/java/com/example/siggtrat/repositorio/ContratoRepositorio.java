package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface ContratoRepositorio extends JpaRepository<Contrato, Long> {

    /**
     * A coluna ativo representa somente a habilitação administrativa.
     */
    List<Contrato> findByAtivoTrue();

    List<Contrato> findAllByOrderByNumeroContratoAsc();

    /**
     * Consulta de apoio ainda usada pela carga de desenvolvimento. 
     * Validações negociais de unicidade usam número + ano.
     */
    Optional<Contrato> findByNumeroContratoIgnoreCase(String numeroContrato);

    Optional<Contrato> findByNumeroContratoIgnoreCaseAndAnoContrato(
            String numeroContrato,
            Integer anoContrato
    );

    List<Contrato> findByGestaoLocalIdOrderByNumeroContratoAsc(Long gestaoLocalId);

    /** Candidatos incluem responsabilidade transferida; a autorização temporal filtra o resultado. */
    @Query("""
            select c from Contrato c
            where c.gestaoLocal.id=:gestao
               or exists (select t.id from TransferenciaGestaoContrato t
                          where t.contrato.id=c.id and t.dataEfetiva<=:data
                            and (t.gestaoOrigem.id=:gestao or t.gestaoDestino.id=:gestao))
            order by c.numeroContrato
            """)
    List<Contrato> buscarCandidatosGestaoTemporal(@Param("gestao") Long gestaoLocalId, @Param("data") LocalDate data);

    @Query("""
            select c from Contrato c
            where c.gestaoLocal.superintendencia.id=:superintendencia
               or exists (select t.id from TransferenciaGestaoContrato t
                          where t.contrato.id=c.id and t.dataEfetiva<=:data
                            and (t.gestaoOrigem.superintendencia.id=:superintendencia
                                 or t.gestaoDestino.superintendencia.id=:superintendencia))
            order by c.numeroContrato
            """)
    List<Contrato> buscarCandidatosSuperintendenciaTemporal(@Param("superintendencia") Long superintendenciaId,
                                                          @Param("data") LocalDate data);

    /**
     * Serializa operações negociais críticas do mesmo contrato. O lock não
     * define precedência negocial; apenas evita duas confirmações concorrentes
     * sobre a mesma fotografia técnica.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> buscarPorIdParaAtualizacao(@Param("id") Long id);

    List<Contrato> findByGestaoLocalSuperintendenciaIdOrderByNumeroContratoAsc(Long superintendenciaId);

    boolean existsByGestaoLocalId(Long gestaoLocalId);

    boolean existsByTipoContratacaoId(Long tipoContratacaoId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(DISTINCT e.id)
              FROM Empregado e
              JOIN e.contratos c
             WHERE c.id = :contratoId
               AND UPPER(e.status.descricao) = 'ATIVO'
               AND e.gestaoLocal.id = c.gestaoLocal.id
            """)
    long countEmpregadosAtivosByContratoId(
            @org.springframework.data.repository.query.Param("contratoId") Long contratoId
    );

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT ec.id_contrato, COUNT(DISTINCT ec.id_empregado)
              FROM empregado_contrato ec
              JOIN empregado e ON e.id_empregado = ec.id_empregado
              JOIN status_empregado s ON s.id_status = e.id_status
             WHERE ec.id_contrato IN (:contratoIds)
               AND UPPER(s.descricao) = 'ATIVO'
             GROUP BY ec.id_contrato
            """, nativeQuery = true)
    List<Object[]> countEmpregadosAtivosByContratoIds(
            @org.springframework.data.repository.query.Param("contratoIds") Collection<Long> contratoIds
    );
}
