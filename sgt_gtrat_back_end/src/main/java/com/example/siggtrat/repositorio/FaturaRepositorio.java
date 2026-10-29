package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
public interface FaturaRepositorio extends JpaRepository<Fatura, Long> {
    List<Fatura> findTop200ByOrderByCriadoEmDesc();
    Optional<Fatura> findByFolhaHorasId(Long folhaHorasId);
    long countByStatusNot(String status);
    long countByContratoGestaoLocalIdAndStatusNot(Long gestaoLocalId, String status);
    long countByStatusNotIn(Collection<String> status);
    long countByContratoGestaoLocalIdAndStatusNotIn(Long gestaoLocalId, Collection<String> status);
}
