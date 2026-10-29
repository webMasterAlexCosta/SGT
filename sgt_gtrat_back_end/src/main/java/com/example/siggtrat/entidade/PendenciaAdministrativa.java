package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dominio.pendencia.EstadoPendencia;
import com.example.siggtrat.dominio.pendencia.EscopoBloqueioPendencia;
import com.example.siggtrat.dominio.pendencia.TipoPendencia;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pendencia_administrativa", indexes = {
        @Index(name = "idx_pendencia_estado_tipo", columnList = "estado,tipo"),
        @Index(name = "idx_pendencia_contrato_estado", columnList = "contrato_id,estado"),
        @Index(name = "idx_pendencia_gestao_estado", columnList = "gestao_local_id,estado"),
        @Index(name = "idx_pendencia_se_estado", columnList = "superintendencia_id,estado"),
        @Index(name = "idx_pendencia_data_efetiva", columnList = "data_efetiva"),
        @Index(name = "idx_pendencia_origem", columnList = "origem_entidade,origem_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_pendencia_chave_origem", columnNames = "chave_origem")
})
public class PendenciaAdministrativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_origem", nullable = false, length = 180, updatable = false)
    private String chaveOrigem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoPendencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPendencia estado = EstadoPendencia.ABERTA;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @Column(name = "surgida_em", nullable = false, updatable = false)
    private Instant surgidaEm = Instant.now();

    @Column(name = "data_efetiva")
    private LocalDate dataEfetiva;

    @Column(name = "operacao_origem", nullable = false, length = 100)
    private String operacaoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_responsavel", nullable = false, length = 32)
    private PerfilAcesso perfilResponsavel = PerfilAcesso.GESTOR_LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "escopo_bloqueio", nullable = false, length = 40)
    private EscopoBloqueioPendencia escopoBloqueio;

    @Column(name = "contrato_id")
    private Long contratoId;
    @Column(name = "contrato_fotografia", length = 120)
    private String contratoFotografia;
    @Column(name = "pessoa_id")
    private Long pessoaId;
    @Column(name = "pessoa_fotografia", length = 180)
    private String pessoaFotografia;
    @Column(name = "vinculo_id")
    private Long vinculoId;
    @Column(name = "frequencia_id")
    private Long frequenciaId;
    @Column(name = "lotacao_id")
    private Long lotacaoId;
    @Column(name = "jornada_id")
    private Long jornadaId;
    @Column(name = "gestao_local_id")
    private Long gestaoLocalId;
    @Column(name = "superintendencia_id")
    private Long superintendenciaId;

    @Column(name = "origem_entidade", nullable = false, length = 80, updatable = false)
    private String origemEntidade;
    @Column(name = "origem_id", nullable = false, updatable = false)
    private Long origemId;

    @Column(name = "iniciada_em")
    private Instant iniciadaEm;
    @Column(name = "iniciada_por_id")
    private Long iniciadaPorId;
    @Column(name = "iniciada_por", length = 120)
    private String iniciadaPor;
    @Column(name = "resolvida_em")
    private Instant resolvidaEm;
    @Column(name = "resolvida_por_id")
    private Long resolvidaPorId;
    @Column(name = "resolvida_por", length = 120)
    private String resolvidaPor;
    @Column(name = "observacao_tratamento", length = 1000)
    private String observacaoTratamento;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    protected void validar() {
        if (chaveOrigem == null || chaveOrigem.isBlank() || tipo == null || estado == null
                || motivo == null || motivo.isBlank() || operacaoOrigem == null || operacaoOrigem.isBlank()
                || perfilResponsavel == null || escopoBloqueio == null
                || origemEntidade == null || origemEntidade.isBlank() || origemId == null) {
            throw new IllegalStateException("Pendência administrativa incompleta.");
        }
        if (surgidaEm == null) surgidaEm = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChaveOrigem() { return chaveOrigem; }
    public void setChaveOrigem(String chaveOrigem) { this.chaveOrigem = chaveOrigem; }
    public TipoPendencia getTipo() { return tipo; }
    public void setTipo(TipoPendencia tipo) { this.tipo = tipo; }
    public EstadoPendencia getEstado() { return estado; }
    public void setEstado(EstadoPendencia estado) { this.estado = estado; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Instant getSurgidaEm() { return surgidaEm; }
    public void setSurgidaEm(Instant surgidaEm) { this.surgidaEm = surgidaEm; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public String getOperacaoOrigem() { return operacaoOrigem; }
    public void setOperacaoOrigem(String operacaoOrigem) { this.operacaoOrigem = operacaoOrigem; }
    public PerfilAcesso getPerfilResponsavel() { return perfilResponsavel; }
    public void setPerfilResponsavel(PerfilAcesso perfilResponsavel) { this.perfilResponsavel = perfilResponsavel; }
    public EscopoBloqueioPendencia getEscopoBloqueio() { return escopoBloqueio; }
    public void setEscopoBloqueio(EscopoBloqueioPendencia escopoBloqueio) { this.escopoBloqueio = escopoBloqueio; }
    public Long getContratoId() { return contratoId; }
    public void setContratoId(Long contratoId) { this.contratoId = contratoId; }
    public String getContratoFotografia() { return contratoFotografia; }
    public void setContratoFotografia(String contratoFotografia) { this.contratoFotografia = contratoFotografia; }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public String getPessoaFotografia() { return pessoaFotografia; }
    public void setPessoaFotografia(String pessoaFotografia) { this.pessoaFotografia = pessoaFotografia; }
    public Long getVinculoId() { return vinculoId; }
    public void setVinculoId(Long vinculoId) { this.vinculoId = vinculoId; }
    public Long getFrequenciaId() { return frequenciaId; }
    public void setFrequenciaId(Long frequenciaId) { this.frequenciaId = frequenciaId; }
    public Long getLotacaoId() { return lotacaoId; }
    public void setLotacaoId(Long lotacaoId) { this.lotacaoId = lotacaoId; }
    public Long getJornadaId() { return jornadaId; }
    public void setJornadaId(Long jornadaId) { this.jornadaId = jornadaId; }
    public Long getGestaoLocalId() { return gestaoLocalId; }
    public void setGestaoLocalId(Long gestaoLocalId) { this.gestaoLocalId = gestaoLocalId; }
    public Long getSuperintendenciaId() { return superintendenciaId; }
    public void setSuperintendenciaId(Long superintendenciaId) { this.superintendenciaId = superintendenciaId; }
    public String getOrigemEntidade() { return origemEntidade; }
    public void setOrigemEntidade(String origemEntidade) { this.origemEntidade = origemEntidade; }
    public Long getOrigemId() { return origemId; }
    public void setOrigemId(Long origemId) { this.origemId = origemId; }
    public Instant getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(Instant iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public Long getIniciadaPorId() { return iniciadaPorId; }
    public void setIniciadaPorId(Long iniciadaPorId) { this.iniciadaPorId = iniciadaPorId; }
    public String getIniciadaPor() { return iniciadaPor; }
    public void setIniciadaPor(String iniciadaPor) { this.iniciadaPor = iniciadaPor; }
    public Instant getResolvidaEm() { return resolvidaEm; }
    public void setResolvidaEm(Instant resolvidaEm) { this.resolvidaEm = resolvidaEm; }
    public Long getResolvidaPorId() { return resolvidaPorId; }
    public void setResolvidaPorId(Long resolvidaPorId) { this.resolvidaPorId = resolvidaPorId; }
    public String getResolvidaPor() { return resolvidaPor; }
    public void setResolvidaPor(String resolvidaPor) { this.resolvidaPor = resolvidaPor; }
    public String getObservacaoTratamento() { return observacaoTratamento; }
    public void setObservacaoTratamento(String observacaoTratamento) { this.observacaoTratamento = observacaoTratamento; }
    public long getVersion() { return version; }
}
