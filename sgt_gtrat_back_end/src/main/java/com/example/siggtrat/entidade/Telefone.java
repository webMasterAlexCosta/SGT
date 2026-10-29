package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonBackReference; // Import novo

@Entity
@Table(name = "telefone", indexes = { @Index(name = "idx_telefone_version", columnList = "version") })
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Telefone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefone")
    private Long id;

    @Column(name = "numero", length = 20, nullable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empregado", nullable = false)
    @JsonBackReference // Impede que o JSON mostre o empregado completo dentro do telefone
    private Empregado empregado;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Telefone() {}
    public Telefone(String numero, Empregado empregado) {
        this.numero = numero;
        this.empregado = empregado;
    }
    // Getters e Setters normais...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
}