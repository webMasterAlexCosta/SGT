package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Trilha imutável de cada consumo de uma permissão excepcional. */
@Entity
@Immutable
@Table(name = "permissao_excepcional_uso", indexes = {
        @Index(name = "idx_permissao_uso_permissao", columnList = "id_permissao_excepcional, usado_em"),
        @Index(name = "idx_permissao_uso_usuario", columnList = "id_usuario, usado_em")
})
public class UsoPermissaoExcepcional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permissao_uso")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_permissao_excepcional", nullable = false, updatable = false)
    private PermissaoExcepcional permissao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 56, updatable = false)
    private TipoPermissaoExcepcional tipo;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "usuario_username", nullable = false, length = 120, updatable = false)
    private String usuarioUsername;

    @Column(name = "tipo_recurso", nullable = false, length = 80, updatable = false)
    private String tipoRecurso;

    @Column(name = "id_recurso", updatable = false)
    private Long recursoId;

    @Column(name = "finalidade", nullable = false, length = 500, updatable = false)
    private String finalidade;

    @Column(name = "usado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant usadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @PrePersist
    protected void validar() {
        if (permissao == null || tipo == null || usuarioId == null || usuarioUsername == null
                || usuarioUsername.isBlank() || tipoRecurso == null || tipoRecurso.isBlank()
                || finalidade == null || finalidade.isBlank()) {
            throw new IllegalStateException("Registro de uso de permissão incompleto.");
        }
        if (usadoEm == null) usadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        usuarioUsername = usuarioUsername.trim();
        tipoRecurso = tipoRecurso.trim();
        finalidade = finalidade.trim().replaceAll("\\s+", " ");
    }

    public Long getId() { return id; }
    public PermissaoExcepcional getPermissao() { return permissao; }
    public void setPermissao(PermissaoExcepcional permissao) { this.permissao = permissao; }
    public TipoPermissaoExcepcional getTipo() { return tipo; }
    public void setTipo(TipoPermissaoExcepcional tipo) { this.tipo = tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }
    public String getTipoRecurso() { return tipoRecurso; }
    public void setTipoRecurso(String tipoRecurso) { this.tipoRecurso = tipoRecurso; }
    public Long getRecursoId() { return recursoId; }
    public void setRecursoId(Long recursoId) { this.recursoId = recursoId; }
    public String getFinalidade() { return finalidade; }
    public void setFinalidade(String finalidade) { this.finalidade = finalidade; }
    public Instant getUsadoEm() { return usadoEm; }
}
