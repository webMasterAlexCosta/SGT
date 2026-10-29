package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Agencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgenciaRepositorio extends JpaRepository<Agencia, Long> {

    java.util.Optional<Agencia> findByBancoIdAndNumeroIgnoreCase(Long bancoId, String numero);
}
