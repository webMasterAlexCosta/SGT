package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAuditoriaRepositorio extends JpaRepository<RegistroAuditoria, Long> {
    Page<RegistroAuditoria> findAll(Specification<RegistroAuditoria> spec, Pageable pageable);
}
