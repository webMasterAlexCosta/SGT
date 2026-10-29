package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmissaoRelatorioAdministrativoRepositorio extends
        JpaRepository<EmissaoRelatorioAdministrativo, Long>,
        JpaSpecificationExecutor<EmissaoRelatorioAdministrativo> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmissaoRelatorioAdministrativo> findTopByChaveSerieOrderByNumeroVersaoDescIdDesc(String chaveSerie);

    Optional<EmissaoRelatorioAdministrativo> findByUsuarioIdAndChaveIdempotencia(Long usuarioId, String chaveIdempotencia);

    Optional<EmissaoRelatorioAdministrativo> findTopByChaveSerieAndGeradoEmLessThanEqualOrderByGeradoEmDescIdDesc(
            String chaveSerie, Instant conhecidoAte);

    @Query("""
        select e from EmissaoRelatorioAdministrativo e
        where e.estado = :estado and e.fim >= :desde
        order by e.geradoEm asc, e.id asc
        """)
    List<EmissaoRelatorioAdministrativo> buscarVigentesAfetaveis(
            @Param("estado") EstadoEmissaoRelatorio estado,
            @Param("desde") LocalDate desde);
}
