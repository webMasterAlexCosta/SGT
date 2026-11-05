package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;

/** Prova do contexto conhecido no registro; não substitui o replay temporal vigente. */
@Entity
@Immutable
@Table(name = "contexto_ocorrencia", uniqueConstraints =
        @UniqueConstraint(name = "uk_contexto_ocorrencia_dia", columnNames = {"ocorrencia_id", "data_referencia"}),
        indexes = @Index(name = "idx_contexto_ocorrencia_escopo", columnList = "gestao_local_id,lotacao_id,jornada_id,data_referencia"))
public class ContextoOcorrencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocorrencia_id", nullable = false, updatable = false) private Ocorrencia ocorrencia;
    @Column(name = "data_referencia", nullable = false, updatable = false) private LocalDate dataReferencia;
    @Column(name = "contrato_id", nullable = false, updatable = false) private Long contratoId;
    @Column(name = "vinculo_id", nullable = false, updatable = false) private Long vinculoId;
    @Column(name = "alocacao_id", nullable = false, updatable = false) private Long alocacaoId;
    @Column(name = "gestao_local_id", nullable = false, updatable = false) private Long gestaoLocalId;
    @Column(name = "superintendencia_id", nullable = false, updatable = false) private Long superintendenciaId;
    @Column(name = "lotacao_id", nullable = false, updatable = false) private Long lotacaoId;
    @Column(name = "jornada_id", nullable = false, updatable = false) private Long jornadaId;

    protected ContextoOcorrencia() {}
    public ContextoOcorrencia(Ocorrencia ocorrencia, LocalDate data, Long contrato, Long vinculo, Long alocacao,
                              Long gestao, Long estado, Long lotacao, Long jornada) {
        this.ocorrencia = ocorrencia; this.dataReferencia = data; this.contratoId = contrato;
        this.vinculoId = vinculo; this.alocacaoId = alocacao; this.gestaoLocalId = gestao;
        this.superintendenciaId = estado; this.lotacaoId = lotacao; this.jornadaId = jornada;
    }
    public Long getId() { return id; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public Long getContratoId() { return contratoId; }
    public Long getVinculoId() { return vinculoId; }
    public Long getAlocacaoId() { return alocacaoId; }
    public Long getGestaoLocalId() { return gestaoLocalId; }
    public Long getSuperintendenciaId() { return superintendenciaId; }
    public Long getLotacaoId() { return lotacaoId; }
    public Long getJornadaId() { return jornadaId; }
}
