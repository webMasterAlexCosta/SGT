package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.EventoDocumentoComprobatorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface EventoDocumentoComprobatorioRepositorio extends JpaRepository<EventoDocumentoComprobatorio, Long> {
    List<EventoDocumentoComprobatorio> findByGrupoLogicoIdOrderByRegistradoEmAscIdAsc(String grupoLogicoId);
    Optional<EventoDocumentoComprobatorio> findTopByGrupoLogicoIdOrderByRegistradoEmDescIdDesc(String grupoLogicoId);
    void deleteByDocumentoId(Long documentoId);
}
