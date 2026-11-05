package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.vinculo.EstadoNegocialVinculo;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
import com.example.siggtrat.dominio.vinculo.SituacaoTemporalVinculo;
import com.example.siggtrat.utilitario.RelogioSistema;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "vinculo_contratual", indexes = {
        @Index(name = "idx_vinculo_pessoa_periodo", columnList = "id_pessoa, data_inicio, data_fim"),
        @Index(name = "idx_vinculo_contrato_periodo", columnList = "id_contrato, data_inicio, data_fim"),
        @Index(name = "idx_vinculo_estado", columnList = "estado_negocial"),
        @Index(name = "idx_vinculo_matricula", columnList = "id_contrato, matricula")
})
public class VinculoContratual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vinculo_contratual")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pessoa", nullable = false, updatable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    /** Último dia em que o vínculo existe. Nulo significa aberto. */
    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "matricula", length = 30)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocial", nullable = false, length = 24)
    private EstadoNegocialVinculo estadoNegocial = EstadoNegocialVinculo.REGISTRADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_motivo_desligamento")
    private MotivoDesligamento motivoDesligamento;

    /** Projeção da causa aplicada, sem depender de renomeações do catálogo mestre. */
    @Column(name = "motivo_desligamento_descricao_foto", length = 160)
    private String motivoDesligamentoDescricaoFotografia;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_desligamento_categoria_foto", length = 48)
    private CategoriaTerminoVinculo motivoDesligamentoCategoriaFotografia;

    @Column(name = "desligamento_registrado_em")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant desligamentoRegistradoEm;

    @Column(name = "desligamento_registrado_por_id")
    private Long desligamentoRegistradoPorId;

    @Column(name = "desligamento_registrado_por_username", length = 120)
    private String desligamentoRegistradoPorUsername;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;

    @Column(name = "criado_por_username", length = 120, updatable = false)
    private String criadoPorUsername;

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        matricula = normalizarMatricula(matricula);
        if (pessoa == null || contrato == null || dataInicio == null) {
            throw new IllegalStateException("Pessoa, contrato e início efetivo são obrigatórios.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalStateException("O fim do vínculo não pode ser anterior ao início.");
        }
        if (estadoNegocial == null) estadoNegocial = EstadoNegocialVinculo.REGISTRADO;
        if (criadoEm == null) criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    }

    public SituacaoTemporalVinculo situacaoEm(LocalDate data) {
        LocalDate referencia = data == null ? RelogioSistema.hoje() : data;
        if (estadoNegocial == EstadoNegocialVinculo.CANCELADO) return SituacaoTemporalVinculo.CANCELADO;
        if (estadoNegocial == EstadoNegocialVinculo.ANULADO) return SituacaoTemporalVinculo.ANULADO;
        if (referencia.isBefore(dataInicio)) return SituacaoTemporalVinculo.PROGRAMADO;
        if (dataFim != null && referencia.isAfter(dataFim)) return SituacaoTemporalVinculo.ENCERRADO;
        return SituacaoTemporalVinculo.VIGENTE;
    }

    public static String normalizarMatricula(String valor) {
        if (valor == null) return null;
        String n = valor.trim().replaceAll("\\s+", " ").toUpperCase();
        return n.isBlank() ? null : n;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pessoa getPessoa() { return pessoa; }
    public void setPessoa(Pessoa pessoa) { this.pessoa = pessoa; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public EstadoNegocialVinculo getEstadoNegocial() { return estadoNegocial; }
    public void setEstadoNegocial(EstadoNegocialVinculo estadoNegocial) { this.estadoNegocial = estadoNegocial; }
    public MotivoDesligamento getMotivoDesligamento() { return motivoDesligamento; }
    public void setMotivoDesligamento(MotivoDesligamento motivoDesligamento) { this.motivoDesligamento = motivoDesligamento; }
    public String getMotivoDesligamentoDescricaoFotografia() { return motivoDesligamentoDescricaoFotografia; }
    public void setMotivoDesligamentoDescricaoFotografia(String valor) { motivoDesligamentoDescricaoFotografia = valor; }
    public CategoriaTerminoVinculo getMotivoDesligamentoCategoriaFotografia() { return motivoDesligamentoCategoriaFotografia; }
    public void setMotivoDesligamentoCategoriaFotografia(CategoriaTerminoVinculo valor) { motivoDesligamentoCategoriaFotografia = valor; }
    public Instant getDesligamentoRegistradoEm() { return desligamentoRegistradoEm; }
    public void setDesligamentoRegistradoEm(Instant valor) { this.desligamentoRegistradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(valor); }
    public Long getDesligamentoRegistradoPorId() { return desligamentoRegistradoPorId; }
    public void setDesligamentoRegistradoPorId(Long valor) { this.desligamentoRegistradoPorId = valor; }
    public String getDesligamentoRegistradoPorUsername() { return desligamentoRegistradoPorUsername; }
    public void setDesligamentoRegistradoPorUsername(String valor) { this.desligamentoRegistradoPorUsername = valor; }
    public Instant getCriadoEm() { return criadoEm; }
    public Long getCriadoPorId() { return criadoPorId; }
    public void setCriadoPorId(Long criadoPorId) { this.criadoPorId = criadoPorId; }
    public String getCriadoPorUsername() { return criadoPorUsername; }
    public void setCriadoPorUsername(String criadoPorUsername) { this.criadoPorUsername = criadoPorUsername; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
