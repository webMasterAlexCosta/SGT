package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RegraAdicional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraAdicionalRepositorio extends JpaRepository<RegraAdicional, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RegraAdicional r where r.id = :id")
    java.util.Optional<RegraAdicional> buscarPorIdParaAtualizacao(@org.springframework.data.repository.query.Param("id") Long id);
    List<RegraAdicional> findByJornada_IdOrderByOrdemAsc(Long idJornada);
}
