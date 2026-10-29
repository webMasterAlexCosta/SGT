package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "tipo_contratacao",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tipo_contratacao_gestao_descricao",
                        columnNames = {"id_gestao_local", "descricao"}
                )
        },
        indexes = {
                @Index(name = "idx_tipo_contratacao_gestao", columnList = "id_gestao_local"),
                @Index(name = "idx_tipo_contratacao_ativo", columnList = "ativo")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class TipoContratacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_contratacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    private GestaoLocal gestaoLocal;

    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "matricula_vinculo_obrigatoria", nullable = false)
    private Boolean matriculaVinculoObrigatoria = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @Column(name = "criado_por_username", length = 120, updatable = false)
    private String criadoPorUsername;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Version
    @Column(name = "version")
    private long version;

    public TipoContratacao() {}

    @PrePersist
    protected void antesDePersistir() {
        normalizar();
        if (criadoEm == null) criadoEm = Instant.now();
    }

    @PreUpdate
    protected void antesDeAtualizar() {
        normalizar();
        atualizadoEm = Instant.now();
    }

    private void normalizar() {
        if (descricao != null) descricao = descricao.trim().replaceAll("\\s+", " ");
        if (ativo == null) ativo = true;
        if (matriculaVinculoObrigatoria == null) matriculaVinculoObrigatoria = false;
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalStateException("Descrição do Tipo de Contratação é obrigatória.");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Boolean getMatriculaVinculoObrigatoria() { return matriculaVinculoObrigatoria; }
    public void setMatriculaVinculoObrigatoria(Boolean valor) { this.matriculaVinculoObrigatoria = valor; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    public Long getCriadoPorId() { return criadoPorId; }
    public void setCriadoPorId(Long criadoPorId) { this.criadoPorId = criadoPorId; }

    public String getCriadoPorUsername() { return criadoPorUsername; }
    public void setCriadoPorUsername(String criadoPorUsername) { this.criadoPorUsername = criadoPorUsername; }

    public Instant getAtualizadoEm() { return atualizadoEm; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
