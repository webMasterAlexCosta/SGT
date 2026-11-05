package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "evento_disponibilidade_catalogo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evento_catalogo_origem", columnNames = "chave_origem"),
        @UniqueConstraint(name = "uk_evento_catalogo_intencao", columnNames = {"tipo_catalogo", "catalogo_id", "chave_idempotencia"})
}, indexes = {
        @Index(name = "idx_evento_catalogo_data", columnList = "tipo_catalogo,catalogo_id,data_efetiva,registrado_em")
})
public class EventoDisponibilidadeCatalogo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_catalogo", nullable = false, length = 40, updatable = false)
    private TipoCatalogoTemporal tipoCatalogo;
    @Column(name = "catalogo_id", nullable = false, updatable = false) private Long catalogoId;
    @Column(name = "data_efetiva", nullable = false, updatable = false) private LocalDate dataEfetiva;
    @Column(nullable = false, updatable = false) private Boolean ativo;
    /** Única origem criada pelo fluxo canônico; null em transições e fontes legadas. */
    @Column(name = "chave_origem", length = 80, updatable = false) private String chaveOrigem;
    /** Estado conhecido antes da transição; null quando a evidência não foi registrada. */
    @Column(name = "ativo_anterior", updatable = false) private Boolean ativoAnterior;
    @Column(nullable = false, length = 500, updatable = false) private String motivo;
    @Column(length = 1000, updatable = false) private String evidencia;
    @Column(length = 1000, updatable = false) private String autorizacao;
    @Column(name = "chave_idempotencia", length = 80, updatable = false) private String chaveIdempotencia;
    @Column(name = "hash_intencao", length = 64, updatable = false) private String hashIntencao;
    @Column(name = "ativo_atual_apos_registro", updatable = false) private Boolean ativoAtualAposRegistro;
    @Column(name = "versao_catalogo_apos", updatable = false) private Long versaoCatalogoApos;
    @Column(name = "responsavel_id", updatable = false) private Long responsavelId;
    @Column(name = "responsavel_username", length = 120, updatable = false) private String responsavelUsername;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoCatalogoTemporal getTipoCatalogo() { return tipoCatalogo; }
    public void setTipoCatalogo(TipoCatalogoTemporal tipoCatalogo) { this.tipoCatalogo = tipoCatalogo; }
    public Long getCatalogoId() { return catalogoId; }
    public void setCatalogoId(Long catalogoId) { this.catalogoId = catalogoId; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public Boolean getAtivo() { return ativo; }
    public String getChaveOrigem() { return chaveOrigem; }
    public void setChaveOrigem(String chaveOrigem) { this.chaveOrigem = chaveOrigem; }
    public Boolean getAtivoAnterior() { return ativoAnterior; }
    public void setAtivoAnterior(Boolean valor) { this.ativoAnterior = valor; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String valor) { this.evidencia = valor; }
    public String getAutorizacao() { return autorizacao; }
    public void setAutorizacao(String valor) { this.autorizacao = valor; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String valor) { this.chaveIdempotencia = valor; }
    public String getHashIntencao() { return hashIntencao; }
    public void setHashIntencao(String valor) { this.hashIntencao = valor; }
    public Boolean getAtivoAtualAposRegistro() { return ativoAtualAposRegistro; }
    public void setAtivoAtualAposRegistro(Boolean valor) { this.ativoAtualAposRegistro = valor; }
    public Long getVersaoCatalogoApos() { return versaoCatalogoApos; }
    public void setVersaoCatalogoApos(Long valor) { this.versaoCatalogoApos = valor; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) {
        this.registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(registradoEm);
    }
}
