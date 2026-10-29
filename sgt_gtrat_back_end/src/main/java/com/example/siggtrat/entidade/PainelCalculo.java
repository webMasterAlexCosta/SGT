package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Check(constraints = "status IN ('PENDENTE','PROCESSANDO','CONCLUIDO','ERRO')")
@Table(
        name = "painel_calculo",
        uniqueConstraints = @UniqueConstraint(name = "uk_painel_calculo_chave", columnNames = "chave_consulta"),
        indexes = {
                @Index(name = "idx_painel_calculo_status_atualizado", columnList = "status, atualizado_em"),
                @Index(name = "idx_painel_calculo_escopo_referencia",
                        columnList = "nivel_escopo, id_superintendencia, id_gestao_local, data_referencia, periodo_dias")
        }
)
public class PainelCalculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_consulta", nullable = false, length = 64)
    private String chaveConsulta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PainelCalculoStatus status = PainelCalculoStatus.PENDENTE;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    @Column(name = "periodo_dias", nullable = false)
    private int periodoDias;

    @Column(name = "nivel_escopo", nullable = false, length = 30)
    private String nivelEscopo;

    @Column(name = "nome_escopo", nullable = false, length = 180)
    private String nomeEscopo;

    @Column(name = "id_superintendencia")
    private Long superintendenciaId;

    @Column(name = "id_gestao_local")
    private Long gestaoLocalId;

    @Column(name = "permite_detalhamento", nullable = false)
    private boolean permiteDetalhamentoOperacional;

    @Column(name = "pode_visualizar_contratos", nullable = false)
    private boolean podeVisualizarContratos;

    @Column(name = "pode_visualizar_empregados", nullable = false)
    private boolean podeVisualizarEmpregados;

    @Column(name = "pode_operar_frequencia", nullable = false)
    private boolean podeOperarFrequencia;

    @Column(name = "pode_acessar_relatorios", nullable = false)
    private boolean podeAcessarRelatorios;

    @Column(name = "progresso", nullable = false)
    private int progresso;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

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
    void antesPersistir() {
        LocalDateTime agora = agora();
        if (criadoEm == null) criadoEm = agora;
        atualizadoEm = agora;
        if (status == null) status = PainelCalculoStatus.PENDENTE;
    }

    @PreUpdate
    void antesAtualizar() {
        atualizadoEm = agora();
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChaveConsulta() { return chaveConsulta; }
    public void setChaveConsulta(String chaveConsulta) { this.chaveConsulta = chaveConsulta; }
    public PainelCalculoStatus getStatus() { return status; }
    public void setStatus(PainelCalculoStatus status) { this.status = status; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }
    public int getPeriodoDias() { return periodoDias; }
    public void setPeriodoDias(int periodoDias) { this.periodoDias = periodoDias; }
    public String getNivelEscopo() { return nivelEscopo; }
    public void setNivelEscopo(String nivelEscopo) { this.nivelEscopo = nivelEscopo; }
    public String getNomeEscopo() { return nomeEscopo; }
    public void setNomeEscopo(String nomeEscopo) { this.nomeEscopo = nomeEscopo; }
    public Long getSuperintendenciaId() { return superintendenciaId; }
    public void setSuperintendenciaId(Long superintendenciaId) { this.superintendenciaId = superintendenciaId; }
    public Long getGestaoLocalId() { return gestaoLocalId; }
    public void setGestaoLocalId(Long gestaoLocalId) { this.gestaoLocalId = gestaoLocalId; }
    public boolean isPermiteDetalhamentoOperacional() { return permiteDetalhamentoOperacional; }
    public void setPermiteDetalhamentoOperacional(boolean valor) { this.permiteDetalhamentoOperacional = valor; }
    public boolean isPodeVisualizarContratos() { return podeVisualizarContratos; }
    public void setPodeVisualizarContratos(boolean valor) { this.podeVisualizarContratos = valor; }
    public boolean isPodeVisualizarEmpregados() { return podeVisualizarEmpregados; }
    public void setPodeVisualizarEmpregados(boolean valor) { this.podeVisualizarEmpregados = valor; }
    public boolean isPodeOperarFrequencia() { return podeOperarFrequencia; }
    public void setPodeOperarFrequencia(boolean valor) { this.podeOperarFrequencia = valor; }
    public boolean isPodeAcessarRelatorios() { return podeAcessarRelatorios; }
    public void setPodeAcessarRelatorios(boolean valor) { this.podeAcessarRelatorios = valor; }
    public int getProgresso() { return progresso; }
    public void setProgresso(int progresso) { this.progresso = Math.max(0, Math.min(100, progresso)); }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
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
