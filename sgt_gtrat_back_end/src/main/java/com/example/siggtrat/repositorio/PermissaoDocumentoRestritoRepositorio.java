package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.PermissaoDocumentoRestrito;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface PermissaoDocumentoRestritoRepositorio extends JpaRepository<PermissaoDocumentoRestrito, Long> {
    List<PermissaoDocumentoRestrito> findByGrupoLogicoIdOrderByConcedidoEmAscIdAsc(String grupoLogicoId);

    @Query("""
        select p from PermissaoDocumentoRestrito p
        where p.grupoLogicoId = :grupo and p.usuarioBeneficiario.id = :usuarioId
          and p.ativo = true and (p.validoAte is null or p.validoAte > :agora)
        order by p.concedidoEm desc, p.id desc
        """)
    List<PermissaoDocumentoRestrito> buscarAtivas(@Param("grupo") String grupo,
                                                   @Param("usuarioId") Long usuarioId,
                                                   @Param("agora") Instant agora);
}

