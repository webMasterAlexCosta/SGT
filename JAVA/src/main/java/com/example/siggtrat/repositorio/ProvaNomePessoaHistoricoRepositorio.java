package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.ProvaNomePessoaHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProvaNomePessoaHistoricoRepositorio extends JpaRepository<ProvaNomePessoaHistorico, Long> {
    Optional<ProvaNomePessoaHistorico> findByContratoIdAndResponsavelIdAndChaveIdempotencia(Long contratoId, Long responsavelId, String chave);
    List<ProvaNomePessoaHistorico> findByPessoaIdAndVinculoIdOrderByRegistradoEmAscIdAsc(Long pessoaId, Long vinculoId);
}
