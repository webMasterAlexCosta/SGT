package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoDisponibilidadeCatalogoRepositorio extends JpaRepository<EventoDisponibilidadeCatalogo, Long> {
    List<EventoDisponibilidadeCatalogo> findByTipoCatalogoAndCatalogoIdOrderByDataEfetivaAscRegistradoEmAscIdAsc(
            TipoCatalogoTemporal tipo, Long catalogoId);
    Optional<EventoDisponibilidadeCatalogo> findByTipoCatalogoAndCatalogoIdAndChaveIdempotencia(
            TipoCatalogoTemporal tipo, Long catalogoId, String chaveIdempotencia);
    default Optional<EventoDisponibilidadeCatalogo>
    findFirstByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
            TipoCatalogoTemporal tipo, Long catalogoId, LocalDate dataEfetiva, Instant conhecidoAte) {
        return buscarConhecidosEmMicros(tipo, catalogoId, dataEfetiva,
                PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte), PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
            select e from EventoDisponibilidadeCatalogo e
             where e.tipoCatalogo = :tipo and e.catalogoId = :id
               and e.dataEfetiva <= :dataEfetiva and e.registradoEm <= :conhecidoAte
             order by e.dataEfetiva desc, e.registradoEm desc, e.id desc
            """)
    List<EventoDisponibilidadeCatalogo> buscarConhecidosEmMicros(
            @Param("tipo") TipoCatalogoTemporal tipo, @Param("id") Long catalogoId,
            @Param("dataEfetiva") LocalDate dataEfetiva, @Param("conhecidoAte") Instant conhecidoAte,
            Pageable limite);

    /** Leitura corrente após o shared lock do item, mesmo com read view anterior em REPEATABLE READ. */
    default Optional<EventoDisponibilidadeCatalogo>
    findTopByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualAndRegistradoEmLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
            TipoCatalogoTemporal tipo, Long catalogoId, LocalDate dataEfetiva, Instant conhecidoAte) {
        return buscarConhecidosEmMicrosComBloqueio(tipo, catalogoId, dataEfetiva,
                PrecisaoTemporal.limiteInclusivoMicros(conhecidoAte), PageRequest.of(0, 1)).stream().findFirst();
    }

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select e from EventoDisponibilidadeCatalogo e
             where e.tipoCatalogo = :tipo and e.catalogoId = :id
               and e.dataEfetiva <= :dataEfetiva and e.registradoEm <= :conhecidoAte
             order by e.dataEfetiva desc, e.registradoEm desc, e.id desc
            """)
    List<EventoDisponibilidadeCatalogo> buscarConhecidosEmMicrosComBloqueio(
            @Param("tipo") TipoCatalogoTemporal tipo, @Param("id") Long catalogoId,
            @Param("dataEfetiva") LocalDate dataEfetiva, @Param("conhecidoAte") Instant conhecidoAte,
            Pageable limite);

    /** Leitura corrente depois do lock do catálogo, inclusive sob REPEATABLE READ. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventoDisponibilidadeCatalogo e where e.tipoCatalogo=:tipo and e.catalogoId=:id "
            + "order by e.dataEfetiva desc, e.registradoEm desc, e.id desc")
    List<EventoDisponibilidadeCatalogo> buscarTrilhaParaAtualizacao(@Param("tipo") TipoCatalogoTemporal tipo,
                                                                   @Param("id") Long catalogoId);
}
