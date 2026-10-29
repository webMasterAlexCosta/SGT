package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.alocacao.EstadoNegocialAlocacao;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Faixa temporal de Lotação + Turno + Cargo dentro de um Vínculo.
 *
 * A linha não é apagada quando sofre retificação ou cancelamento: seu estado e
 * os eventos associados preservam a trilha de auditoria.
 */
@Entity
@Table(name = "alocacao_vinculo", indexes = {
        @Index(name = "idx_alocacao_vinculo_periodo", columnList = "id_vinculo_contratual, data_inicio, data_fim"),
        @Index(name = "idx_alocacao_composicao", columnList = "id_composicao_contratual"),
        @Index(name = "idx_alocacao_estado", columnList = "estado_negocial")
})
public class AlocacaoVinculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alocacao_vinculo")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vinculo_contratual", nullable = false, updatable = false)
    private VinculoContratual vinculo;
    /** Pode ser nula quando a combinação nasceu em aditamento sem composição-base. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_composicao_contratual")
    private ComposicaoContratual composicao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false, updatable = false)
    private Lotacao lotacao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false, updatable = false)
    private Jornada jornada;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo", nullable = false, updatable = false)
    private Cargo cargo;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;
    @Column(name = "data_fim")
    private LocalDate dataFim;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    @Column(name = "criado_por_id", updatable = false)
    private Long criadoPorId;
    @Column(name = "criado_por_username", length = 120, updatable = false)
    private String criadoPorUsername;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocial", nullable = false, length = 24)
    private EstadoNegocialAlocacao estadoNegocial = EstadoNegocialAlocacao.REGISTRADA;
    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void validar() {
        if (vinculo == null || lotacao == null || jornada == null || cargo == null || dataInicio == null) {
            throw new IllegalStateException("Alocação deve conter vínculo, Lotação, Turno, Cargo e início.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) throw new IllegalStateException("Período da alocação é inválido.");
        if (criadoEm == null) criadoEm = Instant.now();
        if (estadoNegocial == null) estadoNegocial = EstadoNegocialAlocacao.REGISTRADA;
    }
    public Long getId() { return id; }
    public VinculoContratual getVinculo() { return vinculo; }
    public void setVinculo(VinculoContratual vinculo) { this.vinculo = vinculo; }
    public ComposicaoContratual getComposicao() { return composicao; }
    public void setComposicao(ComposicaoContratual composicao) { this.composicao = composicao; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public Instant getCriadoEm() { return criadoEm; }
    public Long getCriadoPorId() { return criadoPorId; }
    public void setCriadoPorId(Long valor) { this.criadoPorId = valor; }
    public String getCriadoPorUsername() { return criadoPorUsername; }
    public void setCriadoPorUsername(String valor) { this.criadoPorUsername = valor; }
    public EstadoNegocialAlocacao getEstadoNegocial() { return estadoNegocial; }
    public void setEstadoNegocial(EstadoNegocialAlocacao valor) { this.estadoNegocial = valor; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
