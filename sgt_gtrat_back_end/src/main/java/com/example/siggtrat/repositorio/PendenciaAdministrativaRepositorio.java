package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.entidade.PendenciaAdministrativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PendenciaAdministrativaRepositorio extends JpaRepository<PendenciaAdministrativa, Long>,
        JpaSpecificationExecutor<PendenciaAdministrativa> {
    Optional<PendenciaAdministrativa> findByChaveOrigem(String chaveOrigem);
    List<PendenciaAdministrativa> findByOrigemEntidadeAndOrigemIdAndEstadoNot(
            String origemEntidade, Long origemId, EstadoPendencia estado);
}
