package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.MapeamentoTransferenciaGestao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

public interface MapeamentoTransferenciaGestaoRepositorio extends JpaRepository<MapeamentoTransferenciaGestao, Long> {
    List<MapeamentoTransferenciaGestao> findByTransferenciaIdOrderByIdAsc(Long transferenciaId);

    @Query("select count(m) from MapeamentoTransferenciaGestao m where m.cargoDestino.id=:id and m.transferencia.dataEfetiva>=:data and m.transferencia.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and m.transferencia.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorCargo(@Param("id") Long id, @Param("data") LocalDate data);

    @Query("select count(m) from MapeamentoTransferenciaGestao m where m.lotacaoDestino.id=:id and m.transferencia.dataEfetiva>=:data and m.transferencia.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and m.transferencia.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorLotacao(@Param("id") Long id, @Param("data") LocalDate data);

    @Query("select count(m) from MapeamentoTransferenciaGestao m where m.jornadaDestino.id=:id and m.transferencia.dataEfetiva>=:data and m.transferencia.contrato.estadoNegocial=com.example.siggtrat.entidade.EstadoNegocialContrato.PUBLICADO and m.transferencia.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorJornada(@Param("id") Long id, @Param("data") LocalDate data);
}
