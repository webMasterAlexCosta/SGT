package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.relatorio.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "emissao_relatorio_administrativo", indexes = {
        @Index(name = "idx_emissao_serie_versao", columnList = "chave_serie,numero_versao"),
        @Index(name = "idx_emissao_periodo_estado", columnList = "inicio,fim,estado"),
        @Index(name = "idx_emissao_usuario_data", columnList = "usuario_id,gerado_em"),
        @Index(name = "idx_emissao_escopo", columnList = "gestao_local_escopo_id,superintendencia_escopo_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_emissao_serie_versao", columnNames = {"chave_serie", "numero_versao"}),
        @UniqueConstraint(name = "uk_emissao_usuario_idempotencia", columnNames = {"usuario_id", "chave_idempotencia"})
})
public class EmissaoRelatorioAdministrativo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_serie", nullable = false, length = 64, updatable = false)
    private String chaveSerie;
    @Column(name = "numero_versao", nullable = false, updatable = false)
    private Integer numeroVersao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private TipoRelatorioAdministrativo tipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private FormatoEmissaoRelatorio formato;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private NaturezaEmissaoRelatorio natureza;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEmissaoRelatorio estado = EstadoEmissaoRelatorio.VIGENTE;

    @Column(nullable = false, updatable = false)
    private LocalDate inicio;
    @Column(nullable = false, updatable = false)
    private LocalDate fim;
    @Column(name = "gerado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant geradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    @Column(name = "fonte_conhecida_ate", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant fonteConhecidaAte;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;
    @Column(name = "usuario_username", nullable = false, length = 120, updatable = false)
    private String usuarioUsername;
    @Column(name = "perfil_usuario", nullable = false, length = 32, updatable = false)
    private String perfilUsuario;

    @Column(name = "sujeito_revisao", nullable = false, updatable = false)
    private boolean sujeitoRevisao;
    @Column(name = "quantidade_pendencias", nullable = false, updatable = false)
    private long quantidadePendencias;
    @Column(name = "aviso_revisao", nullable = false, length = 300, updatable = false)
    private String avisoRevisao;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "conteudo_snapshot_gzip", nullable = false, updatable = false,
            columnDefinition = "LONGBLOB")
    private byte[] conteudoSnapshotGzip;
    @Column(name = "hash_conteudo", nullable = false, length = 64, updatable = false)
    private String hashConteudo;
    @Column(name = "tamanho_conteudo_bytes", nullable = false, updatable = false)
    private long tamanhoConteudoBytes;
    @Lob
    @Column(name = "filtros_json", nullable = false, updatable = false)
    private String filtrosJson;
    @Column(name = "contrato_ids_csv", length = 2000, updatable = false)
    private String contratoIdsCsv;
    @Column(name = "lotacao_id", updatable = false)
    private Long lotacaoId;
    @Column(name = "jornada_id", updatable = false)
    private Long jornadaId;
    @Column(name = "gestao_local_escopo_id", updatable = false)
    private Long gestaoLocalEscopoId;
    @Column(name = "superintendencia_escopo_id", updatable = false)
    private Long superintendenciaEscopoId;

    @Column(name = "emissao_anterior_id", updatable = false)
    private Long emissaoAnteriorId;
    @Column(name = "chave_idempotencia", nullable = false, length = 120, updatable = false)
    private String chaveIdempotencia;
    @Column(name = "hash_intencao", nullable = false, length = 64, updatable = false)
    private String hashIntencao;

    @Column(name = "superada_em")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant superadaEm;
    @Enumerated(EnumType.STRING)
    @Column(name = "causa_superacao", length = 32)
    private CausaSuperacaoRelatorio causaSuperacao;
    @Column(name = "motivo_superacao", length = 1000)
    private String motivoSuperacao;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    protected void validar() {
        if (chaveSerie == null || chaveSerie.length() != 64 || numeroVersao == null || numeroVersao < 1
                || tipo == null || formato == null || natureza == null || estado == null
                || inicio == null || fim == null || inicio.isAfter(fim) || geradoEm == null
                || fonteConhecidaAte == null || usuarioId == null || usuarioUsername == null
                || perfilUsuario == null || avisoRevisao == null || conteudoSnapshotGzip == null
                || hashConteudo == null || hashConteudo.length() != 64 || filtrosJson == null
                || chaveIdempotencia == null || chaveIdempotencia.isBlank()
                || hashIntencao == null || hashIntencao.length() != 64) {
            throw new IllegalStateException("Emissão administrativa de relatório incompleta.");
        }
        if (estado == EstadoEmissaoRelatorio.VIGENTE) {
            superadaEm = null;
            causaSuperacao = null;
            motivoSuperacao = null;
        } else if (superadaEm == null || causaSuperacao == null) {
            throw new IllegalStateException("Emissão superada exige data e causa.");
        }
    }

    public Long getId() { return id; }
    public String getChaveSerie() { return chaveSerie; }
    public void setChaveSerie(String v) { chaveSerie = v; }
    public Integer getNumeroVersao() { return numeroVersao; }
    public void setNumeroVersao(Integer v) { numeroVersao = v; }
    public TipoRelatorioAdministrativo getTipo() { return tipo; }
    public void setTipo(TipoRelatorioAdministrativo v) { tipo = v; }
    public FormatoEmissaoRelatorio getFormato() { return formato; }
    public void setFormato(FormatoEmissaoRelatorio v) { formato = v; }
    public NaturezaEmissaoRelatorio getNatureza() { return natureza; }
    public void setNatureza(NaturezaEmissaoRelatorio v) { natureza = v; }
    public EstadoEmissaoRelatorio getEstado() { return estado; }
    public void setEstado(EstadoEmissaoRelatorio v) { estado = v; }
    public LocalDate getInicio() { return inicio; }
    public void setInicio(LocalDate v) { inicio = v; }
    public LocalDate getFim() { return fim; }
    public void setFim(LocalDate v) { fim = v; }
    public Instant getGeradoEm() { return geradoEm; }
    public void setGeradoEm(Instant v) { this.geradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(v); }
    public Instant getFonteConhecidaAte() { return fonteConhecidaAte; }
    public void setFonteConhecidaAte(Instant v) { this.fonteConhecidaAte = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(v); }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long v) { usuarioId = v; }
    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String v) { usuarioUsername = v; }
    public String getPerfilUsuario() { return perfilUsuario; }
    public void setPerfilUsuario(String v) { perfilUsuario = v; }
    public boolean isSujeitoRevisao() { return sujeitoRevisao; }
    public void setSujeitoRevisao(boolean v) { sujeitoRevisao = v; }
    public long getQuantidadePendencias() { return quantidadePendencias; }
    public void setQuantidadePendencias(long v) { quantidadePendencias = v; }
    public String getAvisoRevisao() { return avisoRevisao; }
    public void setAvisoRevisao(String v) { avisoRevisao = v; }
    public byte[] getConteudoSnapshotGzip() { return conteudoSnapshotGzip; }
    public void setConteudoSnapshotGzip(byte[] v) { conteudoSnapshotGzip = v; }
    public String getHashConteudo() { return hashConteudo; }
    public void setHashConteudo(String v) { hashConteudo = v; }
    public long getTamanhoConteudoBytes() { return tamanhoConteudoBytes; }
    public void setTamanhoConteudoBytes(long v) { tamanhoConteudoBytes = v; }
    public String getFiltrosJson() { return filtrosJson; }
    public void setFiltrosJson(String v) { filtrosJson = v; }
    public String getContratoIdsCsv() { return contratoIdsCsv; }
    public void setContratoIdsCsv(String v) { contratoIdsCsv = v; }
    public Long getLotacaoId() { return lotacaoId; }
    public void setLotacaoId(Long v) { lotacaoId = v; }
    public Long getJornadaId() { return jornadaId; }
    public void setJornadaId(Long v) { jornadaId = v; }
    public Long getGestaoLocalEscopoId() { return gestaoLocalEscopoId; }
    public void setGestaoLocalEscopoId(Long v) { gestaoLocalEscopoId = v; }
    public Long getSuperintendenciaEscopoId() { return superintendenciaEscopoId; }
    public void setSuperintendenciaEscopoId(Long v) { superintendenciaEscopoId = v; }
    public Long getEmissaoAnteriorId() { return emissaoAnteriorId; }
    public void setEmissaoAnteriorId(Long v) { emissaoAnteriorId = v; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String v) { chaveIdempotencia = v; }
    public String getHashIntencao() { return hashIntencao; }
    public void setHashIntencao(String v) { hashIntencao = v; }
    public Instant getSuperadaEm() { return superadaEm; }
    public void setSuperadaEm(Instant v) { this.superadaEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(v); }
    public CausaSuperacaoRelatorio getCausaSuperacao() { return causaSuperacao; }
    public void setCausaSuperacao(CausaSuperacaoRelatorio v) { causaSuperacao = v; }
    public String getMotivoSuperacao() { return motivoSuperacao; }
    public void setMotivoSuperacao(String v) { motivoSuperacao = v; }
    public long getVersion() { return version; }
}
