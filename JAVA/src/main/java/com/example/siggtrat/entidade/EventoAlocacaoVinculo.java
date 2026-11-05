package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.alocacao.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Evento imutável da faixa de Alocação. As fotografias anterior e nova permitem
 * explicar uma movimentação sem reescrever a posição histórica.
 */
@Entity
@Table(name = "alocacao_vinculo_evento", indexes = {
        @Index(name = "idx_evt_alocacao_vinculo_efetiva", columnList = "id_vinculo_contratual, data_efetiva"),
        @Index(name = "idx_evt_alocacao_vinculo_registro", columnList = "id_vinculo_contratual, registrado_em")
})
public class EventoAlocacaoVinculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_alocacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vinculo_contratual", nullable = false, updatable = false)
    private VinculoContratual vinculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alocacao_nova", updatable = false)
    private AlocacaoVinculo alocacaoNova;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 48, updatable = false)
    private TipoEventoAlocacaoVinculo tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "relacao_precedencia", nullable = false, length = 24, updatable = false)
    private TipoRelacaoEventoAlocacao relacaoPrecedencia = TipoRelacaoEventoAlocacao.ORIGEM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento_precedente", updatable = false)
    private EventoAlocacaoVinculo eventoPrecedente;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "responsavel_id", updatable = false)
    private Long responsavelId;

    @Column(name = "responsavel_username", length = 120, updatable = false)
    private String responsavelUsername;

    @Column(name = "justificativa", length = 1000, updatable = false)
    private String justificativa;

    @Column(name = "evidencia", length = 500, updatable = false)
    private String evidencia;

    @Column(name = "autorizacao", length = 500, updatable = false)
    private String autorizacao;

    @Column(name = "inclusao_excedente", updatable = false)
    private Boolean inclusaoExcedente = false;

    @Column(name = "efetivo_previsto_excedente", updatable = false)
    private Integer efetivoPrevistoExcedente;

    @Column(name = "ocupacao_apos_inclusao_excedente", updatable = false)
    private Long ocupacaoAposInclusaoExcedente;

    @Column(name = "id_alocacao_anterior", updatable = false)
    private Long alocacaoAnteriorId;

    @Column(name = "inicio_anterior", updatable = false)
    private LocalDate inicioAnterior;

    @Column(name = "fim_anterior", updatable = false)
    private LocalDate fimAnterior;

    @Column(name = "estado_anterior", length = 24, updatable = false)
    private EstadoNegocialAlocacao estadoAnterior;

    @Column(name = "composicao_anterior_id", updatable = false)
    private Long composicaoAnteriorId;

    @Column(name = "lotacao_anterior_id", updatable = false)
    private Long lotacaoAnteriorId;

    @Column(name = "lotacao_anterior_nome", length = 180, updatable = false)
    private String lotacaoAnteriorNome;

    @Column(name = "jornada_anterior_id", updatable = false)
    private Long jornadaAnteriorId;

    @Column(name = "jornada_anterior_descricao", length = 180, updatable = false)
    private String jornadaAnteriorDescricao;

    @Column(name = "cargo_anterior_id", updatable = false)
    private Long cargoAnteriorId;

    @Column(name = "cargo_anterior_nome", length = 180, updatable = false)
    private String cargoAnteriorNome;

    @Column(name = "inicio_novo", updatable = false)
    private LocalDate inicioNovo;

    @Column(name = "fim_novo", updatable = false)
    private LocalDate fimNovo;

    @Column(name = "estado_novo", length = 24, updatable = false)
    private EstadoNegocialAlocacao estadoNovo;

    @Column(name = "composicao_nova_id", updatable = false)
    private Long composicaoNovaId;

    @Column(name = "lotacao_nova_id", updatable = false)
    private Long lotacaoNovaId;

    @Column(name = "lotacao_nova_nome", length = 180, updatable = false)
    private String lotacaoNovaNome;

    @Column(name = "jornada_nova_id", updatable = false)
    private Long jornadaNovaId;

    @Column(name = "jornada_nova_descricao", length = 180, updatable = false)
    private String jornadaNovaDescricao;

    @Column(name = "cargo_novo_id", updatable = false)
    private Long cargoNovoId;

    @Column(name = "cargo_novo_nome", length = 180, updatable = false)
    private String cargoNovoNome;

    @Column(name = "transferencia_gestao_id", updatable = false)
    private Long transferenciaGestaoId;

    @Column(name = "gestao_origem_id_foto", updatable = false)
    private Long gestaoOrigemIdFotografia;

    @Column(name = "gestao_origem_nome_foto", length = 180, updatable = false)
    private String gestaoOrigemNomeFotografia;

    @Column(name = "gestao_destino_id_foto", updatable = false)
    private Long gestaoDestinoIdFotografia;

    @Column(name = "gestao_destino_nome_foto", length = 180, updatable = false)
    private String gestaoDestinoNomeFotografia;

    @Column(name = "hash_anterior", length = 64, updatable = false)
    private String hashAnterior;

    @Column(name = "hash_evento", nullable = false, length = 64, updatable = false)
    private String hashEvento;

    @PrePersist
    protected void validar() {
        if (vinculo == null || tipoEvento == null || dataEfetiva == null || hashEvento == null || hashEvento.length() != 64) {
            throw new IllegalStateException("Evento de alocação incompleto ou sem hash SHA-256.");
        }
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        if (relacaoPrecedencia == null) relacaoPrecedencia = TipoRelacaoEventoAlocacao.ORIGEM;
    }

    public Long getId() { return id; }
    public VinculoContratual getVinculo() { return vinculo; }
    public void setVinculo(VinculoContratual vinculo) { this.vinculo = vinculo; }
    public AlocacaoVinculo getAlocacaoNova() { return alocacaoNova; }
    public void setAlocacaoNova(AlocacaoVinculo alocacaoNova) { this.alocacaoNova = alocacaoNova; }
    public TipoEventoAlocacaoVinculo getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoAlocacaoVinculo tipoEvento) { this.tipoEvento = tipoEvento; }
    public TipoRelacaoEventoAlocacao getRelacaoPrecedencia() { return relacaoPrecedencia; }
    public void setRelacaoPrecedencia(TipoRelacaoEventoAlocacao valor) { this.relacaoPrecedencia = valor; }
    public EventoAlocacaoVinculo getEventoPrecedente() { return eventoPrecedente; }
    public void setEventoPrecedente(EventoAlocacaoVinculo valor) { this.eventoPrecedente = valor; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate valor) { this.dataEfetiva = valor; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant valor) { this.registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(valor); }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long valor) { this.responsavelId = valor; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String valor) { this.responsavelUsername = valor; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String valor) { this.justificativa = valor; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String valor) { this.evidencia = valor; }
    public String getAutorizacao() { return autorizacao; }
    public void setAutorizacao(String valor) { this.autorizacao = valor; }
    public boolean isInclusaoExcedente() { return Boolean.TRUE.equals(inclusaoExcedente); }
    public void setInclusaoExcedente(boolean valor) { this.inclusaoExcedente = valor; }
    public Integer getEfetivoPrevistoExcedente() { return efetivoPrevistoExcedente; }
    public void setEfetivoPrevistoExcedente(Integer valor) { this.efetivoPrevistoExcedente = valor; }
    public Long getOcupacaoAposInclusaoExcedente() { return ocupacaoAposInclusaoExcedente; }
    public void setOcupacaoAposInclusaoExcedente(Long valor) { this.ocupacaoAposInclusaoExcedente = valor; }
    public Long getAlocacaoAnteriorId() { return alocacaoAnteriorId; }
    public void setAlocacaoAnteriorId(Long valor) { this.alocacaoAnteriorId = valor; }
    public LocalDate getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(LocalDate valor) { this.inicioAnterior = valor; }
    public LocalDate getFimAnterior() { return fimAnterior; }
    public void setFimAnterior(LocalDate valor) { this.fimAnterior = valor; }
    public EstadoNegocialAlocacao getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoNegocialAlocacao valor) { this.estadoAnterior = valor; }
    public Long getComposicaoAnteriorId() { return composicaoAnteriorId; }
    public void setComposicaoAnteriorId(Long valor) { this.composicaoAnteriorId = valor; }
    public Long getLotacaoAnteriorId() { return lotacaoAnteriorId; }
    public void setLotacaoAnteriorId(Long valor) { this.lotacaoAnteriorId = valor; }
    public String getLotacaoAnteriorNome() { return lotacaoAnteriorNome; }
    public void setLotacaoAnteriorNome(String valor) { this.lotacaoAnteriorNome = valor; }
    public Long getJornadaAnteriorId() { return jornadaAnteriorId; }
    public void setJornadaAnteriorId(Long valor) { this.jornadaAnteriorId = valor; }
    public String getJornadaAnteriorDescricao() { return jornadaAnteriorDescricao; }
    public void setJornadaAnteriorDescricao(String valor) { this.jornadaAnteriorDescricao = valor; }
    public Long getCargoAnteriorId() { return cargoAnteriorId; }
    public void setCargoAnteriorId(Long valor) { this.cargoAnteriorId = valor; }
    public String getCargoAnteriorNome() { return cargoAnteriorNome; }
    public void setCargoAnteriorNome(String valor) { this.cargoAnteriorNome = valor; }
    public LocalDate getInicioNovo() { return inicioNovo; }
    public void setInicioNovo(LocalDate valor) { this.inicioNovo = valor; }
    public LocalDate getFimNovo() { return fimNovo; }
    public void setFimNovo(LocalDate valor) { this.fimNovo = valor; }
    public EstadoNegocialAlocacao getEstadoNovo() { return estadoNovo; }
    public void setEstadoNovo(EstadoNegocialAlocacao valor) { this.estadoNovo = valor; }
    public Long getComposicaoNovaId() { return composicaoNovaId; }
    public void setComposicaoNovaId(Long valor) { this.composicaoNovaId = valor; }
    public Long getLotacaoNovaId() { return lotacaoNovaId; }
    public void setLotacaoNovaId(Long valor) { this.lotacaoNovaId = valor; }
    public String getLotacaoNovaNome() { return lotacaoNovaNome; }
    public void setLotacaoNovaNome(String valor) { this.lotacaoNovaNome = valor; }
    public Long getJornadaNovaId() { return jornadaNovaId; }
    public void setJornadaNovaId(Long valor) { this.jornadaNovaId = valor; }
    public String getJornadaNovaDescricao() { return jornadaNovaDescricao; }
    public void setJornadaNovaDescricao(String valor) { this.jornadaNovaDescricao = valor; }
    public Long getCargoNovoId() { return cargoNovoId; }
    public void setCargoNovoId(Long valor) { this.cargoNovoId = valor; }
    public String getCargoNovoNome() { return cargoNovoNome; }
    public void setCargoNovoNome(String valor) { this.cargoNovoNome = valor; }
    public Long getTransferenciaGestaoId() { return transferenciaGestaoId; }
    public void setTransferenciaGestaoId(Long valor) { this.transferenciaGestaoId = valor; }
    public Long getGestaoOrigemIdFotografia() { return gestaoOrigemIdFotografia; }
    public void setGestaoOrigemIdFotografia(Long valor) { this.gestaoOrigemIdFotografia = valor; }
    public String getGestaoOrigemNomeFotografia() { return gestaoOrigemNomeFotografia; }
    public void setGestaoOrigemNomeFotografia(String valor) { this.gestaoOrigemNomeFotografia = valor; }
    public Long getGestaoDestinoIdFotografia() { return gestaoDestinoIdFotografia; }
    public void setGestaoDestinoIdFotografia(Long valor) { this.gestaoDestinoIdFotografia = valor; }
    public String getGestaoDestinoNomeFotografia() { return gestaoDestinoNomeFotografia; }
    public void setGestaoDestinoNomeFotografia(String valor) { this.gestaoDestinoNomeFotografia = valor; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String valor) { this.hashAnterior = valor; }
    public String getHashEvento() { return hashEvento; }
    public void setHashEvento(String valor) { this.hashEvento = valor; }
}
