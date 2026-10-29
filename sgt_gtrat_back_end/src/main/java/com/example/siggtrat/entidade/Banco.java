package com.example.siggtrat.entidade;

import jakarta.persistence.*;

@Entity
@Table(name = "banco", indexes = { @Index(name = "idx_banco_version", columnList = "version") })
public class Banco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_banco")
    private Long id;

    @Column(name = "nome", length = 100, nullable = false,unique = true)
    private String nome;

    @Column(name = "codigo", length = 10, nullable = false, unique = true)
    private String codigo;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Banco() {}
    public Banco(String nome, String codigo) {
        this.nome = nome;
        this.codigo = codigo;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
}
