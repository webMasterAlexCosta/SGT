package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Telefone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelefoneRepositorio extends JpaRepository<Telefone, Long> {
}
