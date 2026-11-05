package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;
import java.time.LocalDate;

@Entity
@Table(name = "frequencia_excecao", indexes = {
        @Index(name = "idx_freq_excecao_version", columnList = "version"),
        @Index(name = "idx_excecao_empregado_periodo_tipo", columnList = "id_empregado,data_inicio,data_fim,tipo"),
        @Index(name = "idx_excecao_emp_periodo_tipo_status", columnList = "id_empregado,data_inicio,data_fim,tipo,status")
})
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FrequenciaExcecao {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_frequencia_excecao")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = ConstantesDominio.STATUS_APROVADO;

    @Column(name = "observacao", length = 255)
    private String observacao;

    @Column(name = "empregado_nome_fotografia", length = 255)
    private String empregadoNomeFotografia;

    @Version
    @Column(name = "version")
    private long version = 0L;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getEmpregadoNomeFotografia() { return empregadoNomeFotografia; }
    public void setEmpregadoNomeFotografia(String nome) { this.empregadoNomeFotografia = nome; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    @PreRemove
    private void preservarFonteManual() {
        throw new IllegalStateException("Uma exceção publicada deve ser cancelada, preservando sua raiz histórica.");
    }
}
