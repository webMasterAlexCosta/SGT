package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.frequencia.TipoEventoFrequencia;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "evento_frequencia", indexes = {
        @Index(name = "idx_evento_frequencia_registro", columnList = "id_frequencia, numero_versao"),
        @Index(name = "idx_evento_frequencia_data", columnList = "data_referencia, registrado_em"),
        @Index(name = "idx_evento_frequencia_usuario", columnList = "usuario_id, registrado_em")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_evento_frequencia_versao", columnNames = {"id_frequencia", "numero_versao"}),
        @UniqueConstraint(name = "uk_evento_frequencia_idempotencia", columnNames = "chave_idempotencia")
})
public class EventoFrequencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_frequencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_frequencia", nullable = false, updatable = false)
    private Frequencia frequencia;

    @Column(name = "numero_versao", nullable = false)
    private Integer numeroVersao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoFrequencia tipoEvento;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "snapshot_anterior", columnDefinition = "text")
    private String snapshotAnterior;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "snapshot_novo", nullable = false, columnDefinition = "text")
    private String snapshotNovo;

    @Column(name = "motivo", length = 1000)
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_username", length = 120)
    private String usuarioUsername;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    /** Informada pelo cliente somente em comandos que precisam de idempotência formal. */
    @Column(name = "chave_idempotencia", length = 120)
    private String chaveIdempotencia;

    @Column(name = "hash_intencao", length = 96)
    private String hashIntencao;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(name = "hash_evento", nullable = false, length = 64)
    private String hashEvento;

    @PrePersist
    protected void validar() {
        if (frequencia == null || numeroVersao == null || numeroVersao < 1 || tipoEvento == null
                || dataReferencia == null || snapshotNovo == null || snapshotNovo.isBlank()) {
            throw new IllegalStateException("Evento de frequência incompleto.");
        }
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        chaveIdempotencia = normalizar(chaveIdempotencia);
        motivo = normalizar(motivo);
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String n = valor.trim().replaceAll("\\s+", " ");
        return n.isBlank() ? null : n;
    }

    public Long getId() { return id; }
    public Frequencia getFrequencia() { return frequencia; }
    public void setFrequencia(Frequencia frequencia) { this.frequencia = frequencia; }
    public Integer getNumeroVersao() { return numeroVersao; }
    public void setNumeroVersao(Integer numeroVersao) { this.numeroVersao = numeroVersao; }
    public TipoEventoFrequencia getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoFrequencia tipoEvento) { this.tipoEvento = tipoEvento; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }
    public String getSnapshotAnterior() { return snapshotAnterior; }
    public void setSnapshotAnterior(String snapshotAnterior) { this.snapshotAnterior = snapshotAnterior; }
    public String getSnapshotNovo() { return snapshotNovo; }
    public void setSnapshotNovo(String snapshotNovo) { this.snapshotNovo = snapshotNovo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) { this.registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(registradoEm); }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }
    public String getHashEvento() { return hashEvento; }
    public void setHashEvento(String hashEvento) { this.hashEvento = hashEvento; }
    public String getHashIntencao() { return hashIntencao; }
    public void setHashIntencao(String hashIntencao) { this.hashIntencao = hashIntencao; }

}
