package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ocorrencia", indexes = {
        @Index(name = "idx_ocorrencia_version", columnList = "version"),
        @Index(name = "idx_ocorrencia_empregado_data", columnList = "id_empregado,data_ocorrencia")
})
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Ocorrencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_empregado", nullable = false)
    @NotNull(message = "empregado é obrigatório")
    private Empregado empregado;

    @Column(name = "data_ocorrencia", nullable = false)
    @NotNull(message = "dataOcorrencia é obrigatória")
    private LocalDate dataOcorrencia;

    @Column(nullable = false)
    @NotBlank(message = "motivo é obrigatório")
    private String motivo;

    @Column(name = "tipo_ro")
    private String tipoRo;

    @Column(name = "numero_sei")
    private String numeroSei;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    // Mantido para garantir Integridade dos dados (Security CID)
    @Column(name = "quantidade_dias")
    private Integer quantidadeDias;

    @Column(name = "dias_suspensao")
    private Integer diasSuspensao;

    @Column(name = "dias_afastamento")
    private Integer diasAfastamento;

    // --- MÚLTIPLOS ARQUIVOS ---
    @OneToMany(mappedBy = "ocorrencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<AnexoOcorrencia> anexos = new ArrayList<>();

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Ocorrencia() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }

    public LocalDate getDataOcorrencia() { return dataOcorrencia; }
    public void setDataOcorrencia(LocalDate dataOcorrencia) { this.dataOcorrencia = dataOcorrencia; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getTipoRo() { return tipoRo; }
    public void setTipoRo(String tipoRo) { this.tipoRo = tipoRo; }

    public String getNumeroSei() { return numeroSei; }
    public void setNumeroSei(String numeroSei) { this.numeroSei = numeroSei; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Integer getQuantidadeDias() { return quantidadeDias; }
    public void setQuantidadeDias(Integer quantidadeDias) { this.quantidadeDias = quantidadeDias; }

    public Integer getDiasSuspensao() { return diasSuspensao; }
    public void setDiasSuspensao(Integer diasSuspensao) { this.diasSuspensao = diasSuspensao; }

    public Integer getDiasAfastamento() { return diasAfastamento; }
    public void setDiasAfastamento(Integer diasAfastamento) { this.diasAfastamento = diasAfastamento; }

    public List<AnexoOcorrencia> getAnexos() { return anexos; }
    public void setAnexos(List<AnexoOcorrencia> anexos) { this.anexos = anexos; }
}