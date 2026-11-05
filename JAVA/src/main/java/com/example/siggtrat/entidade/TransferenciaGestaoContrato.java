package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ato excepcional que altera a responsabilidade de Gestão Local sem apagar o
 * contexto anterior do contrato, suas composições ou suas alocações.
 */
@Entity
@Table(name = "transferencia_gestao_contrato", indexes = {
        @Index(name = "idx_transf_gestao_contrato_data", columnList = "id_contrato, data_efetiva"),
        @Index(name = "idx_transf_gestao_evento", columnList = "id_evento_ciclo_vida")
})
public class TransferenciaGestaoContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transferencia_gestao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato", nullable = false, updatable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evento_ciclo_vida", nullable = false, updatable = false)
    private EventoCicloVidaContrato evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_origem", nullable = false, updatable = false)
    private GestaoLocal gestaoOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_destino", nullable = false, updatable = false)
    private GestaoLocal gestaoDestino;

    @Column(name = "gestao_origem_nome_fotografia", length = 180, updatable = false)
    private String gestaoOrigemNomeFotografia;
    @Column(name = "gestao_destino_nome_fotografia", length = 180, updatable = false)
    private String gestaoDestinoNomeFotografia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_contratacao_destino", updatable = false)
    private TipoContratacao tipoContratacaoDestino;
    @Column(name = "tipo_destino_descricao_fotografia", length = 160, updatable = false)
    private String tipoDestinoDescricaoFotografia;

    @Column(name = "data_efetiva", nullable = false, updatable = false)
    private LocalDate dataEfetiva;

    @Column(name = "motivo", nullable = false, length = 1000, updatable = false)
    private String motivo;

    @Column(name = "referencia_sei", nullable = false, length = 120, updatable = false)
    private String referenciaSei;

    @Column(name = "autorizacao_administrativa", nullable = false, length = 500, updatable = false)
    private String autorizacaoAdministrativa;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "responsavel_id", updatable = false)
    private Long responsavelId;

    @Column(name = "responsavel_username", length = 120, updatable = false)
    private String responsavelUsername;

    @OneToMany(mappedBy = "transferencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MapeamentoTransferenciaGestao> mapeamentos = new ArrayList<>();

    @PrePersist
    protected void validar() {
        if (contrato == null || evento == null || gestaoOrigem == null || gestaoDestino == null || dataEfetiva == null
                || vazio(motivo) || vazio(referenciaSei) || vazio(autorizacaoAdministrativa)) {
            throw new IllegalStateException("Transferência excepcional de Gestão Local incompleta.");
        }
        if (gestaoOrigem.getId() != null && gestaoOrigem.getId().equals(gestaoDestino.getId())) {
            throw new IllegalStateException("A Gestão Local de destino deve ser diferente da origem.");
        }
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        // A origem vem da posição documental anterior. Ausência de nome nessa
        // fonte permanece desconhecida; o mestre atual não preenche o passado.
        if (gestaoDestinoNomeFotografia == null) gestaoDestinoNomeFotografia = gestaoDestino.getNome();
        if (tipoDestinoDescricaoFotografia == null && tipoContratacaoDestino != null)
            tipoDestinoDescricaoFotografia = tipoContratacaoDestino.getDescricao();
    }

    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }

    public Long getId() { return id; }
    public Contrato getContrato() { return contrato; }
    public void setContrato(Contrato valor) { this.contrato = valor; }
    public EventoCicloVidaContrato getEvento() { return evento; }
    public void setEvento(EventoCicloVidaContrato valor) { this.evento = valor; }
    public GestaoLocal getGestaoOrigem() { return gestaoOrigem; }
    public void setGestaoOrigem(GestaoLocal valor) { this.gestaoOrigem = valor; }
    public GestaoLocal getGestaoDestino() { return gestaoDestino; }
    public void setGestaoDestino(GestaoLocal valor) { this.gestaoDestino = valor; }
    public String getGestaoOrigemNomeFotografia() { return gestaoOrigemNomeFotografia; }
    public void setGestaoOrigemNomeFotografia(String valor) { this.gestaoOrigemNomeFotografia = valor; }
    public String getGestaoDestinoNomeFotografia() { return gestaoDestinoNomeFotografia; }
    public void setGestaoDestinoNomeFotografia(String valor) { this.gestaoDestinoNomeFotografia = valor; }
    public TipoContratacao getTipoContratacaoDestino() { return tipoContratacaoDestino; }
    public void setTipoContratacaoDestino(TipoContratacao valor) { this.tipoContratacaoDestino = valor; }
    public String getTipoDestinoDescricaoFotografia() { return tipoDestinoDescricaoFotografia; }
    public void setTipoDestinoDescricaoFotografia(String valor) { this.tipoDestinoDescricaoFotografia = valor; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate valor) { this.dataEfetiva = valor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo = valor; }
    public String getReferenciaSei() { return referenciaSei; }
    public void setReferenciaSei(String valor) { this.referenciaSei = valor; }
    public String getAutorizacaoAdministrativa() { return autorizacaoAdministrativa; }
    public void setAutorizacaoAdministrativa(String valor) { this.autorizacaoAdministrativa = valor; }
    public Instant getRegistradoEm() { return registradoEm; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long valor) { this.responsavelId = valor; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String valor) { this.responsavelUsername = valor; }
    public List<MapeamentoTransferenciaGestao> getMapeamentos() { return mapeamentos; }
    public void setMapeamentos(List<MapeamentoTransferenciaGestao> itens) {
        mapeamentos.clear();
        if (itens != null) {
            for (MapeamentoTransferenciaGestao item : itens) {
                item.setTransferencia(this);
                mapeamentos.add(item);
            }
        }
    }
}
