package com.example.siggtrat.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "recuperacao_senha_token",
        indexes = {
                @Index(name = "idx_recuperacao_senha_usuario", columnList = "usuario_id"),
                @Index(name = "idx_recuperacao_senha_expira", columnList = "expire_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recuperacao_senha_token_hash",
                columnNames = "token_hash"
        )
)
public class RecuperacaoSenhaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** O token utilizável nunca é persistido; somente seu SHA-256. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioAplicacao usuario;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "request_ip_hash", length = 64)
    private String requestIpHash;

    @Version
    @Column(name = "version")
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public UsuarioAplicacao getUsuario() { return usuario; }
    public void setUsuario(UsuarioAplicacao usuario) { this.usuario = usuario; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public String getRequestIpHash() { return requestIpHash; }
    public void setRequestIpHash(String requestIpHash) { this.requestIpHash = requestIpHash; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
