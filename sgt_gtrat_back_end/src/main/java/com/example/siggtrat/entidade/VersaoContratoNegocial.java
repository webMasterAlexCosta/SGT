package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Fotografia imutável dos dados negociais do contrato em uma versão.
 *
 * A publicação cria a versão 1. Aditamentos e demais fatos publicados criam
 * novas fotografias imutáveis, sem alterar versões já persistidas.
 */
@Entity
@Table(
        name = "contrato_versao_negocial",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contrato_versao_negocial",
                columnNames = {"id_contrato", "numero_versao"}
        ),
        indexes = {
                @Index(name = "idx_contrato_versao_data", columnList = "id_contrato, data_efetiva"),
                @Index(name = "idx_contrato_versao_registro", columnList = "id_contrato, registrado_em"),
                @Index(name = "idx_contrato_versao_evento", columnList = "id_evento_origem")
        }
)
public class VersaoContratoNegocial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato_versao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento_origem", updatable = false)
    private EventoCicloVidaContrato eventoOrigem;

    @Column(name = "numero_versao", nullable = false, updatable = false)
    private Integer numeroVersao;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm = Instant.now();

    @Column(name = "numero_contrato", length = 80, updatable = false)
    private String numeroContrato;

    @Column(name = "ano_contrato", updatable = false)
    private Integer anoContrato;

    @Column(name = "descricao", length = 500, updatable = false)
    private String descricao;

    @Column(name = "tipo_contratacao_id", updatable = false)
    private Long tipoContratacaoId;

    @Column(name = "tipo_contratacao_descricao", length = 160, updatable = false)
    private String tipoContratacaoDescricao;

    @Column(name = "fornecedor_id", updatable = false)
    private Long fornecedorId;

    @Column(name = "cnpj_empresa", length = 14, updatable = false)
    private String cnpjEmpresa;

    @Column(name = "razao_social_empresa", length = 180, updatable = false)
    private String razaoSocialEmpresa;

    @Column(name = "nome_fantasia_empresa", length = 180, updatable = false)
    private String nomeFantasiaEmpresa;

    @Column(name = "data_inicio", updatable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", updatable = false)
    private LocalDate dataFim;

    @Column(name = "efetivo_previsto", updatable = false)
    private Integer efetivoPrevisto;

    @Column(name = "gestao_local_id", updatable = false)
    private Long gestaoLocalId;

    @Column(name = "gestao_local_nome", length = 180, updatable = false)
    private String gestaoLocalNome;

    public VersaoContratoNegocial() {}

    @PrePersist
    protected void validar() {
        if (registradoEm == null) registradoEm = Instant.now();
        if (contrato == null || numeroVersao == null || numeroVersao < 1 || dataEfetiva == null) {
            throw new IllegalStateException("Versão negocial do contrato incompleta.");
        }
    }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public EventoCicloVidaContrato getEventoOrigem() { return eventoOrigem; }
    public void setEventoOrigem(EventoCicloVidaContrato eventoOrigem) { this.eventoOrigem = eventoOrigem; }
    public Integer getNumeroVersao() { return numeroVersao; }
    public void setNumeroVersao(Integer numeroVersao) { this.numeroVersao = numeroVersao; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public Instant getRegistradoEm() { return registradoEm; }
    public String getNumeroContrato() { return numeroContrato; }
    public void setNumeroContrato(String numeroContrato) { this.numeroContrato = numeroContrato; }
    public Integer getAnoContrato() { return anoContrato; }
    public void setAnoContrato(Integer anoContrato) { this.anoContrato = anoContrato; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Long getTipoContratacaoId() { return tipoContratacaoId; }
    public void setTipoContratacaoId(Long tipoContratacaoId) { this.tipoContratacaoId = tipoContratacaoId; }
    public String getTipoContratacaoDescricao() { return tipoContratacaoDescricao; }
    public void setTipoContratacaoDescricao(String tipoContratacaoDescricao) { this.tipoContratacaoDescricao = tipoContratacaoDescricao; }
    public Long getFornecedorId() { return fornecedorId; }
    public void setFornecedorId(Long fornecedorId) { this.fornecedorId = fornecedorId; }
    public String getCnpjEmpresa() { return cnpjEmpresa; }
    public void setCnpjEmpresa(String cnpjEmpresa) { this.cnpjEmpresa = cnpjEmpresa; }
    public String getRazaoSocialEmpresa() { return razaoSocialEmpresa; }
    public void setRazaoSocialEmpresa(String razaoSocialEmpresa) { this.razaoSocialEmpresa = razaoSocialEmpresa; }
    public String getNomeFantasiaEmpresa() { return nomeFantasiaEmpresa; }
    public void setNomeFantasiaEmpresa(String nomeFantasiaEmpresa) { this.nomeFantasiaEmpresa = nomeFantasiaEmpresa; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }
    public Integer getEfetivoPrevisto() { return efetivoPrevisto; }
    public void setEfetivoPrevisto(Integer efetivoPrevisto) { this.efetivoPrevisto = efetivoPrevisto; }
    public Long getGestaoLocalId() { return gestaoLocalId; }
    public void setGestaoLocalId(Long gestaoLocalId) { this.gestaoLocalId = gestaoLocalId; }
    public String getGestaoLocalNome() { return gestaoLocalNome; }
    public void setGestaoLocalNome(String gestaoLocalNome) { this.gestaoLocalNome = gestaoLocalNome; }
}
