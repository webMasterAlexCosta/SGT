package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RemanejamentoContratual;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RemanejamentoContratualRepositorio extends JpaRepository<RemanejamentoContratual, Long> {
    @EntityGraph(attributePaths = {"evento", "itens", "itens.composicaoBase", "itens.lotacao", "itens.jornada", "itens.cargo"})
    @org.springframework.data.jpa.repository.Query("select r from RemanejamentoContratual r where r.id = :id")
    Optional<RemanejamentoContratual> buscarCompletoPorId(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"evento", "itens", "itens.composicaoBase", "itens.lotacao", "itens.jornada", "itens.cargo"})
    List<RemanejamentoContratual> findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(Long contratoId);

    @EntityGraph(attributePaths = {"evento", "itens", "itens.composicaoBase", "itens.lotacao", "itens.jornada", "itens.cargo"})
    List<RemanejamentoContratual> findByContratoIdAndDataEfetiva(Long contratoId, LocalDate dataEfetiva);

    @EntityGraph(attributePaths = {"evento", "itens", "itens.composicaoBase", "itens.lotacao", "itens.jornada", "itens.cargo"})
    Optional<RemanejamentoContratual> findByEventoId(Long eventoId);

    Optional<RemanejamentoContratual> findTopByContratoIdAndIdentificacaoIgnoreCaseOrderByVersaoInstrumentoDesc(
            Long contratoId, String identificacao);

    @Query("select count(i) from RemanejamentoContratual r join r.itens i where i.cargo.id=:id and r.dataEfetiva>=:data and r.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorCargo(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(i) from RemanejamentoContratual r join r.itens i where i.lotacao.id=:id and r.dataEfetiva>=:data and r.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorLotacao(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(i) from RemanejamentoContratual r join r.itens i where i.jornada.id=:id and r.dataEfetiva>=:data and r.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorJornada(@Param("id") Long id, @Param("data") LocalDate data);
}
