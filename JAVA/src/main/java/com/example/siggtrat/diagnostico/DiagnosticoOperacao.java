package com.example.siggtrat.diagnostico;
/** Contrato interno para o Logger da própria classe; não é contrato HTTP. */
public interface DiagnosticoOperacao {
    org.slf4j.Logger loggerDiagnostico();
}
