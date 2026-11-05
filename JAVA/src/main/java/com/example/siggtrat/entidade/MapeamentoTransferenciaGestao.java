package com.example.siggtrat.entidade;

import jakarta.persistence.*;

/** Mapeia, de modo explícito, um recorte antigo para os catálogos da nova Gestão. */
@Entity
@Table(name = "transferencia_gestao_mapeamento", uniqueConstraints = @UniqueConstraint(
        name = "uk_transf_gestao_origem", columnNames = {"id_transferencia_gestao", "id_lotacao_origem", "id_jornada_origem", "id_cargo_origem"}
), indexes = {
        @Index(name = "idx_transf_map_transferencia", columnList = "id_transferencia_gestao"),
        @Index(name = "idx_transf_map_destino", columnList = "id_lotacao_destino, id_jornada_destino, id_cargo_destino")
})
public class MapeamentoTransferenciaGestao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mapeamento_transferencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_transferencia_gestao", nullable = false, updatable = false)
    private TransferenciaGestaoContrato transferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_composicao_origem", updatable = false)
    private ComposicaoContratual composicaoOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao_origem", nullable = false, updatable = false)
    private Lotacao lotacaoOrigem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada_origem", nullable = false, updatable = false)
    private Jornada jornadaOrigem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo_origem", nullable = false, updatable = false)
    private Cargo cargoOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao_destino", nullable = false, updatable = false)
    private Lotacao lotacaoDestino;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada_destino", nullable = false, updatable = false)
    private Jornada jornadaDestino;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo_destino", nullable = false, updatable = false)
    private Cargo cargoDestino;

    @Column(name = "lotacao_origem_nome_fotografia", updatable = false, length = 150)
    private String lotacaoOrigemNomeFotografia;
    @Column(name = "jornada_origem_descricao_fotografia", updatable = false, length = 150)
    private String jornadaOrigemDescricaoFotografia;
    @Column(name = "cargo_origem_nome_fotografia", updatable = false, length = 150)
    private String cargoOrigemNomeFotografia;

    @Column(name = "lotacao_destino_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String lotacaoDestinoNomeFotografia;

    @Column(name = "jornada_destino_descricao_fotografia", nullable = false, updatable = false, length = 150)
    private String jornadaDestinoDescricaoFotografia;

    @Column(name = "cargo_destino_nome_fotografia", nullable = false, updatable = false, length = 150)
    private String cargoDestinoNomeFotografia;

    @Column(name = "lotacao_destino_municipio_fotografia", updatable = false, length = 180)
    private String lotacaoDestinoMunicipioFotografia;

    @Column(name = "jornada_destino_semanal_fotografia", updatable = false, length = 1000)
    private String jornadaDestinoSemanalFotografia;

    @PrePersist
    protected void validar() {
        if (transferencia == null || lotacaoOrigem == null || jornadaOrigem == null || cargoOrigem == null
                || lotacaoDestino == null || jornadaDestino == null || cargoDestino == null) {
            throw new IllegalStateException("Mapeamento de transferência de Gestão incompleto.");
        }
        if (lotacaoDestinoNomeFotografia == null) lotacaoDestinoNomeFotografia = lotacaoDestino.getNome();
        // A origem vem da fotografia já publicada. Se faltou no legado, continua
        // desconhecida; o cadastro atual só fundamenta a captura do novo destino.
        if (jornadaDestinoDescricaoFotografia == null) jornadaDestinoDescricaoFotografia = jornadaDestino.getDescricao();
        if (cargoDestinoNomeFotografia == null) cargoDestinoNomeFotografia = cargoDestino.getNome();
        lotacaoDestinoMunicipioFotografia = lotacaoDestino.getMunicipio();
        jornadaDestinoSemanalFotografia = com.example.siggtrat.utilitario.FotografiaJornada.capturar(jornadaDestino);
    }

    public Long getId() { return id; }
    public TransferenciaGestaoContrato getTransferencia() { return transferencia; }
    public void setTransferencia(TransferenciaGestaoContrato valor) { this.transferencia = valor; }
    public ComposicaoContratual getComposicaoOrigem() { return composicaoOrigem; }
    public void setComposicaoOrigem(ComposicaoContratual valor) { this.composicaoOrigem = valor; }
    public Lotacao getLotacaoOrigem() { return lotacaoOrigem; }
    public void setLotacaoOrigem(Lotacao valor) { this.lotacaoOrigem = valor; }
    public Jornada getJornadaOrigem() { return jornadaOrigem; }
    public void setJornadaOrigem(Jornada valor) { this.jornadaOrigem = valor; }
    public Cargo getCargoOrigem() { return cargoOrigem; }
    public void setCargoOrigem(Cargo valor) { this.cargoOrigem = valor; }
    public Lotacao getLotacaoDestino() { return lotacaoDestino; }
    public void setLotacaoDestino(Lotacao valor) { this.lotacaoDestino = valor; }
    public Jornada getJornadaDestino() { return jornadaDestino; }
    public void setJornadaDestino(Jornada valor) { this.jornadaDestino = valor; }
    public Cargo getCargoDestino() { return cargoDestino; }
    public void setCargoDestino(Cargo valor) { this.cargoDestino = valor; }
    public String getLotacaoDestinoNomeFotografia() { return lotacaoDestinoNomeFotografia; }
    public String getLotacaoOrigemNomeFotografia() { return lotacaoOrigemNomeFotografia; }
    public void setLotacaoOrigemNomeFotografia(String valor) { this.lotacaoOrigemNomeFotografia = valor; }
    public String getJornadaOrigemDescricaoFotografia() { return jornadaOrigemDescricaoFotografia; }
    public void setJornadaOrigemDescricaoFotografia(String valor) { this.jornadaOrigemDescricaoFotografia = valor; }
    public String getCargoOrigemNomeFotografia() { return cargoOrigemNomeFotografia; }
    public void setCargoOrigemNomeFotografia(String valor) { this.cargoOrigemNomeFotografia = valor; }
    public void setLotacaoDestinoNomeFotografia(String valor) { this.lotacaoDestinoNomeFotografia = valor; }
    public String getJornadaDestinoDescricaoFotografia() { return jornadaDestinoDescricaoFotografia; }
    public void setJornadaDestinoDescricaoFotografia(String valor) { this.jornadaDestinoDescricaoFotografia = valor; }
    public String getCargoDestinoNomeFotografia() { return cargoDestinoNomeFotografia; }
    public void setCargoDestinoNomeFotografia(String valor) { this.cargoDestinoNomeFotografia = valor; }
    public String getLotacaoDestinoMunicipioFotografia() { return lotacaoDestinoMunicipioFotografia; }
    public String getJornadaDestinoSemanalFotografia() { return jornadaDestinoSemanalFotografia; }
}
