// Caminho: java/com/example/siggtrat/model/ConfiguracaoSistema.java
package com.example.siggtrat.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistema {

    @Id
    @Column(name = "chave", length = 100, nullable = false)
    private String chave;

    @Column(name = "valor", length = 255, nullable = false)
    private String valor;

    @Column(name = "descricao", length = 255)
    private String descricao;

    public ConfiguracaoSistema() {}

    public ConfiguracaoSistema(String chave, String valor, String descricao) {
        this.chave = chave;
        this.valor = valor;
        this.descricao = descricao;
    }

    // Getters e Setters
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }

    public String obterValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}