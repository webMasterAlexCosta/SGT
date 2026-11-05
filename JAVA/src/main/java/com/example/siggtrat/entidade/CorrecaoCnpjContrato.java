package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Fato material imutável, separado de troca de empresa e de dados mestres de fornecedor. */
@Entity
@Table(name = "contrato_correcao_cnpj", uniqueConstraints = @UniqueConstraint(
        name = "uk_correcao_cnpj_idempotencia", columnNames = {"id_contrato", "chave_idempotencia"}))
@Getter @Setter
public class CorrecaoCnpjContrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false, updatable = false)
    private EventoCicloVidaContrato evento;
    @Column(name = "cnpj_anterior", nullable = false, length = 14, updatable = false)
    private String cnpjAnterior;
    @Column(name = "cnpj_corrigido", nullable = false, length = 14, updatable = false)
    private String cnpjCorrigido;
    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;
    @Column(name = "data_ato", nullable = false, updatable = false)
    private LocalDate dataAto;
    @Column(nullable = false, length = 1000, updatable = false)
    private String motivo;
    @Column(name = "referencia_sei", nullable = false, length = 120, updatable = false)
    private String referenciaSei;
    @Column(name = "chave_idempotencia", nullable = false, length = 80, updatable = false)
    private String chaveIdempotencia;
    @Column(name = "assinatura_requisicao", nullable = false, length = 64, updatable = false)
    private String assinaturaRequisicao;
    @Column(name = "responsavel_id", nullable = false, updatable = false)
    private Long responsavelId;
    @Column(name = "responsavel_username", nullable = false, length = 120, updatable = false)
    private String responsavelUsername;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros().truncatedTo(ChronoUnit.MICROS);
}
