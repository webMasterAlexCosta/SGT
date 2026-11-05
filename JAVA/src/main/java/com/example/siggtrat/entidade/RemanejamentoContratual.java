package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "remanejamento_contratual",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_remanejamento_contrato_identificacao",
                columnNames = {"id_contrato", "identificacao", "versao_instrumento"}
        ),
        indexes = {
                @Index(name = "idx_remanejamento_contrato_data", columnList = "id_contrato, data_efetiva"),
                @Index(name = "idx_remanejamento_evento", columnList = "id_evento")
        }
)
public class RemanejamentoContratual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_remanejamento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento", nullable = false, unique = true, updatable = false)
    private EventoCicloVidaContrato evento;

    @Column(name = "identificacao", nullable = false, length = 120, updatable = false)
    private String identificacao;

    @Column(name = "versao_instrumento", nullable = false, updatable = false)
    private Integer versaoInstrumento = 1;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "justificativa", nullable = false, length = 1200, updatable = false)
    private String justificativa;

    @Column(name = "referencia_sei", length = 160, updatable = false)
    private String referenciaSei;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "usuario_responsavel_id", updatable = false)
    private Long usuarioResponsavelId;

    @Column(name = "usuario_responsavel_username", length = 120, updatable = false)
    private String usuarioResponsavelUsername;

    @OneToMany(mappedBy = "remanejamento", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("id ASC")
    private List<RemanejamentoComposicao> itens = new ArrayList<>();

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void validar() {
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (contrato == null || evento == null || identificacao == null || identificacao.isBlank()
                || dataEfetiva == null || justificativa == null || justificativa.isBlank()) {
            throw new IllegalStateException("Remanejamento contratual incompleto.");
        }
        identificacao = identificacao.trim();
        if (versaoInstrumento == null || versaoInstrumento < 1) versaoInstrumento = 1;
        justificativa = justificativa.trim().replaceAll("\\s+", " ");
        if (referenciaSei != null) referenciaSei = referenciaSei.trim();
    }

    public void adicionarItem(RemanejamentoComposicao item) {
        if (item == null) return;
        item.setRemanejamento(this);
        itens.add(item);
    }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public EventoCicloVidaContrato getEvento() { return evento; }
    public void setEvento(EventoCicloVidaContrato evento) { this.evento = evento; }
    public String getIdentificacao() { return identificacao; }
    public void setIdentificacao(String identificacao) { this.identificacao = identificacao; }
    public Integer getVersaoInstrumento() { return versaoInstrumento; }
    public void setVersaoInstrumento(Integer versaoInstrumento) { this.versaoInstrumento = versaoInstrumento; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String referenciaSei) { this.referenciaSei = referenciaSei; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getUsuarioResponsavelId() { return usuarioResponsavelId; }
    public void setUsuarioResponsavelId(Long usuarioResponsavelId) { this.usuarioResponsavelId = usuarioResponsavelId; }
    public String getUsuarioResponsavelUsername() { return usuarioResponsavelUsername; }
    public void setUsuarioResponsavelUsername(String usuarioResponsavelUsername) { this.usuarioResponsavelUsername = usuarioResponsavelUsername; }
    public List<RemanejamentoComposicao> getItens() { return itens; }
    public long getVersion() { return version; }
}
