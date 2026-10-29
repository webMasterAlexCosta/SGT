package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import com.example.siggtrat.entidade.EventoDisponibilidadeCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EventoDisponibilidadeCatalogoRepositorio extends JpaRepository<EventoDisponibilidadeCatalogo, Long> {
    List<EventoDisponibilidadeCatalogo>
    findByTipoCatalogoAndCatalogoIdAndDataEfetivaLessThanEqualOrderByDataEfetivaDescRegistradoEmDescIdDesc(
            TipoCatalogoTemporal tipo, Long catalogoId, LocalDate dataEfetiva);
}
