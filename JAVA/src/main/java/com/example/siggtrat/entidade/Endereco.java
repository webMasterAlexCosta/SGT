package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "endereco", indexes = { @Index(name = "idx_endereco_version", columnList = "version") })
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_endereco")
    private Long id;

    @Column(name = "logradouro", length = 150, nullable = false)
    private String logradouro;
    @Column(name = "numero", length = 10)
    private String numero;
    @Column(name = "bairro", length = 100)
    private String bairro;
    @Column(name = "cidade", length = 100)
    private String cidade;
    @Column(name = "uf", length = 2)
    private String uf;
    @Column(name = "cep", length = 10)
    private String cep;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void normalizarEValidar() {
        if (logradouro == null || logradouro.isBlank()) {
            throw new IllegalStateException("O logradouro é obrigatório.");
        }
        logradouro = logradouro.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
        if (numero != null) numero = numero.trim();
        if (bairro != null) bairro = bairro.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
        if (cidade != null) cidade = cidade.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
        if (cep != null) {
            cep = cep.replaceAll("\\D", "");
            if (cep.isBlank()) cep = null;
        }
        if (uf != null && !uf.isBlank()) {
            uf = uf.trim().toUpperCase(java.util.Locale.ROOT);
            if (!com.example.siggtrat.constante.ConstantesDominio.UFS_BRASIL.contains(uf)) {
                throw new IllegalStateException("A UF do endereço é inválida.");
            }
        } else {
            uf = null;
        }
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Endereco() {}
    public Endereco(String logradouro, String numero, String bairro, String cidade, String uf, String cep) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.cep = cep;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
}
