package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.CorrecaoCnpjContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CorrecaoCnpjContratoRepositorio extends JpaRepository<CorrecaoCnpjContrato, Long> {
    Optional<CorrecaoCnpjContrato> findByContratoIdAndChaveIdempotencia(Long contratoId, String chaveIdempotencia);
    List<CorrecaoCnpjContrato> findByContratoIdOrderByRegistradoEmAscIdAsc(Long contratoId);
}
