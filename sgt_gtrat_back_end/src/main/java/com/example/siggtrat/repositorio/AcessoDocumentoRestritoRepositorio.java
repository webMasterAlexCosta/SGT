package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.AcessoDocumentoRestrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcessoDocumentoRestritoRepositorio extends JpaRepository<AcessoDocumentoRestrito, Long> {
    List<AcessoDocumentoRestrito> findByDocumentoGrupoLogicoIdOrderByAcessadoEmAscIdAsc(String grupoLogicoId);
}

