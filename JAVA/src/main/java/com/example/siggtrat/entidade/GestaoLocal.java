package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "gestao_local",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_gestao_local_superintendencia_codigo",
                        columnNames = {"id_superintendencia", "codigo"}
                )
        },
        indexes = {
                @Index(name = "idx_gestao_local_superintendencia", columnList = "id_superintendencia"),
                @Index(name = "idx_gestao_local_ativo", columnList = "ativo")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class GestaoLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gestao_local")
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_superintendencia", nullable = false)
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private SuperintendenciaEstadual superintendencia;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public GestaoLocal() {
    }

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (nome != null) nome = nome.trim();
        if (codigo != null) codigo = codigo.trim().toUpperCase();
        if (ativo == null) ativo = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public SuperintendenciaEstadual getSuperintendencia() { return superintendencia; }
    public void setSuperintendencia(SuperintendenciaEstadual superintendencia) { this.superintendencia = superintendencia; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
