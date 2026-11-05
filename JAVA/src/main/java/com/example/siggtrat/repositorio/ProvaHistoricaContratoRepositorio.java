package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ProvaHistoricaContrato;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ProvaHistoricaContratoRepositorio extends JpaRepository<ProvaHistoricaContrato, Long> {
    List<ProvaHistoricaContrato> findByContratoIdInOrderByContratoIdAscNumeroRevisaoAsc(Collection<Long> contratoIds);

    default List<ProvaHistoricaContrato> findByContratoIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(
            Long contrato, Instant conhecido) {
        return buscarPorContratoConhecidoAteMicros(contrato, PrecisaoTemporal.limiteInclusivoMicros(conhecido));
    }

    @Query("""
            select p from ProvaHistoricaContrato p
             where p.contrato.id = :contrato and p.registradoEm <= :conhecido
             order by p.numeroRevisao asc
            """)
    List<ProvaHistoricaContrato> buscarPorContratoConhecidoAteMicros(
            @Param("contrato") Long contrato, @Param("conhecido") Instant conhecido);

    default List<ProvaHistoricaContrato> findByEventoOrigemIdAndRegistradoEmLessThanEqualOrderByNumeroRevisaoAsc(
            Long evento, Instant conhecido) {
        return buscarPorEventoOrigemConhecidoAteMicros(evento, PrecisaoTemporal.limiteInclusivoMicros(conhecido));
    }

    @Query("""
            select p from ProvaHistoricaContrato p
             where p.eventoOrigem.id = :evento and p.registradoEm <= :conhecido
             order by p.numeroRevisao asc
            """)
    List<ProvaHistoricaContrato> buscarPorEventoOrigemConhecidoAteMicros(
            @Param("evento") Long evento, @Param("conhecido") Instant conhecido);

    Optional<ProvaHistoricaContrato> findByContratoIdAndChaveIdempotencia(Long contrato, String chave);
    Optional<ProvaHistoricaContrato> findByVersaoGeradaId(Long versao);
}
