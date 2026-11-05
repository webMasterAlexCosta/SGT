package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;

/** Autorização explícita para um grupo lógico de documento Pessoal/Restrito. */
@Entity
@Table(name = "documento_restrito_permissao", indexes = {
        @Index(name = "idx_documento_permissao_grupo_usuario", columnList = "grupo_logico_id, id_usuario_beneficiario, ativo"),
        @Index(name = "idx_documento_permissao_validade", columnList = "valido_ate")
})
public class PermissaoDocumentoRestrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_permissao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_documento_referencia", nullable = false, updatable = false)
    private DocumentoComprobatorio documentoReferencia;

    @Column(name = "grupo_logico_id", nullable = false, length = 48, updatable = false)
    private String grupoLogicoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_beneficiario", nullable = false, updatable = false)
    private UsuarioAplicacao usuarioBeneficiario;

    @Column(name = "finalidade", nullable = false, length = 500, updatable = false)
    private String finalidade;

    @Column(name = "justificativa", nullable = false, length = 1000, updatable = false)
    private String justificativa;

    @Column(name = "autorizacao_administrativa", nullable = false, length = 180, updatable = false)
    private String autorizacaoAdministrativa;

    @Column(name = "concedido_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant concedidoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "concedido_por_id", updatable = false)
    private Long concedidoPorId;

    @Column(name = "concedido_por_username", length = 120, updatable = false)
    private String concedidoPorUsername;

    @Column(name = "valido_ate")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant validoAte;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "revogado_em")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant revogadoEm;

    @Column(name = "revogado_por_id")
    private Long revogadoPorId;

    @Column(name = "revogado_por_username", length = 120)
    private String revogadoPorUsername;

    @Column(name = "motivo_revogacao", length = 1000)
    private String motivoRevogacao;

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void validar() {
        if (documentoReferencia == null || grupoLogicoId == null || grupoLogicoId.isBlank()
                || usuarioBeneficiario == null || finalidade == null || finalidade.isBlank()
                || justificativa == null || justificativa.isBlank()
                || autorizacaoAdministrativa == null || autorizacaoAdministrativa.isBlank()) {
            throw new IllegalStateException("Permissão de documento restrito incompleta.");
        }
        if (concedidoEm == null) concedidoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (ativo == null) ativo = true;
        finalidade = finalidade.trim().replaceAll("\\s+", " ");
        justificativa = justificativa.trim().replaceAll("\\s+", " ");
        autorizacaoAdministrativa = autorizacaoAdministrativa.trim();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DocumentoComprobatorio getDocumentoReferencia() { return documentoReferencia; }
    public void setDocumentoReferencia(DocumentoComprobatorio documentoReferencia) { this.documentoReferencia = documentoReferencia; }
    public String getGrupoLogicoId() { return grupoLogicoId; }
    public void setGrupoLogicoId(String grupoLogicoId) { this.grupoLogicoId = grupoLogicoId; }
    public UsuarioAplicacao getUsuarioBeneficiario() { return usuarioBeneficiario; }
    public void setUsuarioBeneficiario(UsuarioAplicacao usuarioBeneficiario) { this.usuarioBeneficiario = usuarioBeneficiario; }
    public String getFinalidade() { return finalidade; }
    public void setFinalidade(String finalidade) { this.finalidade = finalidade; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getAutorizacaoAdministrativa() { return autorizacaoAdministrativa; }
    public void setAutorizacaoAdministrativa(String autorizacaoAdministrativa) { this.autorizacaoAdministrativa = autorizacaoAdministrativa; }
    public Instant getConcedidoEm() { return concedidoEm; }
    public Long getConcedidoPorId() { return concedidoPorId; }
    public void setConcedidoPorId(Long concedidoPorId) { this.concedidoPorId = concedidoPorId; }
    public String getConcedidoPorUsername() { return concedidoPorUsername; }
    public void setConcedidoPorUsername(String concedidoPorUsername) { this.concedidoPorUsername = concedidoPorUsername; }
    public Instant getValidoAte() { return validoAte; }
    public void setValidoAte(Instant validoAte) { this.validoAte = validoAte; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(Instant revogadoEm) { this.revogadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(revogadoEm); }
    public Long getRevogadoPorId() { return revogadoPorId; }
    public void setRevogadoPorId(Long revogadoPorId) { this.revogadoPorId = revogadoPorId; }
    public String getRevogadoPorUsername() { return revogadoPorUsername; }
    public void setRevogadoPorUsername(String revogadoPorUsername) { this.revogadoPorUsername = revogadoPorUsername; }
    public String getMotivoRevogacao() { return motivoRevogacao; }
    public void setMotivoRevogacao(String motivoRevogacao) { this.motivoRevogacao = motivoRevogacao; }
    public long getVersion() { return version; }
}

