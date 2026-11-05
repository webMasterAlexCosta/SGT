package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Relação explícita entre dois eventos do mesmo contrato.
 *
 * eventoFonte é o fato que declara a relação e eventoAlvo é o fato anterior
 * referenciado. Exemplos:
 * - fonte RETIFICA alvo;
 * - fonte CANCELA alvo;
 * - fonte SUBSTITUI alvo;
 * - fonte SUCEDE alvo.
 */
@Entity
@Table(
        name = "contrato_evento_precedencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_evento_precedencia_fonte_alvo_tipo",
                columnNames = {"id_evento_fonte", "id_evento_alvo", "tipo_relacao"}
        ),
        indexes = {
                @Index(name = "idx_evento_precedencia_contrato", columnList = "id_contrato"),
                @Index(name = "idx_evento_precedencia_fonte", columnList = "id_evento_fonte"),
                @Index(name = "idx_evento_precedencia_alvo", columnList = "id_evento_alvo"),
                @Index(name = "idx_evento_precedencia_registrado", columnList = "registrado_em")
        }
)
public class RelacaoPrecedenciaEventoContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_precedencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento_fonte", nullable = false, updatable = false)
    private EventoCicloVidaContrato eventoFonte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento_alvo", nullable = false, updatable = false)
    private EventoCicloVidaContrato eventoAlvo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_relacao", nullable = false, length = 32, updatable = false)
    private TipoRelacaoPrecedenciaNegocial tipoRelacao;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "usuario_responsavel_id", updatable = false)
    private Long usuarioResponsavelId;

    @Column(name = "usuario_responsavel_username", length = 120, updatable = false)
    private String usuarioResponsavelUsername;

    @Column(name = "motivo", length = 1000, updatable = false)
    private String motivo;

    public RelacaoPrecedenciaEventoContrato() {}

    @PrePersist
    protected void validar() {
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (contrato == null || eventoFonte == null || eventoAlvo == null || tipoRelacao == null) {
            throw new IllegalStateException("Relação de precedência incompleta.");
        }
        if (eventoFonte.getId() != null && eventoFonte.getId().equals(eventoAlvo.getId())) {
            throw new IllegalStateException("Um evento não pode declarar precedência sobre ele mesmo.");
        }
        if (motivo != null) motivo = motivo.trim().replaceAll("\\s+", " ");
    }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public EventoCicloVidaContrato getEventoFonte() { return eventoFonte; }
    public void setEventoFonte(EventoCicloVidaContrato eventoFonte) { this.eventoFonte = eventoFonte; }
    public EventoCicloVidaContrato getEventoAlvo() { return eventoAlvo; }
    public void setEventoAlvo(EventoCicloVidaContrato eventoAlvo) { this.eventoAlvo = eventoAlvo; }
    public TipoRelacaoPrecedenciaNegocial getTipoRelacao() { return tipoRelacao; }
    public void setTipoRelacao(TipoRelacaoPrecedenciaNegocial tipoRelacao) { this.tipoRelacao = tipoRelacao; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getUsuarioResponsavelId() { return usuarioResponsavelId; }
    public void setUsuarioResponsavelId(Long usuarioResponsavelId) { this.usuarioResponsavelId = usuarioResponsavelId; }
    public String getUsuarioResponsavelUsername() { return usuarioResponsavelUsername; }
    public void setUsuarioResponsavelUsername(String usuarioResponsavelUsername) { this.usuarioResponsavelUsername = usuarioResponsavelUsername; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
