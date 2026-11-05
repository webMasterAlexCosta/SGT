package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "aditamento_contratual",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aditamento_contrato_identificacao",
                columnNames = {"id_contrato", "identificacao", "versao_instrumento"}
        ),
        indexes = {
                @Index(name = "idx_aditamento_contrato_data", columnList = "id_contrato, data_efetiva"),
                @Index(name = "idx_aditamento_evento", columnList = "id_evento")
        }
)
public class AditamentoContratual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aditamento")
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

    @Column(name = "data_documento", nullable = false, updatable = false)
    private LocalDate dataDocumento;

    @Column(name = "data_assinatura", updatable = false)
    private LocalDate dataAssinatura;

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

    // Deltas negociais. Null significa que o campo não foi alterado pelo instrumento.
    @Column(name = "nova_descricao", length = 500, updatable = false)
    private String novaDescricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novo_tipo_contratacao_id", updatable = false)
    private TipoContratacao novoTipoContratacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novo_fornecedor_id", updatable = false)
    private Fornecedor novoFornecedor;

    @Column(name = "novo_cnpj_empresa", length = 14, updatable = false)
    private String novoCnpjEmpresa;

    @Column(name = "nova_razao_social_empresa", length = 180, updatable = false)
    private String novaRazaoSocialEmpresa;

    @Column(name = "novo_nome_fantasia_empresa", length = 180, updatable = false)
    private String novoNomeFantasiaEmpresa;

    @Column(name = "nova_data_inicio", updatable = false)
    private LocalDate novaDataInicio;

    @Column(name = "nova_data_fim", updatable = false)
    private LocalDate novaDataFim;

    @Column(name = "novo_efetivo_total", updatable = false)
    private Integer novoEfetivoTotal;

    @Column(name = "nova_matricula_vinculo_obrigatoria", updatable = false)
    private Boolean novaMatriculaVinculoObrigatoria;

    @Column(name = "matricula_novo_tipo_fotografada", updatable = false)
    private Boolean matriculaNovoTipoFotografada;

    @Column(name = "novo_tipo_descricao_fotografia", nullable = true, length = 200, updatable = false)
    private String novoTipoDescricaoFotografia;

    @Enumerated(EnumType.STRING)
    @Column(name = "novo_dia_semana_reduzido", length = 12, updatable = false)
    private DayOfWeek novoDiaSemanaReduzido;

    @Column(name = "novas_horas_dia_reduzido", updatable = false)
    private Integer novasHorasDiaReduzido;

    @Column(name = "restaurar_politica_matricula_tipo", updatable = false)
    private Boolean restaurarPoliticaMatriculaDoTipo;

    @Column(name = "remover_configuracao_dia_reduzido", updatable = false)
    private Boolean removerConfiguracaoDiaReduzido;

    @Column(name = "reducao_excepcional", nullable = false, updatable = false)
    private Boolean reducaoExcepcional = false;

    @Column(name = "fundamento_excecao", length = 1500, updatable = false)
    private String fundamentoExcecao;

    @Column(name = "impacto_confirmado", nullable = false, updatable = false)
    private Boolean impactoConfirmado = false;

    @OneToMany(mappedBy = "aditamento", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("id ASC")
    private List<AditamentoComposicao> alteracoesComposicao = new ArrayList<>();

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void validar() {
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (contrato == null || evento == null || identificacao == null || identificacao.isBlank()
                || dataDocumento == null || dataEfetiva == null || justificativa == null || justificativa.isBlank()) {
            throw new IllegalStateException("Aditamento contratual incompleto.");
        }
        identificacao = identificacao.trim();
        justificativa = justificativa.trim().replaceAll("\\s+", " ");
        if (referenciaSei != null) referenciaSei = referenciaSei.trim();
        if (novoCnpjEmpresa != null) novoCnpjEmpresa = novoCnpjEmpresa.replaceAll("\\D", "");
        if (novaDescricao != null) novaDescricao = novaDescricao.trim().replaceAll("\\s+", " ");
        if (novaRazaoSocialEmpresa != null) novaRazaoSocialEmpresa = novaRazaoSocialEmpresa.trim().replaceAll("\\s+", " ");
        if (novoNomeFantasiaEmpresa != null) novoNomeFantasiaEmpresa = novoNomeFantasiaEmpresa.trim().replaceAll("\\s+", " ");
        if (fundamentoExcecao != null) fundamentoExcecao = fundamentoExcecao.trim().replaceAll("\\s+", " ");
        if (versaoInstrumento == null || versaoInstrumento < 1) versaoInstrumento = 1;
        if (reducaoExcepcional == null) reducaoExcepcional = false;
        if (impactoConfirmado == null) impactoConfirmado = false;
    }

    public void adicionarAlteracao(AditamentoComposicao item) {
        if (item == null) return;
        item.setAditamento(this);
        alteracoesComposicao.add(item);
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
    public LocalDate getDataDocumento() { return dataDocumento; }
    public void setDataDocumento(LocalDate dataDocumento) { this.dataDocumento = dataDocumento; }
    public LocalDate getDataAssinatura() { return dataAssinatura; }
    public void setDataAssinatura(LocalDate dataAssinatura) { this.dataAssinatura = dataAssinatura; }
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
    public String getNovaDescricao() { return novaDescricao; }
    public void setNovaDescricao(String novaDescricao) { this.novaDescricao = novaDescricao; }
    public TipoContratacao getNovoTipoContratacao() { return novoTipoContratacao; }
    public void setNovoTipoContratacao(TipoContratacao novoTipoContratacao) { this.novoTipoContratacao = novoTipoContratacao; }
    public Fornecedor getNovoFornecedor() { return novoFornecedor; }
    public void setNovoFornecedor(Fornecedor novoFornecedor) { this.novoFornecedor = novoFornecedor; }
    public String getNovoCnpjEmpresa() { return novoCnpjEmpresa; }
    public void setNovoCnpjEmpresa(String novoCnpjEmpresa) { this.novoCnpjEmpresa = novoCnpjEmpresa; }
    public String getNovaRazaoSocialEmpresa() { return novaRazaoSocialEmpresa; }
    public void setNovaRazaoSocialEmpresa(String novaRazaoSocialEmpresa) { this.novaRazaoSocialEmpresa = novaRazaoSocialEmpresa; }
    public String getNovoNomeFantasiaEmpresa() { return novoNomeFantasiaEmpresa; }
    public void setNovoNomeFantasiaEmpresa(String novoNomeFantasiaEmpresa) { this.novoNomeFantasiaEmpresa = novoNomeFantasiaEmpresa; }
    public LocalDate getNovaDataInicio() { return novaDataInicio; }
    public void setNovaDataInicio(LocalDate novaDataInicio) { this.novaDataInicio = novaDataInicio; }
    public LocalDate getNovaDataFim() { return novaDataFim; }
    public void setNovaDataFim(LocalDate novaDataFim) { this.novaDataFim = novaDataFim; }
    public Integer getNovoEfetivoTotal() { return novoEfetivoTotal; }
    public void setNovoEfetivoTotal(Integer novoEfetivoTotal) { this.novoEfetivoTotal = novoEfetivoTotal; }
    public Boolean getNovaMatriculaVinculoObrigatoria() { return novaMatriculaVinculoObrigatoria; }
    public void setNovaMatriculaVinculoObrigatoria(Boolean valor) { this.novaMatriculaVinculoObrigatoria = valor; }
    public Boolean getMatriculaNovoTipoFotografada() { return matriculaNovoTipoFotografada; }
    public void setMatriculaNovoTipoFotografada(Boolean valor) { this.matriculaNovoTipoFotografada = valor; }
    public String getNovoTipoDescricaoFotografia() { return novoTipoDescricaoFotografia; }
    public void setNovoTipoDescricaoFotografia(String valor) { this.novoTipoDescricaoFotografia = valor; }
    public DayOfWeek getNovoDiaSemanaReduzido() { return novoDiaSemanaReduzido; }
    public void setNovoDiaSemanaReduzido(DayOfWeek valor) { this.novoDiaSemanaReduzido = valor; }
    public Integer getNovasHorasDiaReduzido() { return novasHorasDiaReduzido; }
    public void setNovasHorasDiaReduzido(Integer valor) { this.novasHorasDiaReduzido = valor; }
    public Boolean getRestaurarPoliticaMatriculaDoTipo() { return restaurarPoliticaMatriculaDoTipo; }
    public void setRestaurarPoliticaMatriculaDoTipo(Boolean valor) { this.restaurarPoliticaMatriculaDoTipo = valor; }
    public Boolean getRemoverConfiguracaoDiaReduzido() { return removerConfiguracaoDiaReduzido; }
    public void setRemoverConfiguracaoDiaReduzido(Boolean valor) { this.removerConfiguracaoDiaReduzido = valor; }
    public Boolean getReducaoExcepcional() { return reducaoExcepcional; }
    public void setReducaoExcepcional(Boolean reducaoExcepcional) { this.reducaoExcepcional = reducaoExcepcional; }
    public String getFundamentoExcecao() { return fundamentoExcecao; }
    public void setFundamentoExcecao(String fundamentoExcecao) { this.fundamentoExcecao = fundamentoExcecao; }
    public Boolean getImpactoConfirmado() { return impactoConfirmado; }
    public void setImpactoConfirmado(Boolean impactoConfirmado) { this.impactoConfirmado = impactoConfirmado; }
    public List<AditamentoComposicao> getAlteracoesComposicao() { return alteracoesComposicao; }
    public long getVersion() { return version; }
}
