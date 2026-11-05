package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.ProvaHistoricaAditamentoComposicao;
import com.example.siggtrat.utilitario.PrecisaoTemporal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProvaHistoricaAditamentoComposicaoRepositorio
        extends JpaRepository<ProvaHistoricaAditamentoComposicao, Long> {
    List<ProvaHistoricaAditamentoComposicao> findByProvaId(Long prova);
    default Optional<ProvaHistoricaAditamentoComposicao> primeiroConhecido(Long item,Instant conhecido) {
        return findFirstByAditamentoComposicaoIdAndProvaRegistradoEmLessThanEqualOrderByProvaNumeroRevisaoAsc(
                item,PrecisaoTemporal.limiteInclusivoMicros(conhecido));
    }
    Optional<ProvaHistoricaAditamentoComposicao>
            findFirstByAditamentoComposicaoIdAndProvaRegistradoEmLessThanEqualOrderByProvaNumeroRevisaoAsc(
                    Long item,Instant conhecido);
}
