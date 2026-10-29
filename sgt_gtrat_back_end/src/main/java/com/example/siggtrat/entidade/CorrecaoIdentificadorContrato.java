package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Registro imutável da correção controlada do número/ano de um contrato publicado. */
@Entity
@Table(name = "contrato_correcao_identificador", uniqueConstraints = {
        @UniqueConstraint(name = "uk_correcao_identificador_idempotencia", columnNames = "chave_idempotencia")
}, indexes = {
        @Index(name = "idx_correcao_identificador_contrato", columnList = "id_contrato, registrado_em"),
        @Index(name = "idx_correcao_identificador_anterior", columnList = "numero_anterior, ano_anterior"),
        @Index(name = "idx_correcao_identificador_novo", columnList = "numero_novo, ano_novo")
})
public class CorrecaoIdentificadorContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_correcao_identificador")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento_ciclo_vida", nullable = false, updatable = false)
    private EventoCicloVidaContrato evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_permissao_excepcional", nullable = false, updatable = false)
    private PermissaoExcepcional permissaoUtilizada;

    @Column(name = "numero_anterior", nullable = false, length = 50, updatable = false)
    private String numeroAnterior;

    @Column(name = "ano_anterior", nullable = false, updatable = false)
    private Integer anoAnterior;

    @Column(name = "numero_novo", nullable = false, length = 50, updatable = false)
    private String numeroNovo;

    @Column(name = "ano_novo", nullable = false, updatable = false)
    private Integer anoNovo;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "data_ato", nullable = false, updatable = false)
    private LocalDate dataAto;

    @Column(name = "motivo", nullable = false, length = 1000, updatable = false)
    private String motivo;

    @Column(name = "referencia_sei", nullable = false, length = 120, updatable = false)
    private String referenciaSei;

    @Column(name = "autorizacao_administrativa", nullable = false, length = 180, updatable = false)
    private String autorizacaoAdministrativa;

    @Column(name = "chave_idempotencia", nullable = false, length = 80, updatable = false)
    private String chaveIdempotencia;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm = Instant.now();

    @Column(name = "responsavel_id", nullable = false, updatable = false)
    private Long responsavelId;

    @Column(name = "responsavel_username", nullable = false, length = 120, updatable = false)
    private String responsavelUsername;

    @PrePersist
    protected void validar() {
        if (contrato == null || evento == null || permissaoUtilizada == null || vazio(numeroAnterior)
                || anoAnterior == null || vazio(numeroNovo) || anoNovo == null || dataEfetiva == null
                || dataAto == null || vazio(motivo) || vazio(referenciaSei)
                || vazio(autorizacaoAdministrativa) || vazio(chaveIdempotencia)
                || responsavelId == null || vazio(responsavelUsername)) {
            throw new IllegalStateException("Correção de identificador incompleta.");
        }
        if (registradoEm == null) registradoEm = Instant.now();
        numeroAnterior = numeroAnterior.trim();
        numeroNovo = numeroNovo.trim();
        motivo = normalizar(motivo);
        referenciaSei = referenciaSei.trim();
        autorizacaoAdministrativa = normalizar(autorizacaoAdministrativa);
        chaveIdempotencia = chaveIdempotencia.trim();
    }

    private static boolean vazio(String valor) { return valor == null || valor.isBlank(); }
    private static String normalizar(String valor) { return valor.trim().replaceAll("\\s+", " "); }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public EventoCicloVidaContrato getEvento() { return evento; }
    public void setEvento(EventoCicloVidaContrato evento) { this.evento = evento; }
    public PermissaoExcepcional getPermissaoUtilizada() { return permissaoUtilizada; }
    public void setPermissaoUtilizada(PermissaoExcepcional permissaoUtilizada) { this.permissaoUtilizada = permissaoUtilizada; }
    public String getNumeroAnterior() { return numeroAnterior; }
    public void setNumeroAnterior(String numeroAnterior) { this.numeroAnterior = numeroAnterior; }
    public Integer getAnoAnterior() { return anoAnterior; }
    public void setAnoAnterior(Integer anoAnterior) { this.anoAnterior = anoAnterior; }
    public String getNumeroNovo() { return numeroNovo; }
    public void setNumeroNovo(String numeroNovo) { this.numeroNovo = numeroNovo; }
    public Integer getAnoNovo() { return anoNovo; }
    public void setAnoNovo(Integer anoNovo) { this.anoNovo = anoNovo; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public LocalDate getDataAto() { return dataAto; }
    public void setDataAto(LocalDate dataAto) { this.dataAto = dataAto; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String referenciaSei) { this.referenciaSei = referenciaSei; }
    public String getAutorizacaoAdministrativa() { return autorizacaoAdministrativa; }
    public void setAutorizacaoAdministrativa(String autorizacaoAdministrativa) { this.autorizacaoAdministrativa = autorizacaoAdministrativa; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
}
