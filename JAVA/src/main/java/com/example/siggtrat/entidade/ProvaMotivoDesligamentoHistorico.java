package com.example.siggtrat.entidade;
import jakarta.persistence.*;
import com.example.siggtrat.dominio.vinculo.CategoriaTerminoVinculo;
@Entity
@Table(name = "prova_motivo_desligamento_historico", indexes = {
 @Index(name = "idx_prova_motivo_desligamento_historico_periodo", columnList = "contrato_id,motivo_desligamento_id,data_inicio_efetiva,data_fim_efetiva,registrado_em"),
 @Index(name = "idx_prova_motivo_desligamento_historico_anterior", columnList = "prova_anterior_id"),
 @Index(name = "idx_prova_motivo_desligamento_historico_documento", columnList = "documento_id")},
 uniqueConstraints = @UniqueConstraint(name = "uk_prova_motivo_desligamento_historico_chave", columnNames = {"contrato_id", "responsavel_id", "chave_idempotencia"}))
public class ProvaMotivoDesligamentoHistorico extends ProvaHistoricaBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "motivo_desligamento_id", nullable = false, updatable = false)
    private MotivoDesligamento motivoDesligamento;
    @Column(nullable = false, length = 160, updatable = false)
    private String descricaoComprovada;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 48, updatable = false)
    private CategoriaTerminoVinculo categoriaComprovada;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prova_anterior_id", updatable = false)
    private ProvaMotivoDesligamentoHistorico provaAnterior;

    public MotivoDesligamento getMotivoDesligamento() { return motivoDesligamento; }
    public void setMotivoDesligamento(MotivoDesligamento valor) { this.motivoDesligamento = valor; }
    public String getDescricaoComprovada() { return descricaoComprovada; }
    public void setDescricaoComprovada(String valor) { this.descricaoComprovada = valor; }
    public CategoriaTerminoVinculo getCategoriaComprovada() { return categoriaComprovada; }
    public void setCategoriaComprovada(CategoriaTerminoVinculo valor) { this.categoriaComprovada = valor; }
    public ProvaMotivoDesligamentoHistorico getProvaAnterior() { return provaAnterior; }
    public void setProvaAnterior(ProvaMotivoDesligamentoHistorico valor) { this.provaAnterior = valor; }
}
