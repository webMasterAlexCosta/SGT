package com.example.siggtrat.entidade;
import jakarta.persistence.*;
import java.time.*;
/** Metadados imutáveis de incorporação; conhecer hoje não retroage o conhecimento. */
@MappedSuperclass
public abstract class ProvaHistoricaBase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contrato_id", nullable = false, updatable = false)
    private Contrato contrato;
    @Column(name = "data_inicio_efetiva", nullable = false, updatable = false)
    private LocalDate dataInicioEfetiva;
    @Column(name = "data_fim_efetiva", updatable = false)
    private LocalDate dataFimEfetiva;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "documento_id", updatable = false)
    private DocumentoComprobatorio documentoComprobatorio;
    @Column(updatable = false)
    private Integer versaoDocumento;
    @Column(length = 64, updatable = false)
    private String hashDocumento;
    @Column(length = 1000, updatable = false)
    private String referenciaSei;
    @Column(nullable = false, length = 1000, updatable = false)
    private String motivo;
    @Column(nullable = false, length = 1000, updatable = false)
    private String autorizacao;
    @Column(name = "responsavel_id", nullable = false, updatable = false)
    private Long responsavelId;
    @Column(nullable = false, length = 120, updatable = false)
    private String responsavelUsername;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm;
    @Column(name = "chave_idempotencia", nullable = false, length = 80, updatable = false)
    private String chaveIdempotencia;
    @Column(nullable = false, length = 64, updatable = false)
    private String hashIntencao;
    @Column(nullable = false, length = 64, updatable = false)
    private String hashConteudo;

    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato valor) { this.contrato = valor; }
    public LocalDate getDataInicioEfetiva() { return dataInicioEfetiva; }
    public void setDataInicioEfetiva(LocalDate valor) { this.dataInicioEfetiva = valor; }
    public LocalDate getDataFimEfetiva() { return dataFimEfetiva; }
    public void setDataFimEfetiva(LocalDate valor) { this.dataFimEfetiva = valor; }
    public DocumentoComprobatorio getDocumentoComprobatorio() { return documentoComprobatorio; }
    public void setDocumentoComprobatorio(DocumentoComprobatorio valor) { this.documentoComprobatorio = valor; }
    public Integer getVersaoDocumento() { return versaoDocumento; }
    public void setVersaoDocumento(Integer valor) { this.versaoDocumento = valor; }
    public String getHashDocumento() { return hashDocumento; }
    public void setHashDocumento(String valor) { this.hashDocumento = valor; }
    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String valor) { this.referenciaSei = valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo = valor; }
    public String getAutorizacao() { return autorizacao; }
    public void setAutorizacao(String valor) { this.autorizacao = valor; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long valor) { this.responsavelId = valor; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String valor) { this.responsavelUsername = valor; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant valor) {
        // O recibo e o hash devem usar a mesma precisão que o MySQL persiste.
        this.registradoEm = valor == null ? null : valor.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String valor) { this.chaveIdempotencia = valor; }
    public String getHashIntencao() { return hashIntencao; }
    public void setHashIntencao(String valor) { this.hashIntencao = valor; }
    public String getHashConteudo() { return hashConteudo; }
    public void setHashConteudo(String valor) { this.hashConteudo = valor; }
    @PrePersist protected void validarBase() {
        if (registradoEm == null) setRegistradoEm(com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros());
        if (contrato == null || dataInicioEfetiva == null || dataFimEfetiva != null && dataFimEfetiva.isBefore(dataInicioEfetiva)
                || responsavelId == null || hashIntencao == null || hashConteudo == null
                || (documentoComprobatorio == null && (referenciaSei == null || referenciaSei.isBlank())))
            throw new IllegalStateException("Prova histórica incompleta.");
    }
}
