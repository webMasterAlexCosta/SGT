package com.example.siggtrat.dominio.acesso;

import com.example.siggtrat.constante.ConstantesDominio;

/**
 * Perfil principal e único de uma conta do SGT-GTRAT.
 *
 * <p>Permissões excepcionais não são perfis. Elas possuem ciclo de vida,
 * escopo, validade e auditoria próprios no domínio de permissões.</p>
 */
public enum PerfilAcesso {
    ADMINISTRADOR(ConstantesDominio.PERFIL_ADMINISTRADOR),
    GESTOR_NACIONAL(ConstantesDominio.PERFIL_GESTOR_NACIONAL),
    GESTOR_ESTADUAL(ConstantesDominio.PERFIL_GESTOR_ESTADUAL),
    GESTOR_LOCAL(ConstantesDominio.PERFIL_GESTOR_LOCAL),
    PRESIDENTE(ConstantesDominio.PERFIL_PRESIDENTE),
    OPERACIONAL(ConstantesDominio.PERFIL_OPERACIONAL);

    private final String autoridade;

    PerfilAcesso(String autoridade) {
        this.autoridade = autoridade;
    }

    public String autoridade() {
        return autoridade;
    }

    public boolean possuiConsultaNacional() {
        return this == GESTOR_NACIONAL || this == PRESIDENTE;
    }

    public boolean possuiEscopoEstadual() {
        return this == GESTOR_ESTADUAL;
    }

    public boolean possuiEscritaNegocialLocal() {
        return this == GESTOR_LOCAL;
    }
}
