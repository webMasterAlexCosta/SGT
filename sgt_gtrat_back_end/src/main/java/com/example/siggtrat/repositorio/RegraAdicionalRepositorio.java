package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RegraAdicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraAdicionalRepositorio extends JpaRepository<RegraAdicional, Long> {
    List<RegraAdicional> findByJornada_IdOrderByOrdemAsc(Long idJornada);
}
