package com.example.siggtrat.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Representação simples e estável de uma página para armazenamento no Redis.
 *
 * O PageImpl contém metadados internos do Spring Data que não são uma boa
 * fronteira de serialização. No cache guardamos somente o conteúdo e o total;
 * o Pageable original da requisição reconstrói a resposta HTTP no controller.
 */
public record PaginaCacheDTO<T>(List<T> content, long totalElements) {

    public PaginaCacheDTO {
        content = content == null ? List.of() : List.copyOf(content);
        totalElements = Math.max(totalElements, content.size());
    }

    public static <T> PaginaCacheDTO<T> dePagina(Page<T> page) {
        if (page == null) {
            return new PaginaCacheDTO<>(List.of(), 0L);
        }
        return new PaginaCacheDTO<>(page.getContent(), page.getTotalElements());
    }

    public Page<T> paraPagina(Pageable pageable) {
        return new PageImpl<>(content, pageable, totalElements);
    }
}
