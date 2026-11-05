package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.ProvaHistoricaComposicao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProvaHistoricaComposicaoRepositorio extends JpaRepository<ProvaHistoricaComposicao,Long> {
 List<ProvaHistoricaComposicao> findByProvaId(Long prova);
}
