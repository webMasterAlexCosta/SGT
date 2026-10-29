package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "folha_horas", uniqueConstraints = @UniqueConstraint(name = "uk_folha_horas_contrato_periodo", columnNames = {"id_contrato","periodo_inicio","periodo_fim"}))
@Getter @Setter @NoArgsConstructor
public class FolhaHoras {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_folha_horas") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;
    @Column(name = "periodo_inicio", nullable = false) private LocalDate periodoInicio;
    @Column(name = "periodo_fim", nullable = false) private LocalDate periodoFim;
    @Column(nullable = false, length = 25) private String status = "RASCUNHO";
    @Column(name = "minutos_fisicos", nullable = false) private Integer minutosFisicos = 0;
    @Column(name = "minutos_noturnos", nullable = false) private Integer minutosNoturnos = 0;
    @Column(name = "minutos_adicionais", nullable = false) private Integer minutosAdicionais = 0;
    @Column(name = "minutos_pagaveis", nullable = false) private Integer minutosPagaveis = 0;
    @Column(name = "valor_apurado", nullable = false, precision = 17, scale = 2) private BigDecimal valorApurado = BigDecimal.ZERO;
    @Column(name = "calculo_versao", nullable = false, length = 40) private String calculoVersao;
    @Column(name = "submetido_por", length = 100) private String submetidoPor;
    @Column(name = "submetido_em") private LocalDateTime submetidoEm;
    @Column(name = "aprovado_por", length = 100) private String aprovadoPor;
    @Column(name = "aprovado_em") private LocalDateTime aprovadoEm;
    @Column(name = "motivo_rejeicao", length = 500) private String motivoRejeicao;
    @Version private long version;
}
