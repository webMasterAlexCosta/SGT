package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "empregado",
        indexes = {
                @Index(name = "idx_empregado_version", columnList = "version"),
                @Index(name = "idx_empregado_cpf", columnList = "cpf"),
                @Index(name = "idx_empregado_pessoa_canonica", columnList = "id_pessoa_canonica"),
                @Index(name = "idx_empregado_matricula", columnList = "matricula"),
                @Index(name = "idx_empregado_lotacao", columnList = "id_lotacao"),
                @Index(name = "idx_empregado_jornada", columnList = "id_jornada"),
                @Index(name = "idx_empregado_status", columnList = "id_status"),
                @Index(name = "idx_empregado_cargo", columnList = "id_cargo"),
                @Index(name = "idx_empregado_gestao_local", columnList = "id_gestao_local"),
                @Index(name = "idx_empregado_gestao_local_status_lotacao_jornada",
                        columnList = "id_gestao_local, id_status, id_lotacao, id_jornada"),
                @Index(name = "idx_empregado_freq_lotacao_jornada_status_nome",
                        columnList = "id_lotacao, id_jornada, id_status, nome, id_empregado"),
                @Index(name = "idx_empregado_fechamento_keyset",
                        columnList = "id_lotacao, id_jornada, id_status, id_empregado"),
                @Index(name = "idx_empregado_gestao_local_status_id",
                        columnList = "id_gestao_local, id_status, id_empregado"),
                @Index(name = "idx_empregado_gestao_local_status_nome_id",
                        columnList = "id_gestao_local, id_status, nome, id_empregado"),
                @Index(name = "idx_empregado_gestao_local_lotacao_nome_id",
                        columnList = "id_gestao_local, id_lotacao, nome, id_empregado"),
                @Index(name = "idx_empregado_nome_id",
                        columnList = "nome, id_empregado")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Empregado {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empregado")
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres")
    @Column(name = "matricula", length = 30, unique = true)
    private String matricula;

    @NotBlank(message = "O CPF é obrigatório")
    @Column(nullable = false, unique = true)
    private String cpf;

    /** Projeção operacional automática da identidade permanente do domínio temporal. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pessoa_canonica", unique = true)
    private Pessoa pessoaCanonica;

    @Column(name = "rg", length = 30)
    private String rg;

    @Column(name = "ctps_serie", length = 60)
    private String ctpsSerie;

    @Column(name = "nit_pis_pasep", length = 20)
    private String nitPisPasep;

    @Column(nullable = true)
    private String email;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @NotNull(message = "A Jornada/Turno é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_jornada", nullable = false)
    private Jornada jornada;

    @NotNull(message = "O Status é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_status", nullable = false)
    private StatusEmpregado status;

    @NotNull(message = "A Lotação é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_lotacao", nullable = false)
    private Lotacao lotacao;

    /**
     * Gestão Local responsável pelo empregado. A SE é obtida por gestaoLocal.superintendencia.
     * O vínculo é redundante de propósito: permite aplicar o isolamento em todas
     * as consultas, inclusive quando o histórico de contratos muda.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_gestao_local", nullable = false)
    private GestaoLocal gestaoLocal;

    @NotNull(message = "O Cargo é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cargo", nullable = false)
    private Cargo cargo;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_endereco", referencedColumnName = "id_endereco", unique = true)
    private Endereco endereco;

    @ManyToMany
    @JoinTable(
            name = "empregado_contrato",
            joinColumns = @JoinColumn(name = "id_empregado", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "id_contrato", nullable = false),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_empregado_contrato",
                            columnNames = {"id_empregado", "id_contrato"}
                    )
            },
            indexes = {
                    @Index(name = "idx_empregado_contrato_empregado", columnList = "id_empregado"),
                    @Index(name = "idx_empregado_contrato_contrato_empregado",
                            columnList = "id_contrato, id_empregado")
            }
    )
    @Size(max = 1, message = "O empregado pode possuir somente um contrato atual")
    private List<Contrato> contratos = new ArrayList<>();

    @OneToMany(mappedBy = "empregado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ocorrencia> ocorrencias = new ArrayList<>();

    /** Âncora assinada da cadeia de histórico para detectar alteração e remoção de registros. */
    @Column(name = "historico_ultimo_hash", length = 128)
    private String historicoUltimoHash;

    @Column(name = "historico_quantidade_assinada", nullable = false)
    private long historicoQuantidadeAssinada = 0L;

    @Column(name = "historico_total_ancorado", nullable = false)
    private long historicoTotalAncorado = 0L;

    @Column(name = "historico_ultimo_id_assinado")
    private Long historicoUltimoIdAssinado;

    @Column(name = "historico_ancora_assinatura", length = 128)
    private String historicoAncoraAssinatura;

    @Version
    @Column(name = "version")
    private long version = 0L;

    @PrePersist
    @PreUpdate
    protected void validarIntegridadeOrganizacional() {
        if (gestaoLocal == null || lotacao == null || jornada == null || status == null || cargo == null) {
            throw new IllegalStateException(
                    "Empregado deve possuir Gestão Local, lotação, jornada, status e cargo."
            );
        }

        Long gestaoLocalId = gestaoLocal.getId();
        if (lotacao.getGestaoLocal() == null
                || (gestaoLocalId != null && lotacao.getGestaoLocal().getId() != null
                && !java.util.Objects.equals(gestaoLocalId, lotacao.getGestaoLocal().getId()))) {
            throw new IllegalStateException("Empregado e lotação devem pertencer à mesma Gestão Local.");
        }
        if (jornada.getGestaoLocal() == null
                || (gestaoLocalId != null && jornada.getGestaoLocal().getId() != null
                && !java.util.Objects.equals(gestaoLocalId, jornada.getGestaoLocal().getId()))) {
            throw new IllegalStateException("Empregado e jornada devem pertencer à mesma Gestão Local.");
        }

        boolean inativo = status.getDescricao() != null
                && com.example.siggtrat.constante.ConstantesDominio.STATUS_INATIVO
                .equalsIgnoreCase(status.getDescricao());
        if (!inativo && (contratos == null || contratos.isEmpty())) {
            throw new IllegalStateException("Empregado ativo deve possuir ao menos um contrato.");
        }

        if (contratos != null && contratos.size() > 1) {
            throw new IllegalStateException(
                    "O empregado pode possuir somente um contrato atual. "
                            + "Mudanças de contrato devem encerrar o vínculo anterior no histórico."
            );
        }

        java.util.Set<Long> idsContratos = new java.util.HashSet<>();
        if (contratos != null) {
            for (Contrato contrato : contratos) {
                if (contrato == null || contrato.getGestaoLocal() == null) {
                    throw new IllegalStateException("Todos os contratos do empregado devem possuir Gestão Local.");
                }
                if (gestaoLocalId != null && contrato.getGestaoLocal().getId() != null
                        && !java.util.Objects.equals(gestaoLocalId, contrato.getGestaoLocal().getId())) {
                    throw new IllegalStateException(
                            "Empregado e contratos devem pertencer à mesma Gestão Local."
                    );
                }
                if (!inativo && !Boolean.TRUE.equals(contrato.getAtivo())) {
                    throw new IllegalStateException("Empregado ativo não pode possuir contrato inativo.");
                }
                if (contrato.getId() != null && !idsContratos.add(contrato.getId())) {
                    throw new IllegalStateException("O mesmo contrato não pode ser vinculado duas vezes.");
                }
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public Pessoa getPessoaCanonica() { return pessoaCanonica; }
    public void setPessoaCanonica(Pessoa pessoaCanonica) { this.pessoaCanonica = pessoaCanonica; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getCtpsSerie() { return ctpsSerie; }
    public void setCtpsSerie(String ctpsSerie) { this.ctpsSerie = ctpsSerie; }
    public String getNitPisPasep() { return nitPisPasep; }
    public void setNitPisPasep(String nitPisPasep) { this.nitPisPasep = nitPisPasep; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public StatusEmpregado getStatus() { return status; }
    public void setStatus(StatusEmpregado status) { this.status = status; }
    public Lotacao getLotacao() { return lotacao; }
    public void setLotacao(Lotacao lotacao) { this.lotacao = lotacao; }
    public GestaoLocal getGestaoLocal() { return gestaoLocal; }
    public void setGestaoLocal(GestaoLocal gestaoLocal) { this.gestaoLocal = gestaoLocal; }
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public List<Contrato> getContratos() { return contratos; }
    public void setContratos(List<Contrato> contratos) { this.contratos = contratos; }
    public List<Ocorrencia> getOcorrencias() { return ocorrencias; }
    public void setOcorrencias(List<Ocorrencia> ocorrencias) { this.ocorrencias = ocorrencias; }
    public String getHistoricoUltimoHash() { return historicoUltimoHash; }
    public void setHistoricoUltimoHash(String historicoUltimoHash) { this.historicoUltimoHash = historicoUltimoHash; }
    public long getHistoricoQuantidadeAssinada() { return historicoQuantidadeAssinada; }
    public void setHistoricoQuantidadeAssinada(long historicoQuantidadeAssinada) { this.historicoQuantidadeAssinada = historicoQuantidadeAssinada; }
    public long getHistoricoTotalAncorado() { return historicoTotalAncorado; }
    public void setHistoricoTotalAncorado(long historicoTotalAncorado) { this.historicoTotalAncorado = historicoTotalAncorado; }
    public Long getHistoricoUltimoIdAssinado() { return historicoUltimoIdAssinado; }
    public void setHistoricoUltimoIdAssinado(Long historicoUltimoIdAssinado) { this.historicoUltimoIdAssinado = historicoUltimoIdAssinado; }
    public String getHistoricoAncoraAssinatura() { return historicoAncoraAssinatura; }
    public void setHistoricoAncoraAssinatura(String historicoAncoraAssinatura) { this.historicoAncoraAssinatura = historicoAncoraAssinatura; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
