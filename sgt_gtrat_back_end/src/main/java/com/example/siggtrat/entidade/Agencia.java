package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "agencia",
        indexes = {
                @Index(name = "idx_agencia_version", columnList = "version"),
                @Index(name = "idx_agencia_banco", columnList = "id_banco")
        },
        // Garante que não existam duas agências com o mesmo número dentro de um mesmo banco.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agencia_banco_numero", columnNames = {"id_banco", "numero"})
        }
)
public class Agencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agencia")
    private Long id;

    @Column(name = "numero", length = 20, nullable = false)
    private String numero;

    // Use LAZY fetch to avoid loading the bank entity unless needed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_banco", nullable = false)
    private Banco banco;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void normalizarEValidar() {
        if (numero == null || numero.isBlank()) {
            throw new IllegalStateException("O número da agência é obrigatório.");
        }
        numero = numero.trim().replaceAll("\\s+", "");
        if (banco == null) {
            throw new IllegalStateException("A agência deve estar vinculada a um banco.");
        }
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Agencia() {}
    public Agencia(String numero, Banco banco) {
        this.numero = numero;
        this.banco = banco;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Banco getBanco() { return banco; }
    public void setBanco(Banco banco) { this.banco = banco; }
}