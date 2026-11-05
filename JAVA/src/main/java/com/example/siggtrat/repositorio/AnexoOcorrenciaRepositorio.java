package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.AnexoOcorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface AnexoOcorrenciaRepositorio extends JpaRepository<AnexoOcorrencia, Long> {

    @Query("select a.nomeSalvo from AnexoOcorrencia a")
    List<String> buscarTodosNomesArmazenados();
}