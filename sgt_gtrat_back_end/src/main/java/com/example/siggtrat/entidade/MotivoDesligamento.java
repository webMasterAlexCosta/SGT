package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "motivo_desligamento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_motivo_desligamento_gestao_descricao",
                columnNames = {"id_gestao_local", "descricao"}
        ),
        indexes = {
                @Index(name = "idx_motivo_desligamento_gestao", columnList = "id_gestao_local"),
                @Index(name = "idx_motivo_desligamento_ativo", columnList = "ativo"),
                @Index(name = "idx_motivo_desligamento_categoria", columnList = "categoria")
        }
)
public class MotivoDesligamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_motivo_desligamento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false, updatable = false)
    private GestaoLocal gestaoLocal;

    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 48)
    private CategoriaTerminoVinculo categoria;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

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
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalStateException("Descrição do Motivo de Desligamento é obrigatória.");
        }
        if (categoria == null) {
            throw new IllegalStateException("Categoria do Motivo de Desligamento é obrigatória.");
        }
        if (ativo == null) ativo = true;
        if (gestaoLocal == null) {
            throw new IllegalStateException("Motivo de Desligamento deve pertencer a uma Gestão Local.");
        }
    }

    public Long getId() { return id; }
    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public CategoriaTerminoVinculo getCategoria() { return categoria; }
    public void setCategoria(CategoriaTerminoVinculo categoria) { this.categoria = categoria; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Long getCriadoPorId() { return criadoPorId; }
    public void setCriadoPorId(Long criadoPorId) { this.criadoPorId = criadoPorId; }
    public String getCriadoPorUsername() { return criadoPorUsername; }
    public void setCriadoPorUsername(String criadoPorUsername) { this.criadoPorUsername = criadoPorUsername; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public long getVersion() { return version; }
}
