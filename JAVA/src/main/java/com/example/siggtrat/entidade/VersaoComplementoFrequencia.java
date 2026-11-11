package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Immutable;
import java.time.Instant;
import java.time.LocalDate;

/** A raiz mantém a projeção; cada decisão manual publicada permanece imutável. */
@Entity
@Immutable
@Table(name = "frequencia_complemento_versao", uniqueConstraints = @UniqueConstraint(
        name = "uk_frequencia_complemento_versao", columnNames = {"tipo_complemento", "complemento_id", "numero_versao"}),
        indexes = @Index(name = "idx_complemento_versao_conhecimento",
                columnList = "tipo_complemento,complemento_id,registrado_em"))
public class VersaoComplementoFrequencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tipo_complemento", nullable = false, updatable = false, length = 20)
    private String tipoComplemento;
    @Column(name = "complemento_id", nullable = false, updatable = false)
    private Long complementoId;
    @Column(name = "numero_versao", nullable = false, updatable = false)
    private Integer numeroVersao;
    @Column(name = "tipo_operacao", nullable = false, updatable = false, length = 40)
    private String tipoOperacao;
    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm;
    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;
    @Column(name = "usuario_username", updatable = false, length = 120)
    private String usuarioUsername;
    @Column(name = "justificativa", updatable = false, length = 2000)
    private String justificativa;
    @Column(name = "motivo_tecnico", nullable = false, updatable = false, length = 500)
    private String motivoTecnico;
    @Column(name = "versao_anterior_id", updatable = false)
    private Long versaoAnteriorId;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "fotografia_json", nullable = false, updatable = false, columnDefinition = "text")
    private String fotografiaJson;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "contextos_json", nullable = false, updatable = false, columnDefinition = "text")
    private String contextosJson;
    @Column(name = "hash_anterior", updatable = false, length = 64)
    private String hashAnterior;
    @Column(name = "hash_versao", nullable = false, updatable = false, length = 64)
    private String hashVersao;

    protected VersaoComplementoFrequencia() { }

    public VersaoComplementoFrequencia(String tipoComplemento, Long complementoId, int numeroVersao,
            String tipoOperacao, LocalDate dataEfetiva, Instant registradoEm, Long usuarioId, String usuarioUsername,
            String justificativa, String motivoTecnico, Long versaoAnteriorId, String fotografiaJson,
            String contextosJson, String hashAnterior, String hashVersao) {
        this.tipoComplemento = tipoComplemento; this.complementoId = complementoId; this.numeroVersao = numeroVersao;
        this.tipoOperacao = tipoOperacao; this.dataEfetiva = dataEfetiva; this.registradoEm = registradoEm;
        this.usuarioId = usuarioId; this.usuarioUsername = usuarioUsername; this.justificativa = justificativa;
        this.motivoTecnico = motivoTecnico; this.versaoAnteriorId = versaoAnteriorId; this.fotografiaJson = fotografiaJson;
        this.contextosJson = contextosJson; this.hashAnterior = hashAnterior; this.hashVersao = hashVersao;
    }

    @PrePersist
    private void validar() {
        if (!("EXCECAO".equals(tipoComplemento) || "ADICIONAL".equals(tipoComplemento)) || complementoId == null
                || numeroVersao == null || numeroVersao < 1 || tipoOperacao == null || dataEfetiva == null
                || registradoEm == null || motivoTecnico == null || fotografiaJson == null || contextosJson == null
                || hashVersao == null || hashVersao.length() != 64)
            throw new IllegalStateException("Versão de complemento de frequência incompleta.");
    }
    @PreRemove
    private void preservar() { throw new IllegalStateException("Versão publicada de complemento não pode ser apagada."); }

    public Long getId() { return id; }
    public String getTipoComplemento() { return tipoComplemento; }
    public Long getComplementoId() { return complementoId; }
    public Integer getNumeroVersao() { return numeroVersao; }
    public String getTipoOperacao() { return tipoOperacao; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioUsername() { return usuarioUsername; }
    public String getJustificativa() { return justificativa; }
    public String getMotivoTecnico() { return motivoTecnico; }
    public Long getVersaoAnteriorId() { return versaoAnteriorId; }
    public String getFotografiaJson() { return fotografiaJson; }
    public String getContextosJson() { return contextosJson; }
    public String getHashAnterior() { return hashAnterior; }
    public String getHashVersao() { return hashVersao; }
}
