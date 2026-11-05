package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.EmpregadoListaBaseDTO;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import com.example.siggtrat.entidade.StatusEmpregado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Consulta dedicada à grade de colaboradores.
 *
 * Seleciona apenas os campos exibidos na tela. Conta, endereço, telefones e
 * demais dados do cadastro completo não são materializados nesta consulta.
 */
@Repository
public class EmpregadoListaConsultaRepositorio {

    private static final Set<String> ORDENACOES_PERMITIDAS = Set.of(
            "id", "nome", "matricula", "cpf"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<EmpregadoListaBaseDTO> buscar(
            Specification<Empregado> specification,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EmpregadoListaBaseDTO> query = cb.createQuery(EmpregadoListaBaseDTO.class);
        Root<Empregado> root = query.from(Empregado.class);

        Join<Empregado, StatusEmpregado> status = root.join("status", JoinType.LEFT);
        Join<Empregado, Lotacao> lotacao = root.join("lotacao", JoinType.LEFT);
        Join<Empregado, Cargo> cargo = root.join("cargo", JoinType.LEFT);
        Join<Empregado, Jornada> jornada = root.join("jornada", JoinType.LEFT);

        query.select(cb.construct(
                EmpregadoListaBaseDTO.class,
                root.get("id"),
                root.get("nome"),
                root.get("matricula"),
                root.get("cpf"),
                status.get("id"),
                status.get("descricao"),
                lotacao.get("id"),
                lotacao.get("nome"),
                cargo.get("id"),
                cargo.get("nome"),
                jornada.get("id"),
                jornada.get("descricao")
        ));

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);

        // Os filtros da grade usam EXISTS para coleções; não há multiplicação
        // de linhas. DISTINCT forçava temporary table/filesort no MySQL sobre
        // centenas de milhares de empregados sem necessidade.
        query.orderBy(criarOrdenacao(cb, root, pageable.getSort()));

        TypedQuery<EmpregadoListaBaseDTO> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) Math.min(pageable.getOffset(), Integer.MAX_VALUE));
        typedQuery.setMaxResults(pageable.getPageSize());

        List<EmpregadoListaBaseDTO> conteudo = typedQuery.getResultList();
        long total = contar(specification);
        return new PageImpl<>(conteudo, pageable, total);
    }

    /**
     * Retorna apenas o conteúdo da página. Usado quando o total já está no
     * Redis, evitando repetir o COUNT ao navegar entre páginas.
     */
    @Transactional(readOnly = true)
    public List<EmpregadoListaBaseDTO> buscarConteudo(
            Specification<Empregado> specification,
            Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EmpregadoListaBaseDTO> query = cb.createQuery(EmpregadoListaBaseDTO.class);
        Root<Empregado> root = query.from(Empregado.class);

        Join<Empregado, StatusEmpregado> status = root.join("status", JoinType.LEFT);
        Join<Empregado, Lotacao> lotacao = root.join("lotacao", JoinType.LEFT);
        Join<Empregado, Cargo> cargo = root.join("cargo", JoinType.LEFT);
        Join<Empregado, Jornada> jornada = root.join("jornada", JoinType.LEFT);

        query.select(cb.construct(
                EmpregadoListaBaseDTO.class,
                root.get("id"), root.get("nome"), root.get("matricula"), root.get("cpf"),
                status.get("id"), status.get("descricao"),
                lotacao.get("id"), lotacao.get("nome"),
                cargo.get("id"), cargo.get("nome"),
                jornada.get("id"), jornada.get("descricao")
        ));

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);
        query.orderBy(criarOrdenacao(cb, root, pageable.getSort()));

        TypedQuery<EmpregadoListaBaseDTO> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) Math.min(pageable.getOffset(), Integer.MAX_VALUE));
        typedQuery.setMaxResults(pageable.getPageSize());
        return typedQuery.getResultList();
    }

    @Transactional(readOnly = true)
    public long contar(Specification<Empregado> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Empregado> countRoot = countQuery.from(Empregado.class);

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(countRoot, countQuery, cb);
        if (predicate != null) countQuery.where(predicate);

        countQuery.select(cb.count(countRoot.get("id")));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Order> criarOrdenacao(CriteriaBuilder cb, Root<Empregado> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        if (sort != null) {
            for (Sort.Order item : sort) {
                if (!ORDENACOES_PERMITIDAS.contains(item.getProperty())) continue;
                orders.add(item.isAscending()
                        ? cb.asc(root.get(item.getProperty()))
                        : cb.desc(root.get(item.getProperty())));
            }
        }
        if (orders.isEmpty()) orders.add(cb.asc(root.get("id")));
        return orders;
    }
}
