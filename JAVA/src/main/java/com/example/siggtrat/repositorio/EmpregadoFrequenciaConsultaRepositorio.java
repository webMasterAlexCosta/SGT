package com.example.siggtrat.repositorio;

import com.example.siggtrat.dto.EmpregadoFrequenciaDTO;
import com.example.siggtrat.entidade.Cargo;
import com.example.siggtrat.entidade.Empregado;
import com.example.siggtrat.entidade.Jornada;
import com.example.siggtrat.entidade.Lotacao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Projeção enxuta usada pela grade de frequência.
 *
 * Evita materializar a entidade Empregado e inicializar relações LAZY para cada
 * linha. Em escalas grandes, transfere apenas os nove campos realmente usados
 * pelo React e preserva paginação estável por nome e id.
 */
@Repository
public class EmpregadoFrequenciaConsultaRepositorio {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<EmpregadoFrequenciaDTO> buscar(
            Specification<Empregado> specification,
            Pageable pageable,
            boolean skipCount) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EmpregadoFrequenciaDTO> query = cb.createQuery(EmpregadoFrequenciaDTO.class);
        Root<Empregado> empregado = query.from(Empregado.class);
        Join<Empregado, Lotacao> lotacao = empregado.join("lotacao", JoinType.LEFT);
        Join<Empregado, Jornada> jornada = empregado.join("jornada", JoinType.LEFT);
        Join<Empregado, Cargo> cargo = empregado.join("cargo", JoinType.LEFT);

        query.select(cb.construct(
                EmpregadoFrequenciaDTO.class,
                empregado.get("id"),
                empregado.get("nome"),
                empregado.get("matricula"),
                empregado.get("cpf"),
                lotacao.get("nome"),
                lotacao.get("id"),
                jornada.get("descricao"),
                jornada.get("id"),
                cargo.get("nome")
        ));

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(empregado, query, cb);
        if (predicate != null) query.where(predicate);

        query.orderBy(cb.asc(empregado.get("nome")), cb.asc(empregado.get("id")));

        TypedQuery<EmpregadoFrequenciaDTO> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) Math.min(pageable.getOffset(), Integer.MAX_VALUE));
        typedQuery.setMaxResults(pageable.getPageSize());

        List<EmpregadoFrequenciaDTO> conteudo = typedQuery.getResultList();
        long total = skipCount
                ? pageable.getOffset() + conteudo.size()
                    + (conteudo.size() == pageable.getPageSize() ? 1 : 0)
                : contar(specification);
        return new PageImpl<>(conteudo, pageable, total);
    }

    private long contar(Specification<Empregado> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Empregado> empregado = query.from(Empregado.class);

        Predicate predicate = specification == null
                ? cb.conjunction()
                : specification.toPredicate(empregado, query, cb);
        if (predicate != null) query.where(predicate);

        query.select(cb.count(empregado.get("id")));
        return entityManager.createQuery(query).getSingleResult();
    }
}
