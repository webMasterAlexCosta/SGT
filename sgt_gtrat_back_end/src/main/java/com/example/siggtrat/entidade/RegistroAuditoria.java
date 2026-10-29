package com.example.siggtrat.entidade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable; // Garante Integridade

import java.time.Instant;

/**
 * Registro de auditoria (ações de escrita) para rastreio e conformidade.
 * A anotação @Immutable garante que, uma vez persistido, o registro não pode ser alterado via JPA.
 */
@Entity
@Table(
        name = "registro_auditoria",
        indexes = {
                @Index(name = "idx_registro_auditoria_created_at", columnList = "createdAt"),
                @Index(name = "idx_registro_auditoria_username", columnList = "username"),
                @Index(name = "idx_registro_auditoria_action", columnList = "action"),
                @Index(name = "idx_registro_auditoria_entity", columnList = "entityType,entityId"),
                @Index(name = "idx_registro_auditoria_request", columnList = "requestId")
        }
)
@Getter@Setter@AllArgsConstructor@NoArgsConstructor@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Immutable // CONFIGURAÇÃO CID: Integridade (Logs são imutáveis)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt = Instant.now();

    private String requestId;

    // Quem executou
    private Long userId;
    private String username;

    // O que fez
    @Column(nullable = false, length = 80)
    private String action;

    // Em qual entidade
    @Column(length = 80)
    private String entityType;

    private Long entityId;

    @Column(length = 255)
    private String entityLabel;

    // Contexto
    @Column(length = 45)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    // Request
    @Column(length = 10)
    private String httpMethod;

    @Column(length = 255)
    private String path;

    @Column(columnDefinition = "TEXT")
    private String queryString;

    /**
     * Payload (corpo) da requisição capturado como texto (tipicamente JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String payloadJson;

    private Boolean payloadTruncated;

    // Detalhes em JSON (texto)
    @Column(columnDefinition = "TEXT")
    private String detailsJson;
}