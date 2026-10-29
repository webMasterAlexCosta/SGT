package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "fornecedor", uniqueConstraints = @UniqueConstraint(name = "uk_fornecedor_cnpj", columnNames = "cnpj"))
@Getter @Setter @NoArgsConstructor
public class Fornecedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fornecedor") private Long id;
    @Column(nullable = false, length = 180) private String razaoSocial;
    @Column(nullable = false, length = 14) private String cnpj;
    @Column(length = 180) private String email;
    @Column(nullable = false) private Boolean ativo = true;
    @Column(name = "risco_score") private Integer riscoScore = 0;
    @Column(name = "risco_nivel", length = 20) private String riscoNivel = "BAIXO";
    @Column(name = "proxima_avaliacao") private LocalDate proximaAvaliacao;
    @Column(name = "monitoramento_status", nullable = false, length = 25)
    private String monitoramentoStatus = "EM_DIA";
    @Version private long version;

    @PrePersist @PreUpdate
    void normalizar() {
        if (razaoSocial != null) razaoSocial = razaoSocial.trim().replaceAll("\\s+", " ");
        if (cnpj != null) cnpj = cnpj.replaceAll("\\D", "");
        if (cnpj == null || cnpj.length() != 14) throw new IllegalStateException("CNPJ deve conter 14 dígitos.");
        if (ativo == null) ativo = true;
        if (riscoScore == null) riscoScore = 0;
        if (monitoramentoStatus == null) monitoramentoStatus = "EM_DIA";
    }
}
