package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;

import java.math.BigDecimal;

@Entity
@Table(name = "regra_adicional", indexes = { @Index(name = "idx_regra_adicional_version", columnList = "version") })
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RegraAdicional {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regra_adicional")
    private Long id;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "inicio", length = 5)
    private String inicio;

    @Column(name = "fim", length = 5)
    private String fim;

    @Column(name = "percentual", precision = 6, scale = 2)
    private BigDecimal percentual;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "ordem")
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    @JsonIgnore
    private Jornada jornada;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }

    public String getFim() { return fim; }
    public void setFim(String fim) { this.fim = fim; }

    public BigDecimal getPercentual() { return percentual; }
    public void setPercentual(BigDecimal percentual) { this.percentual = percentual; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}