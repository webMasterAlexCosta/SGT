package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.DayOfWeek;

@Entity
@Table(
        name = "jornada_dia_semana",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jornada_dia_semana",
                columnNames = {"id_jornada", "dia_semana"}
        ),
        indexes = @Index(name = "idx_jornada_dia_semana_jornada", columnList = "id_jornada")
)
public class JornadaDiaSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    @JsonIgnore
    private Jornada jornada;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "dia_trabalho", nullable = false)
    private Boolean diaTrabalho = true;

    @Column(length = 5)
    private String entrada1;

    @Column(length = 5)
    private String saida1;

    @Column(length = 5)
    private String entrada2;

    @Column(length = 5)
    private String saida2;

    public JornadaDiaSemana() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }

    public DayOfWeek getDiaSemana() { return diaSemana; }
    public void setDiaSemana(DayOfWeek diaSemana) { this.diaSemana = diaSemana; }

    public Boolean getDiaTrabalho() { return diaTrabalho; }
    public void setDiaTrabalho(Boolean diaTrabalho) { this.diaTrabalho = diaTrabalho; }

    public String getEntrada1() { return entrada1; }
    public void setEntrada1(String entrada1) { this.entrada1 = entrada1; }

    public String getSaida1() { return saida1; }
    public void setSaida1(String saida1) { this.saida1 = saida1; }

    public String getEntrada2() { return entrada2; }
    public void setEntrada2(String entrada2) { this.entrada2 = entrada2; }

    public String getSaida2() { return saida2; }
    public void setSaida2(String saida2) { this.saida2 = saida2; }
}