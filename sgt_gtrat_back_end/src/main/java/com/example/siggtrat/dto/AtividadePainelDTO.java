package com.example.siggtrat.dto;

import java.time.LocalDateTime;

public class AtividadePainelDTO {
    private Long id;
    private LocalDateTime dataEvento;
    private String descricao;

    private Long empregadoId;
    private String empregadoNome;

    private Long usuarioId;
    private String usuarioUsername;

    public AtividadePainelDTO() {}

    public AtividadePainelDTO(Long id, LocalDateTime dataEvento, String descricao,
                             Long empregadoId, String empregadoNome,
                             Long usuarioId, String usuarioUsername) {
        this.id = id;
        this.dataEvento = dataEvento;
        this.descricao = descricao;
        this.empregadoId = empregadoId;
        this.empregadoNome = empregadoNome;
        this.usuarioId = usuarioId;
        this.usuarioUsername = usuarioUsername;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDateTime dataEvento) { this.dataEvento = dataEvento; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Long getEmpregadoId() { return empregadoId; }
    public void setEmpregadoId(Long empregadoId) { this.empregadoId = empregadoId; }

    public String getEmpregadoNome() { return empregadoNome; }
    public void setEmpregadoNome(String empregadoNome) { this.empregadoNome = empregadoNome; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioUsername() { return usuarioUsername; }
    public void setUsuarioUsername(String usuarioUsername) { this.usuarioUsername = usuarioUsername; }
}