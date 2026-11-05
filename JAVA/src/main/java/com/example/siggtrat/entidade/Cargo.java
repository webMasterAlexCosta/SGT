package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "cargo",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cargo_gestao_local_nome",
                columnNames = {"id_gestao_local", "nome"}
        ),
        indexes = {
                @Index(name = "idx_cargo_version", columnList = "version"),
                @Index(name = "idx_cargo_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_cargo_ativo", columnList = "ativo")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Cargo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo")
    private Long id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "cbo", length = 20)
    private String cbo;

    @Column(name = "descricao", length = 255)
    private String descricao;

    /** Gestão Local proprietária do cadastro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    private GestaoLocal gestaoLocal;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public Cargo() {}
    public Cargo(String nome, String cbo) {
        this.nome = nome;
        this.cbo = cbo;
    }

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (nome != null) nome = nome.trim().replaceAll("\\s+", " ");
        if (cbo != null) {
            cbo = cbo.trim();
            if (cbo.isBlank()) cbo = null;
        }
        if (descricao != null) {
            descricao = descricao.trim().replaceAll("\\s+", " ");
            if (descricao.isBlank()) descricao = null;
        }
        if (ativo == null) ativo = true;
        if (nome == null || nome.isBlank()) throw new IllegalStateException("O nome do cargo é obrigatório.");
        if (gestaoLocal == null) throw new IllegalStateException("O cargo deve possuir uma Gestão Local.");
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCbo() { return cbo; }
    public void setCbo(String cbo) { this.cbo = cbo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
