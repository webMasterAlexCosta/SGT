package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.MapeamentoTransferenciaGestao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapeamentoTransferenciaGestaoRepositorio extends JpaRepository<MapeamentoTransferenciaGestao, Long> {
    List<MapeamentoTransferenciaGestao> findByTransferenciaIdOrderByIdAsc(Long transferenciaId);
}
