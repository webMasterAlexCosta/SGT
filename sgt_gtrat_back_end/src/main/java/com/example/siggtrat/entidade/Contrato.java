package com.example.siggtrat.entidade;

import com.example.siggtrat.utilitario.RelogioSistema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "contrato",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contrato_numero_ano",
                        columnNames = {"numero_contrato", "ano_contrato"}
                )
        },
        indexes = {
                @Index(name = "idx_contrato_version", columnList = "version"),
                @Index(name = "idx_contrato_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_contrato_numero_ano", columnList = "numero_contrato, ano_contrato"),
                @Index(name = "idx_contrato_estado_negocial", columnList = "estado_negocial"),
                @Index(name = "idx_contrato_gestao_local_ativo_periodo",
                        columnList = "id_gestao_local, ativo, data_inicio, data_fim")
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    @JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"})
    private GestaoLocal gestaoLocal;

    /**
     * Relação com o cadastro de fornecedor/governança mantida enquanto os demais módulos são integrados ao modelo temporal.
     * Os dados negociais da empresa também são fotografados no contrato para
     * que alterações no cadastro mestre não reescrevam o passado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_fornecedor")
    private Fornecedor fornecedor;

    @Column(name = "numero_contrato", length = 50)
    private String numeroContrato;

    @Column(name = "ano_contrato")
    private Integer anoContrato;

    @Column(name = "descricao")
    private String descricao;

    /**
     * Fotografia textual da descrição do tipo no recorte atual.
     * Novos contratos devem apontar para TipoContratacao; a história publicada
     * permanece preservada pelas versões negociais.
     */
    @Column(name = "tipo", length = 160)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_contratacao")
    private TipoContratacao tipoContratacao;

    @Column(name = "cnpj_empresa", length = 14)
    private String cnpjEmpresa;

    @Column(name = "razao_social_empresa", length = 180)
    private String razaoSocialEmpresa;

    @Column(name = "nome_fantasia_empresa", length = 180)
    private String nomeFantasiaEmpresa;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    /**
     * A coluna ativo representa exclusivamente a habilitação administrativa.
     * Ela NÃO representa FUTURO/VIGENTE/ENCERRADO.
     */
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /**
     * Somente fatos negociais explícitos são persistidos.
     * O estado é preenchido pelos fluxos negociais do contrato.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocial", length = 24, nullable = false)
    private EstadoNegocialContrato estadoNegocial = EstadoNegocialContrato.RASCUNHO;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "publicado_por_id")
    private Long publicadoPorId;

    @Column(name = "publicado_por_username", length = 120)
    private String publicadoPorUsername;

    @Column(name = "referencia_sei_publicacao", length = 120)
    private String referenciaSeiPublicacao;

    /** Quantidade total contratualmente prevista no recorte atual. */
    @Column(name = "efetivo_previsto", nullable = false)
    private Integer efetivoPrevisto = 0;

    /** Base imutável para calcular o limite acumulado de redução após a publicação. */
    @Column(name = "efetivo_original_publicado")
    private Integer efetivoOriginalPublicado;

    /** Override opcional; nulo herda a configuração do Tipo de Contratação. */
    @Column(name = "matricula_vinculo_obrigatoria")
    private Boolean matriculaVinculoObrigatoria;

    /** Configuração semanal usada no cálculo de frequência do Contrato. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana_reduzido")
    private DayOfWeek diaSemanaReduzido;

    @Column(name = "horas_dia_reduzido")
    private Integer horasDiaReduzido;

    @Version
    @Column(name = "version")
    private long version = 0L;

    public Contrato() {}

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (numeroContrato != null) numeroContrato = numeroContrato.trim().toUpperCase();
        if (descricao != null) descricao = descricao.trim().replaceAll("\\s+", " ");
        if (tipo != null) tipo = tipo.trim().replaceAll("\\s+", " ");
        if (cnpjEmpresa != null) cnpjEmpresa = cnpjEmpresa.replaceAll("\\D", "");
        if (razaoSocialEmpresa != null) razaoSocialEmpresa = razaoSocialEmpresa.trim().replaceAll("\\s+", " ");
        if (nomeFantasiaEmpresa != null) nomeFantasiaEmpresa = nomeFantasiaEmpresa.trim().replaceAll("\\s+", " ");
        if (referenciaSeiPublicacao != null) referenciaSeiPublicacao = referenciaSeiPublicacao.trim();

        if (efetivoPrevisto == null || efetivoPrevisto < 0) efetivoPrevisto = 0;
        if (ativo == null) ativo = true;
        if (estadoNegocial == null) estadoNegocial = EstadoNegocialContrato.RASCUNHO;

        if (anoContrato != null && (anoContrato < 1900 || anoContrato > 9999)) {
            throw new IllegalStateException("Ano do contrato inválido.");
        }
        if (cnpjEmpresa != null && !cnpjEmpresa.isBlank() && cnpjEmpresa.length() != 14) {
            throw new IllegalStateException("CNPJ da empresa deve conter 14 dígitos.");
        }
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalStateException("A data final do contrato não pode ser anterior à data inicial.");
        }
        if (horasDiaReduzido != null && (horasDiaReduzido < 1 || horasDiaReduzido > 24)) {
            throw new IllegalStateException("As horas do dia reduzido devem estar entre 1 e 24.");
        }
        if ((diaSemanaReduzido == null) != (horasDiaReduzido == null)) {
            throw new IllegalStateException("Dia reduzido e quantidade de horas devem ser informados juntos.");
        }
    }

    /**
     * Deriva a situação temporal sem criar fatos negociais por rotina técnica.
     */
    @Transient
    public SituacaoTemporalContrato situacaoTemporalEm(LocalDate dataReferencia) {
        LocalDate data = dataReferencia == null ? RelogioSistema.hoje() : dataReferencia;
        EstadoNegocialContrato estado = getEstadoNegocial();

        if (estado == EstadoNegocialContrato.RASCUNHO) return SituacaoTemporalContrato.RASCUNHO;
        if (estado == EstadoNegocialContrato.CANCELADO) return SituacaoTemporalContrato.CANCELADO;
        if (estado == EstadoNegocialContrato.ANULADO) return SituacaoTemporalContrato.ANULADO;

        if (dataInicio != null && data.isBefore(dataInicio)) return SituacaoTemporalContrato.FUTURO;
        if (dataFim != null && data.isAfter(dataFim)) return SituacaoTemporalContrato.ENCERRADO;
        return SituacaoTemporalContrato.VIGENTE;
    }

    @Transient
    public SituacaoTemporalContrato getSituacaoTemporalAtual() {
        return situacaoTemporalEm(RelogioSistema.hoje());
    }

    @Transient
    public HabilitacaoAdministrativaContrato getHabilitacaoAdministrativa() {
        return Boolean.FALSE.equals(ativo)
                ? HabilitacaoAdministrativaContrato.INATIVO
                : HabilitacaoAdministrativaContrato.ATIVO;
    }

    @Transient
    public boolean estaPublicado() {
        return getEstadoNegocial() != EstadoNegocialContrato.RASCUNHO;
    }

    @Transient
    public boolean podeOperarEm(LocalDate dataReferencia) {
        return Boolean.TRUE.equals(ativo)
                && getEstadoNegocial() == EstadoNegocialContrato.PUBLICADO
                && situacaoTemporalEm(dataReferencia) == SituacaoTemporalContrato.VIGENTE;
    }

    @Transient
    public boolean exigeMatriculaNoVinculo() {
        if (matriculaVinculoObrigatoria != null) return matriculaVinculoObrigatoria;
        return tipoContratacao != null && Boolean.TRUE.equals(tipoContratacao.getMatriculaVinculoObrigatoria());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }

    public Fornecedor getFornecedor() { return fornecedor; }
    public void setFornecedor(Fornecedor fornecedor) { this.fornecedor = fornecedor; }

    public String getNumeroContrato() { return numeroContrato; }
    public void setNumeroContrato(String numeroContrato) { this.numeroContrato = numeroContrato; }

    public Integer getAnoContrato() { return anoContrato; }
    public void setAnoContrato(Integer anoContrato) { this.anoContrato = anoContrato; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getTipo() {
        if (tipo != null && !tipo.isBlank()) return tipo;
        return tipoContratacao != null ? tipoContratacao.getDescricao() : null;
    }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public TipoContratacao getTipoContratacao() { return tipoContratacao; }
    public void setTipoContratacao(TipoContratacao tipoContratacao) {
        this.tipoContratacao = tipoContratacao;
        if (tipoContratacao != null && tipoContratacao.getDescricao() != null) {
            this.tipo = tipoContratacao.getDescricao();
        }
    }

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

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public Boolean getMatriculaVinculoObrigatoria() { return matriculaVinculoObrigatoria; }
    public void setMatriculaVinculoObrigatoria(Boolean valor) { this.matriculaVinculoObrigatoria = valor; }

    public EstadoNegocialContrato getEstadoNegocial() {
        return estadoNegocial == null ? EstadoNegocialContrato.RASCUNHO : estadoNegocial;
    }
    public void setEstadoNegocial(EstadoNegocialContrato estadoNegocial) { this.estadoNegocial = estadoNegocial; }

    public Instant getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(Instant publicadoEm) { this.publicadoEm = publicadoEm; }

    public Long getPublicadoPorId() { return publicadoPorId; }
    public void setPublicadoPorId(Long publicadoPorId) { this.publicadoPorId = publicadoPorId; }

    public String getPublicadoPorUsername() { return publicadoPorUsername; }
    public void setPublicadoPorUsername(String publicadoPorUsername) { this.publicadoPorUsername = publicadoPorUsername; }

    public String getReferenciaSeiPublicacao() { return referenciaSeiPublicacao; }
    public void setReferenciaSeiPublicacao(String referenciaSeiPublicacao) { this.referenciaSeiPublicacao = referenciaSeiPublicacao; }

    public Integer getEfetivoPrevisto() { return efetivoPrevisto; }
    public void setEfetivoPrevisto(Integer efetivoPrevisto) { this.efetivoPrevisto = efetivoPrevisto; }

    public Integer getEfetivoOriginalPublicado() { return efetivoOriginalPublicado; }
    public void setEfetivoOriginalPublicado(Integer efetivoOriginalPublicado) { this.efetivoOriginalPublicado = efetivoOriginalPublicado; }

    public DayOfWeek getDiaSemanaReduzido() { return diaSemanaReduzido; }
    public void setDiaSemanaReduzido(DayOfWeek diaSemanaReduzido) { this.diaSemanaReduzido = diaSemanaReduzido; }

    public Integer getHorasDiaReduzido() { return horasDiaReduzido; }
    public void setHorasDiaReduzido(Integer horasDiaReduzido) { this.horasDiaReduzido = horasDiaReduzido; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
