package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(
        name = "vinculo_gestao_usuario_aplicacao",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vinculo_gestao_usuario_aplicacao_user_lotacao_jornada",
                        columnNames = {"usuario_aplicacao_id", "id_lotacao", "id_jornada"}
                )
        },
        indexes = {
                @Index(name = "idx_vinculo_gestao_usuario_aplicacao_user", columnList = "usuario_aplicacao_id"),
                @Index(name = "idx_vinculo_gestao_usuario_aplicacao_lotacao", columnList = "id_lotacao"),
                @Index(name = "idx_vinculo_gestao_usuario_aplicacao_jornada", columnList = "id_jornada")
        }
)
@JsonIgnoreProperties(value = {"user", "hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class VinculoGestaoUsuarioAplicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_aplicacao_id", nullable = false)
    private UsuarioAplicacao user;

    // Use LAZY fetch for lotacao to avoid unnecessary joins when retrieving vinculos.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false)
    @JsonIgnoreProperties(value = {"turnos"}, allowSetters = true)
    private Lotacao lotacao;

    // Use LAZY fetch for jornada to avoid unnecessary joins when retrieving vinculos.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    @JsonIgnoreProperties(value = {"lotacoes", "regrasAdicionais"}, allowSetters = true)
    private Jornada jornada;

    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void validarIntegridadeOrganizacional() {
        if (user == null || lotacao == null || jornada == null) {
            throw new IllegalStateException("O vínculo de gestão exige usuário, lotação e jornada.");
        }
        if (lotacao.getGestaoLocal() == null || jornada.getGestaoLocal() == null) {
            throw new IllegalStateException("Lotação e jornada do vínculo devem possuir Gestão Local.");
        }
        if (lotacao.getGestaoLocal().getId() != null && jornada.getGestaoLocal().getId() != null
                && !java.util.Objects.equals(
                        lotacao.getGestaoLocal().getId(),
                        jornada.getGestaoLocal().getId()
                )) {
            throw new IllegalStateException(
                    "Lotação e jornada do vínculo de gestão devem pertencer à mesma Gestão Local."
            );
        }
    }

    public VinculoGestaoUsuarioAplicacao() {
    }

    public VinculoGestaoUsuarioAplicacao(UsuarioAplicacao user, Lotacao lotacao, Jornada jornada) {
        this.user = user;
        this.lotacao = lotacao;
        this.jornada = jornada;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsuarioAplicacao getUser() {
        return user;
    }

    public void setUser(UsuarioAplicacao user) {
        this.user = user;
    }

    public Lotacao getLotacao() {
        return lotacao;
    }

    public void setLotacao(Lotacao lotacao) {
        this.lotacao = lotacao;
    }

    public Jornada getJornada() {
        return jornada;
    }

    public void setJornada(Jornada jornada) {
        this.jornada = jornada;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
