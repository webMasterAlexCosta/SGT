package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.TransferenciaGestaoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferenciaGestaoContratoRepositorio extends JpaRepository<TransferenciaGestaoContrato, Long> {
    Optional<TransferenciaGestaoContrato> findByEventoId(Long eventoId);
    List<TransferenciaGestaoContrato> findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(Long contratoId);
}
