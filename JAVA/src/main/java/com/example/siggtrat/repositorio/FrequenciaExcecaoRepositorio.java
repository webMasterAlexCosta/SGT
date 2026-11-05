package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.FrequenciaExcecao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FrequenciaExcecaoRepositorio extends JpaRepository<FrequenciaExcecao, Long>, JpaSpecificationExecutor<FrequenciaExcecao> {

    @Query("""
            select e from FrequenciaExcecao e
            where :data between e.dataInicio and e.dataFim
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
            """)
    List<FrequenciaExcecao> buscarAtivasPorData(@Param("data") LocalDate data);

    @Query("""
            select e from FrequenciaExcecao e
            where :data between e.dataInicio and e.dataFim
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
              and e.empregado.lotacao.id = :lotacaoId
            """)
    List<FrequenciaExcecao> buscarAtivasPorDataELotacao(@Param("data") LocalDate data, @Param("lotacaoId") Long lotacaoId);

    @Query("""
            select e from FrequenciaExcecao e
            where :data between e.dataInicio and e.dataFim
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
              and e.empregado.lotacao.id = :lotacaoId
              and e.empregado.jornada.id = :jornadaId
            """)
    List<FrequenciaExcecao> buscarAtivasPorDataLotacaoEJornada(@Param("data") LocalDate data, @Param("lotacaoId") Long lotacaoId, @Param("jornadaId") Long jornadaId);


    @Query("""
            select e from FrequenciaExcecao e
            where e.empregado.id in :empregadoIds
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
              and e.dataInicio <= :fim
              and e.dataFim >= :inicio
            """)
    List<FrequenciaExcecao> buscarAtivasPorEmpregadosEPeriodo(
            @Param("empregadoIds") java.util.Collection<Long> empregadoIds,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("""
            select (count(e) > 0) from FrequenciaExcecao e
            where e.empregado.id = :empregadoId
              and :data between e.dataInicio and e.dataFim
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
              and upper(e.tipo) = upper(:tipo)
            """)
    boolean existsAtivaTipoByEmpregadoAndData(@Param("empregadoId") Long empregadoId, @Param("data") LocalDate data, @Param("tipo") String tipo);

    @Query("""
            select (count(e) > 0) from FrequenciaExcecao e
            where e.empregado.id = :empregadoId
              and upper(e.status) <> upper(:#{T(com.example.siggtrat.constante.ConstantesDominio).STATUS_CANCELADO})
              and upper(e.tipo) = upper(:tipo)
              and e.id <> coalesce(:ignoreId, -1)
              and e.dataInicio <= :fim
              and e.dataFim >= :inicio
            """)
    boolean existsOverlap(@Param("empregadoId") Long empregadoId,
                          @Param("tipo") String tipo,
                          @Param("inicio") LocalDate inicio,
                          @Param("fim") LocalDate fim,
                          @Param("ignoreId") Long ignoreId);
}