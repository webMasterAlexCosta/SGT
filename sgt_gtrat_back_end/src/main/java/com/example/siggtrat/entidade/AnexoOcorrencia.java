package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "anexo_ocorrencia", indexes = { @Index(name = "idx_anexo_ocorrencia_version", columnList = "version") })
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class AnexoOcorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "nome_salvo", nullable = false)
    private String nomeSalvo; // Nome no disco (UUID)

    @Column(name = "tipo_arquivo")
    private String tipoArquivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ocorrencia")
    @JsonIgnore // Evita loop infinito no JSON
    private Ocorrencia ocorrencia;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public AnexoOcorrencia() {}

    public AnexoOcorrencia(String nomeOriginal, String nomeSalvo, String tipoArquivo, Ocorrencia ocorrencia) {
        this.nomeOriginal = nomeOriginal;
        this.nomeSalvo = nomeSalvo;
        this.tipoArquivo = tipoArquivo;
        this.ocorrencia = ocorrencia;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }

    public String getNomeSalvo() { return nomeSalvo; }
    public void setNomeSalvo(String nomeSalvo) { this.nomeSalvo = nomeSalvo; }

    public String getTipoArquivo() { return tipoArquivo; }
    public void setTipoArquivo(String tipoArquivo) { this.tipoArquivo = tipoArquivo; }

    public Ocorrencia getOcorrencia() { return ocorrencia; }
    public void setOcorrencia(Ocorrencia ocorrencia) { this.ocorrencia = ocorrencia; }
}