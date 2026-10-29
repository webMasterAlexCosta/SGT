package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface FornecedorRepositorio extends JpaRepository<Fornecedor, Long> {
    Optional<Fornecedor> findByCnpj(String cnpj);
    List<Fornecedor> findAllByOrderByRazaoSocialAsc();
}
