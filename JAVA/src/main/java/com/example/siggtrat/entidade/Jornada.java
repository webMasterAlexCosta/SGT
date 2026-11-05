package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode.Include;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "jornada",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_jornada_gestao_local_descricao",
                columnNames = {"id_gestao_local", "descricao"}
        ),
        indexes = {
                @Index(name = "idx_jornada_version", columnList = "version"),
                @Index(name = "idx_jornada_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_jornada_ativo", columnList = "ativo")
        }
)
@AllArgsConstructor@NoArgsConstructor@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Jornada {

    @Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jornada")
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JornadaDiaSemana> dias = new ArrayList<>();

    @OneToMany(mappedBy = "jornada", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RegraAdicional> regrasAdicionais = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    @JsonIgnore
    private GestaoLocal gestaoLocal;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToMany(mappedBy = "turnos")
    @JsonIgnore
    private List<Lotacao> lotacoes = new ArrayList<>();

    @Version
    @Column(name = "version")
    private long version = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public List<JornadaDiaSemana> getDias() { return dias; }
    public void setDias(List<JornadaDiaSemana> dias) {
        this.dias.clear();
        if (dias != null) {
            for (JornadaDiaSemana dia : dias) {
                dia.setJornada(this);
                this.dias.add(dia);
            }
        }
    }

    public List<RegraAdicional> getRegrasAdicionais() { return regrasAdicionais; }
    public void setRegrasAdicionais(List<RegraAdicional> regrasAdicionais) {
        this.regrasAdicionais.clear();
        if (regrasAdicionais != null) {
            for (RegraAdicional r : regrasAdicionais) {
                r.setJornada(this);
                this.regrasAdicionais.add(r);
            }
        }
    }

    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public List<Lotacao> getLotacoes() { return lotacoes; }
    public void setLotacoes(List<Lotacao> lotacoes) { this.lotacoes = lotacoes; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
