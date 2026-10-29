package com.example.siggtrat.entidade;

import com.example.siggtrat.utilitario.UtilitarioHashSeguranca;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "token_ativo",
        indexes = {
                @Index(name = "idx_token_ativo_version", columnList = "version"),
                @Index(name = "idx_token_ativo_username", columnList = "username"),
                @Index(name = "idx_token_ativo_expire", columnList = "expire_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_token_ativo_hash", columnNames = "token_hash")
)
public class TokenAtivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Somente o SHA-256 do JWT é persistido; o token utilizável fica apenas no cookie HttpOnly. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "device_hash", length = 64)
    private String deviceHash;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public TokenAtivo() {}

    public TokenAtivo(String tokenHash, String username, LocalDateTime createdAt, LocalDateTime expireAt) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.createdAt = createdAt;
        this.expireAt = expireAt;
    }

    public static TokenAtivo deTokenBruto(String rawToken, String username, LocalDateTime createdAt, LocalDateTime expireAt) {
        return new TokenAtivo(UtilitarioHashSeguranca.sha256Hexadecimal(rawToken), username, createdAt, expireAt);
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public String getDeviceHash() { return deviceHash; }
    public void setDeviceHash(String deviceHash) { this.deviceHash = deviceHash; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
