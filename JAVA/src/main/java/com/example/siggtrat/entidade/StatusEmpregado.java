package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "status_empregado", indexes = { @Index(name = "idx_status_empregado_version", columnList = "version") })
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class StatusEmpregado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_status")
    private Long id;

    @Column(name = "descricao", length = 30, nullable = false, unique = true)
    private String descricao;

    /**
     * Versão da entidade para controle otimista de concorrência.
     */
    @Version
    @Column(name = "version")
    private long version = 0L;

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public StatusEmpregado() {}
    public StatusEmpregado(String descricao) { this.descricao = descricao; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
