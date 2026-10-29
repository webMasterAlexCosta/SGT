package com.example.siggtrat.repositorio;

import com.example.siggtrat.dominio.documento.*;
import com.example.siggtrat.entidade.DocumentoComprobatorio;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface DocumentoComprobatorioRepositorio extends JpaRepository<DocumentoComprobatorio, Long> {
    List<DocumentoComprobatorio> findByContratoIdOrderByRegistradoEmAscIdAsc(Long contratoId);
    List<DocumentoComprobatorio> findByGrupoLogicoIdOrderByVersaoDocumentoAsc(String grupoLogicoId);
    Optional<DocumentoComprobatorio> findByChaveIdempotencia(String chaveIdempotencia);
    List<DocumentoComprobatorio> findByReferenciaSeiNormalizadaOrderByContratoIdAscRegistradoEmAsc(String referencia);
    List<DocumentoComprobatorio> findByContratoIdAndTipoFatoAndFatoIdOrderByRegistradoEmAscIdAsc(
            Long contratoId, TipoFatoDocumental tipoFato, Long fatoId);
    List<DocumentoComprobatorio> findByContratoIdAndTipoFatoAndFatoChaveNormalizadaAndEstadoOrderByRegistradoEmAscIdAsc(
            Long contratoId, TipoFatoDocumental tipoFato, String fatoChaveNormalizada,
            EstadoDocumentoComprobatorio estado);
    long countByNomeArmazenado(String nomeArmazenado);

    @Query("select distinct d.nomeArmazenado from DocumentoComprobatorio d where d.nomeArmazenado is not null")
    List<String> buscarTodosNomesArmazenados();

    @Query("""
        select case when count(d) > 0 then true else false end
        from DocumentoComprobatorio d
        where d.contrato.id = :contratoId and d.tipoFato = :tipoFato
          and d.fatoChaveNormalizada = :fatoChave
          and d.estado in :estados
        """)
    boolean existeFonte(@Param("contratoId") Long contratoId,
                        @Param("tipoFato") TipoFatoDocumental tipoFato,
                        @Param("fatoChave") String fatoChave,
                        @Param("estados") Collection<EstadoDocumentoComprobatorio> estados);
}

