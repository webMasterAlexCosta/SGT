package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.acesso.TipoPermissaoExcepcional;
import com.example.siggtrat.entidade.PermissaoExcepcional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PermissaoExcepcionalRepositorio extends JpaRepository<PermissaoExcepcional, Long> {
    List<PermissaoExcepcional> findAllByOrderByConcedidoEmDescIdDesc();
    List<PermissaoExcepcional> findByUsuarioBeneficiarioIdOrderByConcedidoEmDescIdDesc(Long usuarioId);

    @Query("""
            select p from PermissaoExcepcional p
            where p.usuarioBeneficiario.id = :usuarioId and p.tipo = :tipo
              and p.ativo = true and p.validoAte > :agora
            order by p.concedidoEm desc, p.id desc
            """)
    List<PermissaoExcepcional> buscarAtivas(@Param("usuarioId") Long usuarioId,
                                             @Param("tipo") TipoPermissaoExcepcional tipo,
                                             @Param("agora") Instant agora);
}
