package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;

/** Titularidade permanente de um número/ano publicado, inclusive após correção. */
@Entity
@Table(name = "reserva_identificador_contrato", uniqueConstraints =
        @UniqueConstraint(name = "uk_reserva_identificador_contrato", columnNames = {"numero", "ano"}),
        indexes = @Index(name = "idx_reserva_identificador_titular", columnList = "id_contrato"))
public class ReservaIdentificadorContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva_identificador")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @Column(name = "numero", nullable = false, length = 50, updatable = false)
    private String numero;

    @Column(name = "ano", nullable = false, updatable = false)
    private Integer ano;

    /** Data de registro da reserva, sem inventar a data de publicação da fonte anterior. */
    @Column(name = "reservado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant reservadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @PrePersist
    protected void validar() {
        numero = normalizarNumero(numero);
        if (contrato == null || numero == null || numero.length() > 50 || ano == null || ano < 1900 || ano > 9999)
            throw new IllegalStateException("Reserva de identificador contratual incompleta ou inválida.");
        if (reservadoEm == null) reservadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    }

    public static String normalizarNumero(String numero) {
        return numero == null || numero.isBlank() ? null : numero.trim().toUpperCase(Locale.ROOT);
    }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public Instant getReservadoEm() { return reservadoEm; }
}
