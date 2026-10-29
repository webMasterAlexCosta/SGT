package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.FrequenciaAdicional;
import com.example.siggtrat.entidade.Empregado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FrequenciaAdicionalRepositorio extends JpaRepository<FrequenciaAdicional, Long>, JpaSpecificationExecutor<FrequenciaAdicional> {

    List<FrequenciaAdicional> findByDataReferencia(LocalDate data);

    List<FrequenciaAdicional> findByDataReferenciaBetween(LocalDate inicio, LocalDate fim);

    List<FrequenciaAdicional> findByDataReferenciaBetweenAndTipoIgnoreCase(LocalDate inicio, LocalDate fim, String tipo);

    Optional<FrequenciaAdicional> findByEmpregadoAndDataReferenciaAndTipoIgnoreCase(Empregado empregado, LocalDate data, String tipo);

    Optional<FrequenciaAdicional> findByEmpregadoIdAndDataReferenciaAndTipoIgnoreCase(Long empregadoId, LocalDate data, String tipo);

    List<FrequenciaAdicional> findByEmpregadoIdInAndDataReferenciaBetweenAndTipoIgnoreCase(
            java.util.Collection<Long> empregadoIds, LocalDate inicio, LocalDate fim, String tipo);

    @Query("""
            select a from FrequenciaAdicional a
            where a.dataReferencia = :data
              and a.empregado.lotacao.id = :lotacaoId
            """)
    List<FrequenciaAdicional> buscarPorDataELotacao(@Param("data") LocalDate data, @Param("lotacaoId") Long lotacaoId);

    @Query("""
            select a from FrequenciaAdicional a
            where a.dataReferencia = :data
              and a.empregado.lotacao.id = :lotacaoId
              and a.empregado.jornada.id = :jornadaId
            """)
    List<FrequenciaAdicional> buscarPorDataELotacaoEJornada(@Param("data") LocalDate data, @Param("lotacaoId") Long lotacaoId, @Param("jornadaId") Long jornadaId);

    @Query("""
            select a from FrequenciaAdicional a
            where a.dataReferencia between :inicio and :fim
              and a.empregado.lotacao.id = :lotacaoId
            """)
    List<FrequenciaAdicional> buscarPorIntervaloELotacao(@Param("inicio") LocalDate inicio,
                                                    @Param("fim") LocalDate fim,
                                                    @Param("lotacaoId") Long lotacaoId);

    @Query("""
            select a from FrequenciaAdicional a
            where a.dataReferencia between :inicio and :fim
              and a.empregado.lotacao.id = :lotacaoId
              and a.empregado.jornada.id = :jornadaId
            """)
    List<FrequenciaAdicional> buscarPorIntervaloELotacaoEJornada(@Param("inicio") LocalDate inicio,
                                                              @Param("fim") LocalDate fim,
                                                              @Param("lotacaoId") Long lotacaoId,
                                                              @Param("jornadaId") Long jornadaId);
}