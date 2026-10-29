package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BancoRepositorio extends JpaRepository<Banco, Long> {
}
