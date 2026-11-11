package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

@Entity
@Check(constraints = "status IS NULL OR status IN ('PRESENCA','PRESENCA_PARCIAL','FALTA','ATRASO','ATESTADO','SUSPENSAO','DESCANSO','DSR_PERDIDO','FOLGA','RT','TR')")
@Table(
        name = "frequencia",
        indexes = {
                @Index(name = "idx_frequencia_version", columnList = "version"),
                @Index(name = "idx_frequencia_empregado", columnList = "id_empregado"),
                @Index(name = "idx_frequencia_pessoa_data", columnList = "id_pessoa,data_referencia"),
                @Index(name = "idx_frequencia_vinculo_data", columnList = "id_vinculo_contratual,data_referencia"),
                @Index(name = "idx_frequencia_alocacao_data", columnList = "id_alocacao_vinculo,data_referencia"),
                @Index(name = "idx_frequencia_estado_data", columnList = "estado_negocial,data_referencia"),
                @Index(name = "idx_frequencia_validade_data", columnList = "validade_operacional,data_referencia"),
                @Index(name = "idx_frequencia_data", columnList = "data_referencia"),
                @Index(name = "idx_frequencia_lotacao_snapshot_data", columnList = "lotacao_snapshot_id,data_referencia"),
                @Index(name = "idx_frequencia_jornada_snapshot_data", columnList = "jornada_snapshot_id,data_referencia"),
                @Index(name = "idx_frequencia_contrato_snapshot_data", columnList = "contrato_snapshot_id,data_referencia"),
                @Index(name = "idx_frequencia_cargo_snapshot_data", columnList = "cargo_snapshot_id,data_referencia"),
                @Index(name = "idx_frequencia_gestao_local_snapshot_data_status", columnList = "gestao_local_snapshot_id,data_referencia,status"),
                @Index(name = "idx_frequencia_se_snapshot_data_status", columnList = "superintendencia_snapshot_id,data_referencia,status"),
                @Index(name = "idx_frequencia_data_status_empregado", columnList = "data_referencia,status,id_empregado"),
                @Index(name = "idx_frequencia_empregado_data_status", columnList = "id_empregado,data_referencia,status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_frequencia_empregado_data", columnNames = {"id_empregado", "data_referencia"}),
                @UniqueConstraint(name = "uk_frequencia_vinculo_data", columnNames = {"id_vinculo_contratual", "data_referencia"})
        }
)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Frequencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    @Column(name = "data_referencia", nullable = false)
    private LocalDate dataReferencia;

    /** Identidade permanente da Pessoa associada ao lançamento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pessoa")
    private Pessoa pessoa;

    /** Vínculo válido resolvido para a data negocial da frequência. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vinculo_contratual")
    private VinculoContratual vinculoContratual;

    /** Alocação histórica válida na data de início da jornada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alocacao_vinculo")
    private AlocacaoVinculo alocacaoVinculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocial", nullable = false, length = 16)
    private EstadoNegocialFrequencia estadoNegocial = EstadoNegocialFrequencia.ABERTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "validade_operacional", nullable = false, length = 24)
    private ValidadeOperacionalFrequencia validadeOperacional = ValidadeOperacionalFrequencia.VALIDA;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_auditoria", nullable = false, length = 32)
    private EstadoAuditoriaFrequencia estadoAuditoria = EstadoAuditoriaFrequencia.ORIGINAL;

    @Column(name = "bloqueada_edicao_operacional", nullable = false)
    private Boolean bloqueadaEdicaoOperacional = false;

    @Column(name = "versao_negocial", nullable = false)
    private Integer versaoNegocial = 1;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "autor_original_id", updatable = false)
    private Long autorOriginalId;

    @Column(name = "autor_original_username", length = 120, updatable = false)
    private String autorOriginalUsername;

    @Column(name = "ultima_retificacao_por_id")
    private Long ultimaRetificacaoPorId;

    @Column(name = "ultima_retificacao_por_username", length = 120)
    private String ultimaRetificacaoPorUsername;

    @Column(name = "motivo_revisao", length = 1000)
    private String motivoRevisao;

    @Column(name = "mes")
    private Integer mes;

    @Column(name = "ano")
    private Integer ano;

    @Column(length = 20)
    private String status;

    @Column(name = "entrada1")
    private String entrada1;

    @Column(name = "saida1")
    private String saida1;

    @Column(name = "entrada2")
    private String entrada2;

    @Column(name = "saida2")
    private String saida2;

    @Column(name = "horas_trabalhadas")
    private String horasTrabalhadas;

    @Column(name = "horario_saida")
    private String horarioSaida;

    @Column(name = "horas_noturnas")
    private Double horasNoturnas;

    /** Minutos efetivamente trabalhados, sem a redução ficta da hora noturna. */
    @Column(name = "minutos_fisicos")
    private Integer minutosFisicos;

    /** Minutos físicos que interceptam o período noturno legal. */
    @Column(name = "minutos_noturnos_reais")
    private Integer minutosNoturnosReais;

    /** Minutos equivalentes acrescentados pelas regras adicionais da jornada. */
    @Column(name = "minutos_adicionais")
    private Integer minutosAdicionais;

    /** Total oficial usado por relatórios, folhasHoras e faturamento. */
    @Column(name = "minutos_pagaveis")
    private Integer minutosPagaveis;

    @Column(name = "calculo_versao", length = 40)
    private String calculoVersao;

    /*
     * Ocorrência é uma sobreposição reversível. A evidência de ponto nunca é
     * removida: guardamos o status anterior e o restauramos quando a última
     * ocorrência aplicável for excluída.
     */
    @Column(name = "status_base_antes_ocorrencia", length = 20)
    private String statusBaseAntesOcorrencia;

    @Column(name = "ocorrencia_override_ativo", nullable = false)
    private Boolean ocorrenciaOverrideAtivo = false;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    /*
     * Retrato imutável do contexto utilizado no cálculo. Ele evita que uma
     * edição futura recalcule a frequência com o turno/lotação/contrato atual.
     */
    @Column(name = "jornada_snapshot_id")
    private Long jornadaSnapshotId;

    @Column(name = "jornada_snapshot_descricao", length = 150)
    private String jornadaSnapshotDescricao;

    @Column(name = "jornada_snapshot_dia_trabalho")
    private Boolean jornadaSnapshotDiaTrabalho;

    @Column(name = "jornada_snapshot_entrada1", length = 5)
    private String jornadaSnapshotEntrada1;

    @Column(name = "jornada_snapshot_saida1", length = 5)
    private String jornadaSnapshotSaida1;

    @Column(name = "jornada_snapshot_entrada2", length = 5)
    private String jornadaSnapshotEntrada2;

    @Column(name = "jornada_snapshot_saida2", length = 5)
    private String jornadaSnapshotSaida2;

    @Column(name = "lotacao_snapshot_id")
    private Long lotacaoSnapshotId;

    @Column(name = "lotacao_snapshot_nome", length = 200)
    private String lotacaoSnapshotNome;

    @Column(name = "lotacao_snapshot_municipio", length = 150)
    private String lotacaoSnapshotMunicipio;

    @Column(name = "gestao_local_snapshot_id")
    private Long gestaoLocalSnapshotId;

    @Column(name = "superintendencia_snapshot_id")
    private Long superintendenciaSnapshotId;

    @Column(name = "contrato_snapshot_id")
    private Long contratoSnapshotId;

    @Column(name = "contrato_snapshot_numero", length = 100)
    private String contratoSnapshotNumero;

    @Column(name = "empregado_snapshot_nome", length = 200)
    private String empregadoSnapshotNome;

    @Column(name = "cargo_snapshot_id")
    private Long cargoSnapshotId;

    @Column(name = "cargo_snapshot_nome", length = 150)
    private String cargoSnapshotNome;

    @Version
    @Column(name = "version")
    private long version = 0L;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void antesPersistirOuAtualizar() {
        if (dataReferencia == null) {
            throw new IllegalStateException("A data de referência da frequência é obrigatória.");
        }
        if (status == null || status.isBlank()) {
            status = null;
        } else {
            status = status.trim().toUpperCase(Locale.ROOT);
            if (!ConstantesDominio.FREQUENCIA_STATUS_PERMITIDOS.contains(status)) {
                throw new IllegalStateException("Status de frequência inválido: " + status);
            }
        }
        this.mes = dataReferencia.getMonthValue();
        this.ano = dataReferencia.getYear();
        if (ocorrenciaOverrideAtivo == null) ocorrenciaOverrideAtivo = false;
        if (estadoNegocial == null) estadoNegocial = EstadoNegocialFrequencia.ABERTA;
        if (validadeOperacional == null) validadeOperacional = ValidadeOperacionalFrequencia.VALIDA;
        if (estadoAuditoria == null) estadoAuditoria = EstadoAuditoriaFrequencia.ORIGINAL;
        if (bloqueadaEdicaoOperacional == null) bloqueadaEdicaoOperacional = false;
        if (versaoNegocial == null || versaoNegocial < 1) versaoNegocial = 1;
        if (registradoEm == null) registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
        this.updatedAt = LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
    }

    public Frequencia() {}

    @Transient
    public boolean possuiSnapshotJornada() {
        return jornadaSnapshotId != null || jornadaSnapshotDiaTrabalho != null;
    }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
    public LocalDate getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(LocalDate dataReferencia) { this.dataReferencia = dataReferencia; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEntrada1() { return entrada1; }
    public void setEntrada1(String entrada1) { this.entrada1 = entrada1; }
    public String getSaida1() { return saida1; }
    public void setSaida1(String saida1) { this.saida1 = saida1; }
    public String getEntrada2() { return entrada2; }
    public void setEntrada2(String entrada2) { this.entrada2 = entrada2; }
    public String getSaida2() { return saida2; }
    public void setSaida2(String saida2) { this.saida2 = saida2; }
    public String getHorasTrabalhadas() { return horasTrabalhadas; }
    public void setHorasTrabalhadas(String horasTrabalhadas) { this.horasTrabalhadas = horasTrabalhadas; }
    public String getHorarioSaida() { return horarioSaida; }
    public void setHorarioSaida(String horarioSaida) { this.horarioSaida = horarioSaida; }
    public Double getHorasNoturnas() { return horasNoturnas; }
    public void setHorasNoturnas(Double horasNoturnas) { this.horasNoturnas = horasNoturnas; }
    public Integer getMinutosFisicos() { return minutosFisicos; }
    public void setMinutosFisicos(Integer minutosFisicos) { this.minutosFisicos = minutosFisicos; }
    public Integer getMinutosNoturnosReais() { return minutosNoturnosReais; }
    public void setMinutosNoturnosReais(Integer minutosNoturnosReais) { this.minutosNoturnosReais = minutosNoturnosReais; }
    public Integer getMinutosAdicionais() { return minutosAdicionais; }
    public void setMinutosAdicionais(Integer minutosAdicionais) { this.minutosAdicionais = minutosAdicionais; }
    public Integer getMinutosPagaveis() { return minutosPagaveis; }
    public void setMinutosPagaveis(Integer minutosPagaveis) { this.minutosPagaveis = minutosPagaveis; }
    public String getCalculoVersao() { return calculoVersao; }
    public void setCalculoVersao(String calculoVersao) { this.calculoVersao = calculoVersao; }
    public String getStatusBaseAntesOcorrencia() { return statusBaseAntesOcorrencia; }
    public void setStatusBaseAntesOcorrencia(String statusBaseAntesOcorrencia) { this.statusBaseAntesOcorrencia = statusBaseAntesOcorrencia; }
    public Boolean getOcorrenciaOverrideAtivo() { return ocorrenciaOverrideAtivo; }
    public void setOcorrenciaOverrideAtivo(Boolean ocorrenciaOverrideAtivo) { this.ocorrenciaOverrideAtivo = ocorrenciaOverrideAtivo; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getJornadaSnapshotId() { return jornadaSnapshotId; }
    public void setJornadaSnapshotId(Long jornadaSnapshotId) { this.jornadaSnapshotId = jornadaSnapshotId; }
    public String getJornadaSnapshotDescricao() { return jornadaSnapshotDescricao; }
    public void setJornadaSnapshotDescricao(String jornadaSnapshotDescricao) { this.jornadaSnapshotDescricao = jornadaSnapshotDescricao; }
    public Boolean getJornadaSnapshotDiaTrabalho() { return jornadaSnapshotDiaTrabalho; }
    public void setJornadaSnapshotDiaTrabalho(Boolean jornadaSnapshotDiaTrabalho) { this.jornadaSnapshotDiaTrabalho = jornadaSnapshotDiaTrabalho; }
    public String getJornadaSnapshotEntrada1() { return jornadaSnapshotEntrada1; }
    public void setJornadaSnapshotEntrada1(String jornadaSnapshotEntrada1) { this.jornadaSnapshotEntrada1 = jornadaSnapshotEntrada1; }
    public String getJornadaSnapshotSaida1() { return jornadaSnapshotSaida1; }
    public void setJornadaSnapshotSaida1(String jornadaSnapshotSaida1) { this.jornadaSnapshotSaida1 = jornadaSnapshotSaida1; }
    public String getJornadaSnapshotEntrada2() { return jornadaSnapshotEntrada2; }
    public void setJornadaSnapshotEntrada2(String jornadaSnapshotEntrada2) { this.jornadaSnapshotEntrada2 = jornadaSnapshotEntrada2; }
    public String getJornadaSnapshotSaida2() { return jornadaSnapshotSaida2; }
    public void setJornadaSnapshotSaida2(String jornadaSnapshotSaida2) { this.jornadaSnapshotSaida2 = jornadaSnapshotSaida2; }
    public Long getLotacaoSnapshotId() { return lotacaoSnapshotId; }
    public void setLotacaoSnapshotId(Long lotacaoSnapshotId) { this.lotacaoSnapshotId = lotacaoSnapshotId; }
    public String getLotacaoSnapshotNome() { return lotacaoSnapshotNome; }
    public void setLotacaoSnapshotNome(String lotacaoSnapshotNome) { this.lotacaoSnapshotNome = lotacaoSnapshotNome; }
    public String getLotacaoSnapshotMunicipio() { return lotacaoSnapshotMunicipio; }
    public void setLotacaoSnapshotMunicipio(String lotacaoSnapshotMunicipio) { this.lotacaoSnapshotMunicipio = lotacaoSnapshotMunicipio; }
    public Long getGestaoLocalSnapshotId() { return gestaoLocalSnapshotId; }
    public void setGestaoLocalSnapshotId(Long gestaoLocalSnapshotId) { this.gestaoLocalSnapshotId = gestaoLocalSnapshotId; }
    public Long getSuperintendenciaSnapshotId() { return superintendenciaSnapshotId; }
    public void setSuperintendenciaSnapshotId(Long superintendenciaSnapshotId) { this.superintendenciaSnapshotId = superintendenciaSnapshotId; }
    public Long getContratoSnapshotId() { return contratoSnapshotId; }
    public void setContratoSnapshotId(Long contratoSnapshotId) { this.contratoSnapshotId = contratoSnapshotId; }
    public String getContratoSnapshotNumero() { return contratoSnapshotNumero; }
    public void setContratoSnapshotNumero(String contratoSnapshotNumero) { this.contratoSnapshotNumero = contratoSnapshotNumero; }
    public String getEmpregadoSnapshotNome() { return empregadoSnapshotNome; }
    public void setEmpregadoSnapshotNome(String empregadoSnapshotNome) { this.empregadoSnapshotNome = empregadoSnapshotNome; }
    public Long getCargoSnapshotId() { return cargoSnapshotId; }
    public void setCargoSnapshotId(Long cargoSnapshotId) { this.cargoSnapshotId = cargoSnapshotId; }
    public String getCargoSnapshotNome() { return cargoSnapshotNome; }
    public void setCargoSnapshotNome(String cargoSnapshotNome) { this.cargoSnapshotNome = cargoSnapshotNome; }
    public Pessoa getPessoa() { return pessoa; }
    public void setPessoa(Pessoa pessoa) { this.pessoa = pessoa; }
    public VinculoContratual getVinculoContratual() { return vinculoContratual; }
    public void setVinculoContratual(VinculoContratual vinculoContratual) { this.vinculoContratual = vinculoContratual; }
    public AlocacaoVinculo getAlocacaoVinculo() { return alocacaoVinculo; }
    public void setAlocacaoVinculo(AlocacaoVinculo alocacaoVinculo) { this.alocacaoVinculo = alocacaoVinculo; }
    public EstadoNegocialFrequencia getEstadoNegocial() { return estadoNegocial; }
    public void setEstadoNegocial(EstadoNegocialFrequencia estadoNegocial) { this.estadoNegocial = estadoNegocial; }
    public ValidadeOperacionalFrequencia getValidadeOperacional() { return validadeOperacional; }
    public void setValidadeOperacional(ValidadeOperacionalFrequencia validadeOperacional) { this.validadeOperacional = validadeOperacional; }
    public EstadoAuditoriaFrequencia getEstadoAuditoria() { return estadoAuditoria; }
    public void setEstadoAuditoria(EstadoAuditoriaFrequencia estadoAuditoria) { this.estadoAuditoria = estadoAuditoria; }
    public Boolean getBloqueadaEdicaoOperacional() { return bloqueadaEdicaoOperacional; }
    public void setBloqueadaEdicaoOperacional(Boolean bloqueadaEdicaoOperacional) { this.bloqueadaEdicaoOperacional = bloqueadaEdicaoOperacional; }
    public Integer getVersaoNegocial() { return versaoNegocial; }
    public void setVersaoNegocial(Integer versaoNegocial) { this.versaoNegocial = versaoNegocial; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) { this.registradoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.instantePersistivelMicros(registradoEm); }
    public Long getAutorOriginalId() { return autorOriginalId; }
    public void setAutorOriginalId(Long autorOriginalId) { this.autorOriginalId = autorOriginalId; }
    public String getAutorOriginalUsername() { return autorOriginalUsername; }
    public void setAutorOriginalUsername(String autorOriginalUsername) { this.autorOriginalUsername = autorOriginalUsername; }
    public Long getUltimaRetificacaoPorId() { return ultimaRetificacaoPorId; }
    public void setUltimaRetificacaoPorId(Long ultimaRetificacaoPorId) { this.ultimaRetificacaoPorId = ultimaRetificacaoPorId; }
    public String getUltimaRetificacaoPorUsername() { return ultimaRetificacaoPorUsername; }
    public void setUltimaRetificacaoPorUsername(String ultimaRetificacaoPorUsername) { this.ultimaRetificacaoPorUsername = ultimaRetificacaoPorUsername; }
    public String getMotivoRevisao() { return motivoRevisao; }
    public void setMotivoRevisao(String motivoRevisao) { this.motivoRevisao = motivoRevisao; }

}
