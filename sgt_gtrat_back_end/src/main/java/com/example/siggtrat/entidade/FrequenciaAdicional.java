package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "frequencia_adicional",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_freq_adic_emp_data_tipo", columnNames = {"id_empregado", "data_referencia", "tipo"})
        },
        indexes = {
                @Index(name = "idx_freq_adicional_version", columnList = "version"),
                @Index(name = "idx_freq_adicional_data_tipo_status_emp", columnList = "data_referencia,tipo,status,id_empregado"),
                @Index(name = "idx_freq_adicional_emp_data_tipo_status", columnList = "id_empregado,data_referencia,tipo,status")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FrequenciaAdicional {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_frequencia_adicional")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "minutos")
    private Integer minutos;

    @Column(name = "quantidade", precision = 10, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "status", nullable = false, length = 20)
    private String status = ConstantesDominio.STATUS_CALCULADO;

    @Column(name = "justificativa", length = 255)
    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_frequencia")
    private Frequencia frequencia;

    @Version
    @Column(name = "version")
    private long version = 0L;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getMinutos() { return minutos; }
    public void setMinutos(Integer minutos) { this.minutos = minutos; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public Frequencia getFrequencia() { return frequencia; }
    public void setFrequencia(Frequencia frequencia) { this.frequencia = frequencia; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}