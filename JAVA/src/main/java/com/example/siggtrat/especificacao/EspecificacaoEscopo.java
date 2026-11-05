package com.example.siggtrat.especificacao;

import com.example.siggtrat.dominio.acesso.PerfilAcesso;
import com.example.siggtrat.entidade.UsuarioAplicacao;
import com.example.siggtrat.servico.EscopoAcessoUsuarioServico;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EspecificacaoEscopo {

    public static <T> Specification<T> filtrarPorEscopo(UsuarioAplicacao user, EscopoAcessoUsuarioServico scopeServico) {
        return (root, query, cb) -> {
            if (user != null && user.getPerfil() == PerfilAcesso.GESTOR_ESTADUAL) {
                if (user.getSuperintendencia() == null || user.getSuperintendencia().getId() == null) {
                    return cb.disjunction();
                }
                Join<Object, Object> empregadoJoin = root.join("empregado", JoinType.INNER);
                return cb.equal(
                        empregadoJoin.get("gestaoLocal").get("superintendencia").get("id"),
                        user.getSuperintendencia().getId());
            }
            if (user != null && (user.getPerfil() == PerfilAcesso.ADMINISTRADOR
                    || user.getPerfil() == PerfilAcesso.GESTOR_NACIONAL)) {
                return cb.conjunction();
            }
            List<EscopoAcessoUsuarioServico.Escopo> scopes = scopeServico.obterEscopos(user);
            if (scopes == null || scopes.isEmpty()) return cb.disjunction();

            // A relacao raiz -> empregado e ManyToOne e, portanto, nao multiplica linhas.
            // Nao use DISTINCT aqui: a frequencia ordena tambem por empregado.nome,
            // e o MySQL rejeita DISTINCT + ORDER BY de coluna associada fora do SELECT.
            Join<Object, Object> empregadoJoin = root.join("empregado", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            for (EscopoAcessoUsuarioServico.Escopo scope : scopes) {
                if (scope.gestaoLocalId() == null) return cb.conjunction();

                Predicate escopo = cb.equal(empregadoJoin.get("gestaoLocal").get("id"), scope.gestaoLocalId());
                if (scope.lotacaoId() != null) {
                    escopo = cb.and(escopo, cb.equal(empregadoJoin.get("lotacao").get("id"), scope.lotacaoId()));
                }
                if (scope.jornadaId() != null) {
                    escopo = cb.and(escopo, cb.equal(empregadoJoin.get("jornada").get("id"), scope.jornadaId()));
                }
                predicates.add(escopo);
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
