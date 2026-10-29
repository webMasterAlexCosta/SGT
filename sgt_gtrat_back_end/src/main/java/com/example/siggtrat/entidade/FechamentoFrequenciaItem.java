package com.example.siggtrat.entidade;

import com.example.siggtrat.constante.ConstantesDominio;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Entity
@Check(constraints = "status IN ('PRESENCA','PRESENCA_PARCIAL','FALTA','ATRASO','ATESTADO','SUSPENSAO','DESCANSO','DSR_PERDIDO','FOLGA','RT','TR')")
@Table(
        name = "fechamento_frequencia_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fechamento_frequencia_item_empregado",
                columnNames = {"id_fechamento", "id_empregado"}
        ),
        indexes = {
                @Index(name = "idx_fechamento_item_fechamento", columnList = "id_fechamento"),
                @Index(name = "idx_fechamento_item_empregado", columnList = "id_empregado")
        }
)
public class FechamentoFrequenciaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_fechamento", nullable = false)
    private FechamentoFrequencia fechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "entrada1", length = 5)
    private String entrada1;

    @Column(name = "saida1", length = 5)
    private String saida1;

    @Column(name = "entrada2", length = 5)
    private String entrada2;

    @Column(name = "saida2", length = 5)
    private String saida2;

    @Column(name = "horas_trabalhadas", length = 20)
    private String horasTrabalhadas;

    @Column(name = "horario_saida", length = 5)
    private String horarioSaida;

    @Column(name = "horas_noturnas")
    private Double horasNoturnas;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (status == null || status.isBlank()) {
            throw new IllegalStateException("O status da alteração do fechamento é obrigatório.");
        }
        status = status.trim().toUpperCase(Locale.ROOT);
        if (!ConstantesDominio.FREQUENCIA_STATUS_PERMITIDOS.contains(status)) {
            throw new IllegalStateException("Status de frequência inválido: " + status);
        }
        if (criadoEm == null) criadoEm = LocalDateTime.now(ZoneId.of(ConstantesDominio.TIMEZONE_PADRAO));
        if (observacao != null) {
            observacao = observacao.trim();
            if (observacao.isBlank()) observacao = null;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FechamentoFrequencia getFechamento() { return fechamento; }
    public void setFechamento(FechamentoFrequencia fechamento) { this.fechamento = fechamento; }
    public Empregado getEmpregado() { return empregado; }
    public void setEmpregado(Empregado empregado) { this.empregado = empregado; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
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
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
