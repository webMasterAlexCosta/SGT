package com.example.siggtrat.repositorio;
import com.example.siggtrat.entidade.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface FornecedorRepositorio extends JpaRepository<Fornecedor, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select f from Fornecedor f where f.id = :id")
    Optional<Fornecedor> buscarPorIdParaAtualizacao(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Fornecedor> findByCnpj(String cnpj);
    List<Fornecedor> findAllByOrderByRazaoSocialAsc();
}
