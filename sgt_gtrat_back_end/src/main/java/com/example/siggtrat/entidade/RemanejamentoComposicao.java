package com.example.siggtrat.entidade;

import jakarta.persistence.*;

@Entity
@Table(name = "remanejamento_composicao", indexes = {
        @Index(name = "idx_rem_comp_remanejamento", columnList = "id_remanejamento"),
        @Index(name = "idx_rem_comp_recorte", columnList = "id_lotacao, id_jornada, id_cargo")
})
public class RemanejamentoComposicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_remanejamento_composicao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_remanejamento", nullable = false, updatable = false)
    private RemanejamentoContratual remanejamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_composicao_base", updatable = false)
    private ComposicaoContratual composicaoBase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false, updatable = false)
    private Lotacao lotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false, updatable = false)
    private Jornada jornada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo", nullable = false, updatable = false)
    private Cargo cargo;

    @Column(name = "lotacao_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String lotacaoNomeFotografia;

    @Column(name = "jornada_descricao_fotografia", nullable = false, updatable = false, length = 150)
    private String jornadaDescricaoFotografia;

    @Column(name = "cargo_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String cargoNomeFotografia;

    @Column(name = "quantidade_anterior", nullable = false, updatable = false)
    private Integer quantidadeAnterior;

    @Column(name = "variacao", nullable = false, updatable = false)
    private Integer variacao;

    @Column(name = "quantidade_resultante", nullable = false, updatable = false)
    private Integer quantidadeResultante;

    @PrePersist
    protected void validar() {
        if (remanejamento == null || lotacao == null || jornada == null || cargo == null
                || quantidadeAnterior == null || variacao == null || quantidadeResultante == null) {
            throw new IllegalStateException("Item de remanejamento incompleto.");
        }
        if (quantidadeAnterior < 0 || quantidadeResultante < 0
                || quantidadeAnterior + variacao != quantidadeResultante) {
            throw new IllegalStateException("Quantidades do remanejamento inconsistentes.");
        }
        if (lotacaoNomeFotografia == null) lotacaoNomeFotografia = lotacao.getNome();
        if (jornadaDescricaoFotografia == null) jornadaDescricaoFotografia = jornada.getDescricao();
        if (cargoNomeFotografia == null) cargoNomeFotografia = cargo.getNome();
    }

    public Long getId() { return id; }
    public RemanejamentoContratual getRemanejamento() { return remanejamento; }
    public void setRemanejamento(RemanejamentoContratual remanejamento) { this.remanejamento = remanejamento; }
    public ComposicaoContratual getComposicaoBase() { return composicaoBase; }
    public void setComposicaoBase(ComposicaoContratual composicaoBase) { this.composicaoBase = composicaoBase; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public String getLotacaoNomeFotografia() { return lotacaoNomeFotografia; }
    public void setLotacaoNomeFotografia(String valor) { this.lotacaoNomeFotografia = valor; }
    public String getJornadaDescricaoFotografia() { return jornadaDescricaoFotografia; }
    public void setJornadaDescricaoFotografia(String valor) { this.jornadaDescricaoFotografia = valor; }
    public String getCargoNomeFotografia() { return cargoNomeFotografia; }
    public void setCargoNomeFotografia(String valor) { this.cargoNomeFotografia = valor; }
    public Integer getQuantidadeAnterior() { return quantidadeAnterior; }
    public void setQuantidadeAnterior(Integer quantidadeAnterior) { this.quantidadeAnterior = quantidadeAnterior; }
    public Integer getVariacao() { return variacao; }
    public void setVariacao(Integer variacao) { this.variacao = variacao; }
    public Integer getQuantidadeResultante() { return quantidadeResultante; }
    public void setQuantidadeResultante(Integer quantidadeResultante) { this.quantidadeResultante = quantidadeResultante; }
}
