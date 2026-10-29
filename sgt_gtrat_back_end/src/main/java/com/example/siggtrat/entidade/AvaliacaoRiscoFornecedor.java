package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "avaliacao_risco_fornecedor", indexes = @Index(name = "idx_risco_fornecedor_proxima", columnList = "proxima_avaliacao"))
@Getter @Setter @NoArgsConstructor
public class AvaliacaoRiscoFornecedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_avaliacao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_fornecedor", nullable = false)
    private Fornecedor fornecedor;
    @Column(name = "avaliado_em", nullable = false) private LocalDate avaliadoEm;
    @Column(name = "proxima_avaliacao", nullable = false) private LocalDate proximaAvaliacao;
    @Column(name = "score_trabalhista", nullable = false) private Integer scoreTrabalhista;
    @Column(name = "score_financeiro", nullable = false) private Integer scoreFinanceiro;
    @Column(name = "score_compliance", nullable = false) private Integer scoreCompliance;
    @Column(name = "score_seguranca", nullable = false) private Integer scoreSeguranca;
    @Column(name = "score_total", nullable = false) private Integer scoreTotal;
    @Column(columnDefinition = "TEXT") private String evidencias;
    @Column(name = "avaliado_por", nullable = false, length = 100) private String avaliadoPor;
    @Version private long version;
}
