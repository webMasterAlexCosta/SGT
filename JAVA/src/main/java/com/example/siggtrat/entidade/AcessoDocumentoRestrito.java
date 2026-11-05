package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.documento.*;
import jakarta.persistence.*;

import java.time.Instant;

/** Registro imutável de cada acesso autorizado a conteúdo Pessoal/Restrito. */
@Entity
@Table(name = "documento_restrito_acesso", indexes = {
        @Index(name = "idx_documento_acesso_documento", columnList = "id_documento, acessado_em"),
        @Index(name = "idx_documento_acesso_usuario", columnList = "usuario_id, acessado_em")
})
public class AcessoDocumentoRestrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_acesso")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento", nullable = false, updatable = false)
    private DocumentoComprobatorio documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acesso", nullable = false, length = 32, updatable = false)
    private TipoAcessoDocumento tipoAcesso;

    @Column(name = "finalidade", nullable = false, length = 500, updatable = false)
    private String finalidade;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "usuario_username", nullable = false, length = 120, updatable = false)
    private String usuarioUsername;

    @Column(name = "perfis_fotografia", length = 400, updatable = false)
    private String perfisFotografia;

    @Column(name = "permissao_id", updatable = false)
    private Long permissaoId;

    @Column(name = "acessado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant acessadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @PrePersist
    protected void validar() {
        if (documento == null || tipoAcesso == null || finalidade == null || finalidade.isBlank()
                || usuarioUsername == null || usuarioUsername.isBlank()) {
            throw new IllegalStateException("Auditoria de acesso restrito incompleta.");
        }
        if (acessadoEm == null) acessadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        finalidade = finalidade.trim().replaceAll("\\s+", " ");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoComprobatorio getDocumento() { return documento; }
    public void setDocumento(DocumentoComprobatorio documento) { this.documento = documento; }
    public TipoAcessoDocumento getTipoAcesso() { return tipoAcesso; }
    public void setTipoAcesso(TipoAcessoDocumento tipoAcesso) { this.tipoAcesso = tipoAcesso; }
    public String getFinalidade() { return finalidade; }
    public void setFinalidade(String finalidade) { this.finalidade = finalidade; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }
    public String getPerfisFotografia() { return perfisFotografia; }
    public void setPerfisFotografia(String perfisFotografia) { this.perfisFotografia = perfisFotografia; }
    public Long getPermissaoId() { return permissaoId; }
    public void setPermissaoId(Long permissaoId) { this.permissaoId = permissaoId; }
    public Instant getAcessadoEm() { return acessadoEm; }
    public void setAcessadoEm(Instant acessadoEm) { this.acessadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(acessadoEm); }
}

