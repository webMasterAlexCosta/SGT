package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.AvaliacaoRiscoFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface AvaliacaoRiscoFornecedorRepositorio extends JpaRepository<AvaliacaoRiscoFornecedor, Long> {
    List<AvaliacaoRiscoFornecedor> findTop200ByOrderByAvaliadoEmDesc();
    long countByProximaAvaliacaoBefore(LocalDate data);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.id) FROM AvaliacaoRiscoFornecedor a " +
            "JOIN Contrato c ON c.fornecedor = a.fornecedor " +
            "WHERE c.gestaoLocal.id = :gestaoLocalId AND a.proximaAvaliacao < :data")
    long countVencidasByGestaoLocalId(
            @org.springframework.data.repository.query.Param("gestaoLocalId") Long gestaoLocalId,
            @org.springframework.data.repository.query.Param("data") LocalDate data);
}
