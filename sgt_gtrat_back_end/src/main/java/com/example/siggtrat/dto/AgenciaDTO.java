package com.example.siggtrat.dto;


public class AgenciaDTO {
    private Long id;
    private String numero;
    private Long bancoId;

    public AgenciaDTO() {}

    public AgenciaDTO(Long id, String numero, Long bancoId) {
        this.id = id;
        this.numero = numero;
        this.bancoId = bancoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getBancoId() {
        return bancoId;
    }

    public void setBancoId(Long bancoId) {
        this.bancoId = bancoId;
    }
}