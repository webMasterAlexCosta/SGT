package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.relatorio.EstadoEmissaoRelatorio;
import com.example.siggtrat.entidade.EmissaoRelatorioAdministrativo;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    default Optional<EmissaoRelatorioAdministrativo> findTopByChaveSerieAndGeradoEmLessThanEqualOrderByGeradoEmDescIdDesc(
            String chaveSerie, Instant conhecidoAte) {
        return buscarPorSerieConhecidaAteMicros(chaveSerie, PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte),
                PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
        select e from EmissaoRelatorioAdministrativo e
        where e.chaveSerie = :chaveSerie and e.geradoEm <= :conhecidoAte
        order by e.geradoEm desc, e.id desc
        """)
    List<EmissaoRelatorioAdministrativo> buscarPorSerieConhecidaAteMicros(
            @Param("chaveSerie") String chaveSerie, @Param("conhecidoAte") Instant conhecidoAte,
            Pageable limite);

    @Query("""
        select e from EmissaoRelatorioAdministrativo e
        where e.estado = :estado and e.fim >= :desde
        order by e.geradoEm asc, e.id asc
        """)
    List<EmissaoRelatorioAdministrativo> buscarVigentesAfetaveis(
            @Param("estado") EstadoEmissaoRelatorio estado,
            @Param("desde") LocalDate desde);

    /** Candidatos escalares: não carrega os arquivos/fotografias de todas as emissões. */
    interface CandidatoInvalidacao {
        Long getId();
        com.example.siggtrat.dominio.relatorio.TipoRelatorioAdministrativo getTipo();
        LocalDate getInicio();
        LocalDate getFim();
        String getContratoIdsCsv();
        Long getGestaoLocalEscopoId();
        Long getSuperintendenciaEscopoId();
        Long getLotacaoId();
        Long getJornadaId();
        Instant getFonteConhecidaAte();
    }

    @Query("""
        select e.id as id, e.tipo as tipo, e.inicio as inicio, e.fim as fim,
               e.contratoIdsCsv as contratoIdsCsv, e.gestaoLocalEscopoId as gestaoLocalEscopoId,
               e.superintendenciaEscopoId as superintendenciaEscopoId,
               e.lotacaoId as lotacaoId, e.jornadaId as jornadaId, e.fonteConhecidaAte as fonteConhecidaAte
        from EmissaoRelatorioAdministrativo e
        where e.estado = :estado and e.fim >= :desde and (:ate is null or e.inicio <= :ate)
          and e.id > :ultimoId
        order by e.id asc
        """)
    List<CandidatoInvalidacao> buscarCandidatasInvalidacaoEmLote(
            @Param("estado") EstadoEmissaoRelatorio estado, @Param("desde") LocalDate desde,
            @Param("ate") LocalDate ate, @Param("ultimoId") long ultimoId, Pageable limite);
}
