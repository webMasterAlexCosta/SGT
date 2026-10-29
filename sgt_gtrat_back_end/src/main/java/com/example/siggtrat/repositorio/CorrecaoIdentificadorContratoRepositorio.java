package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.CorrecaoIdentificadorContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CorrecaoIdentificadorContratoRepositorio extends JpaRepository<CorrecaoIdentificadorContrato, Long> {
    Optional<CorrecaoIdentificadorContrato> findByChaveIdempotencia(String chaveIdempotencia);
    List<CorrecaoIdentificadorContrato> findByContratoIdOrderByRegistradoEmAscIdAsc(Long contratoId);

    @Query("""
            select c from CorrecaoIdentificadorContrato c
            where (lower(c.numeroAnterior) = lower(:numero) and c.anoAnterior = :ano)
               or (lower(c.numeroNovo) = lower(:numero) and c.anoNovo = :ano)
            order by c.registradoEm desc, c.id desc
            """)
    List<CorrecaoIdentificadorContrato> pesquisarHistorico(@Param("numero") String numero,
                                                            @Param("ano") Integer ano);
}
