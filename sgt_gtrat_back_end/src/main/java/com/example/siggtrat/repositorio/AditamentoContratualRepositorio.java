package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.AditamentoContratual;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AditamentoContratualRepositorio extends JpaRepository<AditamentoContratual, Long> {
    @EntityGraph(attributePaths = {"evento", "novoTipoContratacao", "novoFornecedor", "alteracoesComposicao", "alteracoesComposicao.composicaoBase", "alteracoesComposicao.lotacao", "alteracoesComposicao.jornada", "alteracoesComposicao.cargo"})
    @org.springframework.data.jpa.repository.Query("select a from AditamentoContratual a where a.id = :id")
    Optional<AditamentoContratual> buscarCompletoPorId(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"evento", "novoTipoContratacao", "novoFornecedor", "alteracoesComposicao", "alteracoesComposicao.composicaoBase", "alteracoesComposicao.lotacao", "alteracoesComposicao.jornada", "alteracoesComposicao.cargo"})
    List<AditamentoContratual> findByContratoIdOrderByDataEfetivaAscRegistradoEmAsc(Long contratoId);

    @EntityGraph(attributePaths = {"evento", "alteracoesComposicao", "alteracoesComposicao.composicaoBase", "alteracoesComposicao.lotacao", "alteracoesComposicao.jornada", "alteracoesComposicao.cargo"})
    List<AditamentoContratual> findByContratoIdAndDataEfetiva(Long contratoId, LocalDate dataEfetiva);

    @EntityGraph(attributePaths = {"evento", "novoTipoContratacao", "novoFornecedor", "alteracoesComposicao", "alteracoesComposicao.composicaoBase", "alteracoesComposicao.lotacao", "alteracoesComposicao.jornada", "alteracoesComposicao.cargo"})
    Optional<AditamentoContratual> findByEventoId(Long eventoId);

    Optional<AditamentoContratual> findTopByContratoIdAndIdentificacaoIgnoreCaseOrderByVersaoInstrumentoDesc(
            Long contratoId, String identificacao);

    @Query("select count(i) from AditamentoContratual a join a.alteracoesComposicao i where i.cargo.id=:id and a.dataEfetiva>=:data and a.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorCargo(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(i) from AditamentoContratual a join a.alteracoesComposicao i where i.lotacao.id=:id and a.dataEfetiva>=:data and a.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorLotacao(@Param("id") Long id, @Param("data") LocalDate data);
    @Query("select count(i) from AditamentoContratual a join a.alteracoesComposicao i where i.jornada.id=:id and a.dataEfetiva>=:data and a.evento.situacaoFato=com.example.siggtrat.entidade.SituacaoFatoNegocial.VIGENTE")
    long contarUsoFuturoPorJornada(@Param("id") Long id, @Param("data") LocalDate data);
}
