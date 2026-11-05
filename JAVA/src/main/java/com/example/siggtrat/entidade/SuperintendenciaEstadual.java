package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "superintendencia_estadual",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_superintendencia_estadual_uf", columnNames = "uf")
        },
        indexes = {
                @Index(name = "idx_superintendencia_estadual_ativo", columnList = "ativo")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class SuperintendenciaEstadual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_superintendencia")
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public SuperintendenciaEstadual() {
    }

    public SuperintendenciaEstadual(String nome, String uf) {
        this.nome = nome;
        this.uf = uf;
    }

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (uf != null) {
            uf = uf.trim().toUpperCase();
            nome = "SE/" + uf;
        } else if (nome != null) {
            nome = nome.trim();
        }
        if (ativo == null) ativo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
