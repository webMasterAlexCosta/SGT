package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.StatusEmpregado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusEmpregadoRepositorio extends JpaRepository<StatusEmpregado, Long> {

    // --- CORREÇÃO DO ERRO ---
    // Você precisa declarar este método na interface.
    // O Spring irá criar automaticamente o SQL para buscar "ATIVO" (que é o ID 1).
    Optional<StatusEmpregado> findByDescricaoIgnoreCase(String descricao);

}