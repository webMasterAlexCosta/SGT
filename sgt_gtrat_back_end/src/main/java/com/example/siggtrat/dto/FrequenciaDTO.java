package com.example.siggtrat.dto;

import com.example.siggtrat.dominio.frequencia.EstadoAuditoriaFrequencia;
import com.example.siggtrat.dominio.frequencia.EstadoNegocialFrequencia;
import com.example.siggtrat.dominio.frequencia.ValidadeOperacionalFrequencia;

import java.time.LocalDate;
import java.time.LocalDateTime;


public class FrequenciaDTO {

    private Long id;
    private EmpregadoResumoDTO empregado;
    private LocalDate dataReferencia;
    private Integer mes;
    private Integer ano;
    private String status;
    private String entrada1;
    private String saida1;
    private String entrada2;
    private String saida2;
    private String horasTrabalhadas;
    private String horarioSaida;

    private Double horasNoturnas;

    private String observacao;
    private LocalDateTime prazoEdicao;
    private LocalDateTime updatedAt;

    private Long pessoaId;
    private Long vinculoContratualId;
    private Long alocacaoVinculoId;
    private Long contratoId;
    private Long lotacaoId;
    private Long jornadaId;
    private Long cargoId;
    private Long gestaoLocalId;
    private EstadoNegocialFrequencia estadoNegocial;
    private ValidadeOperacionalFrequencia validadeOperacional;
    private EstadoAuditoriaFrequencia estadoAuditoria;
    private Boolean bloqueadaEdicaoOperacional;
    private Integer versaoNegocial;

    /** Obrigatória quando o Gestor Local retifica um registro existente. */
    private String justificativaRetificacao;
    /** Obrigatória na retificação gerencial para tornar reenvios idempotentes. */
    private String chaveIdempotencia;

    public FrequenciaDTO() {}

    public static class EmpregadoResumoDTO {
        private Long id;
        private String nome;

        public EmpregadoResumoDTO() {}

        public EmpregadoResumoDTO(Long id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public EmpregadoResumoDTO getEmpregado() {
        return empregado;
    }
    public void setEmpregado(EmpregadoResumoDTO empregado) {
        this.empregado = empregado;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }
    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }

    public Integer getMes() {
        return mes;
    }
    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAno() {
        return ano;
    }
    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getEntrada1() {
        return entrada1;
    }
    public void setEntrada1(String entrada1) {
        this.entrada1 = entrada1;
    }

    public String getSaida1() {
        return saida1;
    }
    public void setSaida1(String saida1) {
        this.saida1 = saida1;
    }

    public String getEntrada2() {
        return entrada2;
    }
    public void setEntrada2(String entrada2) {
        this.entrada2 = entrada2;
    }

    public String getSaida2() {
        return saida2;
    }
    public void setSaida2(String saida2) {
        this.saida2 = saida2;
    }

    public String getHorasTrabalhadas() {
        return horasTrabalhadas;
    }
    public void setHorasTrabalhadas(String horasTrabalhadas) {
        this.horasTrabalhadas = horasTrabalhadas;
    }

    public String getHorarioSaida() {
        return horarioSaida;
    }
    public void setHorarioSaida(String horarioSaida) {
        this.horarioSaida = horarioSaida;
    }

    public Double getHorasNoturnas() {
        return horasNoturnas;
    }
    public void setHorasNoturnas(Double horasNoturnas) {
        this.horasNoturnas = horasNoturnas;
    }

    public String getObservacao() {
        return observacao;
    }
    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getPrazoEdicao() {
        return prazoEdicao;
    }
    public void setPrazoEdicao(LocalDateTime prazoEdicao) {
        this.prazoEdicao = prazoEdicao;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public Long getVinculoContratualId() { return vinculoContratualId; }
    public void setVinculoContratualId(Long vinculoContratualId) { this.vinculoContratualId = vinculoContratualId; }
    public Long getAlocacaoVinculoId() { return alocacaoVinculoId; }
    public void setAlocacaoVinculoId(Long alocacaoVinculoId) { this.alocacaoVinculoId = alocacaoVinculoId; }
    public Long getContratoId() { return contratoId; }
    public void setContratoId(Long contratoId) { this.contratoId = contratoId; }
    public Long getLotacaoId() { return lotacaoId; }
    public void setLotacaoId(Long lotacaoId) { this.lotacaoId = lotacaoId; }
    public Long getJornadaId() { return jornadaId; }
    public void setJornadaId(Long jornadaId) { this.jornadaId = jornadaId; }
    public Long getCargoId() { return cargoId; }
    public void setCargoId(Long cargoId) { this.cargoId = cargoId; }
    public Long getGestaoLocalId() { return gestaoLocalId; }
    public void setGestaoLocalId(Long gestaoLocalId) { this.gestaoLocalId = gestaoLocalId; }
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
    public String getJustificativaRetificacao() { return justificativaRetificacao; }
    public void setJustificativaRetificacao(String justificativaRetificacao) { this.justificativaRetificacao = justificativaRetificacao; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

}
