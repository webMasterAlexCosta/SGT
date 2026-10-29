package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import java.time.Instant;

/** Reserva histórica permanente: registros desta tabela nunca são reutilizados nem removidos. */
@Entity
@Table(name = "reserva_matricula_contrato", uniqueConstraints =
        @UniqueConstraint(name = "uk_reserva_matricula_contrato", columnNames = {"id_contrato", "matricula"}),
        indexes = @Index(name = "idx_reserva_matricula_vinculo", columnList = "id_vinculo_contratual"))
public class ReservaMatriculaContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva_matricula")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vinculo_contratual", nullable = false, updatable = false)
    private VinculoContratual vinculo;
    @Column(name = "matricula", nullable = false, length = 30, updatable = false)
    private String matricula;
    @Column(name = "reservado_em", nullable = false, updatable = false)
    private Instant reservadoEm = Instant.now();

    @PrePersist
    protected void normalizar() {
        matricula = VinculoContratual.normalizarMatricula(matricula);
        if (contrato == null || vinculo == null || matricula == null) throw new IllegalStateException("Reserva de matrícula incompleta.");
        if (reservadoEm == null) reservadoEm = Instant.now();
    }
    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public VinculoContratual getVinculo() { return vinculo; }
    public void setVinculo(VinculoContratual vinculo) { this.vinculo = vinculo; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public Instant getReservadoEm() { return reservadoEm; }
}
