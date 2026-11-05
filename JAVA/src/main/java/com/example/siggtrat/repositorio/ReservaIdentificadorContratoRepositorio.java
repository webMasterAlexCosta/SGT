package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ReservaIdentificadorContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservaIdentificadorContratoRepositorio extends JpaRepository<ReservaIdentificadorContrato, Long> {
    Optional<ReservaIdentificadorContrato> findByNumeroAndAno(String numero, Integer ano);
}
