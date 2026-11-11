package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.seguranca.DetalhesUsuarioAutenticado;
import com.example.siggtrat.utilitario.UtilitarioMatricula;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/** Carrega a identidade e o estado real da conta a cada autenticação. */
@Service
public class DetalhesUsuarioPersonalizadoServico implements UserDetailsService, com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(DetalhesUsuarioPersonalizadoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final UsuarioAplicacaoRepositorio userRepositorio;

    public DetalhesUsuarioPersonalizadoServico(UsuarioAplicacaoRepositorio userRepositorio) {
        this.userRepositorio = userRepositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String matriculaInformada) throws UsernameNotFoundException {
        final String matricula;
        try {
            matricula = UtilitarioMatricula.normalizar(matriculaInformada);
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("Matrícula inválida.");
        }
        if (matricula == null) throw new UsernameNotFoundException("Matrícula inválida.");

        UsuarioAplicacao appUser = userRepositorio.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException("Matrícula não encontrada."));
        return construirDetalhesUsuario(appUser);
    }

    @Transactional(readOnly = true)
    public UserDetails carregarUsuarioPorId(Long id) throws UsernameNotFoundException {
        UsuarioAplicacao appUser = userRepositorio.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com id: " + id));
        return construirDetalhesUsuario(appUser);
    }

    private UserDetails construirDetalhesUsuario(UsuarioAplicacao appUser) {
        PerfilAcesso perfil = appUser.getPerfil() != null ? appUser.getPerfil() : PerfilAcesso.OPERACIONAL;
        Collection<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(perfil.autoridade()));

        boolean enabled = Boolean.TRUE.equals(appUser.getActive())
                && estruturaOrganizacionalAtiva(appUser, perfil);
        boolean accountNonLocked = appUser.getLockoutEnd() == null
                || !com.example.siggtrat.utilitario.RelogioSistema.agora().isBefore(appUser.getLockoutEnd());

        return new DetalhesUsuarioAutenticado(
                appUser.getUsername(),
                appUser.getPassword(),
                enabled,
                accountNonLocked,
                authorities,
                Boolean.TRUE.equals(appUser.getForceChangePassword())
        );
    }

    private boolean estruturaOrganizacionalAtiva(UsuarioAplicacao usuario, PerfilAcesso perfil) {
        if (perfil == PerfilAcesso.ADMINISTRADOR || perfil == PerfilAcesso.GESTOR_NACIONAL
                || perfil == PerfilAcesso.PRESIDENTE) {
            return true;
        }
        if (perfil == PerfilAcesso.GESTOR_ESTADUAL) {
            return usuario.getSuperintendencia() != null
                    && Boolean.TRUE.equals(usuario.getSuperintendencia().getAtivo());
        }
        if (perfil == PerfilAcesso.GESTOR_LOCAL || perfil == PerfilAcesso.OPERACIONAL) {
            return usuario.getGestaoLocal() != null
                    && Boolean.TRUE.equals(usuario.getGestaoLocal().getAtivo())
                    && usuario.getGestaoLocal().getSuperintendencia() != null
                    && Boolean.TRUE.equals(usuario.getGestaoLocal().getSuperintendencia().getAtivo());
        }
        return false;
    }
}
