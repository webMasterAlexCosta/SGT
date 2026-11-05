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
    @Column(name = "lotacao_nome_fotografia", length = 180, updatable = false)
    private String lotacaoNomeFotografia;
    @Column(name = "jornada_descricao_fotografia", length = 180, updatable = false)
    private String jornadaDescricaoFotografia;
    @Column(name = "cargo_nome_fotografia", length = 180, updatable = false)
    private String cargoNomeFotografia;
    @Column(name = "lotacao_municipio_fotografia", length = 180, updatable = false)
    private String lotacaoMunicipioFotografia;
    @Column(name = "jornada_semanal_fotografia", length = 1000, updatable = false)
    private String jornadaSemanalFotografia;
    /** Null identifica acervo sem esse metadado; true protege a ausência expressamente comprovada. */
    @Column(name = "municipio_ausencia_comprovada", updatable = false)
    private Boolean municipioAusenciaComprovada;
    /** Origem efetivamente utilizada ao importar a fotografia, sem inferência posterior pelos catálogos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aditamento_composicao_fotografia", updatable = false)
    private AditamentoComposicao aditamentoComposicaoFotografia;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;
    @Column(name = "data_fim")
    private LocalDate dataFim;
    @Column(name = "criado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
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
    /** A ausência de um campo da fotografia importada também pertence à prova. */
    @Transient
    private boolean fotografiaImportada;

    @PrePersist
    protected void validar() {
        if (vinculo == null || lotacao == null || jornada == null || cargo == null || dataInicio == null) {
            throw new IllegalStateException("Alocação deve conter vínculo, Lotação, Turno, Cargo e início.");
        }
        if (dataFim != null && dataFim.isBefore(dataInicio)) throw new IllegalStateException("Período da alocação é inválido.");
        if (criadoEm == null) criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (estadoNegocial == null) estadoNegocial = EstadoNegocialAlocacao.REGISTRADA;
        // Atos atuais/futuros capturam seus mestres. Contexto retroativo ou
        // restaurado vem de fonte documental e conserva inclusive as lacunas.
        if (!fotografiaImportada) {
            if (lotacaoNomeFotografia == null) lotacaoNomeFotografia = lotacao.getNome();
            if (jornadaDescricaoFotografia == null) jornadaDescricaoFotografia = jornada.getDescricao();
            if (cargoNomeFotografia == null) cargoNomeFotografia = cargo.getNome();
            if (jornadaSemanalFotografia == null) {
                lotacaoMunicipioFotografia = lotacao.getMunicipio();
                jornadaSemanalFotografia = com.example.siggtrat.utilitario.FotografiaJornada.capturar(jornada);
            }
            municipioAusenciaComprovada = lotacaoMunicipioFotografia == null;
        }
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
    public String getLotacaoNomeFotografia() { return lotacaoNomeFotografia; }
    public String getJornadaDescricaoFotografia() { return jornadaDescricaoFotografia; }
    public String getCargoNomeFotografia() { return cargoNomeFotografia; }
    /** Contexto do recorte publicado, inclusive lacunas que não podem virar dados atuais. */
    public void aplicarFotografiaDocumentada(String lotacaoNome, String jornadaDescricao, String cargoNome,
                                               String municipio, String semana) {
        aplicarFotografiaDocumentada(lotacaoNome,jornadaDescricao,cargoNome,municipio,semana,
                municipio!=null||semana!=null&&!semana.isBlank(),null);
    }
    /** A prova de grade não transforma município desconhecido em ausência documentada. */
    public void aplicarFotografiaDocumentada(String lotacaoNome, String jornadaDescricao, String cargoNome,
            String municipio, String semana, boolean municipioComprovado, AditamentoComposicao origemAditamento) {
        if (id != null) throw new IllegalStateException("A fotografia de Alocação persistida é imutável.");
        lotacaoNomeFotografia = rotuloFotografado(lotacaoNome);
        jornadaDescricaoFotografia = rotuloFotografado(jornadaDescricao);
        cargoNomeFotografia = rotuloFotografado(cargoNome);
        lotacaoMunicipioFotografia = municipio;
        jornadaSemanalFotografia = semana == null || semana.isBlank() ? null : semana;
        municipioAusenciaComprovada = municipioComprovado && municipio == null;
        aditamentoComposicaoFotografia = origemAditamento;
        fotografiaImportada = true;
    }
    /** Reutiliza contexto documentado sem alterar uma fotografia persistida. */
    public void aplicarFotografiaDoMapeamento(MapeamentoTransferenciaGestao mapa) {
        if (id != null) throw new IllegalStateException("A fotografia de Alocação persistida é imutável.");
        if (mapa.getJornadaDestinoSemanalFotografia() == null || mapa.getJornadaDestinoSemanalFotografia().isBlank())
            throw new IllegalStateException("Mapeamento não possui fotografia histórica da jornada de destino.");
        aplicarFotografiaDocumentada(mapa.getLotacaoDestinoNomeFotografia(), mapa.getJornadaDestinoDescricaoFotografia(),
                mapa.getCargoDestinoNomeFotografia(), mapa.getLotacaoDestinoMunicipioFotografia(),
                mapa.getJornadaDestinoSemanalFotografia());
    }
    /** O mapa comprova os nomes da origem; campos não preservados continuam desconhecidos. */
    public void aplicarFotografiaDaOrigemDoMapeamento(MapeamentoTransferenciaGestao mapa) {
        if (id != null) throw new IllegalStateException("A fotografia de Alocação persistida é imutável.");
        aplicarFotografiaDocumentada(mapa.getLotacaoOrigemNomeFotografia(), mapa.getJornadaOrigemDescricaoFotografia(),
                mapa.getCargoOrigemNomeFotografia(), null, null);
    }
    private static String rotuloFotografado(String valor) { return valor == null || valor.isBlank() ? null : valor; }
    public String getLotacaoMunicipioFotografia() { return lotacaoMunicipioFotografia; }
    public String getJornadaSemanalFotografia() { return jornadaSemanalFotografia; }
    public Boolean getMunicipioAusenciaComprovada() { return municipioAusenciaComprovada; }
    public AditamentoComposicao getAditamentoComposicaoFotografia() { return aditamentoComposicaoFotografia; }
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
