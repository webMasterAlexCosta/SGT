package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.VersaoComplementoFrequencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VersaoComplementoFrequenciaRepositorio extends JpaRepository<VersaoComplementoFrequencia, Long> {
    List<VersaoComplementoFrequencia> findByTipoComplementoAndComplementoIdOrderByNumeroVersaoAsc(
            String tipoComplemento, Long complementoId);
    Optional<VersaoComplementoFrequencia> findTopByTipoComplementoAndComplementoIdOrderByNumeroVersaoDesc(
            String tipoComplemento, Long complementoId);
}
