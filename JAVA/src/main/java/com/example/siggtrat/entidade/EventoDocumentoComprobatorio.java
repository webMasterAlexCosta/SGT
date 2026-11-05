package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.documento.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Evento imutável da cadeia documental. */
@Entity
@Table(name = "documento_comprobatorio_evento", indexes = {
        @Index(name = "idx_documento_evento_grupo", columnList = "grupo_logico_id, registrado_em"),
        @Index(name = "idx_documento_evento_documento", columnList = "id_documento")
})
public class EventoDocumentoComprobatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false, updatable = false)
    private DocumentoComprobatorio documento;

    @Column(name = "grupo_logico_id", nullable = false, length = 48, updatable = false)
    private String grupoLogicoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 32, updatable = false)
    private TipoEventoDocumento tipoEvento;

    @Column(name = "data_efetiva", updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "responsavel_id", updatable = false)
    private Long responsavelId;

    @Column(name = "responsavel_username", length = 120, updatable = false)
    private String responsavelUsername;

    @Column(name = "motivo", length = 1200, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 24, updatable = false)
    private EstadoDocumentoComprobatorio estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", length = 24, updatable = false)
    private EstadoDocumentoComprobatorio estadoNovo;

    @Column(name = "referencia_sei_anterior", length = 180, updatable = false)
    private String referenciaSeiAnterior;

    @Column(name = "referencia_sei_nova", length = 180, updatable = false)
    private String referenciaSeiNova;

    @Column(name = "sha256_anterior", length = 64, updatable = false)
    private String sha256Anterior;

    @Column(name = "sha256_novo", length = 64, updatable = false)
    private String sha256Novo;

    @Column(name = "hash_anterior", length = 64, updatable = false)
    private String hashAnterior;

    @Column(name = "hash_evento", nullable = false, length = 64, updatable = false)
    private String hashEvento;

    @PrePersist
    protected void validar() {
        if (documento == null || grupoLogicoId == null || grupoLogicoId.isBlank()
                || tipoEvento == null || hashEvento == null || hashEvento.isBlank()) {
            throw new IllegalStateException("Evento documental incompleto.");
        }
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoComprobatorio getDocumento() { return documento; }
    public void setDocumento(DocumentoComprobatorio documento) { this.documento = documento; }
    public String getGrupoLogicoId() { return grupoLogicoId; }
    public void setGrupoLogicoId(String grupoLogicoId) { this.grupoLogicoId = grupoLogicoId; }
    public TipoEventoDocumento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoDocumento tipoEvento) { this.tipoEvento = tipoEvento; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) { this.registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(registradoEm); }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public EstadoDocumentoComprobatorio getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoDocumentoComprobatorio estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoDocumentoComprobatorio getEstadoNovo() { return estadoNovo; }
    public void setEstadoNovo(EstadoDocumentoComprobatorio estadoNovo) { this.estadoNovo = estadoNovo; }
    public String getReferenciaSeiAnterior() { return referenciaSeiAnterior; }
    public void setReferenciaSeiAnterior(String referenciaSeiAnterior) { this.referenciaSeiAnterior = referenciaSeiAnterior; }
    public String getReferenciaSeiNova() { return referenciaSeiNova; }
    public void setReferenciaSeiNova(String referenciaSeiNova) { this.referenciaSeiNova = referenciaSeiNova; }
    public String getSha256Anterior() { return sha256Anterior; }
    public void setSha256Anterior(String sha256Anterior) { this.sha256Anterior = sha256Anterior; }
    public String getSha256Novo() { return sha256Novo; }
    public void setSha256Novo(String sha256Novo) { this.sha256Novo = sha256Novo; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }
    public String getHashEvento() { return hashEvento; }
    public void setHashEvento(String hashEvento) { this.hashEvento = hashEvento; }
}

