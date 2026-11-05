package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.entidade.MotivoDesligamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotivoDesligamentoRepositorio extends JpaRepository<MotivoDesligamento, Long> {
    List<MotivoDesligamento> findByGestaoLocalIdOrderByDescricaoAsc(Long gestaoLocalId);
    List<MotivoDesligamento> findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(Long superintendenciaId);
    List<MotivoDesligamento> findAllByOrderByGestaoLocalNomeAscDescricaoAsc();
    Optional<MotivoDesligamento> findByGestaoLocalIdAndDescricaoIgnoreCase(Long gestaoLocalId, String descricao);
    List<MotivoDesligamento> findByGestaoLocalIdAndCategoriaOrderByDescricaoAsc(Long gestaoLocalId, CategoriaTerminoVinculo categoria);
    List<MotivoDesligamento> findByGestaoLocalIdAndCategoriaAndAtivoTrueOrderByDescricaoAsc(
            Long gestaoLocalId, CategoriaTerminoVinculo categoria);
}
