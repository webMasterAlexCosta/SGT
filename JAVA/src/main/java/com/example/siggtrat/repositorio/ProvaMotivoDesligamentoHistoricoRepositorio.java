package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.ProvaMotivoDesligamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProvaMotivoDesligamentoHistoricoRepositorio extends JpaRepository<ProvaMotivoDesligamentoHistorico, Long> {
    Optional<ProvaMotivoDesligamentoHistorico> findByContratoIdAndResponsavelIdAndChaveIdempotencia(Long contratoId, Long responsavelId, String chave);
    List<ProvaMotivoDesligamentoHistorico> findByContratoIdAndMotivoDesligamentoIdOrderByRegistradoEmAscIdAsc(Long contratoId, Long motivoId);
}
