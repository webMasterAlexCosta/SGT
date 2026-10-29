package com.example.siggtrat.servico;

import com.example.siggtrat.dto.BancoDTO;
import com.example.siggtrat.entidade.Banco;
import com.example.siggtrat.repositorio.BancoRepositorio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Camada de servico for {@link Banco}. This class encapsulates the regras de negocio
 * and provides methods that operate on {@link Banco} entities while
 * returning or accepting {@link BancoDTO}. Using a servico layer promotes
 * separation of concerns and allows controllers to remain thin, delegating
 * business operations here.
 */
@Service
public class BancoServico {

    private final BancoRepositorio bancoRepositorio;

    public BancoServico(BancoRepositorio bancoRepositorio) {
        this.bancoRepositorio = bancoRepositorio;
    }

    /**
     * Returns a paginated list of banks converted to DTOs.
     */
    public Page<BancoDTO> listarTodos(Pageable pageable) {
        return bancoRepositorio.findAll(pageable).map(this::paraDTO);
    }

    /**
     * Finds a bank by its identifier and converts it to a DTO. Returns null
     * if the bank does not exist.
     */
    public BancoDTO buscarPorId(Long id) {
        return bancoRepositorio.findById(id).map(this::paraDTO).orElse(null);
    }

    /**
     * Creates a new bank based on the provided DTO and returns the
     * persisted entity converted back to a DTO.
     */
    public BancoDTO criar(BancoDTO dto) {
        Banco banco = new Banco();
        banco.setNome(dto.getNome());
        banco.setCodigo(dto.getCodigo());
        Banco saved = bancoRepositorio.save(banco);
        return paraDTO(saved);
    }

    /**
     * Updates an existing bank identified by {@code id} with the values from
     * the provided DTO. If the bank does not exist, returns null.
     */
    public BancoDTO atualizar(Long id, BancoDTO dto) {
        return bancoRepositorio.findById(id).map(existing -> {
            existing.setNome(dto.getNome());
            existing.setCodigo(dto.getCodigo());
            Banco saved = bancoRepositorio.save(existing);
            return paraDTO(saved);
        }).orElse(null);
    }

    /**
     * Deletes a bank by its identifier.
     */
    public void excluir(Long id) {
        bancoRepositorio.deleteById(id);
    }

    /**
     * Helper method to convert a {@link Banco} entity to its corresponding
     * DTO representation.
     */
    private BancoDTO paraDTO(Banco banco) {
        if (banco == null) return null;
        return new BancoDTO(banco.getId(), banco.getNome(), banco.getCodigo());
    }
}