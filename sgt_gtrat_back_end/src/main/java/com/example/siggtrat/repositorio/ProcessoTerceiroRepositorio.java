package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.ProcessoTerceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
public interface ProcessoTerceiroRepositorio extends JpaRepository<ProcessoTerceiro, Long> {
    List<ProcessoTerceiro> findTop200ByOrderByCriadoEmDesc();
    long countByStatusNotIn(Collection<String> status);
    long countByContratoGestaoLocalIdAndStatusNotIn(Long gestaoLocalId, Collection<String> status);
}
