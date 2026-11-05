package com.example.siggtrat.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/** Identidade permanente, independente de contrato, matrícula ou alocação. */
@Entity
@Table(name = "pessoa", uniqueConstraints =
        @UniqueConstraint(name = "uk_pessoa_cpf", columnNames = "cpf"),
        indexes = {
                @Index(name = "idx_pessoa_nome", columnList = "nome"),
                @Index(name = "idx_pessoa_cpf", columnList = "cpf")
        })
public class Pessoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pessoa")
    private Long id;

    @Column(name = "nome", nullable = false, length = 180)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "rg", length = 30)
    private String rg;

    @Column(name = "ctps_serie", length = 60)
    private String ctpsSerie;

    @Column(name = "nit_pis_pasep", length = 20)
    private String nitPisPasep;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "email", length = 180)
    private String email;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_endereco", referencedColumnName = "id_endereco", unique = true)
    private Endereco endereco;

    @Column(name = "criado_em", nullable = false, updatable = false)
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();

    @Column(name = "atualizado_em")
    @org.hibernate.annotations.FractionalSeconds(6)
    private Instant atualizadoEm;

    @Version
    @Column(name = "version")
    private long version;

    @PrePersist
    protected void antesDePersistir() {
        normalizar();
        if (criadoEm == null) criadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    }

    @PreUpdate
    protected void antesDeAtualizar() {
        normalizar();
        atualizadoEm = com.example.siggtrat.utilitario.PrecisaoTemporal.agoraMicros();
    }

    private void normalizar() {
        nome = normalizarTextoObrigatorio(nome, "Nome da pessoa é obrigatório.");
        cpf = somenteDigitos(cpf);
        if (cpf == null || cpf.length() != 11) throw new IllegalStateException("CPF deve conter 11 dígitos.");
        rg = normalizarOpcional(rg);
        ctpsSerie = normalizarOpcional(ctpsSerie);
        nitPisPasep = somenteDigitos(nitPisPasep);
        email = normalizarOpcional(email);
        if (email != null) email = email.toLowerCase(Locale.ROOT);
    }

    private static String normalizarTextoObrigatorio(String valor, String mensagem) {
        String resultado = normalizarOpcional(valor);
        if (resultado == null) throw new IllegalStateException(mensagem);
        return resultado;
    }

    private static String normalizarOpcional(String valor) {
        if (valor == null) return null;
        String resultado = valor.trim().replaceAll("\\s+", " ");
        return resultado.isBlank() ? null : resultado;
    }

    private static String somenteDigitos(String valor) {
        if (valor == null) return null;
        String resultado = valor.replaceAll("\\D", "");
        return resultado.isBlank() ? null : resultado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getCtpsSerie() { return ctpsSerie; }
    public void setCtpsSerie(String ctpsSerie) { this.ctpsSerie = ctpsSerie; }
    public String getNitPisPasep() { return nitPisPasep; }
    public void setNitPisPasep(String nitPisPasep) { this.nitPisPasep = nitPisPasep; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
