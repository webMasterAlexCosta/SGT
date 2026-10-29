package com.example.siggtrat.dto;

public class EstatisticasPainelDTO {
    private long empregados;
    private long ativos;
    private long inativos;
    private long cargos;
    private long lotacoes;

    public EstatisticasPainelDTO() {}

    public EstatisticasPainelDTO(long empregados, long ativos, long inativos, long cargos, long lotacoes) {
        this.empregados = empregados;
        this.ativos = ativos;
        this.inativos = inativos;
        this.cargos = cargos;
        this.lotacoes = lotacoes;
    }

    public long getEmpregados() { return empregados; }
    public void setEmpregados(long empregados) { this.empregados = empregados; }

    public long getAtivos() { return ativos; }
    public void setAtivos(long ativos) { this.ativos = ativos; }

    public long getInativos() { return inativos; }
    public void setInativos(long inativos) { this.inativos = inativos; }

    public long getCargos() { return cargos; }
    public void setCargos(long cargos) { this.cargos = cargos; }

    public long getLotacoes() { return lotacoes; }
    public void setLotacoes(long lotacoes) { this.lotacoes = lotacoes; }
}
