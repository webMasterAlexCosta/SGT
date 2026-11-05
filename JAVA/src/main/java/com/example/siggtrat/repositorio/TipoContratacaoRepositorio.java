package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.TipoContratacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoContratacaoRepositorio extends JpaRepository<TipoContratacao, Long> {
    List<TipoContratacao> findByGestaoLocalIdOrderByDescricaoAsc(Long gestaoLocalId);
    List<TipoContratacao> findByGestaoLocalSuperintendenciaIdOrderByDescricaoAsc(Long superintendenciaId);
    List<TipoContratacao> findAllByOrderByGestaoLocalNomeAscDescricaoAsc();
    Optional<TipoContratacao> findByGestaoLocalIdAndDescricaoIgnoreCase(Long gestaoLocalId, String descricao);
}
