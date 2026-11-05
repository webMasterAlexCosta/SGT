package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** Fonte posterior da configuração introduzida pelo item exato de um ADI. */
@Entity
@Immutable
@Table(name = "prova_historica_aditamento_composicao", uniqueConstraints =
        @UniqueConstraint(name = "uk_prova_item_aditamento", columnNames = {"id_prova", "id_aditamento_composicao"}))
public class ProvaHistoricaAditamentoComposicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prova_historica_aditamento_composicao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prova", nullable = false, updatable = false)
    private ProvaHistoricaContrato prova;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aditamento_composicao", nullable = false, updatable = false)
    private AditamentoComposicao aditamentoComposicao;

    @Column(name = "lotacao_nome", length = 150, updatable = false)
    private String lotacaoNome;
    @Column(name = "jornada_descricao", length = 150, updatable = false)
    private String jornadaDescricao;
    @Column(name = "cargo_nome", length = 150, updatable = false)
    private String cargoNome;
    @Column(name = "lotacao_municipio", length = 180, updatable = false)
    private String lotacaoMunicipio;
    @Column(name = "municipio_comprovado", nullable = false, updatable = false)
    private boolean municipioComprovado;
    @Column(name = "jornada_semanal", length = 1000, updatable = false)
    private String jornadaSemanal;
    @Column(name = "jornada_comprovada", nullable = false, updatable = false)
    private boolean jornadaComprovada;

    protected ProvaHistoricaAditamentoComposicao() { }

    public ProvaHistoricaAditamentoComposicao(ProvaHistoricaContrato prova, AditamentoComposicao aditamentoComposicao,
            String lotacaoNome, String jornadaDescricao, String cargoNome, String lotacaoMunicipio,
            boolean municipioComprovado, String jornadaSemanal, boolean jornadaComprovada) {
        this.prova = prova;
        this.aditamentoComposicao = aditamentoComposicao;
        this.lotacaoNome = lotacaoNome;
        this.jornadaDescricao = jornadaDescricao;
        this.cargoNome = cargoNome;
        this.lotacaoMunicipio = lotacaoMunicipio;
        this.municipioComprovado = municipioComprovado;
        this.jornadaSemanal = jornadaSemanal;
        this.jornadaComprovada = jornadaComprovada;
    }

    public Long getId() { return id; }
    public ProvaHistoricaContrato getProva() { return prova; }
    public AditamentoComposicao getAditamentoComposicao() { return aditamentoComposicao; }
    public String getLotacaoNome() { return lotacaoNome; }
    public String getJornadaDescricao() { return jornadaDescricao; }
    public String getCargoNome() { return cargoNome; }
    public String getLotacaoMunicipio() { return lotacaoMunicipio; }
    public boolean getMunicipioComprovado() { return municipioComprovado; }
    public String getJornadaSemanal() { return jornadaSemanal; }
    public boolean getJornadaComprovada() { return jornadaComprovada; }
}
