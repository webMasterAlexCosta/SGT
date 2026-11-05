package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import jakarta.persistence.*;

import java.time.Instant;

/** Concessão administrativa temporária, pessoal e limitada a um escopo. */
@Entity
@Table(name = "permissao_excepcional", indexes = {
        @Index(name = "idx_permissao_excepcional_beneficiario", columnList = "id_usuario_beneficiario, tipo, ativo"),
        @Index(name = "idx_permissao_excepcional_contrato", columnList = "id_contrato, tipo, ativo"),
        @Index(name = "idx_permissao_excepcional_validade", columnList = "valido_ate, ativo")
})
public class PermissaoExcepcional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permissao_excepcional")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 56, updatable = false)
    private TipoPermissaoExcepcional tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_beneficiario", nullable = false, updatable = false)
    private UsuarioAplicacao usuarioBeneficiario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gestao_local", updatable = false)
    private GestaoLocal gestaoLocal;

    @Column(name = "finalidade", nullable = false, length = 500, updatable = false)
    private String finalidade;

    @Column(name = "justificativa", nullable = false, length = 1000, updatable = false)
    private String justificativa;

    @Column(name = "autorizacao_administrativa", nullable = false, length = 180, updatable = false)
    private String autorizacaoAdministrativa;

    @Column(name = "concedido_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant concedidoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "concedido_por_id", nullable = false, updatable = false)
    private Long concedidoPorId;

    @Column(name = "concedido_por_username", nullable = false, length = 120, updatable = false)
    private String concedidoPorUsername;

    @Column(name = "valido_ate", nullable = false, updatable = false)
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
        if (tipo == null || usuarioBeneficiario == null || vazio(finalidade) || vazio(justificativa)
                || vazio(autorizacaoAdministrativa) || concedidoPorId == null
                || vazio(concedidoPorUsername) || validoAte == null) {
            throw new IllegalStateException("Permissão excepcional incompleta.");
        }
        if (concedidoEm == null) concedidoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (!validoAte.isAfter(concedidoEm)) {
            throw new IllegalStateException("A validade da permissão deve ser posterior à concessão.");
        }
        finalidade = normalizar(finalidade);
        justificativa = normalizar(justificativa);
        autorizacaoAdministrativa = normalizar(autorizacaoAdministrativa);
        if (ativo == null) ativo = true;
    }

    private static boolean vazio(String valor) { return valor == null || valor.isBlank(); }
    private static String normalizar(String valor) { return valor.trim().replaceAll("\\s+", " "); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoPermissaoExcepcional getTipo() { return tipo; }
    public void setTipo(TipoPermissaoExcepcional tipo) { this.tipo = tipo; }
    public UsuarioAplicacao getUsuarioBeneficiario() { return usuarioBeneficiario; }
    public void setUsuarioBeneficiario(UsuarioAplicacao usuarioBeneficiario) { this.usuarioBeneficiario = usuarioBeneficiario; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
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
