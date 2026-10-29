package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.vinculo.TipoEventoVinculoContratual;
import com.example.siggtrat.dominio.vinculo.TipoRelacaoEventoVinculo;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/** Evento imutável, bitemporal e encadeado do vínculo. */
@Entity
@Table(name = "vinculo_contratual_evento", indexes = {
        @Index(name = "idx_evento_vinculo_efetiva", columnList = "id_vinculo_contratual, data_efetiva"),
        @Index(name = "idx_evento_vinculo_conhecimento", columnList = "id_vinculo_contratual, registrado_em")
})
public class EventoVinculoContratual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_vinculo")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vinculo_contratual", nullable = false, updatable = false)
    private VinculoContratual vinculo;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 64, updatable = false)
    private TipoEventoVinculoContratual tipoEvento;
    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm = Instant.now();
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento_precedente", updatable = false)
    private EventoVinculoContratual eventoPrecedente;
    @Enumerated(EnumType.STRING)
    @Column(name = "relacao_precedencia", length = 20, updatable = false)
    private TipoRelacaoEventoVinculo relacaoPrecedencia = TipoRelacaoEventoVinculo.ORIGEM;

    @Column(name = "inicio_anterior", updatable = false)
    private LocalDate inicioAnterior;
    @Column(name = "inicio_novo", updatable = false)
    private LocalDate inicioNovo;
    @Column(name = "fim_anterior", updatable = false)
    private LocalDate fimAnterior;
    @Column(name = "fim_novo", updatable = false)
    private LocalDate fimNovo;
    @Column(name = "matricula_anterior", length = 30, updatable = false)
    private String matriculaAnterior;
    @Column(name = "matricula_nova", length = 30, updatable = false)
    private String matriculaNova;

    @Column(name = "inclusao_excedente", nullable = false, updatable = false)
    private boolean inclusaoExcedente;
    @Column(name = "composicao_excedente_id", updatable = false)
    private Long composicaoExcedenteId;
    @Column(name = "lotacao_excedente_id", updatable = false)
    private Long lotacaoExcedenteId;
    @Column(name = "jornada_excedente_id", updatable = false)
    private Long jornadaExcedenteId;
    @Column(name = "cargo_excedente_id", updatable = false)
    private Long cargoExcedenteId;
    @Column(name = "efetivo_previsto_excedente", updatable = false)
    private Integer efetivoPrevistoExcedente;
    @Column(name = "ocupacao_apos_inclusao_excedente", updatable = false)
    private Long ocupacaoAposInclusaoExcedente;

    @Column(name = "pessoa_nome_foto", nullable = false, length = 180, updatable = false)
    private String pessoaNomeFotografia;
    @Column(name = "pessoa_cpf_foto", nullable = false, length = 11, updatable = false)
    private String pessoaCpfFotografia;
    @Column(name = "contrato_numero_foto", length = 50, updatable = false)
    private String contratoNumeroFotografia;
    @Column(name = "contrato_ano_foto", updatable = false)
    private Integer contratoAnoFotografia;
    @Column(name = "gestao_local_id_foto", updatable = false)
    private Long gestaoLocalIdFotografia;
    @Column(name = "gestao_local_nome_foto", length = 180, updatable = false)
    private String gestaoLocalNomeFotografia;

    @Column(name = "hash_anterior", length = 64, updatable = false)
    private String hashAnterior;
    @Column(name = "hash_evento", nullable = false, length = 64, updatable = false)
    private String hashEvento;

    @PrePersist
    protected void validar() {
        if (vinculo == null || tipoEvento == null || dataEfetiva == null) throw new IllegalStateException("Evento de vínculo incompleto.");
        if (registradoEm == null) registradoEm = Instant.now();
        if (relacaoPrecedencia == null) relacaoPrecedencia = TipoRelacaoEventoVinculo.ORIGEM;
        if (hashEvento == null || hashEvento.length() != 64) throw new IllegalStateException("Hash SHA-256 do evento é obrigatório.");
        if (pessoaNomeFotografia == null || pessoaCpfFotografia == null) throw new IllegalStateException("Fotografia da identidade é obrigatória.");
    }

    public Long getId() { return id; }
    public VinculoContratual getVinculo() { return vinculo; }
    public void setVinculo(VinculoContratual vinculo) { this.vinculo = vinculo; }
    public TipoEventoVinculoContratual getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoVinculoContratual tipoEvento) { this.tipoEvento = tipoEvento; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) { this.registradoEm = registradoEm; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public String getEvidencia() { return evidencia; }
    public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    public String getAutorizacao() { return autorizacao; }
    public void setAutorizacao(String autorizacao) { this.autorizacao = autorizacao; }
    public EventoVinculoContratual getEventoPrecedente() { return eventoPrecedente; }
    public void setEventoPrecedente(EventoVinculoContratual eventoPrecedente) { this.eventoPrecedente = eventoPrecedente; }
    public TipoRelacaoEventoVinculo getRelacaoPrecedencia() { return relacaoPrecedencia; }
    public void setRelacaoPrecedencia(TipoRelacaoEventoVinculo relacaoPrecedencia) { this.relacaoPrecedencia = relacaoPrecedencia; }
    public LocalDate getInicioAnterior() { return inicioAnterior; }
    public void setInicioAnterior(LocalDate valor) { this.inicioAnterior = valor; }
    public LocalDate getInicioNovo() { return inicioNovo; }
    public void setInicioNovo(LocalDate valor) { this.inicioNovo = valor; }
    public LocalDate getFimAnterior() { return fimAnterior; }
    public void setFimAnterior(LocalDate valor) { this.fimAnterior = valor; }
    public LocalDate getFimNovo() { return fimNovo; }
    public void setFimNovo(LocalDate valor) { this.fimNovo = valor; }
    public String getMatriculaAnterior() { return matriculaAnterior; }
    public void setMatriculaAnterior(String valor) { this.matriculaAnterior = valor; }
    public String getMatriculaNova() { return matriculaNova; }
    public void setMatriculaNova(String valor) { this.matriculaNova = valor; }
    public boolean isInclusaoExcedente() { return inclusaoExcedente; }
    public void setInclusaoExcedente(boolean valor) { this.inclusaoExcedente = valor; }
    public Long getComposicaoExcedenteId() { return composicaoExcedenteId; }
    public void setComposicaoExcedenteId(Long valor) { this.composicaoExcedenteId = valor; }
    public Long getLotacaoExcedenteId() { return lotacaoExcedenteId; }
    public void setLotacaoExcedenteId(Long valor) { this.lotacaoExcedenteId = valor; }
    public Long getJornadaExcedenteId() { return jornadaExcedenteId; }
    public void setJornadaExcedenteId(Long valor) { this.jornadaExcedenteId = valor; }
    public Long getCargoExcedenteId() { return cargoExcedenteId; }
    public void setCargoExcedenteId(Long valor) { this.cargoExcedenteId = valor; }
    public Integer getEfetivoPrevistoExcedente() { return efetivoPrevistoExcedente; }
    public void setEfetivoPrevistoExcedente(Integer valor) { this.efetivoPrevistoExcedente = valor; }
    public Long getOcupacaoAposInclusaoExcedente() { return ocupacaoAposInclusaoExcedente; }
    public void setOcupacaoAposInclusaoExcedente(Long valor) { this.ocupacaoAposInclusaoExcedente = valor; }
    public String getPessoaNomeFotografia() { return pessoaNomeFotografia; }
    public void setPessoaNomeFotografia(String valor) { this.pessoaNomeFotografia = valor; }
    public String getPessoaCpfFotografia() { return pessoaCpfFotografia; }
    public void setPessoaCpfFotografia(String valor) { this.pessoaCpfFotografia = valor; }
    public String getContratoNumeroFotografia() { return contratoNumeroFotografia; }
    public void setContratoNumeroFotografia(String valor) { this.contratoNumeroFotografia = valor; }
    public Integer getContratoAnoFotografia() { return contratoAnoFotografia; }
    public void setContratoAnoFotografia(Integer valor) { this.contratoAnoFotografia = valor; }
    public Long getGestaoLocalIdFotografia() { return gestaoLocalIdFotografia; }
    public void setGestaoLocalIdFotografia(Long valor) { this.gestaoLocalIdFotografia = valor; }
    public String getGestaoLocalNomeFotografia() { return gestaoLocalNomeFotografia; }
    public void setGestaoLocalNomeFotografia(String valor) { this.gestaoLocalNomeFotografia = valor; }
    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }
    public String getHashEvento() { return hashEvento; }
    public void setHashEvento(String hashEvento) { this.hashEvento = hashEvento; }
}
