package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import jakarta.persistence.UniqueConstraint;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(
        name = "conta",
        indexes = {
                @Index(name = "idx_conta_version", columnList = "version"),
                @Index(name = "idx_conta_agencia", columnList = "id_agencia"),
                @Index(name = "idx_conta_tipo", columnList = "id_tipo_conta")
        },
        // Evita duplicidade de contas em uma mesma agência. Cada agência não pode ter duas contas com o mesmo número.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conta_agencia_numero", columnNames = {"id_agencia", "numero_conta"})
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta")
    private Long id;

    //@NotBlank(message = "O número da conta é obrigatório")
    @Column(name = "numero_conta", length = 30, nullable = false)
    private String numeroConta;

    //@NotNull(message = "A agência é obrigatória")
    // Use LAZY fetch to avoid loading the agency record unless explicitly accessed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agencia", nullable = false)
    private Agencia agencia;

    //@NotNull(message = "O tipo de conta é obrigatório")
    // Use LAZY fetch to avoid loading the type-of-account unless explicitly accessed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_conta", nullable = false)
    private TipoConta tipoConta;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void normalizarEValidar() {
        if (numeroConta == null || numeroConta.isBlank()) {
            throw new IllegalStateException("O número da conta é obrigatório.");
        }
        numeroConta = numeroConta.trim().replaceAll("\\s+", "");
        if (agencia == null) {
            throw new IllegalStateException("A conta deve possuir uma agência.");
        }
        if (tipoConta == null) {
            throw new IllegalStateException("A conta deve possuir um tipo.");
        }
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Conta() {}

    public Conta(String numeroConta, Agencia agencia, TipoConta tipoConta) {
        this.numeroConta = numeroConta;
        this.agencia = agencia;
        this.tipoConta = tipoConta;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }
    public Agencia getAgencia() { return agencia; }
    public void setAgencia(Agencia agencia) { this.agencia = agencia; }
    public TipoConta getTipoConta() { return tipoConta; }
    public void setTipoConta(TipoConta tipoConta) { this.tipoConta = tipoConta; }
}