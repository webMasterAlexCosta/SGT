package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import com.example.siggtrat.repositorio.UsuarioAplicacaoRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AcessoOrganizacionalServico {

    private final UsuarioAplicacaoRepositorio userRepositorio;
    private final EscopoAcessoUsuarioServico userAccessScopeServico;

    public AcessoOrganizacionalServico(UsuarioAplicacaoRepositorio userRepositorio,
                                       EscopoAcessoUsuarioServico userAccessScopeServico) {
        this.userRepositorio = userRepositorio;
        this.userAccessScopeServico = userAccessScopeServico;
    }

    public UsuarioAplicacao usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        return userRepositorio.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado."));
    }

    public PerfilAcesso perfil(UsuarioAplicacao usuario) {
        return usuario == null ? null : usuario.getPerfil();
    }

    public boolean possuiPerfil(UsuarioAplicacao usuario, PerfilAcesso esperado) {
        return usuario != null && esperado != null && esperado == usuario.getPerfil();
    }

    public boolean ehAdministrador(UsuarioAplicacao usuario) {
        return possuiPerfil(usuario, PerfilAcesso.ADMINISTRADOR);
    }

    public boolean ehGestorNacional(UsuarioAplicacao usuario) {
        return possuiPerfil(usuario, PerfilAcesso.GESTOR_NACIONAL);
    }

    public boolean ehPresidente(UsuarioAplicacao usuario) {
        return possuiPerfil(usuario, PerfilAcesso.PRESIDENTE);
    }

    /** Acesso global de leitura; nunca implica escrita negocial. */
    public boolean ehGlobal(UsuarioAplicacao usuario) {
        return ehAdministrador(usuario) || ehGestorNacional(usuario);
    }

    public boolean podeVisualizarTodasSuperintendencias(UsuarioAplicacao user) {
        return ehAdministrador(user) || ehGestorNacional(user) || ehPresidente(user);
    }
    public boolean ehGestorEstadual(UsuarioAplicacao user) { return possuiPerfil(user, PerfilAcesso.GESTOR_ESTADUAL); }
    public boolean ehGestaoLocal(UsuarioAplicacao user) { return possuiPerfil(user, PerfilAcesso.GESTOR_LOCAL); }
    public boolean ehOperacional(UsuarioAplicacao user) { return possuiPerfil(user, PerfilAcesso.OPERACIONAL); }

    public boolean podeVisualizarPainelGestao(UsuarioAplicacao user) {
        return podeVisualizarTodasSuperintendencias(user) || ehGestorEstadual(user)
                || (ehGestaoLocal(user) && possuiGestaoLocalAtiva(user));
    }

    public boolean podeAcessarRelatoriosGestao(UsuarioAplicacao user) {
        return ehAdministrador(user) || ehGestorNacional(user) || ehPresidente(user)
                || ehGestorEstadual(user) || (ehGestaoLocal(user) && possuiGestaoLocalAtiva(user));
    }

    /**
     * Escopo interno de alocação dos relatórios gerenciais. O usuário
     * operacional não acessa os endpoints de relatório; a validação abaixo
     * continua útil para os dados auxiliares da frequência diária.
     */
    public boolean podeAcessarAlocacaoRelatorio(UsuarioAplicacao user, Contrato contrato,
                                                 Lotacao lotacao, Jornada jornada) {
        if (!podeAcessarContrato(user, contrato)) return false;
        if (ehAdministrador(user) || ehGestorNacional(user)) return true;
        if (ehPresidente(user)) return true;
        if (ehGestorEstadual(user) || ehGestaoLocal(user)) {
            boolean lotacaoOk = lotacao == null || podeAcessarLotacao(user, lotacao);
            boolean jornadaOk = jornada == null || podeAcessarJornada(user, jornada);
            return lotacaoOk && jornadaOk;
        }
        if (!ehOperacional(user) || lotacao == null || jornada == null) return false;
        GestaoLocal gestaoLocal = contrato != null ? contrato.getGestaoLocal() : null;
        Long gestaoLocalId = gestaoLocal != null ? gestaoLocal.getId() : null;
        return userAccessScopeServico.podeAcessarJornadaLotacao(
                user, gestaoLocalId, lotacao.getId(), jornada.getId());
    }

    public boolean podeUsarFrequenciaDiaria(UsuarioAplicacao user) {
        // Escrita negocial de frequência pertence ao Gestor Local e ao Operacional.
        // Administrador é perfil técnico e não atua como responsável negocial por padrão.
        return (ehGestaoLocal(user) && possuiGestaoLocalAtiva(user))
                || (ehOperacional(user) && possuiGestaoLocalAtiva(user));
    }

    public boolean podeConsultarFrequencia(UsuarioAplicacao user) {
        return ehAdministrador(user) || ehGestorNacional(user) || ehGestorEstadual(user)
                || (ehGestaoLocal(user) && possuiGestaoLocalAtiva(user))
                || (ehOperacional(user) && possuiGestaoLocalAtiva(user));
    }

    public boolean podeConsultarEmpregados(UsuarioAplicacao user) {
        return ehAdministrador(user) || ehGestorNacional(user) || ehGestorEstadual(user)
                || (ehGestaoLocal(user) && possuiGestaoLocalAtiva(user))
                || (ehOperacional(user) && possuiGestaoLocalAtiva(user));
    }

    public boolean podeGerenciarEmpregados(UsuarioAplicacao user) {
        return ehGestaoLocal(user) && possuiGestaoLocalAtiva(user);
    }

    public boolean podeConsultarContratos(UsuarioAplicacao user) {
        // O usuário operacional precisa consultar os contratos do próprio escopo
        // para montar a tela de frequência, mas não pode alterá-los.
        // A Presidência possui consulta nacional dos contratos, sem herdar acesso
        // operacional a lotações, jornadas ou empregados.
        return ehAdministrador(user) || ehGestorNacional(user) || ehPresidente(user)
                || ehGestorEstadual(user) || ehGestaoLocal(user) || ehOperacional(user);
    }

    public boolean podeGerenciarContratos(UsuarioAplicacao user) {
        return ehGestaoLocal(user) && possuiGestaoLocalAtiva(user);
    }

    public boolean podeGerenciarCalendario(UsuarioAplicacao user) {
        return ehGestaoLocal(user) && possuiGestaoLocalAtiva(user);
    }

    public SuperintendenciaEstadual superintendenciaEfetiva(UsuarioAplicacao user) {
        if (user == null) return null;
        if (user.getSuperintendencia() != null) return user.getSuperintendencia();
        return user.getGestaoLocal() != null ? user.getGestaoLocal().getSuperintendencia() : null;
    }

    public GestaoLocal gestaoLocalEfetiva(UsuarioAplicacao user) { return user != null ? user.getGestaoLocal() : null; }

    public boolean podeAcessarSuperintendencia(UsuarioAplicacao user, Long superintendenciaId) {
        if (superintendenciaId == null) return false;
        if (podeVisualizarTodasSuperintendencias(user)) return true;
        SuperintendenciaEstadual se = superintendenciaEfetiva(user);
        return se != null && Objects.equals(se.getId(), superintendenciaId);
    }

    public boolean podeAcessarGestaoLocal(UsuarioAplicacao user, GestaoLocal gestaoLocal) {
        if (gestaoLocal == null || gestaoLocal.getId() == null) return false;
        if (ehAdministrador(user) || ehGestorNacional(user)) return true;
        if (ehPresidente(user)) return false;
        if (ehGestorEstadual(user)) {
            return gestaoLocal.getSuperintendencia() != null
                    && podeAcessarSuperintendencia(user, gestaoLocal.getSuperintendencia().getId());
        }
        return Boolean.TRUE.equals(gestaoLocal.getAtivo())
                && possuiGestaoLocalAtiva(user)
                && Objects.equals(user.getGestaoLocal().getId(), gestaoLocal.getId());
    }

    public boolean podeAcessarLotacao(UsuarioAplicacao user, Lotacao lotacao) {
        if (lotacao == null) return false;
        if (ehAdministrador(user) || ehGestorNacional(user)) return true;
        if (lotacao.getGestaoLocal() == null || !podeAcessarGestaoLocal(user, lotacao.getGestaoLocal())) return false;
        if (ehOperacional(user)) {
            return userAccessScopeServico.podeAcessarLotacao(user, lotacao.getGestaoLocal().getId(), lotacao.getId());
        }
        return true;
    }

    public boolean podeAcessarJornada(UsuarioAplicacao user, Jornada jornada) {
        if (jornada == null) return false;
        if (ehAdministrador(user) || ehGestorNacional(user)) return true;
        GestaoLocal gestaoLocal = jornada.getGestaoLocal();
        if (gestaoLocal != null && !podeAcessarGestaoLocal(user, gestaoLocal)) return false;
        if (!ehOperacional(user)) return gestaoLocal != null && podeAcessarGestaoLocal(user, gestaoLocal);
        Long gestaoLocalId = gestaoLocal != null ? gestaoLocal.getId() : user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
        return userAccessScopeServico.podeAcessarJornada(user, gestaoLocalId, jornada.getId());
    }

    /**
     * Valida o par lotação/turno do lançamento. Para o perfil OPERACIONAL a combinação
     * precisa existir no mesmo vínculo, sem ampliar o escopo operacional.
     */
    public boolean podeLancarFrequenciaDiaria(UsuarioAplicacao user, Lotacao lotacao, Jornada jornada) {
        if (!podeUsarFrequenciaDiaria(user) || lotacao == null || jornada == null) return false;
        GestaoLocal gestaoLocalLotacao = lotacao.getGestaoLocal();
        GestaoLocal gestaoLocalJornada = jornada.getGestaoLocal();
        if (gestaoLocalLotacao == null || gestaoLocalJornada == null
                || !Objects.equals(gestaoLocalLotacao.getId(), gestaoLocalJornada.getId())) {
            return false;
        }
        if (ehOperacional(user)) {
            return userAccessScopeServico.podeAcessarJornadaLotacao(
                    user,
                    gestaoLocalLotacao.getId(),
                    lotacao.getId(),
                    jornada.getId()
            );
        }
        return podeAcessarLotacao(user, lotacao) && podeAcessarJornada(user, jornada);
    }

    public boolean podeAcessarContrato(UsuarioAplicacao user, Contrato contrato) {
        if (!podeConsultarContratos(user) || contrato == null) return false;
        // Contrato é uma exceção deliberada ao escopo operacional da Presidência:
        // ela pode consultar qualquer contrato do país, mas continua sem acesso
        // direto aos demais recursos locais das Gestões Locais.
        if (ehAdministrador(user) || ehGestorNacional(user) || ehPresidente(user)) return true;
        return contrato.getGestaoLocal() != null && podeAcessarGestaoLocal(user, contrato.getGestaoLocal());
    }

    public boolean podeAcessarEmpregado(UsuarioAplicacao user, Empregado empregado) {
        if (!podeConsultarEmpregados(user) || empregado == null) return false;
        if (ehAdministrador(user) || ehGestorNacional(user)) return true;
        if (empregado.getGestaoLocal() == null) return false;
        if (!podeAcessarGestaoLocal(user, empregado.getGestaoLocal())) return false;
        return !ehOperacional(user) || userAccessScopeServico.podeGerenciar(user, empregado);
    }

    public void exigirConsultaContrato(UsuarioAplicacao user) {
        if (!podeConsultarContratos(user)) throw proibido("Seu perfil não possui acesso aos contratos.");
    }
    public void exigirGestaoContrato(UsuarioAplicacao user) {
        if (!podeGerenciarContratos(user)) throw proibido("Seu perfil não pode alterar contratos.");
    }
    public void exigirAcessoContrato(UsuarioAplicacao user, Contrato contrato) {
        if (!podeAcessarContrato(user, contrato)) throw naoEncontrado("Contrato não encontrado no seu escopo de acesso.");
    }
    public void exigirFrequenciaDiaria(UsuarioAplicacao user) {
        if (!podeUsarFrequenciaDiaria(user)) throw proibido("A frequência diária é operacional e não está disponível para este perfil.");
    }
    public void exigirConsultaFrequencia(UsuarioAplicacao user) {
        if (!podeConsultarFrequencia(user)) throw proibido("Seu perfil não possui acesso de leitura à frequência.");
    }
    public void exigirConsultaEmpregado(UsuarioAplicacao user, Empregado empregado) {
        if (!podeAcessarEmpregado(user, empregado)) throw naoEncontrado("Empregado não encontrado no seu escopo de acesso.");
    }
    public void exigirGestaoEmpregado(UsuarioAplicacao user) {
        if (!podeGerenciarEmpregados(user)) throw proibido("Somente a Gestão Local responsável pode gerir o cadastro dos empregados.");
    }
    public void exigirPainelGestao(UsuarioAplicacao user) {
        if (!podeVisualizarPainelGestao(user)) throw proibido("Seu perfil não possui acesso ao painel gerencial.");
    }

    private ResponseStatusException proibido(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }
    private ResponseStatusException naoEncontrado(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private boolean possuiGestaoLocalAtiva(UsuarioAplicacao user) {
        return user != null && user.getGestaoLocal() != null
                && Boolean.TRUE.equals(user.getGestaoLocal().getAtivo());
    }
}
