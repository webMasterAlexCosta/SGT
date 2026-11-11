package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "contrato_evento_ciclo_vida",
        indexes = {
                @Index(name = "idx_contrato_evento_contrato_data", columnList = "id_contrato, data_efetiva"),
                @Index(name = "idx_contrato_evento_tipo", columnList = "tipo_evento"),
                @Index(name = "idx_contrato_evento_registrado", columnList = "registrado_em"),
                @Index(name = "idx_contrato_evento_chave_data", columnList = "id_contrato, chave_negocial, data_efetiva"),
                @Index(name = "idx_contrato_evento_fato_logico", columnList = "fato_logico_id, versao_negocial")
        }
)
public class EventoCicloVidaContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_ciclo_vida")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 48, updatable = false)
    private TipoEventoCicloVidaContrato tipoEvento;

    /**
     * Identidade estável da cadeia de versões de um mesmo fato. Não depende do
     * ID técnico do banco e é preservada em retificações/substituições.
     */
    @Column(name = "fato_logico_id", length = 48, nullable = false, updatable = false)
    private String fatoLogicoId;

    /** Chave semântica usada para detectar colisões reais na mesma data. */
    @Column(name = "chave_negocial", length = 96, updatable = false)
    private String chaveNegocial = "CICLO_VIDA";

    @Column(name = "versao_negocial", updatable = false)
    private Integer versaoNegocial = 1;

    /** Ordem formal do instrumento, quando o documento possuir sequência própria. */
    @Column(name = "ordem_documental", updatable = false)
    private Integer ordemDocumental;

    /**
     * Conveniência da projeção atual. Consultas bitemporais reconstroem validade
     * pelas relações conhecidas no instante consultado e não confiam apenas aqui.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_fato", length = 24)
    private SituacaoFatoNegocial situacaoFato = SituacaoFatoNegocial.VIGENTE;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    /**
     * Data do ato/documento quando distinta da data efetiva.
     * É especialmente relevante para anulação.
     */
    @Column(name = "data_ato", updatable = false)
    private LocalDate dataAto;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "usuario_responsavel_id", updatable = false)
    private Long usuarioResponsavelId;

    @Column(name = "usuario_responsavel_username", length = 120, updatable = false)
    private String usuarioResponsavelUsername;

    @Column(name = "motivo", length = 1000, updatable = false)
    private String motivo;

    @Column(name = "referencia_sei", length = 120, updatable = false)
    private String referenciaSei;

    @Column(name = "valor_anterior", length = 255, updatable = false)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 255, updatable = false)
    private String valorNovo;

    @Embedded
    private ConferenciaDocumentalTransferencia conferenciaDocumental;

    public ConferenciaDocumentalTransferencia getConferenciaDocumental() { return conferenciaDocumental; }
    public void registrarConferenciaDocumental(ConferenciaDocumentalTransferencia conferencia) {
        if (id != null || conferenciaDocumental != null)
            throw new IllegalStateException("A conferência deve integrar o primeiro registro imutável do evento.");
        conferenciaDocumental = java.util.Objects.requireNonNull(conferencia);
    }

    public EventoCicloVidaContrato() {}

    @PrePersist
    protected void normalizar() {
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (fatoLogicoId == null || fatoLogicoId.isBlank()) fatoLogicoId = UUID.randomUUID().toString();
        if (chaveNegocial == null || chaveNegocial.isBlank()) chaveNegocial = "CICLO_VIDA";
        chaveNegocial = chaveNegocial.trim().toUpperCase();
        if (versaoNegocial == null || versaoNegocial < 1) versaoNegocial = 1;
        if (situacaoFato == null) situacaoFato = SituacaoFatoNegocial.VIGENTE;
        if (motivo != null) motivo = motivo.trim().replaceAll("\\s+", " ");
        if (referenciaSei != null) referenciaSei = referenciaSei.trim();
        if (tipoEvento == null) throw new IllegalStateException("Tipo do evento contratual é obrigatório.");
        if (dataEfetiva == null) throw new IllegalStateException("Data efetiva do evento contratual é obrigatória.");
    }

    public Long getId() { return id; }

    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }

    public TipoEventoCicloVidaContrato getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoCicloVidaContrato tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getFatoLogicoId() { return fatoLogicoId; }
    public void setFatoLogicoId(String fatoLogicoId) { this.fatoLogicoId = fatoLogicoId; }

    public String getChaveNegocial() {
        if (chaveNegocial != null && !chaveNegocial.isBlank()) return chaveNegocial;
        if (tipoEvento == null) return "CICLO_VIDA";
        return switch (tipoEvento) {
            case INATIVACAO_ADMINISTRATIVA, REATIVACAO_ADMINISTRATIVA -> "HABILITACAO_ADMINISTRATIVA";
            case ENCERRAMENTO_ANTECIPADO -> "VIGENCIA";
            case CORRECAO_IDENTIFICADOR -> "IDENTIFICACAO";
            case CORRECAO_CNPJ -> "CNPJ_EMPRESA";
            case TRANSFERENCIA_GESTAO_LOCAL -> "GESTAO_LOCAL";
            case CANCELAMENTO_TRANSFERENCIA_GESTAO -> "CANCELAMENTO_TRANSFERENCIA_GESTAO";
            case ADITAMENTO -> "ADITAMENTO";
            case CANCELAMENTO_ADITAMENTO -> "CANCELAMENTO_ADITAMENTO";
            case REMANEJAMENTO -> "REMANEJAMENTO";
            case CANCELAMENTO_REMANEJAMENTO -> "CANCELAMENTO_REMANEJAMENTO";
            case PUBLICACAO, CANCELAMENTO_CONTRATO_FUTURO, ANULACAO, CORRECAO_ANULACAO -> "ESTADO_NEGOCIAL";
        };
    }
    public void setChaveNegocial(String chaveNegocial) { this.chaveNegocial = chaveNegocial; }

    public Integer getVersaoNegocial() { return versaoNegocial == null ? 1 : versaoNegocial; }
    public void setVersaoNegocial(Integer versaoNegocial) { this.versaoNegocial = versaoNegocial; }

    public Integer getOrdemDocumental() { return ordemDocumental; }
    public void setOrdemDocumental(Integer ordemDocumental) { this.ordemDocumental = ordemDocumental; }

    public SituacaoFatoNegocial getSituacaoFato() { return situacaoFato == null ? SituacaoFatoNegocial.VIGENTE : situacaoFato; }
    public void setSituacaoFato(SituacaoFatoNegocial situacaoFato) { this.situacaoFato = situacaoFato; }

    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }

    public LocalDate getDataAto() { return dataAto; }
    public void setDataAto(LocalDate dataAto) { this.dataAto = dataAto; }

    public Instant getRegistradoEm() { return registradoEm; }

    public Long getUsuarioResponsavelId() { return usuarioResponsavelId; }
    public void setUsuarioResponsavelId(Long usuarioResponsavelId) { this.usuarioResponsavelId = usuarioResponsavelId; }

    public String getUsuarioResponsavelUsername() { return usuarioResponsavelUsername; }
    public void setUsuarioResponsavelUsername(String usuarioResponsavelUsername) { this.usuarioResponsavelUsername = usuarioResponsavelUsername; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String referenciaSei) { this.referenciaSei = referenciaSei; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNovo() { return valorNovo; }
    public void setValorNovo(String valorNovo) { this.valorNovo = valorNovo; }
}
