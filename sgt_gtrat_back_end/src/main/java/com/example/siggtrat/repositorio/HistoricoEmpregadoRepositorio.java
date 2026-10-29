package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.AtividadePainelDTO;
import com.example.siggtrat.entidade.HistoricoEmpregado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoEmpregadoRepositorio extends JpaRepository<HistoricoEmpregado, Long> {

    List<HistoricoEmpregado> findByEmpregado_IdOrderByDataEventoDesc(Long idEmpregado);


    Page<HistoricoEmpregado> findByEmpregado_IdOrderByDataEventoDesc(Long idEmpregado, Pageable pageable);

    Page<HistoricoEmpregado> findByGestaoLocalSnapshotIdOrderByDataEventoDesc(Long gestaoLocalId, Pageable pageable);

    Page<HistoricoEmpregado> findBySuperintendenciaSnapshotIdOrderByDataEventoDesc(
            Long superintendenciaId, Pageable pageable);

    Page<HistoricoEmpregado> findByEmpregado_IdAndGestaoLocalSnapshotIdOrderByDataEventoDesc(
            Long empregadoId, Long gestaoLocalId, Pageable pageable);

    Page<HistoricoEmpregado> findByEmpregado_IdAndSuperintendenciaSnapshotIdOrderByDataEventoDesc(
            Long empregadoId, Long superintendenciaId, Pageable pageable);

    boolean existsByEmpregado_IdAndGestaoLocalSnapshotId(Long empregadoId, Long gestaoLocalId);

    boolean existsByEmpregado_IdAndSuperintendenciaSnapshotId(Long empregadoId, Long superintendenciaId);

    List<HistoricoEmpregado> findByEmpregado_IdOrderByIdAsc(Long idEmpregado);

    java.util.Optional<HistoricoEmpregado> findTopByEmpregado_IdAndHashIntegridadeStartingWithOrderByIdDesc(Long idEmpregado, String prefixo);


    @Query("select new com.example.siggtrat.dto.AtividadePainelDTO(h.id, h.dataEvento, h.descricao, e.id, e.nome, u.id, u.username) " +
            "from HistoricoEmpregado h join h.empregado e left join h.usuario u")
    Page<AtividadePainelDTO> buscarAtividadesRecentes(Pageable pageable);

    @Query("select new com.example.siggtrat.dto.AtividadePainelDTO(h.id, h.dataEvento, h.descricao, e.id, e.nome, u.id, u.username) " +
            "from HistoricoEmpregado h join h.empregado e left join h.usuario u where h.gestaoLocalSnapshotId = :gestaoLocalId")
    Page<AtividadePainelDTO> buscarAtividadesRecentesPorGestaoLocal(Long gestaoLocalId, Pageable pageable);

    @Query("select new com.example.siggtrat.dto.AtividadePainelDTO(h.id, h.dataEvento, h.descricao, e.id, e.nome, u.id, u.username) " +
            "from HistoricoEmpregado h join h.empregado e left join h.usuario u where h.superintendenciaSnapshotId = :superintendenciaId")
    Page<AtividadePainelDTO> buscarAtividadesRecentesPorSuperintendencia(Long superintendenciaId, Pageable pageable);
}
