package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "historico_empregado",
        // Indexes improve lookup speed when filtering by empregado, data do evento ou usuário.  Versão já indexada para controle de concorrência.
        indexes = {
                @Index(name = "idx_historico_empregado_version", columnList = "version"),
                @Index(name = "idx_historico_empregado_empregado", columnList = "id_empregado"),
                @Index(name = "idx_historico_empregado_data", columnList = "data_evento"),
                @Index(name = "idx_historico_empregado_usuario", columnList = "id_usuario"),
                @Index(name = "idx_historico_empregado_gestao_local_data", columnList = "gestao_local_snapshot_id,data_evento"),
                @Index(name = "idx_historico_empregado_se_data", columnList = "superintendencia_snapshot_id,data_evento")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class HistoricoEmpregado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    // Alterado de Date para LocalDateTime
    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = true)
    private UsuarioAplicacao usuario;

    /**
     * Escopo organizacional imutável do evento. O vínculo atual do empregado
     * pode mudar depois de uma transferência e não pode redefinir quem enxerga
     * os períodos históricos anteriores.
     */
    @Column(name = "gestao_local_snapshot_id", nullable = false)
    private Long gestaoLocalSnapshotId;

    @Column(name = "superintendencia_snapshot_id", nullable = false)
    private Long superintendenciaSnapshotId;

    @Column(name = "hash_integridade", nullable = false, length = 128)
    private String hashIntegridade;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public HistoricoEmpregado() {}

    @PrePersist
    protected void preencherEscopoHistorico() {
        if (empregado == null || empregado.getGestaoLocal() == null
                || empregado.getGestaoLocal().getId() == null
                || empregado.getGestaoLocal().getSuperintendencia() == null
                || empregado.getGestaoLocal().getSuperintendencia().getId() == null) {
            throw new IllegalStateException("O histórico exige o escopo de Gestão Local e Gestão Estadual do empregado.");
        }
        if (gestaoLocalSnapshotId == null) {
            gestaoLocalSnapshotId = empregado.getGestaoLocal().getId();
        }
        if (superintendenciaSnapshotId == null) {
            superintendenciaSnapshotId = empregado.getGestaoLocal().getSuperintendencia().getId();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }

    public LocalDateTime getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDateTime dataEvento) { this.dataEvento = dataEvento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public UsuarioAplicacao getUsuario() { return usuario; }
    public void setUsuario(UsuarioAplicacao usuario) { this.usuario = usuario; }

    public Long getGestaoLocalSnapshotId() { return gestaoLocalSnapshotId; }
    public void setGestaoLocalSnapshotId(Long gestaoLocalSnapshotId) { this.gestaoLocalSnapshotId = gestaoLocalSnapshotId; }

    public Long getSuperintendenciaSnapshotId() { return superintendenciaSnapshotId; }
    public void setSuperintendenciaSnapshotId(Long superintendenciaSnapshotId) { this.superintendenciaSnapshotId = superintendenciaSnapshotId; }

    public String getHashIntegridade() { return hashIntegridade; }
    public void setHashIntegridade(String hashIntegridade) { this.hashIntegridade = hashIntegridade; }
}
