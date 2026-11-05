package com.example.siggtrat.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Estado técnico de invalidação; não constitui fato ou fotografia de negócio. */
@Entity
@Table(name = "revisao_fontes_painel")
public class RevisaoFontesPainel {
    @Id
    private Integer id;

    @Column(nullable = false, length = 36)
    private String valor;

    @Version
    private long version;

    public RevisaoFontesPainel() {}
    public RevisaoFontesPainel(String valor) { this.id = 1; this.valor = valor; }
    public Integer getId() { return id; }
    public String getValor() { return valor; }
}
