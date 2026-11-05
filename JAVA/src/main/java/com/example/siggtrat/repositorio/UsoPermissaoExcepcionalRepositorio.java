package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.UsoPermissaoExcepcional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsoPermissaoExcepcionalRepositorio extends JpaRepository<UsoPermissaoExcepcional, Long> {
    List<UsoPermissaoExcepcional> findByPermissaoIdOrderByUsadoEmDescIdDesc(Long permissaoId);
}
