package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RevisaoFontesPainel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RevisaoFontesPainelRepositorio extends JpaRepository<RevisaoFontesPainel, Integer> {
    // Projeção escalar: uma entidade já carregada no contexto JPA não pode
    // ocultar a revisão que outro escritor confirmou.
    @Query("select r.valor from RevisaoFontesPainel r where r.id = 1")
    Optional<String> valorAtual();

    @Modifying(flushAutomatically = true)
    @Query("update RevisaoFontesPainel r set r.valor = :valor, r.version = r.version + 1 where r.id = 1")
    int registrarMudanca(@Param("valor") String valor);
}
