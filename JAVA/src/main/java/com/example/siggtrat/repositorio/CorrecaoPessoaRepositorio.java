package com.example.siggtrat.repositorio;

import com.example.siggtrat.entidade.CorrecaoPessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrecaoPessoaRepositorio extends JpaRepository<CorrecaoPessoa, Long> {
    List<CorrecaoPessoa> findByPessoaIdOrderByRegistradoEmAscIdAsc(Long pessoaId);
}
