package com.example.siggtrat.dominio.documento;

/** Estado negocial de uma versão documental; nenhuma versão publicada é apagada. */
public enum EstadoDocumentoComprobatorio {
    RASCUNHO,
    PUBLICADO,
    SUBSTITUIDO,
    INVALIDO
}

