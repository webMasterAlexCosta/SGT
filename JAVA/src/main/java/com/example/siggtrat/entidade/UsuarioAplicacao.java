package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario_aplicacao", indexes = {
        @Index(name = "idx_usuario_aplicacao_version", columnList = "version"),
        @Index(name = "idx_usuario_aplicacao_superintendencia", columnList = "id_superintendencia"),
        @Index(name = "idx_usuario_aplicacao_gestao_local", columnList = "id_gestao_local")
})
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class UsuarioAplicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    /** Identificador funcional utilizado no autenticar. */
    @Column(unique = true, length = 8)
    private String matricula;

    @Column(nullable = false, unique = true)
    private String email;

    /** Perfil principal. Autorizações excepcionais são entidades separadas. */
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 32)
    private PerfilAcesso perfil = PerfilAcesso.OPERACIONAL;

    // --- RELACIONAMENTOS ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lotacao")
    private Lotacao lotacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jornada")
    private Jornada jornada;

    /**
     * Escopo organizacional. O presidente/admin não possui vínculo obrigatório;
     * a superintendência possui somente a SE e a Gestão Local possui ambos.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superintendencia")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private SuperintendenciaEstadual superintendencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gestao_local")
    @JsonIgnoreProperties(value = {"superintendencia", "hibernateLazyInitializer", "handler"}, allowSetters = true)
    private GestaoLocal gestaoLocal;

    // Use LAZY loading for management scope relationships. Loading all management links on every user
    // fetch can be expensive when the application lists many users.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"user"}, allowSetters = true)
    private List<VinculoGestaoUsuarioAplicacao> vinculosGestao = new ArrayList<>();

    // --- SEGURANÇA E CONTROLO ---

    // Flag que indica se o usuário está ativo. Não pode ser nulo. Padrão false para exigir aprovação.
    @Column(nullable = false)
    private Boolean active = false;

    // Força a troca de senha (Padrão true)
    // Obliga o usuário a trocar a senha no próximo autenticar. Não pode ser nulo. Padrão true.
    @Column(name = "force_change_password", nullable = false)
    private Boolean forceChangePassword = true;

    @Column(name = "secret_2fa")
    private String secret2fa;

    @Column(name = "pending_secret_2fa")
    private String pendingSecret2fa;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled = false;

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    // Validade do desbloqueio retroativo
    @Column(name = "retroactive_unlock_expire_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime retroactiveUnlockExpireAt;

    // Foto do Perfil
    @Column(name = "foto_url")
    private String fotoUrl;

    // --- NOVO CAMPO: DATA DE CRIAÇÃO DA CONTA ---
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    // --- MÉTODOS DE CICLO DE VIDA (JPA) ---

    @PrePersist
    protected void aoCriar() {
        if (this.createdAt == null) {
            this.createdAt = com.example.siggtrat.utilitario.RelogioSistema.agora();
        }
        if (perfil == null) perfil = PerfilAcesso.OPERACIONAL;
        if (twoFactorEnabled == null) twoFactorEnabled = false;
        validarEscopoOperacional();
    }

    @PreUpdate
    protected void aoAtualizar() {
        validarEscopoOperacional();
    }

    private void validarEscopoOperacional() {
        if (lotacao != null && jornada != null) {
            if (lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null) {
                throw new IllegalStateException(
                        "Lotação e jornada do usuário devem possuir Gestão Local."
                );
            }
            if (lotacao.getGestaoLocal().getId() != null && jornada.getGestaoLocal().getId() != null
                    && !java.util.Objects.equals(
                            lotacao.getGestaoLocal().getId(),
                            jornada.getGestaoLocal().getId()
                    )) {
                throw new IllegalStateException(
                        "Lotação e jornada do usuário devem pertencer à mesma Gestão Local."
                );
            }
        }
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    // --- CONSTRUTORES ---

    public UsuarioAplicacao() {
        this.active = false;
        this.failedAttempts = 0;
        this.forceChangePassword = true; // Nasce a pedir troca
    }

    // --- GETTERS E SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public PerfilAcesso getPerfil() { return perfil; }
    public void setPerfil(PerfilAcesso perfil) { this.perfil = perfil; }

    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }

    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }

    public SuperintendenciaEstadual getSuperintendencia() { return superintendencia; }
    public void setSuperintendencia(SuperintendenciaEstadual superintendencia) { this.superintendencia = superintendencia; }

    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }

    public List<VinculoGestaoUsuarioAplicacao> getVinculosGestao() { return vinculosGestao; }
    public void setVinculosGestao(List<VinculoGestaoUsuarioAplicacao> vinculosGestao) {
        this.vinculosGestao.clear();
        if (vinculosGestao == null) return;
        for (VinculoGestaoUsuarioAplicacao vinculo : vinculosGestao) {
            if (vinculo == null) continue;
            vinculo.setUser(this);
            this.vinculosGestao.add(vinculo);
        }
    }

    public void adicionarVinculoGestao(VinculoGestaoUsuarioAplicacao vinculo) {
        if (vinculo == null) return;
        vinculo.setUser(this);
        this.vinculosGestao.add(vinculo);
    }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getForceChangePassword() { return forceChangePassword; }
    public void setForceChangePassword(Boolean forceChangePassword) { this.forceChangePassword = forceChangePassword; }

    public String getSecret2fa() { return secret2fa; }
    public void setSecret2fa(String secret2fa) { this.secret2fa = secret2fa; }
    public String getPendingSecret2fa() { return pendingSecret2fa; }
    public void setPendingSecret2fa(String pendingSecret2fa) { this.pendingSecret2fa = pendingSecret2fa; }
    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockoutEnd() { return lockoutEnd; }
    public void setLockoutEnd(LocalDateTime lockoutEnd) { this.lockoutEnd = lockoutEnd; }

    public LocalDateTime getRetroactiveUnlockExpireAt() { return retroactiveUnlockExpireAt; }
    public void setRetroactiveUnlockExpireAt(LocalDateTime retroactiveUnlockExpireAt) { this.retroactiveUnlockExpireAt = retroactiveUnlockExpireAt; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
