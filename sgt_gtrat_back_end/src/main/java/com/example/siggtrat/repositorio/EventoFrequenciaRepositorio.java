package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoFrequencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoFrequenciaRepositorio extends JpaRepository<EventoFrequencia, Long> {
    List<EventoFrequencia> findByFrequenciaIdOrderByNumeroVersaoAscIdAsc(Long frequenciaId);
    Optional<EventoFrequencia> findTopByFrequenciaIdOrderByNumeroVersaoDescIdDesc(Long frequenciaId);
    Optional<EventoFrequencia> findByChaveIdempotencia(String chaveIdempotencia);
}
