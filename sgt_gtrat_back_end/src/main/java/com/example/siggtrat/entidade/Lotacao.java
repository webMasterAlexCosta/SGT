package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "lotacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lotacao_gestao_local_nome",
                columnNames = {"id_gestao_local", "nome"}
        ),
        indexes = {
                @Index(name = "idx_lotacao_version", columnList = "version"),
                @Index(name = "idx_lotacao_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_lotacao_municipio", columnList = "municipio"),
                @Index(name = "idx_lotacao_ativo", columnList = "ativo")
        }
)
public class Lotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lotacao")
    private Long id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    /** Unidade de gestão responsável pela lotação. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    @JsonIgnoreProperties(value = {"superintendencia", "hibernateLazyInitializer", "handler"}, allowSetters = true)
    private GestaoLocal gestaoLocal;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lotacao_jornada",
            joinColumns = @JoinColumn(name = "id_lotacao", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "id_jornada", nullable = false),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_lotacao_jornada",
                    columnNames = {"id_lotacao", "id_jornada"}
            ),
            indexes = {
                    @Index(name = "idx_lotacao_jornada_lotacao", columnList = "id_lotacao"),
                    @Index(name = "idx_lotacao_jornada_jornada_lotacao",
                            columnList = "id_jornada, id_lotacao")
            }
    )
    @JsonIgnore
    private List<Jornada> turnos = new ArrayList<>();

    @Version
    @Column(name = "version")
    private long version = 0L;

    public Lotacao() {}

    public Lotacao(String nome) {
        this.nome = nome;
    }

    @PrePersist
    @PreUpdate
    protected void normalizar() {
        if (nome != null) nome = nome.trim().replaceAll("\\s+", " ");
        if (municipio != null) {
            municipio = municipio.trim().replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT);
            if (municipio.isBlank()) municipio = null;
        }
        if (ativo == null) ativo = true;
        if (nome == null || nome.isBlank()) {
            throw new IllegalStateException("O nome da lotação é obrigatório.");
        }
        String nomeNormalizado = nome.toUpperCase(java.util.Locale.ROOT);
        if (java.util.Set.of("#VALOR!", "#N/A", "#REF!", "#DIV/0!", "#NOME?").contains(nomeNormalizado)) {
            throw new IllegalStateException("O nome da lotação contém um marcador inválido de planilha: " + nome);
        }
        if (gestaoLocal == null) {
            throw new IllegalStateException("A lotação deve possuir uma Gestão Local.");
        }
        if (turnos != null) {
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (Jornada turno : turnos) {
                if (turno == null || turno.getGestaoLocal() == null) {
                    throw new IllegalStateException("Todos os turnos da lotação devem possuir Gestão Local.");
                }
                if (gestaoLocal.getId() != null && turno.getGestaoLocal().getId() != null
                        && !java.util.Objects.equals(gestaoLocal.getId(), turno.getGestaoLocal().getId())) {
                    throw new IllegalStateException("A lotação e seus turnos devem pertencer à mesma Gestão Local.");
                }
                if (turno.getId() != null && !ids.add(turno.getId())) {
                    throw new IllegalStateException("A mesma jornada não pode ser vinculada duas vezes à lotação.");
                }
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public List<Jornada> getTurnos() { return turnos; }
    public void setTurnos(List<Jornada> turnos) { this.turnos = turnos; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
