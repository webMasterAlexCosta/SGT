package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tabela_tarifaria", indexes = @Index(name = "idx_tabela_tarifaria_contrato_vigencia", columnList = "id_contrato,vigencia_inicio,vigencia_fim"))
@Getter @Setter @NoArgsConstructor
public class TabelaTarifaria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tabela_tarifaria") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_cargo") private Cargo cargo;
    @Column(name = "vigencia_inicio", nullable = false) private LocalDate vigenciaInicio;
    @Column(name = "vigencia_fim") private LocalDate vigenciaFim;
    @Column(name = "valor_hora", nullable = false, precision = 15, scale = 4) private BigDecimal valorHora;
    @Column(name = "adicional_noturno_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal adicionalNoturnoPercent = new BigDecimal("20.00");
    @Column(length = 3, nullable = false) private String moeda = "BRL";
    @Column(nullable = false) private Boolean ativo = true;
    @Version private long version;

    @PrePersist @PreUpdate
    void validar() {
        if (contrato == null || vigenciaInicio == null || valorHora == null || valorHora.signum() <= 0)
            throw new IllegalStateException("Contrato, vigência e valor-hora válido são obrigatórios.");
        if (vigenciaFim != null && vigenciaFim.isBefore(vigenciaInicio))
            throw new IllegalStateException("Fim da vigência não pode anteceder o início.");
        if (adicionalNoturnoPercent == null) adicionalNoturnoPercent = BigDecimal.ZERO;
        if (adicionalNoturnoPercent.signum() < 0 || adicionalNoturnoPercent.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalStateException("O adicional noturno deve estar entre 0% e 100%.");
        if (ativo == null) ativo = true;
    }
}
