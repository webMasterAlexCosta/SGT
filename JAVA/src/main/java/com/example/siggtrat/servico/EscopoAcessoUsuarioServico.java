package com.example.siggtrat.servico;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Resolve o escopo operacional do usuário.
 *
 * A GestaoLocal enxerga todas as suas lotações. O usuário operacional enxerga apenas
 * as combinações lotação/turno explicitamente atribuídas a ele. Presidência e
 * Superintendência são perfis gerenciais e, portanto, não recebem permissão de
 * lançamento de frequência por este serviço.
 */
@Service
public class EscopoAcessoUsuarioServico implements com.example.siggtrat.diagnostico.DiagnosticoOperacao {
    private static final org.slf4j.Logger LOG_DIAGNOSTICO = org.slf4j.LoggerFactory.getLogger(EscopoAcessoUsuarioServico.class);

    @Override public org.slf4j.Logger loggerDiagnostico() { return LOG_DIAGNOSTICO; }


    public record Escopo(
            Long superintendenciaId,
            String superintendenciaNome,
            Long gestaoLocalId,
            String gestaoLocalNome,
            Long lotacaoId,
            String lotacaoNome,
            Long jornadaId,
            String jornadaDescricao
    ) {
        public boolean corresponde(Long targetGestaoLocalId, Long targetLotacaoId, Long targetJornadaId) {
            boolean gestaoLocalOk = gestaoLocalId == null || Objects.equals(gestaoLocalId, targetGestaoLocalId);
            boolean lotacaoOk = lotacaoId == null || Objects.equals(lotacaoId, targetLotacaoId);
            boolean jornadaOk = jornadaId == null || Objects.equals(jornadaId, targetJornadaId);
            return gestaoLocalOk && lotacaoOk && jornadaOk;
        }
    }

    public List<Escopo> obterEscopos(UsuarioAplicacao user) {
        if (user == null) return List.of();
        PerfilAcesso perfil = user.getPerfil();

        if (perfil == PerfilAcesso.ADMINISTRADOR || perfil == PerfilAcesso.GESTOR_NACIONAL) {
            return List.of(new Escopo(null, "Escopo nacional", null, "Todas", null, "Todas", null, "Todos"));
        }

        if (perfil == PerfilAcesso.GESTOR_LOCAL) {
            GestaoLocal gestaoLocal = user.getGestaoLocal();
            if (gestaoLocal == null) return List.of();
            return List.of(criarEscopo(gestaoLocal, null, null));
        }

        if (perfil != PerfilAcesso.OPERACIONAL) {
            return List.of();
        }

        GestaoLocal gestaoLocal = user.getGestaoLocal();
        if (gestaoLocal == null) return List.of();

        List<Escopo> scopes = new ArrayList<>();
        if (user.getVinculosGestao() != null) {
            for (VinculoGestaoUsuarioAplicacao vinculo : user.getVinculosGestao()) {
                if (vinculo == null || vinculo.getLotacao() == null) continue;
                if (vinculo.getLotacao().getGestaoLocal() == null
                        || !Objects.equals(gestaoLocal.getId(), vinculo.getLotacao().getGestaoLocal().getId())) continue;
                scopes.add(criarEscopo(gestaoLocal, vinculo.getLotacao(), vinculo.getJornada()));
            }
        }

        if (scopes.isEmpty() && user.getLotacao() != null) {
            if (user.getLotacao().getGestaoLocal() != null
                    && Objects.equals(gestaoLocal.getId(), user.getLotacao().getGestaoLocal().getId())) {
                scopes.add(criarEscopo(gestaoLocal, user.getLotacao(), user.getJornada()));
            }
        }
        return removerDuplicados(scopes);
    }

    public Escopo obterEscopoPrincipal(UsuarioAplicacao user) {
        List<Escopo> scopes = obterEscopos(user);
        return scopes.isEmpty() ? null : scopes.get(0);
    }

    public boolean podeGerenciar(UsuarioAplicacao user, Empregado empregado) {
        if (empregado == null) return false;
        return podeGerenciar(
                user,
                empregado.getGestaoLocal() != null ? empregado.getGestaoLocal().getId() : null,
                empregado.getLotacao() != null ? empregado.getLotacao().getId() : null,
                empregado.getJornada() != null ? empregado.getJornada().getId() : null
        );
    }

    public boolean podeConsultar(UsuarioAplicacao user, Empregado empregado) {
        if (user == null || empregado == null) return false;
        PerfilAcesso perfil = user.getPerfil();
        if (perfil == PerfilAcesso.ADMINISTRADOR || perfil == PerfilAcesso.GESTOR_NACIONAL) return true;
        if (perfil == PerfilAcesso.GESTOR_ESTADUAL) {
            Long superintendenciaUsuarioId = user.getSuperintendencia() != null
                    ? user.getSuperintendencia().getId()
                    : null;
            Long superintendenciaEmpregadoId = empregado.getGestaoLocal() != null
                    && empregado.getGestaoLocal().getSuperintendencia() != null
                    ? empregado.getGestaoLocal().getSuperintendencia().getId()
                    : null;
            return superintendenciaUsuarioId != null
                    && Objects.equals(superintendenciaUsuarioId, superintendenciaEmpregadoId);
        }
        return podeGerenciar(user, empregado);
    }

    public boolean podeGerenciar(UsuarioAplicacao user, Long lotacaoId, Long jornadaId) {
        Long gestaoLocalId = user != null && user.getGestaoLocal() != null ? user.getGestaoLocal().getId() : null;
        return podeGerenciar(user, gestaoLocalId, lotacaoId, jornadaId);
    }

    public boolean podeGerenciar(UsuarioAplicacao user, Long gestaoLocalId, Long lotacaoId, Long jornadaId) {
        if (user == null) return false;
        PerfilAcesso perfil = user.getPerfil();
        if (perfil != PerfilAcesso.GESTOR_LOCAL && perfil != PerfilAcesso.OPERACIONAL) return false;
        List<Escopo> scopes = obterEscopos(user);
        return scopes.stream().anyMatch(scope -> scope.corresponde(gestaoLocalId, lotacaoId, jornadaId));
    }


    /**
     * Verifica se o usuário pode consultar a lotação informada. Para a GestaoLocal,
     * uma lotação nula no escopo representa todas as lotações da própria GestaoLocal.
     * Para o usuário operacional, somente lotações explicitamente vinculadas
     * são aceitas.
     */
    public boolean podeAcessarLotacao(UsuarioAplicacao user, Long gestaoLocalId, Long lotacaoId) {
        if (user == null || lotacaoId == null) return false;
        PerfilAcesso perfil = user.getPerfil();
        if (perfil != PerfilAcesso.GESTOR_LOCAL && perfil != PerfilAcesso.OPERACIONAL) return false;
        return obterEscopos(user).stream().anyMatch(scope ->
                (scope.gestaoLocalId() == null || Objects.equals(scope.gestaoLocalId(), gestaoLocalId))
                        && (scope.lotacaoId() == null || Objects.equals(scope.lotacaoId(), lotacaoId)));
    }

    /**
     * Verifica se o usuário pode consultar o turno informado sem ampliar o
     * escopo operacional para outros turnos da mesma GestaoLocal.
     */
    public boolean podeAcessarJornada(UsuarioAplicacao user, Long gestaoLocalId, Long jornadaId) {
        if (user == null || jornadaId == null) return false;
        PerfilAcesso perfil = user.getPerfil();
        if (perfil != PerfilAcesso.GESTOR_LOCAL && perfil != PerfilAcesso.OPERACIONAL) return false;
        return obterEscopos(user).stream().anyMatch(scope ->
                (scope.gestaoLocalId() == null || Objects.equals(scope.gestaoLocalId(), gestaoLocalId))
                        && (scope.jornadaId() == null || Objects.equals(scope.jornadaId(), jornadaId)));
    }

    public boolean podeAcessarJornadaLotacao(UsuarioAplicacao user, Long gestaoLocalId, Long lotacaoId, Long jornadaId) {
        if (user == null || lotacaoId == null || jornadaId == null) return false;
        PerfilAcesso perfil = user.getPerfil();
        if (perfil != PerfilAcesso.GESTOR_LOCAL && perfil != PerfilAcesso.OPERACIONAL) return false;
        return obterEscopos(user).stream().anyMatch(scope -> scope.corresponde(gestaoLocalId, lotacaoId, jornadaId));
    }

    public Set<Long> obterIdsLotacoes(UsuarioAplicacao user) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Escopo scope : obterEscopos(user)) if (scope.lotacaoId() != null) ids.add(scope.lotacaoId());
        return ids;
    }

    public Set<Long> obterIdsJornadas(UsuarioAplicacao user) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Escopo scope : obterEscopos(user)) if (scope.jornadaId() != null) ids.add(scope.jornadaId());
        return ids;
    }

    public Map<Long, List<Escopo>> agruparEscoposPorLotacao(UsuarioAplicacao user) {
        Map<Long, List<Escopo>> grouped = new LinkedHashMap<>();
        for (Escopo scope : obterEscopos(user)) {
            grouped.computeIfAbsent(scope.lotacaoId(), k -> new ArrayList<>()).add(scope);
        }
        return grouped;
    }

    private Escopo criarEscopo(GestaoLocal gestaoLocal, Lotacao lotacao, Jornada jornada) {
        SuperintendenciaEstadual se = gestaoLocal != null ? gestaoLocal.getSuperintendencia() : null;
        return new Escopo(
                se != null ? se.getId() : null,
                se != null ? se.getNome() : null,
                gestaoLocal != null ? gestaoLocal.getId() : null,
                gestaoLocal != null ? gestaoLocal.getNome() : null,
                lotacao != null ? lotacao.getId() : null,
                lotacao != null ? lotacao.getNome() : null,
                jornada != null ? jornada.getId() : null,
                jornada != null ? jornada.getDescricao() : null
        );
    }

    private List<Escopo> removerDuplicados(List<Escopo> scopes) {
        Map<String, Escopo> unique = new LinkedHashMap<>();
        for (Escopo scope : scopes) {
            if (scope == null) continue;
            String key = scope.gestaoLocalId() + ":" + scope.lotacaoId() + ":" + scope.jornadaId();
            unique.putIfAbsent(key, scope);
        }
        return new ArrayList<>(unique.values());
    }
}
