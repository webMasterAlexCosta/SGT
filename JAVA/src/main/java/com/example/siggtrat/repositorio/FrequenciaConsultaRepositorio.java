package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.Frequencia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
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
 * Consulta paginada de frequência com opção de pular o COUNT nas páginas
 * seguintes. O React já envia skipCount=true depois da primeira página; antes
 * esse parâmetro era ignorado e o MySQL repetia a contagem completa em todas as
 * requisições do carregamento em lote.
 */
@Repository
public class FrequenciaConsultaRepositorio {

    private static final Set<String> ORDENACOES_PERMITIDAS = Set.of(
            "id", "dataReferencia", "empregadoSnapshotNome", "empregado.nome"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<Frequencia> buscar(
            Specification<Frequencia> specification,
            Pageable pageable,
            boolean skipCount
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Frequencia> query = cb.createQuery(Frequencia.class);
        Root<Frequencia> root = query.from(Frequencia.class);
        root.fetch("empregado", jakarta.persistence.criteria.JoinType.LEFT);

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);
        query.select(root);
        query.orderBy(criarOrdenacao(cb, root, pageable.getSort()));

        TypedQuery<Frequencia> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) Math.min(pageable.getOffset(), Integer.MAX_VALUE));
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Frequencia> conteudo = typedQuery.getResultList();

        long total = skipCount
                ? pageable.getOffset() + conteudo.size()
                    + (conteudo.size() == pageable.getPageSize() ? 1L : 0L)
                : contar(specification);
        return new PageImpl<>(conteudo, pageable, total);
    }

    private long contar(Specification<Frequencia> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Frequencia> root = query.from(Frequencia.class);

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(root, query, cb);
        if (predicate != null) query.where(predicate);
        query.select(cb.count(root.get("id")));
        return entityManager.createQuery(query).getSingleResult();
    }

    private List<Order> criarOrdenacao(CriteriaBuilder cb, Root<Frequencia> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        if (sort != null) {
            for (Sort.Order item : sort) {
                if (!ORDENACOES_PERMITIDAS.contains(item.getProperty())) continue;
                Path<?> path = resolverCaminho(root, item.getProperty());
                orders.add(item.isAscending() ? cb.asc(path) : cb.desc(path));
            }
        }
        if (orders.isEmpty()) orders.add(cb.asc(root.get("id")));
        return orders;
    }

    private Path<?> resolverCaminho(Path<?> root, String property) {
        Path<?> atual = root;
        for (String parte : property.split("\\.")) {
            atual = atual.get(parte);
        }
        return atual;
    }
}
