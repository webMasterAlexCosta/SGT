package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.FolhaHoras;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
public interface FolhaHorasRepositorio extends JpaRepository<FolhaHoras, Long> {
    List<FolhaHoras> findTop200ByOrderByPeriodoInicioDesc();
    Optional<FolhaHoras> findByContratoIdAndPeriodoInicioAndPeriodoFim(Long contratoId, LocalDate inicio, LocalDate fim);
    long countByStatusIn(Collection<String> status);
    long countByContratoGestaoLocalIdAndStatusIn(Long gestaoLocalId, Collection<String> status);
}
