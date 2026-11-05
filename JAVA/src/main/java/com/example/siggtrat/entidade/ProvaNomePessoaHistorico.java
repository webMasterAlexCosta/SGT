package com.example.siggtrat.entidade;
import jakarta.persistence.*;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
@Entity
@Table(name = "prova_nome_pessoa_historico", indexes = {
 @Index(name = "idx_prova_nome_pessoa_historico_periodo", columnList = "pessoa_id,vinculo_id,data_inicio_efetiva,data_fim_efetiva,registrado_em"),
 @Index(name = "idx_prova_nome_pessoa_historico_anterior", columnList = "prova_anterior_id"),
 @Index(name = "idx_prova_nome_pessoa_historico_documento", columnList = "documento_id")},
 uniqueConstraints = @UniqueConstraint(name = "uk_prova_nome_pessoa_historico_chave", columnNames = {"contrato_id", "responsavel_id", "chave_idempotencia"}))
public class ProvaNomePessoaHistorico extends ProvaHistoricaBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pessoa_id", nullable = false, updatable = false)
    private Pessoa pessoa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vinculo_id", nullable = false, updatable = false)
    private VinculoContratual vinculo;
    @Column(nullable = false, length = 180, updatable = false)
    private String nomeComprovado;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prova_anterior_id", updatable = false)
    private ProvaNomePessoaHistorico provaAnterior;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "evento_origem_retificado_id", updatable = false)
    private EventoVinculoContratual eventoOrigemRetificado;

    public Pessoa getPessoa() { return pessoa; }
    public void setPessoa(Pessoa valor) { this.pessoa = valor; }
    public VinculoContratual getVinculo() { return vinculo; }
    public void setVinculo(VinculoContratual valor) { this.vinculo = valor; }
    public String getNomeComprovado() { return nomeComprovado; }
    public void setNomeComprovado(String valor) { this.nomeComprovado = valor; }
    public ProvaNomePessoaHistorico getProvaAnterior() { return provaAnterior; }
    public void setProvaAnterior(ProvaNomePessoaHistorico valor) { this.provaAnterior = valor; }
    public EventoVinculoContratual getEventoOrigemRetificado() { return eventoOrigemRetificado; }
    public void setEventoOrigemRetificado(EventoVinculoContratual valor) { this.eventoOrigemRetificado = valor; }
}
