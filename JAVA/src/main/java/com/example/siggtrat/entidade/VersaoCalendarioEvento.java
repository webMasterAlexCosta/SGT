package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.time.LocalDate;

/** Fato imutável; CalendarioEvento é apenas sua projeção vigente. */
@Entity
@Immutable
@Table(name = "calendario_evento_versao", uniqueConstraints =
        @UniqueConstraint(name = "uk_calendario_evento_versao", columnNames = {"id_calendario_evento", "numero_versao"}),
        indexes = @Index(name = "idx_calendario_versao_conhecimento", columnList = "id_calendario_evento,registrado_em"))
public class VersaoCalendarioEvento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calendario_evento_versao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_calendario_evento", nullable = false, updatable = false)
    private CalendarioEvento calendarioEvento;

    @Column(name = "numero_versao", nullable = false, updatable = false)
    private Integer numeroVersao;
    @Column(name = "tipo_operacao", length = 40, nullable = false, updatable = false)
    private String tipoOperacao;
    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm;
    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;
    @Column(name = "usuario_username", length = 120, updatable = false)
    private String usuarioUsername;
    @Column(name = "justificativa", length = 2000, updatable = false)
    private String justificativa;
    @Column(name = "motivo_tecnico", length = 500, nullable = false, updatable = false)
    private String motivoTecnico;
    @Column(name = "versao_anterior_id", updatable = false)
    private Long versaoAnteriorId;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) @Column(name = "fotografia_json", nullable = false, updatable = false, columnDefinition = "text")
    private String fotografiaJson;
    @Column(name = "hash_anterior", length = 64, updatable = false)
    private String hashAnterior;
    @Column(name = "hash_versao", length = 64, nullable = false, updatable = false)
    private String hashVersao;

    protected VersaoCalendarioEvento() { }

    public VersaoCalendarioEvento(CalendarioEvento calendarioEvento, int numeroVersao, String tipoOperacao,
            LocalDate dataEfetiva, Instant registradoEm, Long usuarioId, String usuarioUsername,
            String justificativa, String motivoTecnico, Long versaoAnteriorId, String fotografiaJson,
            String hashAnterior, String hashVersao) {
        this.calendarioEvento = calendarioEvento; this.numeroVersao = numeroVersao; this.tipoOperacao = tipoOperacao;
        this.dataEfetiva = dataEfetiva; this.registradoEm = registradoEm; this.usuarioId = usuarioId;
        this.usuarioUsername = usuarioUsername; this.justificativa = justificativa; this.motivoTecnico = motivoTecnico;
        this.versaoAnteriorId = versaoAnteriorId; this.fotografiaJson = fotografiaJson;
        this.hashAnterior = hashAnterior; this.hashVersao = hashVersao;
    }

    @PrePersist
    private void validar() {
        if (calendarioEvento == null || numeroVersao == null || numeroVersao < 1 || tipoOperacao == null
                || dataEfetiva == null || registradoEm == null || motivoTecnico == null
                || fotografiaJson == null || hashVersao == null || hashVersao.length() != 64)
            throw new IllegalStateException("Versão do calendário incompleta.");
    }
    @PreRemove
    private void preservar() { throw new IllegalStateException("Uma versão publicada do calendário não pode ser apagada."); }

    public Long getId() { return id; }
    public CalendarioEvento getCalendarioEvento() { return calendarioEvento; }
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
    public String getHashAnterior() { return hashAnterior; }
    public String getHashVersao() { return hashVersao; }
}
