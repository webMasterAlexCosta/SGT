package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;

import java.time.LocalDate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Evento de calendário aplicado por escopo organizacional. */
@Entity
@Table(
        name = "calendario_evento",
        indexes = {
                @Index(name = "idx_calendario_evento_version", columnList = "version"),
                @Index(name = "idx_calendario_evento_data", columnList = "data_evento"),
                @Index(name = "idx_calendario_evento_se", columnList = "id_superintendencia"),
                @Index(name = "idx_calendario_evento_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_calendario_evento_lotacao", columnList = "id_lotacao")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class CalendarioEvento {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_calendario_evento")
    private Long id;

    @Column(name = "data_evento", nullable = false)
    private LocalDate data;

    /** Ex.: FERIADO, PONTO_FACULTATIVO, DIA_DESCANSO, EVENTO_INTERNO. */
    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "descricao", length = 120)
    private String descricao;

    /** NACIONAL, ESTADUAL, MUNICIPAL ou LOTACAO. */
    @Column(name = "abrangencia", nullable = false, length = 30)
    private String abrangencia = "ESTADUAL";

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "municipio", length = 80)
    private String municipio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superintendencia")
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private SuperintendenciaEstadual superintendencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_gestao_local")
    @JsonIgnoreProperties(value = {"superintendencia", "hibernateLazyInitializer", "handler"})
    private GestaoLocal gestaoLocal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lotacao")
    @JsonIgnore
    private Lotacao lotacao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "cancelado", nullable = false)
    private boolean cancelado;

    // Nulo em dados legados: não se inventa o instante de publicação anterior.
    @Column(name = "criado_em", updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant criadoEm;

    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    private void registrarCriacao() {
        if (criadoEm == null) criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getAbrangencia() { return abrangencia; }
    public void setAbrangencia(String abrangencia) { this.abrangencia = abrangencia; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public SuperintendenciaEstadual getSuperintendencia() { return superintendencia; }
    public void setSuperintendencia(SuperintendenciaEstadual superintendencia) { this.superintendencia = superintendencia; }
    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }

    @Transient
    @JsonProperty("lotacaoId")
    public Long getLotacaoId() { return lotacao != null ? lotacao.getId() : null; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public boolean isCancelado() { return cancelado; }
    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }
    public Instant getCriadoEm() { return criadoEm; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
