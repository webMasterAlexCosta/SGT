package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.pessoa.TipoCorrecaoPessoa;
import jakarta.persistence.*;

import java.time.Instant;

/** Registro imutável de correção excepcional de identidade. */
@Entity
@Table(name = "pessoa_correcao", indexes = {
        @Index(name = "idx_pessoa_correcao_pessoa_data", columnList = "id_pessoa, registrado_em")
})
public class CorrecaoPessoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correcao_pessoa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pessoa", nullable = false, updatable = false)
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_correcao", nullable = false, length = 20, updatable = false)
    private TipoCorrecaoPessoa tipoCorrecao;

    @Column(name = "valor_anterior", nullable = false, length = 220, updatable = false)
    private String valorAnterior;

    @Column(name = "valor_novo", nullable = false, length = 220, updatable = false)
    private String valorNovo;

    @Column(name = "motivo", nullable = false, length = 1000, updatable = false)
    private String motivo;

    @Column(name = "evidencia", nullable = false, length = 500, updatable = false)
    private String evidencia;

    @Column(name = "autorizacao", nullable = false, length = 500, updatable = false)
    private String autorizacao;

    @Column(name = "impactos_encontrados", length = 2000, updatable = false)
    private String impactosEncontrados;

    @Column(name = "responsavel_id", updatable = false)
    private Long responsavelId;

    @Column(name = "responsavel_username", length = 120, updatable = false)
    private String responsavelUsername;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm = Instant.now();

    @PrePersist
    protected void validar() {
        if (pessoa == null || tipoCorrecao == null) throw new IllegalStateException("Pessoa e tipo da correção são obrigatórios.");
        valorAnterior = obrigatorio(valorAnterior, "Valor anterior é obrigatório.");
        valorNovo = obrigatorio(valorNovo, "Valor novo é obrigatório.");
        motivo = obrigatorio(motivo, "Motivo da correção é obrigatório.");
        evidencia = obrigatorio(evidencia, "Evidência da correção é obrigatória.");
        autorizacao = obrigatorio(autorizacao, "Autorização da correção é obrigatória.");
        impactosEncontrados = opcional(impactosEncontrados);
        if (registradoEm == null) registradoEm = Instant.now();
    }

    private static String obrigatorio(String valor, String mensagem) {
        String n = opcional(valor);
        if (n == null) throw new IllegalStateException(mensagem);
        return n;
    }
    private static String opcional(String valor) {
        if (valor == null) return null;
        String n = valor.trim().replaceAll("\\s+", " ");
        return n.isBlank() ? null : n;
    }

    public Long getId() { return id; }
    public Pessoa getPessoa() { return pessoa; }
    public void setPessoa(Pessoa pessoa) { this.pessoa = pessoa; }
    public TipoCorrecaoPessoa getTipoCorrecao() { return tipoCorrecao; }
    public void setTipoCorrecao(TipoCorrecaoPessoa tipoCorrecao) { this.tipoCorrecao = tipoCorrecao; }
    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public void setValorNovo(String valorNovo) { this.valorNovo = valorNovo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getAutorizacao() { return autorizacao; }
    public void setAutorizacao(String autorizacao) { this.autorizacao = autorizacao; }
    public String getImpactosEncontrados() { return impactosEncontrados; }
    public void setImpactosEncontrados(String impactosEncontrados) { this.impactosEncontrados = impactosEncontrados; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public Instant getRegistradoEm() { return registradoEm; }
}
