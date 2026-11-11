package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Version private long version;

    @PrePersist @PreUpdate
    void normalizar() {
        if (razaoSocial != null) razaoSocial = razaoSocial.trim().replaceAll("\\s+", " ");
        if (cnpj != null) cnpj = cnpj.replaceAll("\\D", "");
        if (cnpj == null || cnpj.length() != 14) throw new IllegalStateException("CNPJ deve conter 14 dígitos.");
        if (ativo == null) ativo = true;
    }
}
