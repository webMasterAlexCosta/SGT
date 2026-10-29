package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fatura_terceirizacao", uniqueConstraints = @UniqueConstraint(name = "uk_fatura_folha_horas", columnNames = "id_folha_horas"))
@Getter @Setter @NoArgsConstructor
public class Fatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fatura") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_folha_horas", nullable = false)
    private FolhaHoras folhaHoras;
    @Column(nullable = false, length = 25) private String status = "RASCUNHO";
    @Column(name = "valor_bruto", nullable = false, precision = 17, scale = 2) private BigDecimal valorBruto;
    @Column(nullable = false, precision = 17, scale = 2) private BigDecimal glosas = BigDecimal.ZERO;
    @Column(name = "valor_liquido", nullable = false, precision = 17, scale = 2) private BigDecimal valorLiquido;
    @Column(name = "numero_documento", length = 80) private String numeroDocumento;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm;
    @Column(name = "emitido_em") private LocalDateTime emitidoEm;
    @Column(name = "pago_em") private LocalDateTime pagoEm;
    @Version private long version;
    @PrePersist void criar() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
}
