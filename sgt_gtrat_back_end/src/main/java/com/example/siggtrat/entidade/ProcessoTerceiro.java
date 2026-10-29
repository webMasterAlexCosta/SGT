package com.example.siggtrat.entidade;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "processo_terceiro", indexes = @Index(name = "idx_processo_terceiro_status_prazo", columnList = "status,prazo"))
@Getter @Setter @NoArgsConstructor
public class ProcessoTerceiro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_processo") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_empregado", nullable = false)
    private Empregado empregado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;
    @Column(nullable = false, length = 20) private String tipo;
    @Column(nullable = false, length = 20) private String status = "PENDENTE";
    @Column(nullable = false) private LocalDate prazo;
    @Column(name = "checklist_json", columnDefinition = "TEXT") private String checklistJson;
    @Column(columnDefinition = "TEXT") private String observacao;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm;
    @Column(name = "concluido_em") private LocalDateTime concluidoEm;
    @Version private long version;
    @PrePersist void criar() { if (criadoEm == null) criadoEm = LocalDateTime.now(); validar(); }
    @PreUpdate void atualizar() { validar(); }
    private void validar() {
        if (!java.util.Set.of("ONBOARDING", "OFFBOARDING").contains(tipo))
            throw new IllegalStateException("Tipo deve ser ONBOARDING ou OFFBOARDING.");
        if (!java.util.Set.of("PENDENTE", "EM_ANDAMENTO", "CONCLUIDO", "BLOQUEADO", "CANCELADO").contains(status))
            throw new IllegalStateException("Status de processo inválido.");
    }
}
