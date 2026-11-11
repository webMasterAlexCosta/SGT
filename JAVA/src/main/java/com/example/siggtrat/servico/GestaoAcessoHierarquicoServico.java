package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.dto.AtivacaoUsuarioAplicacaoDTO;
import com.example.siggtrat.entidade.GestaoLocal;
import com.example.siggtrat.entidade.SuperintendenciaEstadual;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.repositorio.GestaoLocalRepositorio;
import com.example.siggtrat.repositorio.SuperintendenciaEstadualRepositorio;
import com.example.siggtrat.repositorio.TokenAtivoRepositorio;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import com.example.siggtrat.seguranca.FiltroAutenticacaoJwt;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

/**
 * Mantém o ciclo de acesso da hierarquia organizacional consistente.
 * Cada nível somente pode ser reativado pelo seu gestor e recebe uma nova
 * senha temporária; a reativação nunca se propaga para os níveis inferiores.
 */
@Service
@Transactional
public class GestaoAcessoHierarquicoServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(GestaoAcessoHierarquicoServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    private final UsuarioAplicacaoRepositorio usuarioRepositorio;
    private final SuperintendenciaEstadualRepositorio superintendenciaRepositorio;
    private final GestaoLocalRepositorio gestaoLocalRepositorio;
    private final TokenAtivoRepositorio tokenAtivoRepositorio;
    private final FiltroAutenticacaoJwt filtroAutenticacaoJwt;
    private final PasswordEncoder codificadorSenha;
    private final GeradorSenhaTemporariaServico geradorSenhaTemporaria;

    public GestaoAcessoHierarquicoServico(
            UsuarioAplicacaoRepositorio usuarioRepositorio,
            SuperintendenciaEstadualRepositorio superintendenciaRepositorio,
            GestaoLocalRepositorio gestaoLocalRepositorio,
            TokenAtivoRepositorio tokenAtivoRepositorio,
            FiltroAutenticacaoJwt filtroAutenticacaoJwt,
            PasswordEncoder codificadorSenha,
            GeradorSenhaTemporariaServico geradorSenhaTemporaria
    ) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.superintendenciaRepositorio = superintendenciaRepositorio;
        this.gestaoLocalRepositorio = gestaoLocalRepositorio;
        this.tokenAtivoRepositorio = tokenAtivoRepositorio;
        this.filtroAutenticacaoJwt = filtroAutenticacaoJwt;
        this.codificadorSenha = codificadorSenha;
        this.geradorSenhaTemporaria = geradorSenhaTemporaria;
    }

    public void inativarSuperintendencia(Long superintendenciaId) {
        SuperintendenciaEstadual superintendencia = superintendenciaRepositorio.findById(superintendenciaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Gestor estadual não encontrado."));
        superintendencia.setAtivo(false);
        superintendenciaRepositorio.save(superintendencia);

        List<GestaoLocal> gestoesLocais = gestaoLocalRepositorio
                .findBySuperintendenciaIdOrderByNomeAsc(superintendenciaId);
        gestoesLocais.forEach(gestaoLocal -> gestaoLocal.setAtivo(false));
        gestaoLocalRepositorio.saveAll(gestoesLocais);

        inativarContas(usuarioRepositorio
                .buscarUsuariosVinculadosASuperintendencia(superintendenciaId));
    }

    public void inativarGestaoLocal(Long gestaoLocalId) {
        GestaoLocal gestaoLocal = gestaoLocalRepositorio.findById(gestaoLocalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Gestão Local não encontrada."));
        gestaoLocal.setAtivo(false);
        gestaoLocalRepositorio.save(gestaoLocal);
        inativarContas(usuarioRepositorio.buscarUsuariosVinculadosAoGestaoLocal(gestaoLocalId));
    }

    public void inativarUsuario(UsuarioAplicacao usuario) {
        if (usuario == null) return;
        inativarContas(List.of(usuario));
    }

    public AtivacaoUsuarioAplicacaoDTO ativarGestorEstadual(Long superintendenciaId) {
        List<UsuarioAplicacao> gestores = usuarioRepositorio
                .findByPerfilAndSuperintendenciaIdOrderByUsernameAsc(
                        PerfilAcesso.GESTOR_ESTADUAL, superintendenciaId);
        if (gestores.size() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A reativação exige exatamente um usuário gestor estadual vinculado à unidade.");
        }
        inativarSuperintendencia(superintendenciaId);
        return ativarComSenhaTemporaria(gestores.get(0));
    }

    public AtivacaoUsuarioAplicacaoDTO ativarComSenhaTemporaria(UsuarioAplicacao usuario) {
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }
        if (Boolean.TRUE.equals(usuario.getActive())) {
            throw new com.example.siggtrat.excecao.ExcecaoValidacaoNegocio("O usuário já está ativo.");
        }

        ativarEstruturaDoNivel(usuario);
        String senhaTemporaria = geradorSenhaTemporaria.gerar();
        usuario.setPassword(codificadorSenha.encode(senhaTemporaria));
        usuario.setActive(true);
        usuario.setForceChangePassword(true);
        usuario.setFailedAttempts(0);
        usuario.setLockoutEnd(null);
        usuario.setRetroactiveUnlockExpireAt(null);
        usuarioRepositorio.save(usuario);
        revogarSessoes(usuario);

        return new AtivacaoUsuarioAplicacaoDTO(
                "Usuário ativado. Entregue a senha temporária por um canal seguro.",
                usuario.getUsername(),
                senhaTemporaria,
                true
        );
    }

    private void ativarEstruturaDoNivel(UsuarioAplicacao usuario) {
        PerfilAcesso perfil = usuario.getPerfil();
        if (perfil == PerfilAcesso.GESTOR_ESTADUAL) {
            SuperintendenciaEstadual superintendencia = usuario.getSuperintendencia();
            if (superintendencia == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "O gestor estadual não possui unidade vinculada.");
            }
            superintendencia.setAtivo(true);
            superintendenciaRepositorio.save(superintendencia);
            return;
        }

        if (perfil == PerfilAcesso.GESTOR_LOCAL) {
            GestaoLocal gestaoLocal = exigirGestaoLocalComSuperintendenciaAtiva(usuario);
            if (!Boolean.TRUE.equals(gestaoLocal.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reative primeiro a Gestão Local antes de reativar a conta deste gestor.");
            }
            return;
        }

        if (perfil == PerfilAcesso.OPERACIONAL) {
            GestaoLocal gestaoLocal = exigirGestaoLocalComSuperintendenciaAtiva(usuario);
            if (!Boolean.TRUE.equals(gestaoLocal.getAtivo())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reative primeiro a Gestão Local responsável por este usuário operacional.");
            }
        }
    }

    private GestaoLocal exigirGestaoLocalComSuperintendenciaAtiva(UsuarioAplicacao usuario) {
        GestaoLocal gestaoLocal = usuario.getGestaoLocal();
        if (gestaoLocal == null || gestaoLocal.getSuperintendencia() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O usuário não possui Gestão Local e Gestão Estadual vinculadas.");
        }
        if (!Boolean.TRUE.equals(gestaoLocal.getSuperintendencia().getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Reative primeiro o gestor estadual responsável por este usuário.");
        }
        return gestaoLocal;
    }

    private void inativarContas(Collection<UsuarioAplicacao> usuarios) {
        if (usuarios == null || usuarios.isEmpty()) return;
        // A senha é deliberadamente descartada e nunca é exibida. Um único
        // hash por operação evita executar o algoritmo de custo elevado para
        // cada integrante de equipes grandes; toda reativação o substitui por
        // uma credencial temporária individual.
        String hashSenhaDescartada = codificadorSenha.encode(geradorSenhaTemporaria.gerar());
        usuarios.forEach(usuario -> {
            usuario.setActive(false);
            usuario.setPassword(hashSenhaDescartada);
            usuario.setForceChangePassword(true);
            usuario.setFailedAttempts(0);
            usuario.setLockoutEnd(null);
            usuario.setRetroactiveUnlockExpireAt(null);
            usuario.setSecret2fa(null);
            usuario.setPendingSecret2fa(null);
            usuario.setTwoFactorEnabled(false);
        });
        usuarioRepositorio.saveAll(usuarios);
        usuarios.forEach(this::revogarSessoes);
    }

    private void revogarSessoes(UsuarioAplicacao usuario) {
        if (usuario == null || usuario.getUsername() == null || usuario.getUsername().isBlank()) return;
        tokenAtivoRepositorio.deleteByUsername(usuario.getUsername());
        filtroAutenticacaoJwt.removerSessoesUsuarioDoCache(usuario.getUsername());
    }
}
