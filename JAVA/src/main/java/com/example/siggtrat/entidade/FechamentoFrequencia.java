package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Check(constraints = "status IN ('PENDENTE','PROCESSANDO','CONCLUIDO','ERRO','CANCELADO')")
@Table(
        name = "fechamento_frequencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fechamento_frequencia_escopo",
                columnNames = "chave_escopo"
        ),
        indexes = {
                @Index(name = "idx_fechamento_frequencia_status", columnList = "status, atualizado_em"),
                @Index(name = "idx_fechamento_frequencia_data_lotacao_jornada",
                        columnList = "data_referencia, id_lotacao, id_jornada"),
                @Index(name = "idx_fechamento_frequencia_usuario", columnList = "id_usuario_criador")
        }
)
public class FechamentoFrequencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_escopo", nullable = false, length = 64)
    private String chaveEscopo;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false)
    private Lotacao lotacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    private Jornada jornada;

    /** IDs ordenados e separados por vírgula. Vazio representa todos os contratos do escopo. */
    @Column(name = "contrato_ids", columnDefinition = "TEXT")
    private String contratoIds;

    @Column(name = "justificativa_retificacao", length = 1000)
    private String justificativaRetificacao;

    @Column(name = "chave_idempotencia", length = 120)
    private String chaveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FechamentoFrequenciaStatus status = FechamentoFrequenciaStatus.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_criador", nullable = false)
    private UsuarioAplicacao usuarioCriador;

    @Column(name = "total_previsto", nullable = false)
    private long totalPrevisto;

    @Column(name = "total_processado", nullable = false)
    private long totalProcessado;

    @Column(name = "total_inserido", nullable = false)
    private long totalInserido;

    @Column(name = "total_atualizado", nullable = false)
    private long totalAtualizado;

    @Column(name = "total_preservado", nullable = false)
    private long totalPreservado;

    @Column(name = "total_alteracoes", nullable = false)
    private long totalAlteracoes;

    /** Quando true, o trabalho percorre somente as alterações explícitas de um fechamento já concluído. */
    @Column(name = "somente_alteracoes", nullable = false)
    private boolean somenteAlteracoes;

    @Column(name = "ultimo_empregado_id_processado")
    private Long ultimoEmpregadoIdProcessado;

    @Column(name = "tentativa", nullable = false)
    private int tentativa = 1;

    @Column(name = "cancelamento_solicitado", nullable = false)
    private boolean cancelamentoSolicitado;

    @Column(name = "mensagem", length = 500)
    private String mensagem;

    @Column(name = "detalhe_erro", columnDefinition = "TEXT")
    private String detalheErro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "iniciado_em")
    private LocalDateTime iniciadoEm;

    @Column(name = "heartbeat_em")
    private LocalDateTime heartbeatEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void antesPersistir() {
        LocalDateTime agora = agora();
        if (criadoEm == null) criadoEm = agora;
        atualizadoEm = agora;
        if (status == null) status = FechamentoFrequenciaStatus.PENDENTE;
    }

    @PreUpdate
    protected void antesAtualizar() {
        atualizadoEm = agora();
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChaveEscopo() { return chaveEscopo; }
    public void setChaveEscopo(String chaveEscopo) { this.chaveEscopo = chaveEscopo; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public String getContratoIds() { return contratoIds; }
    public void setContratoIds(String contratoIds) { this.contratoIds = contratoIds; }
    public String getJustificativaRetificacao() { return justificativaRetificacao; }
    public void setJustificativaRetificacao(String valor) { this.justificativaRetificacao = valor; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String valor) { this.chaveIdempotencia = valor; }
    public FechamentoFrequenciaStatus getStatus() { return status; }
    public void setStatus(FechamentoFrequenciaStatus status) { this.status = status; }
    public UsuarioAplicacao getUsuarioCriador() { return usuarioCriador; }
    public void setUsuarioCriador(UsuarioAplicacao usuarioCriador) { this.usuarioCriador = usuarioCriador; }
    public long getTotalPrevisto() { return totalPrevisto; }
    public void setTotalPrevisto(long totalPrevisto) { this.totalPrevisto = totalPrevisto; }
    public long getTotalProcessado() { return totalProcessado; }
    public void setTotalProcessado(long totalProcessado) { this.totalProcessado = totalProcessado; }
    public long getTotalInserido() { return totalInserido; }
    public void setTotalInserido(long totalInserido) { this.totalInserido = totalInserido; }
    public long getTotalAtualizado() { return totalAtualizado; }
    public void setTotalAtualizado(long totalAtualizado) { this.totalAtualizado = totalAtualizado; }
    public long getTotalPreservado() { return totalPreservado; }
    public void setTotalPreservado(long totalPreservado) { this.totalPreservado = totalPreservado; }
    public long getTotalAlteracoes() { return totalAlteracoes; }
    public void setTotalAlteracoes(long totalAlteracoes) { this.totalAlteracoes = totalAlteracoes; }
    public boolean isSomenteAlteracoes() { return somenteAlteracoes; }
    public void setSomenteAlteracoes(boolean somenteAlteracoes) { this.somenteAlteracoes = somenteAlteracoes; }
    public Long getUltimoEmpregadoIdProcessado() { return ultimoEmpregadoIdProcessado; }
    public void setUltimoEmpregadoIdProcessado(Long ultimoEmpregadoIdProcessado) { this.ultimoEmpregadoIdProcessado = ultimoEmpregadoIdProcessado; }
    public int getTentativa() { return tentativa; }
    public void setTentativa(int tentativa) { this.tentativa = tentativa; }
    public boolean isCancelamentoSolicitado() { return cancelamentoSolicitado; }
    public void setCancelamentoSolicitado(boolean cancelamentoSolicitado) { this.cancelamentoSolicitado = cancelamentoSolicitado; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public String getDetalheErro() { return detalheErro; }
    public void setDetalheErro(String detalheErro) { this.detalheErro = detalheErro; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public LocalDateTime getHeartbeatEm() { return heartbeatEm; }
    public void setHeartbeatEm(LocalDateTime heartbeatEm) { this.heartbeatEm = heartbeatEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
