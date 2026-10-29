package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.TabelaTarifaria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TabelaTarifariaRepositorio extends JpaRepository<TabelaTarifaria, Long> {
    List<TabelaTarifaria> findByContratoIdOrderByVigenciaInicioDesc(Long contratoId);
}
