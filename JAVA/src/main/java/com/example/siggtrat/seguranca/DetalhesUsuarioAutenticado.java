package com.example.siggtrat.seguranca;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** Fotografia da conta consultada no banco para autorizar a requisição atual. */
public final class DetalhesUsuarioAutenticado extends User {
    private static final long serialVersionUID = 1L;
    private final boolean trocaSenhaObrigatoria;

    public DetalhesUsuarioAutenticado(String username, String password, boolean enabled,
                                      boolean accountNonLocked,
                                      Collection<? extends GrantedAuthority> authorities,
                                      boolean trocaSenhaObrigatoria) {
        super(username, password, enabled, true, true, accountNonLocked, authorities);
        this.trocaSenhaObrigatoria = trocaSenhaObrigatoria;
    }

    public boolean isTrocaSenhaObrigatoria() {
        return trocaSenhaObrigatoria;
    }
}
