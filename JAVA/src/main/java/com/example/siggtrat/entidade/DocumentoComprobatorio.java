package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.documento.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Uma versão imutável quanto ao conteúdo de uma fonte comprobatória. Mudanças
 * de arquivo ou SEI criam nova linha; somente o estado da versão anterior muda.
 */
@Entity
@Table(name = "documento_comprobatorio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_documento_chave_idempotencia", columnNames = "chave_idempotencia"),
        @UniqueConstraint(name = "uk_documento_grupo_versao", columnNames = {"grupo_logico_id", "versao_documento"})
}, indexes = {
        @Index(name = "idx_documento_contrato_fato", columnList = "id_contrato, tipo_fato, fato_id"),
        @Index(name = "idx_documento_fato_chave", columnList = "id_contrato, tipo_fato, fato_chave_normalizada"),
        @Index(name = "idx_documento_sei", columnList = "referencia_sei_normalizada"),
        @Index(name = "idx_documento_estado", columnList = "estado_documento"),
        @Index(name = "idx_documento_classificacao", columnList = "classificacao_acesso")
})
public class DocumentoComprobatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fato", nullable = false, length = 40, updatable = false)
    private TipoFatoDocumental tipoFato;

    @Column(name = "fato_id")
    private Long fatoId;

    @Column(name = "fato_chave", length = 160, updatable = false)
    private String fatoChave;

    @Column(name = "fato_chave_normalizada", length = 160, updatable = false)
    private String fatoChaveNormalizada;

    @Column(name = "grupo_logico_id", nullable = false, length = 48, updatable = false)
    private String grupoLogicoId;

    @Column(name = "versao_documento", nullable = false, updatable = false)
    private Integer versaoDocumento = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento_anterior", updatable = false)
    private DocumentoComprobatorio documentoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_documento", nullable = false, length = 24)
    private EstadoDocumentoComprobatorio estado = EstadoDocumentoComprobatorio.RASCUNHO;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_acesso", nullable = false, length = 28, updatable = false)
    private ClassificacaoAcessoDocumento classificacaoAcesso;

    @Column(name = "titulo", nullable = false, length = 180, updatable = false)
    private String titulo;

    @Column(name = "descricao", length = 1200, updatable = false)
    private String descricao;

    @Column(name = "referencia_sei", length = 180, updatable = false)
    private String referenciaSei;

    @Column(name = "referencia_sei_normalizada", length = 180, updatable = false)
    private String referenciaSeiNormalizada;

    @Column(name = "nome_original_arquivo", length = 255, updatable = false)
    private String nomeOriginalArquivo;

    @Column(name = "nome_armazenado", length = 80, updatable = false)
    private String nomeArmazenado;

    @Column(name = "tipo_mime", length = 120, updatable = false)
    private String tipoMime;

    @Column(name = "tamanho_bytes", updatable = false)
    private Long tamanhoBytes;

    @Column(name = "sha256_arquivo", length = 64, updatable = false)
    private String sha256Arquivo;

    @Column(name = "data_efetiva", updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "chave_idempotencia", length = 96, updatable = false)
    private String chaveIdempotencia;

    /** Nulo apenas em versões anteriores à validação integral da intenção. */
    @Column(name = "hash_intencao", length = 64, updatable = false)
    private String hashIntencao;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "registrado_por_id", updatable = false)
    private Long registradoPorId;

    @Column(name = "registrado_por_username", length = 120, updatable = false)
    private String registradoPorUsername;

    @Column(name = "publicado_em")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant publicadoEm;

    @Column(name = "publicado_por_id")
    private Long publicadoPorId;

    @Column(name = "publicado_por_username", length = 120)
    private String publicadoPorUsername;

    /** Âncora por versão, mantida junto de cada novo evento; null identifica acervo legado. */
    @Column(name = "quantidade_eventos_auditoria")
    private Long quantidadeEventosAuditoria;

    @Column(name = "ultimo_hash_evento_auditoria", length = 64)
    private String ultimoHashEventoAuditoria;

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void normalizar() {
        if (contrato == null || tipoFato == null || classificacaoAcesso == null
                || titulo == null || titulo.isBlank()) {
            throw new IllegalStateException("Documento deve possuir contrato, fato, classificação e título.");
        }
        if (!possuiArquivo() && !possuiSei()) {
            throw new IllegalStateException("Documento deve possuir anexo, referência SEI ou ambos.");
        }
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (grupoLogicoId == null || grupoLogicoId.isBlank()) grupoLogicoId = UUID.randomUUID().toString();
        if (versaoDocumento == null || versaoDocumento < 1) versaoDocumento = 1;
        if (estado == null) estado = EstadoDocumentoComprobatorio.RASCUNHO;
        titulo = limpar(titulo);
        descricao = limpar(descricao);
        referenciaSei = limpar(referenciaSei);
        referenciaSeiNormalizada = normalizarReferencia(referenciaSei);
        fatoChave = limpar(fatoChave);
        fatoChaveNormalizada = normalizarChave(fatoChave);
        chaveIdempotencia = limpar(chaveIdempotencia);
    }

    public boolean possuiArquivo() { return nomeArmazenado != null && !nomeArmazenado.isBlank(); }
    public boolean possuiSei() { return referenciaSei != null && !referenciaSei.isBlank(); }
    public TipoFonteComprobatoria getTipoFonte() {
        if (possuiArquivo() && possuiSei()) return TipoFonteComprobatoria.ANEXO_E_SEI;
        return possuiArquivo() ? TipoFonteComprobatoria.ANEXO : TipoFonteComprobatoria.SEI;
    }
    public static String normalizarReferencia(String valor) {
        return valor == null ? null : valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
    public static String normalizarChave(String valor) {
        return valor == null ? null : valor.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
    private static String limpar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim().replaceAll("\\s+", " ");
    }

    public Long getQuantidadeEventosAuditoria() { return quantidadeEventosAuditoria; }
    public void setQuantidadeEventosAuditoria(Long quantidade) { this.quantidadeEventosAuditoria = quantidade; }
    public String getUltimoHashEventoAuditoria() { return ultimoHashEventoAuditoria; }
    public void setUltimoHashEventoAuditoria(String hash) { this.ultimoHashEventoAuditoria = hash; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato contrato) { this.contrato = contrato; }
    public TipoFatoDocumental getTipoFato() { return tipoFato; }
    public void setTipoFato(TipoFatoDocumental tipoFato) { this.tipoFato = tipoFato; }
    public Long getFatoId() { return fatoId; }
    public void setFatoId(Long fatoId) { this.fatoId = fatoId; }
    public String getFatoChave() { return fatoChave; }
    public void setFatoChave(String fatoChave) {
        this.fatoChave = fatoChave;
        this.fatoChaveNormalizada = normalizarChave(fatoChave);
    }
    public String getFatoChaveNormalizada() { return fatoChaveNormalizada; }
    public String getGrupoLogicoId() { return grupoLogicoId; }
    public void setGrupoLogicoId(String grupoLogicoId) { this.grupoLogicoId = grupoLogicoId; }
    public Integer getVersaoDocumento() { return versaoDocumento; }
    public void setVersaoDocumento(Integer versaoDocumento) { this.versaoDocumento = versaoDocumento; }
    public DocumentoComprobatorio getDocumentoAnterior() { return documentoAnterior; }
    public void setDocumentoAnterior(DocumentoComprobatorio documentoAnterior) { this.documentoAnterior = documentoAnterior; }
    public EstadoDocumentoComprobatorio getEstado() { return estado; }
    public void setEstado(EstadoDocumentoComprobatorio estado) { this.estado = estado; }
    public ClassificacaoAcessoDocumento getClassificacaoAcesso() { return classificacaoAcesso; }
    public void setClassificacaoAcesso(ClassificacaoAcessoDocumento classificacaoAcesso) { this.classificacaoAcesso = classificacaoAcesso; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String referenciaSei) {
        this.referenciaSei = referenciaSei;
        this.referenciaSeiNormalizada = normalizarReferencia(referenciaSei);
    }
    public String getReferenciaSeiNormalizada() { return referenciaSeiNormalizada; }
    public String getNomeOriginalArquivo() { return nomeOriginalArquivo; }
    public void setNomeOriginalArquivo(String nomeOriginalArquivo) { this.nomeOriginalArquivo = nomeOriginalArquivo; }
    public String getNomeArmazenado() { return nomeArmazenado; }
    public void setNomeArmazenado(String nomeArmazenado) { this.nomeArmazenado = nomeArmazenado; }
    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public String getSha256Arquivo() { return sha256Arquivo; }
    public void setSha256Arquivo(String sha256Arquivo) { this.sha256Arquivo = sha256Arquivo; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public String getHashIntencao() { return hashIntencao; }
    public void setHashIntencao(String hashIntencao) { this.hashIntencao = hashIntencao; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getRegistradoPorId() { return registradoPorId; }
    public void setRegistradoPorId(Long registradoPorId) { this.registradoPorId = registradoPorId; }
    public String getRegistradoPorUsername() { return registradoPorUsername; }
    public void setRegistradoPorUsername(String registradoPorUsername) { this.registradoPorUsername = registradoPorUsername; }
    public Instant getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(Instant publicadoEm) { this.publicadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(publicadoEm); }
    public Long getPublicadoPorId() { return publicadoPorId; }
    public void setPublicadoPorId(Long publicadoPorId) { this.publicadoPorId = publicadoPorId; }
    public String getPublicadoPorUsername() { return publicadoPorUsername; }
    public void setPublicadoPorUsername(String publicadoPorUsername) { this.publicadoPorUsername = publicadoPorUsername; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
