package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.TipoConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoContaRepositorio extends JpaRepository<TipoConta, Long> {
}
