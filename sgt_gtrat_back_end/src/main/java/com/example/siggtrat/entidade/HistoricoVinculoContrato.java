package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "historico_vinculo_contrato",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hist_vinculo_ativo",
                        columnNames = {"id_empregado", "id_contrato", "vinculo_ativo"}
                ),
                @UniqueConstraint(
                        name = "uk_hist_um_vinculo_ativo_empregado",
                        columnNames = {"id_empregado", "vinculo_ativo"}
                )
        },
        indexes = {
                @Index(name = "idx_hist_vinculo_contrato_version", columnList = "version"),
                @Index(name = "idx_hist_vinculo_empregado_inicio",
                        columnList = "id_empregado, data_inicio"),
                @Index(name = "idx_hist_vinculo_empregado_periodo",
                        columnList = "id_empregado, data_inicio, data_fim"),
                @Index(name = "idx_hist_vinculo_empregado_contrato_fim",
                        columnList = "id_empregado, id_contrato, data_fim"),
                @Index(name = "idx_hist_vinculo_contrato", columnList = "id_contrato"),
                @Index(name = "idx_hist_vinculo_gestao_local_periodo",
                        columnList = "gestao_local_snapshot_id, data_inicio, data_fim"),
                @Index(name = "idx_hist_vinculo_se_periodo",
                        columnList = "superintendencia_snapshot_id, data_inicio, data_fim"),
                @Index(name = "idx_hist_relatorio_contrato_periodo_empregado",
                        columnList = "id_contrato, data_inicio, data_fim, id_empregado"),
                @Index(name = "idx_hist_relatorio_lotacao_periodo_empregado",
                        columnList = "id_lotacao, data_inicio, data_fim, id_empregado"),
                @Index(name = "idx_hist_relatorio_jornada_periodo_empregado",
                        columnList = "id_jornada, data_inicio, data_fim, id_empregado")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoVinculoContrato {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    /**
     * O histórico é a fonte canônica da alocação. Estes relacionamentos não
     * são apenas informativos: relatórios históricos deixam de consultar os
     * campos atuais do empregado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lotacao")
    private Lotacao lotacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jornada")
    private Jornada jornada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo")
    private Cargo cargo;

    @Column(name = "empregado_snapshot_nome", length = 200)
    private String empregadoSnapshotNome;

    @Column(name = "contrato_snapshot_numero", length = 100)
    private String contratoSnapshotNumero;

    @Column(name = "lotacao_snapshot_nome", length = 200)
    private String lotacaoSnapshotNome;

    @Column(name = "lotacao_snapshot_municipio", length = 150)
    private String lotacaoSnapshotMunicipio;

    @Column(name = "jornada_snapshot_descricao", length = 150)
    private String jornadaSnapshotDescricao;

    @Column(name = "jornada_snapshot_dias_trabalho", length = 200)
    private String jornadaSnapshotDiasTrabalho;

    @Column(name = "jornada_snapshot_minutos_dia", length = 300)
    private String jornadaSnapshotMinutosDia;

    @Column(name = "cargo_snapshot_nome", length = 150)
    private String cargoSnapshotNome;

    @Column(name = "gestao_local_snapshot_id", nullable = false)
    private Long gestaoLocalSnapshotId;

    @Column(name = "superintendencia_snapshot_id", nullable = false)
    private Long superintendenciaSnapshotId;

    @Column(name = "data_inicio", nullable = false)
    private ZonedDateTime dataInicio;

    @Column(name = "data_fim")
    private ZonedDateTime dataFim;

    @Column(name = "observacao")
    private String observacao;

    /**
     * Marcador técnico para garantir no MySQL apenas um vínculo ativo por
     * empregado. Registros encerrados usam NULL, permitindo vários históricos
     * antigos pela regra de unicidade do MySQL.
     */
    @Column(name = "vinculo_ativo")
    private Boolean vinculoAtivo;

    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void validarIntegridade() {
        this.vinculoAtivo = dataFim == null ? Boolean.TRUE : null;
        if (empregado == null || contrato == null) {
            throw new IllegalStateException("O histórico exige empregado e contrato.");
        }
        if (dataInicio == null) {
            throw new IllegalStateException("A data inicial do vínculo é obrigatória.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalStateException("A data final do vínculo não pode ser anterior à data inicial.");
        }
        if (empregadoSnapshotNome == null && empregado != null) empregadoSnapshotNome = empregado.getNome();
        if (contratoSnapshotNumero == null && contrato != null) contratoSnapshotNumero = contrato.getNumeroContrato();
        if (lotacaoSnapshotNome == null && lotacao != null) lotacaoSnapshotNome = lotacao.getNome();
        if (lotacaoSnapshotMunicipio == null && lotacao != null) lotacaoSnapshotMunicipio = lotacao.getMunicipio();
        if (jornadaSnapshotDescricao == null && jornada != null) jornadaSnapshotDescricao = jornada.getDescricao();
        if (cargoSnapshotNome == null && cargo != null) cargoSnapshotNome = cargo.getNome();
        if (contrato.getGestaoLocal() == null || contrato.getGestaoLocal().getId() == null
                || contrato.getGestaoLocal().getSuperintendencia() == null
                || contrato.getGestaoLocal().getSuperintendencia().getId() == null) {
            throw new IllegalStateException("O histórico contratual exige Gestão Local e Gestão Estadual.");
        }
        if (gestaoLocalSnapshotId == null) gestaoLocalSnapshotId = contrato.getGestaoLocal().getId();
        if (superintendenciaSnapshotId == null) {
            superintendenciaSnapshotId = contrato.getGestaoLocal().getSuperintendencia().getId();
        }
        if (empregado.getGestaoLocal() != null && contrato.getGestaoLocal() != null
                && empregado.getGestaoLocal().getId() != null && contrato.getGestaoLocal().getId() != null
                && !java.util.Objects.equals(empregado.getGestaoLocal().getId(), contrato.getGestaoLocal().getId())) {
            throw new IllegalStateException("Empregado e contrato do histórico devem pertencer à mesma Gestão Local.");
        }
    }

    public boolean estaAtivo() { return dataFim == null; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public String getEmpregadoSnapshotNome() { return empregadoSnapshotNome; }
    public void setEmpregadoSnapshotNome(String empregadoSnapshotNome) { this.empregadoSnapshotNome = empregadoSnapshotNome; }
    public String getContratoSnapshotNumero() { return contratoSnapshotNumero; }
    public void setContratoSnapshotNumero(String contratoSnapshotNumero) { this.contratoSnapshotNumero = contratoSnapshotNumero; }
    public String getLotacaoSnapshotNome() { return lotacaoSnapshotNome; }
    public void setLotacaoSnapshotNome(String lotacaoSnapshotNome) { this.lotacaoSnapshotNome = lotacaoSnapshotNome; }
    public String getLotacaoSnapshotMunicipio() { return lotacaoSnapshotMunicipio; }
    public void setLotacaoSnapshotMunicipio(String lotacaoSnapshotMunicipio) { this.lotacaoSnapshotMunicipio = lotacaoSnapshotMunicipio; }
    public String getJornadaSnapshotDescricao() { return jornadaSnapshotDescricao; }
    public void setJornadaSnapshotDescricao(String jornadaSnapshotDescricao) { this.jornadaSnapshotDescricao = jornadaSnapshotDescricao; }
    public String getJornadaSnapshotDiasTrabalho() { return jornadaSnapshotDiasTrabalho; }
    public void setJornadaSnapshotDiasTrabalho(String jornadaSnapshotDiasTrabalho) { this.jornadaSnapshotDiasTrabalho = jornadaSnapshotDiasTrabalho; }
    public String getJornadaSnapshotMinutosDia() { return jornadaSnapshotMinutosDia; }
    public void setJornadaSnapshotMinutosDia(String jornadaSnapshotMinutosDia) { this.jornadaSnapshotMinutosDia = jornadaSnapshotMinutosDia; }
    public String getCargoSnapshotNome() { return cargoSnapshotNome; }
    public void setCargoSnapshotNome(String cargoSnapshotNome) { this.cargoSnapshotNome = cargoSnapshotNome; }
    public Long getGestaoLocalSnapshotId() { return gestaoLocalSnapshotId; }
    public void setGestaoLocalSnapshotId(Long gestaoLocalSnapshotId) { this.gestaoLocalSnapshotId = gestaoLocalSnapshotId; }
    public Long getSuperintendenciaSnapshotId() { return superintendenciaSnapshotId; }
    public void setSuperintendenciaSnapshotId(Long superintendenciaSnapshotId) { this.superintendenciaSnapshotId = superintendenciaSnapshotId; }
    public ZonedDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(ZonedDateTime dataInicio) { this.dataInicio = dataInicio; }
    public ZonedDateTime getDataFim() { return dataFim; }
    public void setDataFim(ZonedDateTime dataFim) { this.dataFim = dataFim; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Boolean getVinculoAtivo() { return vinculoAtivo; }
    public void setVinculoAtivo(Boolean vinculoAtivo) { this.vinculoAtivo = vinculoAtivo; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
