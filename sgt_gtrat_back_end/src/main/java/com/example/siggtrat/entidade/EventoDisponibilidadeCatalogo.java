package com.example.siggtrat.entidade;

import com.example.siggtrat.dominio.catalogo.TipoCatalogoTemporal;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "evento_disponibilidade_catalogo", indexes = {
        @Index(name = "idx_evento_catalogo_data", columnList = "tipo_catalogo,catalogo_id,data_efetiva,registrado_em")
})
public class EventoDisponibilidadeCatalogo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_catalogo", nullable = false, length = 40)
    private TipoCatalogoTemporal tipoCatalogo;
    @Column(name = "catalogo_id", nullable = false) private Long catalogoId;
    @Column(name = "data_efetiva", nullable = false) private LocalDate dataEfetiva;
    @Column(nullable = false) private Boolean ativo;
    @Column(nullable = false, length = 500) private String motivo;
    @Column(name = "responsavel_id") private Long responsavelId;
    @Column(name = "responsavel_username", length = 120) private String responsavelUsername;
    @Column(name = "registrado_em", nullable = false, updatable = false) private Instant registradoEm = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoCatalogoTemporal getTipoCatalogo() { return tipoCatalogo; }
    public void setTipoCatalogo(TipoCatalogoTemporal tipoCatalogo) { this.tipoCatalogo = tipoCatalogo; }
    public Long getCatalogoId() { return catalogoId; }
    public void setCatalogoId(Long catalogoId) { this.catalogoId = catalogoId; }
    public LocalDate getDataEfetiva() { return dataEfetiva; }
    public void setDataEfetiva(LocalDate dataEfetiva) { this.dataEfetiva = dataEfetiva; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public String getResponsavelUsername() { return responsavelUsername; }
    public void setResponsavelUsername(String responsavelUsername) { this.responsavelUsername = responsavelUsername; }
    public Instant getRegistradoEm() { return registradoEm; }
    public void setRegistradoEm(Instant registradoEm) { this.registradoEm = registradoEm; }
}
