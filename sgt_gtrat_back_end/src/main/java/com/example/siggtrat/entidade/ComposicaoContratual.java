package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "composicao_contratual",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_composicao_contrato_lotacao_jornada_cargo_inicio",
                columnNames = {"id_contrato", "id_lotacao", "id_jornada", "id_cargo", "data_inicio_vigencia"}
        ),
        indexes = {
                @Index(name = "idx_composicao_contrato", columnList = "id_contrato"),
                @Index(name = "idx_composicao_periodo", columnList = "id_contrato, data_inicio_vigencia, data_fim_vigencia"),
                @Index(name = "idx_composicao_recorte", columnList = "id_lotacao, id_jornada, id_cargo")
        }
)
public class ComposicaoContratual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_composicao_contratual")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false)
    private Lotacao lotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    private Jornada jornada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo", nullable = false)
    private Cargo cargo;

    @Column(name = "lotacao_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String lotacaoNomeFotografia;

    @Column(name = "jornada_descricao_fotografia", nullable = false, updatable = false, length = 150)
    private String jornadaDescricaoFotografia;

    @Column(name = "cargo_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String cargoNomeFotografia;

    @Column(name = "efetivo_previsto", nullable = false)
    private Integer efetivoPrevisto = 0;

    @Column(name = "data_inicio_vigencia", nullable = false)
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @Version
    @Column(name = "version")
    private long version;

    public ComposicaoContratual() {}

    @PrePersist
    @PreUpdate
    protected void validar() {
        if (contrato == null || lotacao == null || jornada == null || cargo == null) {
            throw new IllegalStateException("Contrato, Lotação, Turno e Cargo são obrigatórios na composição.");
        }
        if (efetivoPrevisto == null || efetivoPrevisto < 0) {
            throw new IllegalStateException("Efetivo previsto da composição não pode ser negativo.");
        }
        if (dataInicioVigencia == null) {
            throw new IllegalStateException("Data inicial da composição é obrigatória.");
        }
        if (dataFimVigencia != null && dataFimVigencia.isBefore(dataInicioVigencia)) {
            throw new IllegalStateException("Data final da composição não pode ser anterior à data inicial.");
        }
        if (lotacaoNomeFotografia == null) lotacaoNomeFotografia = lotacao.getNome();
        if (jornadaDescricaoFotografia == null) jornadaDescricaoFotografia = jornada.getDescricao();
        if (cargoNomeFotografia == null) cargoNomeFotografia = cargo.getNome();
        if (lotacaoNomeFotografia == null || jornadaDescricaoFotografia == null || cargoNomeFotografia == null) {
            throw new IllegalStateException("Fotografia dos catálogos da composição é obrigatória.");
        }
    }

    @Transient
    public boolean validaEm(LocalDate data) {
        if (data == null || dataInicioVigencia == null || data.isBefore(dataInicioVigencia)) return false;
        return dataFimVigencia == null || !data.isAfter(dataFimVigencia);
    }

    @Transient
    public boolean permiteNovaAlocacaoEm(LocalDate data) {
        return validaEm(data) && efetivoPrevisto != null && efetivoPrevisto > 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public String getLotacaoNomeFotografia() { return lotacaoNomeFotografia; }
    public void setLotacaoNomeFotografia(String valor) { this.lotacaoNomeFotografia = valor; }
    public String getJornadaDescricaoFotografia() { return jornadaDescricaoFotografia; }
    public void setJornadaDescricaoFotografia(String valor) { this.jornadaDescricaoFotografia = valor; }
    public String getCargoNomeFotografia() { return cargoNomeFotografia; }
    public void setCargoNomeFotografia(String valor) { this.cargoNomeFotografia = valor; }
    public Integer getEfetivoPrevisto() { return efetivoPrevisto; }
    public void setEfetivoPrevisto(Integer efetivoPrevisto) { this.efetivoPrevisto = efetivoPrevisto; }
    public LocalDate getDataInicioVigencia() { return dataInicioVigencia; }
    public void setDataInicioVigencia(LocalDate dataInicioVigencia) { this.dataInicioVigencia = dataInicioVigencia; }
    public LocalDate getDataFimVigencia() { return dataFimVigencia; }
    public void setDataFimVigencia(LocalDate dataFimVigencia) { this.dataFimVigencia = dataFimVigencia; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
